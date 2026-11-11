import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
    Alert,
    Box,
    Button,
    Chip,
    CircularProgress,
    IconButton,
    Paper,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableRow,
    Tooltip,
    Typography,
} from "@mui/material";
import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import ContentCopyIcon from "@mui/icons-material/ContentCopy";

import {
    createTable,
    getAllTables,
    updateTable,
    type VenueTableRequestDto,
    type VenueTableResponseDto,
} from "../../api/tables.api";
import { getApiErrorMessage } from "../../api/error";
import TableDialog from "../../components/TableDialog";

export default function TableListPage() {
    const queryClient = useQueryClient();

    // null: closed; without table: create; with table: edit
    const [dialog, setDialog] = useState<{ table?: VenueTableResponseDto } | null>(null);
    const [dialogError, setDialogError] = useState<string | null>(null);

    const tablesQuery = useQuery({ queryKey: ["tables"], queryFn: getAllTables });

    const saveMutation = useMutation({
        mutationFn: ({ id, dto }: { id?: number; dto: VenueTableRequestDto }) =>
            id === undefined ? createTable(dto) : updateTable(id, dto),
        onSuccess: async () => {
            setDialog(null);
            setDialogError(null);
            await queryClient.invalidateQueries({ queryKey: ["tables"] });
        },
        onError: (e) => setDialogError(getApiErrorMessage(e, "Failed to save table")),
    });

    function openDialog(table?: VenueTableResponseDto) {
        setDialogError(null);
        setDialog({ table });
    }

    if (tablesQuery.isLoading) return <CircularProgress />;
    if (tablesQuery.isError) return <Alert severity="error">Error loading tables</Alert>;

    const tables = tablesQuery.data ?? [];

    return (
        <Box>
            <Stack direction="row" alignItems="center" justifyContent="space-between" mb={2}>
                <Box>
                    <Typography variant="h5" fontWeight={800}>
                        Tables
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                        Each table has a QR code token; a guest who scans it joins the table's visit.
                    </Typography>
                </Box>
                <Button variant="contained" onClick={() => openDialog()}>
                    New table
                </Button>
            </Stack>

            <Paper variant="outlined">
                <Table size="small">
                    <TableHead>
                        <TableRow>
                            <TableCell>Label</TableCell>
                            <TableCell>QR code token</TableCell>
                            <TableCell width={140}>Status</TableCell>
                            <TableCell width={80} align="right">
                                Actions
                            </TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {tables.map((t) => (
                            <TableRow key={t.tableId} hover>
                                <TableCell sx={{ fontWeight: 700 }}>{t.label}</TableCell>
                                <TableCell>
                                    <Stack direction="row" alignItems="center" spacing={0.5}>
                                        <Box component="span" sx={{ fontFamily: "monospace", letterSpacing: 1 }}>
                                            {t.code}
                                        </Box>
                                        <Tooltip title="Copy">
                                            <IconButton size="small" onClick={() => navigator.clipboard?.writeText(t.code)}>
                                                <ContentCopyIcon fontSize="inherit" />
                                            </IconButton>
                                        </Tooltip>
                                    </Stack>
                                </TableCell>
                                <TableCell>
                                    {t.active ? (
                                        <Chip size="small" label="In service" color="success" variant="outlined" />
                                    ) : (
                                        <Chip size="small" label="Out of service" variant="outlined" />
                                    )}
                                </TableCell>
                                <TableCell align="right">
                                    <Tooltip title="Edit">
                                        <IconButton
                                            size="small"
                                            onClick={() => openDialog(t)}
                                            sx={{ border: "1px solid rgba(255,255,255,0.18)", borderRadius: 2 }}
                                        >
                                            <EditOutlinedIcon fontSize="small" />
                                        </IconButton>
                                    </Tooltip>
                                </TableCell>
                            </TableRow>
                        ))}

                        {tables.length === 0 && (
                            <TableRow>
                                <TableCell colSpan={4}>No tables yet</TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
            </Paper>

            {dialog && (
                <TableDialog
                    open
                    initial={dialog.table}
                    loading={saveMutation.isPending}
                    errorText={dialogError}
                    onClose={() => {
                        if (!saveMutation.isPending) setDialog(null);
                    }}
                    onSubmit={(dto) => saveMutation.mutate({ id: dialog.table?.tableId, dto })}
                />
            )}
        </Box>
    );
}
