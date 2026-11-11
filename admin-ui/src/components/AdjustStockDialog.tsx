import { useState } from "react";
import {
    Alert,
    Button,
    Chip,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Stack,
    TextField,
    ToggleButton,
    ToggleButtonGroup,
    Typography,
    Box,
} from "@mui/material";

import { addQuantity, formatQuantity, parseQuantity } from "../utils/quantity";

type Mode = "add" | "writeOff";

// mounted only while open (see ProductListPage), so every opening starts from zero
type Props = {
    open: boolean;
    productName: string;
    currentStock: number;
    loading?: boolean;
    errorText?: string | null;
    onClose: () => void;
    // positive for a delivery, negative for a write-off
    onSubmit: (delta: number) => void;
};

const QUICK_VALUES = [1, 5, 10, 20];

export default function AdjustStockDialog({
                                              open,
                                              productName,
                                              currentStock,
                                              loading = false,
                                              errorText = null,
                                              onClose,
                                              onSubmit,
                                          }: Props) {
    const [mode, setMode] = useState<Mode>("add");
    // text so decimals like "0.5" can be typed
    const [amountText, setAmountText] = useState("0");

    const amount = parseQuantity(amountText) ?? 0;
    const delta = mode === "add" ? amount : -amount;
    const newStock = addQuantity(currentStock, delta);
    const tooMuch = mode === "writeOff" && amount > currentStock;
    const canSave = !loading && amount > 0 && !tooMuch;

    function applyQuick(v: number) {
        setAmountText((prev) => formatQuantity(addQuantity(parseQuantity(prev) ?? 0, v)));
    }

    function submit() {
        if (canSave) onSubmit(delta);
    }

    return (
        <Dialog open={open} onClose={loading ? undefined : onClose} fullWidth maxWidth="xs">
            <DialogTitle>Adjust stock</DialogTitle>

            <DialogContent>
                {errorText ? (
                    <Alert severity="error" sx={{ mb: 2 }}>
                        {errorText}
                    </Alert>
                ) : null}

                <Stack spacing={1.5} sx={{ mt: 1 }}>
                    <Typography fontWeight={800}>{productName}</Typography>
                    <Typography color="text.secondary">
                        Current stock: {formatQuantity(currentStock)}
                        {amount > 0 && !tooMuch ? ` → ${formatQuantity(newStock)}` : ""}
                    </Typography>

                    <ToggleButtonGroup
                        exclusive
                        fullWidth
                        size="small"
                        value={mode}
                        onChange={(_, value: Mode | null) => value && setMode(value)}
                    >
                        <ToggleButton value="add">Add delivery</ToggleButton>
                        <ToggleButton value="writeOff">Write off</ToggleButton>
                    </ToggleButtonGroup>

                    <TextField
                        label={mode === "add" ? "Quantity to add" : "Quantity to write off"}
                        type="number"
                        value={amountText}
                        onChange={(e) => setAmountText(e.target.value)}
                        slotProps={{ htmlInput: { min: 0, step: "any" } }}
                        error={tooMuch || (amountText.trim() !== "" && amountText.trim() !== "0" && parseQuantity(amountText) === null)}
                        helperText={
                            tooMuch
                                ? `Only ${formatQuantity(currentStock)} in stock`
                                : mode === "add"
                                    ? "How many units arrived"
                                    : "Spoiled, broken, used outside of orders, inventory correction"
                        }
                        fullWidth
                        autoFocus
                        onKeyDown={(e) => {
                            if (e.key === "Enter") submit();
                        }}
                    />

                    <Box>
                        <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                            Quick values
                        </Typography>

                        <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                            {QUICK_VALUES.map((v) => (
                                <Chip
                                    key={v}
                                    label={`${mode === "add" ? "+" : "−"}${v}`}
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
                                onClick={() => setAmountText("0")}
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
                <Button
                    onClick={submit}
                    disabled={!canSave}
                    variant="contained"
                    color={mode === "add" ? "primary" : "error"}
                >
                    {mode === "add" ? "Add stock" : "Write off"}
                </Button>
            </DialogActions>
        </Dialog>
    );
}
