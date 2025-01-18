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
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import static org.apache.spark.sql.functions.col;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final MongoTemplate mongoTemplate;
    private final ProductRepository productRepository;

    private final SparkSession sparkSession;


    public Page<Product> findByParsedQuery(UserRequest userRequest, Document query) {
        PageRequest pageRequest = PageRequest.of(userRequest.getPage(), userRequest.getSize());
        List<Product> products = this.productRepository.findByParsedQuery(query, pageRequest);
        return new PageImpl<>(products, pageRequest, products.size());
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
    public String getProductCategory(String parentAsin) {
        try {
            Dataset<Row> product = sparkSession.read()
                    .format("mongodb")
                    .option("uri", "mongodb://localhost:27017")
                    .option("database", "amazon_reviews")
                    .option("collection", "metadata")
                    .load()  // D'abord charger les données
                    .filter(col("parent_asin").equalTo(parentAsin))
                    .filter(col("main_category").isin("Movies & TV", "Toys & Games"))
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



}
