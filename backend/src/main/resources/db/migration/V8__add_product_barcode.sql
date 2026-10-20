ALTER TABLE products
    ADD COLUMN barcode VARCHAR(128);

CREATE UNIQUE INDEX ux_products_barcode
    ON products (barcode)
    WHERE barcode IS NOT NULL AND btrim(barcode) <> '';

ALTER TABLE products
    ADD CONSTRAINT products_barcode_not_blank
        CHECK (barcode IS NULL OR btrim(barcode) <> '');