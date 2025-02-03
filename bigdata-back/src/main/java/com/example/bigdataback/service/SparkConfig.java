package com.example.bigdataback.service;

import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

import javax.annotation.PreDestroy;

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
                    .master("local[4]")
                    // MongoDB Configuration
                    .config("spark.mongodb.input.uri", "mongodb://localhost:27017")
                    .config("spark.mongodb.output.uri", "mongodb://localhost:27017")
                    .config("spark.mongodb.database", "amazon_reviews")
                    .config("spark.mongodb.input.partitioner", "MongoPaginateBySizePartitioner")
                    .config("spark.mongodb.input.partition.size", "32")
                    .config("spark.mongodb.input.max_batch_size", "512")
                    .config("spark.mongodb.socket.timeout", "180000")
                    .config("spark.mongodb.operation.timeout", "180000")
                    .config("spark.ui.enabled", "false")
                    // Network Configuration
                    .config("spark.driver.host", "localhost")
                    .config("spark.driver.bindAddress", "localhost")
                    .config("spark.driver.port", "0")
                    .config("spark.blockManager.port", "0")
                    .config("spark.rpc.message.maxSize", "1024")
                    // Memory Configuration
                    .config("spark.driver.memory", driverMemory)
                    .config("spark.executor.memory", executorMemory)
                    .config("spark.driver.maxResultSize", "4g")
                    .config("spark.memory.fraction", "0.6")
                    .config("spark.memory.storageFraction", "0.5")
                    // Performance Configuration
                    .config("spark.sql.shuffle.partitions", "8")
                    .config("spark.default.parallelism", "4")
                    .config("spark.sql.adaptive.enabled", "true")
                    .config("spark.sql.adaptive.coalescePartitions.enabled", "true")
                    .config("spark.network.timeout", "800")
                    .config("spark.cleaner.referenceTracking.cleanCheckpoints", "true")
                    .config("spark.cleaner.periodicGC.interval", "1min")
                    .config("spark.sql.autoBroadcastJoinThreshold", "10m")
                    .config("spark.sql.broadcastTimeout", "300")

                    .config("spark.sql.adaptive.localShuffleReader.enabled", "true")
                    .config("spark.sql.adaptive.skewedJoin.enabled", "true")
                    .config("spark.task.maxFailures", "4")
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
