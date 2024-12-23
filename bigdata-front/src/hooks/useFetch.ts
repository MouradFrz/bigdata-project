//useFetch.js
import { useState, useEffect } from 'react';
import axios, { AxiosError, AxiosResponse } from 'axios';

function useFetch<T>(url: string, method: string, body?: object, deps?: any[]) {
    const [data, setData] = useState<T | null>(null);
    const [loading, setLoading] = useState<boolean | null>(null);
    const [error, setError] = useState<AxiosError | null>(null);

    useEffect(() => {
        setLoading(true);
        setData(null);
        setError(null);
        // const source = axios.CancelToken.source();
        if (method === 'post') {
            axios
                .post<any, AxiosResponse<T>>(url, body)
                .then((res) => {
                    setError(null);
                    setData(res.data);
                })
                .catch((err: AxiosError) => {
                    setError(err);
                })
                .finally(() => {
                    setLoading(false);
                });
            return;
        }
        axios
            .get<any, AxiosResponse<T>>(url)
            .then((res) => {
                setError(null);
                setData(res.data);
            })
            .catch((err: AxiosError) => {
                setError(err);
            })
            .finally(() => {
                setLoading(false);
            });
    }, [...(deps ?? [])]);

    return { data, loading, error };
}

export default useFetch;
