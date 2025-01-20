package com.example.bigdataback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VerifiedReviewComparisonDTO {
    private String month;
    private Double verifiedRating;
    private Double nonVerifiedRating;
    private Long verifiedCount;
    private Long nonVerifiedCount;
} 