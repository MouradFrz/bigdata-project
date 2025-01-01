import { Swiper, SwiperSlide } from 'swiper/react';
import 'swiper/css';
import 'swiper/css/navigation';
import 'swiper/css/pagination';
import { Navigation, Pagination } from 'swiper/modules';
import { Product } from '../../types';
import ProductCard from './ProductCard';

function Carousel({ products }: { products: Product[] }) {
    return (
        <div className="swiper" id="slider5">
            <div className="swiper-wrapper">
                <Swiper
                    modules={[Navigation, Pagination]}
                    navigation={{
                        nextEl: '.swiper-button-next-ex5',
                        prevEl: '.swiper-button-prev-ex5',
                    }}
                    pagination={{
                        clickable: true,
                    }}
                    breakpoints={{
                        1024: {
                            slidesPerView: 4,
                            spaceBetween: 30,
                        },
                        768: {
                            slidesPerView: 2,
                            spaceBetween: 40,
                        },
                        320: {
                            slidesPerView: 1,
                            spaceBetween: 20,
                        },
                    }}
                    dir={'ltr'}
                    key={'false'}
                >
                    {products.map((product) => {
                        return (
                            <SwiperSlide key={product.parentAsin}>
                                <ProductCard product={product} />
                            </SwiperSlide>
                        );
                    })}
                </Swiper>
            </div>
            <button className="swiper-button-prev-ex5 grid place-content-center ltr:left-2 rtl:right-2 p-1 transition text-white border border-primary  hover:border-primary bg-primary  rounded-full absolute z-[999] top-[44%] aspect-square w-8 -translate-y-1/2">
                &lt;
            </button>
            <button className="swiper-button-next-ex5 grid place-content-center ltr:right-2 rtl:left-2 p-1 transition text-white border border-primary  hover:border-primary bg-primary rounded-full absolute z-[999] top-[44%] aspect-square w-8 -translate-y-1/2">
                &gt;
            </button>
        </div>
    );
}

export default Carousel;
