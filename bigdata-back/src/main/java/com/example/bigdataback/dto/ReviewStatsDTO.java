package com.example.bigdataback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor

public class ReviewStatsDTO {
    private long totalReviews; // Use `long` here to handle both Integer and Long
    private long verifiedPurchases;
}
