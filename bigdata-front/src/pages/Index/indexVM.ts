import React, { useState, useEffect } from 'react';
import useFetch from '../../hooks/useFetch';
import { PaginatedSearchResponse, Product } from '../../types';
const searchEndpoint = 'http://localhost:9999/api/v1/products/search';

function useIndexVM() {
    const [currentPage, setCurrentPage] = useState<number>(0);
    const [displayedProducts, setDisplayedProducts] = useState<Product[] | null>(null);
    const { data, loading, error } = useFetch<PaginatedSearchResponse>(`${searchEndpoint}?page=${currentPage}`);
    useEffect(() => {
        if (data?.content) {
            console.log(data.content);
            setDisplayedProducts(data?.content);
        }
    }, [data]);

    const incrementCurrentPage = () => {
        setCurrentPage((prev) => {
            return prev + 1;
        });
    };
    const decrementCurrentPage = () => {
        setCurrentPage((prev) => {
            if (prev === 0) {
                return 0;
            }
            return prev - 1;
        });
    };
    return { incrementCurrentPage, decrementCurrentPage, data, loading, error, displayedProducts, currentPage };
}

export default useIndexVM;