import { lazy } from 'react';
import { layouts } from '.';

const Index = lazy(() => import('../pages/Index/Index'));

const routes: { path: string; element: any; layout: keyof typeof layouts }[] = [
    // dashboard
    {
        path: '/',
        element: <Index />,
        layout: 'sidebar',
    },
];

export { routes };
