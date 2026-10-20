ALTER TABLE suppliers
    ADD COLUMN nit VARCHAR(32),
    ADD COLUMN email VARCHAR(160);

ALTER TABLE suppliers
    ADD CONSTRAINT suppliers_nit_not_blank
        CHECK (nit IS NULL OR btrim(nit) <> ''),
    ADD CONSTRAINT suppliers_email_not_blank
        CHECK (email IS NULL OR btrim(email) <> '');

CREATE UNIQUE INDEX uq_suppliers_nit
    ON suppliers (lower(btrim(nit)))
    WHERE nit IS NOT NULL;

CREATE INDEX idx_suppliers_email_lower
    ON suppliers (lower(email));