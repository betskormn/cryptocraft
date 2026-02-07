export function norm(v: unknown) {
    return String(v ?? "").toLowerCase();
}

export function includes(v: unknown, q: string) {
    return norm(v).includes(q);
}