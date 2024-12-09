package com.example.bigdataback.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchCriteria {
    private String keyword;
    private String mainCategory;
    private Double minRating;
    private Double minPrice;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;
}
