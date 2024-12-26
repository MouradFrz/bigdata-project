package com.example.bigdataback.service;

import com.example.bigdataback.dto.SubCategoryPercentageDto;
import com.mongodb.BasicDBObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        // Étape 1 : Projeter la sous-catégorie à partir de l'index 1 de la liste "categories"
        ProjectionOperation projectionOperation = project()
                .andExpression("categories[0]").as("mainCategory")  // Catégorie principale
                .andExpression("categories[1]").as("subCategory");  // Sous-catégorie à l'index 1

        // Étape 2 : Regrouper les documents par sous-catégorie et compter les occurrences
        GroupOperation groupBySubCategory = Aggregation.group("subCategory")
                .count().as("count");

        // Étape 3 : Calculer le total des documents pour la normalisation
        GroupOperation totalGroup = Aggregation.group()
                .sum("count").as("total")
                .push(Aggregation.ROOT).as("categories");  // Utilisation de l'objet ROOT pour récupérer les sous-catégories et leurs comptages

        // Étape 4 : Calculer le pourcentage de chaque sous-catégorie
        UnwindOperation unwindCategories = Aggregation.unwind("categories");
        ProjectionOperation calculatePercentage = project()
                .and("categories.subCategory").as("subCategory")
                .and("categories.count").as("count")
                .andExpression("categories.count / total * 100").as("percentage");

        // Construire l'agrégation
        Aggregation aggregation = newAggregation(
                projectionOperation,
                groupBySubCategory,
                totalGroup,
                unwindCategories,
                calculatePercentage
        );

        // Exécuter l'agrégation
        AggregationResults<SubCategoryPercentageDto> results = mongoTemplate.aggregate(
                aggregation,
                "metadata",  // Nom de la collection
                SubCategoryPercentageDto.class  // Mapper vers SubCategoryPercentageDto
        );

        // Retourner les résultats agrégés
        return results.getMappedResults();
    }

    public List<SubCategoryPercentageDto> getCategoryStatistics() {
        Aggregation aggregation = newAggregation(
                match(Criteria.where("categories").is("Toys & Games")), // Étape 1 : Filtrer
                project()
                        .andExpression("categories[1]").as("subCategory") // Étape 2 : Projeter la sous-catégorie
                        .and("categories").as("categories"),
                unwind("categories"), // Étape 3 : Unwind
                group("categories").count().as("count"), // Étape 4 : Grouper par catégorie et compter
                group()
                        .sum("count").as("total") // Calculer le total des documents
                        .push(new BasicDBObject("subCategory", "$_id")
                                .append("count", "$count")).as("categories"),
                unwind("categories"), // Étape 5 : Unwind les catégories pour calcul des pourcentages
                project()
                        .and("categories.subCategory").as("subCategory")
                        .and("categories.count").as("count")
                        .andExpression("categories.count / total * 100").as("percentage"),
                match(Criteria.where("percentage").gt(5)) // Étape 6 : Filtrer ceux avec un pourcentage > 5
        );

        // Exécuter l'agrégation et mapper sur le DTO
        AggregationResults<SubCategoryPercentageDto> results =
                mongoTemplate.aggregate(aggregation, "metadata", SubCategoryPercentageDto.class);

        return results.getMappedResults();
    }

   }