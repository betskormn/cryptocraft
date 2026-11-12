import { useState } from "react";
import {
    Alert,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    MenuItem,
    Stack,
    TextField,
} from "@mui/material";

import { ROLE_HINT, ROLE_LABEL, type VenueRole } from "../api/auth.api";
import type { StaffRequestDto } from "../api/staff.api";

type Props = {
    open: boolean;
    loading?: boolean;
    errorText?: string | null;
    onClose: () => void;
    onSubmit: (dto: StaffRequestDto) => void;
};

// mounted only while open (see StaffListPage), so every opening starts empty
export default function StaffDialog({ open, loading = false, errorText = null, onClose, onSubmit }: Props) {
    const [email, setEmail] = useState("");
    const [displayName, setDisplayName] = useState("");
    const [role, setRole] = useState<VenueRole>("STAFF");
    const [password, setPassword] = useState("");

    const trimmedEmail = email.trim();
    const validEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmedEmail);
    // a person who already has an account keeps their name and password
    const canSave = validEmail && (password.length === 0 || password.length >= 8) && !loading;

    const submit = () => {
        if (!canSave) return;
        onSubmit({
            email: trimmedEmail,
            displayName: displayName.trim() || undefined,
            role,
            password: password || undefined,
        });
    };

    return (
        <Dialog open={open} onClose={loading ? undefined : onClose} fullWidth maxWidth="xs">
            <DialogTitle>Add a person</DialogTitle>
            <DialogContent>
                {errorText ? (
                    <Alert severity="error" sx={{ mb: 2 }}>
                        {errorText}
                    </Alert>
                ) : null}
                <Stack spacing={2} sx={{ pt: 1 }}>
                    <TextField
                        autoFocus
                        type="email"
                        label="Email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        helperText="Their login. Someone who already has an account just gets this venue."
                    />
                    <TextField select label="Role" value={role} onChange={(e) => setRole(e.target.value as VenueRole)} helperText={ROLE_HINT[role]}>
                        {(Object.keys(ROLE_LABEL) as VenueRole[]).map((r) => (
                            <MenuItem key={r} value={r}>
                                {ROLE_LABEL[r]}
                            </MenuItem>
                        ))}
                    </TextField>
                    <TextField
                        label="Name"
                        value={displayName}
                        onChange={(e) => setDisplayName(e.target.value)}
                        helperText="For a new account"
                    />
                    <TextField
                        type="password"
                        label="Initial password"
                        autoComplete="new-password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        error={password.length > 0 && password.length < 8}
                        helperText="For a new account, at least 8 characters; they change it after signing in"
                    />
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose} disabled={loading} variant="outlined">
                    Cancel
                </Button>
                <Button onClick={submit} disabled={!canSave} variant="contained">
                    Add
                </Button>
            </DialogActions>
        </Dialog>
    );
}
