import { useState } from 'react';
import SearchBar from '../../components/Index/SearchBar';
import SearchResults from '../../components/Index/SearchResults';
import Loader from '../../components/Loader';
import useIndexVM from './indexVM';
const Index = () => {
    const { incrementCurrentPage, decrementCurrentPage, loading, error, displayedProducts, currentPage, searchKeyword, setSearchKeyword } = useIndexVM();
    if (error && error?.status === 400)
        return (
            <div className="container">
                <SearchBar searchKeyword={searchKeyword} setSearchKeyword={setSearchKeyword} />
                <p>Invalid Request</p>
            </div>
        );
    if (error) return <>Something went wrong</>;
    return (
        <div className="container">
            <SearchBar searchKeyword={searchKeyword} setSearchKeyword={setSearchKeyword} />
            <h1 className="font-bold text-xl my-4">Products</h1>
            {loading ? (
                <div className="flex justify-center">
                    <Loader />{' '}
                </div>
            ) : (
                displayedProducts && <SearchResults products={displayedProducts} />
            )}
            <div className="flex">
                <button className="btn rounded-full" onClick={decrementCurrentPage}>
                    {' '}
                    &lt;
                </button>
                <button className="btn btn-primary rounded-full">{currentPage + 1}</button>
                <button className="btn rounded-full" onClick={incrementCurrentPage}>
                    {' '}
                    &gt;
                </button>
            </div>
        </div>
    );
};

export default Index;
