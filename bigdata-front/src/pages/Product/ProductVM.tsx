import React from 'react';
import { useParams } from 'react-router-dom';
import { ProductDetailsWithReviewsResponse } from '../../types';

import { useGetProductDetailsQuery } from '../../store/services/product';
import { FetchBaseQueryError } from '@reduxjs/toolkit/query';
import { SerializedError } from '@reduxjs/toolkit';

function useProductVM(): { productWithReviews: ProductDetailsWithReviewsResponse; isLoading: boolean; error: FetchBaseQueryError | SerializedError | undefined | null } {
    const { id: productId } = useParams();
    const { data, error, isLoading } = useGetProductDetailsQuery(productId ?? '');

    return { productWithReviews: data, error, isLoading };
}

export default useProductVM;
