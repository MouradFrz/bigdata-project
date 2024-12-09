package com.example.bigdataback.entity;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Map;

@Data
public class ProductDetails {
    @Field("item_weight")
    private String itemWeight;

    @Field("manufacturer_recommended_age")
    private String manufacturerRecommendedAge;

    @Field("best_sellers_rank")
    private Map<String, Object> bestSellersRank;

    @Field("is_discontinued_by_manufacturer")
    private String isDiscontinuedByManufacturer;

    private String manufacturer;
}
