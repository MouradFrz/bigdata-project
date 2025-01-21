package com.example.bigdataback.service;

import com.example.bigdataback.dto.SentimentStats;
import com.example.bigdataback.entity.Review;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import com.johnsnowlabs.nlp.pretrained.PretrainedPipeline;

@Service
public class SentimentAnalysisWithSparkService {

    private final MongoTemplate mongoTemplate;
    private final SparkSession sparkSession;

    public SentimentAnalysisWithSparkService(MongoTemplate mongoTemplate, SparkSessionProvider sparkSessionProvider) {
        this.mongoTemplate = mongoTemplate;
        this.sparkSession = sparkSessionProvider.getSparkSession();
    }

    public SentimentStats analyzeSentimentsByParentAsin(String parentAsin) {
        // ✅ Fetch reviews from MongoDB (Only Title & Text, Ignore `parent_asin`)
        List<Review> reviews = mongoTemplate.find(
                query(where("parent_asin").is(parentAsin)),
                Review.class,
                "reviews"
        );

        if (reviews.isEmpty()) {
            throw new IllegalArgumentException("No reviews found for parent_asin: " + parentAsin);
        }

        // ✅ Keep Only Title and Text (No `parent_asin`)
        List<SimpleReview> simpleReviews = reviews.stream()
                .map(review -> new SimpleReview(review.getTitle(), review.getText()))
                .collect(Collectors.toList());

        // ✅ Convert List to Spark DataFrame
        Dataset<Row> dataset = sparkSession.createDataFrame(simpleReviews, SimpleReview.class);

        // ✅ Load Pretrained Sentiment Analysis Pipeline
        PretrainedPipeline pipeline = new PretrainedPipeline("analyze_sentiment", "en");

        // ✅ Apply Sentiment Analysis
        Dataset<Row> sentimentDataset = pipeline.transform(dataset);

        // ✅ Keep only one sentiment per review (avoid overcounting)
        Dataset<Row> reviewSentiments = sentimentDataset
                .withColumn("sentiment", functions.expr("sentiment.result[0]")) // Pick first sentiment for each review
                .select("sentiment");

        // ✅ Count unique reviews per sentiment category
        Dataset<Row> sentimentCounts = reviewSentiments.groupBy("sentiment").count();

        // ✅ Collect Results
        List<Row> resultRows = sentimentCounts.collectAsList();

        // ✅ Map Results to SentimentStats DTO
        SentimentStats stats = new SentimentStats();
        for (Row row : resultRows) {
            String sentiment = row.getString(0);
            long count = row.getLong(1);
            switch (sentiment.toLowerCase()) {
                case "positive":
                    stats.setPositiveCount(count);
                    break;
                case "neutral":
                    stats.setNeutralCount(count);
                    break;
                case "negative":
                    stats.setNegativeCount(count);
                    break;
            }
        }

        return stats;
    }

    // ✅ Inner Class to Hold Only Title and Text
    public static class SimpleReview {
        private String title;
        private String text;

        public SimpleReview(String title, String text) {
            this.title = title;
            this.text = text;
        }

        public String getTitle() { return title; }
        public String getText() { return text; }
    }
}
