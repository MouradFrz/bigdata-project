package com.example.bigdataback.service;

import com.example.bigdataback.entity.Product;
import com.example.bigdataback.entity.Review;
import com.example.bigdataback.repository.ProductRepository;
import com.example.bigdataback.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductDetailService {
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final BookRecommendationService bookRecommendationService;

    private final ToysGamesRecommendationService toysGamesRecommendationService;

    public Map<String, Object> getProductDetailsWithRecommendations(
            String parentAsin,
            Boolean verifiedOnly,
            Integer maxRecommendations) {

        // 1. Récupérer le produit principal
        Product product = productRepository.findByParentAsin(parentAsin)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // 2. Récupérer les reviews
        List<Review> reviews = verifiedOnly ?
                reviewRepository.findByParentAsinAndVerifiedPurchase(parentAsin, true) :
                reviewRepository.findByParentAsin(parentAsin);

        // 3. Obtenir les recommandations selon la catégorie
        List<Product> recommendations = getRecommendationsByCategory(product, maxRecommendations);

        // 4. send response
        Map<String, Object> response = new HashMap<>();
        response.put("product", product);
        response.put("reviews", reviews);
        response.put("recommendations", recommendations);

        return response;
    }

    private List<Product> getRecommendationsByCategory(Product product, Integer maxRecommendations) {
        String category = product.getMainCategory().toLowerCase();

        return switch (category) {
            case "books" -> bookRecommendationService.getBookRecommendations(product, maxRecommendations); //not functional
            case "toys & games" -> toysGamesRecommendationService.getRecommendations(product.getParentAsin(), maxRecommendations);
            case "movies & tv" -> bookRecommendationService.getBookRecommendations(product, maxRecommendations); //not functional
            default -> bookRecommendationService.getBookRecommendations(product, maxRecommendations);
        };
    }


}
