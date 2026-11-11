import { http } from "./http";

export type SessionStatus = "OPEN" | "CLOSED";

export type TableSessionResponseDto = {
    sessionId: string;
    tableId: number;
    tableLabel: string;
    status: SessionStatus;
    openedAt: string;
    closedAt?: string | null;
};

export type OrderLineResponseDto = {
    itemId: number;
    itemName: string;
    itemType: "MEAL" | "DRINK";
    quantity: number;
    unitPrice: number;
    lineTotal: number;
};

export type OrderResponseDto = {
    orderId: string;
    clientOrderId: string;
    participantId: string;
    status: "PLACED" | "CANCELLED";
    stockStatus: "PENDING" | "WRITTEN_OFF";
    total: number;
    createdAt: string;
    lines: OrderLineResponseDto[];
};

export type ParticipantBillResponseDto = {
    participantId: string;
    displayName?: string | null;
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

export async function closeSession(sessionId: string): Promise<TableSessionResponseDto> {
    const res = await http.post<TableSessionResponseDto>(`/api/v1/table-sessions/${sessionId}/close`);
    return res.data;
}
