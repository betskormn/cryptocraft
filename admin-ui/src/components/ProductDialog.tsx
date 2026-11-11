import { useState } from "react";
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
import { addQuantity, formatQuantity, parseQuantity } from "../utils/quantity";

// mounted only while open (see ProductListPage), so the form starts from `initial` on every opening
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
    const editing = mode === "edit" && initial ? initial : null;

    const [name, setName] = useState(editing?.name ?? "");
    const [description, setDescription] = useState(editing?.description ?? "");
    const [image, setImage] = useState(editing?.image ?? "");
    // initial stock on create only; afterwards stock changes through Add stock / Write off
    // text so decimals like "1.5" can be typed
    const [quantity, setQuantity] = useState(formatQuantity(editing?.quantityInStock ?? 0));
    const [unit, setUnit] = useState<Unit>(editing?.unit ?? "MILLILITERS");
    const [brandId, setBrandId] = useState<number | "">(editing?.brandId ?? brands[0]?.brandId ?? "");

    const canSave =
        !loading &&
        name.trim().length > 0 &&
        description.trim().length > 0 &&
        parseQuantity(quantity, { allowZero: true }) !== null &&
        unit.length > 0 &&
        brandId !== "";

    function handleSubmit() {
        if (!canSave) return;

        onSubmit({
            name: name.trim(),
            description: description.trim(),
            image: image.trim() ? image.trim() : null,
            quantity: parseQuantity(quantity, { allowZero: true }) ?? 0,
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
                    <Grid size={{ xs: 12, md: 6 }}>
                        <TextField
                            label="Name"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            fullWidth
                            required
                            autoFocus
                        />
                    </Grid>

                    <Grid size={{ xs: 12, md: 3 }}>
                        <Stack direction="row" alignItems="center" spacing={0.5}>
                            <IconButton
                                size="small"
                                disabled={editing !== null}
                                onClick={() =>
                                    setQuantity((q) => formatQuantity(Math.max(0, addQuantity(parseQuantity(q, { allowZero: true }) ?? 0, -1))))
                                }
                            >
                                −
                            </IconButton>

                            <TextField
                                value={quantity}
                                label={editing ? "Stock" : "Initial stock"}
                                title={editing ? "Use Add stock / Write off in the product list" : undefined}
                                disabled={editing !== null}
                                onChange={(e) => setQuantity(e.target.value)}
                                error={parseQuantity(quantity, { allowZero: true }) === null}
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
                                disabled={editing !== null}
                                onClick={() =>
                                    setQuantity((q) => formatQuantity(addQuantity(parseQuantity(q, { allowZero: true }) ?? 0, 1)))
                                }
                            >
                                +
                            </IconButton>
                        </Stack>
                    </Grid>

                    <Grid size={{ xs: 12, md: 3 }}>
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

                    <Grid size={{ xs: 12, md: 6 }}>
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

                    <Grid size={{ xs: 12, md: 6 }}>
                        <TextField
                            label="Image URL"
                            value={image}
                            onChange={(e) => setImage(e.target.value)}
                            fullWidth
                        />
                    </Grid>

                    <Grid size={{ xs: 12 }}>
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