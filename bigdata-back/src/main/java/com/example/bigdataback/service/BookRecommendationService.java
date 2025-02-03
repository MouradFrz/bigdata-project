package com.example.bigdataback.service;

import com.example.bigdataback.entity.Product;
import com.example.bigdataback.entity.ProductImage;
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
                        "rating_number", "categories", "description", "details","images"
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
        // Configuration des stopwords
        Column stopwordsArray = array(
                lit("the"), lit("a"), lit("an"), lit("and"), lit("or"), lit("but"),
                lit("in"), lit("on"), lit("at"), lit("to"), lit("for"), lit("of"),
                lit("with"), lit("by"), lit("from"), lit("book"), lit("edition"), lit("volume")
        );

        // Obtenir le texte source
        List<String> sourceTexts = new ArrayList<>();
        Row sourceRow = sourceBook.first();
        String sourceTitle = getStringOrNull(sourceRow, "title");
        if (sourceTitle != null) {
            sourceTexts.add(sourceTitle);
        }
        List<String> sourceDesc = getListFromSeq(sourceRow, "description");
        if (sourceDesc != null) {
            sourceTexts.addAll(sourceDesc);
        }

        // Traitement des mots-clés source
        Dataset<Row> sourceWords = spark.createDataset(sourceTexts, Encoders.STRING())
                .select(explode(
                        split(lower(
                                regexp_replace(col("value"), "[^a-zA-Z\\s]", " ")
                        ), "\\s+")
                ).as("word"))
                .where(length(col("word")).gt(2)
                        .and(not(array_contains(stopwordsArray, col("word")))))
                .distinct();

        // Nombre total de mots-clés source
        long sourceKeywordsCount = sourceWords.count();
        if (sourceKeywordsCount == 0) {
            return candidates
                    .withColumn("theme_score", lit(0.5))
                    .select("parent_asin", "theme_score");
        }

        return candidates
                // Analyse du titre et de la description
                .select(
                        col("parent_asin"),
                        explode(
                                array(
                                        col("title"),
                                        coalesce(array_join(col("description"), " "), lit(""))
                                )
                        ).as("text")
                )
                // Extraction des mots
                .select(
                        col("parent_asin"),
                        explode(
                                split(lower(
                                        regexp_replace(col("text"), "[^a-zA-Z\\s]", " ")
                                ), "\\s+")
                        ).as("word")
                )
                // Filtrage des mots courts et stopwords
                .where(length(col("word")).gt(2)
                        .and(not(array_contains(stopwordsArray, col("word")))))
                .distinct()
                // Comparaison avec les mots-clés source
                .join(sourceWords, "word")
                .groupBy("parent_asin")
                .agg(count("*").as("matching_words"))
                // Calcul du score final
                .withColumn("theme_score",
                        lit(0.2).plus(
                                least(
                                        lit(0.8),
                                        col("matching_words").divide(lit(sourceKeywordsCount))
                                )
                        ))
                .select("parent_asin", "theme_score");
    }

    private Dataset<Row> calculateAgeScores(Dataset<Row> candidates, Dataset<Row> sourceBook) {
        Row sourceRow = sourceBook.first();
        String sourceAgeRange = extractAgeRange(sourceRow);

        if (sourceAgeRange == null) {
            return candidates
                    .withColumn("age_score", lit(0.5))
                    .select("parent_asin", "age_score");
        }

        // Définir les tranches d'âge similaires de manière statique
        Map<String, Set<String>> similarAges = Map.of(
                "0-2", Set.of("baby", "infant", "toddler"),
                "3-5", Set.of("preschool", "kindergarten"),
                "6-8", Set.of("early reader", "first reader"),
                "9-12", Set.of("middle grade", "elementary"),
                "13-17", Set.of("young adult", "teen"),
                "18+", Set.of("adult", "college")
        );

        // Convertir les mots-clés en une chaîne de recherche pour Spark SQL
        String keywordsPattern = similarAges.getOrDefault(sourceAgeRange, Collections.emptySet())
                .stream()
                .collect(Collectors.joining("|", "(", ")"));

        return candidates
                .withColumn("age_range",
                        lower(
                                concat_ws(" ",
                                        coalesce(col("title"), lit("")),
                                        coalesce(array_join(col("description"), " "), lit(""))
                                )
                        )
                )
                .withColumn("age_score",
                        when(col("age_range").isNull(), 0.5)
                                .when(col("age_range").rlike(sourceAgeRange), 1.0)
                                .when(
                                        col("age_range").rlike(keywordsPattern),
                                        0.8
                                )
                                .otherwise(0.4)
                )
                .drop("age_range")
                .select("parent_asin", "age_score");
    }
    private static boolean containsAgeKeywords(String ageRange, List<String> keywords) {
        if (ageRange == null || keywords == null || keywords.isEmpty()) {
            return false;
        }
        return keywords.stream().anyMatch(ageRange::contains);
    }

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
                        "details", "final_score", "images"
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

            List<ProductImage> images = getImagesFromRow(row);
            if (images != null && !images.isEmpty()) {
                product.setImages(images);
            }

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

    private List<ProductImage> getImagesFromRow(Row row) {
        try {
            if (row.isNullAt(row.fieldIndex("images"))) {
                log.debug("No images field for product: {}", getStringOrNull(row, "title"));
                return Collections.emptyList();
            }

            scala.collection.Seq<Row> imageRows = row.getSeq(row.fieldIndex("images"));
            if (imageRows == null) {
                log.debug("Null images array for product: {}", getStringOrNull(row, "title"));
                return Collections.emptyList();
            }

            List<Row> javaImageRows = scala.collection.JavaConverters.seqAsJavaList(imageRows);
            List<ProductImage> images = javaImageRows.stream()
                    .map(imageRow -> {
                        ProductImage image = new ProductImage();
                        String thumb = getStringFromImageRow(imageRow, "thumb");
                        String large = getStringFromImageRow(imageRow, "large");
                        String hiRes = getStringFromImageRow(imageRow, "hi_res");
                        String variant = getStringFromImageRow(imageRow, "variant");

                        image.setThumb(thumb);
                        image.setLarge(large);
                        image.setHiRes(hiRes);
                        image.setVariant(variant);

                        return image;
                    })
                    .filter(this::hasDisplayableUrl)
                    .collect(Collectors.toList());

            if (!images.isEmpty()) {
                log.debug("Successfully extracted {} images for product: {}",
                        images.size(),
                        getStringOrNull(row, "title"));
            }

            return images;
        } catch (Exception e) {
            log.error("Error extracting images for product {}: {}",
                    getStringOrNull(row, "title"),
                    e.getMessage(),
                    e);
            return Collections.emptyList();
        }
    }

    private boolean hasDisplayableUrl(ProductImage image) {
        boolean hasUrl = image.getLarge() != null ||
                image.getThumb() != null ||
                image.getHiRes() != null;

        if (!hasUrl) {
            log.debug("Filtering out image without any usable URLs, variant: {}",
                    image.getVariant());
        }

        return hasUrl;
    }

    private String getStringFromImageRow(Row imageRow, String fieldName) {
        try {
            if (imageRow.isNullAt(imageRow.fieldIndex(fieldName))) {
                return null;
            }
            String value = imageRow.getString(imageRow.fieldIndex(fieldName));
            return value != null && !value.trim().isEmpty() ? value.trim() : null;
        } catch (IllegalArgumentException e) {
            return null;
        } catch (Exception e) {
            log.debug("Error getting {} from image row: {}", fieldName, e.getMessage());
            return null;
        }
    }

}
