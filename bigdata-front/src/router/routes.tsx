import { lazy } from 'react';
import { layouts } from '.';
import Metric from '../pages/Metric/Metric';

const Index = lazy(() => import('../pages/Index/Index'));
const Product = lazy(() => import('../pages/Product/Product'));
const AnalyticsReferencePage = lazy(() => import('../pages/AnalyticsReferencePage/AnalyticsReferencePage'));

const routes: { path: string; element: any; layout: keyof typeof layouts }[] = [
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
    {
        path: '/analytics',
        element: <AnalyticsReferencePage />,
        layout: 'sidebar',
    },
    {
        path: '/metric/:metricId',
        element: <Metric />,
        layout: 'sidebar',
    },
];

export { routes };
