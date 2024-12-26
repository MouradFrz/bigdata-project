package com.example.bigdataback.service;

import com.example.bigdataback.dto.ReviewStatsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewService {

    private final MongoTemplate mongoTemplate;
    public ReviewStatsDTO getReviewStats() {
        // Create a projection to calculate verified purchases using $cond
        ProjectionOperation projectionOperation = Aggregation.project()
                .and(ConditionalOperators.when(Criteria.where("verified_purchase").is(true))
                        .then(1)
                        .otherwise(0))
                .as("verifiedPurchaseCount");

        // Group and aggregate the results
        Aggregation aggregation = Aggregation.newAggregation(
                projectionOperation,
                Aggregation.group()
                        .count().as("totalReviews")
                        .sum("verifiedPurchaseCount").as("verifiedPurchases")
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(aggregation, "reviews", Map.class);
        Map<String, Object> result = results.getUniqueMappedResult();

        long totalReviews = result != null && result.get("totalReviews") != null
                ? ((Number) result.get("totalReviews")).longValue()
                : 0L;

        long verifiedPurchases = result != null && result.get("verifiedPurchases") != null
                ? ((Number) result.get("verifiedPurchases")).longValue()
                : 0L;

        return new ReviewStatsDTO(totalReviews, verifiedPurchases);
    }
    public Map<String, Long> getReviewCounts() {
        // Count total reviews
        long totalReviews = mongoTemplate.count(new Query(), "reviews");

        // Count verified purchases
        long verifiedPurchases = mongoTemplate.count(
                new Query(Criteria.where("verified_purchase").is(true)),
                "reviews"
        );

        // Return the results as a map
        Map<String, Long> result = new HashMap<>();
        result.put("totalReviews", totalReviews);
        result.put("verifiedPurchases", verifiedPurchases);

        return result;
    }
}
