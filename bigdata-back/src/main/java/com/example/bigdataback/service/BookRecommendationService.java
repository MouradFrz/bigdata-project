package com.example.bigdataback.service;

import com.example.bigdataback.entity.Product;
import com.example.bigdataback.repository.ProductRepository;
import com.example.bigdataback.repository.ReviewRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookRecommendationService {

    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;

    // Liste des genres littéraires courants
    private final Set<String> bookGenres = Set.of(
            "fantasy", "science fiction", "mystery", "thriller", "romance",
            "fiction", "non-fiction", "biography", "history", "children",
            "young adult", "comic", "manga", "horror", "adventure"
    );

    public List<Product> getBookRecommendations(Product sourceBook, Integer maxRecommendations) {
        BookAnalysis sourceAnalysis = analyzeBook(sourceBook);

        List<Product> candidates = productRepository.findByMainCategoryAndNotParentAsin("books", sourceBook.getParentAsin());

        return candidates.stream()
                .map(candidate -> {
                    BookAnalysis candidateAnalysis = analyzeBook(candidate);
                    double score = calculateBookSimilarityScore(sourceAnalysis, candidateAnalysis);
                    return new ScoredProduct(candidate, score);
                })
                .filter(scoredProduct -> scoredProduct.getScore() > 0.2) // Seuil minimum de pertinence
                .sorted(Comparator.comparing(ScoredProduct::getScore).reversed())
                .limit(maxRecommendations)
                .map(ScoredProduct::getProduct)
                .collect(Collectors.toList());
    }

    @Data
    @Builder
    private static class BookAnalysis {
        private String title;
        private Set<String> genres;
        private String author;
        private String series;
        private String format;
        private Integer publicationYear;
        private Double averageRating;
        private Integer ratingCount;
        private Set<String> significantWords;
        private Double price;
    }

    private BookAnalysis analyzeBook(Product book) {
        String title = book.getTitle().toLowerCase();
        String description = book.getDescription() != null ?
                String.join(" ", book.getDescription()).toLowerCase() : "";

        return BookAnalysis.builder()
                .title(title)
                .genres(extractGenres(title, description))
                .author(extractAuthor(book))
                .series(extractSeries(title))
                .format(extractFormat(book))
                .publicationYear(extractYear(book))
                .averageRating(book.getAverageRating())
                .ratingCount(book.getRatingNumber())
                .significantWords(extractSignificantWords(title, description))
                .price(book.getPrice())
                .build();
    }

    private Set<String> extractGenres(String title, String description) {
        String fullText = title + " " + description;
        return bookGenres.stream()
                .filter(genre -> fullText.contains(genre))
                .collect(Collectors.toSet());
    }

    private String extractAuthor(Product book) {
        if (book.getDetails() != null) {
            // Recherche de l'auteur dans les détails du produit
            String details = book.getDetails().toString().toLowerCase();
            Pattern authorPattern = Pattern.compile("author[s]?:[\\s]*(.*?)[,;\\n]");
            Matcher matcher = authorPattern.matcher(details);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }
        return null;
    }

    private String extractSeries(String title) {
        // Patterns courants pour les séries de livres
        List<Pattern> seriesPatterns = Arrays.asList(
                Pattern.compile("(.*?)\\s+#?\\d+"),
                Pattern.compile("(.*?):\\s+book\\s+\\d+"),
                Pattern.compile("(.*?)\\s+trilogy"),
                Pattern.compile("(.*?)\\s+series")
        );

        for (Pattern pattern : seriesPatterns) {
            Matcher matcher = pattern.matcher(title.toLowerCase());
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }
        return null;
    }

    private String extractFormat(Product book) {
        if (book.getDetails() != null) {
            String details = book.getDetails().toString().toLowerCase();
            if (details.contains("hardcover")) return "hardcover";
            if (details.contains("paperback")) return "paperback";
            if (details.contains("kindle")) return "ebook";
            if (details.contains("audiobook")) return "audiobook";
        }
        return null;
    }

    private Integer extractYear(Product book) {
        if (book.getDetails() != null) {
            String details = book.getDetails().toString();
            Pattern yearPattern = Pattern.compile("(19|20)\\d{2}");
            Matcher matcher = yearPattern.matcher(details);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group());
            }
        }
        return null;
    }

    private Set<String> extractSignificantWords(String title, String description) {
        // Mots à ignorer
        Set<String> stopWords = Set.of("the", "a", "an", "and", "or", "but", "in", "on", "at", "to");

        String fullText = (title + " " + description).toLowerCase();
        return Arrays.stream(fullText.split("\\W+"))
                .filter(word -> word.length() > 2)
                .filter(word -> !stopWords.contains(word))
                .collect(Collectors.toSet());
    }

    private double calculateBookSimilarityScore(BookAnalysis source, BookAnalysis candidate) {
        Map<String, Double> weights = Map.of(
                "series", 0.25,
                "author", 0.20,
                "genre", 0.15,
                "format", 0.10,
                "rating", 0.15,
                "year", 0.05,
                "words", 0.10
        );

        double score = 0.0;

        // Score série
        if (source.getSeries() != null && source.getSeries().equals(candidate.getSeries())) {
            score += weights.get("series");
        }

        // Score auteur
        if (source.getAuthor() != null && source.getAuthor().equals(candidate.getAuthor())) {
            score += weights.get("author");
        }

        // Score genre
        if (!source.getGenres().isEmpty() && !candidate.getGenres().isEmpty()) {
            double genreOverlap = calculateSetOverlap(source.getGenres(), candidate.getGenres());
            score += weights.get("genre") * genreOverlap;
        }

        // Score format
        if (source.getFormat() != null && source.getFormat().equals(candidate.getFormat())) {
            score += weights.get("format");
        }

        // Score rating
        if (candidate.getAverageRating() != null && candidate.getRatingCount() != null) {
            double ratingScore = (candidate.getAverageRating() / 5.0) * 0.7 +
                    (Math.min(Math.log1p(candidate.getRatingCount()) / 10, 1)) * 0.3;
            score += weights.get("rating") * ratingScore;
        }

        // Score année
        if (source.getPublicationYear() != null && candidate.getPublicationYear() != null) {
            int yearDiff = Math.abs(source.getPublicationYear() - candidate.getPublicationYear());
            if (yearDiff <= 5) {  // Livres publiés à moins de 5 ans d'écart
                score += weights.get("year") * (1 - yearDiff/5.0);
            }
        }

        // Score mots significatifs
        if (!source.getSignificantWords().isEmpty() && !candidate.getSignificantWords().isEmpty()) {
            double wordOverlap = calculateSetOverlap(
                    source.getSignificantWords(),
                    candidate.getSignificantWords()
            );
            score += weights.get("words") * wordOverlap;
        }

        return score;
    }

    private <T> double calculateSetOverlap(Set<T> set1, Set<T> set2) {
        Set<T> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        Set<T> union = new HashSet<>(set1);
        union.addAll(set2);
        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }

    @Data
    @AllArgsConstructor
    private static class ScoredProduct {
        private Product product;
        private double score;
    }
}
