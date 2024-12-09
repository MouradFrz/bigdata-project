package com.example.bigdataback.entity;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.util.List;

@Data
@Document(collection = "reviews")
public class Review {
    @Id
    private ObjectId id;

    private Integer rating;
    private String title;
    private String text;
    private List<ReviewImage> images;
    private String asin;

    @Field("parent_asin")
    private String parentAsin;

    @Field("user_id")
    private String userId;

    private Long timestamp;

    @Field("helpful_vote")
    private Integer helpfulVote;

    @Field("verified_purchase")
    private Boolean verifiedPurchase;
}
