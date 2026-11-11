import { http } from "./http";

export const UNIT_OPTIONS = [
    "LITERS",
    "MILLILITERS",
    "BOTTLES",
    "GRAMS",
    "KILOGRAMS",
    "PORTION",
    "PIECE",
    "BOWL",
    "GLASS",
    "SHOT",
] as const;

export type Unit = (typeof UNIT_OPTIONS)[number];

export const UNIT_LABEL: Record<Unit, string> = {
    LITERS: "Liters (L)",
    MILLILITERS: "Milliliters (ml)",
    BOTTLES: "Bottles",
    GRAMS: "Grams (g)",
    KILOGRAMS: "Kilograms (kg)",
    PORTION: "Portion",
    PIECE: "Piece",
    BOWL: "Bowl",
    GLASS: "Glass",
    SHOT: "Shot",
};

export type ProductResponseDto = {
    productId: number;
    name: string;
    description: string;
    image?: string | null;
    // in the product's unit, up to 3 decimals
    quantityInStock: number;
    unit: Unit;
    brandId: number;
};

export type ProductRequestDto = {
    name: string;
    description: string;
    image?: string | null;
    quantity: number;
    unit: Unit;
    brandId: number;
};

// positive delta adds a delivery, negative writes stock off (never below zero)
export type AdjustStockRequestDto = {
    delta: number;
};

export async function getAllProducts(): Promise<ProductResponseDto[]> {
    const res = await http.get<ProductResponseDto[]>("/api/v1/products");
    return res.data;
}

export async function createProduct(dto: ProductRequestDto): Promise<ProductResponseDto> {
    const res = await http.post<ProductResponseDto>("/api/v1/products", dto);
    return res.data;
}

export async function updateProduct(id: number, dto: ProductRequestDto): Promise<ProductResponseDto> {
    const res = await http.put<ProductResponseDto>(`/api/v1/products/${id}`, dto);
    return res.data;
}

export async function deleteProduct(id: number): Promise<void> {
    await http.delete(`/api/v1/products/${id}`);
}

// atomic: PATCH /api/v1/products/{id}/stock with { delta }
export async function adjustProductStock(id: number, delta: number): Promise<ProductResponseDto> {
    const res = await http.patch<ProductResponseDto>(`/api/v1/products/${id}/stock`, { delta });
    return res.data;
}