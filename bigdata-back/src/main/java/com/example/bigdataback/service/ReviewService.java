package com.example.bigdataback.service;

import com.example.bigdataback.dto.ReviewStatsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.bson.Document;
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

    public long countReviewsByParentAsin(String parentAsin) {
        // ✅ Construct the query to count reviews for the given parent_asin
        Query query = new Query(Criteria.where("parent_asin").is(parentAsin));

        // ✅ Execute the count query in MongoDB
        return mongoTemplate.count(query, "reviews");
    }


}
