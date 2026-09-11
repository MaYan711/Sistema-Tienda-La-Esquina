INSERT INTO measurement_units (code, name, allows_decimal)
VALUES
    ('UND', 'Unidad', FALSE),
    ('PAQ', 'Paquete', FALSE),
    ('CAJA', 'Caja', FALSE),
    ('DOC', 'Docena', FALSE),
    ('LB', 'Libra', TRUE),
    ('KG', 'Kilogramo', TRUE),
    ('G', 'Gramo', TRUE),
    ('L', 'Litro', TRUE),
    ('ML', 'Mililitro', TRUE)
ON CONFLICT (code) DO NOTHING;

WITH seed_categories (name, description) AS (
    VALUES
        ('Bebidas', 'Gaseosas, jugos, agua pura y bebidas de consumo diario'),
        ('Refrigerados', 'Leche, huevos, queso, crema, yogurt y productos refrigerados'),
        ('Alimentos', 'Categoria general para productos comestibles'),
        ('Abarrotes', 'Productos empacados y articulos basicos de consumo diario'),
        ('Granos basicos', 'Arroz, frijol y otros granos de consumo diario'),
        ('Snacks y dulces', 'Boquitas, galletas, chocolates y golosinas'),
        ('Limpieza', 'Productos para limpieza del hogar y mantenimiento general'),
        ('Higiene personal', 'Articulos de cuidado personal y uso diario'),
        ('Articulos para mascotas', 'Alimentos y accesorios basicos para mascotas'),
        ('Cigarrillos', 'Productos de tabaco controlados por la tienda'),
        ('Recargas telefonicas', 'Servicios y recargas de telefonia')
)
INSERT INTO product_categories (name, description)
SELECT name, description
FROM seed_categories
WHERE NOT EXISTS (
    SELECT 1
    FROM product_categories
    WHERE lower(product_categories.name) = lower(seed_categories.name)
);

WITH seed_suppliers (name, phone, address) AS (
    VALUES
        ('Distribuidora La Terminal', '7761-1200', 'Zona 3, Quetzaltenango'),
        ('Abarrotes El Centro', '7765-3344', 'Zona 1, Quetzaltenango'),
        ('Distribuidora Quetzalteca', '7771-8899', 'Salida a San Marcos, Quetzaltenango'),
        ('Proveedor local de barrio', '5555-0101', 'Zona 9, Quetzaltenango')
)
INSERT INTO suppliers (name, phone, address)
SELECT name, phone, address
FROM seed_suppliers
WHERE NOT EXISTS (
    SELECT 1
    FROM suppliers
    WHERE lower(suppliers.name) = lower(seed_suppliers.name)
);
