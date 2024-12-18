//useFetch.js
import { useState, useEffect } from 'react';
import axios, { AxiosResponse } from 'axios';

function useFetch<T>(url: string) {
    const [data, setData] = useState<T | null>(null);
    const [loading, setLoading] = useState<boolean | null>(null);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        setLoading(true);
        setData(null);
        setError(null);
        // const source = axios.CancelToken.source();
        axios
            .get<any, AxiosResponse<T>>(url)
            .then((res) => {
                setError(null);
                setData(res.data);
            })
            .catch((err) => {
                setError('An error occurred. Awkward..');
            })
            .finally(() => {
                setLoading(false);
            });
    }, [url]);

    return { data, loading, error };
}

export default useFetch;
