export interface Product {
    mainCategory: string;
    title: string;
    averageRating: number;
    ratingNumber: number;
    features: string[];
    description: string[];
    price: number | null;
    images:
        | {
              thumb: string;
              large: string;
              variant: string;
              hi_res: string | null;
          }[]
        | null;
    videos: [];
    store: string;
    categories: string[];
    details: object;
    parentAsin: string;
    boughtTogether: string | null;
}

export interface PaginatedSearchResponse {
    content: Product[];
    pageable: {
        pageNumber: number;
    };
    totalPages: number;
}

export interface Review {
    rating: number;
    title: string;
    text: string;
    images: Record<string, string>[];
    asin: string;
    parent_asin: string;
    user_id: string;
    timestamp: number;
    helpful_vote: number;
    verified_purchase: boolean;
}

export interface MetricSchema {
    type: string;
    dataUrl: string;
    id: number;
    title: string;
    description: string;
}

export interface ProductDetailsWithReviewsResponse {
    product: Product;
    reviews: Review[];
}
