package com.example.bigdataback.controller;

import com.example.bigdataback.dto.ErrorResponse;
import com.example.bigdataback.dto.ProductSummary;
import com.example.bigdataback.dto.UserRequest;
import com.example.bigdataback.entity.Product;
import com.example.bigdataback.ollama.OllamaService;
import com.example.bigdataback.parser.QueryParser;
import com.example.bigdataback.service.ProductDetailService;
import com.example.bigdataback.service.ProductService;
import com.example.bigdataback.service.SparkRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bson.Document;
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

    @PostMapping
    public ResponseEntity<?> processingUserRequest(@RequestBody UserRequest userRequest) {
        log.info("Received user request.......... {}", userRequest.getRequest());

        if(userRequest.getRequest().isEmpty()) {
            log.info("User request is empty.......... {}", userRequest.getRequest());
            return ResponseEntity.ok(productService.findByParsedQuery(userRequest, new Document()));
        } else {
            log.info("User request is not empty.......... {}", userRequest.getRequest());
            Document query = QueryParser.parseQuery(userRequest.getRequest());

            if (!query.isEmpty()) {
                log.info("Parsed query.........{}", query.toJson());
                return ResponseEntity.ok(productService.findByParsedQuery(userRequest, query));
            } else {
                log.info("Can't parse the request.......... {}", userRequest.getRequest());
                userRequest.setRequest(ollamaService.refactorUserRequest(userRequest.getRequest()));
                Document queryOllama = QueryParser.parseQuery(userRequest.getRequest());
                log.info("Parsed query after ollama process.........{}", queryOllama.toJson());
                return ResponseEntity.ok(productService.findByParsedQuery(userRequest, queryOllama));
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
            List<Product> recommendations = sparkRecommendationService.getSparkRecommendations(
                    parentAsin,
                    maxRecommendations
            );
            long endTime = System.currentTimeMillis();

            Map<String, Object> response = new HashMap<>();
            response.put("recommendations", recommendations);
            response.put("executionTime", endTime - startTime);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in spark recommendations: ", e);
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
    public ResponseEntity<List<ProductSummary>> getTopRatedProducts() {
        return ResponseEntity.ok(productService.getTopRatedProducts());
    }
}
