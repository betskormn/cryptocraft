import { http } from "./http";

export type VenueTableResponseDto = {
    tableId: number;
    label: string;
    // token in the table's QR code
    code: string;
    active: boolean;
};

export type VenueTableRequestDto = {
    label: string;
    active?: boolean;
};

export async function getAllTables(): Promise<VenueTableResponseDto[]> {
    const res = await http.get<VenueTableResponseDto[]>("/api/v1/tables");
    return res.data;
}

export async function createTable(dto: VenueTableRequestDto): Promise<VenueTableResponseDto> {
    const res = await http.post<VenueTableResponseDto>("/api/v1/tables", dto);
    return res.data;
}

export async function updateTable(id: number, dto: VenueTableRequestDto): Promise<VenueTableResponseDto> {
    const res = await http.put<VenueTableResponseDto>(`/api/v1/tables/${id}`, dto);
    return res.data;
}
