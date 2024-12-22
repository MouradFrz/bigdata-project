package com.example.bigdataback.repository;

import com.example.bigdataback.entity.Product;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<Product, ObjectId> {

    @Query("?0")
    List<Product> findByParsedQuery(Document query, Pageable pageable);

    @Query("{ 'main_category': ?0, 'parent_asin': { $ne: ?1 } }")
    List<Product> findByMainCategoryAndNotParentAsin(
            String mainCategory,
            String excludeParentAsin
    );

    @Query(value = "{ 'main_category': ?0, 'parent_asin': { $ne: ?1 } }",
            fields = "{ 'parent_asin': 1, 'title': 1, 'average_rating': 1, 'rating_number': 1, 'price': 1 }")
    List<Product> findSimilarProducts(String mainCategory, String excludeParentAsin, Pageable pageable);

    Optional<Product> findByParentAsin(String parentAsin);

}

