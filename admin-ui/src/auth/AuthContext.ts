import { createContext, useContext } from "react";

import type { MeResponseDto, VenueRole } from "../api/auth.api";
import type { TenantResponseDto } from "../api/tenants.api";
import type { Area } from "./access";

export type Auth = {
    me: MeResponseDto;
    // venues the person works in (all of them for a platform admin)
    venues: TenantResponseDto[];
    // the venue being worked in; its id goes out as X-Tenant-Id
    venue: TenantResponseDto | null;
    // role in that venue; a platform admin may do everything
    role: VenueRole | null;
    can: (area: Area) => boolean;
    switchVenue: (tenantId: number) => Promise<void>;
};

export const AuthContext = createContext<Auth | null>(null);

export function useAuth(): Auth {
    const auth = useContext(AuthContext);
    if (!auth) throw new Error("useAuth outside AuthProvider");
    return auth;
}
