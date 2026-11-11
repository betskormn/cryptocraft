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
    Switch,
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
import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";

import { getAllProducts } from "../../api/products.api";
import {
    createItem,
    deleteItem,
    getAllItems,
    setItemStopped,
    updateItem,
    type ItemRequestDto,
    type ItemResponseDto,
} from "../../api/items.api";
import { getApiErrorMessage } from "../../api/error";
import ConfirmDialog from "../../components/ConfirmDialog";
import ItemDialog from "../../components/ItemDialog";
import { includes } from "../../utils/text";
import { useTableQuery } from "../../hooks/useTableQuery";

type SortKey = "id" | "order" | "name" | "type" | "products" | "price";

export default function ItemListPage() {
    const qc = useQueryClient();

    const [createOpen, setCreateOpen] = useState(false);
    const [editOpen, setEditOpen] = useState(false);
    const [selected, setSelected] = useState<ItemResponseDto | null>(null);
    const [errorText, setErrorText] = useState<string | null>(null);

    const [deleteError, setDeleteError] = useState<string | null>(null);
    const [deleteOpen, setDeleteOpen] = useState(false);
    const [deleteTarget, setDeleteTarget] = useState<ItemResponseDto | null>(null);

    const [stopListError, setStopListError] = useState<string | null>(null);

    const itemsQuery = useQuery({ queryKey: ["items"], queryFn: getAllItems });
    const productsQuery = useQuery({ queryKey: ["products"], queryFn: getAllProducts });

    const createMut = useMutation({
        mutationFn: (dto: ItemRequestDto) => createItem(dto),
        onSuccess: async () => {
            setCreateOpen(false);
            setErrorText(null);
            await qc.invalidateQueries({ queryKey: ["items"] });
        },
        onError: (e) => setErrorText(getApiErrorMessage(e, "Failed to create item")),
    });

    const editMut = useMutation({
        mutationFn: ({ id, dto }: { id: number; dto: ItemRequestDto }) => updateItem(id, dto),
        onSuccess: async () => {
            setEditOpen(false);
            setSelected(null);
            setErrorText(null);
            await qc.invalidateQueries({ queryKey: ["items"] });
        },
        onError: (e) => setErrorText(getApiErrorMessage(e, "Failed to update item")),
    });

    const delMut = useMutation({
        mutationFn: (id: number) => deleteItem(id),
        onSuccess: async () => {
            setDeleteOpen(false);
            setDeleteTarget(null);
            setDeleteError(null);
            await qc.invalidateQueries({ queryKey: ["items"] });
        },
        onError: (e) => setDeleteError(getApiErrorMessage(e, "Failed to delete item")),
    });

    const stopListMut = useMutation({
        mutationFn: ({ id, stopped }: { id: number; stopped: boolean }) => setItemStopped(id, stopped),
        onSuccess: async () => {
            setStopListError(null);
            await qc.invalidateQueries({ queryKey: ["items"] });
        },
        onError: (e) => setStopListError(getApiErrorMessage(e, "Failed to change the stop-list")),
    });

    const items = useMemo(
        () => (Array.isArray(itemsQuery.data) ? itemsQuery.data : []),
        [itemsQuery.data]
    );
    const products = useMemo(
        () => (Array.isArray(productsQuery.data) ? productsQuery.data : []),
        [productsQuery.data]
    );

    const tq = useTableQuery<ItemResponseDto, SortKey>(items, {
        initialRowsPerPage: 10,
        initialSort: { key: "id", dir: "asc" },
        filterFn: (it, q) => includes(it.itemId, q) || includes(it.name, q) || includes(it.type, q),
        sortKeys: {
            id: (it) => it.itemId,
            order: (it) => it.displayOrder,
            name: (it) => it.name,
            type: (it) => it.type,
            products: (it) => it.products?.length ?? 0,
            price: (it) => it.basePrice,
        },
    });

    if (itemsQuery.isLoading || productsQuery.isLoading) return <CircularProgress />;
    if (itemsQuery.isError) return <Alert severity="error">Error loading items</Alert>;
    if (productsQuery.isError) return <Alert severity="error">Error loading products</Alert>;

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
                    Items
                </Typography>

                <Stack direction={{ xs: "column", md: "row" }} spacing={1.5} alignItems="center">
                    <TextField
                        size="small"
                        placeholder="Search by id, name, type..."
                        value={tq.search}
                        onChange={(e) => tq.setSearch(e.target.value)}
                        sx={{ width: { xs: "100%", md: 340 } }}
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
                        Create item
                    </Button>
                </Stack>
            </Stack>

            {stopListError ? (
                <Alert severity="error" sx={{ mb: 2 }} onClose={() => setStopListError(null)}>
                    {stopListError}
                </Alert>
            ) : null}

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

                            <TableCell width={90} sortDirection={tq.sort.key === "order" ? tq.sort.dir : false}>
                                <Tooltip title="Position on menus and TV screens">
                                    <TableSortLabel
                                        active={tq.sort.key === "order"}
                                        direction={tq.sort.key === "order" ? tq.sort.dir : "asc"}
                                        onClick={() => tq.toggleSort("order")}
                                    >
                                        Menu #
                                    </TableSortLabel>
                                </Tooltip>
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

                            <TableCell width={110} sortDirection={tq.sort.key === "type" ? tq.sort.dir : false}>
                                <TableSortLabel
                                    active={tq.sort.key === "type"}
                                    direction={tq.sort.key === "type" ? tq.sort.dir : "asc"}
                                    onClick={() => tq.toggleSort("type")}
                                >
                                    Type
                                </TableSortLabel>
                            </TableCell>

                            <TableCell width={110} sortDirection={tq.sort.key === "products" ? tq.sort.dir : false}>
                                <TableSortLabel
                                    active={tq.sort.key === "products"}
                                    direction={tq.sort.key === "products" ? tq.sort.dir : "asc"}
                                    onClick={() => tq.toggleSort("products")}
                                >
                                    Products
                                </TableSortLabel>
                            </TableCell>

                            <TableCell
                                width={100}
                                sortDirection={tq.sort.key === "price" ? tq.sort.dir : false}
                            >
                                <TableSortLabel
                                    active={tq.sort.key === "price"}
                                    direction={tq.sort.key === "price" ? tq.sort.dir : "asc"}
                                    onClick={() => tq.toggleSort("price")}
                                >
                                    Price
                                </TableSortLabel>
                            </TableCell>

                            <TableCell width={110}>
                                <Tooltip title="Off = on the stop-list: shown on the menu, cannot be ordered">
                                    <span>On sale</span>
                                </Tooltip>
                            </TableCell>

                            <TableCell width={110} align="right">
                                Actions
                            </TableCell>
                        </TableRow>
                    </TableHead>

                    <TableBody>
                        {tq.paged.map((it) => (
                            <TableRow key={it.itemId} hover>
                                <TableCell>{it.itemId}</TableCell>
                                <TableCell sx={{ color: "text.secondary" }}>{it.displayOrder ?? "—"}</TableCell>
                                <TableCell sx={{ opacity: it.stopped ? 0.5 : 1 }}>{it.name}</TableCell>
                                <TableCell sx={{ color: "text.secondary" }}>{it.type}</TableCell>
                                <TableCell sx={{ color: "text.secondary" }}>{it.products?.length ?? 0}</TableCell>
                                <TableCell sx={{ color: "text.secondary" }}>
                                    {it.basePrice}
                                </TableCell>

                                <TableCell>
                                    <Switch
                                        size="small"
                                        checked={!it.stopped}
                                        disabled={stopListMut.isPending}
                                        onChange={(e) => stopListMut.mutate({ id: it.itemId, stopped: !e.target.checked })}
                                        slotProps={{ input: { "aria-label": `${it.name} on sale` } }}
                                    />
                                </TableCell>

                                <TableCell align="right">
                                    <Stack direction="row" spacing={0.5} justifyContent="flex-end" alignItems="center">
                                        <Tooltip title="Edit">
                      <span>
                        <IconButton
                            size="small"
                            disabled={editMut.isPending}
                            onClick={() => {
                                setErrorText(null);
                                setSelected(it);
                                setEditOpen(true);
                            }}
                            sx={{ border: "1px solid rgba(255,255,255,0.18)", borderRadius: 2 }}
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
                                setDeleteError(null);
                                setDeleteTarget(it);
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
                                <TableCell colSpan={8}>
                                    {tq.search.trim() ? "No matches found" : "No items yet"}
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

            {createOpen && (
                <ItemDialog
                    open
                    mode="create"
                    loading={createMut.isPending}
                    errorText={errorText}
                    products={products}
                    onClose={() => {
                        if (!createMut.isPending) setCreateOpen(false);
                    }}
                    onSubmit={(dto) => createMut.mutate(dto)}
                />
            )}

            {editOpen && selected && (
                <ItemDialog
                    open
                    mode="edit"
                    loading={editMut.isPending}
                    errorText={errorText}
                    products={products}
                    initial={selected}
                    onClose={() => {
                        if (!editMut.isPending) {
                            setEditOpen(false);
                            setSelected(null);
                        }
                    }}
                    onSubmit={(dto) => {
                        if (!selected) return;
                        editMut.mutate({ id: selected.itemId, dto });
                    }}
                />
            )}

            <ConfirmDialog
                open={deleteOpen}
                title={`Delete item "${deleteTarget?.name ?? ""}"?`}
                loading={delMut.isPending}
                errorText={deleteError}
                onCancel={() => {
                    if (!delMut.isPending) {
                        setDeleteOpen(false);
                        setDeleteTarget(null);
                    }
                }}
                onConfirm={() => {
                    if (deleteTarget) delMut.mutate(deleteTarget.itemId);
                }}
            />
        </Box>
    );
}