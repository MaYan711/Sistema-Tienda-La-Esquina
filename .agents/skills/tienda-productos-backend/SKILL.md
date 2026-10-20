---
name: tienda-productos-backend
description: Skill para desarrollar y revisar el modulo de productos e inventario del Sistema Tienda La Esquina.
---

# Tienda La Esquina - Productos e Inventario

## Objetivo

Apoyar el desarrollo del backend del modulo de productos e inventario del Sistema Tienda La Esquina.

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

Respetar la estructura existente:

- application
- config
- domain
- exception
- security
- web

No crear una arquitectura diferente sin necesidad.

## Roles

Existen solamente:

- ADMIN
- EMPLOYEE

### ADMIN

Puede:

- crear productos
- editar productos
- activar o desactivar productos
- modificar precios
- administrar categorias
- administrar unidades de medida
- ajustar inventario
- consultar movimientos
- consultar alertas

### EMPLOYEE

Puede:

- consultar productos
- buscar productos
- consultar precios
- consultar existencias

No puede modificar productos ni ajustar inventario.

Los permisos deben validarse siempre en backend.

## Productos

Cada producto maneja:

- codigo
- nombre
- descripcion
- imagen
- categoria
- unidad de medida
- precio de compra
- precio de venta
- existencia actual
- existencia minima
- estado activo

El codigo de producto no puede duplicarse.

No permitir:

- precios negativos
- existencias negativas
- existencias minimas negativas
- categorias inexistentes
- unidades inexistentes

## Estado de inventario

El estado no debe almacenarse manualmente.

Calcularlo de esta manera:

- AVAILABLE: currentStock > minimumStock
- LOW_STOCK: currentStock > 0 y currentStock <= minimumStock
- OUT_OF_STOCK: currentStock = 0

## Eliminacion

No eliminar fisicamente productos.

Utilizar desactivacion logica mediante active=false.

Esto permite conservar el historial de ventas, entradas y movimientos.

## Manejo de stock

No modificar currentStock directamente desde la edicion normal de un producto.

El stock solamente puede modificarse mediante:

- entrada de mercaderia
- venta
- ajuste autorizado

## Ajustes de inventario

Cada ajuste debe registrar:

- producto
- stock anterior
- stock nuevo
- diferencia
- motivo
- usuario responsable
- fecha y hora

No permitir que el nuevo stock sea negativo.

Cada ajuste debe generar tambien un movimiento de inventario.

## Movimientos

Registrar movimientos originados por:

- PURCHASE
- SALE
- ADJUSTMENT

Los movimientos deben conservar trazabilidad.

## Alertas

Generar alertas cuando:

- stock <= minimumStock y stock > 0: LOW_STOCK
- stock = 0: OUT_OF_STOCK

Evitar crear alertas duplicadas pendientes para el mismo producto y tipo.

Cuando el producto vuelva a tener stock suficiente, la alerta puede resolverse.

## API REST

Utilizar:

- DTOs para requests
- DTOs para responses
- validaciones Jakarta
- excepciones controladas
- codigos HTTP correctos
- paginacion
- filtros
- Swagger / OpenAPI

## Productos API

Debe soportar:

- listar productos
- obtener producto por id
- buscar por codigo o nombre
- filtrar por categoria
- filtrar por activo
- filtrar por estado de inventario
- paginacion
- ordenamiento
- crear
- editar
- activar
- desactivar

## Seguridad

Utilizar:

ADMIN:

@PreAuthorize("hasRole('ADMIN')")

ADMIN y EMPLOYEE:

@PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")

No confiar solamente en restricciones del frontend.

## Base de datos

Todo cambio estructural debe hacerse mediante Flyway.

No modificar migraciones que ya hayan sido ejecutadas.

Crear una nueva version cuando el esquema necesite evolucionar.

## Pruebas

Antes de dar una funcionalidad por terminada:

- verificar compilacion
- probar en Swagger
- probar con ADMIN
- probar con EMPLOYEE
- probar datos validos
- probar datos invalidos
- probar productos duplicados
- probar stock negativo
- probar LOW_STOCK
- probar OUT_OF_STOCK
- revisar codigos HTTP

## Restricciones actuales

No implementar por ahora:

- lotes
- fechas de vencimiento
- alertas de vencimiento

Estas funcionalidades quedaron fuera del alcance inicial del proyecto.

## Regla principal

No modificar autenticacion, 2FA o recuperacion de contrasena salvo que sea estrictamente necesario para integrar este modulo.
