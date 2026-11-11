import type { AxiosError } from "axios";

// inventory errors are RFC 9457 ProblemDetail ({ title, detail }); the gateway's own errors use { error }
type ApiErrorBody = {
    detail?: string;
    title?: string;
    message?: string;
    error?: string;
};

export function getApiErrorMessage(err: unknown, fallback = "Something went wrong") {
    const data = (err as AxiosError<ApiErrorBody | string>)?.response?.data;

    if (typeof data === "string" && data.trim()) return data;
    if (data && typeof data === "object") {
        if (data.detail) return data.detail;
        if (data.title) return data.title;
        if (data.message) return data.message;
        if (data.error) return data.error;
    }

    return fallback;
}
