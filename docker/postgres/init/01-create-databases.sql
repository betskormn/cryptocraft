-- Runs once, when the postgres volume is created. One database per service that owns data:
-- `postgres` (default) for inventory-service, `orders` for order-service.
-- For a volume created before this script: docker exec postgres psql -U postgres -c "CREATE DATABASE orders"
CREATE DATABASE orders;
