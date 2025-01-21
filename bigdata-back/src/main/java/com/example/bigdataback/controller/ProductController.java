package com.example.bigdataback.controller;

import com.example.bigdataback.dto.*;
import com.example.bigdataback.entity.Product;
import com.example.bigdataback.ollama.OllamaService;
import com.example.bigdataback.parser.DocumentValidator;
import com.example.bigdataback.parser.QueryParser;
import com.example.bigdataback.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping(value = "/products")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    private final ProductDetailService productDetailService;

    private final SparkRecommendationService sparkRecommendationService;

    private final OllamaService ollamaService;

    private final MovieRecommendationService movieRecommendationService;

    private final BookRecommendationService bookRecommendationService;

    @PostMapping
    public ResponseEntity<?> processingUserRequest(@RequestBody UserRequest userRequest) {
        Long startTime = System.currentTimeMillis();

        log.info("Received user request.......... {}", userRequest.getRequest());

        if (userRequest.getRequest().isEmpty()) {
            log.info("User request is empty.......... {}", userRequest.getRequest());
            Page<Product> products = productService.findByParsedQuery(userRequest, new Document());
            Long endTime = System.currentTimeMillis();
            log.info("Total time taken to process the request: {} s", (endTime - startTime) / 1000);
            return ResponseEntity.ok(products);
        } else {

            log.info("User request is not empty.......... {}", userRequest.getRequest());

            Document query = QueryParser.parseQuery(userRequest.getRequest());
            log.info("Parsed query 1 .........{}", query.toJson());

            if (query.isEmpty() || DocumentValidator.containsInvalidParts(query)) {
                log.info("Parsed query contains invalid parts..........");
                Long startTimeOllama = System.currentTimeMillis();
                userRequest.setRequest(ollamaService.refactorUserRequest(userRequest.getRequest()));
                Long endTimeOllama = System.currentTimeMillis();
                log.info("Total time taken by ollama: {} s", (endTimeOllama - startTimeOllama) / 1000);
                Document queryOllama = QueryParser.parseQuery(userRequest.getRequest());
                log.info("Parsed query after ollama process .........{}", queryOllama.toJson());
                Page<Product> products = productService.findByParsedQuery(userRequest, queryOllama);
                Long endTime = System.currentTimeMillis();
                log.info("Total time taken to process the request: {} s", (endTime - startTime) / 1000);
                return ResponseEntity.ok(products);
            } else {
                Document query1 = QueryParser.parseQuery(userRequest.getRequest());
                log.info("Parsed query without ollama .........{}", query1.toJson());

                Page<Product> products = productService.findByParsedQuery(userRequest, query1);
                Long endTime = System.currentTimeMillis();
                log.info("Total time taken to process the request: {} s", (endTime - startTime) / 1000);
                return ResponseEntity.ok(products);
            }
        }
    }


    @GetMapping("/{parentAsin}")
    public ResponseEntity<?> getProductDetails(
            @PathVariable String parentAsin,
            @RequestParam(required = false, defaultValue = "false") Boolean verifiedOnly) {
        try {
            Map<String, Object> response = productDetailService.getProductDetailsWithReviews(
                    parentAsin,
                    verifiedOnly
            );
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.startsWith("PRODUCT_NOT_FOUND:")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ErrorResponse.builder()
                                .code(HttpStatus.NOT_FOUND.value())
                                .message("Le produit n'existe pas")
                                .build()
                );
            } else if (errorMessage.startsWith("NO_REVIEWS_FOUND:")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ErrorResponse.builder()
                                .code(HttpStatus.NOT_FOUND.value())
                                .message("Le produit existe mais n'a pas de reviews")
                                .build()
                );
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        ErrorResponse.builder()
                                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .message("Erreur technique lors de la récupération des détails du produit")
                                .build()
                );
            }
        }
    }

    @GetMapping("/{parentAsin}/spark-recommendations")
    public ResponseEntity<?> getSparkRecommendations(
            @PathVariable String parentAsin,
            @RequestParam(required = false, defaultValue = "5") Integer maxRecommendations) {
        try {
            long startTime = System.currentTimeMillis();

            String category = productService.getProductCategory(parentAsin);
            log.info("catégrie trouvée {}",category);
            if (category == null) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Product not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            // Obtenir les recommandations selon la catégorie
            List<Product> recommendations;
            if (category.equals("Movies & TV")) {
                recommendations = movieRecommendationService.getMovieRecommendations(
                        parentAsin,
                        maxRecommendations
                );
            } else if (category.equals("Toys & Games")) {
                recommendations = sparkRecommendationService.getSparkRecommendations(
                        parentAsin,
                        maxRecommendations
                );
            } else if (category.equals("Books")) {
                recommendations = bookRecommendationService.getBookRecommendations(
                        parentAsin,
                        maxRecommendations
                );
            }else {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Unsupported category: " + category);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            long endTime = System.currentTimeMillis();

            Map<String, Object> response = new HashMap<>();
            response.put("recommendations", recommendations);
            response.put("executionTime", endTime - startTime);
            response.put("category", category);
            response.put("count", recommendations.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error in recommendations for asin {}: ", parentAsin, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("stackTrace", ExceptionUtils.getStackTrace(e));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /*
        This end point return the top 10 rated products in a categorie.Each product must have more than 60000 review to be considered in the rating.
        Graph type: Histograme

        Each bar will represent a product,the height will be based on the rating number and on the average_rating
     */
    @GetMapping("/top-rated")

    public ResponseEntity<List<ProductSummary>> getTopRatedProductsByCategory(@RequestParam String mainCategory
    ) {
        return ResponseEntity.ok(productService.getTopRatedProductsByCategory(mainCategory));

    }
 
    
    @GetMapping("/rating-distribution")
    public ResponseEntity<List<RatingDistributionDTO>> getRatingDistribution() {
        return ResponseEntity.ok(productService.getRatingDistribution());
    }


    @GetMapping("/review-timeline")
    public ResponseEntity<List<ReviewTimelineDTO>> getReviewTimeline() {
        return ResponseEntity.ok(productService.getReviewTimeline());
    }

    @GetMapping("/verified-vs-nonverified")
    public ResponseEntity<List<VerifiedReviewComparisonDTO>> getVerifiedVsNonVerifiedComparison() {
        return ResponseEntity.ok(productService.getVerifiedVsNonVerifiedComparison());
    }

    @GetMapping("/price-distribution")
    public ResponseEntity<List<PriceDistributionDTO>> getPriceDistribution() {
        return ResponseEntity.ok(productService.getPriceDistribution());
    }

    @GetMapping("/review-helpfulness")
    public ResponseEntity<List<ReviewHelpfulnessDTO>> getReviewHelpfulnessAnalysis() {
        return ResponseEntity.ok(productService.getReviewHelpfulnessAnalysis());
    }


}
