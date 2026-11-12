import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Box, Button, IconButton, MenuItem, Select, Tooltip, Typography } from "@mui/material";
import AddBusinessOutlinedIcon from "@mui/icons-material/AddBusinessOutlined";
import LiveTvOutlinedIcon from "@mui/icons-material/LiveTvOutlined";

import { createTenant, type TenantRequestDto } from "../api/tenants.api";
import { getApiErrorMessage } from "../api/error";
import { useAuth } from "../auth/AuthContext";
import TenantCreateDialog from "./TenantCreateDialog";

// empty = same origin: Vite (dev) and nginx (prod) proxy /tv.html and /ws to the gateway
const TV_BASE_URL = (import.meta.env.VITE_TV_BASE_URL as string | undefined) ?? "";

// the venues of the signed-in person; only a platform admin creates new ones
export default function TenantSwitcher() {
    const queryClient = useQueryClient();
    const { me, venues, venue: current, switchVenue } = useAuth();
    const [createOpen, setCreateOpen] = useState(false);
    const [createError, setCreateError] = useState<string | null>(null);

    const createMutation = useMutation({
        mutationFn: (dto: TenantRequestDto) => createTenant(dto),
        onSuccess: async (created) => {
            setCreateOpen(false);
            setCreateError(null);
            await queryClient.invalidateQueries({ queryKey: ["venues"] });
            await switchVenue(created.tenantId);
        },
        onError: (err) => setCreateError(getApiErrorMessage(err, "Failed to create venue")),
    });

    return (
        <Box sx={{ display: "flex", alignItems: "center", gap: 1, ml: "auto" }}>
            <Typography variant="body2" sx={{ opacity: 0.7, display: { xs: "none", md: "block" } }}>
                Venue
            </Typography>
            <Select
                size="small"
                value={current ? String(current.tenantId) : ""}
                displayEmpty
                onChange={(e) => void switchVenue(Number(e.target.value))}
                renderValue={() => (current ? current.name : "No venue")}
                sx={{ minWidth: { xs: 140, sm: 200 } }}
            >
                {venues.map((t) => (
                    <MenuItem key={t.tenantId} value={String(t.tenantId)} disabled={t.status === "SUSPENDED"}>
                        <Box>
                            <Typography variant="body2">{t.name}</Typography>
                            <Typography variant="caption" sx={{ opacity: 0.6 }}>
                                {t.slug}
                                {t.status !== "ACTIVE" ? ` · ${t.status.toLowerCase()}` : ""}
                            </Typography>
                        </Box>
                    </MenuItem>
                ))}
            </Select>

            <Tooltip title={current ? `Open the TV price board of ${current.name}` : "Select a venue first"}>
                <span>
                    <IconButton
                        disabled={!current || current.status !== "ACTIVE"}
                        href={current ? `${TV_BASE_URL}/tv.html?venue=${encodeURIComponent(current.slug)}` : ""}
                        target="_blank"
                        rel="noopener"
                    >
                        <LiveTvOutlinedIcon />
                    </IconButton>
                </span>
            </Tooltip>

            {me.platformAdmin ? (
                <Button
                    size="small"
                    variant="outlined"
                    startIcon={<AddBusinessOutlinedIcon />}
                    onClick={() => {
                        setCreateError(null);
                        setCreateOpen(true);
                    }}
                    sx={{ display: { xs: "none", sm: "inline-flex" } }}
                >
                    New venue
                </Button>
            ) : null}

            {createOpen && (
                <TenantCreateDialog
                    open
                    loading={createMutation.isPending}
                    errorText={createError}
                    onClose={() => setCreateOpen(false)}
                    onSubmit={(dto) => createMutation.mutate(dto)}
                />
            )}
        </Box>
    );
}
