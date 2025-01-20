package com.example.bigdataback.service;

import com.example.bigdataback.entity.Product;
import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.*;
import org.apache.spark.sql.expressions.UserDefinedFunction;
import org.apache.spark.sql.types.DataTypes;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static org.apache.spark.sql.functions.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class MovieRecommendationService {
    private final SparkSession spark;

    @PostConstruct
    public void init() {
        spark.udf().register("calculateActorMatch", calculateActorMatch);
        spark.udf().register("calculateMpaaMatch", calculateMpaaMatch);
        spark.udf().register("calculateThemeMatch", calculateThemeMatch);
        log.info("Movie UDFs registered successfully");
    }

    @Data
    @Builder
    private static class MovieMetadata {
        private final String title;
        private final List<String> description;
        private final List<String> categories;
        private final String mediaFormat;
        private final List<String> actors;
        private final String runtime;
        private final String mpaaRating;
        private final Double price;
    }

    public List<Product> getMovieRecommendations(String parentAsin, Integer maxRecommendations) {
        try {
            Dataset<Row> sourceProduct = loadAndAnalyzeSourceProduct(parentAsin);
            if (sourceProduct == null) return Collections.emptyList();

            Row sourceRow = sourceProduct.first();
            MovieMetadata sourceMetadata = extractMovieMetadata(sourceRow);

            Dataset<Row> candidates = loadCandidates(parentAsin);
            if (candidates == null) return Collections.emptyList();

            Map<String, Dataset<Row>> scores = new HashMap<>();
            scores.put("rating", calculateRatingScores(candidates));
            scores.put("genre", calculateGenreScores(candidates, sourceMetadata.getCategories()));
            scores.put("actor", calculateActorScores(candidates, sourceMetadata.getActors()));
            scores.put("theme", calculateThemeScores(candidates, sourceMetadata));
            scores.put("mpaa", calculateMpaaScores(candidates, sourceMetadata.getMpaaRating()));
            scores.put("price", calculatePriceScores(candidates, sourceMetadata.getPrice()));

            Dataset<Row> recommendations = assembleRecommendations(candidates, scores, maxRecommendations);

            List<Product> result = extractAndMapResults(recommendations);
            cleanupDatasets(sourceProduct, candidates);
            return result;

        } catch (Exception e) {
            log.error("Error in movie recommendations for asin {}: {}", parentAsin, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private Dataset<Row> loadAndAnalyzeSourceProduct(String parentAsin) {
        Dataset<Row> sourceProduct = spark.read()
                .format("mongodb")
                .option("uri", "mongodb://localhost:27017")
                .option("database", "amazon_reviews")
                .option("collection", "metadata")
                .load()
                .filter(col("parent_asin").equalTo(parentAsin))
                .cache();

        if (sourceProduct.count() == 0) {
            log.warn("No source product found for parentAsin: {}", parentAsin);
            return null;
        }
        return sourceProduct;
    }

    private Dataset<Row> loadCandidates(String parentAsin) {
        Dataset<Row> candidates = spark.read()
                .format("mongodb")
                .option("uri", "mongodb://localhost:27017")
                .option("database", "amazon_reviews")
                .option("collection", "metadata")
                .load()
                .filter(col("main_category").equalTo("Movies & TV"))
                .filter(col("parent_asin").notEqual(parentAsin))
                .select(
                        "parent_asin", "title", "price", "average_rating",
                        "rating_number", "categories", "description",
                        "main_category", "details"
                )
                .cache();

        long count = candidates.count();
        if (count == 0) {
            log.warn("No candidates found");
            return null;
        }
        log.info("Found {} candidates", count);
        return candidates;
    }

    private MovieMetadata extractMovieMetadata(Row row) {
        Map<String, String> details = getMapFromRow(row, "details");

        return MovieMetadata.builder()
                .title(getStringOrNull(row, "title"))
                .description(getListFromSeq(row, "description"))
                .categories(getListFromSeq(row, "categories"))
                .mediaFormat(details.getOrDefault("Media Format", null))
                .actors(details.containsKey("Actors") ?
                        Arrays.asList(details.get("Actors").split("\\s*,\\s*")) :
                        Collections.emptyList())
                .runtime(details.getOrDefault("Run time", null))
                .mpaaRating(details.getOrDefault("MPAA rating", null))
                .price(getDoubleOrNull(row, "price"))
                .build();
    }

    private Dataset<Row> calculateRatingScores(Dataset<Row> candidates) {
        return candidates
                .withColumn("rating_score",
                        when(col("average_rating").isNotNull()
                                        .and(col("rating_number").isNotNull()),
                                col("average_rating").divide(5.0)
                                        .multiply(
                                                when(col("rating_number").gt(100), 1.0)
                                                        .when(col("rating_number").gt(50), 0.9)
                                                        .when(col("rating_number").gt(20), 0.8)
                                                        .otherwise(0.7)
                                        ))
                                .otherwise(0.5))
                .select("parent_asin", "rating_score");
    }

    private Dataset<Row> calculateGenreScores(Dataset<Row> candidates, List<String> sourceCategories) {
        if (sourceCategories.isEmpty()) {
            return candidates
                    .withColumn("genre_score", lit(0.5))
                    .select("parent_asin", "genre_score");
        }

        Column categoriesCol = array(sourceCategories.stream()
                .map(functions::lit)
                .toArray(Column[]::new));

        return candidates
                .withColumn("genre_score",
                        when(col("categories").isNull(), 0.5)
                                .otherwise(
                                        size(array_intersect(col("categories"), categoriesCol))
                                                .divide(size(categoriesCol))
                                                .multiply(0.8)
                                                .plus(0.2)
                                ))
                .select("parent_asin", "genre_score");
    }
    private Dataset<Row> calculateActorScores(Dataset<Row> candidates, List<String> sourceActors) {
        if (sourceActors.isEmpty()) {
            return candidates
                    .withColumn("actor_score", lit(0.5))
                    .select("parent_asin", "actor_score");
        }

        return candidates
                .withColumn("actor_score",
                        callUDF("calculateActorMatch",
                                expr("details.Actors"),
                                array(sourceActors.stream()
                                        .map(functions::lit)
                                        .toArray(Column[]::new))))
                .select("parent_asin", "actor_score");
    }

    private static UserDefinedFunction calculateActorMatch = udf(
            (String actorsStr, scala.collection.Seq<String> sourceActors) -> {
                if (actorsStr == null || sourceActors == null) return 0.5;

                Set<String> candidateActors = new HashSet<>(Arrays.asList(actorsStr.split("\\s*,\\s*")));
                Set<String> sourceActorSet = new HashSet<>(scala.collection.JavaConverters.seqAsJavaList(sourceActors));

                Set<String> intersection = new HashSet<>(candidateActors);
                intersection.retainAll(sourceActorSet);

                if (intersection.isEmpty()) return 0.5;
                return Math.min(0.5 + (intersection.size() * 0.25), 1.0);
            }, DataTypes.DoubleType
    );

    private Dataset<Row> calculateThemeScores(Dataset<Row> candidates, MovieMetadata source) {
        return candidates
                .withColumn("theme_score",
                        callUDF("calculateThemeMatch",
                                col("title"),
                                col("description"),
                                lit(source.getTitle()),
                                array(source.getDescription().stream()
                                        .map(functions::lit)
                                        .toArray(Column[]::new))))
                .select("parent_asin", "theme_score");
    }

    private static UserDefinedFunction calculateThemeMatch = udf(
            (String title, scala.collection.Seq<String> description,
             String sourceTitle, scala.collection.Seq<String> sourceDescription) -> {

                Set<String> themeKeywords = new HashSet<>();
                Set<String> sourceKeywords = new HashSet<>();

                Set<String> stopwords = new HashSet<>(Arrays.asList(
                        "the", "a", "an", "and", "or", "but", "in", "on", "at", "to",
                        "for", "of", "with", "by", "from", "up", "about", "into", "over",
                        "after", "dvd", "blu-ray", "edition", "version", "series"
                ));

                addKeywords(sourceTitle, sourceKeywords, stopwords);
                if (sourceDescription != null) {
                    scala.collection.JavaConverters.seqAsJavaList(sourceDescription)
                            .forEach(desc -> addKeywords(desc, sourceKeywords, stopwords));
                }

                addKeywords(title, themeKeywords, stopwords);
                if (description != null) {
                    scala.collection.JavaConverters.seqAsJavaList(description)
                            .forEach(desc -> addKeywords(desc, themeKeywords, stopwords));
                }

                if (sourceKeywords.isEmpty()) return 0.5;

                Set<String> intersection = new HashSet<>(sourceKeywords);
                intersection.retainAll(themeKeywords);

                return 0.3 + Math.min(0.7, (intersection.size() * 1.0 / sourceKeywords.size()));
            }, DataTypes.DoubleType
    );

    private Dataset<Row> calculateMpaaScores(Dataset<Row> candidates, String sourceMpaaRating) {
        return candidates
                .withColumn("mpaa_score",
                        callUDF("calculateMpaaMatch",
                                expr("details.`MPAA rating`"),
                                lit(sourceMpaaRating)))
                .select("parent_asin", "mpaa_score");
    }

    private static UserDefinedFunction calculateMpaaMatch = udf(
            (String candidateRating, String sourceRating) -> {
                if (candidateRating == null || sourceRating == null) return 0.5;

                String source = sourceRating.replaceAll("\\s*\\([^)]*\\)", "").trim();
                String candidate = candidateRating.replaceAll("\\s*\\([^)]*\\)", "").trim();

                if (source.equals(candidate)) return 1.0;

                Map<String, List<String>> similarRatings = new HashMap<>();
                similarRatings.put("G", Arrays.asList("TV-Y", "TV-G"));
                similarRatings.put("PG", Arrays.asList("TV-PG"));
                similarRatings.put("PG-13", Arrays.asList("TV-14"));
                similarRatings.put("R", Arrays.asList("TV-MA", "NC-17"));
                similarRatings.put("NR", Arrays.asList("Unrated", "Not Rated"));

                List<String> similarToSource = similarRatings.get(source);
                if (similarToSource != null && similarToSource.contains(candidate)) {
                    return 0.8;
                }

                return 0.4;
            }, DataTypes.DoubleType
    );

    private Dataset<Row> calculatePriceScores(Dataset<Row> candidates, Double sourcePrice) {
        if (sourcePrice == null) {
            return candidates
                    .withColumn("price_score", lit(0.5))
                    .select("parent_asin", "price_score");
        }

        double upperLimit = sourcePrice * 1.1;
        double lowerLimit = sourcePrice * 0.9;
        double upperLimit25 = sourcePrice * 1.25;
        double lowerLimit25 = sourcePrice * 0.75;

        return candidates
                .withColumn("price_score",
                        when(col("price").isNull(), 0.5)
                                .otherwise(
                                        when(col("price").equalTo(lit(sourcePrice)), 1.0)
                                                .when(col("price").leq(lit(upperLimit))
                                                        .and(col("price").geq(lit(lowerLimit))), 0.9)
                                                .when(col("price").leq(lit(upperLimit25))
                                                        .and(col("price").geq(lit(lowerLimit25))), 0.7)
                                                .otherwise(0.4)
                                ))
                .select("parent_asin", "price_score");
    }

    private Dataset<Row> assembleRecommendations(Dataset<Row> candidates,
                                                 Map<String, Dataset<Row>> scores, Integer maxRecommendations) {
        Dataset<Row> result = candidates;

        for (Map.Entry<String, Dataset<Row>> score : scores.entrySet()) {
            result = result.join(score.getValue(), "parent_asin");
        }

        return result.withColumn("final_score",
                col("rating_score").multiply(0.30)    // Notes et nombre d'avis
                        .plus(col("genre_score").multiply(0.25))  // Catégories
                        .plus(col("theme_score").multiply(0.20))  // Thème
                        .plus(col("actor_score").multiply(0.15))  // Acteurs
                        .plus(col("mpaa_score").multiply(0.07))   // Classification d'âge
                        .plus(col("price_score").multiply(0.03))  // Prix
                )
                .filter(col("rating_number").gt(5)
                        .and(col("final_score").gt(0.4)))
                .orderBy(col("final_score").desc())
                .limit(maxRecommendations);
    }

    private static void addKeywords(String text, Set<String> keywords, Set<String> stopwords) {
        if (text == null) return;
        Arrays.stream(text.toLowerCase().split("\\W+"))
                .filter(word -> word.length() > 2)
                .filter(word -> !stopwords.contains(word))
                .forEach(keywords::add);
    }

    private String getStringOrNull(Row row, String fieldName) {
        try {
            return row.isNullAt(row.fieldIndex(fieldName)) ? null : row.getString(row.fieldIndex(fieldName));
        } catch (Exception e) {
            log.debug("Field {} not found or invalid", fieldName);
            return null;
        }
    }

    private Double getDoubleOrNull(Row row, String fieldName) {
        try {
            return row.isNullAt(row.fieldIndex(fieldName)) ? null : row.getDouble(row.fieldIndex(fieldName));
        } catch (Exception e) {
            log.debug("Field {} not found or invalid", fieldName);
            return null;
        }
    }

    private List<String> getListFromSeq(Row row, String fieldName) {
        try {
            if (row.isNullAt(row.fieldIndex(fieldName))) return Collections.emptyList();
            scala.collection.Seq<String> seq = row.getSeq(row.fieldIndex(fieldName));
            return seq == null ? Collections.emptyList() :
                    scala.collection.JavaConverters.seqAsJavaList(seq);
        } catch (Exception e) {
            log.debug("Field {} not found or invalid", fieldName);
            return Collections.emptyList();
        }
    }

    private Map<String, String> getMapFromRow(Row row, String fieldName) {
        try {
            if (row.isNullAt(row.fieldIndex(fieldName))) return Collections.emptyMap();
            return row.getJavaMap(row.fieldIndex(fieldName));
        } catch (Exception e) {
            log.debug("Field {} not found or invalid", fieldName);
            return Collections.emptyMap();
        }
    }

    private void cleanupDatasets(Dataset<Row>... datasets) {
        for (Dataset<Row> dataset : datasets) {
            if (dataset != null) {
                dataset.unpersist();
            }
        }
        System.gc();
    }

    private List<Product> extractAndMapResults(Dataset<Row> recommendations) {
        return recommendations
                .select(
                        "parent_asin", "title", "price", "average_rating",
                        "rating_number", "main_category", "categories",
                        "description", "details", "final_score"
                )
                .collectAsList()
                .stream()
                .map(this::convertRowToProduct)
                .collect(Collectors.toList());
    }

    private Product convertRowToProduct(Row row) {
        try {
            Product product = new Product();
            product.setParentAsin(getStringOrNull(row, "parent_asin"));
            product.setTitle(getStringOrNull(row, "title"));
            product.setMainCategory("Movies & TV");
            product.setPrice(getDoubleOrNull(row, "price"));
            product.setAverageRating(getDoubleOrNull(row, "average_rating"));
            product.setRatingNumber(row.isNullAt(row.fieldIndex("rating_number")) ?
                    null : row.getInt(row.fieldIndex("rating_number")));
            product.setCategories(getListFromSeq(row, "categories"));
            product.setDescription(getListFromSeq(row, "description"));

            if (!row.isNullAt(row.fieldIndex("details"))) {
                Row detailsRow = row.getStruct(row.fieldIndex("details"));
                Map<String, Object> details = new HashMap<>();

                for (String fieldName : detailsRow.schema().fieldNames()) {
                    if (!detailsRow.isNullAt(detailsRow.fieldIndex(fieldName))) {
                        String value = detailsRow.getString(detailsRow.fieldIndex(fieldName));
                        // Convertir la valeur String en List<String> si nécessaire
                        details.put(fieldName, Collections.singletonList(value));
                    }
                }
                product.setDetails(details);
            }

            product.setFinalScore(getDoubleOrNull(row, "final_score"));
            return product;
        } catch (Exception e) {
            log.error("Error converting row to product: {}", e.getMessage());
            throw new RuntimeException("Failed to convert row to product", e);
        }
    }
}
