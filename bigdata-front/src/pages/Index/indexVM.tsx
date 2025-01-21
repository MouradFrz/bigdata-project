import React, { useState, useEffect } from 'react';
import { Product } from '../../types';
import { useGetFilteredProductsMutation } from '../../store/services/product';

function useIndexVM() {
    const [currentPage, setCurrentPage] = useState<number>(0);
    const [displayedProducts, setDisplayedProducts] = useState<Product[] | null>(null);
    const [searchKeyword, setSearchKeyword] = useState<string>('');
    const [getProducts, { data, isLoading: loading, error }] = useGetFilteredProductsMutation();
    useEffect(() => {
        getProducts({ request: searchKeyword, size: 20, page: currentPage });
    }, [searchKeyword, currentPage]);

    useEffect(() => {
        if (data) {
            setDisplayedProducts(data.content);
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
    return { incrementCurrentPage, decrementCurrentPage, data, loading, error, displayedProducts, currentPage, searchKeyword, setSearchKeyword };
}

export default useIndexVM;
