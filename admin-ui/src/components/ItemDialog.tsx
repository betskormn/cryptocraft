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
    const [rows, setRows] = useState<ItemProductRow[]>([{ productId: "", quantity: 1 }]);

    useEffect(() => {
        if (!open) return;

        if (mode === "edit" && initial) {
            setName(initial.name ?? "");
            setRecipe(initial.recipe ?? "");
            setImage(initial.image ?? "");
            setType((initial.type ?? "") as Type);

            // response products: ProductResponseDto[] (без quantity рецепту)
            // Тому для редагування нам потрібна кількість з бекенду.
            // Якщо бекенд не повертає quantity для item-products, то редагувати точні кількості неможливо.
            // В такому разі ставимо quantity=1 за замовчуванням.
            setRows(
                (initial.products?.length ? initial.products : []).map((p) => ({
                    productId: p.productId,
                    quantity: 1,
                })) || [{ productId: "", quantity: 1 }]
            );
        } else {
            setName("");
            setRecipe("");
            setImage("");
            setType("");
            setRows([{ productId: "", quantity: 1 }]);
        }
    }, [open, mode, initial]);

    const canSave = useMemo(() => {
        const validBase = !loading && name.trim().length > 0 && String(type).trim().length > 0;
        const validRows =
            rows.length > 0 &&
            rows.every((r) => r.productId !== "" && Number(r.quantity) > 0);
        return validBase && validRows;
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

                <Grid container spacing={2} sx={{ mt: 0.5 }}>
                    <Grid item xs={12} md={6}>
                        <TextField
                            label="Name"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            fullWidth
                            required
                            autoFocus
                        />
                    </Grid>


                    <Grid item xs={12} md={6}>
                        <TextField
                            label="Type"
                            select
                            value={type}
                            onChange={(e) => setType(e.target.value as Type)}
                            fullWidth
                            required
                        >
                            {TYPE_OPTIONS.map((t) => (
                                <MenuItem key={t} value={t}>
                                    {TYPE_LABEL[t]}
                                </MenuItem>
                            ))}
                        </TextField>
                    </Grid>


                    <Grid item xs={12} md={6}>
                        <TextField
                            label="Image URL"
                            value={image}
                            onChange={(e) => setImage(e.target.value)}
                            fullWidth
                        />
                    </Grid>

                    <Grid item xs={12} md={6}>
                        <TextField
                            label="Recipe"
                            value={recipe}
                            onChange={(e) => setRecipe(e.target.value)}
                            fullWidth
                            multiline
                            minRows={2}
                        />
                    </Grid>
                </Grid>

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

                                <TextField
                                    label="Quantity"
                                    type="number"
                                    value={r.quantity}
                                    onChange={(e) => updateRow(idx, { quantity: Number(e.target.value) })}
                                    inputProps={{ min: 1 }}
                                    sx={{ width: { md: 180 } }}
                                />

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