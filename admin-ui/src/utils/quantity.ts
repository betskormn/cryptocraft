// Stock and recipe quantities: in the product's unit, up to 3 decimals (matches NUMERIC(12,3) in inventory).
// Form fields keep the raw text so values like "0.05" can be typed; parse on validation/submit.

const MAX_DECIMALS = 3;

export function parseQuantity(text: string, { allowZero = false } = {}): number | null {
    const trimmed = text.trim();
    if (!/^\d+(\.\d+)?$/.test(trimmed)) return null;

    const decimals = trimmed.split(".")[1]?.length ?? 0;
    if (decimals > MAX_DECIMALS) return null;

    const value = Number(trimmed);
    if (!Number.isFinite(value)) return null;
    return value > 0 || (allowZero && value === 0) ? value : null;
}

// adds a step without floating point noise (0.1 + 1 -> 1.1, not 1.1000000000000001)
export function addQuantity(value: number, step: number): number {
    return Math.round((value + step) * 10 ** MAX_DECIMALS) / 10 ** MAX_DECIMALS;
}

export function formatQuantity(value: number): string {
    return String(Number(value.toFixed(MAX_DECIMALS)));
}
