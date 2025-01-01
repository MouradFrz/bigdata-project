import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import { Product } from '../../types';

export const productApi = createApi({
    reducerPath: 'productApi',
    baseQuery: fetchBaseQuery({ baseUrl: 'http://localhost:9999/api/v1/products' }),
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
        }),
    }),
});

export const { useGetProductDetailsQuery, useGetFilteredProductsMutation } = productApi;
