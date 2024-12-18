import { lazy } from 'react';
import { layouts } from '.';

const Index = lazy(() => import('../pages/Index/Index'));
const Product = lazy(() => import('../pages/Product/Product'));

const routes: { path: string; element: any; layout: keyof typeof layouts }[] = [
    // dashboard
    {
        path: '/',
        element: <Index />,
        layout: 'sidebar',
    },
    {
        path: '/product/:id',
        element: <Product />,
        layout: 'sidebar',
    },
];

export { routes };
