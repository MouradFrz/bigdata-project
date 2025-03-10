package com.example.bigdataback.service;

import com.example.bigdataback.entity.Product;
import com.example.bigdataback.entity.Review;
import com.example.bigdataback.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.spark.sql.functions.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductDetailService {
    private final SparkSession spark;
    private final ProductRepository productRepository;

    public Map<String, Object> getProductDetailsWithReviews(String parentAsin, Boolean verifiedOnly) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 1. Vérifier d'abord si le produit existe
            Product product = productRepository.findByParentAsin(parentAsin)
                    .orElseThrow(() -> new RuntimeException("PRODUCT_NOT_FOUND: " + parentAsin));

            // 2. Récupérer les reviews avec Spark + MongoDB
            Dataset<Row> reviewsDF = spark.read()
                    .format("mongodb")
                    .option("uri", "mongodb://localhost:27017")
                    .option("database", "amazon_reviews")
                    .option("collection", "reviews")
                    .load();

            log.info("Number of reviews found: {}", reviewsDF.count());

            List<Review> reviews = new ArrayList<>();
            Map<String, Object> stats = new HashMap<>();
            reviewsDF = reviewsDF.filter(col("parent_asin").equalTo(parentAsin));

            if (verifiedOnly) {
                reviewsDF = reviewsDF.filter(col("verified_purchase").equalTo(true));
            }

            if (!reviewsDF.isEmpty()) {
                // 3. Calculer les statistiques
                Dataset<Row> statsDF = reviewsDF.agg(
                        count("*").as("total_reviews"),
                        avg("rating").as("average_rating"),
                        sum(when(col("verified_purchase").equalTo(true), 1).otherwise(0))
                                .as("verified_reviews")
                );

                // 4. Récupérer les 50 reviews les plus récentes
                reviews = reviewsDF
                        .orderBy(col("timestamp").desc())
                        .limit(50)
                        .collectAsList()
                        .stream()
                        .map(this::convertRowToReview)
                        .collect(Collectors.toList());

                // 5. Récupérer les stats
                Row statsRow = statsDF.first();
                stats = Map.of(
                        "totalReviews", statsRow.getLong(0),
                        "averageRating", statsRow.getDouble(1),
                        "verifiedReviews", statsRow.getLong(2)
                );
            }

            // 6. Construire la réponse finale
            response.put("product", product);
            response.put("reviews", reviews);
            response.put("stats", stats);

            return response;

        } catch (Exception e) {
            log.error("Error getting product details for asin {}: {}", parentAsin, e.getMessage());
            throw e;
        }
    }

    private Review convertRowToReview(Row row) {
        try {
            int idIdx = row.fieldIndex("_id");
            int ratingIdx = row.fieldIndex("rating");
            int titleIdx = row.fieldIndex("title");
            int textIdx = row.fieldIndex("text");
            int asinIdx = row.fieldIndex("asin");
            int parentAsinIdx = row.fieldIndex("parent_asin");
            int userIdIdx = row.fieldIndex("user_id");
            int timestampIdx = row.fieldIndex("timestamp");
            int helpfulVoteIdx = row.fieldIndex("helpful_vote");
            int verifiedPurchaseIdx = row.fieldIndex("verified_purchase");

            Review review = new Review();
            if (!row.isNullAt(idIdx)) {
                Object idValue = row.get(idIdx);
                if (idValue instanceof String) {
                    review.setId(new ObjectId((String) idValue));
                } else if (idValue instanceof Row) {
                    Row idStruct = (Row) idValue;
                    if (!idStruct.isNullAt(0)) {
                        review.setId(new ObjectId(idStruct.getString(0)));
                    }
                }
            }
            if (row.isNullAt(ratingIdx)) {
                review.setRating(0);
            } else {
                Object ratingValue = row.get(ratingIdx);
                if (ratingValue instanceof Integer) {
                    review.setRating((Integer) ratingValue);
                } else if (ratingValue instanceof Double) {
                    review.setRating((int) ((Double) ratingValue).doubleValue());
                }
            }

            review.setTitle(row.getString(titleIdx));
            review.setText(row.getString(textIdx));
            review.setAsin(row.getString(asinIdx));
            review.setParentAsin(row.getString(parentAsinIdx));
            review.setUserId(row.getString(userIdIdx));
            review.setTimestamp(row.getLong(timestampIdx));

            if (row.isNullAt(helpfulVoteIdx)) {
                review.setHelpfulVote(0);
            } else {
                Object helpfulValue = row.get(helpfulVoteIdx);
                if (helpfulValue instanceof Integer) {
                    review.setHelpfulVote((Integer) helpfulValue);
                } else if (helpfulValue instanceof Double) {
                    review.setHelpfulVote((int) ((Double) helpfulValue).doubleValue());
                }
            }

            review.setVerifiedPurchase(row.getBoolean(verifiedPurchaseIdx));

            return review;
        } catch (Exception e) {
            log.error("Error converting row to review: {}", e.getMessage());
            throw new RuntimeException("Failed to convert row to review", e);
        }
    }
}
