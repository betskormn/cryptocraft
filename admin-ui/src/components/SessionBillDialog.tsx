import { useQuery } from "@tanstack/react-query";
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

import { getSessionBill } from "../api/sessions.api";

type Props = {
    open: boolean;
    sessionId: string;
    closing?: boolean;
    errorText?: string | null;
    onClose: () => void;
    onCloseVisit: () => void;
};

const money = (value: number) => value.toFixed(2);
const time = (iso: string) => new Date(iso).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });

// the bill of one visit, split per guest; every line at the price locked when it was sent
export default function SessionBillDialog({ open, sessionId, closing = false, errorText = null, onClose, onCloseVisit }: Props) {
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

                <Stack spacing={2} divider={<Divider flexItem />}>
                    {bill?.participants.map((p, idx) => (
                        <Box key={p.participantId}>
                            <Stack direction="row" justifyContent="space-between" alignItems="baseline" mb={1}>
                                <Typography fontWeight={800}>{p.displayName || `Guest ${idx + 1}`}</Typography>
                                <Typography fontWeight={800}>{money(p.total)}</Typography>
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
                                            o.lines.map((l) => (
                                                <TableRow key={`${o.orderId}-${l.itemId}`}>
                                                    <TableCell sx={{ color: "text.secondary" }}>{time(o.createdAt)}</TableCell>
                                                    <TableCell>
                                                        {l.itemName}{" "}
                                                        <Chip size="small" variant="outlined" label={l.itemType === "DRINK" ? "bar" : "kitchen"} />
                                                    </TableCell>
                                                    <TableCell align="right">{l.quantity}</TableCell>
                                                    <TableCell align="right">{money(l.unitPrice)}</TableCell>
                                                    <TableCell align="right">{money(l.lineTotal)}</TableCell>
                                                </TableRow>
                                            ))
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
