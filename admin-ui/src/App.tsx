import { Routes, Route, Navigate } from "react-router-dom";

import BrandListPage from "./pages/brands/BrandListPage";
import ProductListPage from "./pages/products/ProductListPage";
import ItemListPage from "./pages/items/ItemListPage";
import TableListPage from "./pages/tables/TableListPage";
import VisitListPage from "./pages/visits/VisitListPage";
import AdminLayout from "./components/AdminLayout";

// AdminLayout owns the app bar, navigation drawer and page container
export default function App() {
    return (
        <Routes>
            <Route element={<AdminLayout />}>
                <Route path="/" element={<Navigate to="/brands" replace />} />
                <Route path="/brands" element={<BrandListPage />} />
                <Route path="/products" element={<ProductListPage />} />
                <Route path="/items" element={<ItemListPage />} />
                <Route path="/tables" element={<TableListPage />} />
                <Route path="/visits" element={<VisitListPage />} />
                <Route path="*" element={<div>NOT FOUND</div>} />
            </Route>
        </Routes>
    );
}
