import React from 'react';
import { useParams } from 'react-router-dom';
import { Product, ProductDetailsWithReviewsResponse } from '../../types';

import { useGetProductDetailsQuery, useGetProductRecommendationsQuery } from '../../store/services/product';
import { FetchBaseQueryError } from '@reduxjs/toolkit/query';
import { SerializedError } from '@reduxjs/toolkit';
interface ProductVMReturn {
    productWithReviews: ProductDetailsWithReviewsResponse;
    error: FetchBaseQueryError | SerializedError | undefined | null;
    recommendations?: { recommendations: Product[] };
    recommendationsError: FetchBaseQueryError | SerializedError | undefined | null;
}

function useProductVM(): ProductVMReturn {
    const { id: productId } = useParams();
    const { data: productWithReviews, error } = useGetProductDetailsQuery(productId ?? '');
    const { data: recommendations, error: recommendationsError } = useGetProductRecommendationsQuery(productId ?? '');

    return { productWithReviews, error, recommendations, recommendationsError };
}

export default useProductVM;
