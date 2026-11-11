import axios from "axios";
import { getCurrentTenantId } from "../tenant/currentTenant";

export const http = axios.create({
    baseURL: "",
    headers: { "Content-Type": "application/json" },
});


http.interceptors.request.use((config) => {
    const tenantId = getCurrentTenantId();
    if (tenantId) {
        config.headers = config.headers ?? {};
        (config.headers as any)["X-Tenant-Id"] = tenantId;
    }
    return config;
});

export default http;