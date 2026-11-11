import { NavLink, Outlet } from "react-router-dom";
import {
    AppBar,
    Box,
    Container,
    Drawer,
    List,
    ListItemButton,
    ListItemIcon,
    ListItemText,
    Toolbar,
    Typography,
} from "@mui/material";

import StorefrontOutlinedIcon from "@mui/icons-material/StorefrontOutlined";
import Inventory2OutlinedIcon from "@mui/icons-material/Inventory2Outlined";
import CategoryOutlinedIcon from "@mui/icons-material/CategoryOutlined";

import TenantSwitcher from "./TenantSwitcher";

const drawerWidth = 240;

export default function AdminLayout() {
    return (
        <Box sx={{ display: "flex", minHeight: "100vh" }}>
            <AppBar position="fixed" elevation={0} sx={{ zIndex: (t) => t.zIndex.drawer + 1 }}>
                <Toolbar>
                    <Typography variant="h6" sx={{ fontWeight: 850 }}>
                        CryptoCraft Admin{" "}
                        <Box component="span" sx={{ color: "primary.main" }}>
                            •
                        </Box>{" "}
                        Inventory
                    </Typography>
                    <TenantSwitcher />
                </Toolbar>
            </AppBar>

            <Drawer
                variant="permanent"
                sx={{
                    width: drawerWidth,
                    flexShrink: 0,
                    [`& .MuiDrawer-paper`]: {
                        width: drawerWidth,
                        boxSizing: "border-box",
                        borderRight: "1px solid rgba(255,255,255,0.10)",
                        backgroundImage: "none",
                        backgroundColor: "rgba(34,40,49,0.55)",
                        backdropFilter: "blur(10px)",
                    },
                }}
            >
                <Toolbar />

                <Box sx={{ px: 1.2, py: 1 }}>
                    <List sx={{ display: "grid", gap: 0.5 }}>
                        <ListItemButton
                            component={NavLink}
                            to="/brands"
                            // NavLink adds the "active" class styled below
                            sx={{
                                borderRadius: 2,
                                "&.active": {
                                    backgroundColor: "rgba(242,162,58,0.12)",
                                    border: "1px solid rgba(242,162,58,0.22)",
                                },
                            }}
                        >
                            <ListItemIcon>
                                <StorefrontOutlinedIcon />
                            </ListItemIcon>
                            <ListItemText primary="Brands" />
                        </ListItemButton>

                        <ListItemButton
                            component={NavLink}
                            to="/products"
                            sx={{
                                borderRadius: 2,
                                "&.active": {
                                    backgroundColor: "rgba(242,162,58,0.12)",
                                    border: "1px solid rgba(242,162,58,0.22)",
                                },
                            }}
                        >
                            <ListItemIcon>
                                <Inventory2OutlinedIcon />
                            </ListItemIcon>
                            <ListItemText primary="Products" />
                        </ListItemButton>

                        <ListItemButton
                            component={NavLink}
                            to="/items"
                            sx={{
                                borderRadius: 2,
                                "&.active": {
                                    backgroundColor: "rgba(242,162,58,0.12)",
                                    border: "1px solid rgba(242,162,58,0.22)",
                                },
                            }}
                        >
                            <ListItemIcon>
                                <CategoryOutlinedIcon />
                            </ListItemIcon>
                            <ListItemText primary="Items" />
                        </ListItemButton>
                    </List>
                </Box>
            </Drawer>

            <Box component="main" sx={{ flexGrow: 1 }}>
                <Toolbar />
                <Container sx={{ py: 3 }}>
                    <Outlet />
                </Container>
            </Box>
        </Box>
    );
}