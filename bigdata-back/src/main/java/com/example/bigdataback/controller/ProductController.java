package com.example.bigdataback.controller;

import com.example.bigdataback.dto.ErrorResponse;
import com.example.bigdataback.dto.UserRequest;
import com.example.bigdataback.parser.QueryParser;
import com.example.bigdataback.service.ProductDetailService;
import com.example.bigdataback.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(value = "/products")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    private final ProductDetailService productDetailService;

    @PostMapping
    public ResponseEntity<?> processingUserRequest(@RequestBody UserRequest userRequest) {
        log.info("Received user request.......... {}", userRequest.getRequest());
        Document query = QueryParser.parseQuery(userRequest.getRequest());
        log.info("Parsed query.........{}", query.toJson());
        if (query.isEmpty() && !userRequest.getRequest().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ErrorResponse.builder()
                            .code(HttpStatus.BAD_REQUEST.value())
                            .message("The request you provided is invalid")
                            .build()
            );
        } else {
            return ResponseEntity.ok(productService.findByParsedQuery(userRequest, query));
        }
    }

    @GetMapping("/{parentAsin}")
    public ResponseEntity<Map<String, Object>> getProductDetails(
            @PathVariable String parentAsin,
            @RequestParam(required = false, defaultValue = "false") Boolean verifiedOnly,
            @RequestParam(required = false, defaultValue = "5") Integer maxRecommendations) {

        Map<String, Object> response = productDetailService.getProductDetailsWithRecommendations(
                parentAsin,
                verifiedOnly,
                maxRecommendations
        );

        return ResponseEntity.ok(response);
    }
}
