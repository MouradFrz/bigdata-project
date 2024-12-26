package com.example.bigdataback.controller;

import com.example.bigdataback.dto.ReviewStatsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.bigdataback.service.ReviewService;

import java.util.Map;

@RestController
@RequestMapping(value = "/reviews")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;

    /*
    this end point return the number of verified reviews(purshases) and the total number of reviews
    Graph type: pie chart
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getReviewCounts() {
        return ResponseEntity.ok(reviewService.getReviewCounts());
    }
}
