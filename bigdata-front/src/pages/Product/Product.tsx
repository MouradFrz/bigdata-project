import React from 'react';
import useProductVM from './ProductVM';
import { mockProducts } from '../../mockapi/mockProducts';
import Carousel from '../../components/Product/Carousel';

function Product() {
    const productData = mockProducts[0];
    return (
        <div>
            <div className="w-full flex gap-4">
                <img src={productData.images[0].large} className="w-[40%] h-[40%]" alt="" />
                <div className="w-[50%]">
                    <div className="bg-black mb-2 font-bold px-5 py-2 rounded-sm text-white-light w-fit">GYM</div>
                    <h1 className="font-extrabold text-2xl">{productData.title}</h1>
                    <h3 className="font-extrabold text-xl text-red-600 mb-3">{productData.price} $</h3>
                    <h3 className="font-bold mb-2 text-lg">A propos de cet article</h3>
                    <p className="text-lg max-w-[75%]">{productData.description}</p>
                </div>
            </div>
            <h2 className="text-3xl font-extrabold my-4">Produits Similaires</h2>
            <div>
                <Carousel products={mockProducts} />
            </div>
            <h2 className="text-3xl font-extrabold my-4">Avis client</h2>
        </div>
    );
}

export default Product;
