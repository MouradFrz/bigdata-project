import React, { useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { Product, ProductDetailsWithReviewsResponse } from '../../types';
import { useSelector } from 'react-redux';
import { productApi, useGetProductDetailsQuery, useLazyGetProductRecommendationsQuery } from '../../store/services/product';
import { FetchBaseQueryError } from '@reduxjs/toolkit/query';
import { SerializedError } from '@reduxjs/toolkit';

interface ProductVMReturn {
    productWithReviews: ProductDetailsWithReviewsResponse;
    error: FetchBaseQueryError | SerializedError | undefined | null;
    recommendations?: { recommendations: Product[] };
    recommendationsError: FetchBaseQueryError | SerializedError | undefined | null;
    isFetchingProductDetails: boolean;
    isFetchingRecommendations: boolean;
}

function useProductVM(): ProductVMReturn {
    const { id: productId } = useParams() as { id: string };
    const { data: productWithReviews, error, isFetching: isFetchingProductDetails } = useGetProductDetailsQuery(productId);
    const [getProductRecommendations, { data: recommendations, error: recommendationsError, isFetching: isFetchingRecommendations }] = useLazyGetProductRecommendationsQuery();
    const recommendationAlreadyFetched = useSelector(productApi.endpoints.getProductRecommendations.select(productId));
    const productDetailsAlreadyFetched = useSelector(productApi.endpoints.getProductDetails.select(productId));
    useEffect(() => {
        if (productDetailsAlreadyFetched.data && !recommendationAlreadyFetched.data) {
            getProductRecommendations(productId);
        }
    }, [productWithReviews, productId]);

    return { productWithReviews, error, recommendations, recommendationsError, isFetchingProductDetails, isFetchingRecommendations };
}

export default useProductVM;
