import ProductCard from './ProductCard';
import { Product } from '../../types';
function SearchResults({ products }: { products: Product[] }) {
    return (
        <div>
            {products?.map((product) => (
                <ProductCard product={product} />
            ))}
        </div>
    );
}

export default SearchResults;
