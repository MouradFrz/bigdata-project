package com.example.bigdataback.dto;

import lombok.Data;

@Data
public class SentimentStats {
    private long positiveCount;
    private long neutralCount;
    private long negativeCount;
}
