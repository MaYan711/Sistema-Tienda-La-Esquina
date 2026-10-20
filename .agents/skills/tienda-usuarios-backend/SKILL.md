---
name: tienda-usuarios-backend
description: Skill para desarrollar y revisar el modulo de usuarios y roles del Sistema Tienda La Esquina.
---

# Tienda La Esquina - Usuarios y Roles

## Tecnologias

- Java 21
- Spring Boot 4.1
- Spring Security
- JWT
- PostgreSQL
- Flyway
- Swagger / OpenAPI
- Gradle

## Arquitectura

Respetar la estructura actual:

- application
- config
- domain
- exception
- security
- web

No reemplazar la arquitectura existente.

## Roles

Solamente existen:

- ADMIN
- EMPLOYEE

## Administracion de usuarios

Solo ADMIN puede:

- listar usuarios
- consultar usuario por id
- crear usuarios
- editar usuarios
- asignar roles
- activar usuarios
- desactivar usuarios

EMPLOYEE no puede administrar cuentas.

## Registro

La creacion de cuentas debe estar controlada por ADMIN.

No debe existir registro publico de nuevas cuentas para usuarios finales.

## Usuario

Cada cuenta debe manejar al menos:

- email
- passwordHash
- role
- verified
- enabled
- twoFactorEnabled
- tokenVersion
- createdAt
- updatedAt

No almacenar contraseñas en texto plano.

## Creacion

Al crear un usuario:

- normalizar email
- validar email unico
- validar password
- validar rol
- usar BCrypt
- establecer verified=true
- establecer enabled=true
- registrar auditoria

## Actualizacion

Permitir modificar:

- email
- role

No cambiar contrasena desde el CRUD general.

La contrasena se administra mediante los flujos de seguridad existentes.

## Desactivacion

No eliminar usuarios fisicamente.

Usar enabled=false.

No permitir que el ADMIN autenticado se desactive a si mismo.

## Cambio de rol

No permitir que el ADMIN autenticado cambie su propio rol de ADMIN a EMPLOYEE.

Solo permitir ADMIN o EMPLOYEE.

## Sesiones

Cuando se cambie:

- rol
- enabled

invalidar sesiones existentes incrementando tokenVersion si el modelo actual lo soporta.

## Auditoria

Registrar:

- creacion de usuario
- modificacion
- cambio de rol
- activacion
- desactivacion

Guardar usuario administrador responsable, fecha y datos relevantes.

## API

Usar:

- DTOs
- Jakarta Validation
- ProblemDetail
- paginacion
- filtros
- Swagger
- codigos HTTP correctos

## Listado

Debe soportar filtros:

- search por email
- role
- enabled

Y:

- page
- size
- sortBy
- direction

No exponer passwordHash.

## Seguridad

Usar:

@PreAuthorize("hasRole('ADMIN')")

El backend debe validar permisos aunque el frontend oculte controles.

## Base de datos

No modificar migraciones ejecutadas.

Crear nueva migracion solo si realmente falta una columna necesaria.

## Pruebas

Validar:

- ADMIN puede listar
- ADMIN puede crear EMPLOYEE
- email duplicado devuelve error
- rol invalido devuelve error
- password debil/invalido devuelve error
- ADMIN puede editar
- ADMIN puede desactivar
- usuario desactivado no puede autenticarse
- ADMIN no puede desactivarse a si mismo
- ADMIN no puede quitarse su propio rol
- EMPLOYEE recibe 403
- sin JWT recibe 401
- passwordHash nunca aparece en responses
- cambios importantes generan auditoria

No modificar productos, inventario, ventas o proveedores.
