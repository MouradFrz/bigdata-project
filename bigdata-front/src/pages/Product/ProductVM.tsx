import React from 'react';
import { useParams } from 'react-router-dom';

function useProductVM() {
    const { id: productId } = useParams();
    console.log(productId);
    return {};
}

export default useProductVM;
