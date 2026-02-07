import { useEffect, useMemo, useState } from "react";
import {
    Button,
    Chip,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Stack,
    TextField,
    Typography,
    Box,
} from "@mui/material";

type Props = {
    open: boolean;
    productName: string;
    currentStock: number;
    loading?: boolean;
    onClose: () => void;
    onSubmit: (delta: number) => void;
};

const QUICK_VALUES = [1, 5, 10, 20];

export default function AdjustStockDialog({
                                              open,
                                              productName,
                                              currentStock,
                                              loading = false,
                                              onClose,
                                              onSubmit,
                                          }: Props) {
    const [delta, setDelta] = useState<number>(0);

    useEffect(() => {
        if (open) setDelta(0);
    }, [open]);

    const canSave = useMemo(
        () => !loading && Number.isFinite(delta) && delta > 0,
        [loading, delta]
    );

    function applyQuick(v: number) {
        setDelta((prev) => (Number.isFinite(prev) ? prev : 0) + v);
    }

    return (
        <Dialog open={open} onClose={loading ? undefined : onClose} fullWidth maxWidth="xs">
            <DialogTitle>Add stock</DialogTitle>

            <DialogContent>
                <Stack spacing={1.5} sx={{ mt: 1 }}>
                    <Typography fontWeight={800}>{productName}</Typography>
                    <Typography color="text.secondary">Current stock: {currentStock}</Typography>

                    <TextField
                        label="Add quantity"
                        type="number"
                        value={delta}
                        onChange={(e) => setDelta(Number(e.target.value))}
                        inputProps={{ min: 0, step: 1 }}
                        helperText="Enter how many units arrived"
                        fullWidth
                        autoFocus
                        onKeyDown={(e) => {
                            if (e.key === "Enter" && canSave) onSubmit(delta);
                        }}
                    />

                    <Box>
                        <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                            Quick add
                        </Typography>

                        <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                            {QUICK_VALUES.map((v) => (
                                <Chip
                                    key={v}
                                    label={`+${v}`}
                                    clickable
                                    onClick={() => applyQuick(v)}
                                    disabled={loading}
                                    variant="outlined"
                                    sx={{
                                        borderColor: "rgba(242,162,58,0.35)",
                                        "&:hover": { backgroundColor: "rgba(242,162,58,0.10)" },
                                    }}
                                />
                            ))}

                            <Chip
                                label="Reset"
                                clickable
                                onClick={() => setDelta(0)}
                                disabled={loading}
                                variant="outlined"
                                sx={{
                                    borderColor: "rgba(255,255,255,0.18)",
                                    "&:hover": { backgroundColor: "rgba(255,255,255,0.06)" },
                                }}
                            />
                        </Stack>
                    </Box>
                </Stack>
            </DialogContent>

            <DialogActions>
                <Button onClick={onClose} disabled={loading} variant="outlined">
                    Cancel
                </Button>
                <Button onClick={() => onSubmit(delta)} disabled={!canSave} variant="contained">
                    Add stock
                </Button>
            </DialogActions>
        </Dialog>
    );
}