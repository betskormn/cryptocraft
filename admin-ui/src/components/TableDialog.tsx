import { useState } from "react";
import {
    Alert,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    FormControlLabel,
    Stack,
    Switch,
    TextField,
} from "@mui/material";

import type { VenueTableRequestDto, VenueTableResponseDto } from "../api/tables.api";

// mounted only while open (see TableListPage), so every opening starts from `initial`
type Props = {
    open: boolean;
    initial?: VenueTableResponseDto | null;
    loading?: boolean;
    errorText?: string | null;
    onClose: () => void;
    onSubmit: (dto: VenueTableRequestDto) => void;
};

export default function TableDialog({
                                        open,
                                        initial = null,
                                        loading = false,
                                        errorText = null,
                                        onClose,
                                        onSubmit,
                                    }: Props) {
    const [label, setLabel] = useState(initial?.label ?? "");
    const [active, setActive] = useState(initial?.active ?? true);

    const trimmed = label.trim();
    const canSave = trimmed.length > 0 && trimmed.length <= 50 && !loading;
    const submit = () => {
        if (canSave) onSubmit({ label: trimmed, active });
    };

    return (
        <Dialog open={open} onClose={loading ? undefined : onClose} fullWidth maxWidth="xs">
            <DialogTitle>{initial ? "Edit table" : "New table"}</DialogTitle>

            <DialogContent>
                {errorText ? (
                    <Alert severity="error" sx={{ mb: 2 }}>
                        {errorText}
                    </Alert>
                ) : null}

                <Stack spacing={2} sx={{ pt: 1 }}>
                    <TextField
                        autoFocus
                        fullWidth
                        label="Label"
                        placeholder="5, Bar 2, Terrace 1"
                        value={label}
                        onChange={(e) => setLabel(e.target.value)}
                        onKeyDown={(e) => {
                            if (e.key === "Enter") submit();
                        }}
                    />
                    <FormControlLabel
                        control={<Switch checked={active} onChange={(e) => setActive(e.target.checked)} />}
                        label="In service (guests can join by QR code)"
                    />
                </Stack>
            </DialogContent>

            <DialogActions>
                <Button onClick={onClose} disabled={loading} variant="outlined">
                    Cancel
                </Button>
                <Button onClick={submit} disabled={!canSave} variant="contained">
                    Save
                </Button>
            </DialogActions>
        </Dialog>
    );
}
