import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';

export const statsApi = createApi({
    reducerPath: 'statsApi',
    baseQuery: fetchBaseQuery({ baseUrl: 'http://localhost:9999/api/v1' }),
    endpoints: (builder) => ({
        getTopRated: builder.query<{ title: string; averageRating: number; ratingNumber: number }[], void>({
            query: () => `/products/top-rated`,
        }),
        getRatingDistribution: builder.query<{ rating: number; count: number }[], void>({
            query: () => `/products/rating-distribution`,
        }),
        getReviewTimeline: builder.query<{ month: string; count: number }[], void>({
            query: () => `/products/review-timeline`,
        }),
    }),
});

export const { useGetTopRatedQuery, useGetRatingDistributionQuery, useGetReviewTimelineQuery } = statsApi;
