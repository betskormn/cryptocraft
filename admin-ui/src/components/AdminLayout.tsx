import { NavLink, Outlet } from "react-router-dom";
import {
    Alert,
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
import TableRestaurantOutlinedIcon from "@mui/icons-material/TableRestaurantOutlined";
import ReceiptLongOutlinedIcon from "@mui/icons-material/ReceiptLongOutlined";
import SoupKitchenOutlinedIcon from "@mui/icons-material/SoupKitchenOutlined";
import LocalBarOutlinedIcon from "@mui/icons-material/LocalBarOutlined";
import BadgeOutlinedIcon from "@mui/icons-material/BadgeOutlined";

import { useAuth } from "../auth/AuthContext";
import type { Area } from "../auth/access";
import TenantSwitcher from "./TenantSwitcher";
import UserMenu from "./UserMenu";

const drawerWidth = 240;

// each page shows only to the roles that may use it (auth/access.ts)
const NAV_ITEMS: Array<{ to: string; label: string; Icon: typeof StorefrontOutlinedIcon; area: Area }> = [
    { to: "/brands", label: "Brands", Icon: StorefrontOutlinedIcon, area: "menu" },
    { to: "/products", label: "Products", Icon: Inventory2OutlinedIcon, area: "menu" },
    { to: "/items", label: "Items", Icon: CategoryOutlinedIcon, area: "menu" },
    { to: "/tables", label: "Tables", Icon: TableRestaurantOutlinedIcon, area: "tables" },
    { to: "/visits", label: "Visits", Icon: ReceiptLongOutlinedIcon, area: "visits" },
    // full-screen kitchen/bar displays (KDS)
    { to: "/kds/kitchen", label: "Kitchen screen", Icon: SoupKitchenOutlinedIcon, area: "kitchen" },
    { to: "/kds/bar", label: "Bar screen", Icon: LocalBarOutlinedIcon, area: "kitchen" },
    { to: "/staff", label: "Staff", Icon: BadgeOutlinedIcon, area: "staff" },
];

export default function AdminLayout() {
    const { venue, can } = useAuth();

    return (
        <Box sx={{ display: "flex", minHeight: "100vh" }}>
            <AppBar position="fixed" elevation={0} sx={{ zIndex: (t) => t.zIndex.drawer + 1 }}>
                <Toolbar>
                    <Typography variant="h6" sx={{ fontWeight: 850 }}>
                        CryptoCraft Admin{" "}
                        <Box component="span" sx={{ color: "primary.main" }}>
                            •
                        </Box>{" "}
                        Venue
                    </Typography>
                    <TenantSwitcher />
                    <UserMenu />
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
                        {NAV_ITEMS.filter((item) => can(item.area)).map(({ to, label, Icon }) => (
                            <ListItemButton
                                key={to}
                                component={NavLink}
                                to={to}
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
                                    <Icon />
                                </ListItemIcon>
                                <ListItemText primary={label} />
                            </ListItemButton>
                        ))}
                    </List>
                </Box>
            </Drawer>

            <Box component="main" sx={{ flexGrow: 1 }}>
                <Toolbar />
                <Container sx={{ py: 3 }}>
                    {venue ? (
                        <Outlet />
                    ) : (
                        <Alert severity="info">
                            You have no venues yet. Ask the venue's admin to add you on their Staff page.
                        </Alert>
                    )}
                </Container>
            </Box>
        </Box>
    );
}