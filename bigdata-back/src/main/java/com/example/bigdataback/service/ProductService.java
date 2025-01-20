package com.example.bigdataback.service;

import com.example.bigdataback.dto.UserRequest;
import com.example.bigdataback.dto.VerifiedReviewComparisonDTO;
import com.example.bigdataback.dto.PriceDistributionDTO;
import com.example.bigdataback.dto.ProductSummary;
import com.example.bigdataback.dto.RatingDistributionDTO;
import com.example.bigdataback.dto.ReviewHelpfulnessDTO;
import com.example.bigdataback.dto.ReviewTimelineDTO;
import com.example.bigdataback.entity.Product;
import com.example.bigdataback.entity.Review;
import com.example.bigdataback.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationExpression;
import org.springframework.data.mongodb.core.aggregation.AggregationOperationContext;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final MongoTemplate mongoTemplate;
    private final ProductRepository productRepository;

    public Page<Product> findByParsedQuery(UserRequest userRequest, Document query) {
        PageRequest pageRequest = PageRequest.of(userRequest.getPage(), userRequest.getSize());
        List<Product> products = this.productRepository.findByParsedQuery(query, pageRequest);
        return new PageImpl<>(products, pageRequest, products.size());
    }

    public List<ProductSummary> getTopRatedProducts() {
        Query query = new Query()
                .addCriteria(Criteria.where("rating_number").gt(65000)) // Filtre : rating_number > 2500
                .with(Sort.by(Sort.Direction.DESC, "average_rating")) // Tri par average_rating décroissant
                .limit(10); // Limite à 10 résultats

        List<Product> products = mongoTemplate.find(query, Product.class);

        // Transformation en DTO
        return products.stream()
                .map(product -> new ProductSummary(
                        product.getId().toHexString(), // Conversion ObjectId en String
                        product.getTitle(),
                        product.getAverageRating(),
                        product.getRatingNumber()
                ))
                .collect(Collectors.toList());
    }

    public List<RatingDistributionDTO> getRatingDistribution() {
        TypedAggregation<Product> aggregation = Aggregation.newAggregation(
            Product.class,
            Aggregation.group("average_rating")
                .count().as("count"),
            Aggregation.project()
                .and("_id").as("rating")
                .and("count").as("count"),
            Aggregation.sort(Sort.Direction.ASC, "rating")
        );

        AggregationResults<RatingDistributionDTO> results = 
            mongoTemplate.aggregate(aggregation, RatingDistributionDTO.class);
        return results.getMappedResults();
    }


    // 3. Review Timeline Analysis
    
    public List<ReviewTimelineDTO> getReviewTimeline() {
        TypedAggregation<Review> aggregation = Aggregation.newAggregation(
            Review.class,
            Aggregation.project()
                .and(context -> new Document("$toDate", "$timestamp")).as("date")
                .and("rating").as("rating"),
            Aggregation.project()
                .and(context -> new Document("$dateToString", 
                    new Document("format", "%Y-%m-01")
                        .append("date", "$date")
                )).as("monthlyTimestamp")
                .and("rating").as("rating"),
            Aggregation.group("monthlyTimestamp")
                .count().as("reviewCount")
                .avg("rating").as("averageRating"),
            Aggregation.project()
                .and(context -> new Document("$dateFromString", 
                    new Document("dateString", "$_id")
                )).as("timestamp")
                .and("reviewCount").as("reviewCount")
                .and("averageRating").as("averageRating"),
            Aggregation.sort(Sort.Direction.ASC, "timestamp")
        );

        AggregationResults<ReviewTimelineDTO> results = 
            mongoTemplate.aggregate(aggregation, "reviews", ReviewTimelineDTO.class);
        return results.getMappedResults();
    }

    public List<VerifiedReviewComparisonDTO> getVerifiedVsNonVerifiedComparison() {
        TypedAggregation<Review> aggregation = Aggregation.newAggregation(
            Review.class,
            Aggregation.project()
                .and(context -> new Document("$toDate", "$timestamp")).as("date")
                .and("rating").as("rating")
                .and("verified_purchase").as("verified_purchase"),
            Aggregation.project()
                .and(context -> new Document("$dateToString", 
                    new Document("format", "%Y-%m-01")
                        .append("date", "$date")
                )).as("monthlyTimestamp")
                .and("rating").as("rating")
                .and("verified_purchase").as("verified_purchase"),
            Aggregation.group("monthlyTimestamp")
                .avg(context -> new Document("$cond", Arrays.asList(
                    new Document("$eq", Arrays.asList("$verified_purchase", true)),
                    "$rating",
                    null
                ))).as("verifiedRating")
                .avg(context -> new Document("$cond", Arrays.asList(
                    new Document("$eq", Arrays.asList("$verified_purchase", false)),
                    "$rating",
                    null
                ))).as("nonVerifiedRating")
                .sum(context -> new Document("$cond", Arrays.asList(
                    new Document("$eq", Arrays.asList("$verified_purchase", true)),
                    1,
                    0
                ))).as("verifiedCount")
                .sum(context -> new Document("$cond", Arrays.asList(
                    new Document("$eq", Arrays.asList("$verified_purchase", false)),
                    1,
                    0
                ))).as("nonVerifiedCount"),
            Aggregation.project()
                .and("_id").as("month")
                .and("verifiedRating").as("verifiedRating")
                .and("nonVerifiedRating").as("nonVerifiedRating")
                .and("verifiedCount").as("verifiedCount")
                .and("nonVerifiedCount").as("nonVerifiedCount"),
            Aggregation.sort(Sort.Direction.ASC, "month")
        );

        AggregationResults<VerifiedReviewComparisonDTO> results =
            mongoTemplate.aggregate(aggregation, "reviews", VerifiedReviewComparisonDTO.class);
        return results.getMappedResults();
    }


    public List<PriceDistributionDTO> getPriceDistribution() {
        TypedAggregation<Product> aggregation = Aggregation.newAggregation(
            Product.class,
            Aggregation.match(Criteria.where("price").exists(true).ne(null)),
            Aggregation.group("main_category")
                .min("price").as("minPrice")
                .max("price").as("maxPrice")
                .avg("price").as("avgPrice")
                .count().as("productCount")
                // For median, we'll use the average as an approximation since true median requires more complex operations
                .avg("price").as("medianPrice"),
            Aggregation.project()
                .and("_id").as("category")
                .and("minPrice").as("minPrice")
                .and("maxPrice").as("maxPrice")
                .and("avgPrice").as("avgPrice")
                .and("medianPrice").as("medianPrice")
                .and("productCount").as("productCount"),
            Aggregation.sort(Sort.Direction.DESC, "productCount")
        );

        AggregationResults<PriceDistributionDTO> results =
            mongoTemplate.aggregate(aggregation, "metadata", PriceDistributionDTO.class);
        return results.getMappedResults();
    }

    public List<ReviewHelpfulnessDTO> getReviewHelpfulnessAnalysis() {
        TypedAggregation<Review> aggregation = Aggregation.newAggregation(
            Review.class,
            Aggregation.match(Criteria.where("helpful_vote").exists(true)),
            Aggregation.group("rating")
                .avg("helpful_vote").as("averageHelpfulVotes")
                .count().as("reviewCount"),
            Aggregation.project()
                .and("_id").as("rating")
                .and("averageHelpfulVotes").as("averageHelpfulVotes")
                .and("reviewCount").as("reviewCount"),
            Aggregation.sort(Sort.Direction.ASC, "rating")
        );

        AggregationResults<ReviewHelpfulnessDTO> results =
            mongoTemplate.aggregate(aggregation, "reviews", ReviewHelpfulnessDTO.class);
        return results.getMappedResults();
    }

}
