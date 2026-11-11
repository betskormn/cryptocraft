import type { AxiosError } from "axios";

export function getApiErrorMessage(err: unknown, fallback = "Something went wrong") {
    const e = err as AxiosError<any>;
    const data = e?.response?.data;

    if (data?.detail) return String(data.detail);
    if (data?.title) return String(data.title);
    // inventory's GlobalErrorResponseDto
    if (data?.description) return String(data.description);
    if (typeof data === "string") return data;

    return fallback;
}