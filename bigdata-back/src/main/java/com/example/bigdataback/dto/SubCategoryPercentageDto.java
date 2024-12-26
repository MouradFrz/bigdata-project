package com.example.bigdataback.dto;

import lombok.Data;

@Data
public class SubCategoryPercentageDto {
    private String subCategory;
    private Long count;
    private Double percentage;
}
