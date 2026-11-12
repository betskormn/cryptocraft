package com.cryptocraft.security;

/**
 * Claims identity-service puts into access tokens, and the scope of service-to-service tokens.
 */
public final class CryptoCraftClaims {

    // the person's venues and the role in each: {"1": "ADMIN", "7": "STAFF"}
    public static final String VENUES = "venues";
    // runs the platform: every venue, the platform API
    public static final String PLATFORM_ADMIN = "platform_admin";
    public static final String NAME = "name";
    public static final String EMAIL = "email";

    // guest tokens (issued by identity-service at Order's request when a guest joins a table by QR code):
    // "guest": true, the venue and the guest of the visit; they never carry venues or platform_admin
    public static final String GUEST = "guest";
    public static final String TENANT_ID = "tenant_id";
    public static final String PARTICIPANT_ID = "participant_id";
    public static final String TABLE_SESSION_ID = "table_session_id";

    // client_credentials tokens of our own services (Order calling Inventory)
    public static final String SCOPE_INTERNAL = "internal";

    private CryptoCraftClaims() {
    }
}
