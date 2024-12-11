package com.example.bigdataback.controller;

import com.example.bigdataback.dto.SearchCriteria;
import com.example.bigdataback.dto.UserRequest;
import com.example.bigdataback.entity.Product;
import com.example.bigdataback.mapper.QueryMapper;
import com.example.bigdataback.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/products")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;
    private final QueryMapper queryMapper;

    @GetMapping("/search")
    public ResponseEntity<Page<Product>> searchProducts(
            @RequestParam(required = false) String mainCategory,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Received request with mainCategory: '{}', page: {}, size: {}",
                mainCategory, page, size);

        SearchCriteria criteria = SearchCriteria.builder()
                .mainCategory(mainCategory)
                .keyword(keyword)
                .minRating(minRating)
                .minPrice(minPrice)
                .page(page)
                .size(size)
                .build();

        Page<Product> results = productService.search(criteria);
        log.info("Returning page {} with {} results", page, results.getNumberOfElements());
        return ResponseEntity.ok(results);
    }

    @PostMapping
    public ResponseEntity<Page<Product>> processingUserRequest(@RequestBody UserRequest userRequest) {
        Page<Product> results = queryMapper.mapUserRequest(userRequest);
        return ResponseEntity.ok(results);
    }
}
