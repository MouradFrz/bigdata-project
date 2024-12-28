package com.example.bigdataback.service;

import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;
import org.springframework.stereotype.Service;

@Service
public class SparkSessionProvider {

    private final SparkSession sparkSession;

    public SparkSessionProvider() {
        try {
            // Spark configuration setup
            SparkConf sparkConf = new SparkConf()
                    .setAppName("SentimentAnalysis")
                    .setMaster("local[*]")
                    .set("spark.driver.allowMultipleContexts", "true") // Enable multiple contexts
                    .set("spark.executor.memory", "2g")               // Set executor memory
                    .set("spark.driver.memory", "2g");                // Set driver memory

            // Disable Spark UI if not needed
            sparkConf.set("spark.ui.enabled", "false");

            // Initialize SparkSession with the configuration
            this.sparkSession = SparkSession.builder()
                    .config(sparkConf)
                    .getOrCreate();
        } catch (Exception e) {
            // Enhanced error handling
            throw new RuntimeException("Error initializing SparkSession: " + e.getMessage(), e);
        }
    }

    // Getter for SparkSession
    public SparkSession getSparkSession() {
        return sparkSession;
    }
}
