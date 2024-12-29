package com.example.bigdataback.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.bson.types.ObjectId;
import java.util.List;

@Data
@Document(collection = "metadata")
public class Product {
    @Id
    private ObjectId id;

    @Field("main_category")
    private String mainCategory;

    private String title;

    @Field("average_rating")
    private Double averageRating;

    @Field("rating_number")
    private Integer ratingNumber;

    private List<String> features;
    private List<String> description;
    private Double price;
    private List<ProductImage> images;
    @JsonIgnore
    private List<ProductVideo> videos;
    private String store;
    private List<String> categories;
    private ProductDetails details;

    @Field("parent_asin")
    private String parentAsin;

    @Field("bought_together")
    private Object boughtTogether;
    @Field("final_score")
    private Double finalScore;
}
