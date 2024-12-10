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
        const source = axios.CancelToken.source();
        axios
            .get<any, AxiosResponse<T>>(url, { cancelToken: source.token })
            .then((res) => {
                setLoading(false);
                setError(null);
                setData(res.data);
            })
            .catch((err) => {
                setLoading(false);
                setError('An error occurred. Awkward..');
            });
        return () => {
            source.cancel();
        };
    }, [url]);

    return { data, loading, error };
}

export default useFetch;
