package com.example.bigdataback.entity;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
public class ReviewImage {
    @Field("small_image_url")
    private String smallImageUrl;

    @Field("medium_image_url")
    private String mediumImageUrl;

    @Field("large_image_url")
    private String largeImageUrl;

    @Field("attachment_type")
    private String attachmentType;
}
