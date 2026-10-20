# Tienda La Esquina Client

Cliente web de **Tienda La Esquina**, sistema de gestion para una tienda de barrio. El frontend permite autenticar usuarios y operar los modulos de productos e inventario, ventas en efectivo, proveedores, entradas de mercaderia, alertas, movimientos de inventario, usuarios y seguridad de cuenta.

La aplicacion esta construida con **Angular 21**, componentes standalone, **PrimeNG**, **Tailwind CSS** y **Transloco**. Consume la API del backend bajo el contexto `/api/v1`.

## Tecnologias principales

- Angular 21.2.
- TypeScript 5.9.
- PrimeNG 21.1.9 con tema Aura.
- PrimeIcons 7.
- Tailwind CSS 4.3.1.
- `tailwindcss-primeui` 0.6.1.
- Transloco 8.4.0.
- RxJS 7.8.
- Vitest 4 para pruebas unitarias mediante el builder de Angular.
- npm 11.6.2 como gestor de paquetes configurado en el proyecto.

## Requisitos

- Node.js compatible con Angular 21. De acuerdo con las dependencias bloqueadas del proyecto, se admite Node.js `^20.19.0`, `^22.12.0` o `>=24.0.0`.
- npm. El proyecto declara `npm@11.6.2` como package manager.
- El backend de **Tienda La Esquina** ejecutandose en `http://localhost:8090` para desarrollo local.
- PostgreSQL y las variables de entorno del backend configuradas cuando se pruebe la aplicacion contra la API real.
- Navegador moderno con soporte para `localStorage`.

Los comandos del cliente deben ejecutarse desde la carpeta:

```text
frontend/
```

Las dependencias exactas estan registradas en `package-lock.json` y deben instalarse con `npm ci`.

## Configuracion

### API

La URL base utilizada por la aplicacion se define en los archivos de entorno:

- `src/environments/environment.ts`: usa `/api/v1` en desarrollo.
- `src/environments/environment.production.ts`: usa `/api/v1` en produccion.
- `angular.json`: reemplaza el archivo de entorno durante el build de produccion.

Durante `ng serve`, `proxy.conf.json` redirige todas las solicitudes que comienzan con `/api` hacia:

```text
http://localhost:8090
```

De esta forma, el frontend puede consumir en desarrollo endpoints como:

```text
http://localhost:4200/api/v1/...
```

mientras Angular los redirige al backend local.

En produccion, `/api/v1` debe estar disponible desde el mismo origen de la aplicacion o mediante la configuracion de infraestructura/reverse proxy correspondiente.

### Internacionalizacion

La aplicacion utiliza **Transloco** para centralizar los textos visibles. Actualmente solo esta habilitado el idioma espanol (`es`).

La configuracion se encuentra en `src/app/app.config.ts`:

- `availableLangs`: `['es']`.
- `defaultLang`: `es`.
- `fallbackLang`: `es`.
- `TranslocoHttpLoader` carga los catalogos desde `/i18n/{lang}.json`.
- El catalogo actual se encuentra en `public/i18n/es.json`.

Para utilizar una traduccion en un componente standalone:

```typescript
import { TranslocoPipe } from '@jsverse/transloco';

@Component({
  imports: [TranslocoPipe],
})
export class ExampleComponent {}
```

En la plantilla:

```html
<h1>{{ 'auth.login.title' | transloco }}</h1>
<input [placeholder]="'auth.fields.emailPlaceholder' | transloco" />
```

Al agregar nuevos textos de interfaz, se recomienda registrarlos en `public/i18n/es.json` y consumirlos mediante Transloco.

### PrimeNG y estilos

PrimeNG se configura globalmente en `src/app/app.config.ts` con:

- Tema `Aura`.
- Ripple habilitado.
- Modo oscuro deshabilitado actualmente.
- Capa CSS `primeng` integrada con los estilos de la aplicacion.

Los estilos globales cargados por Angular son:

```text
src/tailwind.css
node_modules/primeicons/primeicons.css
src/styles.scss
```

`src/tailwind.css` importa Tailwind CSS y la integracion `tailwindcss-primeui`.

## Autenticacion y sesion

El sistema trabaja actualmente con dos roles:

```text
ADMIN
EMPLOYEE
```

No existe una ruta publica para registrar cuentas desde el frontend. La administracion y creacion de usuarios se realiza desde el modulo protegido de usuarios para cuentas con rol `ADMIN`.

La sesion autenticada se conserva en `localStorage` bajo la clave:

```text
tienda-la-esquina.auth.session
```

La sesion almacenada contiene:

- Access token JWT.
- Tipo de token.
- Fecha de expiracion calculada por el cliente.
- Informacion del usuario autenticado.

Cuando la aplicacion inicia y existe una sesion vigente, se valida el usuario actual mediante:

```text
GET /api/v1/auth/me
```

Si el token esta vencido o la API informa que la autenticacion ya no es valida, la sesion local se elimina y el usuario es redirigido al inicio de sesion.

### Interceptors

Los interceptors se registran mediante `provideHttpClient` en `app.config.ts`.

`authInterceptor`:

- Agrega `Authorization: Bearer <token>` a las solicitudes protegidas dirigidas a la API.
- No agrega el token a los endpoints publicos de login, verificacion OTP y recuperacion de contrasena.

`apiErrorInterceptor`:

- Centraliza los errores HTTP de la API.
- Interpreta respuestas con estructura `ProblemDetail`.
- Muestra mensajes globales con `MessageService` de PrimeNG.
- Limpia la sesion cuando una solicitud protegida devuelve un fallo real de autenticacion.

No se deben registrar ni incluir en el codigo fuente contrasenas, OTP, tokens bearer ni credenciales del servicio de correo.

## Ejecucion

Instala las dependencias:

```bash
npm ci
```

Inicia el servidor de desarrollo:

```bash
npm start
```

La aplicacion estara disponible en:

```text
http://localhost:4200/
```

El servidor de desarrollo utiliza `proxy.conf.json` para comunicarse con el backend ejecutado en `http://localhost:8090`.

### Comandos disponibles

```bash
npm start
npm run build
npm run watch
npm test
```

Verificacion de formato:

```bash
npx prettier --check "src/**/*.{ts,html,scss}" "public/i18n/es.json" "proxy.conf.json"
```

El build de produccion genera los artefactos dentro de:

```text
dist/tienda-client/
```

El proyecto contiene actualmente una prueba base de creacion del componente principal en `src/app/app.spec.ts`. `npm test` utiliza el builder de pruebas unitarias de Angular y Vitest se encuentra incluido entre las dependencias de desarrollo.

## Arquitectura del frontend

El cliente utiliza componentes standalone, carga diferida de paginas mediante `loadComponent`, guards de navegacion, interceptors HTTP y una separacion entre infraestructura, layouts, paginas y componentes compartidos.

```text
frontend/
├── public/
│   ├── favicon.ico
│   └── i18n/
│       └── es.json
├── src/
│   ├── app/
│   │   ├── core/
│   │   │   ├── guards/
│   │   │   │   ├── auth.guard.ts
│   │   │   │   ├── guest.guard.ts
│   │   │   │   └── role.guard.ts
│   │   │   ├── i18n/
│   │   │   │   └── transloco-http-loader.ts
│   │   │   ├── interceptors/
│   │   │   │   ├── api-error.interceptor.ts
│   │   │   │   └── auth.interceptor.ts
│   │   │   ├── models/
│   │   │   │   ├── api.models.ts
│   │   │   │   ├── auth.models.ts
│   │   │   │   ├── store.models.ts
│   │   │   │   └── user.models.ts
│   │   │   └── services/
│   │   │       ├── api-error.service.ts
│   │   │       ├── auth-session.service.ts
│   │   │       ├── auth.service.ts
│   │   │       ├── catalog.service.ts
│   │   │       ├── inventory.service.ts
│   │   │       ├── notification.service.ts
│   │   │       ├── product.service.ts
│   │   │       ├── sale.service.ts
│   │   │       ├── stock-entry.service.ts
│   │   │       ├── supplier.service.ts
│   │   │       └── user.service.ts
│   │   ├── layouts/
│   │   │   ├── app-shell/
│   │   │   └── public-layout/
│   │   ├── pages/
│   │   │   ├── auth/
│   │   │   │   ├── login/
│   │   │   │   ├── login-verify/
│   │   │   │   ├── recovery-request/
│   │   │   │   └── recovery-reset/
│   │   │   ├── protected/
│   │   │   │   ├── admin/
│   │   │   │   ├── alerts/
│   │   │   │   ├── dashboard/
│   │   │   │   ├── inventory-movements/
│   │   │   │   ├── products/
│   │   │   │   ├── sales/
│   │   │   │   ├── security/
│   │   │   │   ├── stock-entries/
│   │   │   │   ├── suppliers/
│   │   │   │   └── users/
│   │   │   └── unauthorized/
│   │   ├── shared/
│   │   │   └── components/
│   │   │       ├── brand-mark/
│   │   │       ├── field-error/
│   │   │       ├── form-feedback/
│   │   │       └── page-heading/
│   │   ├── app.config.ts
│   │   ├── app.routes.ts
│   │   ├── app.spec.ts
│   │   └── app.ts
│   ├── environments/
│   │   ├── environment.ts
│   │   └── environment.production.ts
│   ├── index.html
│   ├── main.ts
│   ├── styles.scss
│   └── tailwind.css
├── angular.json
├── package.json
├── package-lock.json
└── proxy.conf.json
```

## Servicios y consumo de la API

Los servicios del frontend centralizan el acceso HTTP a cada modulo del backend.

| Servicio | Recurso base |
| --- | --- |
| `AuthService` | `/api/v1/auth` |
| `CatalogService` | `/api/v1/catalog` |
| `ProductService` | `/api/v1/products` |
| `InventoryService` | `/api/v1/inventory` |
| `NotificationService` | `/api/v1/notifications` |
| `SaleService` | `/api/v1/sales` |
| `SupplierService` | `/api/v1/suppliers` |
| `StockEntryService` | `/api/v1/stock-entries` |
| `UserService` | `/api/v1/users` |

Entre las operaciones consumidas actualmente se encuentran:

- Inicio de sesion y verificacion OTP.
- Recuperacion y cambio de contrasena.
- Activacion y desactivacion de 2FA.
- Consulta del usuario autenticado.
- Consulta de categorias y unidades de medida.
- Busqueda, creacion, edicion y cambio de estado de productos.
- Ajustes y consulta de movimientos de inventario.
- Consulta de notificaciones y marcado de alertas como leidas.
- Registro y consulta de ventas.
- Administracion de proveedores.
- Registro y consulta de entradas de mercaderia.
- Administracion de usuarios y estado de acceso.

## Funcionalidades implementadas

### Autenticacion

- Inicio de sesion con correo y contrasena.
- Verificacion OTP cuando la cuenta tiene 2FA activo.
- Recuperacion de contrasena mediante correo y OTP.
- Restauracion de sesion al recargar la aplicacion si el token sigue vigente.
- Cierre de sesion local.

### Seguridad de cuenta

- Cambio de contrasena.
- Activacion de autenticacion de dos factores.
- Desactivacion de autenticacion de dos factores.
- Confirmacion de los cambios de 2FA mediante OTP.

### Productos e inventario

- Listado paginado de productos.
- Busqueda de productos.
- Filtro por categoria.
- Filtro por estado de inventario.
- Visualizacion de precios y existencias.
- Creacion y edicion de productos para `ADMIN`.
- Activacion o desactivacion de productos para `ADMIN`.
- Ajuste manual de existencias para `ADMIN`.
- Consulta de categorias y unidades de medida desde el catalogo.

### Ventas en efectivo

- Registro de ventas para `ADMIN` y `EMPLOYEE`.
- Busqueda de productos disponibles.
- Validacion de existencias antes de agregar productos a la venta.
- Soporte para unidades que permiten o no cantidades decimales.
- Calculo automatico del total.
- Ingreso de efectivo recibido.
- Calculo automatico del cambio.
- Consulta paginada del historial de ventas.
- Filtros por referencia y fechas.

### Proveedores

Disponible para `ADMIN`:

- Listado y busqueda de proveedores.
- Creacion de proveedores.
- Edicion de proveedores.
- Activacion y desactivacion de proveedores.
- Paginacion y filtros.

### Entradas de mercaderia

Disponible para `ADMIN`:

- Registro de entradas provenientes de proveedores.
- Seleccion de productos.
- Cantidad y costo unitario por producto.
- Calculo de totales de la entrada.
- Consulta del historial de entradas.
- Filtros por proveedor, fechas y referencia.

### Alertas de inventario

Disponible para `ADMIN`:

- Consulta de alertas de productos con stock bajo o agotado.
- Consulta paginada.
- Marcado de notificaciones como leidas.

### Movimientos de inventario

Disponible para `ADMIN`:

- Consulta del historial de movimientos.
- Trazabilidad de entradas, ventas y ajustes.
- Identificacion del producto y del usuario relacionado con el movimiento.
- Paginacion y filtros.

### Usuarios

Disponible para `ADMIN`:

- Listado paginado de usuarios.
- Busqueda y filtros por rol y estado.
- Creacion de cuentas.
- Edicion de cuentas.
- Activacion y desactivacion de usuarios.
- Manejo de los roles `ADMIN` y `EMPLOYEE`.

## Rutas y autorizacion

### Rutas publicas

- `/auth/login`: inicio de sesion con correo y contrasena.
- `/auth/login/verify`: verificacion del OTP de inicio de sesion cuando 2FA esta activo.
- `/auth/recovery`: solicitud de recuperacion de contrasena.
- `/auth/recovery/reset`: verificacion del OTP y establecimiento de una nueva contrasena.

Una sesion autenticada no puede permanecer en las rutas `/auth/**`. `guestGuard` y `guestChildGuard` redirigen al usuario hacia `/app/dashboard`.

### Rutas protegidas

| Ruta | Acceso | Funcion |
| --- | --- | --- |
| `/app/dashboard` | Usuario autenticado | Resumen de cuenta y estado de seguridad |
| `/app/security` | Usuario autenticado | Cambio de contrasena y administracion de 2FA |
| `/app/products` | Usuario autenticado | Consulta de productos; las acciones de mantenimiento se muestran al `ADMIN` |
| `/app/sales` | `ADMIN`, `EMPLOYEE` | Registro y consulta de ventas en efectivo |
| `/app/suppliers` | `ADMIN` | Administracion de proveedores |
| `/app/stock-entries` | `ADMIN` | Registro y consulta de entradas de mercaderia |
| `/app/inventory/movements` | `ADMIN` | Historial de movimientos de inventario |
| `/app/alerts` | `ADMIN` | Alertas y notificaciones de inventario |
| `/app/users` | `ADMIN` | Administracion de usuarios y empleados |
| `/app/admin` | `ADMIN` | Verificacion de acceso administrativo contra la API |
| `/unauthorized` | Usuario autenticado | Pantalla de acceso denegado por rol |

El `authGuard` protege el shell principal y sus rutas hijas. `roleGuard` valida los roles declarados en los metadatos de las rutas y redirige a `/unauthorized` cuando el usuario autenticado no posee el rol requerido.

La navegacion lateral tambien se filtra por rol, pero la autorizacion definitiva debe mantenerse igualmente en el backend.

## Manejo de errores

El frontend admite respuestas de error con la estructura `ProblemDetail`, incluyendo campos como:

```text
type
title
status
detail
instance
code
```

`ApiErrorService` contiene mensajes de respaldo en espanol para errores conocidos, entre ellos credenciales invalidas, OTP invalido, sesion vencida, cuenta deshabilitada, falta de permisos y errores de validacion.

Los mensajes globales se presentan mediante Toast de PrimeNG.

## Flujo general de la aplicacion

```text
Usuario
   |
   v
Angular 21
   |
   | HTTP / JSON
   v
/api/v1
   |
   v
Spring Boot
   |
   v
PostgreSQL
```

En desarrollo, Angular utiliza `proxy.conf.json` para redirigir `/api` al backend local en el puerto `8090`.

## Notas de desarrollo

- El proyecto usa componentes standalone; no depende de un `AppModule` tradicional.
- Las paginas principales se cargan de forma diferida mediante `loadComponent`.
- Los formularios utilizan Reactive Forms.
- La interfaz usa Signals de Angular para manejar estado local en varias paginas.
- La comunicacion asincrona utiliza RxJS.
- El tema visual principal se define mediante variables globales en `src/styles.scss` y estilos SCSS por componente.
- Los textos generales de autenticacion, navegacion, dashboard y varios modulos se encuentran centralizados en Transloco.
- No se debe almacenar informacion sensible adicional en `localStorage` ni incluir secretos del backend en el codigo del frontend.
