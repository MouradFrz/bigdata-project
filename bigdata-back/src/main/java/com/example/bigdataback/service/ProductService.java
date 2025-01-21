package com.example.bigdataback.service;

import com.example.bigdataback.dto.*;
import com.example.bigdataback.entity.Product;
import com.example.bigdataback.entity.Review;
import com.example.bigdataback.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
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
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.spark.sql.functions.col;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final MongoTemplate mongoTemplate;
    private final ProductRepository productRepository;

    private final SparkSession sparkSession;


    public Page<Product> findByParsedQuery(UserRequest userRequest, Document query) {
        PageRequest pageRequest = PageRequest.of(userRequest.getPage(), userRequest.getSize());

        Long startTimeMongo = System.currentTimeMillis();
        List<Product> products = this.productRepository.findByParsedQuery(query, pageRequest);
        Long endTimeMongo = System.currentTimeMillis();
        log.info("Total time taken by mongo: {} s", (endTimeMongo - startTimeMongo) / 1000);

        return new PageImpl<>(products, pageRequest, products.size());
    }
    
    public String getProductCategory(String parentAsin) {
        try {
            Dataset<Row> product = sparkSession.read()
                    .format("mongodb")
                    .option("uri", "mongodb://localhost:27017")
                    .option("database", "amazon_reviews")
                    .option("collection", "metadata")
                    .load()
                    .filter(col("parent_asin").equalTo(parentAsin))
                    .filter(col("main_category").isin("Movies & TV", "Toys & Games", "Books"))
                    .select("main_category");

            if (product.count() == 0) {
                log.warn("No product found for parentAsin: {}", parentAsin);
                return null;
            }

            return product.first().getString(0);
        } catch (Exception e) {
            log.error("Error getting product category: {}", e.getMessage());
            return null;
        }
    }

    public List<ProductSummary> getTopRatedProductsByCategory(String mainCategory) {
        // Étape 1: Calculer la moyenne globale des notes pour la catégorie sélectionnée
        Aggregation avgAggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("main_category").is(mainCategory)),
                Aggregation.group().avg("average_rating").as("globalAverage")
        );

        // Exécuter l'agrégation pour obtenir la moyenne globale
        AggregationResults<Document> avgResult = mongoTemplate.aggregate(avgAggregation, "metadata", Document.class);
        double globalAverage = avgResult.getUniqueMappedResult() != null ?
                avgResult.getUniqueMappedResult().getDouble("globalAverage") :
                3.5; // Valeur par défaut si aucune donnée

        // Seuil minimal de votes pour être pertinent (ex. 5000 avis)
        int C = 5000;

        // Étape 2: Récupérer tous les produits de la catégorie
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(
                        Criteria.where("main_category").is(mainCategory)
                                .and("rating_number").exists(true)
                                .and("average_rating").exists(true)
                ),
                // Trier par average_rating descendant avant de calculer le score
                Aggregation.sort(Sort.by(Sort.Direction.DESC, "average_rating")),
                // S'assurer que les noms des produits sont uniques
                Aggregation.group("title")
                        .first("_id").as("id")
                        .first("title").as("title")
                        .first("average_rating").as("average_rating")
                        .first("rating_number").as("rating_number")
        );

        List<Document> productDocs = mongoTemplate.aggregate(aggregation, "metadata", Document.class).getMappedResults();

        // Étape 3: Appliquer la pondération bayésienne et trier les produits en conséquence
        return productDocs.stream()
                .map(doc -> {
                    double R = doc.getDouble("average_rating");
                    int v = doc.getInteger("rating_number", 0);

                    // Calcul du score bayésien
                    double W = ((R * v) + (C * globalAverage)) / (v + C);

                    return new ProductSummary(
                            doc.getObjectId("id").toHexString(),
                            doc.getString("title"),
                            W, // Utilisation du score bayésien au lieu du average_rating brut
                            v
                    );
                })
                .sorted(Comparator.comparing(ProductSummary::getAverageRating).reversed()) // Trier selon W
                .limit(10) // Limiter aux 10 meilleurs
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
