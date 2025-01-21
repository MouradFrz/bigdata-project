import React from 'react';
import { Product } from '../../types';
import { displayStars } from '../Index/ProductCard';
import { useNavigate } from 'react-router-dom';

export function truncateText(text: string, maxLength: number): string {
    if (!text) return '';
    if (text.length > maxLength) {
        return text.substring(0, maxLength) + '...';
    }
    return text;
}

function ProductCard({ product }: { product: Product }) {
    const navigate = useNavigate();
    return (
        <div key={product.parentAsin} className="flex flex-col justify-between dark:bg-black shadow-md p-4 h-[calc(100%-2rem)]  mb-10 bg-white">
            <div>
                {product.images ? <img src={product.images[0].large} alt="" className=" w-full aspect-square" /> : <div className="w-full aspect-square bg-gray-500 rounded-md"></div>}
                <h1 className="mt-2">{truncateText(product.title, 70)}</h1>
            </div>
            <div className="">
                <div className="flex mt-2 w-full">{displayStars(product.averageRating)}</div>
                <div className="flex justify-between items-center mt-2">
                    <p className="text-xl text-red-600 font-extrabold w-full ">{product.price ? product.price + ' $' : 'Price unavailable'} </p>
                    <button
                        onClick={() => {
                            navigate(`/product/${[product.parentAsin]}`);
                        }}
                        className="btn btn-sm btn-primary"
                    >
                        Details
                    </button>
                </div>
            </div>
        </div>
    );
}

export default ProductCard;
