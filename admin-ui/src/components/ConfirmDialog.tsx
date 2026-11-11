import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
} from "@mui/material";

type Props = {
  open: boolean;
  title: string;
  description?: string;
  confirmText?: string;
  cancelText?: string;
  loading?: boolean;
  // e.g. the server refusing to delete something still in use
  errorText?: string | null;
  onCancel: () => void;
  onConfirm: () => void;
};

export default function ConfirmDialog({
  open,
  title,
  description = "This action cannot be undone.",
  confirmText = "Delete",
  cancelText = "Cancel",
  loading = false,
  errorText = null,
  onCancel,
  onConfirm,
}: Props) {
  return (
    <Dialog open={open} onClose={loading ? undefined : onCancel}>
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>
        {errorText ? (
          <Alert severity="error" sx={{ mb: 2 }}>
            {errorText}
          </Alert>
        ) : null}
        <DialogContentText>{description}</DialogContentText>
      </DialogContent>
      <DialogActions>
        <Button onClick={onCancel} disabled={loading} variant="outlined">
          {cancelText}
        </Button>
        <Button onClick={onConfirm} disabled={loading} color="error" variant="contained">
          {confirmText}
        </Button>
      </DialogActions>
    </Dialog>
  );
}