import React from 'react';
import useProductVM from './ProductVM';
import { mockProducts } from '../../mockapi/mockProducts';
import { mockReviews } from '../../mockapi/mockReviews';
import Carousel from '../../components/Product/Carousel';
import Accordion from '../../components/Product/Accordion';
import Loader from '../../components/Loader';

function Product() {
    const { productWithReviews, error, recommendationsError, recommendations } = useProductVM();
    if (error) return <>Something went wrong</>;

    return (
        <div>
            {productWithReviews ? (
                <div className="w-full flex gap-4">
                    {productWithReviews.product.images ? (
                        <img src={productWithReviews.product.images[0].large} className="w-[40%] h-[40%]" alt="" />
                    ) : (
                        <div className='className="w-[40%] h-[40%] bg-gray-500 rounded-md'></div>
                    )}
                    <div className="w-[50%]">
                        <div className="bg-black mb-2 font-bold px-5 py-2 rounded-sm text-white-light w-fit">{productWithReviews.product.mainCategory}</div>
                        <h1 className="font-extrabold text-2xl">{productWithReviews.product.title}</h1>
                        <h3 className="font-extrabold text-xl text-red-600 mb-3">{productWithReviews.product.price ? `${productWithReviews.product.price} $` : 'Price unavailable'}</h3>
                        <h3 className="font-bold mb-2 text-lg">A propos de cet article</h3>
                        <p className="text-lg max-w-[75%]">{productWithReviews.product.description}</p>
                    </div>
                </div>
            ) : (
                <div className="flex justify-center">
                    <Loader />{' '}
                </div>
            )}
            <h2 className="text-3xl font-extrabold my-4">Produits Similaires</h2>
            {recommendationsError ? (
                <p>Could not fetch recommendations</p>
            ) : (
                <div>
                    {recommendations ? (
                        <Carousel products={recommendations.recommendations} />
                    ) : (
                        <div className="flex justify-center">
                            <Loader />{' '}
                        </div>
                    )}
                </div>
            )}

            <h2 className="text-3xl font-extrabold my-4">Avis client</h2>
            {productWithReviews ? (
                <div>
                    <Accordion reviews={productWithReviews.reviews} />
                </div>
            ) : (
                <div className="flex justify-center">
                    <Loader />{' '}
                </div>
            )}
        </div>
    );
}

export default Product;
