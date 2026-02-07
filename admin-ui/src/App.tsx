import { Routes, Route, Navigate } from "react-router-dom";
import { AppBar, Box, Container, Toolbar, Typography } from "@mui/material";

import BrandListPage from "./pages/brands/BrandListPage";
import ProductListPage from "./pages/products/ProductListPage";
import ItemListPage from "./pages/items/ItemListPage";
import AdminLayout from "./components/AdminLayout";

export default function App() {
    return (
        <Box minHeight="100vh">
            <AppBar position="sticky" elevation={1}>
                <Toolbar>
                    <Typography variant="h6" sx={{ fontWeight: 800, letterSpacing: 0.4 }}>
                        CryptoCraft Admin <Box component="span" sx={{ color: "primary.main" }}>•</Box>
                    </Typography>
                </Toolbar>
            </AppBar>

            <Container sx={{ py: 3 }}>
                <Routes>
                    <Route element={<AdminLayout />}>
                        <Route path="/" element={<Navigate to="/brands" replace />} />
                        <Route path="/brands" element={<BrandListPage />} />
                        <Route path="/products" element={<ProductListPage />} />
                        <Route path="/items" element={<ItemListPage />} />
                        <Route path="*" element={<div>NOT FOUND</div>} />
                    </Route>
                </Routes>

            </Container>
        </Box>
    );
}