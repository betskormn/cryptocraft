import { http } from "./http";
import type { TenantResponseDto } from "./tenants.api";

export type VenueRole = "ADMIN" | "INVENTORY_MANAGER" | "STAFF";

export const ROLE_LABEL: Record<VenueRole, string> = {
    ADMIN: "Admin",
    INVENTORY_MANAGER: "Inventory manager",
    STAFF: "Staff",
};

export const ROLE_HINT: Record<VenueRole, string> = {
    ADMIN: "Everything in the venue, including staff and tables",
    INVENTORY_MANAGER: "Menu, recipes and stock",
    STAFF: "Visits, bills, kitchen and bar screens, stop-list",
};

export type MeResponseDto = {
    userId: string;
    email: string;
    displayName: string;
    platformAdmin: boolean;
    venues: Array<{ tenantId: number; role: VenueRole }>;
};

export type PasswordChangeRequestDto = {
    currentPassword: string;
    newPassword: string;
};

// identity-service: the signed-in person and their role in each venue
export async function getMe(): Promise<MeResponseDto> {
    const res = await http.get<MeResponseDto>("/api/v1/me");
    return res.data;
}

export async function changePassword(dto: PasswordChangeRequestDto): Promise<void> {
    await http.put("/api/v1/me/password", dto);
}

// inventory-service: names of the venues the person works in (all of them for a platform admin)
export async function getMyVenues(): Promise<TenantResponseDto[]> {
    const res = await http.get<TenantResponseDto[]>("/api/v1/venues");
    return res.data;
}
