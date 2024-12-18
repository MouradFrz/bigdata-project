package com.example.bigdataback.controller;

import com.example.bigdataback.dto.SearchCriteria;
import com.example.bigdataback.parser.QueryParser;
import com.example.bigdataback.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/products")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<?> processingUserRequest(@RequestBody SearchCriteria searchCriteria) {
        log.info("Received user request.......... {}", searchCriteria.getRequest());
        Document query = QueryParser.parseQuery(searchCriteria.getRequest());
        log.info("Parsed query.........{}", query.toJson());
        return ResponseEntity.ok(productService.findByParsedQuery(searchCriteria, query.toJson()));
    }
}
