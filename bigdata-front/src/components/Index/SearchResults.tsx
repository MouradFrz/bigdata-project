import ProductCard from './ProductCard';
import { mockProducts } from '../../mockapi/mockProducts';
function SearchResults() {
    return (
        <div>
            {mockProducts.map((product) => (
                <ProductCard product={product} />
            ))}
        </div>
    );
}

export default SearchResults;
