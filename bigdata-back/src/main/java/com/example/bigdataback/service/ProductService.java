package com.example.bigdataback.service;

import com.example.bigdataback.dto.ProductSummary;
import com.example.bigdataback.dto.SearchCriteria;
import com.example.bigdataback.entity.Product;
import com.example.bigdataback.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final MongoTemplate mongoTemplate;
    private final ProductRepository productRepository;

    public Page<Product> search(SearchCriteria criteria) {
        Query query = buildQuery(criteria);

        // compter le total sans pagination
        long total = mongoTemplate.count(query, Product.class, "metadata");
        log.info("Total documents before pagination: {}", total);

        // Ajouter la pagination à la requête
        PageRequest pageRequest = PageRequest.of(criteria.getPage(), criteria.getSize());
        query.with(pageRequest);

        // Exécuter la requête avec pagination
        List<Product> products = mongoTemplate.find(query, Product.class, "metadata");
        log.info("Retrieved {} products for page {} of size {}",
                products.size(), criteria.getPage(), criteria.getSize());

        return new PageImpl<>(products, pageRequest, total);
    }

    private Query buildQuery(SearchCriteria criteria) {
        Query query = new Query();

        if (criteria.getMainCategory() != null && !criteria.getMainCategory().isEmpty()) {
            log.info("Adding category filter: '{}'", criteria.getMainCategory());
            query.addCriteria(Criteria.where("main_category")
                    .regex("^" + criteria.getMainCategory() + "$", "i"));
        }

        if (criteria.getKeyword() != null && !criteria.getKeyword().isEmpty()) {
            log.info("Adding keyword filter: {}", criteria.getKeyword());
            query.addCriteria(Criteria.where("title").regex(criteria.getKeyword(), "i"));
        }

        if (criteria.getMinRating() != null) {
            log.info("Adding rating filter: >= {}", criteria.getMinRating());
            query.addCriteria(Criteria.where("average_rating").gte(criteria.getMinRating()));
        }

        if (criteria.getMinPrice() != null) {
            log.info("Adding price filter: >= {}", criteria.getMinPrice());
            query.addCriteria(Criteria.where("price").gte(criteria.getMinPrice()));
        }

        return query;
    }

    public Page<Product> findProductsWithPriceGreaterThan(SearchCriteria criteria) {
        PageRequest pageRequest = PageRequest.of(criteria.getPage(), criteria.getSize());
        List<Product> products = this.productRepository.findProductsWithPriceGreaterThan(criteria.getMinPrice(), pageRequest);
        return new PageImpl<>(products, pageRequest, products.size());
    }

    public Page<Product> findProductsWithTopRating(SearchCriteria criteria) {
        PageRequest pageRequest = PageRequest.of(criteria.getPage(), criteria.getSize());
        List<Product> products = this.productRepository.findProductsWithRatingGreaterThan(criteria.getMinRating(), pageRequest);
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



}
