package com.example.bigdataback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReviewHelpfulnessDTO {
    private Integer rating;
    private Double averageHelpfulVotes;
    private Long reviewCount;
} 