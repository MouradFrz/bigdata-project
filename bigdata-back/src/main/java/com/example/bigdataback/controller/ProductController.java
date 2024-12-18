package com.example.bigdataback.controller;

import com.example.bigdataback.dto.UserRequest;
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
    public ResponseEntity<?> processingUserRequest(@RequestBody UserRequest userRequest) {
        log.info("Received user request.......... {}", userRequest.getRequest());
        Document query = QueryParser.parseQuery(userRequest.getRequest());
        log.info("Parsed query.........{}", query.toJson());
        return ResponseEntity.ok(productService.findByParsedQuery(userRequest, query.toJson()));
    }
}
