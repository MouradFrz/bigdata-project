export interface Product {
    main_category: string;
    title: string;
    averageRating: number;
    rating_number: number;
    features: string[];
    description: string[];
    price: number | null;
    images: {
        thumb: string;
        large: string;
        variant: string;
        hi_res: string | null;
    }[];
    videos: [];
    store: string;
    categories: string[];
    details: object;
    parent_asin: string;
    bought_together: string | null;
}

export interface PaginatedSearchResponse {
    content: Product[];
    pageable: {
        pageNumber: number;
    };
    totalPages: number;
}
