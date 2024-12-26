package com.example.bigdataback.repository;

import com.example.bigdataback.entity.Review;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface ReviewRepository extends MongoRepository<Review, String> {

    List<Review> findByParentAsin(String parentAsin);
    List<Review> findByParentAsinAndVerifiedPurchase(String parentAsin, boolean verifiedPurchase);

    List<Review> findByParentAsinIn(List<String> parentAsins);

}
