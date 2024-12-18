package com.example.bigdataback.repository;

import com.example.bigdataback.entity.Product;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, ObjectId> {

    @Query("?0")
    List<Product> findByParsedQuery(String query, Pageable pageable);
}
