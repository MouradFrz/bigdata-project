
package com.example.bigdataback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewTimelineDTO {
    private Long timestamp;
    private Integer reviewCount;
    private Double averageRating;
}