package com.example.bigdataback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryStatsDTO {
    private String mainCategory;
    private Double averageRating;
    private Integer totalProducts;
}