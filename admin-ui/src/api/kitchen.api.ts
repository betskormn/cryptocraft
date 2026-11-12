import { http } from "./http";

export type Station = "MEAL" | "DRINK";

export type LineStatus = "NEW" | "PREPARING" | "READY" | "SERVED" | "CANCELLED";

export const IN_WORK: LineStatus[] = ["NEW", "PREPARING", "READY"];

export type KitchenLineResponseDto = {
    lineId: number;
    orderId: string;
    tableLabel: string;
    guestName?: string | null;
    itemId: number;
    itemName: string;
    itemType: Station;
    quantity: number;
    status: LineStatus;
    cancelReason?: string | null;
    placedAt: string;
    statusChangedAt?: string | null;
};

export async function getQueue(station: Station): Promise<KitchenLineResponseDto[]> {
    const res = await http.get<KitchenLineResponseDto[]>("/api/v1/kitchen/lines", { params: { itemType: station } });
    return res.data;
}

export async function changeLineStatus(lineId: number, status: LineStatus, reason?: string): Promise<KitchenLineResponseDto> {
    const res = await http.patch<KitchenLineResponseDto>(`/api/v1/kitchen/lines/${lineId}/status`, { status, reason });
    return res.data;
}
