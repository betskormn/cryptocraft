import axios from "axios";

export const http = axios.create({
    baseURL: "",
    headers: { "Content-Type": "application/json" },
});


function resolveTenantId(): string | null {
    const envTid = import.meta.env.VITE_TENANT_ID as string | undefined;
    if (envTid) return String(envTid);

    const params = new URLSearchParams(window.location.search);
    const fromQuery = params.get("tenant");
    if (fromQuery) {
        localStorage.setItem("tenantId", fromQuery);
        return fromQuery;
    }

    const fromLS = localStorage.getItem("tenantId");
    if (fromLS) return fromLS;

    return null;
}

http.interceptors.request.use((config) => {
    const tenantId = resolveTenantId();
    if (tenantId) {
        config.headers = config.headers ?? {};
        (config.headers as any)["X-Tenant-Id"] = tenantId;
    } else {
    }
    return config;
});

export default http;