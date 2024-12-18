package com.example.bigdataback.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {

    private String request;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;
}
