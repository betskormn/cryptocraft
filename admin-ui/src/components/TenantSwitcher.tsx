import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Box, Button, IconButton, MenuItem, Select, Tooltip, Typography } from "@mui/material";
import AddBusinessOutlinedIcon from "@mui/icons-material/AddBusinessOutlined";
import LiveTvOutlinedIcon from "@mui/icons-material/LiveTvOutlined";

import { createTenant, getAllTenants, type TenantRequestDto } from "../api/tenants.api";
import { getApiErrorMessage } from "../api/error";
import { getCurrentTenantId, setCurrentTenantId } from "../tenant/currentTenant";
import TenantCreateDialog from "./TenantCreateDialog";

const TENANTS_KEY = ["platform-tenants"];
const TV_BASE_URL = (import.meta.env.VITE_TV_BASE_URL as string | undefined) || "http://localhost:8083";

export default function TenantSwitcher() {
    const queryClient = useQueryClient();
    const [currentId, setCurrentId] = useState<string | null>(getCurrentTenantId());
    const [createOpen, setCreateOpen] = useState(false);
    const [createError, setCreateError] = useState<string | null>(null);

    const tenantsQuery = useQuery({ queryKey: TENANTS_KEY, queryFn: getAllTenants });
    const tenants = tenantsQuery.data ?? [];
    const current = tenants.find((t) => String(t.tenantId) === currentId);

    // every other query is venue data fetched with the previous X-Tenant-Id
    const switchTo = async (tenantId: string) => {
        setCurrentTenantId(tenantId);
        setCurrentId(tenantId);
        await queryClient.resetQueries({ predicate: (q) => q.queryKey[0] !== TENANTS_KEY[0] });
    };

    const createMutation = useMutation({
        mutationFn: (dto: TenantRequestDto) => createTenant(dto),
        onSuccess: async (created) => {
            setCreateOpen(false);
            setCreateError(null);
            await queryClient.invalidateQueries({ queryKey: TENANTS_KEY });
            await switchTo(String(created.tenantId));
        },
        onError: (err) => setCreateError(getApiErrorMessage(err, "Failed to create venue")),
    });

    return (
        <Box sx={{ display: "flex", alignItems: "center", gap: 1, ml: "auto" }}>
            <Typography variant="body2" sx={{ opacity: 0.7, display: { xs: "none", sm: "block" } }}>
                Venue
            </Typography>
            <Select
                size="small"
                value={current ? String(current.tenantId) : ""}
                displayEmpty
                onChange={(e) => switchTo(String(e.target.value))}
                renderValue={() => (current ? current.name : tenantsQuery.isLoading ? "Loading…" : "Select venue")}
                sx={{ minWidth: 200 }}
            >
                {tenants.map((t) => (
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

            <Button
                size="small"
                variant="outlined"
                startIcon={<AddBusinessOutlinedIcon />}
                onClick={() => {
                    setCreateError(null);
                    setCreateOpen(true);
                }}
            >
                New venue
            </Button>

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
