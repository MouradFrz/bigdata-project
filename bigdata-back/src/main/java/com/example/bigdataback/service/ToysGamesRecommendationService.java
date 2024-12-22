package com.example.bigdataback.service;


import com.example.bigdataback.entity.Product;
import com.example.bigdataback.entity.Review;
import com.example.bigdataback.repository.ProductRepository;
import com.example.bigdataback.repository.ReviewRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class ToysGamesRecommendationService {

    private static final int MAX_CANDIDATES = 50;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;

    // Constantes pour la catégorisation
    private static final Map<String, List<String>> TOY_CATEGORIES = Map.of(
            "educational", List.of("learning", "educational", "stem", "science", "math"),
            "creative", List.of("art", "craft", "building", "construction"),
            "games", List.of("board game", "card game", "puzzle", "strategy"),
            "action", List.of("action figure", "vehicle", "robot"),
            "outdoor", List.of("sports", "outdoor", "active")
    );

    // Poids des différents critères
    private static final Map<String, Double> WEIGHTS = Map.of(
            "review_sentiment", 0.25,    // Reviews et ratings
            "age_match", 0.20,          // Tranche d'âge
            "toy_type", 0.15,           // Type de jouet/jeu
            "price", 0.05               // Prix
    );

    public List<Product> getRecommendations(String parentAsin, Integer maxRecommendations) {
        try {
            // 1. Récupérer le produit source
            Product sourceProduct = productRepository.findByParentAsin(parentAsin)
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            // 2. Récupérer les candidats
            List<Product> candidates = productRepository.findSimilarProducts(
                    sourceProduct.getMainCategory(),
                    parentAsin,
                    PageRequest.of(0, MAX_CANDIDATES)
            );

            // 3. Récupérer toutes les reviews
            List<String> allAsins = new ArrayList<>();
            allAsins.add(parentAsin);
            allAsins.addAll(candidates.stream()
                    .map(Product::getParentAsin)
                    .collect(Collectors.toList()));

            Map<String, List<Review>> reviewsByAsin = reviewRepository
                    .findByParentAsinIn(allAsins)
                    .stream()
                    .collect(Collectors.groupingBy(Review::getParentAsin));

            // 4. Calculer les scores
            return candidates.stream()
                    .map(candidate -> {
                        double score = calculateScore(
                                sourceProduct,
                                candidate,
                                reviewsByAsin.getOrDefault(parentAsin, Collections.emptyList()),
                                reviewsByAsin.getOrDefault(candidate.getParentAsin(), Collections.emptyList())
                        );
                        return new ScoredProduct(candidate, score);
                    })
                    .filter(scored -> scored.getScore() > 0.3)
                    .sorted(Comparator.comparing(ScoredProduct::getScore).reversed())
                    .limit(maxRecommendations)
                    .map(ScoredProduct::getProduct)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting recommendations for product {}: {}", parentAsin, e.getMessage());
            return Collections.emptyList();
        }
    }

    private double calculateScore(Product source, Product candidate,
                                  List<Review> sourceReviews, List<Review> candidateReviews) {

        Map<String, Double> scores = new HashMap<>();

        // 1. Score des reviews
        scores.put("review_sentiment", calculateReviewScore(candidateReviews));

        // 2. Score type de jouet
        scores.put("toy_type", calculateToyTypeScore(source, candidate));

        // 3. Score tranche d'âge
        scores.put("age_match", calculateAgeMatchScore(source, candidate));

        // 4. Score prix (si disponible)
        if (source.getPrice() != null && candidate.getPrice() != null) {
            scores.put("price", calculatePriceScore(source.getPrice(), candidate.getPrice()));
        }

        return calculateWeightedScore(scores);
    }

    private double calculateReviewScore(List<Review> reviews) {
        if (reviews.isEmpty()) return 0.0;

        // Score basé sur les notes et les reviews vérifiées
        double averageRating = reviews.stream()
                .mapToDouble(Review::getRating)
                .average()
                .orElse(0.0) / 5.0;

        long verifiedCount = reviews.stream()
                .filter(Review::getVerifiedPurchase)
                .count();

        double verifiedRatio = (double) verifiedCount / reviews.size();

        return (averageRating * 0.7) + (verifiedRatio * 0.3);
    }

    private double calculateToyTypeScore(Product source, Product candidate) {
        Set<String> sourceTypes = extractToyTypes(source);
        Set<String> candidateTypes = extractToyTypes(candidate);

        if (sourceTypes.isEmpty() || candidateTypes.isEmpty()) {
            return 0.5;
        }

        Set<String> intersection = new HashSet<>(sourceTypes);
        intersection.retainAll(candidateTypes);

        return !intersection.isEmpty() ? 1.0 : 0.0;
    }

    private Set<String> extractToyTypes(Product product) {
        String searchText = String.join(" ",
                product.getTitle().toLowerCase(),
                product.getDescription() != null ?
                        String.join(" ", product.getDescription()).toLowerCase() : ""
        );

        return TOY_CATEGORIES.entrySet().stream()
                .filter(entry -> entry.getValue().stream()
                        .anyMatch(searchText::contains))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private double calculateAgeMatchScore(Product source, Product candidate) {
        String sourceAge = extractAgeRange(source);
        String candidateAge = extractAgeRange(candidate);

        if (sourceAge.equals("unknown") || candidateAge.equals("unknown")) {
            return 0.5;
        }

        return sourceAge.equals(candidateAge) ? 1.0 : 0.0;
    }

    private String extractAgeRange(Product product) {
        String searchText = String.join(" ",
                product.getTitle().toLowerCase(),
                product.getDescription() != null ?
                        String.join(" ", product.getDescription()).toLowerCase() : ""
        );

        if (searchText.contains("0-2") || searchText.contains("baby")) return "0-2";
        if (searchText.contains("2-4") || searchText.contains("toddler")) return "2-4";
        if (searchText.contains("4-8") || searchText.contains("kid")) return "4-8";
        if (searchText.contains("8-12")) return "8-12";
        if (searchText.contains("12+") || searchText.contains("teen")) return "12+";

        return "unknown";
    }


    private double calculatePriceScore(Double price1, Double price2) {
        double priceDiff = Math.abs(price1 - price2);
        double maxPrice = Math.max(price1, price2);
        return Math.max(0, 1 - (priceDiff / maxPrice));
    }

    private double calculateWeightedScore(Map<String, Double> scores) {
        return scores.entrySet().stream()
                .mapToDouble(entry ->
                        entry.getValue() * WEIGHTS.getOrDefault(entry.getKey(), 0.0))
                .sum();
    }

    @Data
    @AllArgsConstructor
    private static class ScoredProduct {
        private Product product;
        private double score;
    }
}
