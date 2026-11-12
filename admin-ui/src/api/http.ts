import axios from "axios";
import { getCurrentTenantId } from "../tenant/currentTenant";
import { signIn } from "../auth/session";

// same origin: the gateway's session cookie authenticates every call; axios sends the XSRF-TOKEN cookie
// back as X-XSRF-TOKEN by itself
export const http = axios.create({
    baseURL: "",
    headers: { "Content-Type": "application/json" },
});


http.interceptors.request.use((config) => {
    const tenantId = getCurrentTenantId();
    if (tenantId) {
        config.headers.set("X-Tenant-Id", tenantId);
    }
    return config;
});

// the session ended (signed out elsewhere, expired, account disabled): sign in again
http.interceptors.response.use(undefined, (error) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
        signIn();
    }
    return Promise.reject(error);
});

export default http;
