import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
    Alert,
    Box,
    Button,
    CircularProgress,
    IconButton,
    InputAdornment,
    Paper,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TablePagination,
    TableRow,
    TableSortLabel,
    TextField,
    Tooltip,
    Typography,
} from "@mui/material";
import SearchIcon from "@mui/icons-material/Search";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";

import { createBrand, deleteBrand, getAllBrands, type BrandResponseDto } from "../../api/brands.api";
import BrandCreateDialog from "../../components/BrandCreateDialog";
import ConfirmDialog from "../../components/ConfirmDialog";
import { includes } from "../../utils/text";
import { useTableQuery } from "../../hooks/useTableQuery";

type SortKey = "id" | "name";

export default function BrandListPage() {
    const queryClient = useQueryClient();

    const [createOpen, setCreateOpen] = useState(false);
    const [createError, setCreateError] = useState<string | null>(null);

    const [deleteOpen, setDeleteOpen] = useState(false);
    const [deleteTarget, setDeleteTarget] = useState<{ id: number; name: string } | null>(null);

    const brandsQuery = useQuery({
        queryKey: ["brands"],
        queryFn: getAllBrands,
    });

    const createMutation = useMutation({
        mutationFn: (name: string) => createBrand({ name }),
        onSuccess: async () => {
            setCreateOpen(false);
            setCreateError(null);
            await queryClient.invalidateQueries({ queryKey: ["brands"] });
        },
        onError: () => {
            setCreateError("Failed to create brand (maybe it already exists?)");
        },
    });

    const deleteMutation = useMutation({
        mutationFn: (id: number) => deleteBrand(id),
        onSuccess: async () => {
            setDeleteOpen(false);
            setDeleteTarget(null);
            await queryClient.invalidateQueries({ queryKey: ["brands"] });
        },
    });

    const brands = useMemo(
        () => (Array.isArray(brandsQuery.data) ? brandsQuery.data : []),
        [brandsQuery.data]
    );

    const tq = useTableQuery<BrandResponseDto, SortKey>(brands, {
        initialRowsPerPage: 10,
        initialSort: { key: "id", dir: "asc" },
        filterFn: (b, q) => includes(b.brandId, q) || includes(b.name, q),
        sortKeys: {
            id: (b) => b.brandId,
            name: (b) => b.name,
        },
    });

    function openDeleteConfirm(id: number, name: string) {
        setDeleteTarget({ id, name });
        setDeleteOpen(true);
    }

    if (brandsQuery.isLoading) return <CircularProgress />;
    if (brandsQuery.isError) return <Alert severity="error">Error loading brands</Alert>;

    return (
        <Box>
            <Stack
                direction={{ xs: "column", md: "row" }}
                spacing={2}
                alignItems={{ xs: "stretch", md: "center" }}
                justifyContent="space-between"
                mb={2}
            >
                <Typography variant="h5" fontWeight={800}>
                    Brands
                </Typography>

                <Stack direction={{ xs: "column", md: "row" }} spacing={1.5} alignItems="center">
                    <TextField
                        size="small"
                        placeholder="Search by id or name..."
                        value={tq.search}
                        onChange={(e) => tq.setSearch(e.target.value)}
                        sx={{ width: { xs: "100%", md: 320 } }}
                        InputProps={{
                            startAdornment: (
                                <InputAdornment position="start">
                                    <SearchIcon fontSize="small" />
                                </InputAdornment>
                            ),
                        }}
                    />

                    <Button variant="contained" onClick={() => setCreateOpen(true)}>
                        Create brand
                    </Button>
                </Stack>
            </Stack>

            <Paper variant="outlined">
                <Table size="small">
                    <TableHead>
                        <TableRow>
                            <TableCell width={90} sortDirection={tq.sort.key === "id" ? tq.sort.dir : false}>
                                <TableSortLabel
                                    active={tq.sort.key === "id"}
                                    direction={tq.sort.key === "id" ? tq.sort.dir : "asc"}
                                    onClick={() => tq.toggleSort("id")}
                                >
                                    ID
                                </TableSortLabel>
                            </TableCell>

                            <TableCell sortDirection={tq.sort.key === "name" ? tq.sort.dir : false}>
                                <TableSortLabel
                                    active={tq.sort.key === "name"}
                                    direction={tq.sort.key === "name" ? tq.sort.dir : "asc"}
                                    onClick={() => tq.toggleSort("name")}
                                >
                                    Name
                                </TableSortLabel>
                            </TableCell>

                            <TableCell width={120} align="right">
                                Actions
                            </TableCell>
                        </TableRow>
                    </TableHead>

                    <TableBody>
                        {tq.paged.map((b) => (
                            <TableRow key={b.brandId} hover>
                                <TableCell>{b.brandId}</TableCell>
                                <TableCell>{b.name}</TableCell>

                                <TableCell align="right">
                                    <Tooltip title="Delete">
                    <span>
                      <IconButton
                          size="small"
                          disabled={deleteMutation.isPending}
                          onClick={() => openDeleteConfirm(b.brandId, b.name)}
                          sx={{
                              border: "1px solid rgba(255,255,255,0.18)",
                              borderRadius: 2,
                              color: "error.main",
                          }}
                      >
                        <DeleteOutlineIcon fontSize="small" />
                      </IconButton>
                    </span>
                                    </Tooltip>
                                </TableCell>
                            </TableRow>
                        ))}

                        {tq.totalFiltered === 0 && (
                            <TableRow>
                                <TableCell colSpan={3}>
                                    {tq.search.trim() ? "No matches found" : "No brands yet"}
                                </TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>

                <TablePagination
                    component="div"
                    count={tq.totalFiltered}
                    page={tq.page}
                    onPageChange={(_, newPage) => tq.setPage(newPage)}
                    rowsPerPage={tq.rowsPerPage}
                    onRowsPerPageChange={(e) => {
                        tq.setRowsPerPage(parseInt(e.target.value, 10));
                        tq.setPage(0);
                    }}
                    rowsPerPageOptions={tq.rowsPerPageOptions}
                />
            </Paper>

            <BrandCreateDialog
                open={createOpen}
                loading={createMutation.isPending}
                errorText={createError}
                onClose={() => {
                    setCreateOpen(false);
                    setCreateError(null);
                }}
                onSubmit={(name) => createMutation.mutate(name)}
            />

            <ConfirmDialog
                open={deleteOpen}
                title={`Delete brand "${deleteTarget?.name ?? ""}"?`}
                description="This action cannot be undone."
                loading={deleteMutation.isPending}
                onCancel={() => {
                    if (!deleteMutation.isPending) {
                        setDeleteOpen(false);
                        setDeleteTarget(null);
                    }
                }}
                onConfirm={() => {
                    if (deleteTarget) deleteMutation.mutate(deleteTarget.id);
                }}
            />
        </Box>
    );
}