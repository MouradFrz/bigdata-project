package com.example.bigdataback.parser;

import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class QueryParser {

    public static Document parseQuery(String input) {
        input = input.replaceAll("['\"?@:!]+", "");

        input = input.toLowerCase().replaceAll(",", ".");
        log.info("Parsing query: {}", input);
        List<Document> orConditionsList = new ArrayList<>();

        if (input.contains(" ou ")) {
            String[] orConditions = input.split("\\b ou \\b");
            for (String condition : orConditions) {
                orConditionsList.add(processAndCondition(condition));
            }
            return new Document("$or", orConditionsList);
        } else {
            return processAndCondition(input);
        }
    }

    private static Document processAndCondition(String condition) {
        List<Document> andConditionsList = new ArrayList<>();

        if (condition.contains(" et ")) {
            String[] andConditions = condition.split("\\b et \\b");
            for (String subCondition : andConditions) {
                andConditionsList.add(parseSingleCondition(subCondition));
            }
            return new Document("$and", andConditionsList);
        } else {
            return parseSingleCondition(condition);
        }
    }

    private static Document parseSingleCondition(String condition) {
        Pattern pricePattern = Pattern.compile("prix (inférieur|supérieur|égal) à (\\d+(?:\\.\\d+)?)");
        Pattern ratingPattern = Pattern.compile("note (supérieure|inférieure|égal) à (\\d+(?:\\.\\d+)?)");
        Pattern mainCategoryPattern = Pattern.compile("catégorie principale (est |)(.+)");
        Pattern categoryPattern = Pattern.compile("catégorie (contient |)(.+)");
        Pattern storePattern = Pattern.compile("magasin (est |)(.+)");
        Pattern keywordPattern = Pattern.compile("(titre|description) (contient |)(.+)");

        Matcher priceMatcher = pricePattern.matcher(condition);
        Matcher ratingMatcher = ratingPattern.matcher(condition);
        Matcher mainCategoryMatcher = mainCategoryPattern.matcher(condition);
        Matcher categoryMatcher = categoryPattern.matcher(condition);
        Matcher storeMatcher = storePattern.matcher(condition);
        Matcher keywordMatcher = keywordPattern.matcher(condition);

        if (priceMatcher.find()) {
            String operator = priceMatcher.group(1);
            double priceValue = parseDouble(priceMatcher.group(2));
            return new Document("price", getMongoOperator(operator, priceValue));
        }

        if (ratingMatcher.find()) {
            String operator = ratingMatcher.group(1);
            double ratingValue = parseDouble(ratingMatcher.group(2));
            return new Document("average_rating", getMongoOperator(operator, ratingValue));
        }

        if (mainCategoryMatcher.find()) {
            String mainCategory = mainCategoryMatcher.group(2).trim();
            return new Document("main_category", mainCategory);
        }

        if (categoryMatcher.find()) {
            String category = categoryMatcher.group(2).trim();
            return new Document("categories", new Document("$regex", category).append("$options", "i"));
        }

        if (storeMatcher.find()) {
            String store = storeMatcher.group(2).trim();
            return new Document("store", new Document("$regex", store).append("$options", "i"));
        }

        if (keywordMatcher.find()) {
            String field = keywordMatcher.group(1).trim();
            String keyword = keywordMatcher.group(3).trim();
            return new Document(field, new Document("$regex", keyword).append("$options", "i"));
        }

        return new Document();
    }

    private static Document getMongoOperator(String operator, double value) {
        if (operator.matches("(?i)inférieur(e)?")) {
            return new Document("$lt", value);
        } else if (operator.matches("(?i)supérieur(e)?")) {
            return new Document("$gt", value);
        } else if (operator.matches("(?i)égal")) {
            return new Document("$eq", value);
        } else {
            return new Document();
        }
    }

    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            log.error("Failed to parse numeric value: {}", value, e);
            return 0.0;
        }
    }
}
