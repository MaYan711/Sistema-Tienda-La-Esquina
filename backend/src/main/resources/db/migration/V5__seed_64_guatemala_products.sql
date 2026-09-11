CREATE TEMP TABLE seed_demo_products (
    code VARCHAR(64),
    name VARCHAR(160),
    description VARCHAR(500),
    category_name VARCHAR(80),
    unit_code VARCHAR(16),
    purchase_price NUMERIC(14, 4),
    sale_price NUMERIC(14, 2),
    current_stock NUMERIC(14, 3),
    minimum_stock NUMERIC(14, 3),
    image_url VARCHAR(500)
) ON COMMIT DROP;

INSERT INTO seed_demo_products (
    code, name, description, category_name, unit_code, purchase_price, sale_price, current_stock, minimum_stock, image_url
)
VALUES
    ('BEB001', 'Coca-Cola 600 ml', 'Gaseosa Coca-Cola sabor original en botella de 600 ml', 'Bebidas', 'UND', 5.62, 7.20, 24.000, 10.000, 'https://walmartgt.vtexassets.com/arquivos/ids/946989-150-auto?aspect=true&height=auto&v=639052101037700000&width=150'),
    ('BEB002', 'Coca-Cola Zero Azucar 600 ml', 'Gaseosa Coca-Cola sin azucar en botella de 600 ml', 'Bebidas', 'UND', 5.62, 7.20, 6.000, 10.000, 'https://walmartgt.vtexassets.com/arquivos/ids/1038851-150-auto?aspect=true&height=auto&v=639207828089900000&width=150'),
    ('BEB003', 'Pepsi 600 ml', 'Gaseosa Pepsi en botella de 600 ml', 'Bebidas', 'UND', 4.06, 5.20, 20.000, 8.000, 'https://walmartgt.vtexassets.com/arquivos/ids/654055-150-auto?aspect=true&height=auto&v=638658212381030000&width=150'),
    ('BEB004', 'Fanta Naranja 600 ml', 'Gaseosa Fanta sabor naranja en botella de 600 ml', 'Bebidas', 'UND', 4.37, 5.60, 18.000, 8.000, 'https://walmartgt.vtexassets.com/arquivos/ids/1065104-150-auto?aspect=true&height=auto&v=639233580138070000&width=150'),
    ('BEB005', 'Fanta Uva 600 ml', 'Gaseosa Fanta sabor uva en botella de 600 ml', 'Bebidas', 'UND', 4.37, 5.60, 15.000, 8.000, 'https://walmartgt.vtexassets.com/arquivos/ids/1038867-150-auto?aspect=true&height=auto&v=639207831069100000&width=150'),
    ('BEB006', 'Del Frutal Mango 330 ml', 'Nectar Del Frutal sabor mango de 330 ml', 'Bebidas', 'UND', 3.08, 3.95, 30.000, 10.000, 'https://walmartgt.vtexassets.com/arquivos/ids/833812-150-auto?aspect=true&height=auto&v=638859489435900000&width=150'),
    ('BEB007', 'Gatorade Uva 600 ml', 'Bebida hidratante Gatorade sabor uva de 600 ml', 'Bebidas', 'UND', 7.64, 9.80, 12.000, 6.000, 'https://walmartgt.vtexassets.com/arquivos/ids/452041-150-auto?aspect=true&height=auto&v=638416330056430000&width=150'),
    ('BEB008', 'Agua Pura Salvavidas 20 oz', 'Agua pura Salvavidas botella con tapa rosca de 20 oz', 'Bebidas', 'UND', 2.57, 3.30, 25.000, 12.000, 'https://walmartgt.vtexassets.com/arquivos/ids/651467-150-auto?aspect=true&height=auto&v=638655640241470000&width=150'),
    ('REF001', 'Huevos Granjazul Blancos Grandes 60 unidades', 'Carton de huevos blancos grandes Granjazul de 60 unidades', 'Refrigerados', 'PAQ', 64.35, 82.50, 12.000, 4.000, 'https://walmartgt.vtexassets.com/arquivos/ids/362979-150-auto?aspect=true&height=auto&v=638195241957530000&width=150'),
    ('REF002', 'Huevos Avicola Fatima Blancos Grandes 30 unidades', 'Carton de huevos blancos grandes Avicola Fatima de 30 unidades', 'Refrigerados', 'PAQ', 34.32, 44.00, 4.000, 6.000, 'https://walmartgt.vtexassets.com/arquivos/ids/819868-150-auto?aspect=true&height=auto&v=638839668012900000&width=150'),
    ('REF003', 'Leche Deslactosada Coronado Caja 12 L', 'Caja de 12 unidades de leche deslactosada Coronado, total 12 litros', 'Refrigerados', 'CAJA', 128.70, 165.00, 0.000, 2.000, 'https://walmartgt.vtexassets.com/arquivos/ids/1026667-150-auto?aspect=true&height=auto&v=639202629196500000&width=150'),
    ('REF004', 'Crema Trebolac Pura 450 ml', 'Crema pura Trebolac presentacion de 450 ml', 'Refrigerados', 'UND', 18.33, 23.50, 10.000, 4.000, 'https://walmartgt.vtexassets.com/arquivos/ids/434022-150-auto?aspect=true&height=auto&v=638377656020770000&width=150'),
    ('REF005', 'Queso Great Value Mozzarella 226 g', 'Queso mozzarella rallado Great Value de 226 g', 'Refrigerados', 'UND', 21.84, 28.00, 8.000, 4.000, 'https://walmartgt.vtexassets.com/arquivos/ids/683439-150-auto?aspect=true&height=auto&v=638678912912800000&width=150'),
    ('REF006', 'Queso Crema Dos Pinos 210 g', 'Queso crema original Dos Pinos de 210 g', 'Refrigerados', 'UND', 19.89, 25.50, 9.000, 4.000, 'https://walmartgt.vtexassets.com/arquivos/ids/770509-150-auto?aspect=true&height=auto&v=638762162501130000&width=150'),
    ('REF007', 'Yogurt Oikos Natural 900 g', 'Yogurt Oikos natural sin azucar de 900 g', 'Refrigerados', 'UND', 35.10, 45.00, 12.000, 4.000, 'https://walmartgt.vtexassets.com/arquivos/ids/999205-150-auto?aspect=true&height=auto&v=639150834774900000&width=150'),
    ('REF008', 'Queso Trebolac Panela 350 g', 'Queso panela Trebolac de 350 g', 'Refrigerados', 'UND', 31.01, 39.75, 6.000, 3.000, 'https://walmartgt.vtexassets.com/arquivos/ids/1039636-150-auto?aspect=true&height=auto&v=639207990687170000&width=150'),
    ('GRA001', 'Arroz Molinero Blanco 1 kg', 'Arroz blanco Molinero en bolsa de 1 kg', 'Granos basicos', 'UND', 13.46, 17.25, 18.000, 8.000, 'https://walmartgt.vtexassets.com/arquivos/ids/510419-150-auto?aspect=true&height=auto&v=638469562332430000&width=150'),
    ('GRA002', 'Arroz Gallo Dorado Precocido 400 g', 'Arroz precocido Gallo Dorado de 400 g', 'Granos basicos', 'UND', 6.12, 7.85, 15.000, 8.000, 'https://walmartgt.vtexassets.com/arquivos/ids/833983-150-auto?aspect=true&height=auto&v=638859490228430000&width=150'),
    ('GRA003', 'Frijoles Great Value Negros 400 g', 'Frijol negro Great Value en presentacion de 400 g', 'Granos basicos', 'UND', 6.24, 8.00, 4.000, 6.000, 'https://walmartgt.vtexassets.com/arquivos/ids/968086-150-auto?aspect=true&height=auto&v=639086703734970000&width=150'),
    ('GRA004', 'Arroz Molinero Blanco 400 g', 'Arroz blanco Molinero en bolsa de 400 g', 'Granos basicos', 'UND', 5.69, 7.30, 20.000, 8.000, 'https://walmartgt.vtexassets.com/arquivos/ids/452167-150-auto?aspect=true&height=auto&v=638416330788100000&width=150'),
    ('GRA005', 'Frijol Albay Negro 400 g', 'Frijol negro Albay en presentacion de 400 g', 'Granos basicos', 'UND', 7.80, 10.00, 14.000, 6.000, 'https://walmartgt.vtexassets.com/arquivos/ids/587107-150-auto?aspect=true&height=auto&v=638604870060830000&width=150'),
    ('GRA006', 'Frijol Albay Rojo 400 g', 'Frijol rojo Albay en presentacion de 400 g', 'Granos basicos', 'UND', 8.97, 11.50, 13.000, 6.000, 'https://walmartgt.vtexassets.com/arquivos/ids/587104-150-auto?aspect=true&height=auto&v=638604870044800000&width=150'),
    ('GRA007', 'Arroz Gallo Dorado Precocido 1 kg', 'Arroz precocido Gallo Dorado en bolsa de 1 kg', 'Granos basicos', 'UND', 15.02, 19.25, 9.000, 5.000, 'https://walmartgt.vtexassets.com/arquivos/ids/795259-150-auto?aspect=true&height=auto&v=638799294218130000&width=150'),
    ('GRA008', 'Arroz Gallo Dorado Integral 1 kg', 'Arroz integral precocido Gallo Dorado de 1 kg', 'Granos basicos', 'UND', 15.99, 20.50, 8.000, 4.000, 'https://walmartgt.vtexassets.com/arquivos/ids/986651-150-auto?aspect=true&height=auto&v=639129267924500000&width=150'),
    ('ABA001', 'Azucar Los Tulipanes Morena 2 kg', 'Azucar morena Los Tulipanes en presentacion de 2 kg', 'Abarrotes', 'UND', 14.20, 18.20, 16.000, 6.000, 'https://walmartgt.vtexassets.com/arquivos/ids/509561-150-auto?aspect=true&height=auto&v=638468913976870000&width=150'),
    ('ABA002', 'Pasta Ina Espagueti 200 g', 'Pasta larga Ina tipo espagueti de 200 g', 'Abarrotes', 'UND', 3.08, 3.95, 30.000, 10.000, 'https://walmartgt.vtexassets.com/arquivos/ids/400223-150-auto?aspect=true&height=auto&v=638309182236400000&width=150'),
    ('ABA003', 'Azucar Los Tulipanes Morena 800 g', 'Azucar morena Los Tulipanes en presentacion de 800 g', 'Abarrotes', 'UND', 5.62, 7.20, 14.000, 6.000, 'https://walmartgt.vtexassets.com/arquivos/ids/581393-150-auto?aspect=true&height=auto&v=638596996270530000&width=150'),
    ('ABA004', 'Frijol Ducal Molido Negro 993 g', 'Frijol negro molido Ducal de 993 g', 'Abarrotes', 'UND', 11.70, 15.00, 12.000, 5.000, 'https://walmartgt.vtexassets.com/arquivos/ids/846516-150-auto?aspect=true&height=auto&v=638881176845900000&width=150'),
    ('ABA005', 'Sal YaEsta Refinada 400 g', 'Sal refinada YaEsta de 400 g', 'Abarrotes', 'UND', 1.87, 2.40, 3.000, 5.000, 'https://walmartgt.vtexassets.com/arquivos/ids/510122-150-auto?aspect=true&height=auto&v=638469455569030000&width=150'),
    ('ABA006', 'Sal B&Z Yodada Artesanal 920 g', 'Sal yodada artesanal B&Z de 920 g', 'Abarrotes', 'UND', 2.11, 2.70, 20.000, 8.000, 'https://walmartgt.vtexassets.com/arquivos/ids/1068458-150-auto?aspect=true&height=auto&v=639238836219800000&width=150'),
    ('ABA007', 'Sopa Maggi Pollo con Fideos 55 g', 'Sopa Maggi sabor pollo con fideos de 55 g', 'Abarrotes', 'UND', 2.15, 2.75, 25.000, 10.000, 'https://walmartgt.vtexassets.com/arquivos/ids/497543-150-auto?aspect=true&height=auto&v=638451853173900000&width=150'),
    ('ABA008', 'Atun Calvo en Agua 3 Pack 426 g', 'Paquete de tres latas de atun Calvo en agua, 426 g total', 'Abarrotes', 'PAQ', 23.40, 30.00, 10.000, 4.000, 'https://walmartgt.vtexassets.com/arquivos/ids/982367-150-auto?aspect=true&height=auto&v=639117132245500000&width=150'),
    ('SNA001', 'Tortrix Quesito Crema 300 g', 'Snack Tortrix sabor quesito crema de 300 g', 'Snacks y dulces', 'UND', 14.24, 18.25, 18.000, 8.000, 'https://walmartgt.vtexassets.com/arquivos/ids/773972-150-auto?aspect=true&height=auto&v=638767135267570000&width=150'),
    ('SNA002', 'Tortrix Mix Detodito 150 g', 'Snack Tortrix Mix Detodito de 150 g', 'Snacks y dulces', 'UND', 8.38, 10.75, 16.000, 8.000, 'https://walmartgt.vtexassets.com/arquivos/ids/542511-150-auto?aspect=true&height=auto&v=638550984747430000&width=150'),
    ('SNA003', 'Tortrix Barbacoa 150 g', 'Snack Tortrix sabor barbacoa de 150 g', 'Snacks y dulces', 'UND', 7.80, 10.00, 15.000, 8.000, 'https://walmartgt.vtexassets.com/arquivos/ids/930574-150-auto?aspect=true&height=auto&v=639026388189270000&width=150'),
    ('SNA004', 'Tortrix Picante 150 g', 'Snack Tortrix sabor picante de 150 g', 'Snacks y dulces', 'UND', 8.00, 10.25, 4.000, 8.000, 'https://walmartgt.vtexassets.com/arquivos/ids/930559-150-auto?aspect=true&height=auto&v=639026388074270000&width=150'),
    ('SNA005', 'Galletas Oreo Original 12 Pack 432 g', 'Paquete de galletas Oreo original con 12 unidades, 432 g', 'Snacks y dulces', 'PAQ', 15.60, 20.00, 12.000, 6.000, 'https://walmartgt.vtexassets.com/arquivos/ids/974395-150-auto?aspect=true&height=auto&v=639099126276870000&width=150'),
    ('SNA006', 'Galletas Pozuelo Chiky Chocolate 480 g', 'Galletas Pozuelo Chiky chocolate de 480 g', 'Snacks y dulces', 'UND', 15.60, 20.00, 10.000, 5.000, 'https://walmartgt.vtexassets.com/arquivos/ids/1054760-150-auto?aspect=true&height=auto&v=639223533983770000&width=150'),
    ('SNA007', 'Galleta Gama Wafer Vainilla 12 U 240 g', 'Paquete de galletas Gama wafer vainilla, 12 unidades y 240 g', 'Snacks y dulces', 'PAQ', 8.00, 10.25, 14.000, 6.000, 'https://walmartgt.vtexassets.com/arquivos/ids/965860-150-auto?aspect=true&height=auto&v=639081759711400000&width=150'),
    ('SNA008', 'Galleta Can Can Extra Chocolate y Vainilla 12 U 450 g', 'Paquete de galletas Can Can Extra chocolate y vainilla de 450 g', 'Snacks y dulces', 'PAQ', 9.91, 12.70, 12.000, 6.000, 'https://walmartgt.vtexassets.com/arquivos/ids/574139-150-auto?aspect=true&height=auto&v=638590962152000000&width=150'),
    ('LIM001', 'Ajax Triclorin con Cloro 600 g', 'Limpiador en polvo Ajax Triclorin con cloro de 600 g', 'Limpieza', 'UND', 17.55, 22.50, 10.000, 4.000, 'https://walmartgt.vtexassets.com/arquivos/ids/1051604-150-auto?aspect=true&height=auto&v=639221514205630000&width=150'),
    ('LIM002', 'Cloro Magia Blanca Galon 3.785 L', 'Cloro Magia Blanca en presentacion de un galon, 3.785 L', 'Limpieza', 'UND', 18.33, 23.50, 8.000, 4.000, 'https://walmartgt.vtexassets.com/arquivos/ids/819985-150-auto?aspect=true&height=auto&v=638839767183270000&width=150'),
    ('LIM003', 'Lavaplatos Axion Limon 1 kg', 'Pasta lavaplatos Axion aroma limon de 1 kg', 'Limpieza', 'UND', 14.24, 18.25, 12.000, 5.000, 'https://walmartgt.vtexassets.com/arquivos/ids/974805-150-auto?aspect=true&height=auto&v=639099130413830000&width=150'),
    ('LIM004', 'Papel Higienico Supermax 16 Rollos', 'Paquete de papel higienico Supermax de 16 rollos', 'Limpieza', 'PAQ', 53.04, 68.00, 6.000, 3.000, 'https://walmartgt.vtexassets.com/arquivos/ids/470925-150-auto?aspect=true&height=auto&v=638419355678030000&width=150'),
    ('LIM005', 'Servilletas Nube Blancas 500 U', 'Paquete de servilletas Nube blancas de 500 unidades', 'Limpieza', 'PAQ', 17.94, 23.00, 15.000, 5.000, 'https://walmartgt.vtexassets.com/arquivos/ids/1042477-150-auto?aspect=true&height=auto&v=639211086540600000&width=150'),
    ('LIM006', 'Toalla de Papel Suli 3 Rollos', 'Paquete de toalla de papel Suli de 3 rollos', 'Limpieza', 'PAQ', 14.04, 18.00, 9.000, 4.000, 'https://walmartgt.vtexassets.com/arquivos/ids/799380-150-auto?aspect=true&height=auto&v=638803260269270000&width=150'),
    ('LIM007', 'Bolsas para Basura Supermax Blancas 50 U', 'Paquete de bolsas para basura Supermax blancas de 50 unidades', 'Limpieza', 'PAQ', 15.41, 19.75, 0.000, 5.000, 'https://walmartgt.vtexassets.com/arquivos/ids/857872-150-auto?aspect=true&height=auto&v=638899209416000000&width=150'),
    ('LIM008', 'Esponja Scotch-Brite Doble Uso 2 U', 'Paquete de dos esponjas Scotch-Brite doble uso', 'Limpieza', 'PAQ', 9.36, 12.00, 14.000, 5.000, 'https://walmartgt.vtexassets.com/arquivos/ids/695132-150-auto?aspect=true&height=auto&v=638687785668000000&width=150'),
    ('HIG001', 'Protectores Diarios Kotex Largos 50 U', 'Protectores diarios Kotex largos extra proteccion, 50 unidades', 'Higiene personal', 'PAQ', 16.38, 21.00, 8.000, 4.000, 'https://walmartgt.vtexassets.com/arquivos/ids/848375-150-auto?aspect=true&height=auto&v=638881899550900000&width=150'),
    ('HIG002', 'Cepillo Oral-B 7 Beneficios 2 U', 'Paquete de dos cepillos dentales Oral-B 7 Beneficios', 'Higiene personal', 'PAQ', 21.06, 27.00, 10.000, 4.000, 'https://walmartgt.vtexassets.com/arquivos/ids/1079035-150-auto?aspect=true&height=auto&v=639244838081830000&width=150'),
    ('HIG003', 'Pasta Dental Oral-B Extra Blancura 150 ml', 'Pasta dental Oral-B Extra Blancura de 150 ml', 'Higiene personal', 'UND', 14.24, 18.25, 12.000, 5.000, 'https://walmartgt.vtexassets.com/arquivos/ids/1057420-150-auto?aspect=true&height=auto&v=639227497925730000&width=150'),
    ('HIG004', 'Desodorante Dove Invisible Dry 150 ml', 'Desodorante Dove Invisible Dry en aerosol de 150 ml', 'Higiene personal', 'UND', 30.42, 39.00, 9.000, 4.000, 'https://walmartgt.vtexassets.com/arquivos/ids/675519-150-auto?aspect=true&height=auto&v=638672022960670000&width=150'),
    ('HIG005', 'Antitranspirante Secret Coconut 45 g', 'Antitranspirante Secret aroma Coconut de 45 g', 'Higiene personal', 'UND', 17.75, 22.75, 10.000, 4.000, 'https://walmartgt.vtexassets.com/arquivos/ids/1075216-150-auto?aspect=true&height=auto&v=639240533686600000&width=150'),
    ('HIG006', 'Tampones Kotex Medio con Aplicador 8 U', 'Tampones Kotex tamano medio con aplicador, 8 unidades', 'Higiene personal', 'PAQ', 18.14, 23.25, 3.000, 5.000, 'https://walmartgt.vtexassets.com/arquivos/ids/848560-150-auto?aspect=true&height=auto&v=638881900174600000&width=150'),
    ('HIG007', 'Pantene Pro-V Miracles Colageno 90 ml', 'Serum sellador Pantene Pro-V Miracles Colageno de 90 ml', 'Higiene personal', 'UND', 29.64, 38.00, 7.000, 3.000, 'https://walmartgt.vtexassets.com/arquivos/ids/1075164-150-auto?aspect=true&height=auto&v=639240533481330000&width=150'),
    ('HIG008', 'Pantene Mascarilla Nutre y Sella Puntas 300 ml', 'Mascarilla Pantene para nutrir y sellar puntas, 300 ml', 'Higiene personal', 'UND', 35.49, 45.50, 6.000, 3.000, 'https://walmartgt.vtexassets.com/arquivos/ids/932579-150-auto?aspect=true&height=auto&v=639033192399400000&width=150'),
    ('MAS001', 'Dog Chow Adulto Medianos y Grandes 25 kg', 'Alimento seco Dog Chow para perro adulto mediano y grande, 25 kg', 'Articulos para mascotas', 'UND', 355.49, 455.75, 4.000, 2.000, 'https://walmartgt.vtexassets.com/arquivos/ids/984022-150-auto?aspect=true&height=auto&v=639118929213270000&width=150'),
    ('MAS002', 'Rufo Perro Adulto 44 lb', 'Alimento seco Rufo para perro adulto en saco de 44 lb', 'Articulos para mascotas', 'UND', 181.74, 233.00, 6.000, 2.000, 'https://walmartgt.vtexassets.com/arquivos/ids/816776-150-auto?aspect=true&height=auto&v=638834052192270000&width=150'),
    ('MAS003', 'Felix Salmon en Salsa 85 g', 'Alimento humedo Felix para gato sabor salmon en salsa de 85 g', 'Articulos para mascotas', 'UND', 5.85, 7.50, 18.000, 6.000, 'https://walmartgt.vtexassets.com/arquivos/ids/770037-150-auto?aspect=true&height=auto&v=638760926916530000&width=150'),
    ('MAS004', 'Felix Pate Pavo y Menudencias 156 g', 'Alimento humedo Felix para gato pate de pavo y menudencias, 156 g', 'Articulos para mascotas', 'UND', 11.31, 14.50, 12.000, 5.000, 'https://walmartgt.vtexassets.com/arquivos/ids/861116-150-auto?aspect=true&height=auto&v=638907975066130000&width=150'),
    ('MAS005', 'Felix Pate Salmon 156 g', 'Alimento humedo Felix para gato pate sabor salmon de 156 g', 'Articulos para mascotas', 'UND', 11.31, 14.50, 11.000, 5.000, 'https://walmartgt.vtexassets.com/arquivos/ids/861082-150-auto?aspect=true&height=auto&v=638907861217430000&width=150'),
    ('MAS006', 'Felix Pollo en Salsa 85 g', 'Alimento humedo Felix para gato sabor pollo en salsa de 85 g', 'Articulos para mascotas', 'UND', 5.85, 7.50, 20.000, 6.000, 'https://walmartgt.vtexassets.com/arquivos/ids/770060-150-auto?aspect=true&height=auto&v=638760927017170000&width=150'),
    ('MAS007', 'Rufo Perro Cachorro 44 lb', 'Alimento seco Rufo para perro cachorro en saco de 44 lb', 'Articulos para mascotas', 'UND', 175.50, 225.00, 5.000, 2.000, 'https://walmartgt.vtexassets.com/arquivos/ids/816783-150-auto?aspect=true&height=auto&v=638834058075900000&width=150'),
    ('MAS008', 'Dog Chow Humedo Carne 100 g', 'Alimento humedo Dog Chow para perro sabor carne de 100 g', 'Articulos para mascotas', 'UND', 7.02, 9.00, 15.000, 5.000, 'https://walmartgt.vtexassets.com/arquivos/ids/814150-150-auto?aspect=true&height=auto&v=638830105172970000&width=150');

INSERT INTO products (
    code, name, description, category_id, unit_id, purchase_price, sale_price, current_stock, minimum_stock, image_url
)
SELECT
    s.code, s.name, s.description, c.id, u.id, s.purchase_price, s.sale_price, s.current_stock, s.minimum_stock, s.image_url
FROM seed_demo_products s
JOIN product_categories c ON lower(c.name) = lower(s.category_name)
JOIN measurement_units u ON u.code = s.unit_code
WHERE NOT EXISTS (
    SELECT 1 FROM products p WHERE lower(p.code) = lower(s.code)
);

UPDATE products p
SET
    name = s.name,
    description = s.description,
    category_id = c.id,
    unit_id = u.id,
    purchase_price = s.purchase_price,
    sale_price = s.sale_price,
    current_stock = s.current_stock,
    minimum_stock = s.minimum_stock,
    image_url = s.image_url,
    active = TRUE
FROM seed_demo_products s
JOIN product_categories c ON lower(c.name) = lower(s.category_name)
JOIN measurement_units u ON u.code = s.unit_code
WHERE lower(p.code) = lower(s.code);

INSERT INTO notifications (product_id, notification_type, title, message)
SELECT
    p.id,
    CASE WHEN p.current_stock = 0 THEN 'OUT_OF_STOCK' ELSE 'LOW_STOCK' END,
    CASE WHEN p.current_stock = 0 THEN 'Producto agotado' ELSE 'Existencia baja' END,
    CASE
        WHEN p.current_stock = 0 THEN 'El producto ' || p.name || ' se encuentra agotado.'
        ELSE 'El producto ' || p.name || ' tiene ' || trim(to_char(p.current_stock, 'FM999999990.###')) || ' unidades y su minimo es ' || trim(to_char(p.minimum_stock, 'FM999999990.###')) || '.'
    END
FROM products p
WHERE p.active = TRUE
  AND p.current_stock <= p.minimum_stock
  AND p.code IN (SELECT code FROM seed_demo_products)
  AND NOT EXISTS (
      SELECT 1
      FROM notifications n
      WHERE n.product_id = p.id
        AND n.notification_type = CASE WHEN p.current_stock = 0 THEN 'OUT_OF_STOCK' ELSE 'LOW_STOCK' END
        AND n.is_read = FALSE
  );
