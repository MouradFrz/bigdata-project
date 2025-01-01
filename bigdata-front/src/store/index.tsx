import { combineReducers, configureStore } from '@reduxjs/toolkit';
import themeConfigSlice from './themeConfigSlice';
import { productApi } from './services/product';
import { statsApi } from './services/stats';

const rootReducer = combineReducers({
    themeConfig: themeConfigSlice,
    [productApi.reducerPath]: productApi.reducer,
    [statsApi.reducerPath]: statsApi.reducer,
});

export default configureStore({
    reducer: rootReducer,
    middleware: (getDefaultMiddleware) => getDefaultMiddleware().concat(productApi.middleware).concat(statsApi.middleware),
});

export type IRootState = ReturnType<typeof rootReducer>;
