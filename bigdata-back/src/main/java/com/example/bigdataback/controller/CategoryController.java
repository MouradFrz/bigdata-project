package com.example.bigdataback.controller;

import com.example.bigdataback.dto.SubCategoryPercentageDto;
import com.example.bigdataback.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;

    //Not finished
    @GetMapping("/percentages")
    public ResponseEntity<List<SubCategoryPercentageDto>> getSubCategoryPercentages() {
        List<SubCategoryPercentageDto> percentages = categoryService.getCategoryStatistics();
        return ResponseEntity.ok(percentages);
    }
}
