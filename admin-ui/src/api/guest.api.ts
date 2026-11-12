import axios from "axios";

import type { GuestStatus, OrderResponseDto, ParticipantBillResponseDto } from "./sessions.api";

// A guest's own client: no staff session, the venue from the QR link and the guest token from joining.
// Same origin as the admin: Vite (dev) and nginx (prod) pass /api and /ws to the gateway.
export function guestHttp(tenantId: string, token: string | null) {
    const http = axios.create({ headers: { "Content-Type": "application/json", "X-Tenant-Id": tenantId } });
    if (token) http.defaults.headers.common["Authorization"] = `Bearer ${token}`;
    return http;
}

export type JoinTableResponseDto = {
    sessionId: string;
    participantId: string;
    tableId: number;
    tableLabel: string;
    status: GuestStatus;
    guestToken: string;
    tokenExpiresAt: string;
    venueName?: string | null;
    venueSlug?: string | null;
    currency?: string | null;
};

export type SellableItem = {
    itemId: number;
    name: string;
    type: "MEAL" | "DRINK";
    image?: string | null;
    displayOrder?: number | null;
    available: boolean;
    portions?: number | null;
};

export type PriceMessage = {
    itemId: number;
    price: number;
    stopped?: boolean;
};

export async function joinTable(tenantId: string, token: string | null, tableCode: string, displayName?: string): Promise<JoinTableResponseDto> {
    const res = await guestHttp(tenantId, token).post<JoinTableResponseDto>("/api/v1/table-sessions/join", {
        tableCode,
        displayName: displayName || undefined,
    });
    return res.data;
}

export async function getMenu(tenantId: string): Promise<SellableItem[]> {
    const res = await guestHttp(tenantId, null).get<SellableItem[]>("/api/v1/items/sellable");
    return res.data;
}

export async function sendOrder(
    tenantId: string,
    token: string,
    clientOrderId: string,
    lines: Array<{ itemId: number; quantity: number }>
): Promise<OrderResponseDto> {
    const res = await guestHttp(tenantId, token).post<OrderResponseDto>("/api/v1/orders", { clientOrderId, lines });
    return res.data;
}

export async function getMyBill(tenantId: string, token: string): Promise<ParticipantBillResponseDto> {
    const res = await guestHttp(tenantId, token).get<ParticipantBillResponseDto>("/api/v1/participants/me/bill");
    return res.data;
}

// live prices of the venue: the current price of every item on connect, then every change
export function priceSocketUrl(slug: string): string {
    const scheme = window.location.protocol === "https:" ? "wss" : "ws";
    return `${scheme}://${window.location.host}/ws/prices/${encodeURIComponent(slug)}`;
}
