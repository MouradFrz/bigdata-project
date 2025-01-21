package com.example.bigdataback.service;

import com.example.bigdataback.dto.SubCategoryPercentageDto;
import com.mongodb.BasicDBObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryService {
    private final MongoTemplate mongoTemplate;

    public List<SubCategoryPercentageDto> calculateSubCategoryPercentages() {
        Aggregation aggregation = newAggregation(
                // Step 1: Group by main_category and count occurrences
                group("main_category").count().as("count"),
                // Step 2: Calculate the total count of all documents
                group()
                        .sum("count").as("total")
                        .push(new BasicDBObject("subCategory", "$_id")
                                .append("count", "$count")).as("categories"),
                // Step 3: Unwind the categories for percentage calculation
                unwind("categories"),
                // Step 4: Project the percentage calculation
                project()
                        .and("categories.subCategory").as("subCategory")
                        .and("categories.count").as("count")
                        .andExpression("categories.count / total * 100").as("percentage"),
                // Step 5: Optional - Sort by percentage descending
                sort(Sort.Direction.DESC, "percentage")
        );

        // Execute aggregation and map results to DTO
        AggregationResults<SubCategoryPercentageDto> results =
                mongoTemplate.aggregate(aggregation, "metadata", SubCategoryPercentageDto.class);

        return results.getMappedResults();

    }


   }