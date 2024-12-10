import SearchBar from '../../components/Index/SearchBar';
import SearchResults from '../../components/Index/SearchResults';
import Loader from '../../components/Loader';
import useIndexVM from './indexVM';
const Index = () => {
    const { incrementCurrentPage, decrementCurrentPage, data, loading, error, displayedProducts, currentPage } = useIndexVM();
    if (!error)
        return (
            <div className="container">
                <SearchBar />
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
    return <>Something went wrong...</>;
};

export default Index;
