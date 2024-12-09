package com.example.bigdataback.entity;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
public class ProductVideo {
    private String title;
    private String url;

    @Field("user_id")
    private String userId;
}
