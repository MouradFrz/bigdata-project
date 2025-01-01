import React from 'react';
import { Product } from '../../types';
import { displayStars } from '../Index/ProductCard';

function truncateText(text: string, maxLength: number): string {
    if (text.length > maxLength) {
        return text.substring(0, maxLength) + '...';
    }
    return text;
}

function ProductCard({ product }: { product: Product }) {
    return (
        <div className="flex flex-col justify-between dark:bg-black shadow-md p-4 h-[calc(100%-2rem)]  mb-10 bg-white">
            <div>
                <img src={product.images[0].large} alt="" className=" w-full aspect-square" />
                <h1 className="mt-2">{truncateText(product.title, 70)}</h1>
            </div>
            <div className="">
                <div className="flex mt-2 w-full">{displayStars(product.averageRating)}</div>
                <p className="text-xl text-red-600 font-extrabold w-full mt-2">{product.price ? product.price + ' $' : 'Price unavailable'} </p>
            </div>
        </div>
    );
}

export default ProductCard;
