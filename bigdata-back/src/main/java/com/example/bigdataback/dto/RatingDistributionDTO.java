package com.example.bigdataback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RatingDistributionDTO {
    private Double rating;
    private Long count;
} 
