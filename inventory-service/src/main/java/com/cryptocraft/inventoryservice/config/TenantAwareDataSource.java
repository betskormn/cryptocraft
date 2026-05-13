package com.cryptocraft.inventoryservice.config;

import com.cryptocraft.inventoryservice.tenant.TenantContext;
import org.jspecify.annotations.NullMarked;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

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

    private void applyTenant(Connection c) {
        Long tenantId = TenantContext.get();
        //TODO вирішити це питання
        tenantId = (tenantId != null) ? tenantId : 1L;
        try (Statement st = c.createStatement()) {
            st.execute("SET app.tenant_id = " + tenantId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to apply tenant context on connection", e);
        }
    }

}
