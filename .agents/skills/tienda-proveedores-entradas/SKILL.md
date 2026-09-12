---
name: tienda-proveedores-entradas
description: Skill para desarrollar y revisar proveedores y entradas de mercaderia del Sistema Tienda La Esquina.
---

# Tienda La Esquina - Proveedores y Entradas

## Tecnologias

- Java 21
- Spring Boot 4.1
- PostgreSQL
- Flyway
- Spring Security
- JWT
- Swagger OpenAPI
- Gradle

## Arquitectura

Respetar:

- application
- config
- domain
- exception
- security
- web

No reemplazar la arquitectura existente.

## Roles

El modulo administrativo de proveedores y entradas se implementa para ADMIN.

Usar:

@PreAuthorize("hasRole('ADMIN')")

No confiar solamente en controles del frontend.

## Proveedores

Permitir:

- listar
- buscar
- consultar por id
- crear
- editar
- activar
- desactivar

No eliminar proveedores fisicamente.

Conservar historial relacionado.

Nombre obligatorio.

Buscar al menos por:
- nombre
- NIT
- telefono
- correo cuando exista

Usar paginacion y ordenamiento.

## Entradas de mercaderia

Una entrada debe registrar:

- proveedor
- fecha
- referencia o factura cuando exista
- productos
- cantidad por producto
- costo unitario
- subtotal
- total
- usuario responsable
- fecha de creacion

La entrada debe contener al menos un producto.

No permitir cantidades <= 0.

No permitir costos <= 0.

Respetar cantidades decimales solamente cuando la unidad del producto las permita.

No permitir productos duplicados dentro de la misma entrada.

El total debe ser calculado por backend.

No confiar en totales enviados por frontend.

## Usuario responsable

Obtener exclusivamente desde JWT / Spring Security.

Nunca recibir adjustedBy, createdBy o userId desde el request.

## Inventario

Al confirmar una entrada:

- obtener cada producto con bloqueo de escritura
- guardar stock anterior
- sumar la cantidad recibida
- actualizar currentStock
- generar InventoryMovement
- movementType = STOCK_ENTRY
- sourceType = STOCK_ENTRY
- sourceId = id de la entrada
- guardar stock anterior y posterior

Toda la operacion debe estar en una unica transaccion.

Si falla un item debe hacerse rollback completo.

## Precio de compra

Evaluar la estructura actual.

Si el dominio lo permite de forma segura, actualizar purchasePrice
del producto usando el ultimo costo unitario recibido.

El costo historico de cada entrada nunca debe alterarse.

## Entradas confirmadas

No editar ni eliminar entradas confirmadas.

Para corregir diferencias posteriores utilizar el mecanismo de ajuste
de inventario existente.

## Historial

Permitir consultar entradas por:

- proveedor
- fecha inicial
- fecha final
- usuario responsable
- referencia si existe

Permitir consultar el detalle completo de una entrada.

## Resumen

Proporcionar los datos necesarios para:

- proveedores activos
- entradas/compras del mes actual
- monto comprado durante el mes actual

## Auditoria

Registrar:

- SUPPLIER_CREATED
- SUPPLIER_UPDATED
- SUPPLIER_ACTIVATED
- SUPPLIER_DEACTIVATED
- STOCK_ENTRY_CREATED

Guardar actor, entidad, resumen y datos relevantes.

## Seguridad

No modificar:

- autenticacion
- 2FA
- recuperacion de contrasena
- productos salvo integracion estrictamente necesaria
- usuarios

## Base de datos

No modificar migraciones ya ejecutadas.

Crear una nueva migracion solo si el esquema actual realmente carece
de una estructura necesaria.

## API

Usar:

- DTOs
- Jakarta Validation
- ProblemDetail
- Swagger
- paginacion
- filtros seguros
- lista blanca para sortBy

## Pruebas

Validar:

- CRUD logico de proveedores
- busqueda
- proveedor inexistente
- proveedor inactivo
- entrada exitosa
- entrada sin items
- cantidad cero o negativa
- costo cero o negativo
- producto inexistente
- producto inactivo
- decimales no permitidos
- producto repetido
- total calculado correctamente
- stock aumentado
- STOCK_ENTRY generado
- actor tomado del JWT
- rollback completo
- filtros del historial
- seguridad ADMIN
- auditoria

No hacer commit ni push automaticamente.
