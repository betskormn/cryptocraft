import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { Alert } from "@mui/material";

import { useAuth } from "./AuthContext";
import { HOME, type Area } from "./access";

// a page the person's role in this venue cannot use (an old bookmark, a role change)
export function RequireArea({ area, children }: { area: Area; children: ReactNode }) {
    const { can } = useAuth();
    return can(area) ? children : <Alert severity="warning">Your role in this venue does not include this page.</Alert>;
}

// "/" opens the first part of the admin the role may use
export function HomeRedirect() {
    const { can } = useAuth();
    const area = (Object.keys(HOME) as Area[]).find((a) => can(a));
    return area ? <Navigate to={HOME[area]} replace /> : <Alert severity="info">Your role gives no pages in this venue yet.</Alert>;
}
