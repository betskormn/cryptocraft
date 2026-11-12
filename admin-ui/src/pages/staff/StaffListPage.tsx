import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
    Alert,
    Box,
    Button,
    CircularProgress,
    IconButton,
    MenuItem,
    Paper,
    Select,
    Snackbar,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableRow,
    Tooltip,
    Typography,
} from "@mui/material";
import PersonRemoveOutlinedIcon from "@mui/icons-material/PersonRemoveOutlined";

import { ROLE_HINT, ROLE_LABEL, type VenueRole } from "../../api/auth.api";
import { addStaff, changeStaffRole, getStaff, removeStaff, type StaffRequestDto, type StaffResponseDto } from "../../api/staff.api";
import { getApiErrorMessage } from "../../api/error";
import { useAuth } from "../../auth/AuthContext";
import ConfirmDialog from "../../components/ConfirmDialog";
import StaffDialog from "../../components/StaffDialog";

export default function StaffListPage() {
    const queryClient = useQueryClient();
    const { me, venue } = useAuth();

    const [adding, setAdding] = useState(false);
    const [addError, setAddError] = useState<string | null>(null);
    const [removing, setRemoving] = useState<StaffResponseDto | null>(null);
    const [notice, setNotice] = useState<string | null>(null);

    const staffQuery = useQuery({ queryKey: ["staff"], queryFn: getStaff });
    const refresh = () => queryClient.invalidateQueries({ queryKey: ["staff"] });

    const addMutation = useMutation({
        mutationFn: (dto: StaffRequestDto) => addStaff(dto),
        onSuccess: async () => {
            setAdding(false);
            await refresh();
        },
        onError: (e) => setAddError(getApiErrorMessage(e, "Failed to add the person")),
    });

    const roleMutation = useMutation({
        mutationFn: ({ userId, role }: { userId: string; role: VenueRole }) => changeStaffRole(userId, role),
        onSuccess: refresh,
        onError: (e) => setNotice(getApiErrorMessage(e, "Failed to change the role")),
    });

    const removeMutation = useMutation({
        mutationFn: (userId: string) => removeStaff(userId),
        onSuccess: async () => {
            setRemoving(null);
            await refresh();
        },
    });

    if (staffQuery.isLoading) return <CircularProgress />;
    if (staffQuery.isError) return <Alert severity="error">{getApiErrorMessage(staffQuery.error, "Error loading staff")}</Alert>;

    const staff = staffQuery.data ?? [];

    return (
        <Box>
            <Stack direction="row" alignItems="center" justifyContent="space-between" mb={2} gap={2}>
                <Box>
                    <Typography variant="h5" fontWeight={800}>
                        Staff
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                        Who works in {venue?.name ?? "this venue"} and what they may do. A role change applies within minutes.
                    </Typography>
                </Box>
                <Button
                    variant="contained"
                    onClick={() => {
                        setAddError(null);
                        setAdding(true);
                    }}
                >
                    Add a person
                </Button>
            </Stack>

            <Paper variant="outlined" sx={{ overflowX: "auto" }}>
                <Table size="small">
                    <TableHead>
                        <TableRow>
                            <TableCell>Name</TableCell>
                            <TableCell>Email</TableCell>
                            <TableCell width={220}>Role</TableCell>
                            <TableCell width={80} align="right">
                                Actions
                            </TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {staff.map((p) => (
                            <TableRow key={p.userId} hover>
                                <TableCell sx={{ fontWeight: 700 }}>
                                    {p.displayName}
                                    {p.userId === me.userId ? (
                                        <Typography component="span" variant="caption" color="text.secondary">
                                            {" "}
                                            (you)
                                        </Typography>
                                    ) : null}
                                </TableCell>
                                <TableCell sx={{ overflowWrap: "anywhere" }}>{p.email}</TableCell>
                                <TableCell>
                                    <Tooltip title={ROLE_HINT[p.role]} placement="left">
                                        <Select
                                            size="small"
                                            fullWidth
                                            value={p.role}
                                            disabled={roleMutation.isPending}
                                            onChange={(e) => roleMutation.mutate({ userId: p.userId, role: e.target.value as VenueRole })}
                                        >
                                            {(Object.keys(ROLE_LABEL) as VenueRole[]).map((r) => (
                                                <MenuItem key={r} value={r}>
                                                    {ROLE_LABEL[r]}
                                                </MenuItem>
                                            ))}
                                        </Select>
                                    </Tooltip>
                                </TableCell>
                                <TableCell align="right">
                                    <Tooltip title="Remove from this venue">
                                        <IconButton
                                            size="small"
                                            onClick={() => {
                                                removeMutation.reset();
                                                setRemoving(p);
                                            }}
                                            sx={{ border: "1px solid rgba(255,255,255,0.18)", borderRadius: 2 }}
                                        >
                                            <PersonRemoveOutlinedIcon fontSize="small" />
                                        </IconButton>
                                    </Tooltip>
                                </TableCell>
                            </TableRow>
                        ))}

                        {staff.length === 0 && (
                            <TableRow>
                                <TableCell colSpan={4}>Nobody works here yet</TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
            </Paper>

            {adding ? (
                <StaffDialog
                    open
                    loading={addMutation.isPending}
                    errorText={addError}
                    onClose={() => setAdding(false)}
                    onSubmit={(dto) => addMutation.mutate(dto)}
                />
            ) : null}

            <ConfirmDialog
                open={!!removing}
                title={`Remove ${removing?.displayName ?? ""}?`}
                description="They lose access to this venue at their next token refresh (within minutes). Their account and other venues stay."
                confirmText="Remove"
                loading={removeMutation.isPending}
                errorText={removeMutation.isError ? getApiErrorMessage(removeMutation.error, "Failed to remove") : null}
                onCancel={() => setRemoving(null)}
                onConfirm={() => removing && removeMutation.mutate(removing.userId)}
            />

            <Snackbar open={!!notice} autoHideDuration={5000} onClose={() => setNotice(null)}>
                <Alert severity="error" variant="filled" onClose={() => setNotice(null)}>
                    {notice}
                </Alert>
            </Snackbar>
        </Box>
    );
}
