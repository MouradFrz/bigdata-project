package com.example.bigdataback.mapper;

import com.example.bigdataback.dto.UserRequest;
import com.example.bigdataback.entity.Product;
import com.example.bigdataback.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import static com.example.bigdataback.constant.UserRequestConstant.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueryMapper {

    private final ProductService productService;

    public Page<Product> mapUserRequest(UserRequest userRequest) {
        if (userRequest.getRequest().equals(PRODUCTS_WITH_PRICE_GREATER_THAN)) {
            log.info("Processing products with price greater than");
            return productService.findProductsWithPriceGreaterThan(userRequest.getSearchCriteria());
        }

        if (userRequest.getRequest().equals(TOP_RATED_PRODUCTS)) {
            log.info("Processing top rated products");
            userRequest.getSearchCriteria().setMinRating(MIN_RATING);
            log.info("Rating parameters set to {}", MIN_RATING);

            return productService.findProductsWithTopRating(userRequest.getSearchCriteria());
        }
        return Page.empty();
    }
}
