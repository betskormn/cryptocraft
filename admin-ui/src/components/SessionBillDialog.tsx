import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
    Alert,
    Box,
    Button,
    Chip,
    CircularProgress,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Divider,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableRow,
    Typography,
} from "@mui/material";

import { approveGuest, getSessionBill, rejectGuest } from "../api/sessions.api";
import { getApiErrorMessage } from "../api/error";
import type { LineStatus } from "../api/kitchen.api";

type Props = {
    open: boolean;
    sessionId: string;
    closing?: boolean;
    errorText?: string | null;
    onClose: () => void;
    onCloseVisit: () => void;
};

const money = (value: number) => value.toFixed(2);
// cancelled lines stay in the bill, struck out, and are not charged
const LINE_STATUS: Record<LineStatus, string> = {
    NEW: "sent",
    PREPARING: "preparing",
    READY: "ready",
    SERVED: "served",
    CANCELLED: "cancelled",
};
const time = (iso: string) => new Date(iso).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });

// the bill of one visit, split per guest; every line at the price locked when it was sent
export default function SessionBillDialog({ open, sessionId, closing = false, errorText = null, onClose, onCloseVisit }: Props) {
    const queryClient = useQueryClient();
    const decideMutation = useMutation({
        mutationFn: ({ participantId, approve }: { participantId: string; approve: boolean }) =>
            approve ? approveGuest(sessionId, participantId) : rejectGuest(sessionId, participantId),
        onSuccess: async () => {
            await queryClient.invalidateQueries({ queryKey: ["session-bill", sessionId] });
            await queryClient.invalidateQueries({ queryKey: ["open-sessions"] });
        },
    });
    const billQuery = useQuery({
        queryKey: ["session-bill", sessionId],
        queryFn: () => getSessionBill(sessionId),
        refetchInterval: 10_000,
    });
    const bill = billQuery.data;

    return (
        <Dialog open={open} onClose={closing ? undefined : onClose} fullWidth maxWidth="md">
            <DialogTitle>
                {bill ? `Table ${bill.tableLabel}` : "Bill"}
                {bill ? (
                    <Typography component="span" sx={{ ml: 2, color: "primary.main", fontWeight: 800 }}>
                        {money(bill.total)}
                    </Typography>
                ) : null}
            </DialogTitle>

            <DialogContent>
                {errorText ? (
                    <Alert severity="error" sx={{ mb: 2 }}>
                        {errorText}
                    </Alert>
                ) : null}
                {billQuery.isLoading ? <CircularProgress /> : null}
                {billQuery.isError ? <Alert severity="error">Error loading the bill</Alert> : null}
                {decideMutation.isError ? (
                    <Alert severity="error" sx={{ mb: 2 }}>
                        {getApiErrorMessage(decideMutation.error, "Failed to update the guest")}
                    </Alert>
                ) : null}

                <Stack spacing={2} divider={<Divider flexItem />}>
                    {bill?.participants.map((p, idx) => (
                        <Box key={p.participantId}>
                            <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1} gap={1} flexWrap="wrap">
                                <Stack direction="row" alignItems="center" gap={1}>
                                    <Typography fontWeight={800}>{p.displayName || `Guest ${idx + 1}`}</Typography>
                                    {p.status === "PENDING" ? <Chip size="small" color="warning" label="new guest, not confirmed" /> : null}
                                    {p.status === "REJECTED" ? <Chip size="small" color="error" variant="outlined" label="rejected" /> : null}
                                </Stack>
                                <Stack direction="row" alignItems="center" gap={1}>
                                    {p.status === "PENDING" ? (
                                        <>
                                            <Button
                                                size="small"
                                                color="error"
                                                variant="outlined"
                                                disabled={decideMutation.isPending}
                                                onClick={() => decideMutation.mutate({ participantId: p.participantId, approve: false })}
                                            >
                                                Not at the table
                                            </Button>
                                            <Button
                                                size="small"
                                                variant="contained"
                                                disabled={decideMutation.isPending}
                                                onClick={() => decideMutation.mutate({ participantId: p.participantId, approve: true })}
                                            >
                                                Confirm guest
                                            </Button>
                                        </>
                                    ) : null}
                                    <Typography fontWeight={800}>{money(p.total)}</Typography>
                                </Stack>
                            </Stack>
                            {p.orders.length === 0 ? (
                                <Typography variant="body2" color="text.secondary">
                                    Nothing ordered yet
                                </Typography>
                            ) : (
                                <Table size="small">
                                    <TableHead>
                                        <TableRow>
                                            <TableCell width={70}>Sent</TableCell>
                                            <TableCell>Item</TableCell>
                                            <TableCell width={60} align="right">Qty</TableCell>
                                            <TableCell width={90} align="right">Price</TableCell>
                                            <TableCell width={100} align="right">Total</TableCell>
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {p.orders.flatMap((o) =>
                                            o.lines.map((l) => {
                                                const cancelled = l.status === "CANCELLED";
                                                const held = o.status === "AWAITING_APPROVAL";
                                                const struck = cancelled ? { textDecoration: "line-through", color: "text.disabled" } : undefined;
                                                return (
                                                    <TableRow key={l.lineId}>
                                                        <TableCell sx={{ color: "text.secondary" }}>{time(o.createdAt)}</TableCell>
                                                        <TableCell>
                                                            <Box component="span" sx={struck}>
                                                                {l.itemName}
                                                            </Box>{" "}
                                                            <Chip size="small" variant="outlined" label={l.itemType === "DRINK" ? "bar" : "kitchen"} />{" "}
                                                            <Chip
                                                                size="small"
                                                                color={held ? "warning" : cancelled ? "error" : l.status === "SERVED" ? "success" : "default"}
                                                                label={held ? "waits for confirmation" : LINE_STATUS[l.status]}
                                                            />
                                                            {cancelled && l.cancelReason ? (
                                                                <Typography variant="caption" color="text.secondary" sx={{ display: "block" }}>
                                                                    {l.cancelReason}
                                                                </Typography>
                                                            ) : null}
                                                        </TableCell>
                                                        <TableCell align="right" sx={struck}>{l.quantity}</TableCell>
                                                        <TableCell align="right" sx={struck}>{money(l.unitPrice)}</TableCell>
                                                        <TableCell align="right" sx={struck}>{money(l.lineTotal)}</TableCell>
                                                    </TableRow>
                                                );
                                            })
                                        )}
                                    </TableBody>
                                </Table>
                            )}
                        </Box>
                    ))}
                    {bill && bill.participants.length === 0 ? (
                        <Typography color="text.secondary">No guests joined yet</Typography>
                    ) : null}
                </Stack>
            </DialogContent>

            <DialogActions>
                <Button onClick={onClose} disabled={closing} variant="outlined">
                    Back
                </Button>
                <Button onClick={onCloseVisit} disabled={closing || !bill || bill.status !== "OPEN"} color="error" variant="contained">
                    Close visit
                </Button>
            </DialogActions>
        </Dialog>
    );
}
