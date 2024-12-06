package com.example.bigdataback.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.BsonTimestamp;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "products")
public class Product {

    @Id
    @JsonProperty(value = "ObjectId")
    private ObjectId id;

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "price")
    private BigDecimal price;

    @JsonProperty(value = "date")
    private BsonTimestamp date;
}
