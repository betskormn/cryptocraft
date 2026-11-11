package com.cryptocraft.inventoryservice.config;

import com.cryptocraft.inventoryservice.tenant.TenantContext;
import org.jspecify.annotations.NullMarked;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TenantAwareDataSource extends DelegatingDataSource {

    public TenantAwareDataSource(DataSource targetDataSource) {
        super(targetDataSource);
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection c = super.getConnection();
        applyTenant(c);
        return c;
    }


    @NullMarked
    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection c = super.getConnection(username, password);
        applyTenant(c);
        return c;
    }

    /**
     * Overwrites app.tenant_id on every checkout from the pool, so a pooled connection never keeps
     * the previous request's tenant. Without a tenant the value is empty and the RLS policies
     * (app_current_tenant() = NULL) return no rows and reject writes.
     */
    private void applyTenant(Connection c) throws SQLException {
        Long tenantId = TenantContext.get();
        String value = tenantId != null ? tenantId.toString() : "";
        try (PreparedStatement st = c.prepareStatement("SELECT set_config('app.tenant_id', ?, false)")) {
            st.setString(1, value);
            st.execute();
        } catch (SQLException e) {
            c.close();
            throw e;
        }
    }

}
