import { useEffect, useMemo, useState } from "react";
import {
    Alert,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Grid,
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
import type { ProductResponseDto } from "../api/products.api";

type ItemProductRow = { productId: number | ""; quantity: number };

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
    const [name, setName] = useState("");
    const [recipe, setRecipe] = useState("");
    const [image, setImage] = useState("");
    const [type, setType] = useState<Type>("");
    const [basePrice, setBasePrice] = useState<number>(0);
    const [minPrice, setMinPrice] = useState<number>(0);
    const [maxPrice, setMaxPrice] = useState<number>(0);
    const [rows, setRows] = useState<ItemProductRow[]>([{ productId: "", quantity: 1 }]);

    useEffect(() => {
        if (!open) return;

        if (mode === "edit" && initial) {
            setName(initial.name ?? "");
            setRecipe(initial.recipe ?? "");
            setImage(initial.image ?? "");
            setType((initial.type ?? "") as Type);
            setBasePrice(initial.basePrice ?? 0);
            setMinPrice(initial.minPrice ?? 0);
            setMaxPrice(initial.maxPrice ?? 0);
            setRows(
                (initial.products ?? [])
                    .slice()
                    .sort((a, b) => a.name.localeCompare(b.name))
                    .map((p) => ({
                        productId: p.productId,
                        quantity: p.quantity ?? 1,
                    }))
            );
        } else {
            setName("");
            setRecipe("");
            setImage("");
            setType("");
            setBasePrice(0);
            setMinPrice(0);
            setMaxPrice(0);
            setRows([{ productId: "", quantity: 1 }]);
        }
    }, [open, mode, initial]);

    const canSave = useMemo(() => {
        const validBase = !loading && name.trim().length > 0 && String(type).trim().length > 0;
        const validRows =
            rows.length > 0 &&
            rows.every((r) => r.productId !== "" && Number(r.quantity) > 0);
        const validPrices =
            basePrice > 0 &&
            minPrice > 0 &&
            maxPrice > 0;
        return validBase && validRows && validPrices;
    }, [loading, name, type, rows]);

    function addRow() {
        setRows((prev) => [...prev, { productId: "", quantity: 1 }]);
    }

    function removeRow(idx: number) {
        setRows((prev) => prev.filter((_, i) => i !== idx));
    }

    function updateRow(idx: number, patch: Partial<ItemProductRow>) {
        setRows((prev) => prev.map((r, i) => (i === idx ? { ...r, ...patch } : r)));
    }

    function handleSubmit() {
        if (!canSave) return;

        onSubmit({
            name: name.trim(),
            recipe: recipe.trim() ? recipe.trim() : null,
            image: image.trim() ? image.trim() : null,
            type: String(type).trim(),
            basePrice,
            minPrice,
            maxPrice,
            products: rows.map((r) => ({
                productId: Number(r.productId),
                quantity: Number(r.quantity),
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
                                onChange={(e) => setBasePrice(Number(e.target.value))}
                                fullWidth
                                required
                                helperText="min ≤ base ≤ max"
                            />

                            <TextField
                                label="Min price"
                                type="number"
                                value={minPrice}
                                onChange={(e) => setMinPrice(Number(e.target.value))}
                                fullWidth
                                required
                            />

                            <TextField
                                label="Max price"
                                type="number"
                                value={maxPrice}
                                onChange={(e) => setMaxPrice(Number(e.target.value))}
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
                                        <MenuItem key={p.productId} value={p.productId}>
                                            {p.name} (#{p.productId})
                                        </MenuItem>
                                    ))}
                                </TextField>

                                <Stack direction="row" alignItems="center" spacing={1}>
                                    <IconButton
                                        size="small"
                                        onClick={() =>
                                            updateRow(idx, { quantity: Math.max(1, r.quantity - 1) })
                                        }
                                    >
                                        −
                                    </IconButton>

                                    <TextField
                                        value={r.quantity}
                                        label="Quantity"
                                        onChange={(e) =>
                                            updateRow(idx, { quantity: Number(e.target.value) })
                                        }
                                        size="small"
                                        sx={{
                                            width: 70,
                                            "& input": {
                                                textAlign: "center",
                                            },
                                        }}
                                    />

                                    <IconButton
                                        size="small"
                                        onClick={() =>
                                            updateRow(idx, { quantity: r.quantity + 1 })
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