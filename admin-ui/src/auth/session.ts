// Signing in and out happens at the gateway, which talks to identity-service and keeps the tokens in its
// session; the page only has an HttpOnly session cookie and never sees a token.

const RETURN_KEY = "signIn.returnTo";
const STARTED_KEY = "signIn.startedAt";
// back from the sign-in page and still not signed in: stop instead of redirecting in a loop
const LOOP_GUARD_MS = 15_000;

function storage(): Storage | null {
    try {
        return window.sessionStorage;
    } catch {
        return null;
    }
}

// false right after a sign-in that did not give a session (e.g. cookies blocked)
export function canStartSignIn(): boolean {
    const startedAt = Number(storage()?.getItem(STARTED_KEY) ?? 0);
    return Date.now() - startedAt >= LOOP_GUARD_MS;
}

/**
 * Sends the browser to the sign-in page; it comes back to the page it was on.
 * Does nothing right after a failed attempt unless forced (a click on "Try again").
 */
export function signIn(force = false): void {
    if (!force && !canStartSignIn()) return;

    const s = storage();
    s?.setItem(STARTED_KEY, String(Date.now()));
    s?.setItem(RETURN_KEY, window.location.pathname + window.location.search);
    window.location.assign("/oauth2/authorization/identity");
}

// the page the person was on before signing in, once
export function takeReturnPath(): string | null {
    const s = storage();
    const path = s?.getItem(RETURN_KEY) ?? null;
    s?.removeItem(RETURN_KEY);
    s?.removeItem(STARTED_KEY);
    return path && path.startsWith("/") && !path.startsWith("//") ? path : null;
}

// a real form POST: the gateway ends its session, then identity-service's, then comes back here
export function signOut(): void {
    const form = document.createElement("form");
    form.method = "POST";
    form.action = "/logout";
    const csrf = document.createElement("input");
    csrf.type = "hidden";
    csrf.name = "_csrf";
    csrf.value = readCookie("XSRF-TOKEN") ?? "";
    form.appendChild(csrf);
    document.body.appendChild(form);
    form.submit();
}

function readCookie(name: string): string | null {
    const match = document.cookie.split("; ").find((c) => c.startsWith(name + "="));
    return match ? decodeURIComponent(match.slice(name.length + 1)) : null;
}
