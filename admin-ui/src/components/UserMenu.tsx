import { useState } from "react";
import { Box, Button, Divider, ListItemIcon, Menu, MenuItem, Snackbar, Alert, Typography } from "@mui/material";
import AccountCircleOutlinedIcon from "@mui/icons-material/AccountCircleOutlined";
import KeyOutlinedIcon from "@mui/icons-material/KeyOutlined";
import LogoutIcon from "@mui/icons-material/Logout";

import { ROLE_LABEL } from "../api/auth.api";
import { useAuth } from "../auth/AuthContext";
import { signOut } from "../auth/session";
import ChangePasswordDialog from "./ChangePasswordDialog";

// who is signed in, their role in the current venue, password and sign-out
export default function UserMenu() {
    const { me, role } = useAuth();
    const [anchor, setAnchor] = useState<HTMLElement | null>(null);
    const [passwordOpen, setPasswordOpen] = useState(false);
    const [changed, setChanged] = useState(false);

    const roleLabel = me.platformAdmin ? "Platform admin" : role ? ROLE_LABEL[role] : "No access to this venue";

    return (
        <>
            <Button
                color="inherit"
                onClick={(e) => setAnchor(e.currentTarget)}
                startIcon={<AccountCircleOutlinedIcon />}
                sx={{ ml: 1, minWidth: 0 }}
            >
                <Box component="span" sx={{ display: { xs: "none", md: "inline" }, maxWidth: 160, overflow: "hidden", textOverflow: "ellipsis" }}>
                    {me.displayName}
                </Box>
            </Button>
            <Menu anchorEl={anchor} open={!!anchor} onClose={() => setAnchor(null)}>
                <Box sx={{ px: 2, py: 1, maxWidth: 280 }}>
                    <Typography fontWeight={750} sx={{ overflowWrap: "anywhere" }}>
                        {me.displayName}
                    </Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ overflowWrap: "anywhere" }}>
                        {me.email}
                    </Typography>
                    <Typography variant="caption" color="primary.main">
                        {roleLabel}
                    </Typography>
                </Box>
                <Divider />
                <MenuItem
                    onClick={() => {
                        setAnchor(null);
                        setPasswordOpen(true);
                    }}
                >
                    <ListItemIcon>
                        <KeyOutlinedIcon fontSize="small" />
                    </ListItemIcon>
                    Change password
                </MenuItem>
                <MenuItem onClick={signOut}>
                    <ListItemIcon>
                        <LogoutIcon fontSize="small" />
                    </ListItemIcon>
                    Sign out
                </MenuItem>
            </Menu>

            {passwordOpen ? (
                <ChangePasswordDialog
                    open
                    onClose={(done) => {
                        setPasswordOpen(false);
                        setChanged(done);
                    }}
                />
            ) : null}
            <Snackbar open={changed} autoHideDuration={4000} onClose={() => setChanged(false)}>
                <Alert severity="success" variant="filled" onClose={() => setChanged(false)}>
                    Password changed
                </Alert>
            </Snackbar>
        </>
    );
}
