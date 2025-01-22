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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import static org.apache.spark.sql.functions.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class SparkRecommendationService {

    private final SparkSession spark;

    @PostConstruct
    public void init() {
        spark.udf().register("calculateAgeMatch", calculateAgeMatch);
        log.info("UDFs registered successfully");
    }

    public List<Product> getSparkRecommendations(String parentAsin, Integer maxRecommendations) {
        try {
            log.info("Starting Spark recommendations for parentAsin: {}", parentAsin);

            // 1. Charger et analyser le produit source
            Dataset<Row> sourceProduct = loadAndAnalyzeSourceProduct(parentAsin);
            if (sourceProduct == null) return Collections.emptyList();

            Row sourceRow = sourceProduct.first();
            String sourceCategory = sourceRow.getString(sourceRow.fieldIndex("main_category"));
            Double sourcePrice = null;
            if (!sourceRow.isNullAt(sourceRow.fieldIndex("price"))) {
                sourcePrice = sourceRow.getDouble(sourceRow.fieldIndex("price"));
            }

            // 2. Charger les candidats
            Dataset<Row> candidates = loadCandidates(sourceCategory, parentAsin);
            if (candidates == null) return Collections.emptyList();

            // 3. Calculer les scores
            Dataset<Row> ratingScores = calculateRatingScores(candidates);
            Dataset<Row> keywordScores = calculateKeywordScores(candidates, sourceRow);
            Dataset<Row> categoryScores = calculateCategoryScores(candidates, sourceRow);
            Dataset<Row> priceScores = calculatePriceScores(candidates, sourcePrice);
            Dataset<Row> ageScores = calculateAgeScores(candidates, sourceRow);

            // 4. Assembler les recommandations
            Dataset<Row> recommendations = candidates
                    .join(ratingScores, "parent_asin")
                    .join(keywordScores, "parent_asin")
                    .join(categoryScores, "parent_asin")
                    .join(priceScores, "parent_asin")
                    .join(ageScores, "parent_asin")
                    .withColumn("final_score",
                            col("rating_score").multiply(0.25)
                                    .plus(col("keyword_score").multiply(0.3))
                                    .plus(col("category_score").multiply(0.2))
                                    .plus(col("price_score").multiply(0.15))
                                    .plus(col("age_score").multiply(0.1))
                    )
                    .filter(
                            col("rating_number").gt(10)
                                    .and(col("final_score").gt(0.35))
                    )
                    .orderBy(col("final_score").desc())
                    .limit(maxRecommendations);

            List<Product> result = extractAndMapResults(recommendations);

            cleanupDatasets(sourceProduct, candidates);

            return result;

        } catch (Exception e) {
            log.error("Error in recommendations for asin {}: {}", parentAsin, e.getMessage(), e);
            throw new RuntimeException("Failed to generate recommendations", e);
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

    private Dataset<Row> loadCandidates(String sourceCategory, String parentAsin) {
        Dataset<Row> candidates = spark.read()
                .format("mongodb")
                .option("uri", "mongodb://localhost:27017")
                .option("database", "amazon_reviews")
                .option("collection", "metadata")
                .load()
                .filter(col("main_category").equalTo(sourceCategory))
                .filter(col("parent_asin").notEqual(parentAsin))
                .select(
                        "parent_asin",
                        "title",
                        "price",
                        "average_rating",
                        "rating_number",
                        "categories",
                        "description",
                        "main_category",
                        "images"
                )
                .repartition(8)
                .cache();

        long count = candidates.count();
        if (count == 0) {
            log.warn("No candidates found for category: {}", sourceCategory);
            return null;
        }
        log.info("Found {} candidates in category {}", count, sourceCategory);
        return candidates;
    }

    private Dataset<Row> calculateRatingScores(Dataset<Row> candidates) {
        return candidates
                .select(
                        col("parent_asin"),
                        when(col("average_rating").isNotNull()
                                        .and(col("rating_number").isNotNull()),
                                col("average_rating").divide(5.0)
                                        .multiply(
                                                when(col("rating_number").gt(100), 1.0)
                                                        .when(col("rating_number").gt(50), 0.9)
                                                        .when(col("rating_number").gt(20), 0.8)
                                                        .otherwise(0.7)
                                        ))
                                .otherwise(0.5)
                                .as("rating_score")
                );
    }

    private Dataset<Row> calculateKeywordScores(Dataset<Row> candidates, Row sourceRow) {
        String sourceTitle = sourceRow.getString(sourceRow.fieldIndex("title")).toLowerCase();

        return candidates
                .withColumn("keyword_score",
                        when(arrays_overlap(
                                split(lower(col("title")), " "),
                                split(lit(sourceTitle), " ")
                        ), lit(1.0))
                                .otherwise(lit(0.5))
                )
                .select("parent_asin", "keyword_score");
    }

    private Dataset<Row> calculateCategoryScores(Dataset<Row> candidates, Row sourceRow) {
        try {
            scala.collection.Seq<String> sourceCategories = null;
            if (!sourceRow.isNullAt(sourceRow.fieldIndex("categories"))) {
                sourceCategories = sourceRow.getSeq(sourceRow.fieldIndex("categories"));
            }

            if (sourceCategories == null || sourceCategories.isEmpty()) {
                return candidates
                        .withColumn("category_score", lit(0.5))
                        .select("parent_asin", "category_score");
            }

            List<String> sourceCategoriesList = scala.collection.JavaConverters.seqAsJavaList(sourceCategories);
            Column sourceCategoriesCol = array(sourceCategoriesList.stream()
                    .map(functions::lit)
                    .toArray(Column[]::new));

            return candidates
                    .withColumn("category_score",
                            when(col("categories").isNull(), 0.5)
                                    .otherwise(
                                            when(size(array_intersect(col("categories"), sourceCategoriesCol)).gt(0),
                                                    size(array_intersect(col("categories"), sourceCategoriesCol))
                                                            .divide(size(sourceCategoriesCol))
                                                            .multiply(0.8)
                                                            .plus(0.2))
                                                    .otherwise(0.2)
                                    ))
                    .select("parent_asin", "category_score");
        } catch (Exception e) {
            log.error("Error calculating category scores: {}", e.getMessage());
            return candidates
                    .withColumn("category_score", lit(0.5))
                    .select("parent_asin", "category_score");
        }
    }

    private Dataset<Row> calculatePriceScores(Dataset<Row> candidates, Double sourcePrice) {
        if (sourcePrice == null) {
            return candidates
                    .withColumn("price_score", lit(0.5))
                    .select("parent_asin", "price_score");
        }

        return candidates
                .withColumn("price_score",
                        when(col("price").isNull(), 0.5)
                                .otherwise(
                                        when(
                                                col("price").leq(lit(sourcePrice))
                                                        .and(col("price").geq(lit(sourcePrice * 0.7)))
                                                        .and(col("average_rating").geq(lit(4.0))), lit(1.0))
                                                .when(
                                                        col("price").geq(lit(sourcePrice))
                                                                .and(col("price").leq(lit(sourcePrice * 1.3))), lit(0.8))
                                                .when(
                                                        col("price").lt(lit(sourcePrice * 0.7))
                                                                .and(col("price").geq(lit(sourcePrice * 0.5)))
                                                                .and(col("average_rating").geq(lit(3.5))), lit(0.7))
                                                .otherwise(lit(0.3))
                                ))
                .select("parent_asin", "price_score");
    }

    private static UserDefinedFunction calculateAgeMatch = udf(
            (String title, scala.collection.Seq<String> description) -> {
                StringBuilder searchText = new StringBuilder();

                if (title != null) {
                    searchText.append(title.toLowerCase()).append(" ");
                }

                if (description != null && !description.isEmpty()) {
                    scala.collection.JavaConverters.seqAsJavaListConverter(description)
                            .asJava()
                            .forEach(desc -> searchText.append(desc.toLowerCase()).append(" "));
                }

                String fullText = searchText.toString();

                if (fullText.contains("0-2") || fullText.contains("baby")) return 1.0;
                if (fullText.contains("2-4") || fullText.contains("toddler")) return 1.0;
                if (fullText.contains("4-8") || fullText.contains("kid")) return 1.0;
                if (fullText.contains("8-12")) return 1.0;
                if (fullText.contains("12+") || fullText.contains("teen")) return 1.0;

                return 0.5;
            }, DataTypes.DoubleType
    );

    private Dataset<Row> calculateAgeScores(Dataset<Row> candidates, Row sourceRow) {
        return candidates
                .withColumn("age_score",
                        callUDF("calculateAgeMatch",
                                col("title"),
                                col("description")))
                .select("parent_asin", "age_score");
    }

    private List<Product> extractAndMapResults(Dataset<Row> recommendations) {
        return recommendations
                .select(
                        "parent_asin",
                        "title",
                        "price",
                        "average_rating",
                        "rating_number",
                        "main_category",
                        "categories",
                        "description",
                        "final_score",
                        "images"
                )
                .collectAsList()
                .stream()
                .map(this::convertRowToProduct)
                .collect(Collectors.toList());
    }

    private void cleanupDatasets(Dataset<Row>... datasets) {
        for (Dataset<Row> dataset : datasets) {
            if (dataset != null) {
                dataset.unpersist();
            }
        }
        System.gc();
    }

    private Product convertRowToProduct(Row row) {
        try {
            Product product = new Product();
            product.setParentAsin(row.getString(row.fieldIndex("parent_asin")));
            product.setTitle(row.getString(row.fieldIndex("title")));
            product.setMainCategory(row.getString(row.fieldIndex("main_category")));

            if (!row.isNullAt(row.fieldIndex("price"))) {
                product.setPrice(row.getDouble(row.fieldIndex("price")));
            }
            if (!row.isNullAt(row.fieldIndex("average_rating"))) {
                product.setAverageRating(row.getDouble(row.fieldIndex("average_rating")));
            }
            if (!row.isNullAt(row.fieldIndex("rating_number"))) {
                product.setRatingNumber(row.getInt(row.fieldIndex("rating_number")));
            }
            if (!row.isNullAt(row.fieldIndex("final_score"))) {
                product.setFinalScore(row.getDouble(row.fieldIndex("final_score")));
            }

            if (!row.isNullAt(row.fieldIndex("categories"))) {
                scala.collection.Seq<String> categoriesSeq = row.getSeq(row.fieldIndex("categories"));
                product.setCategories(scala.collection.JavaConverters.seqAsJavaList(categoriesSeq));
            }

            if (!row.isNullAt(row.fieldIndex("description"))) {
                scala.collection.Seq<String> descSeq = row.getSeq(row.fieldIndex("description"));
                product.setDescription(scala.collection.JavaConverters.seqAsJavaList(descSeq));
            }

            try {
                List<ProductImage> images = getImagesFromRow(row);
                if (!images.isEmpty()) {
                    product.setImages(images);
                }
            } catch (Exception e) {
                log.warn("Error processing images for product {}: {}",
                        product.getTitle(), e.getMessage());
            }

            return product;
        } catch (Exception e) {
            log.error("Error converting row to product: {}", e.getMessage());
            throw new RuntimeException("Failed to convert row to product", e);
        }
    }

    private List<ProductImage> getImagesFromRow(Row row) {
        try {
            if (row.isNullAt(row.fieldIndex("images"))) {
                return Collections.emptyList();
            }

            scala.collection.Seq<Row> imageRows = row.getSeq(row.fieldIndex("images"));
            if (imageRows == null) {
                return Collections.emptyList();
            }

            List<Row> javaImageRows = scala.collection.JavaConverters.seqAsJavaList(imageRows);
            return javaImageRows.stream()
                    .map(imageRow -> {
                        ProductImage image = new ProductImage();

                        image.setThumb(getStringFromImageRow(imageRow, "thumb"));
                        image.setLarge(getStringFromImageRow(imageRow, "large"));
                        image.setHiRes(getStringFromImageRow(imageRow, "hi_res"));
                        image.setVariant(getStringFromImageRow(imageRow, "variant"));

                        return image;
                    })
                    .filter(this::hasDisplayableUrl)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error extracting images from row: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private boolean hasDisplayableUrl(ProductImage image) {
        return image.getLarge() != null ||
                image.getThumb() != null ||
                image.getHiRes() != null;
    }

    private String getStringFromImageRow(Row imageRow, String fieldName) {
        try {
            if (imageRow.isNullAt(imageRow.fieldIndex(fieldName))) {
                return null;
            }
            String value = imageRow.getString(imageRow.fieldIndex(fieldName));
            return (value != null && !value.trim().isEmpty()) ? value.trim() : null;
        } catch (Exception e) {
            return null;
        }
    }

}
