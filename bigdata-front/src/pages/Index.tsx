import SearchBar from '../components/Index/SearchBar';
import SearchResults from '../components/Index/SearchResults';

const Index = () => {
    return (
        <div className="container">
            <SearchBar />
            <h1 className="font-bold text-xl my-4">Products</h1>
            <SearchResults />
        </div>
    );
};

export default Index;
