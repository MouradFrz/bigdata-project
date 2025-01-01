import { combineReducers, configureStore } from '@reduxjs/toolkit';
import themeConfigSlice from './themeConfigSlice';
import { productApi } from './services/product';

const rootReducer = combineReducers({
    themeConfig: themeConfigSlice,
    [productApi.reducerPath]: productApi.reducer,
});

export default configureStore({
    reducer: rootReducer,
    middleware: (getDefaultMiddleware) => getDefaultMiddleware().concat(productApi.middleware),
});

export type IRootState = ReturnType<typeof rootReducer>;
