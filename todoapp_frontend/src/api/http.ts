import axios, { AxiosInstance } from "axios";

const http: AxiosInstance = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URI || '/api',
    timeout: 10000
});

http.interceptors.response.use(
    (res) => res,
    (err) => {
        const resp = err?.response;
        if (resp && resp.data) (err as any).pretty = resp.data;
        return Promise.reject(err);
    }
);

export default http;