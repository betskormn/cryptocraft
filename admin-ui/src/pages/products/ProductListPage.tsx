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
    Snackbar,
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
import AddCircleIcon from "@mui/icons-material/AddCircle";
import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";

import { getAllBrands } from "../../api/brands.api";
import {
    adjustProductStock,
    createProduct,
    deleteProduct,
    getAllProducts,
    updateProduct,
    UNIT_LABEL,
    type ProductRequestDto,
    type ProductResponseDto,
} from "../../api/products.api";
import { getApiErrorMessage } from "../../api/error";

import ConfirmDialog from "../../components/ConfirmDialog";
import ProductDialog from "../../components/ProductDialog";
import AdjustStockDialog from "../../components/AdjustStockDialog";

import { includes, norm } from "../../utils/text";
import { useTableQuery } from "../../hooks/useTableQuery";

type SortKey = "id" | "name" | "stock" | "unit" | "brand";

export default function ProductListPage() {
    const qc = useQueryClient();

    const [createOpen, setCreateOpen] = useState(false);
    const [editOpen, setEditOpen] = useState(false);
    const [selected, setSelected] = useState<ProductResponseDto | null>(null);
    const [errorText, setErrorText] = useState<string | null>(null);

    const [deleteOpen, setDeleteOpen] = useState(false);
    const [deleteTarget, setDeleteTarget] = useState<ProductResponseDto | null>(null);

    // add stock dialog
    const [addOpen, setAddOpen] = useState(false);
    const [addTarget, setAddTarget] = useState<ProductResponseDto | null>(null);

    // toast
    const [toastOpen, setToastOpen] = useState(false);
    const [toastText, setToastText] = useState("");

    const productsQuery = useQuery({ queryKey: ["products"], queryFn: getAllProducts });
    const brandsQuery = useQuery({ queryKey: ["brands"], queryFn: getAllBrands });

    const createMut = useMutation({
        mutationFn: (dto: ProductRequestDto) => createProduct(dto),
        onSuccess: async () => {
            setCreateOpen(false);
            setErrorText(null);
            await qc.invalidateQueries({ queryKey: ["products"] });
        },
        onError: (e) => setErrorText(getApiErrorMessage(e, "Failed to create product")),
    });

    const editMut = useMutation({
        mutationFn: ({ id, dto }: { id: number; dto: ProductRequestDto }) => updateProduct(id, dto),
        onSuccess: async () => {
            setEditOpen(false);
            setSelected(null);
            setErrorText(null);
            await qc.invalidateQueries({ queryKey: ["products"] });
        },
        onError: (e) => setErrorText(getApiErrorMessage(e, "Failed to update product")),
    });

    const delMut = useMutation({
        mutationFn: (id: number) => deleteProduct(id),
        onSuccess: async () => {
            setDeleteOpen(false);
            setDeleteTarget(null);
            await qc.invalidateQueries({ queryKey: ["products"] });
        },
    });

    // atomic PATCH: /products/{id}/stock { delta }
    const addStockMut = useMutation({
        mutationFn: ({ productId, delta }: { productId: number; delta: number }) =>
            adjustProductStock(productId, delta),

        // optimistic update
        onMutate: async ({ productId, delta }) => {
            await qc.cancelQueries({ queryKey: ["products"] });

            const prev = qc.getQueryData<ProductResponseDto[]>(["products"]);
            if (prev) {
                qc.setQueryData<ProductResponseDto[]>(["products"], (old) =>
                    (old ?? []).map((p) =>
                        p.productId === productId
                            ? { ...p, quantityInStock: (p.quantityInStock ?? 0) + delta }
                            : p
                    )
                );
            }
            return { prev };
        },

        onError: (_err, _vars, ctx) => {
            if (ctx?.prev) qc.setQueryData(["products"], ctx.prev);
        },

        onSuccess: async (updated, vars) => {
            setAddOpen(false);
            setAddTarget(null);

            setToastText(
                `Stock increased: ${updated.name} +${vars.delta} (new: ${updated.quantityInStock})`
            );
            setToastOpen(true);

            await qc.invalidateQueries({ queryKey: ["products"] });
        },
    });

    // safe arrays
    const products = useMemo(
        () => (Array.isArray(productsQuery.data) ? productsQuery.data : []),
        [productsQuery.data]
    );

    const brands = useMemo(
        () => (Array.isArray(brandsQuery.data) ? brandsQuery.data : []),
        [brandsQuery.data]
    );

    const brandNameById = useMemo(
        () => new Map(brands.map((b) => [b.brandId, b.name])),
        [brands]
    );

    // unified search + sort + pagination
    const tq = useTableQuery<ProductResponseDto, SortKey>(products, {
        initialRowsPerPage: 10,
        initialSort: { key: "id", dir: "asc" },

        filterFn: (p, q) => {
            const brandName = norm(brandNameById.get(p.brandId));
            return (
                includes(p.productId, q) ||
                includes(p.name, q) ||
                includes(p.description, q) ||
                includes(p.brandId, q) ||
                includes(p.unit, q) ||
                brandName.includes(q)
            );
        },

        sortKeys: {
            id: (p) => p.productId,
            name: (p) => p.name,
            stock: (p) => p.quantityInStock,
            unit: (p) => p.unit,
            brand: (p) => brandNameById.get(p.brandId) ?? "",
        },
    });

    // early returns after hooks
    if (productsQuery.isLoading || brandsQuery.isLoading) return <CircularProgress />;
    if (productsQuery.isError) return <Alert severity="error">Error loading products</Alert>;
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
                <Typography variant="h5" fontWeight={850}>
                    Products
                </Typography>

                <Stack direction={{ xs: "column", md: "row" }} spacing={1.5} alignItems="center">
                    <TextField
                        size="small"
                        placeholder="Search by name, brand, id..."
                        value={tq.search}
                        onChange={(e) => tq.setSearch(e.target.value)}
                        sx={{ width: { xs: "100%", md: 360 } }}
                        InputProps={{
                            startAdornment: (
                                <InputAdornment position="start">
                                    <SearchIcon fontSize="small" />
                                </InputAdornment>
                            ),
                        }}
                    />

                    <Button
                        variant="contained"
                        onClick={() => {
                            setErrorText(null);
                            setCreateOpen(true);
                        }}
                    >
                        Create product
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

                            <TableCell width={120} sortDirection={tq.sort.key === "stock" ? tq.sort.dir : false}>
                                <TableSortLabel
                                    active={tq.sort.key === "stock"}
                                    direction={tq.sort.key === "stock" ? tq.sort.dir : "asc"}
                                    onClick={() => tq.toggleSort("stock")}
                                >
                                    Stock
                                </TableSortLabel>
                            </TableCell>

                            <TableCell width={170} sortDirection={tq.sort.key === "unit" ? tq.sort.dir : false}>
                                <TableSortLabel
                                    active={tq.sort.key === "unit"}
                                    direction={tq.sort.key === "unit" ? tq.sort.dir : "asc"}
                                    onClick={() => tq.toggleSort("unit")}
                                >
                                    Unit
                                </TableSortLabel>
                            </TableCell>

                            <TableCell width={240} sortDirection={tq.sort.key === "brand" ? tq.sort.dir : false}>
                                <TableSortLabel
                                    active={tq.sort.key === "brand"}
                                    direction={tq.sort.key === "brand" ? tq.sort.dir : "asc"}
                                    onClick={() => tq.toggleSort("brand")}
                                >
                                    Brand
                                </TableSortLabel>
                            </TableCell>

                            <TableCell width={240} align="right">
                                Actions
                            </TableCell>
                        </TableRow>
                    </TableHead>

                    <TableBody>
                        {tq.paged.map((p) => (
                            <TableRow key={p.productId} hover>
                                <TableCell>{p.productId}</TableCell>
                                <TableCell>{p.name}</TableCell>
                                <TableCell>{p.quantityInStock}</TableCell>
                                <TableCell sx={{ color: "text.secondary" }}>
                                    {UNIT_LABEL[p.unit] ?? p.unit}
                                </TableCell>
                                <TableCell sx={{ color: "text.secondary" }}>
                                    {brandNameById.get(p.brandId) ?? `#${p.brandId}`}
                                </TableCell>

                                <TableCell align="right">
                                    <Stack direction="row" spacing={0.5} justifyContent="flex-end" alignItems="center">
                                        <Tooltip title="Add stock">
                      <span>
                        <IconButton
                            size="small"
                            disabled={addStockMut.isPending}
                            onClick={() => {
                                setAddTarget(p);
                                setAddOpen(true);
                            }}
                            sx={{
                                mr: 0.25,
                                color: "primary.contrastText",
                                backgroundColor: "primary.main",
                                "&:hover": { backgroundColor: "primary.dark" },
                                boxShadow: "0 8px 18px rgba(0,0,0,0.35)",
                            }}
                        >
                          <AddCircleIcon fontSize="small" />
                        </IconButton>
                      </span>
                                        </Tooltip>

                                        <Tooltip title="Edit">
                      <span>
                        <IconButton
                            size="small"
                            disabled={editMut.isPending}
                            onClick={() => {
                                setErrorText(null);
                                setSelected(p);
                                setEditOpen(true);
                            }}
                            sx={{
                                border: "1px solid rgba(255,255,255,0.18)",
                                borderRadius: 2,
                            }}
                        >
                          <EditOutlinedIcon fontSize="small" />
                        </IconButton>
                      </span>
                                        </Tooltip>

                                        <Tooltip title="Delete">
                      <span>
                        <IconButton
                            size="small"
                            disabled={delMut.isPending}
                            onClick={() => {
                                setDeleteTarget(p);
                                setDeleteOpen(true);
                            }}
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
                                    </Stack>
                                </TableCell>
                            </TableRow>
                        ))}

                        {tq.totalFiltered === 0 && (
                            <TableRow>
                                <TableCell colSpan={6}>
                                    {tq.search.trim() ? "No matches found" : "No products yet"}
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

            <ProductDialog
                open={createOpen}
                mode="create"
                loading={createMut.isPending}
                errorText={errorText}
                brands={brands}
                onClose={() => {
                    if (!createMut.isPending) setCreateOpen(false);
                }}
                onSubmit={(dto) => createMut.mutate(dto)}
            />

            <ProductDialog
                open={editOpen}
                mode="edit"
                loading={editMut.isPending}
                errorText={errorText}
                brands={brands}
                initial={selected}
                onClose={() => {
                    if (!editMut.isPending) {
                        setEditOpen(false);
                        setSelected(null);
                    }
                }}
                onSubmit={(dto) => {
                    if (!selected) return;
                    editMut.mutate({ id: selected.productId, dto });
                }}
            />

            <ConfirmDialog
                open={deleteOpen}
                title={`Delete product "${deleteTarget?.name ?? ""}"?`}
                loading={delMut.isPending}
                onCancel={() => {
                    if (!delMut.isPending) {
                        setDeleteOpen(false);
                        setDeleteTarget(null);
                    }
                }}
                onConfirm={() => {
                    if (deleteTarget) delMut.mutate(deleteTarget.productId);
                }}
            />

            <AdjustStockDialog
                open={addOpen}
                productName={addTarget?.name ?? ""}
                currentStock={addTarget?.quantityInStock ?? 0}
                loading={addStockMut.isPending}
                onClose={() => {
                    if (!addStockMut.isPending) {
                        setAddOpen(false);
                        setAddTarget(null);
                    }
                }}
                onSubmit={(delta) => {
                    if (!addTarget) return;
                    addStockMut.mutate({ productId: addTarget.productId, delta });
                }}
            />

            <Snackbar
                open={toastOpen}
                autoHideDuration={3500}
                onClose={() => setToastOpen(false)}
                anchorOrigin={{ vertical: "bottom", horizontal: "left" }}
            >
                <Alert
                    onClose={() => setToastOpen(false)}
                    severity="success"
                    variant="filled"
                    sx={{
                        borderRadius: 2,
                        backgroundColor: "rgba(34,40,49,0.92)",
                        color: "text.primary",
                        border: "1px solid rgba(242,162,58,0.20)",
                    }}
                >
                    {toastText}
                </Alert>
            </Snackbar>
        </Box>
    );
}