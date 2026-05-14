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
    Stack,
    MenuItem,
    TextField,
} from "@mui/material";

import type { BrandResponseDto } from "../api/brands.api";
import { UNIT_OPTIONS, UNIT_LABEL, type Unit, type ProductRequestDto, type ProductResponseDto } from "../api/products.api";

type Props = {
    open: boolean;
    mode: "create" | "edit";
    loading?: boolean;
    errorText?: string | null;
    brands: BrandResponseDto[];
    initial?: ProductResponseDto | null;
    onClose: () => void;
    onSubmit: (dto: ProductRequestDto) => void;
};

export default function ProductDialog({
                                          open,
                                          mode,
                                          loading = false,
                                          errorText = null,
                                          brands,
                                          initial,
                                          onClose,
                                          onSubmit,
                                      }: Props) {
    const [name, setName] = useState("");
    const [description, setDescription] = useState("");
    const [image, setImage] = useState("");
    const [quantity, setQuantity] = useState<number>(0);
    const [unit, setUnit] = useState<Unit>("MILLILITERS");
    const [brandId, setBrandId] = useState<number | "">("");

    useEffect(() => {
        if (!open) return;

        if (mode === "edit" && initial) {
            setName(initial.name ?? "");
            setDescription(initial.description ?? "");
            setImage(initial.image ?? "");
            setQuantity(initial.quantityInStock ?? 0); // response -> request mapping
            setUnit((initial.unit ?? "MILLILITERS") as Unit);
            setBrandId(initial.brandId ?? "");
        } else {
            setName("");
            setDescription("");
            setImage("");
            setQuantity(0);
            setUnit("MILLILITERS");
            setBrandId(brands[0]?.brandId ?? "");
        }
    }, [open, mode, initial, brands]);

    const canSave = useMemo(() => {
        return (
            !loading &&
            name.trim().length > 0 &&
            description.trim().length > 0 &&
            quantity >= 0 &&
            unit.length > 0 &&
            brandId !== ""
        );
    }, [loading, name, description, quantity, unit, brandId]);

    function handleSubmit() {
        if (!canSave) return;

        onSubmit({
            name: name.trim(),
            description: description.trim(),
            image: image.trim() ? image.trim() : null,
            quantity: Number(quantity),
            unit,
            brandId: Number(brandId),
        });
    }

    return (
        <Dialog open={open} onClose={loading ? undefined : onClose} fullWidth maxWidth="md">
            <DialogTitle>{mode === "create" ? "Create product" : "Edit product"}</DialogTitle>

            <DialogContent>
                {errorText ? (
                    <Alert severity="error" sx={{ mb: 2 }}>
                        {errorText}
                    </Alert>
                ) : null}

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

                    <Grid item xs={12} md={3}>
                        <Stack direction="row" alignItems="center" spacing={0.5}>
                            <IconButton
                                size="small"
                                onClick={() => setQuantity((q) => Math.max(0, q - 1))}
                            >
                                −
                            </IconButton>

                            <TextField
                                value={quantity}
                                label="Quantity"
                                onChange={(e) => setQuantity(Number(e.target.value))}
                                size="small"
                                sx={{
                                    width: 80,
                                    "& input": {
                                        textAlign: "center",
                                    },
                                }}
                            />
                            <IconButton
                                size="small"
                                onClick={() => setQuantity((q) => q + 1)}
                            >
                                +
                            </IconButton>
                        </Stack>
                    </Grid>

                    <Grid item xs={12} md={3}>
                        <TextField
                            label="Unit"
                            select
                            value={unit}
                            onChange={(e) => setUnit(e.target.value as Unit)}
                            fullWidth
                            required
                        >
                            {UNIT_OPTIONS.map((u) => (
                                <MenuItem key={u} value={u}>
                                    {UNIT_LABEL[u]}
                                </MenuItem>
                            ))}
                        </TextField>
                    </Grid>

                    <Grid item xs={12} md={6}>
                        <TextField
                            label="Brand"
                            select
                            value={brandId}
                            onChange={(e) => setBrandId(Number(e.target.value))}
                            fullWidth
                            required
                        >
                            {brands.map((b) => (
                                <MenuItem key={b.brandId} value={b.brandId}>
                                    {b.name} (#{b.brandId})
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

                    <Grid item xs={12}>
                        <TextField
                            label="Description"
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                            fullWidth
                            required
                            multiline
                            minRows={4}
                        />
                    </Grid>
                </Grid>
            </DialogContent>

            <DialogActions>
                <Button onClick={onClose} disabled={loading} variant="outlined">
                    Cancel
                </Button>
                <Button onClick={handleSubmit} disabled={!canSave} variant="contained">
                    Save
                </Button>
            </DialogActions>
        </Dialog>
    );
}