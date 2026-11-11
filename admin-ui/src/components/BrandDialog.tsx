import { useState } from "react";
import {
    Alert,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    TextField,
} from "@mui/material";

// mounted only while open, so every opening starts from initialName
type Props = {
    open: boolean;
    mode: "create" | "edit";
    initialName?: string;
    loading?: boolean;
    errorText?: string | null;
    onClose: () => void;
    onSubmit: (name: string) => void;
};

export default function BrandDialog({
                                        open,
                                        mode,
                                        initialName = "",
                                        loading = false,
                                        errorText = null,
                                        onClose,
                                        onSubmit,
                                    }: Props) {
    const [name, setName] = useState(initialName);

    const trimmed = name.trim();
    const canSave = trimmed.length > 0 && trimmed !== initialName.trim() && !loading;

    return (
        <Dialog open={open} onClose={loading ? undefined : onClose} fullWidth maxWidth="sm">
            <DialogTitle>{mode === "create" ? "Create brand" : "Rename brand"}</DialogTitle>

            <DialogContent>
                {errorText ? (
                    <Alert severity="error" sx={{ mb: 2 }}>
                        {errorText}
                    </Alert>
                ) : null}

                <TextField
                    autoFocus
                    fullWidth
                    label="Brand name"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    onKeyDown={(e) => {
                        if (e.key === "Enter" && canSave) onSubmit(trimmed);
                    }}
                />
            </DialogContent>

            <DialogActions>
                <Button onClick={onClose} disabled={loading} variant="outlined">
                    Cancel
                </Button>
                <Button
                    onClick={() => onSubmit(trimmed)}
                    disabled={!canSave}
                    variant="contained"
                >
                    Save
                </Button>
            </DialogActions>
        </Dialog>
    );
}