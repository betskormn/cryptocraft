--liquibase formatted sql

--changeset roman:001-identity-app-role runOnChange:true splitStatements:false
--comment: the service's own role; identity data is not venue-scoped, so there is no RLS here
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${identity_db_user}') THEN
        CREATE ROLE ${identity_db_user} LOGIN PASSWORD '${identity_db_password}' NOSUPERUSER NOCREATEROLE NOCREATEDB;
    ELSE
        ALTER ROLE ${identity_db_user} LOGIN PASSWORD '${identity_db_password}' NOSUPERUSER NOCREATEROLE NOCREATEDB;
    END IF;
END $$;

--changeset roman:001-identity-tables splitStatements:true endDelimiter:;
--comment: staff accounts, their venues and roles, and the key that signs tokens

-- one person, whatever venues they work in; email is the login
CREATE TABLE users
(
    user_id        UUID PRIMARY KEY,
    email          VARCHAR(254) NOT NULL,
    display_name   VARCHAR(100) NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    enabled        BOOLEAN      NOT NULL DEFAULT TRUE,
    -- runs the platform: every venue and the platform API
    platform_admin BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP             DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_users_email ON users (lower(email));

-- role: 1 ADMIN, 2 INVENTORY_MANAGER, 3 STAFF (VenueRole)
CREATE TABLE venue_memberships
(
    user_id    UUID      NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    tenant_id  BIGINT    NOT NULL,
    role       SMALLINT  NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP          DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, tenant_id)
);
CREATE INDEX ix_venue_memberships_tenant ON venue_memberships (tenant_id);

-- RSA key pair that signs tokens; kept so tokens stay valid across restarts and instances
CREATE TABLE signing_keys
(
    key_id      VARCHAR(64) PRIMARY KEY,
    public_key  TEXT        NOT NULL,
    private_key TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

--changeset roman:001-identity-oauth2-tables splitStatements:true endDelimiter:;
--comment: Spring Authorization Server storage (its schema, blob -> text, timestamp -> timestamptz for PostgreSQL)
CREATE TABLE oauth2_registered_client
(
    id                            VARCHAR(100)                            NOT NULL,
    client_id                     VARCHAR(100)                            NOT NULL,
    client_id_issued_at           TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    client_secret                 VARCHAR(200)  DEFAULT NULL,
    client_secret_expires_at      TIMESTAMPTZ   DEFAULT NULL,
    client_name                   VARCHAR(200)                            NOT NULL,
    client_authentication_methods VARCHAR(1000)                           NOT NULL,
    authorization_grant_types     VARCHAR(1000)                           NOT NULL,
    redirect_uris                 VARCHAR(1000) DEFAULT NULL,
    post_logout_redirect_uris     VARCHAR(1000) DEFAULT NULL,
    scopes                        VARCHAR(1000)                           NOT NULL,
    client_settings               VARCHAR(2000)                           NOT NULL,
    token_settings                VARCHAR(2000)                           NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE oauth2_authorization
(
    id                            VARCHAR(100) NOT NULL,
    registered_client_id          VARCHAR(100) NOT NULL,
    principal_name                VARCHAR(200) NOT NULL,
    authorization_grant_type      VARCHAR(100) NOT NULL,
    authorized_scopes             VARCHAR(1000) DEFAULT NULL,
    attributes                    TEXT          DEFAULT NULL,
    state                         VARCHAR(500)  DEFAULT NULL,
    authorization_code_value      TEXT          DEFAULT NULL,
    authorization_code_issued_at  TIMESTAMPTZ   DEFAULT NULL,
    authorization_code_expires_at TIMESTAMPTZ   DEFAULT NULL,
    authorization_code_metadata   TEXT          DEFAULT NULL,
    access_token_value            TEXT          DEFAULT NULL,
    access_token_issued_at        TIMESTAMPTZ   DEFAULT NULL,
    access_token_expires_at       TIMESTAMPTZ   DEFAULT NULL,
    access_token_metadata         TEXT          DEFAULT NULL,
    access_token_type             VARCHAR(100)  DEFAULT NULL,
    access_token_scopes           VARCHAR(1000) DEFAULT NULL,
    oidc_id_token_value           TEXT          DEFAULT NULL,
    oidc_id_token_issued_at       TIMESTAMPTZ   DEFAULT NULL,
    oidc_id_token_expires_at      TIMESTAMPTZ   DEFAULT NULL,
    oidc_id_token_metadata        TEXT          DEFAULT NULL,
    refresh_token_value           TEXT          DEFAULT NULL,
    refresh_token_issued_at       TIMESTAMPTZ   DEFAULT NULL,
    refresh_token_expires_at      TIMESTAMPTZ   DEFAULT NULL,
    refresh_token_metadata        TEXT          DEFAULT NULL,
    user_code_value               TEXT          DEFAULT NULL,
    user_code_issued_at           TIMESTAMPTZ   DEFAULT NULL,
    user_code_expires_at          TIMESTAMPTZ   DEFAULT NULL,
    user_code_metadata            TEXT          DEFAULT NULL,
    device_code_value             TEXT          DEFAULT NULL,
    device_code_issued_at         TIMESTAMPTZ   DEFAULT NULL,
    device_code_expires_at        TIMESTAMPTZ   DEFAULT NULL,
    device_code_metadata          TEXT          DEFAULT NULL,
    PRIMARY KEY (id)
);
-- expired authorizations are deleted by AuthorizationCleanup
CREATE INDEX ix_oauth2_authorization_expiry ON oauth2_authorization (refresh_token_expires_at, access_token_expires_at);

CREATE TABLE oauth2_authorization_consent
(
    registered_client_id VARCHAR(100)  NOT NULL,
    principal_name       VARCHAR(200)  NOT NULL,
    authorities          VARCHAR(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
);

--changeset roman:001-identity-grants splitStatements:true endDelimiter:;
GRANT USAGE ON SCHEMA public TO ${identity_db_user};
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO ${identity_db_user};
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO ${identity_db_user};
