import { http } from "./http";
import type { LineStatus } from "./kitchen.api";

export type SessionStatus = "OPEN" | "CLOSED";

// a waiter confirms that a new guest really sits at the table; until then their orders are held
export type GuestStatus = "PENDING" | "APPROVED" | "REJECTED";

export type TableSessionResponseDto = {
    sessionId: string;
    tableId: number;
    tableLabel: string;
    status: SessionStatus;
    openedAt: string;
    closedAt?: string | null;
    // guests waiting for a waiter to confirm them
    pendingGuests: number;
};

export type OrderLineResponseDto = {
    lineId: number;
    itemId: number;
    itemName: string;
    itemType: "MEAL" | "DRINK";
    quantity: number;
    unitPrice: number;
    lineTotal: number;
    status: LineStatus;
    cancelReason?: string | null;
};

export type OrderResponseDto = {
    orderId: string;
    clientOrderId: string;
    participantId: string;
    // AWAITING_APPROVAL: the guest's first orders, held until a waiter confirms the guest
    status: "PLACED" | "CANCELLED" | "AWAITING_APPROVAL";
    stockStatus: "PENDING" | "WRITTEN_OFF" | "FAILED";
    total: number;
    createdAt: string;
    lines: OrderLineResponseDto[];
};

export type ParticipantBillResponseDto = {
    participantId: string;
    displayName?: string | null;
    status: GuestStatus;
    total: number;
    orders: OrderResponseDto[];
};

export type SessionBillResponseDto = {
    sessionId: string;
    tableLabel: string;
    status: SessionStatus;
    total: number;
    participants: ParticipantBillResponseDto[];
};

export async function getOpenSessions(): Promise<TableSessionResponseDto[]> {
    const res = await http.get<TableSessionResponseDto[]>("/api/v1/table-sessions");
    return res.data;
}

export async function getSessionBill(sessionId: string): Promise<SessionBillResponseDto> {
    const res = await http.get<SessionBillResponseDto>(`/api/v1/table-sessions/${sessionId}/bill`);
    return res.data;
}

export async function approveGuest(sessionId: string, participantId: string): Promise<ParticipantBillResponseDto> {
    const res = await http.post<ParticipantBillResponseDto>(`/api/v1/table-sessions/${sessionId}/participants/${participantId}/approve`);
    return res.data;
}

export async function rejectGuest(sessionId: string, participantId: string): Promise<ParticipantBillResponseDto> {
    const res = await http.post<ParticipantBillResponseDto>(`/api/v1/table-sessions/${sessionId}/participants/${participantId}/reject`);
    return res.data;
}

export async function closeSession(sessionId: string): Promise<TableSessionResponseDto> {
    const res = await http.post<TableSessionResponseDto>(`/api/v1/table-sessions/${sessionId}/close`);
    return res.data;
}
