package com.example.bigdataback.service;

import jakarta.annotation.PreDestroy;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;


@Configuration
public class SparkConfig {
    private SparkSession sparkSession;
    private static final Logger log = LoggerFactory.getLogger(SparkConfig.class);

    @Value("${spark.config.driver.memory:8g}")
    private String driverMemory;

    @Value("${spark.config.executor.memory:6g}")
    private String executorMemory;

    @Bean
    @Scope("singleton")
    public SparkSession sparkSession() {
        try {
            System.setProperty("spark.driver.allowMultipleContexts", "true");
            System.setProperty("org.apache.spark.logger.level", "WARN");

            sparkSession = SparkSession.builder()
                    .appName("AmazonRecommendations")
                    .master("local[*]")
                    // Configuration MongoDB
                    .config("spark.mongodb.input.uri", "mongodb://localhost:27017")
                    .config("spark.mongodb.output.uri", "mongodb://localhost:27017")
                    .config("spark.mongodb.database", "amazon_reviews")
                    .config("spark.mongodb.input.partitioner", "MongoSinglePartitioner")
                    // Configuration de base
                    .config("spark.driver.host", "localhost")
                    .config("spark.driver.bindAddress", "localhost")
                    .config("spark.ui.enabled", "false")
                    // Configuration mémoire
                    .config("spark.driver.memory", driverMemory)
                    .config("spark.executor.memory", executorMemory)
                    .config("spark.driver.maxResultSize", "4g")
                    .config("spark.memory.fraction", "0.7")
                    .config("spark.memory.storageFraction", "0.3")
                    // Configurations mémoire optimisées
                    .config("spark.driver.memory", driverMemory)
                    .config("spark.executor.memory", executorMemory)
                    .config("spark.driver.maxResultSize", "6g")  // Augmenté
                    .config("spark.memory.fraction", "0.8")      // Augmenté
                    .config("spark.memory.storageFraction", "0.3")
                    .config("spark.shuffle.memoryFraction", "0.4") // Ajouté
                    .config("spark.storage.memoryFraction", "0.4") // Ajouté
                    // Configurations performance optimisées
                    .config("spark.default.parallelism", "8")    // Augmenté
                    .config("spark.sql.shuffle.partitions", "20") // Augmenté
                    .config("spark.sql.adaptive.enabled", "true")
                    .config("spark.sql.adaptive.shuffle.targetPostShuffleInputSize", "128mb") // Augmenté
                    .config("spark.sql.adaptive.coalescePartitions.enabled", "true")
                    .config("spark.sql.files.maxPartitionBytes", "256mb") // Augmenté
                    .config("spark.sql.inMemoryColumnarStorage.compressed", "true")
                    .config("spark.sql.inMemoryColumnarStorage.batchSize", "5000") // Réduit
                    .config("spark.sql.broadcastTimeout", "1800") // Augmenté
                    .config("spark.network.timeout", "1200")      // Augmenté
                    // Nouvelles configurations pour la gestion de la mémoire
                    .config("spark.cleaner.periodicGC.interval", "15min")
                    .config("spark.cleaner.referenceTracking.cleanCheckpoints", "true")
                    .config("spark.speculation", "false")
                    .config("spark.rdd.compress", "true")
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
            if (sparkSession != null) {
                sparkSession.stop();
                sparkSession = null;
                log.info("SparkSession stopped successfully");
            }
        } catch (Exception e) {
            log.error("Error while stopping SparkSession", e);
        }
    }
}
