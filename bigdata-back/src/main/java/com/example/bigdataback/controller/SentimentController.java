package com.example.bigdataback.controller;

import com.example.bigdataback.dto.SentimentStats;
import com.example.bigdataback.service.SentimentAnalysisWithSparkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(value = "/sentiment")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class SentimentController {

    @Autowired
    private SentimentAnalysisWithSparkService sentimentAnalysisService;

    @GetMapping("/Productsentiments")
    public ResponseEntity<SentimentStats> analyzeSentiments(@RequestParam String parentAsin) {
        SentimentStats stats = sentimentAnalysisService.analyzeSentimentsByParentAsin(parentAsin);
        return ResponseEntity.ok(stats);
    }
}
