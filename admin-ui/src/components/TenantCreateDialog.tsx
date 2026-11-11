import { useState } from "react";
import {
    Alert,
    Button,
    Checkbox,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    FormControlLabel,
    Stack,
    TextField,
} from "@mui/material";

import type { TenantRequestDto } from "../api/tenants.api";

// mounted only while open (see TenantSwitcher), so every opening starts with an empty form
type Props = {
    open: boolean;
    loading?: boolean;
    errorText?: string | null;
    onClose: () => void;
    onSubmit: (dto: TenantRequestDto) => void;
};

const SLUG_PATTERN = /^[a-z0-9]+(-[a-z0-9]+)*$/;

const UA_TO_LATIN: Record<string, string> = {
    а: "a", б: "b", в: "v", г: "h", ґ: "g", д: "d", е: "e", є: "ie", ж: "zh", з: "z", и: "y", і: "i",
    ї: "i", й: "i", к: "k", л: "l", м: "m", н: "n", о: "o", п: "p", р: "r", с: "s", т: "t", у: "u",
    ф: "f", х: "kh", ц: "ts", ч: "ch", ш: "sh", щ: "shch", ь: "", ю: "iu", я: "ia",
};

function toSlug(name: string): string {
    return name
        .toLowerCase()
        .split("")
        .map((c) => UA_TO_LATIN[c] ?? c)
        .join("")
        .normalize("NFKD")
        .replace(/[^a-z0-9]+/g, "-")
        .replace(/^-+|-+$/g, "")
        .slice(0, 64);
}

export default function TenantCreateDialog({
                                               open,
                                               loading = false,
                                               errorText = null,
                                               onClose,
                                               onSubmit,
                                           }: Props) {
    const [name, setName] = useState("");
    const [slug, setSlug] = useState("");
    // the slug follows the name until the user edits it by hand
    const [slugTouched, setSlugTouched] = useState(false);
    const [priceTick, setPriceTick] = useState("30");
    const [demoMenu, setDemoMenu] = useState(true);

    const tick = Number(priceTick);
    const slugValid = slug.length >= 3 && SLUG_PATTERN.test(slug);
    const tickValid = Number.isInteger(tick) && tick >= 1 && tick <= 86400;
    const canSave = name.trim().length > 0 && slugValid && tickValid && !loading;

    const submit = () =>
        onSubmit({ name: name.trim(), slug, priceTickSeconds: tick, demoMenu });

    return (
        <Dialog open={open} onClose={loading ? undefined : onClose} fullWidth maxWidth="sm">
            <DialogTitle>New venue</DialogTitle>

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
                        label="Venue name"
                        value={name}
                        onChange={(e) => {
                            setName(e.target.value);
                            if (!slugTouched) setSlug(toSlug(e.target.value));
                        }}
                    />
                    <TextField
                        fullWidth
                        label="Slug"
                        value={slug}
                        error={slug.length > 0 && !slugValid}
                        helperText="Used in TV and QR links; lowercase letters, digits and dashes; cannot be changed later"
                        onChange={(e) => {
                            setSlugTouched(true);
                            setSlug(e.target.value.toLowerCase());
                        }}
                    />
                    <TextField
                        fullWidth
                        label="Price change interval, seconds"
                        type="number"
                        value={priceTick}
                        error={!tickValid}
                        onChange={(e) => setPriceTick(e.target.value)}
                    />
                    <FormControlLabel
                        control={<Checkbox checked={demoMenu} onChange={(e) => setDemoMenu(e.target.checked)} />}
                        label="Copy the demo menu (brands, products, items, recipes)"
                    />
                </Stack>
            </DialogContent>

            <DialogActions>
                <Button onClick={onClose} disabled={loading} variant="outlined">
                    Cancel
                </Button>
                <Button onClick={submit} disabled={!canSave} variant="contained">
                    Create
                </Button>
            </DialogActions>
        </Dialog>
    );
}
