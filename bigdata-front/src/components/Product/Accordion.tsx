import React, { useCallback } from 'react';
import AnimateHeight from 'react-animate-height';
import { useState } from 'react';
import { Review } from '../../types';
import { displayStars } from '../Index/ProductCard';
import Checkmark from '../../../public/checkmark.svg';
function Accordion({ reviews }: { reviews: Review[] }) {
    const [active1, setActive1] = useState<string>('0');
    const [displayedReviewsCount, setDisplayedReviewsCount] = useState<number>(5);
    const togglePara1 = (value: string) => {
        setActive1((oldValue) => {
            return oldValue === value ? '' : value;
        });
    };
    const showMoreReviews = useCallback(
        (incrementBy: number) => {
            setDisplayedReviewsCount((prev) => prev + incrementBy);
        },
        [displayedReviewsCount]
    );
    return (
        <div className="mb-5">
            {reviews.slice(0, displayedReviewsCount).map((review, index) => {
                return (
                    <div key={index} className="border border-[#d3d3d3] dark:border-[#3b3f5c] rounded font-semibold">
                        <div className="border-b h-min border-[#d3d3d3] dark:border-[#3b3f5c]">
                            <button type="button" className={` p-4 w-full flex items-center text-dark dark:text-dark-light`} onClick={() => togglePara1(String(index))}>
                                <div className="flex justify-between w-full">
                                    <div className="flex">
                                        <p>{review.title}</p>
                                        <span className="flex ms-4">{displayStars(review.rating)}</span>
                                    </div>
                                    <div>{review.verified_purchase && <img className="w-5" src={Checkmark} />}</div>
                                </div>
                                <div className={` ltr:ml-auto rtl:mr-auto`}></div>
                            </button>
                            <div>
                                <AnimateHeight duration={300} height={active1 === String(index) ? 'auto' : 0}>
                                    <div className="space-y-2 p-4 text-white-dark text-[13px]">
                                        <p>{review.text}</p>
                                    </div>
                                </AnimateHeight>
                            </div>
                        </div>
                    </div>
                );
            })}
            <button className="btn shadow-none w-full" disabled={displayedReviewsCount >= reviews.length} onClick={() => showMoreReviews(10)}>
                Voir plus de commentaires
            </button>
        </div>
    );
}

export default Accordion;
