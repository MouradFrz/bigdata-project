import { createBrowserRouter } from 'react-router-dom';
import BlankLayout from '../components/Layouts/BlankLayout';
import DefaultLayout from '../components/Layouts/DefaultLayout';
import { routes } from './routes';
import Sidebar from '../components/Layouts/Sidebar';
import SidebarLayout from '../components/Layouts/SidebarLayout';

export const layouts = {
    blank: BlankLayout,
    default: DefaultLayout,
    sidebar: Sidebar,
};

const finalRoutes = routes.map((route) => {
    return {
        ...route,
        element:
            route.layout === 'blank' ? (
                <BlankLayout>{route.element}</BlankLayout>
            ) : route.layout === 'sidebar' ? (
                <SidebarLayout>{route.element}</SidebarLayout>
            ) : (
                <DefaultLayout>{route.element}</DefaultLayout>
            ),
    };
});

const router = createBrowserRouter(finalRoutes);

export default router;
