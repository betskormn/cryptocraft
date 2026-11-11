import { http } from "./http";

export type TenantStatus = "ACTIVE" | "SUSPENDED" | "TEMPLATE";

export type TenantResponseDto = {
    tenantId: number;
    name: string;
    slug: string;
    currency: string;
    timezone: string;
    status: TenantStatus;
    priceTickSeconds: number;
};

export type TenantRequestDto = {
    name: string;
    slug: string;
    currency?: string;
    timezone?: string;
    priceTickSeconds?: number;
    demoMenu?: boolean;
};

export async function getAllTenants(): Promise<TenantResponseDto[]> {
    const res = await http.get<TenantResponseDto[]>("/api/v1/platform/tenants");
    return res.data;
}

export async function createTenant(dto: TenantRequestDto): Promise<TenantResponseDto> {
    const res = await http.post<TenantResponseDto>("/api/v1/platform/tenants", dto);
    return res.data;
}
