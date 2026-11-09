# Pizzería Kotlin

Sistema de pedidos de una pizzería por consola, hecho en Kotlin.
Actividad sumativa **"Creando soluciones con Kotlin"** – Desarrollo App Móviles I.

## Cómo ejecutarlo

1. Abrir la carpeta del proyecto en **IntelliJ IDEA**.
2. Abrir `src/Main.kt` y ejecutar la función `main`.

La librería **kotlin-reflect** viene incluida en la carpeta `lib` y ya está configurada en `PizzeriaApp.iml`.
La librería **kotlin-stdlib** la descarga IntelliJ automáticamente la primera vez que se abre el proyecto,
por lo que en ese momento se necesita conexión a internet.

## Versiones usadas

- IntelliJ IDEA 2026.2.3
- JDK 24
- Kotlin 2.4.20

El proyecto está configurado con JDK 24. Si el equipo tiene otra versión, IntelliJ pedirá elegir un SDK
en **File > Project Structure > Project**.

## Qué hace el programa

- Pide los datos del cliente (nombre, dirección, celular y correo) y los valida.
- Permite hacer **varios pedidos**, eligiendo pizzas de una carta numerada.
- El cliente puede **agregar bebidas** al pedido; el descuento por cantidad cuenta solo las pizzas.
- Calcula subtotal, descuentos (por cantidad y por promoción) e IVA de 19%.
- Revisa el inventario de ingredientes y rechaza el pedido si no alcanza.
- Asigna el repartidor disponible más cercano.
- Termina con un **resumen final de la jornada**.

## Contenidos de la semana 3

| Contenido | Dónde está en Main.kt |
|---|---|
| Regex (correo y teléfono) | `regexCorreo` dentro de `main`, función `formatearTelefono` |
| Ranges (cantidad 1-100 y promoción por fechas) | `RANGO_CANTIDAD`, `RANGO_PROMOCION`, `estaEnPromocion` |
| Anotaciones | `@Promocionable` usada en los objetos `Carta` y `Bebidas` |
| Reflection | `describirClase`, `productosPromocionables` |
| Sobrecarga de operadores | `plus` (+) en `Pedido`, `==` vía `equals` en `Producto` |
| Desestructuración | `component1`, `component2` y `component3` en `Cliente` y `Pedido` |
| Igualdad | `equals` y `hashCode` en `Cliente` y `Producto`, `registrarCliente`, `registrarProducto` |
| Resumen final | `generarResumenFinal` |

## Decisiones a tener en cuenta

- El teléfono se formatea como **celular chileno** `+56 (9) XXXX-XXXX`, en vez del ejemplo genérico de la pauta.
- La **promoción por fechas** va del 1 al 30 de noviembre de 2026 y da 10% de descuento.
- Los **estados del pedido** avanzan en orden: En preparación → Listo → En camino.
