--liquibase formatted sql

--changeset roman:008-widen-text-columns splitStatements:true endDelimiter:;
--comment: match the request DTO limits (recipe 2000, image URL 500); VARCHAR(255) turned longer values into 500 errors
ALTER TABLE items ALTER COLUMN recipe TYPE TEXT;
ALTER TABLE items ALTER COLUMN image TYPE VARCHAR(500);
ALTER TABLE products ALTER COLUMN image TYPE VARCHAR(500);
