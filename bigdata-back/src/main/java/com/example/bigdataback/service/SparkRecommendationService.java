package com.example.bigdataback.service;

import com.example.bigdataback.entity.Product;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
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

    public List<Product> getSparkRecommendations(String parentAsin, Integer maxRecommendations) {
        try {
            log.info("Starting Spark recommendations for parentAsin: {}", parentAsin);

            // 1. Charger et filtrer les données source avec sélection minimale
            Dataset<Row> sourceProduct = spark.read()
                    .format("mongodb")
                    .option("uri", "mongodb://localhost:27017")
                    .option("database", "amazon_reviews")
                    .option("collection", "metadata")
                    .load()
                    .select("parent_asin", "main_category")
                    .filter(col("parent_asin").equalTo(parentAsin))
                    .cache();

            if (sourceProduct.count() == 0) {
                return Collections.emptyList();
            }

            String sourceCategory = sourceProduct.first().getString(
                    sourceProduct.schema().fieldIndex("main_category")
            );

            // 2. Charger et filtrer les produits
            Dataset<Row> filteredProducts = spark.read()
                    .format("mongodb")
                    .option("uri", "mongodb://localhost:27017")
                    .option("database", "amazon_reviews")
                    .option("collection", "metadata")
                    .load()
                    .select("parent_asin", "title", "price", "average_rating", "main_category")
                    .filter(col("main_category").equalTo(sourceCategory))
                    .filter(col("parent_asin").notEqual(parentAsin))
                    .cache();

            // 3. Charger et filtrer les reviews
            Dataset<Row> filteredReviews = spark.read()
                    .format("mongodb")
                    .option("uri", "mongodb://localhost:27017")
                    .option("database", "amazon_reviews")
                    .option("collection", "reviews")
                    .load()
                    .select("parent_asin", "rating", "verified_purchase")
                    .cache();

            // 4. Calculer les recommandations
            Dataset<Row> recommendations = filteredProducts
                    .join(filteredReviews, "parent_asin")
                    .groupBy(
                            col("parent_asin"),
                            col("title").as("product_title"),
                            col("price"),
                            col("average_rating"),
                            col("main_category")
                    )
                    .agg(
                            avg("rating").as("avg_rating"),
                            count("*").as("review_count"),
                            sum(when(col("verified_purchase"), 1).otherwise(0))
                                    .as("verified_reviews_count")
                    )
                    .withColumn("score",
                            col("avg_rating").multiply(0.4)
                                    .plus(col("review_count").divide(100).multiply(0.3))
                                    .plus(col("verified_reviews_count").divide(col("review_count")).multiply(0.3))
                    )
                    .orderBy(col("score").desc())
                    .limit(maxRecommendations);

            List<Product> result = recommendations.collectAsList().stream()
                    .map(this::convertRowToProduct)
                    .collect(Collectors.toList());

            // 5. Nettoyer le cache
            filteredProducts.unpersist();
            filteredReviews.unpersist();
            sourceProduct.unpersist();

            return result;

        } catch (Exception e) {
            log.error("Error in Spark recommendations: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate Spark recommendations", e);
        }
    }

    private Product convertRowToProduct(Row row) {
        Product product = new Product();
        product.setParentAsin(row.getString(row.fieldIndex("parent_asin")));
        product.setTitle(row.getString(row.fieldIndex("product_title")));
        product.setPrice(row.getDouble(row.fieldIndex("price")));
        product.setAverageRating(row.getDouble(row.fieldIndex("average_rating")));
        product.setMainCategory(row.getString(row.fieldIndex("main_category")));
        return product;
    }

    @PreDestroy
    public void cleanup() {
        if (spark != null) {
            spark.close();
        }
    }
}
