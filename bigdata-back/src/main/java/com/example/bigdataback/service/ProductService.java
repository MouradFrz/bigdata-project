package com.example.bigdataback.service;

import com.example.bigdataback.dto.UserRequest;
import com.example.bigdataback.dto.CategoryStatsDTO;
import com.example.bigdataback.dto.ProductSummary;
import com.example.bigdataback.dto.RatingDistributionDTO;
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
        // 2. Category Performance Analysis
    public List<CategoryStatsDTO> getCategoryStats() {
        TypedAggregation<Product> aggregation = Aggregation.newAggregation(
            Product.class,
            Aggregation.group("main_category")
                .avg("average_rating").as("averageRating")
                .count().as("totalProducts"),
            Aggregation.project()
                .and("_id").as("mainCategory")
                .and("averageRating").as("averageRating")
                .and("totalProducts").as("totalProducts"),
            Aggregation.sort(Sort.Direction.DESC, "averageRating")
        );

        AggregationResults<CategoryStatsDTO> results = 
            mongoTemplate.aggregate(aggregation, CategoryStatsDTO.class);
        return results.getMappedResults();
    }

    // 3. Review Timeline Analysis
    
    public List<ReviewTimelineDTO> getReviewTimeline() {
        TypedAggregation<Review> aggregation = Aggregation.newAggregation(
            Review.class,
            Aggregation.project()
                .and(context -> new Document("$subtract", Arrays.asList(
                    "$timestamp",
                    new Document("$mod", Arrays.asList("$timestamp", 86400000L))
                ))).as("dailyTimestamp")
                .and("rating").as("rating"),
            Aggregation.group("dailyTimestamp")
                .count().as("reviewCount")
                .avg("rating").as("averageRating"),
            Aggregation.project()
                .and("_id").as("timestamp")
                .and("reviewCount").as("reviewCount")
                .and("averageRating").as("averageRating"),
            Aggregation.sort(Sort.Direction.ASC, "timestamp")
        );

        AggregationResults<ReviewTimelineDTO> results = 
            mongoTemplate.aggregate(aggregation, "reviews", ReviewTimelineDTO.class);
        return results.getMappedResults();
    }


}
