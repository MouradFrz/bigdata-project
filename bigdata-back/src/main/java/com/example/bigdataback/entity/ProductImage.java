package com.example.bigdataback.entity;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
public class ProductImage {
    private String thumb;
    private String large;
    private String variant;

    @Field("hi_res")
    private String hiRes;
}
