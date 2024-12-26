package com.example.bigdataback.repository;

import com.example.bigdataback.entity.Product;
import com.example.bigdataback.entity.Review;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends MongoRepository<Review, ObjectId> {
}
