import type { VenueRole } from "../api/auth.api";

// parts of the admin; the services enforce the same rules, this only hides what a role cannot use
export type Area = "menu" | "tables" | "visits" | "kitchen" | "staff";

const AREAS: Record<VenueRole, Area[]> = {
    ADMIN: ["menu", "tables", "visits", "kitchen", "staff"],
    INVENTORY_MANAGER: ["menu"],
    STAFF: ["visits", "kitchen"],
};

export function roleCan(role: VenueRole | null, area: Area): boolean {
    return role !== null && AREAS[role].includes(area);
}

// where a role starts
export const HOME: Record<Area, string> = {
    menu: "/items",
    tables: "/tables",
    visits: "/visits",
    kitchen: "/kds/kitchen",
    staff: "/staff",
};
