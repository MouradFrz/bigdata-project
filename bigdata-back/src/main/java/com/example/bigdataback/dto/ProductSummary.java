package com.example.bigdataback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductSummary {
    private String id;
    private String title;
    private Double averagerating;
    private Integer ratingNumber;
}
