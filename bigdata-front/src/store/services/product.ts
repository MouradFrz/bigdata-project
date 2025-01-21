import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import { Product } from '../../types';

export const productApi = createApi({
    reducerPath: 'productApi',
    baseQuery: fetchBaseQuery({ baseUrl: 'http://localhost:9999/api/v1/products' }),
    tagTypes: ['ProductRecommendations', 'Product'],
    endpoints: (builder) => ({
        getFilteredProducts: builder.mutation<{ content: Product[] }, { request: string; size: number; page: number }>({
            query: (body) => ({
                url: ``,
                method: 'POST',
                body,
            }),
        }),
        getProductDetails: builder.query<any, string>({
            query: (parentAsin) => `/${parentAsin}`,
            providesTags: (_, __, parentAsin) => [{ type: 'Product', id: parentAsin }],
        }),
        getProductRecommendations: builder.query<{ recommendations: Product[] }, string>({
            query: (parentAsin) => `/${parentAsin}/spark-recommendations`,
            providesTags: (_, __, parentAsin) => [{ type: 'ProductRecommendations', id: parentAsin }],
        }),
    }),
});

export const { useGetProductDetailsQuery, useGetFilteredProductsMutation, useGetProductRecommendationsQuery, useLazyGetProductRecommendationsQuery } = productApi;
