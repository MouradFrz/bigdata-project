package com.example.bigdataback.service;

import com.example.bigdataback.entity.Product;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.*;
import org.apache.spark.sql.expressions.UserDefinedFunction;
import org.apache.spark.sql.types.DataTypes;
import org.springframework.stereotype.Service;
import scala.collection.immutable.Seq;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.spark.sql.functions.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookRecommendationService {
    private final SparkSession spark;

    @PostConstruct
    public void init() {
        spark.udf().register("calculateThemeMatch", calculateThemeMatch);
        spark.udf().register("calculateAgeMatch", calculateAgeMatch);
        log.info("Book UDFs registered successfully");
    }

    public List<Product> getBookRecommendations(String parentAsin, Integer maxRecommendations) {
        try {
            Dataset<Row> sourceBook = loadAndAnalyzeSourceBook(parentAsin);
            log.info("Source book loaded: {}", sourceBook != null);
            if (sourceBook == null) return Collections.emptyList();
            Dataset<Row> candidates = loadCandidates(parentAsin);
            log.info("Candidates loaded: {}", candidates != null);
            if (candidates == null) return Collections.emptyList();
            long candidateCount = candidates.count();
            log.info("Initial candidate count: {}", candidateCount);

            // Calculer et logger chaque score
            Map<String, Dataset<Row>> scores = new HashMap<>();
            scores.put("rating", calculateRatingScores(candidates));
            log.info("Rating scores calculated");

            scores.put("category", calculateCategoryScores(candidates, sourceBook));
            log.info("Category scores calculated");

            scores.put("theme", calculateThemeScores(candidates, sourceBook));
            log.info("Theme scores calculated");

            scores.put("age", calculateAgeScores(candidates, sourceBook));
            log.info("Age scores calculated");

            scores.put("price", calculatePriceScores(candidates, sourceBook));
            log.info("Price scores calculated");

            // Logger les statistiques
            Dataset<Row> recommendations = assembleRecommendations(candidates, scores, maxRecommendations);
            long finalCount = recommendations.count();
            log.info("Final recommendations count: {}", finalCount);

            return extractAndMapResults(recommendations);
        } catch (Exception e) {
            log.error("Error in book recommendations for asin {}: {}", parentAsin, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    private Dataset<Row> loadAndAnalyzeSourceBook(String parentAsin) {
        Dataset<Row> sourceBook = spark.read()
                .format("mongodb")
                .option("uri", "mongodb://localhost:27017")
                .option("database", "amazon_reviews")
                .option("collection", "metadata")
                .load()
                .filter(col("parent_asin").equalTo(parentAsin))
                .cache();

        if (sourceBook.count() == 0) {
            log.warn("No source book found for parentAsin: {}", parentAsin);
            return null;
        }
        return sourceBook;
    }

    private Dataset<Row> loadCandidates(String parentAsin) {
        Dataset<Row> candidates = spark.read()
                .format("mongodb")
                .option("uri", "mongodb://localhost:27017")
                .option("database", "amazon_reviews")
                .option("collection", "metadata")
                .load()
                .filter(col("main_category").equalTo("Books"))
                .filter(col("parent_asin").notEqual(parentAsin))
                .select(
                        "parent_asin", "title", "price", "average_rating",
                        "rating_number", "categories", "description", "details"
                )
                .cache();

        long count = candidates.count();
        log.info("Found {} book candidates", count);
        return count > 0 ? candidates : null;
    }

    private Dataset<Row> calculateRatingScores(Dataset<Row> candidates) {
        return candidates
                .withColumn("rating_score",
                        when(col("average_rating").isNotNull()
                                        .and(col("rating_number").isNotNull()),
                                col("average_rating").divide(5.0)
                                        .multiply(
                                                when(col("rating_number").gt(100), 1.0)
                                                        .when(col("rating_number").gt(50), 0.85)
                                                        .when(col("rating_number").gt(20), 0.7)
                                                        .when(col("rating_number").gt(10), 0.6)
                                                        .otherwise(0.5)
                                        ))
                                .otherwise(0.4))
                .select("parent_asin", "rating_score");
    }
    private Dataset<Row> calculateCategoryScores(Dataset<Row> candidates, Dataset<Row> sourceBook) {
        List<String> sourceCategories = getListFromSeq(sourceBook.first(), "categories");
        if (sourceCategories.isEmpty()) {
            return candidates
                    .withColumn("category_score", lit(0.5))
                    .select("parent_asin", "category_score");
        }

        spark.udf().register("calculateCategoryOverlap", (scala.collection.Seq<String> categories) -> {
            if (categories == null) return 0.5;

            List<String> candidateCategories = scala.collection.JavaConverters.seqAsJavaList(categories);
            long matches = candidateCategories.stream()
                    .filter(sourceCategories::contains)
                    .count();

            return matches == 0 ? 0.3 :
                    0.3 + Math.min(0.7, (matches * 0.7) / sourceCategories.size());
        }, DataTypes.DoubleType);

        return candidates
                .withColumn("category_score",
                        callUDF("calculateCategoryOverlap", col("categories")))
                .select("parent_asin", "category_score");
    }
    private Dataset<Row> calculateThemeScores(Dataset<Row> candidates, Dataset<Row> sourceBook) {
        String sourceTitle = sourceBook.first().getString(sourceBook.first().fieldIndex("title"));
        List<String> sourceDesc = getListFromSeq(sourceBook.first(), "description");

        spark.udf().register("calculateThemeMatchUDF",
                (String title, scala.collection.Seq<String> description) -> {
                    Set<String> themeKeywords = new HashSet<>();
                    Set<String> sourceKeywords = new HashSet<>();

                    Set<String> stopwords = new HashSet<>(Arrays.asList(
                            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to",
                            "for", "of", "with", "by", "from", "book", "edition", "volume"
                    ));

                    addKeywords(sourceTitle, sourceKeywords, stopwords);
                    if (sourceDesc != null) {
                        sourceDesc.forEach(desc -> addKeywords(desc, sourceKeywords, stopwords));
                    }

                    addKeywords(title, themeKeywords, stopwords);
                    if (description != null) {
                        scala.collection.JavaConverters.seqAsJavaList(description)
                                .forEach(desc -> addKeywords(desc, themeKeywords, stopwords));
                    }

                    if (sourceKeywords.isEmpty()) return 0.5;

                    Set<String> intersection = new HashSet<>(sourceKeywords);
                    intersection.retainAll(themeKeywords);

                    return 0.2 + Math.min(0.8, (intersection.size() * 1.0 / sourceKeywords.size()));
                }, DataTypes.DoubleType);

        return candidates
                .withColumn("theme_score",
                        callUDF("calculateThemeMatchUDF",
                                col("title"),
                                col("description")))
                .select("parent_asin", "theme_score");
    }
    private static UserDefinedFunction calculateThemeMatch = udf(
            (String title, Seq<String> description, String sourceTitle, List<String> sourceDescription) -> {
                Set<String> themeKeywords = new HashSet<>();
                Set<String> sourceKeywords = new HashSet<>();

                Set<String> stopwords = new HashSet<>(Arrays.asList(
                        "the", "a", "an", "and", "or", "but", "in", "on", "at", "to",
                        "for", "of", "with", "by", "from", "book", "edition", "volume"
                ));

                // Traiter le titre et la description source
                addKeywords(sourceTitle, sourceKeywords, stopwords);
                if (sourceDescription != null) {
                    sourceDescription.forEach(desc -> addKeywords(desc, sourceKeywords, stopwords));
                }

                // Traiter le titre et la description du candidat
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
    private Dataset<Row> calculateAgeScores(Dataset<Row> candidates, Dataset<Row> sourceBook) {
        Row sourceRow = sourceBook.first();
        String sourceAgeRange = extractAgeRange(sourceRow);

        return candidates
                .withColumn("age_score",
                        callUDF("calculateAgeMatch",
                                col("title"),
                                col("description"),
                                lit(sourceAgeRange)))
                .select("parent_asin", "age_score");
    }

    private static UserDefinedFunction calculateAgeMatch = udf(
            (String title, scala.collection.Seq<String> description, String sourceAgeRange) -> {
                if (sourceAgeRange == null) return 0.5;

                String bookAge = extractAgeFromText(title);
                if (bookAge == null && description != null) {
                    for (String desc : scala.collection.JavaConverters.seqAsJavaList(description)) {
                        bookAge = extractAgeFromText(desc);
                        if (bookAge != null) break;
                    }
                }

                if (bookAge == null) return 0.5;

                if (bookAge.equals(sourceAgeRange)) return 1.0;

                // Tranches d'âge similaires
                Map<String, Set<String>> similarAges = new HashMap<>();
                similarAges.put("0-2", Set.of("baby", "infant", "toddler"));
                similarAges.put("3-5", Set.of("preschool", "kindergarten"));
                similarAges.put("6-8", Set.of("early reader", "first reader"));
                similarAges.put("9-12", Set.of("middle grade", "elementary"));
                similarAges.put("13-17", Set.of("young adult", "teen"));
                similarAges.put("18+", Set.of("adult", "college"));

                for (Map.Entry<String, Set<String>> entry : similarAges.entrySet()) {
                    if ((entry.getKey().equals(sourceAgeRange) && entry.getValue().contains(bookAge)) ||
                            (entry.getKey().equals(bookAge) && entry.getValue().contains(sourceAgeRange))) {
                        return 0.8;
                    }
                }

                return 0.4;
            }, DataTypes.DoubleType
    );

    private Dataset<Row> calculatePriceScores(Dataset<Row> candidates, Dataset<Row> sourceBook) {
        Double sourcePrice = getDoubleOrNull(sourceBook.first(), "price");
        if (sourcePrice == null) {
            return candidates
                    .withColumn("price_score", lit(0.5))
                    .select("parent_asin", "price_score");
        }

        double upperLimit = sourcePrice * 1.2;
        double lowerLimit = sourcePrice * 0.8;

        return candidates
                .withColumn("price_score",
                        when(col("price").isNull(), 0.5)
                                .otherwise(
                                        when(col("price").equalTo(lit(sourcePrice)), 1.0)
                                                .when(col("price").leq(lit(upperLimit))
                                                        .and(col("price").geq(lit(lowerLimit))), 0.8)
                                                .otherwise(0.5)
                                ))
                .select("parent_asin", "price_score");
    }

    private Dataset<Row> assembleRecommendations(Dataset<Row> candidates,
                                                 Map<String, Dataset<Row>> scores, Integer maxRecommendations) {
        Dataset<Row> result = candidates;

        for (Dataset<Row> score : scores.values()) {
            result = result.join(score, "parent_asin");
        }

        return result
                .withColumn("final_score",
                        col("rating_score").multiply(0.25)
                                .plus(col("category_score").multiply(0.25))
                                .plus(col("theme_score").multiply(0.20))
                                .plus(col("age_score").multiply(0.20))
                                .plus(col("price_score").multiply(0.10))
                )
                .filter(col("rating_number").gt(1)
                        .and(col("final_score").gt(0.35)))
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

    private String extractAgeRange(Row row) {
        try {
            // Chercher dans le titre
            String title = row.getString(row.fieldIndex("title"));
            String ageRange = extractAgeFromText(title);
            if (ageRange != null) return ageRange;

            List<String> description = getListFromSeq(row, "description");
            for (String desc : description) {
                ageRange = extractAgeFromText(desc);
                if (ageRange != null) return ageRange;
            }

            Map<String, String> details = getMapFromRow(row, "details");
            if (details != null) {
                String[] fields = {"Age Range", "Reading Level", "Grade Level"};
                for (String field : fields) {
                    if (details.containsKey(field)) {
                        ageRange = extractAgeFromText(details.get(field));
                        if (ageRange != null) return ageRange;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error extracting age range: {}", e.getMessage());
        }
        return null;
    }

    private static String extractAgeFromText(String text) {
        if (text == null) return null;
        text = text.toLowerCase();

        // Patterns pour les tranches d'âge
        List<Pattern> patterns = Arrays.asList(
                Pattern.compile("(\\d+)\\s*-\\s*(\\d+)\\s*years?"),
                Pattern.compile("ages?\\s*(\\d+)\\s*-\\s*(\\d+)"),
                Pattern.compile("(\\d+)\\+\\s*years?")
        );

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group();
            }
        }

        // Mots-clés pour les tranches d'âge
        Map<String, String> ageKeywords = Map.of(
                "baby", "0-2",
                "toddler", "0-2",
                "preschool", "3-5",
                "kindergarten", "3-5",
                "elementary", "6-8",
                "middle grade", "9-12",
                "young adult", "13-17",
                "teen", "13-17",
                "adult", "18+"
        );

        for (Map.Entry<String, String> entry : ageKeywords.entrySet()) {
            if (text.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    private List<Product> extractAndMapResults(Dataset<Row> recommendations) {
        return recommendations
                .select(
                        "parent_asin", "title", "price", "average_rating",
                        "rating_number", "categories", "description",
                        "details", "final_score"
                )
                .collectAsList()
                .stream()
                .map(this::convertRowToProduct)
                .collect(Collectors.toList());
    }

    private String getStringOrNull(Row row, String fieldName) {
        try {
            return row.isNullAt(row.fieldIndex(fieldName)) ?
                    null : row.getString(row.fieldIndex(fieldName));
        } catch (Exception e) {
            log.debug("Field {} not found or invalid", fieldName);
            return null;
        }
    }

    private Double getDoubleOrNull(Row row, String fieldName) {
        try {
            return row.isNullAt(row.fieldIndex(fieldName)) ?
                    null : row.getDouble(row.fieldIndex(fieldName));
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

    private Product convertRowToProduct(Row row) {
        try {
            Product product = new Product();
            product.setParentAsin(getStringOrNull(row, "parent_asin"));
            product.setTitle(getStringOrNull(row, "title"));
            product.setMainCategory("Books");
            product.setPrice(getDoubleOrNull(row, "price"));
            product.setAverageRating(getDoubleOrNull(row, "average_rating"));
            product.setRatingNumber(row.isNullAt(row.fieldIndex("rating_number")) ?
                    null : row.getInt(row.fieldIndex("rating_number")));
            product.setCategories(getListFromSeq(row, "categories"));
            product.setDescription(getListFromSeq(row, "description"));

            Map<String, String> details = getMapFromRow(row, "details");
            if (!details.isEmpty()) {
                Map<String, Object> convertedDetails = new HashMap<>();
                details.forEach((key, value) -> convertedDetails.put(key, value));
                product.setDetails(convertedDetails);
            }

            product.setFinalScore(getDoubleOrNull(row, "final_score"));
            return product;
        } catch (Exception e) {
            log.error("Error converting row to product: {}", e.getMessage());
            throw new RuntimeException("Failed to convert row to product", e);
        }
    }
}
