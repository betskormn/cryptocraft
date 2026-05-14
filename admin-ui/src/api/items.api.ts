// src/api/items.api.ts
import { http } from "./http";
import type { ProductResponseDto } from "./products.api";

export const TYPE_OPTIONS = ["MEAL", "DRINK"] as const;
export type Type = (typeof TYPE_OPTIONS)[number];

export const TYPE_LABEL: Record<Type, string> = {
    MEAL: "Meal",
    DRINK: "Drink",
};

export type ItemRequestDto = {
    name: string;
    recipe?: string | null;
    image?: string | null;
    type: Type;
    products: Array<{
        productId: number;
        quantity: number;
    }>;
};

export type ItemResponseDto = {
    itemId: number;
    name: string;
    recipe?: string | null;
    image?: string | null;
    type: string;
    basePrice: number;
    minPrice: number;
    maxPrice: number;
    products: ItemProductResponseDto[];
};


export type ItemProductResponseDto = {
    productId: number;
    name: string;
    quantity: number;
};


export async function getAllItems(): Promise<ItemResponseDto[]> {
    const res = await http.get<ItemResponseDto[]>("/api/v1/items");
    return res.data;
}

export async function createItem(dto: ItemRequestDto): Promise<ItemResponseDto> {
    const res = await http.post<ItemResponseDto>("/api/v1/items", dto);
    return res.data;
}

export async function updateItem(id: number, dto: ItemRequestDto): Promise<ItemResponseDto> {
    const res = await http.put<ItemResponseDto>(`/api/v1/items/${id}`, dto);
    return res.data;
}

export async function deleteItem(id: number): Promise<void> {
    await http.delete(`/api/v1/items/${id}`);
}