// Until there is a login, the admin works with the venue chosen in the switcher (kept in localStorage).
// ?tenant=<id> in the URL selects a venue once on load; VITE_TENANT_ID is the fallback.

const STORAGE_KEY = "tenantId";

// survives a blocked localStorage until the page reloads
let selected: string | null = null;

export function initTenantFromUrl(): void {
    const url = new URL(window.location.href);
    const fromQuery = url.searchParams.get("tenant");
    if (!fromQuery) return;

    setCurrentTenantId(fromQuery);
    url.searchParams.delete("tenant");
    window.history.replaceState(null, "", url);
}

export function getCurrentTenantId(): string | null {
    if (selected) return selected;

    try {
        const stored = localStorage.getItem(STORAGE_KEY);
        if (stored) return stored;
    } catch {
        // storage blocked: fall through to the env default
    }

    const envTid = import.meta.env.VITE_TENANT_ID as string | undefined;
    return envTid ? String(envTid) : null;
}

export function setCurrentTenantId(tenantId: string): void {
    selected = tenantId;
    try {
        localStorage.setItem(STORAGE_KEY, tenantId);
    } catch {
        // storage blocked: the in-memory choice is used
    }
}
