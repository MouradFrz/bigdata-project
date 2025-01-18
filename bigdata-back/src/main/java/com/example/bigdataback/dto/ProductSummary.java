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
    private Double averageRating;
    private Integer ratingNumber;

    public double getAverageRating() { // ✅ Ajout du getter !
        return averageRating;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getRatingNumber() {
        return ratingNumber;
    }
}
