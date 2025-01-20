package com.example.bigdataback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PriceDistributionDTO {
    private String category;
    private Double minPrice;
    private Double maxPrice;
    private Double avgPrice;
    private Double medianPrice;
    private Long productCount;
} 