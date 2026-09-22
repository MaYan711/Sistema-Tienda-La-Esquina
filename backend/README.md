# Tienda La Esquina API

API REST del sistema **Tienda La Esquina**, una solución web para digitalizar y centralizar la administración de una tienda de barrio. El backend gestiona autenticación, usuarios, catálogo, productos, inventario, alertas de stock, proveedores, entradas de mercadería y ventas.

La API utiliza el contexto base `/api/v1`. En el perfil de desarrollo (`dev`) se ejecuta en el puerto `8090` y en el perfil de producción (`prod`) en el puerto `8080`.

## Tecnologías principales

- Java 21.
- Spring Boot 4.1.0.
- Spring Web MVC para la API REST.
- Spring Security para autenticación y autorización.
- JWT para sesiones stateless.
- OTP para segundo factor y recuperación de contraseña.
- Spring Data JPA / Hibernate para persistencia.
- PostgreSQL 18.
- Flyway para migraciones de base de datos.
- MapStruct 1.6.3 para mapeo entre entidades y DTO.
- Spring Mail con SMTP para envío de códigos OTP.
- Springdoc OpenAPI 3.1.0 y Swagger UI para documentación de la API.
- Gradle 9.5.1 para construcción y administración de dependencias.
- Docker y Docker Compose para PostgreSQL en desarrollo.

El frontend que consume esta API está desarrollado con Angular 21 y se encuentra en el directorio `frontend/` del repositorio.

## Módulos del sistema

Actualmente el backend incluye los siguientes módulos funcionales:

- Autenticación y seguridad.
- Administración de usuarios.
- Roles `ADMIN` y `EMPLOYEE`.
- Recuperación y cambio de contraseña.
- Segundo factor de autenticación mediante OTP.
- Categorías de productos.
- Unidades de medida.
- Productos.
- Control de existencias.
- Movimientos de inventario.
- Ajustes manuales de inventario.
- Alertas y notificaciones de stock.
- Proveedores.
- Entradas de mercadería.
- Ventas.
- Auditoría de operaciones relevantes.

## Requisitos

Para ejecutar el backend en desarrollo se necesita:

- Java 21.
- Docker y Docker Compose.
- PostgreSQL 18 mediante el contenedor incluido en el proyecto.
- Variables de entorno configuradas.
- Conexión SMTP válida si se desean probar los correos OTP reales.

Los comandos del backend deben ejecutarse desde el directorio:

```text
backend/
```

## Base de datos

El archivo `docker-compose.yaml` crea un contenedor PostgreSQL 18 con la siguiente configuración de desarrollo:

```text
Base de datos: tienda_la_esquina_db
Host: localhost
Puerto del host: 5438
Puerto del contenedor: 5432
Contenedor: tienda-la-esquina-postgres
Volumen: tienda_postgres_data
```

La URL JDBC utilizada localmente debe apuntar a:

```text
jdbc:postgresql://localhost:5438/tienda_la_esquina_db
```

## Configuración

El proyecto utiliza `application.properties` como configuración común y los perfiles:

```text
application-dev.properties
application-prod.properties
```

El perfil activo por defecto es:

```text
dev
```

Spring carga de forma opcional un archivo `.env` mediante:

```properties
spring.config.import=optional:file:.env[.properties],optional:file:backend/.env[.properties]
```

Por lo tanto, para desarrollo puede crearse `backend/.env` tomando como referencia las variables siguientes.

### Variables de entorno

```env
DATABASE_URL=jdbc:postgresql://localhost:5438/tienda_la_esquina_db
DATABASE_USERNAME=tienda_user
DATABASE_PASSWORD=change-this-password

SECRET_KEY_JWT=replace-with-at-least-32-random-characters
EXPIRATION_TIME_JWT=86400000

INITIAL_ADMIN_EMAIL=admin@tiendalaesquina.local
INITIAL_ADMIN_PASSWORD=replace-with-a-strong-password

OTP_LENGTH=6
OTP_EXPIRATION=PT10M
OTP_MAX_ATTEMPTS=5
OTP_RESEND_COOLDOWN=PT1M

MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=tu-correo-remitente@gmail.com
MAIL_PASSWORD=tu-contrasena-de-aplicacion
MAIL_FROM=tu-correo-remitente@gmail.com
```

No deben subirse credenciales reales, contraseñas, claves JWT ni contraseñas de aplicación de correo al repositorio.

## Administrador inicial

Durante el arranque, el sistema crea o normaliza de forma idempotente una cuenta con rol `ADMIN` utilizando:

```text
INITIAL_ADMIN_EMAIL
INITIAL_ADMIN_PASSWORD
```

El correo predeterminado, si no se configura otro, es:

```text
admin@tiendalaesquina.local
```

La contraseña inicial debe cumplir la política de contraseñas. Si `INITIAL_ADMIN_PASSWORD` no está definida o no cumple la política, la aplicación falla explícitamente durante el arranque.

Si el administrador ya existe, el sistema no reemplaza su contraseña automáticamente. Sí garantiza que la cuenta tenga rol `ADMIN`, se encuentre verificada y esté habilitada.

## Política de contraseñas y OTP

Las contraseñas deben cumplir las siguientes reglas:

- Entre 8 y 72 caracteres.
- Al menos una letra mayúscula.
- Al menos una letra minúscula.
- Al menos un número.

La política OTP predeterminada es:

```text
Longitud: 6 dígitos
Expiración: 10 minutos
Intentos máximos: 5
Tiempo mínimo para reenvío: 1 minuto
```

Los OTP no se almacenan en texto plano.

## Correo electrónico

El envío de correo utiliza SMTP. La configuración predeterminada está preparada para Gmail:

```text
Host: smtp.gmail.com
Puerto: 587
Autenticación SMTP: habilitada
STARTTLS: habilitado y requerido
```

Las credenciales se proporcionan mediante `MAIL_USERNAME` y `MAIL_PASSWORD`. Para Gmail se recomienda utilizar una contraseña de aplicación y no la contraseña normal de la cuenta.

## Ejecución

### 1. Iniciar PostgreSQL

Desde `backend/`:

```bash
docker compose up -d
```

Verificar el estado del contenedor:

```bash
docker compose ps
```

### 2. Ejecutar el backend

En Windows con Git Bash:

```bash
./gradlew bootRun
```

En Windows con PowerShell o CMD:

```powershell
.\gradlew.bat bootRun
```

Al iniciar correctamente, la API de desarrollo queda disponible en:

```text
http://localhost:8090/api/v1
```

### 3. Detener PostgreSQL

```bash
docker compose down
```

Para detener los contenedores sin eliminar el volumen persistente de PostgreSQL, basta con el comando anterior.

## Comandos de verificación

En Git Bash, Linux o macOS:

```bash
./gradlew compileJava
./gradlew test
./gradlew check
./gradlew build
```

En PowerShell o CMD:

```powershell
.\gradlew.bat compileJava
.\gradlew.bat test
.\gradlew.bat check
.\gradlew.bat build
```

Las pruebas disponibles actualmente incluyen validaciones de arranque, política de contraseñas, administración de usuarios, flujo principal del sistema, ajustes de inventario y movimientos/notificaciones de stock.

## Arquitectura

El backend utiliza una arquitectura por capas bajo el paquete raíz:

```text
com.tiendalaesquina
```

La estructura principal es:

```text
src/main/java/com/tiendalaesquina/
├── application/
│   ├── auth/           Autenticación, contraseñas y OTP
│   ├── catalog/        Categorías y unidades de medida
│   ├── common/         Utilidades compartidas
│   ├── inventory/      Inventario, movimientos, ajustes y alertas
│   ├── mail/           Abstracción y envío de correo
│   ├── product/        Casos de uso de productos
│   ├── sale/           Casos de uso de ventas
│   ├── stockentry/     Entradas de mercadería
│   ├── supplier/       Proveedores
│   └── user/           Administración de usuarios
├── config/             Seguridad, propiedades, OpenAPI y bootstrap
├── domain/
│   ├── model/          Entidades y enumeraciones del dominio
│   └── repository/     Repositorios de persistencia
├── exception/          Excepciones de aplicación
├── security/           JWT y respuestas de seguridad
└── web/
    ├── admin/          Operaciones administrativas
    ├── auth/           Endpoints de autenticación
    ├── catalog/        Endpoints de catálogo
    ├── dto/            Objetos de entrada y salida
    ├── exception/      Manejo centralizado de errores HTTP
    ├── inventory/      Endpoints de inventario
    ├── notification/   Endpoints de alertas
    ├── product/        Endpoints de productos
    ├── sale/           Endpoints de ventas
    ├── stockentry/     Endpoints de entradas de mercadería
    ├── supplier/       Endpoints de proveedores
    ├── user/           Endpoints de usuarios
    └── validation/     Validaciones web
```

El flujo general de una solicitud es:

```text
Angular -> Controller -> Service -> Repository -> PostgreSQL
```

## Migraciones de base de datos

Las migraciones forward-only de Flyway se encuentran en:

```text
src/main/resources/db/migration/
```

Actualmente el proyecto contiene:

```text
V1__create_core_tables.sql
V2__seed_roles.sql
V3__create_store_operations_tables.sql
V4__seed_catalog_basics.sql
V5__seed_64_guatemala_products.sql
V6__add_supplier_contact_fields.sql
```

Resumen:

- `V1`: crea las tablas base de autenticación, usuarios, OTP y auditoría.
- `V2`: registra los roles `ADMIN` y `EMPLOYEE`.
- `V3`: crea las tablas operativas de catálogo, productos, inventario, proveedores, entradas, ventas y notificaciones.
- `V4`: carga categorías y unidades de medida iniciales.
- `V5`: carga 64 productos de referencia orientados a una tienda de barrio en Guatemala.
- `V6`: agrega NIT y correo electrónico a proveedores.

Hibernate está configurado con:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Por lo tanto, la creación y evolución del esquema debe realizarse mediante Flyway y no mediante generación automática de Hibernate.

## Seguridad y roles

El sistema utiliza sesiones stateless con JWT.

Los roles existentes son:

- `ADMIN`: acceso a administración y configuración del sistema.
- `EMPLOYEE`: acceso a las operaciones permitidas para empleados, principalmente consulta de catálogo/productos y registro/consulta de ventas.

No existe registro público de usuarios. Las cuentas del sistema son administradas por un usuario con rol `ADMIN`.

## Contrato de la API

Todas las rutas siguientes utilizan el prefijo:

```text
/api/v1
```

### Autenticación

Endpoints públicos:

- `POST /auth/login`: iniciar sesión con correo y contraseña.
- `POST /auth/login/verify`: verificar el OTP de inicio de sesión.
- `POST /auth/verify-login`: alias de verificación del OTP de inicio de sesión.
- `POST /auth/password-recovery`: solicitar recuperación de contraseña.
- `POST /auth/forgot-password`: alias de solicitud de recuperación.
- `POST /auth/password-recovery/verify`: restablecer la contraseña utilizando OTP.
- `POST /auth/reset-password`: alias del restablecimiento de contraseña.

Endpoints autenticados:

- `GET /auth/me`: obtener la cuenta autenticada.
- `POST /auth/password/change`: cambiar la contraseña actual.
- `POST /auth/change-password`: alias del cambio de contraseña.
- `POST /auth/2fa/enable`: solicitar activación de 2FA.
- `POST /auth/2fa/enable/verify`: confirmar activación de 2FA.
- `POST /auth/2fa/disable`: solicitar desactivación de 2FA.
- `POST /auth/2fa/disable/verify`: confirmar desactivación de 2FA.

### Usuarios

Requieren rol `ADMIN`:

- `GET /users`: listar y filtrar usuarios.
- `GET /users/{id}`: consultar un usuario.
- `POST /users`: crear un usuario.
- `PUT /users/{id}`: editar correo y rol.
- `PATCH /users/{id}/status`: activar o desactivar un usuario.

### Catálogo

Consulta disponible para `ADMIN` y `EMPLOYEE`:

- `GET /catalog/categories`: listar categorías.
- `GET /catalog/measurement-units`: listar unidades de medida.

Administración exclusiva de `ADMIN`:

- `POST /catalog/categories`.
- `PUT /catalog/categories/{id}`.
- `PATCH /catalog/categories/{id}/status`.
- `POST /catalog/measurement-units`.
- `PUT /catalog/measurement-units/{id}`.
- `PATCH /catalog/measurement-units/{id}/status`.

### Productos

Consulta disponible para `ADMIN` y `EMPLOYEE`:

- `GET /products`: listar, buscar y filtrar productos.
- `GET /products/{id}`: consultar un producto.

Administración exclusiva de `ADMIN`:

- `POST /products`: crear un producto.
- `PUT /products/{id}`: editar un producto.
- `PATCH /products/{id}/status`: activar o desactivar un producto.

### Inventario

Requieren rol `ADMIN`:

- `GET /inventory/movements`: consultar movimientos de inventario.
- `POST /inventory/adjustments`: realizar un ajuste manual de existencias.

### Notificaciones de stock

Requieren rol `ADMIN`:

- `GET /notifications`: consultar alertas/notificaciones de inventario.
- `PATCH /notifications/{id}/read`: marcar una notificación como leída.

### Proveedores

Requieren rol `ADMIN`:

- `GET /suppliers`: listar y filtrar proveedores.
- `GET /suppliers/{id}`: consultar un proveedor.
- `POST /suppliers`: crear un proveedor.
- `PUT /suppliers/{id}`: editar un proveedor.
- `PATCH /suppliers/{id}/status`: activar o desactivar un proveedor.

### Entradas de mercadería

Requieren rol `ADMIN`:

- `POST /stock-entries`: registrar una entrada de mercadería.
- `GET /stock-entries`: listar y filtrar entradas.
- `GET /stock-entries/{id}`: consultar el detalle de una entrada.

### Ventas

Disponibles para `ADMIN` y `EMPLOYEE`:

- `POST /sales`: registrar una venta.
- `GET /sales`: listar y filtrar ventas.
- `GET /sales/{id}`: consultar el detalle de una venta.

### Administración

- `GET /admin/ping`: endpoint de verificación exclusivo para `ADMIN`.

## Manejo de errores

La API utiliza respuestas compatibles con `application/problem+json` para representar errores de forma consistente.

Las respuestas de error siguen la estructura de Problem Details e incluyen campos como:

```text
type
title
status
detail
instance
code
```

Los mensajes destinados al cliente se manejan en español.

## OpenAPI y Swagger

Con la aplicación ejecutándose en desarrollo, la documentación se encuentra disponible en:

### Swagger UI

```text
http://localhost:8090/api/v1/swagger-ui.html
```

### OpenAPI JSON

```text
http://localhost:8090/api/v1/v3/api-docs
```

Swagger permite consultar los endpoints, revisar los DTO y probar solicitudes directamente desde el navegador.

Para los endpoints protegidos debe utilizarse un JWT válido mediante el esquema `bearerAuth`.

## Integración con el frontend

El frontend Angular consume la API mediante HTTP y JSON.

En desarrollo, el flujo principal es:

```text
Usuario
  -> Angular 21
  -> API REST Spring Boot 4.1
  -> Spring Data JPA / Hibernate
  -> PostgreSQL 18
```

La URL base del backend en desarrollo es:

```text
http://localhost:8090/api/v1
```

## Estado actual del proyecto

El backend cuenta actualmente con soporte para:

- Autenticación con JWT.
- Recuperación de contraseña mediante OTP.
- Segundo factor de autenticación opcional.
- Roles `ADMIN` y `EMPLOYEE`.
- CRUD administrativo de usuarios.
- Catálogo de categorías y unidades de medida.
- Gestión de productos.
- Control y movimientos de inventario.
- Ajustes manuales de existencias.
- Alertas de stock.
- Gestión de proveedores.
- Registro y consulta de entradas de mercadería.
- Registro y consulta de ventas.
- Migraciones Flyway y datos iniciales.
- Documentación OpenAPI/Swagger.
- Pruebas automatizadas de los flujos principales.
