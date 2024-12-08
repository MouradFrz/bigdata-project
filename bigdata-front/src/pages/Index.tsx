import SearchBar from '../components/Index/SearchBar';
import SearchResults from '../components/Index/SearchResults';

const Index = () => {
    return (
        <div className="container">
            <SearchBar />
            <h1 className="font-bold text-xl my-4">Products</h1>
            <SearchResults />
            <div className="flex">
                <button className="btn rounded-full"> &lt;</button>
                <button className="btn btn-primary rounded-full">1</button>
                <button className="btn rounded-full"> &gt;</button>
            </div>
        </div>
    );
};

export default Index;
