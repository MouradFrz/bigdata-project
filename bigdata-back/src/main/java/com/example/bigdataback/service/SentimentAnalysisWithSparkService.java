package com.example.bigdataback.service;

import com.example.bigdataback.dto.SentimentStats;
import com.example.bigdataback.entity.Review;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
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
        // Fetch reviews from MongoDB
        List<Review> reviews = mongoTemplate.find(
                query(where("parent_asin").is(parentAsin)),
                Review.class,
                "reviews"
        );

        if (reviews.isEmpty()) {
            throw new IllegalArgumentException("No reviews found for the provided parent_asin: " + parentAsin);
        }

        // Simplify reviews to include only title and text
        List<SimpleReview> simpleReviews = reviews.stream()
                .map(review -> new SimpleReview(review.getTitle(), review.getText()))
                .collect(Collectors.toList());

        // Convert the simplified reviews to a Spark DataFrame
        Dataset<Row> dataset = sparkSession.createDataFrame(simpleReviews, SimpleReview.class);

        // Load pre-trained Spark NLP pipeline for sentiment analysis
        PretrainedPipeline pipeline = new PretrainedPipeline("analyze_sentiment", "en");

        // Transform dataset using Spark NLP pipeline
        Dataset<Row> sentimentDataset = pipeline.transform(dataset);

        // Extract sentiment results from the annotations column
        Dataset<Row> sentimentResults = sentimentDataset.selectExpr("explode(sentiment.result) as sentiment");

        // Group by sentiment and count the occurrences
        Dataset<Row> sentimentCounts = sentimentResults.groupBy("sentiment").count();

        // Collect the results
        List<Row> resultRows = sentimentCounts.collectAsList();

        // Map the results to the SentimentStats object
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

    // Inner class for simplified reviews
    public static class SimpleReview {
        private String title;
        private String text;

        public SimpleReview(String title, String text) {
            this.title = title;
            this.text = text;
        }

        public String getTitle() {
            return title;
        }

        public String getText() {
            return text;
        }
    }
}
