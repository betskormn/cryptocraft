import { useState } from "react";
import {
    Button,
    Chip,
    Dialog,
    DialogActions,
    DialogContent,
    DialogContentText,
    DialogTitle,
    Stack,
    TextField,
} from "@mui/material";

import type { KitchenLineResponseDto } from "../api/kitchen.api";

type Props = {
    open: boolean;
    line: KitchenLineResponseDto;
    loading?: boolean;
    onClose: () => void;
    onConfirm: (reason?: string) => void;
};

const QUICK_REASONS = ["Ran out", "Guest changed their mind", "Sent by mistake"];

// cancelling takes the line off the guest's bill; the reason is shown there
export default function CancelLineDialog({ open, line, loading = false, onClose, onConfirm }: Props) {
    const [reason, setReason] = useState("");

    return (
        <Dialog open={open} onClose={loading ? undefined : onClose} fullWidth maxWidth="xs">
            <DialogTitle>
                Cancel {line.quantity}× {line.itemName}?
            </DialogTitle>
            <DialogContent>
                <DialogContentText sx={{ mb: 2 }}>
                    Table {line.tableLabel}. The line is taken off the guest's bill; the stock already written off is not returned.
                </DialogContentText>
                <Stack direction="row" spacing={1} useFlexGap flexWrap="wrap" sx={{ mb: 2 }}>
                    {QUICK_REASONS.map((r) => (
                        <Chip key={r} label={r} variant={reason === r ? "filled" : "outlined"} onClick={() => setReason(r)} />
                    ))}
                </Stack>
                <TextField
                    label="Reason"
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                    fullWidth
                    size="small"
                    slotProps={{ htmlInput: { maxLength: 255 } }}
                />
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose} disabled={loading} variant="outlined">
                    Back
                </Button>
                <Button
                    onClick={() => onConfirm(reason.trim() || undefined)}
                    disabled={loading}
                    color="error"
                    variant="contained"
                >
                    Cancel line
                </Button>
            </DialogActions>
        </Dialog>
    );
}
