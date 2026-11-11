import { http } from "./http";

export type BrandResponseDto = {
    brandId: number;
    name: string;
};

export type BrandRequestDto = {
    name: string;
};

export async function getAllBrands(): Promise<BrandResponseDto[]> {
    const res = await http.get<BrandResponseDto[]>("/api/v1/brands");
    return res.data;
}

export async function createBrand(dto: BrandRequestDto): Promise<BrandResponseDto> {
    const res = await http.post<BrandResponseDto>("/api/v1/brands", dto);
    return res.data;
}

export async function updateBrand(id: number, dto: BrandRequestDto): Promise<BrandResponseDto> {
    const res = await http.put<BrandResponseDto>(`/api/v1/brands/${id}`, dto);
    return res.data;
}

export async function deleteBrand(id: number): Promise<void> {
    await http.delete(`/api/v1/brands/${id}`);
}