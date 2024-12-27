package com.example.bigdataback.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.SparkSession;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.*;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.JavaRDD;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final SparkSession sparkSession;
    private final MongoTemplate mongoTemplate;

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }

    @GetMapping("/spark")
    public ResponseEntity<String> testSparkConnection() {
        try {
            log.info("Starting Spark connection test");

            // Get Java SparkContext
            JavaSparkContext jsc = JavaSparkContext.fromSparkContext(sparkSession.sparkContext());

            // Create test data using Java API
            List<String> data = Arrays.asList("test1", "test2", "test3");
            JavaRDD<String> testRdd = jsc.parallelize(data);

            long count = testRdd.count();
            String firstElement = testRdd.first();

            String response = String.format("Spark connection successful. Count: %d, First value: %s",
                    count, firstElement);
            log.info(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error in Spark connection test", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Spark connection failed: " + e.getMessage());
        }
    }

    @GetMapping("/mongo")
    public ResponseEntity<Map<String, Object>> testMongo() {
        try {
            // Query the metadata collection
            long metadataCount = mongoTemplate.getCollection("metadata").countDocuments();
            long reviewsCount = mongoTemplate.getCollection("reviews").countDocuments();

            Map<String, Object> response = new HashMap<>();
            response.put("metadataCount", metadataCount);
            response.put("reviewsCount", reviewsCount);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in MongoDB test", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
