import { useState } from "react";
import {
    Alert,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    IconButton,
    MenuItem,
    Paper,
    Stack,
    TextField,
    Typography,
} from "@mui/material";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";
import AddIcon from "@mui/icons-material/Add";

import { TYPE_OPTIONS, TYPE_LABEL, type Type } from "../api/items.api";
import type { ItemRequestDto, ItemResponseDto } from "../api/items.api";
import { UNIT_LABEL, type ProductResponseDto } from "../api/products.api";
import { addQuantity, formatQuantity, parseQuantity } from "../utils/quantity";

// quantity kept as text so decimals like "0.05" can be typed
type ItemProductRow = { productId: number | ""; quantity: string };

// mounted only while open (see ItemListPage), so the form starts from `initial` on every opening
type Props = {
    open: boolean;
    mode: "create" | "edit";
    loading?: boolean;
    errorText?: string | null;
    products: ProductResponseDto[];
    initial?: ItemResponseDto | null;
    onClose: () => void;
    onSubmit: (dto: ItemRequestDto) => void;
};

export default function ItemDialog({
                                       open,
                                       mode,
                                       loading = false,
                                       errorText = null,
                                       products,
                                       initial,
                                       onClose,
                                       onSubmit,
                                   }: Props) {
    const editing = mode === "edit" && initial ? initial : null;

    const [name, setName] = useState(editing?.name ?? "");
    const [recipe, setRecipe] = useState(editing?.recipe ?? "");
    const [image, setImage] = useState(editing?.image ?? "");
    const [type, setType] = useState<Type | "">((editing?.type as Type | undefined) ?? "");
    // kept as text so a field can be cleared while typing
    const [basePrice, setBasePrice] = useState(editing ? String(editing.basePrice) : "");
    const [minPrice, setMinPrice] = useState(editing ? String(editing.minPrice) : "");
    const [maxPrice, setMaxPrice] = useState(editing ? String(editing.maxPrice) : "");
    const [displayOrder, setDisplayOrder] = useState(editing?.displayOrder != null ? String(editing.displayOrder) : "");
    const [rows, setRows] = useState<ItemProductRow[]>(() =>
        editing && editing.products.length > 0
            ? editing.products
                .slice()
                .sort((a, b) => a.name.localeCompare(b.name))
                .map((p) => ({ productId: p.productId, quantity: formatQuantity(p.quantity ?? 1) }))
            : [{ productId: "", quantity: "1" }]
    );

    const base = Number(basePrice);
    const min = Number(minPrice);
    const max = Number(maxPrice);
    const pricesFilled = basePrice !== "" && minPrice !== "" && maxPrice !== "";
    const pricesPositive = base > 0 && min > 0 && max > 0;
    const priceRangeValid = min <= base && base <= max;
    const priceError = !pricesFilled
        ? null
        : !pricesPositive
            ? "Prices must be greater than 0"
            : !priceRangeValid
                ? "Required: min ≤ base ≤ max"
                : null;

    const validRows =
        rows.length > 0 && rows.every((r) => r.productId !== "" && parseQuantity(r.quantity) !== null);
    const displayOrderValid = displayOrder.trim() === "" || /^\d+$/.test(displayOrder.trim());
    const canSave =
        !loading &&
        name.trim().length > 0 &&
        type !== "" &&
        validRows &&
        displayOrderValid &&
        pricesFilled &&
        priceError === null;

    function addRow() {
        setRows((prev) => [...prev, { productId: "", quantity: "1" }]);
    }

    function removeRow(idx: number) {
        setRows((prev) => prev.filter((_, i) => i !== idx));
    }

    function updateRow(idx: number, patch: Partial<ItemProductRow>) {
        setRows((prev) => prev.map((r, i) => (i === idx ? { ...r, ...patch } : r)));
    }

    function handleSubmit() {
        // canSave includes type !== "", which narrows type for TypeScript below
        if (!canSave) return;

        onSubmit({
            name: name.trim(),
            recipe: recipe.trim() ? recipe.trim() : null,
            image: image.trim() ? image.trim() : null,
            type,
            basePrice: base,
            minPrice: min,
            maxPrice: max,
            displayOrder: displayOrder.trim() === "" ? null : Number(displayOrder),
            products: rows.map((r) => ({
                productId: Number(r.productId),
                quantity: parseQuantity(r.quantity) ?? 0,
            })),
        });
    }

    return (
        <Dialog open={open} onClose={loading ? undefined : onClose} fullWidth maxWidth="md">
            <DialogTitle>{mode === "create" ? "Create item" : "Edit item"}</DialogTitle>

            <DialogContent>
                {errorText ? <Alert severity="error" sx={{ mb: 2 }}>{errorText}</Alert> : null}

                <Stack spacing={2} sx={{ mt: 1 }}>
                    <TextField
                        label="Name"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        fullWidth
                        required
                    />

                    <Stack direction={{ xs: "column", md: "row" }} spacing={2}>
                        <TextField
                            label="Type"
                            select
                            value={type}
                            onChange={(e) => setType(e.target.value as Type)}
                            required
                            sx={{
                                minWidth: 80,
                                maxWidth: 120,
                                width: { xs: "100%", md: 200 },
                                flexShrink: 0
                            }}
                        >
                            {TYPE_OPTIONS.map((t) => (
                                <MenuItem key={t} value={t}>
                                    {TYPE_LABEL[t]}
                                </MenuItem>
                            ))}
                        </TextField>
                        <TextField
                            label="Image URL"
                            value={image}
                            onChange={(e) => setImage(e.target.value)}
                            fullWidth
                        />
                        <TextField
                            label="Menu position"
                            type="number"
                            value={displayOrder}
                            onChange={(e) => setDisplayOrder(e.target.value)}
                            error={!displayOrderValid}
                            helperText="Order on menus and TV; empty = after numbered items, by name"
                            slotProps={{ htmlInput: { min: 0, step: 1 } }}
                            sx={{ width: { xs: "100%", md: 220 }, flexShrink: 0 }}
                        />

                    </Stack>
                    <TextField
                        label="Recipe"
                        value={recipe}
                        onChange={(e) => setRecipe(e.target.value)}
                        fullWidth
                        multiline
                        minRows={4}
                    />
                    <Stack spacing={1}>
                        <Typography fontWeight={700}>
                            Pricing
                        </Typography>

                        <Stack direction={{ xs: "column", md: "row" }} spacing={2}>
                            <TextField
                                label="Base price"
                                type="number"
                                value={basePrice}
                                onChange={(e) => setBasePrice(e.target.value)}
                                fullWidth
                                required
                                error={priceError !== null}
                                helperText={priceError ?? "min ≤ base ≤ max"}
                            />

                            <TextField
                                label="Min price"
                                type="number"
                                value={minPrice}
                                onChange={(e) => setMinPrice(e.target.value)}
                                error={priceError !== null}
                                fullWidth
                                required
                            />

                            <TextField
                                label="Max price"
                                type="number"
                                value={maxPrice}
                                onChange={(e) => setMaxPrice(e.target.value)}
                                error={priceError !== null}
                                fullWidth
                                required
                            />
                        </Stack>
                    </Stack>
                </Stack>


                <Paper variant="outlined" sx={{ mt: 2, p: 2 }}>
                    <Stack direction="row" alignItems="center" justifyContent="space-between" mb={1}>
                        <Typography fontWeight={800}>Products in item</Typography>
                        <Button startIcon={<AddIcon />} onClick={addRow} variant="outlined" disabled={loading}>
                            Add product
                        </Button>
                    </Stack>

                    <Stack spacing={1.5}>
                        {rows.map((r, idx) => (
                            <Stack key={idx} direction={{ xs: "column", md: "row" }} spacing={1.5} alignItems={{ md: "center" }}>
                                <TextField
                                    select
                                    fullWidth
                                    label="Product"
                                    value={r.productId}
                                    onChange={(e) => updateRow(idx, { productId: Number(e.target.value) })}
                                >
                                    {products.map((p) => (
                                        <MenuItem
                                            key={p.productId}
                                            value={p.productId}
                                            // one recipe line per product: adjust the quantity instead
                                            disabled={rows.some((other, i) => i !== idx && other.productId === p.productId)}
                                        >
                                            {p.name} (#{p.productId}) · {UNIT_LABEL[p.unit] ?? p.unit}
                                        </MenuItem>
                                    ))}
                                </TextField>

                                <Stack direction="row" alignItems="center" spacing={1}>
                                    <IconButton
                                        size="small"
                                        onClick={() =>
                                            updateRow(idx, {
                                                quantity: formatQuantity(Math.max(1, addQuantity(parseQuantity(r.quantity) ?? 1, -1))),
                                            })
                                        }
                                    >
                                        −
                                    </IconButton>

                                    <TextField
                                        value={r.quantity}
                                        label="Quantity"
                                        onChange={(e) => updateRow(idx, { quantity: e.target.value })}
                                        error={parseQuantity(r.quantity) === null}
                                        size="small"
                                        sx={{
                                            width: 90,
                                            "& input": {
                                                textAlign: "center",
                                            },
                                        }}
                                    />

                                    <IconButton
                                        size="small"
                                        onClick={() =>
                                            updateRow(idx, {
                                                quantity: formatQuantity(addQuantity(parseQuantity(r.quantity) ?? 0, 1)),
                                            })
                                        }
                                    >
                                        +
                                    </IconButton>
                                </Stack>


                                <IconButton
                                    onClick={() => removeRow(idx)}
                                    disabled={loading || rows.length === 1}
                                    color="error"
                                >
                                    <DeleteOutlineIcon />
                                </IconButton>
                            </Stack>
                        ))}
                    </Stack>
                </Paper>
            </DialogContent>

            <DialogActions>
                <Button onClick={onClose} disabled={loading} variant="outlined">Cancel</Button>
                <Button onClick={handleSubmit} disabled={!canSave} variant="contained">Save</Button>
            </DialogActions>
        </Dialog>
    );
}