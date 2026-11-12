import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, Stack, TextField } from "@mui/material";

import { changePassword } from "../api/auth.api";
import { getApiErrorMessage } from "../api/error";

type Props = {
    open: boolean;
    onClose: (changed: boolean) => void;
};

// mounted only while open, so every opening starts empty
export default function ChangePasswordDialog({ open, onClose }: Props) {
    const [currentPassword, setCurrentPassword] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [repeat, setRepeat] = useState("");

    const mutation = useMutation({
        mutationFn: () => changePassword({ currentPassword, newPassword }),
        onSuccess: () => onClose(true),
    });

    const mismatch = repeat.length > 0 && repeat !== newPassword;
    const canSave = currentPassword.length > 0 && newPassword.length >= 8 && repeat === newPassword && !mutation.isPending;

    return (
        <Dialog open={open} onClose={mutation.isPending ? undefined : () => onClose(false)} fullWidth maxWidth="xs">
            <DialogTitle>Change password</DialogTitle>
            <DialogContent>
                {mutation.isError ? (
                    <Alert severity="error" sx={{ mb: 2 }}>
                        {getApiErrorMessage(mutation.error, "Failed to change the password")}
                    </Alert>
                ) : null}
                <Stack spacing={2} sx={{ pt: 1 }}>
                    <TextField
                        autoFocus
                        type="password"
                        label="Current password"
                        autoComplete="current-password"
                        value={currentPassword}
                        onChange={(e) => setCurrentPassword(e.target.value)}
                    />
                    <TextField
                        type="password"
                        label="New password"
                        autoComplete="new-password"
                        helperText="At least 8 characters"
                        value={newPassword}
                        onChange={(e) => setNewPassword(e.target.value)}
                    />
                    <TextField
                        type="password"
                        label="Repeat the new password"
                        autoComplete="new-password"
                        error={mismatch}
                        helperText={mismatch ? "Does not match" : " "}
                        value={repeat}
                        onChange={(e) => setRepeat(e.target.value)}
                    />
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button variant="outlined" disabled={mutation.isPending} onClick={() => onClose(false)}>
                    Cancel
                </Button>
                <Button variant="contained" disabled={!canSave} onClick={() => mutation.mutate()}>
                    Change
                </Button>
            </DialogActions>
        </Dialog>
    );
}
