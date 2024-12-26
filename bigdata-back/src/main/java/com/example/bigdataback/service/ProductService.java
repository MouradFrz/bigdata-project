package com.example.bigdataback.service;

import com.example.bigdataback.dto.UserRequest;
import com.example.bigdataback.dto.ProductSummary;
import com.example.bigdataback.entity.Product;
import com.example.bigdataback.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
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



}
