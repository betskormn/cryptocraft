import { http } from "./http";
import type { VenueRole } from "./auth.api";

export type StaffResponseDto = {
    userId: string;
    email: string;
    displayName: string;
    role: VenueRole;
    enabled: boolean;
};

// a new email creates an account with the initial password; an existing account just gets the venue
export type StaffRequestDto = {
    email: string;
    displayName?: string;
    role: VenueRole;
    password?: string;
};

export async function getStaff(): Promise<StaffResponseDto[]> {
    const res = await http.get<StaffResponseDto[]>("/api/v1/staff");
    return res.data;
}

export async function addStaff(dto: StaffRequestDto): Promise<StaffResponseDto> {
    const res = await http.post<StaffResponseDto>("/api/v1/staff", dto);
    return res.data;
}

export async function changeStaffRole(userId: string, role: VenueRole): Promise<StaffResponseDto> {
    const res = await http.put<StaffResponseDto>(`/api/v1/staff/${userId}`, { role });
    return res.data;
}

export async function removeStaff(userId: string): Promise<void> {
    await http.delete(`/api/v1/staff/${userId}`);
}
