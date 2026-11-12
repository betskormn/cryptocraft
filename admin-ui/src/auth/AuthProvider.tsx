import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import type { AxiosError } from "axios";
import { Alert, Box, Button, CircularProgress, Stack, Typography } from "@mui/material";

import { getMe, getMyVenues } from "../api/auth.api";
import type { TenantResponseDto } from "../api/tenants.api";
import { getCurrentTenantId, setCurrentTenantId } from "../tenant/currentTenant";
import { roleCan, type Area } from "./access";
import { AuthContext, type Auth } from "./AuthContext";
import { canStartSignIn, signIn, takeReturnPath } from "./session";

// queries that are not data of one venue
const PERSON_QUERIES = ["me", "venues"];

function isUnauthorized(error: unknown): boolean {
    return (error as AxiosError | null)?.response?.status === 401;
}

// the stored venue if the person still has it, else their first active one
function pickVenue(venues: TenantResponseDto[], storedId: string | null): TenantResponseDto | null {
    return (
        venues.find((v) => String(v.tenantId) === storedId) ??
        venues.find((v) => v.status === "ACTIVE") ??
        venues[0] ??
        null
    );
}

/**
 * Nothing of the admin is shown before the person is signed in: without a session the browser goes to the
 * sign-in page and comes back to where it was.
 */
export default function AuthProvider({ children }: { children: ReactNode }) {
    const queryClient = useQueryClient();
    const navigate = useNavigate();
    // bumped on a venue switch: the venue itself lives outside React (X-Tenant-Id of every request)
    const [, setVenueVersion] = useState(0);

    const meQuery = useQuery({ queryKey: ["me"], queryFn: getMe, retry: false, staleTime: 5 * 60_000 });
    const venuesQuery = useQuery({ queryKey: ["venues"], queryFn: getMyVenues, enabled: meQuery.isSuccess, retry: false });

    // read once on load: a sign-in that just came back without a session (our own redirect below does not count)
    const [returnedWithoutSession] = useState(() => !canStartSignIn());
    const unauthorized = isUnauthorized(meQuery.error);
    const signInLoop = unauthorized && returnedWithoutSession;

    useEffect(() => {
        if (unauthorized && canStartSignIn()) signIn();
    }, [unauthorized]);

    // back from the sign-in page: return to the page the person was on
    useEffect(() => {
        if (!meQuery.isSuccess) return;
        const path = takeReturnPath();
        if (path) navigate(path, { replace: true });
    }, [meQuery.isSuccess, navigate]);

    const switchVenue = useCallback(
        async (tenantId: number) => {
            setCurrentTenantId(String(tenantId));
            setVenueVersion((v) => v + 1);
            // every other query is venue data fetched with the previous X-Tenant-Id
            await queryClient.resetQueries({ predicate: (q) => !PERSON_QUERIES.includes(String(q.queryKey[0])) });
        },
        [queryClient]
    );

    const me = meQuery.data;
    const venues = useMemo(() => venuesQuery.data ?? [], [venuesQuery.data]);
    const venue = pickVenue(venues, getCurrentTenantId());
    // keep requests on a venue the person may open (the stored one may have been taken away)
    if (venue && String(venue.tenantId) !== getCurrentTenantId()) {
        setCurrentTenantId(String(venue.tenantId));
    }

    if (signInLoop) {
        return (
            <Centered>
                <Alert severity="warning">Signing in did not complete. Cookies may be blocked for this site.</Alert>
                <Button variant="contained" onClick={() => signIn(true)}>
                    Try again
                </Button>
            </Centered>
        );
    }
    if (meQuery.isError && !unauthorized) {
        return (
            <Centered>
                <Alert severity="error">Cannot reach the sign-in service</Alert>
                <Button variant="outlined" onClick={() => void meQuery.refetch()}>
                    Retry
                </Button>
            </Centered>
        );
    }
    if (!me || venuesQuery.isPending) {
        return (
            <Centered>
                <CircularProgress />
                <Typography color="text.secondary">{unauthorized ? "Opening the sign-in page…" : "Loading…"}</Typography>
            </Centered>
        );
    }

    const role = me.platformAdmin
        ? "ADMIN"
        : (me.venues.find((v) => v.tenantId === venue?.tenantId)?.role ?? null);
    const auth: Auth = {
        me,
        venues,
        venue,
        role,
        can: (area: Area) => roleCan(role, area),
        switchVenue,
    };

    return <AuthContext.Provider value={auth}>{children}</AuthContext.Provider>;
}

function Centered({ children }: { children: ReactNode }) {
    return (
        <Box sx={{ minHeight: "100vh", display: "grid", placeItems: "center", p: 2 }}>
            <Stack spacing={2} alignItems="center">
                {children}
            </Stack>
        </Box>
    );
}
