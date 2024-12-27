package com.example.bigdataback.service;

import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import javax.annotation.PreDestroy;

@Configuration
public class SparkConfig {

    private SparkSession sparkSession;
    private static final Logger log = LoggerFactory.getLogger(SparkConfig.class);

    static {
        System.setProperty("spark.driver.allowMultipleContexts", "true");
    }

    @Bean
    public SparkSession sparkSession() {
        try {
            // Configure Spark logging using system properties instead
            System.setProperty("org.apache.spark.logger.level", "WARN");
            System.setProperty("akka.logger.level", "WARN");

            sparkSession = SparkSession.builder()
                    .appName("AmazonRecommendations")
                    .master("local[*]")
                    .config("spark.mongodb.input.uri", "mongodb://localhost:27017")
                    .config("spark.mongodb.output.uri", "mongodb://localhost:27017")
                    .config("spark.mongodb.database", "amazon_reviews")
                    .config("spark.driver.host", "localhost")
                    .config("spark.driver.bindAddress", "localhost")
                    .config("spark.ui.enabled", "false")
                    // Optimisations de performance
                    .config("spark.default.parallelism", "4")
                    .config("spark.sql.shuffle.partitions", "4")
                    .config("spark.memory.fraction", "0.8")
                    .config("spark.memory.storageFraction", "0.3")
                    .config("spark.mongodb.input.partitioner", "MongoSinglePartitioner")
                    .config("spark.mongodb.input.readPreference.name", "secondaryPreferred")
                    .getOrCreate();

            log.info("SparkSession created successfully");
            return sparkSession;
        } catch (Exception e) {
            log.error("Failed to create SparkSession", e);
            throw new RuntimeException("Failed to create SparkSession", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        try {
            if (sparkSession != null && !sparkSession.sparkContext().isStopped()) {
                sparkSession.close();
                log.info("SparkSession closed successfully");
            }
        } catch (Exception e) {
            log.error("Error while closing SparkSession", e);
        }
    }
}
