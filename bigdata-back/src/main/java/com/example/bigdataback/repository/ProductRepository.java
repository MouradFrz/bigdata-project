package com.example.bigdataback.repository;

import com.example.bigdataback.entity.Product;
import org.bson.types.ObjectId;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, ObjectId> {

    List<Product> findByMainCategory(String mainCategory);

    List<Product> findByAverageRatingBetween(Double minRating, Double maxRating);

    @Query("{ 'title' : { $regex: ?0, $options: 'i' }}")
    List<Product> findByTitleContainingIgnoreCase(String keyword);

    @Query("{price:  {$gte: ?0}}")
    List<Product> findProductsWithPriceGreaterThan(Double price, Pageable pageable);

    @Query("{average_rating:  {$gte: ?0}}")
    List<Product> findProductsWithRatingGreaterThan(Double minRating, PageRequest pageRequest);
}
