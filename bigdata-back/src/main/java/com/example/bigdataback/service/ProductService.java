package com.example.bigdataback.service;

import com.example.bigdataback.dto.UserRequest;
import com.example.bigdataback.entity.Product;
import com.example.bigdataback.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final MongoTemplate mongoTemplate;
    private final ProductRepository productRepository;

    public Page<Product> findByParsedQuery(UserRequest userRequest, String query) {
        PageRequest pageRequest = PageRequest.of(userRequest.getPage(), userRequest.getSize());
        List<Product> products = this.productRepository.findByParsedQuery(query, pageRequest);
        return new PageImpl<>(products, pageRequest, products.size());
    }
}
