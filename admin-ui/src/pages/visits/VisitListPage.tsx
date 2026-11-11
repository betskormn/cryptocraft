import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
    Alert,
    Box,
    Button,
    CircularProgress,
    Paper,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableRow,
    Typography,
} from "@mui/material";

import { closeSession, getOpenSessions } from "../../api/sessions.api";
import { getApiErrorMessage } from "../../api/error";
import SessionBillDialog from "../../components/SessionBillDialog";

const since = (iso: string) => new Date(iso).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });

// tables with a visit in progress; staff open the bill and close the visit once it is settled
export default function VisitListPage() {
    const queryClient = useQueryClient();
    const [selected, setSelected] = useState<string | null>(null);
    const [closeError, setCloseError] = useState<string | null>(null);

    const sessionsQuery = useQuery({
        queryKey: ["open-sessions"],
        queryFn: getOpenSessions,
        refetchInterval: 10_000,
    });

    const closeMutation = useMutation({
        mutationFn: (sessionId: string) => closeSession(sessionId),
        onSuccess: async () => {
            setSelected(null);
            setCloseError(null);
            await queryClient.invalidateQueries({ queryKey: ["open-sessions"] });
        },
        onError: (e) => setCloseError(getApiErrorMessage(e, "Failed to close the visit")),
    });

    if (sessionsQuery.isLoading) return <CircularProgress />;
    if (sessionsQuery.isError) return <Alert severity="error">Error loading visits</Alert>;

    const sessions = sessionsQuery.data ?? [];

    return (
        <Box>
            <Stack mb={2}>
                <Typography variant="h5" fontWeight={800}>
                    Visits
                </Typography>
                <Typography variant="body2" color="text.secondary">
                    Tables with guests right now. Bills are split per guest; close a visit when it is paid.
                </Typography>
            </Stack>

            <Paper variant="outlined">
                <Table size="small">
                    <TableHead>
                        <TableRow>
                            <TableCell>Table</TableCell>
                            <TableCell width={160}>Opened</TableCell>
                            <TableCell width={120} align="right">
                                Bill
                            </TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {sessions.map((s) => (
                            <TableRow key={s.sessionId} hover>
                                <TableCell sx={{ fontWeight: 700 }}>{s.tableLabel}</TableCell>
                                <TableCell sx={{ color: "text.secondary" }}>{since(s.openedAt)}</TableCell>
                                <TableCell align="right">
                                    <Button
                                        size="small"
                                        variant="outlined"
                                        onClick={() => {
                                            setCloseError(null);
                                            setSelected(s.sessionId);
                                        }}
                                    >
                                        Open bill
                                    </Button>
                                </TableCell>
                            </TableRow>
                        ))}
                        {sessions.length === 0 && (
                            <TableRow>
                                <TableCell colSpan={3}>No open visits</TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
            </Paper>

            {selected && (
                <SessionBillDialog
                    open
                    sessionId={selected}
                    closing={closeMutation.isPending}
                    errorText={closeError}
                    onClose={() => setSelected(null)}
                    onCloseVisit={() => closeMutation.mutate(selected)}
                />
            )}
        </Box>
    );
}
