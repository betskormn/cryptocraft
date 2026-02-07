import { useEffect, useState } from "react";
import { Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, TextField } from "@mui/material";

type Props = {
    open: boolean;
    title: string;
    initialJson?: object;
    loading?: boolean;
    errorText?: string | null;
    onClose: () => void;
    onSubmit: (payload: any) => void;
};

export default function JsonCrudDialog({
                                           open,
                                           title,
                                           initialJson = {},
                                           loading = false,
                                           errorText = null,
                                           onClose,
                                           onSubmit,
                                       }: Props) {
    const [value, setValue] = useState("");

    useEffect(() => {
        if (open) setValue(JSON.stringify(initialJson, null, 2));
    }, [open, initialJson]);

    function handleSubmit() {
        try {
            const payload = JSON.parse(value || "{}");
            onSubmit(payload);
        } catch (e) {
            // local error displayed as alert (simple)
        }
    }

    let parseError: string | null = null;
    try {
        JSON.parse(value || "{}");
    } catch {
        parseError = "Invalid JSON";
    }

    const canSave = !loading && !parseError;

    return (
        <Dialog open={open} onClose={loading ? undefined : onClose} fullWidth maxWidth="sm">
            <DialogTitle>{title}</DialogTitle>
            <DialogContent>
                {errorText ? <Alert severity="error" sx={{ mb: 2 }}>{errorText}</Alert> : null}
                {parseError ? <Alert severity="warning" sx={{ mb: 2 }}>{parseError}</Alert> : null}

                <TextField
                    fullWidth
                    multiline
                    minRows={10}
                    label="Payload (JSON)"
                    value={value}
                    onChange={(e) => setValue(e.target.value)}
                    sx={{ mt: 1 }}
                />
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose} disabled={loading} variant="outlined">Cancel</Button>
                <Button onClick={handleSubmit} disabled={!canSave} variant="contained">Save</Button>
            </DialogActions>
        </Dialog>
    );
}
``