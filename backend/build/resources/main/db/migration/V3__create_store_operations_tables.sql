CREATE OR REPLACE FUNCTION touch_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE measurement_units (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(16) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL UNIQUE,
    allows_decimal BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT measurement_units_code_not_blank CHECK (btrim(code) <> ''),
    CONSTRAINT measurement_units_name_not_blank CHECK (btrim(name) <> '')
);

CREATE TRIGGER trg_measurement_units_updated_at
    BEFORE UPDATE ON measurement_units
    FOR EACH ROW
    EXECUTE FUNCTION touch_updated_at();

CREATE TABLE product_categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_id BIGINT REFERENCES app_users (id) ON DELETE SET NULL,
    updated_by_id BIGINT REFERENCES app_users (id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT product_categories_name_not_blank CHECK (btrim(name) <> '')
);

CREATE UNIQUE INDEX ux_product_categories_name_lower
    ON product_categories (lower(name));

CREATE INDEX idx_product_categories_active
    ON product_categories (active);

CREATE TRIGGER trg_product_categories_updated_at
    BEFORE UPDATE ON product_categories
    FOR EACH ROW
    EXECUTE FUNCTION touch_updated_at();

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(500),
    image_url VARCHAR(500),
    category_id BIGINT NOT NULL REFERENCES product_categories (id),
    unit_id BIGINT NOT NULL REFERENCES measurement_units (id),
    purchase_price NUMERIC(14, 4) NOT NULL DEFAULT 0,
    sale_price NUMERIC(14, 2) NOT NULL,
    current_stock NUMERIC(14, 3) NOT NULL DEFAULT 0,
    minimum_stock NUMERIC(14, 3) NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_id BIGINT REFERENCES app_users (id) ON DELETE SET NULL,
    updated_by_id BIGINT REFERENCES app_users (id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT products_code_not_blank CHECK (btrim(code) <> ''),
    CONSTRAINT products_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT products_purchase_price_nonnegative CHECK (purchase_price >= 0),
    CONSTRAINT products_sale_price_nonnegative CHECK (sale_price >= 0),
    CONSTRAINT products_current_stock_nonnegative CHECK (current_stock >= 0),
    CONSTRAINT products_minimum_stock_nonnegative CHECK (minimum_stock >= 0)
);

CREATE UNIQUE INDEX ux_products_active_code_lower
    ON products (lower(code))
    WHERE active;

CREATE INDEX idx_products_name_lower
    ON products (lower(name));

CREATE INDEX idx_products_category
    ON products (category_id);

CREATE INDEX idx_products_stock_status
    ON products (active, current_stock, minimum_stock);

CREATE TRIGGER trg_products_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION touch_updated_at();

CREATE TABLE suppliers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    phone VARCHAR(32),
    address VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_id BIGINT REFERENCES app_users (id) ON DELETE SET NULL,
    updated_by_id BIGINT REFERENCES app_users (id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT suppliers_name_not_blank CHECK (btrim(name) <> '')
);

CREATE INDEX idx_suppliers_name_lower
    ON suppliers (lower(name));

CREATE INDEX idx_suppliers_active
    ON suppliers (active);

CREATE TRIGGER trg_suppliers_updated_at
    BEFORE UPDATE ON suppliers
    FOR EACH ROW
    EXECUTE FUNCTION touch_updated_at();

CREATE TABLE stock_entries (
    id BIGSERIAL PRIMARY KEY,
    supplier_id BIGINT REFERENCES suppliers (id) ON DELETE SET NULL,
    entry_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    document_number VARCHAR(80),
    notes VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    created_by_id BIGINT NOT NULL REFERENCES app_users (id) ON DELETE RESTRICT,
    confirmed_by_id BIGINT REFERENCES app_users (id) ON DELETE SET NULL,
    confirmed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT stock_entries_status_valid CHECK (status IN ('DRAFT', 'CONFIRMED', 'CANCELLED')),
    CONSTRAINT stock_entries_document_number_not_blank CHECK (document_number IS NULL OR btrim(document_number) <> '')
);

CREATE INDEX idx_stock_entries_supplier
    ON stock_entries (supplier_id);

CREATE INDEX idx_stock_entries_entry_date
    ON stock_entries (entry_date DESC);

CREATE INDEX idx_stock_entries_created_by
    ON stock_entries (created_by_id);

CREATE TRIGGER trg_stock_entries_updated_at
    BEFORE UPDATE ON stock_entries
    FOR EACH ROW
    EXECUTE FUNCTION touch_updated_at();

CREATE TABLE stock_entry_items (
    id BIGSERIAL PRIMARY KEY,
    stock_entry_id BIGINT NOT NULL REFERENCES stock_entries (id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products (id) ON DELETE RESTRICT,
    quantity NUMERIC(14, 3) NOT NULL,
    unit_cost NUMERIC(14, 4) NOT NULL,
    line_total NUMERIC(14, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT stock_entry_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT stock_entry_items_unit_cost_nonnegative CHECK (unit_cost >= 0),
    CONSTRAINT stock_entry_items_line_total_nonnegative CHECK (line_total >= 0),
    CONSTRAINT stock_entry_items_product_unique UNIQUE (stock_entry_id, product_id)
);

CREATE INDEX idx_stock_entry_items_product
    ON stock_entry_items (product_id);

CREATE TABLE sales (
    id BIGSERIAL PRIMARY KEY,
    sale_number VARCHAR(40) NOT NULL UNIQUE,
    sold_by_id BIGINT NOT NULL REFERENCES app_users (id) ON DELETE RESTRICT,
    sale_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    subtotal_amount NUMERIC(14, 2) NOT NULL,
    total_amount NUMERIC(14, 2) NOT NULL,
    cash_received NUMERIC(14, 2) NOT NULL,
    change_amount NUMERIC(14, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    notes VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT sales_sale_number_not_blank CHECK (btrim(sale_number) <> ''),
    CONSTRAINT sales_subtotal_nonnegative CHECK (subtotal_amount >= 0),
    CONSTRAINT sales_total_nonnegative CHECK (total_amount >= 0),
    CONSTRAINT sales_cash_received_valid CHECK (cash_received >= total_amount),
    CONSTRAINT sales_change_nonnegative CHECK (change_amount >= 0),
    CONSTRAINT sales_change_valid CHECK (change_amount = cash_received - total_amount),
    CONSTRAINT sales_status_valid CHECK (status IN ('COMPLETED', 'CANCELLED'))
);

CREATE INDEX idx_sales_sale_date
    ON sales (sale_date DESC);

CREATE INDEX idx_sales_sold_by
    ON sales (sold_by_id);

CREATE TABLE sale_items (
    id BIGSERIAL PRIMARY KEY,
    sale_id BIGINT NOT NULL REFERENCES sales (id) ON DELETE RESTRICT,
    product_id BIGINT NOT NULL REFERENCES products (id) ON DELETE RESTRICT,
    quantity NUMERIC(14, 3) NOT NULL,
    unit_price NUMERIC(14, 2) NOT NULL,
    unit_cost_snapshot NUMERIC(14, 4) NOT NULL DEFAULT 0,
    line_total NUMERIC(14, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT sale_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT sale_items_unit_price_nonnegative CHECK (unit_price >= 0),
    CONSTRAINT sale_items_unit_cost_snapshot_nonnegative CHECK (unit_cost_snapshot >= 0),
    CONSTRAINT sale_items_line_total_nonnegative CHECK (line_total >= 0),
    CONSTRAINT sale_items_product_unique UNIQUE (sale_id, product_id)
);

CREATE INDEX idx_sale_items_product
    ON sale_items (product_id);

CREATE TABLE inventory_adjustments (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products (id) ON DELETE RESTRICT,
    adjusted_by_id BIGINT NOT NULL REFERENCES app_users (id) ON DELETE RESTRICT,
    adjustment_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    quantity_before NUMERIC(14, 3) NOT NULL,
    quantity_after NUMERIC(14, 3) NOT NULL,
    quantity_delta NUMERIC(14, 3) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT inventory_adjustments_quantity_before_nonnegative CHECK (quantity_before >= 0),
    CONSTRAINT inventory_adjustments_quantity_after_nonnegative CHECK (quantity_after >= 0),
    CONSTRAINT inventory_adjustments_delta_not_zero CHECK (quantity_delta <> 0),
    CONSTRAINT inventory_adjustments_delta_valid CHECK (quantity_delta = quantity_after - quantity_before),
    CONSTRAINT inventory_adjustments_reason_not_blank CHECK (btrim(reason) <> '')
);

CREATE INDEX idx_inventory_adjustments_product_date
    ON inventory_adjustments (product_id, adjustment_date DESC);

CREATE INDEX idx_inventory_adjustments_adjusted_by
    ON inventory_adjustments (adjusted_by_id);

CREATE TABLE inventory_movements (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products (id) ON DELETE RESTRICT,
    movement_type VARCHAR(32) NOT NULL,
    quantity_delta NUMERIC(14, 3) NOT NULL,
    quantity_before NUMERIC(14, 3) NOT NULL,
    quantity_after NUMERIC(14, 3) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_id BIGINT,
    reason VARCHAR(500),
    created_by_id BIGINT NOT NULL REFERENCES app_users (id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT inventory_movements_type_valid CHECK (movement_type IN ('STOCK_ENTRY', 'SALE', 'ADJUSTMENT')),
    CONSTRAINT inventory_movements_source_type_valid CHECK (source_type IN ('STOCK_ENTRY', 'SALE', 'ADJUSTMENT')),
    CONSTRAINT inventory_movements_delta_not_zero CHECK (quantity_delta <> 0),
    CONSTRAINT inventory_movements_quantity_before_nonnegative CHECK (quantity_before >= 0),
    CONSTRAINT inventory_movements_quantity_after_nonnegative CHECK (quantity_after >= 0),
    CONSTRAINT inventory_movements_quantity_after_valid CHECK (quantity_after = quantity_before + quantity_delta)
);

CREATE INDEX idx_inventory_movements_product_created
    ON inventory_movements (product_id, created_at DESC);

CREATE INDEX idx_inventory_movements_source
    ON inventory_movements (source_type, source_id);

CREATE INDEX idx_inventory_movements_created_by
    ON inventory_movements (created_by_id);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    notification_type VARCHAR(32) NOT NULL,
    title VARCHAR(160) NOT NULL,
    message VARCHAR(500) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_by_id BIGINT REFERENCES app_users (id) ON DELETE SET NULL,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT notifications_type_valid CHECK (notification_type IN ('LOW_STOCK', 'OUT_OF_STOCK')),
    CONSTRAINT notifications_title_not_blank CHECK (btrim(title) <> ''),
    CONSTRAINT notifications_message_not_blank CHECK (btrim(message) <> ''),
    CONSTRAINT notifications_read_consistent CHECK ((is_read = FALSE AND read_at IS NULL) OR (is_read = TRUE AND read_at IS NOT NULL))
);

CREATE UNIQUE INDEX ux_notifications_unread_product_type
    ON notifications (product_id, notification_type)
    WHERE is_read = FALSE;

CREATE INDEX idx_notifications_created
    ON notifications (created_at DESC);

CREATE TABLE audit_events (
    id BIGSERIAL PRIMARY KEY,
    actor_user_id BIGINT REFERENCES app_users (id) ON DELETE SET NULL,
    action VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id VARCHAR(64),
    summary VARCHAR(255),
    before_data JSONB,
    after_data JSONB,
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT audit_events_action_not_blank CHECK (btrim(action) <> ''),
    CONSTRAINT audit_events_entity_type_not_blank CHECK (btrim(entity_type) <> '')
);

CREATE INDEX idx_audit_events_actor_created
    ON audit_events (actor_user_id, created_at DESC);

CREATE INDEX idx_audit_events_entity
    ON audit_events (entity_type, entity_id);

CREATE TABLE ai_agent_runs (
    id BIGSERIAL PRIMARY KEY,
    agent_name VARCHAR(80) NOT NULL,
    skill_name VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    input_summary JSONB NOT NULL DEFAULT '{}'::jsonb,
    output_summary JSONB,
    requested_by_id BIGINT REFERENCES app_users (id) ON DELETE SET NULL,
    error_message VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    CONSTRAINT ai_agent_runs_agent_name_not_blank CHECK (btrim(agent_name) <> ''),
    CONSTRAINT ai_agent_runs_skill_name_not_blank CHECK (btrim(skill_name) <> ''),
    CONSTRAINT ai_agent_runs_status_valid CHECK (status IN ('REQUESTED', 'RUNNING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_ai_agent_runs_created
    ON ai_agent_runs (created_at DESC);

CREATE TABLE ai_recommendations (
    id BIGSERIAL PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES ai_agent_runs (id) ON DELETE CASCADE,
    product_id BIGINT REFERENCES products (id) ON DELETE SET NULL,
    recommendation_type VARCHAR(40) NOT NULL,
    title VARCHAR(160) NOT NULL,
    detail TEXT NOT NULL,
    suggested_quantity NUMERIC(14, 3),
    priority VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
    decision_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    decided_by_id BIGINT REFERENCES app_users (id) ON DELETE SET NULL,
    decided_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ai_recommendations_type_valid CHECK (recommendation_type IN ('RESTOCK', 'PRICE_REVIEW', 'SLOW_MOVING', 'LOW_STOCK_RISK')),
    CONSTRAINT ai_recommendations_title_not_blank CHECK (btrim(title) <> ''),
    CONSTRAINT ai_recommendations_detail_not_blank CHECK (btrim(detail) <> ''),
    CONSTRAINT ai_recommendations_suggested_quantity_positive CHECK (suggested_quantity IS NULL OR suggested_quantity > 0),
    CONSTRAINT ai_recommendations_priority_valid CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT ai_recommendations_decision_valid CHECK (decision_status IN ('PENDING', 'ACCEPTED', 'DISMISSED')),
    CONSTRAINT ai_recommendations_decision_consistent CHECK (
        (decision_status = 'PENDING' AND decided_at IS NULL)
        OR (decision_status <> 'PENDING' AND decided_at IS NOT NULL)
    )
);

CREATE INDEX idx_ai_recommendations_product
    ON ai_recommendations (product_id);

CREATE INDEX idx_ai_recommendations_status_priority
    ON ai_recommendations (decision_status, priority);
