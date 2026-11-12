import { Routes, Route } from "react-router-dom";

import BrandListPage from "./pages/brands/BrandListPage";
import ProductListPage from "./pages/products/ProductListPage";
import ItemListPage from "./pages/items/ItemListPage";
import TableListPage from "./pages/tables/TableListPage";
import VisitListPage from "./pages/visits/VisitListPage";
import KitchenDisplayPage from "./pages/kds/KitchenDisplayPage";
import StaffListPage from "./pages/staff/StaffListPage";
import GuestPage from "./pages/guest/GuestPage";
import AdminLayout from "./components/AdminLayout";
import AuthProvider from "./auth/AuthProvider";
import { HomeRedirect, RequireArea } from "./auth/RequireArea";

// The guest page of a table's QR code needs no staff sign-in. Everything else is behind AuthProvider;
// AdminLayout owns the app bar, navigation drawer and page container; kitchen/bar screens are full-screen.
// Each staff page is shown only to the roles that may use it.
export default function App() {
    return (
        <Routes>
            <Route path="/g/:tenantId/:code" element={<GuestPage />} />
            <Route path="*" element={<StaffApp />} />
        </Routes>
    );
}

function StaffApp() {
    return (
        <AuthProvider>
            <Routes>
                <Route path="/kds/kitchen" element={<RequireArea area="kitchen"><KitchenDisplayPage key="MEAL" station="MEAL" /></RequireArea>} />
                <Route path="/kds/bar" element={<RequireArea area="kitchen"><KitchenDisplayPage key="DRINK" station="DRINK" /></RequireArea>} />
                <Route element={<AdminLayout />}>
                    <Route path="/" element={<HomeRedirect />} />
                    <Route path="/brands" element={<RequireArea area="menu"><BrandListPage /></RequireArea>} />
                    <Route path="/products" element={<RequireArea area="menu"><ProductListPage /></RequireArea>} />
                    <Route path="/items" element={<RequireArea area="menu"><ItemListPage /></RequireArea>} />
                    <Route path="/tables" element={<RequireArea area="tables"><TableListPage /></RequireArea>} />
                    <Route path="/visits" element={<RequireArea area="visits"><VisitListPage /></RequireArea>} />
                    <Route path="/staff" element={<RequireArea area="staff"><StaffListPage /></RequireArea>} />
                    <Route path="*" element={<div>NOT FOUND</div>} />
                </Route>
            </Routes>
        </AuthProvider>
    );
}
