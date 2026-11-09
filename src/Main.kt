// Mi primera app en Kotlin - Sistema de pedidos de una pizzería

import java.time.LocalDate
import java.util.Locale
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties

// Cantidad de pizzas permitida por pedido (mínimo y máximo)
val RANGO_CANTIDAD = 1..100

// Período de promoción (desde y hasta, ambos incluidos)
val RANGO_PROMOCION = LocalDate.of(2026, 11, 1)..LocalDate.of(2026, 11, 30)

// Descuento extra durante la promoción: 10%
const val DESCUENTO_PROMOCION = 0.10

// Indica si una fecha cae dentro del período de promoción
fun estaEnPromocion(fecha: LocalDate): Boolean {
    return fecha in RANGO_PROMOCION
}

// Clase base para cualquier persona del sistema
open class Usuario(val nombre: String, val contacto: String) {

    open fun presentarse(): String {
        return "Soy $nombre, mi contacto es $contacto"
    }
}

// Cliente: además del contacto tiene correo y dirección
class Cliente(nombre: String, telefono: String, val correo: String, val direccion: String) : Usuario(nombre, telefono) {

    override fun presentarse(): String {
        return "Soy el cliente $nombre | Teléfono: $contacto | Correo: $correo | Dirección: $direccion"
    }
}

// Repartidor: tiene distancia al local y si está disponible
class Repartidor(nombre: String, telefono: String, val distanciaKm: Double, val disponible: Boolean) : Usuario(nombre, telefono) {

    override fun presentarse(): String {
        return "Soy el repartidor $nombre | Teléfono: $contacto | Estoy a $distanciaKm km"
    }

    fun entregarPedido(pedido: Pedido): String {
        return "$nombre entrega el pedido de ${pedido.cliente.nombre} en ${pedido.cliente.direccion}"
    }
}

// Tipos de producto que puede tener la carta
enum class Categoria { PIZZA, BEBIDA, INGREDIENTE }

// Un producto de la carta con su receta (cantidad de cada ingrediente por unidad) y su categoría
class Producto(val nombre: String, val precio: Double, val ingredientes: Map<String, Int>, val categoria: Categoria) {

    // No se permite crear un producto con precio 0 o negativo
    init {
        require(precio > 0) { "El precio debe ser mayor a 0" }
    }

    // Descripción con nombre, precio e ingredientes
    fun detallarProducto(): String {
        val textoIngredientes = ingredientes.map { "${it.key} x${it.value}" }.joinToString(", ")
        return "$nombre - $$precio | Ingredientes: $textoIngredientes"
    }
}

// Marca un producto de la carta que puede entrar en promociones
// Se puede leer en tiempo de ejecución (reflection)
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Promocionable(val motivo: String = "")

// Pizzas de la carta, cada una con su receta (unidades por UNA pizza)
object Carta {
    @Promocionable(motivo = "Más vendida")
    val pepperoni = Producto("Pepperoni", 8990.0, mapOf("masa" to 1, "salsa de tomate" to 1, "queso" to 2, "pepperoni" to 3), Categoria.PIZZA)

    val napolitana = Producto("Napolitana", 9490.0, mapOf("masa" to 1, "salsa de tomate" to 1, "queso" to 2, "tomate" to 2), Categoria.PIZZA)

    val hawaiana = Producto("Hawaiana", 9490.0, mapOf("masa" to 1, "salsa de tomate" to 1, "queso" to 2, "piña" to 2, "jamón" to 2), Categoria.PIZZA)

    @Promocionable(motivo = "Opción saludable")
    val vegetariana = Producto("Vegetariana", 9490.0, mapOf("masa" to 1, "salsa de tomate" to 1, "queso" to 2, "verduras" to 3), Categoria.PIZZA)

    @Promocionable(motivo = "La más económica")
    val margarita = Producto("Margarita", 8490.0, mapOf("masa" to 1, "salsa de tomate" to 1, "queso" to 2, "albahaca" to 1), Categoria.PIZZA)
}

// Bebidas de la carta: no tienen receta, por eso el mapa de ingredientes va vacío
object Bebidas {
    @Promocionable(motivo = "Ideal para compartir")
    val bebida500 = Producto("Bebida 500 ml", 1490.0, emptyMap(), Categoria.BEBIDA)

    @Promocionable(motivo = "Fruta de temporada")
    val jugoNatural = Producto("Jugo natural", 2490.0, emptyMap(), Categoria.BEBIDA)

    val aguaMineral = Producto("Agua mineral", 1190.0, emptyMap(), Categoria.BEBIDA)
}

// Productos marcados con @Promocionable dentro de la fuente indicada (por ejemplo Carta o Bebidas)
// Se buscan con reflection: se revisa cada propiedad y se lee su valor con el getter
fun productosPromocionables(fuente: Any): List<Producto> {
    return fuente::class.memberProperties
        .filter { it.hasAnnotation<Promocionable>() }
        .mapNotNull { it.getter.call(fuente) as? Producto }
}

// Describe una clase con reflection: nombre, propiedades y funciones declaradas en ella
fun describirClase(clase: KClass<*>): String {
    val descripcion = StringBuilder()
    descripcion.appendLine("Clase: ${clase.simpleName}")

    // Propiedades como "nombre: Tipo", ordenadas por nombre
    // Se quita el paquete del tipo (ej: kotlin.String -> String) para que sea más fácil de leer
    descripcion.appendLine("  Propiedades:")
    for (propiedad in clase.declaredMemberProperties.sortedBy { it.name }) {
        val tipo = propiedad.returnType.toString().replace(Regex("\\b[a-z]\\w*\\."), "")
        descripcion.appendLine("    - ${propiedad.name}: $tipo")
    }

    // Funciones solo con su nombre, ordenadas por nombre
    descripcion.appendLine("  Funciones:")
    val funciones = clase.declaredFunctions.sortedBy { it.name }
    if (funciones.isEmpty()) {
        descripcion.appendLine("    (ninguna)")
    } else {
        for (funcion in funciones) {
            descripcion.appendLine("    - ${funcion.name}()")
        }
    }

    return descripcion.toString().trimEnd()
}

// Un pedido de un cliente con los productos que pidió
// Las notas son opcionales: si no se indican quedan en null
// La fecha, si no se indica, es la de hoy
class Pedido(val cliente: Cliente, val productos: List<Producto>, val notas: String? = null, val fecha: LocalDate = LocalDate.now()) {

    // Solo se cambia con cambiarEstado() para que siempre sea un estado válido
    var estado: String = "En preparación"
        private set

    // Suma de los precios de todos los productos
    fun calcularSubtotal(): Double {
        return productos.sumOf { it.precio }
    }

    // Descuento del pedido: el de cantidad (función existente) más el de promoción si corresponde
    fun calcularDescuento(): Double {
        val descuentoCantidad = calcularDescuento(calcularSubtotal(), productos.size)
        val descuentoPromocion = if (estaEnPromocion(fecha)) calcularSubtotal() * DESCUENTO_PROMOCION else 0.0
        return descuentoCantidad + descuentoPromocion
    }

    // IVA sobre el monto ya descontado, usando la función existente
    fun calcularImpuesto(): Double {
        return calcularImpuesto(calcularSubtotal() - calcularDescuento())
    }

    // Total con descuento e impuesto, usando las funciones existentes
    fun calcularTotal(): Double {
        return calcularTotal(calcularSubtotal(), calcularDescuento(), calcularImpuesto())
    }

    // Cantidad por producto, ejemplo: "2 Pepperoni, 1 Vegetariana"
    fun resumirProductos(): String {
        return productos.groupBy { it.nombre }.map { "${it.value.size} ${it.key}" }.joinToString(", ")
    }

    // Cambia el estado solo si es uno de los permitidos
    fun cambiarEstado(nuevoEstado: String) {
        val estadosValidos = listOf("En preparación", "Listo", "En camino")
        if (nuevoEstado in estadosValidos) {
            estado = nuevoEstado
        } else {
            println("Aviso: estado \"$nuevoEstado\" no válido. El pedido sigue \"$estado\".")
        }
    }
}

// 10% de descuento si son más de 3 pizzas
fun calcularDescuento(subtotal: Double, cantidadPizzas: Int): Double {
    return if (cantidadPizzas > 3) subtotal * 0.10 else 0.0
}

// IVA de 19% sobre la base
fun calcularImpuesto(base: Double): Double {
    return base * 0.19
}

// Total a pagar: subtotal menos descuento más impuesto
fun calcularTotal(subtotal: Double, descuento: Double, impuesto: Double): Double {
    return subtotal - descuento + impuesto
}

// Cantidad total de cada ingrediente para un lote de pizzas
fun calcularIngredientesNecesarios(receta: Map<String, Int>, cantidadPizzas: Int): Map<String, Int> {
    val necesarios = mutableMapOf<String, Int>()
    for ((ingrediente, cantidadPorPizza) in receta) {
        necesarios[ingrediente] = cantidadPorPizza * cantidadPizzas
    }
    return necesarios
}

// Resta del inventario lo que usa el pedido
fun descontarInventario(inventario: MutableMap<String, Int>, necesarios: Map<String, Int>) {
    for ((ingrediente, cantidad) in necesarios) {
        inventario[ingrediente] = (inventario[ingrediente] ?: 0) - cantidad
    }
}

// Suma unidades de un ingrediente al inventario; la cantidad debe ser positiva
fun reabastecerInventario(inventario: MutableMap<String, Int>, ingrediente: String, cantidad: Int) {
    require(cantidad > 0) { "La cantidad a reabastecer debe ser mayor a 0 (se recibió $cantidad)" }
    inventario[ingrediente] = (inventario[ingrediente] ?: 0) + cantidad
}

// Monto en pesos chilenos, con punto de miles y sin decimales. Ejemplo: "$26.970"
fun formatearMonto(monto: Double): String {
    return String.format(Locale.forLanguageTag("es-CL"), "$%,.0f", monto)
}

// Formatea celulares chilenos como "+56 (9) XXXX-XXXX"; devuelve null si no es válido
fun formatearTelefono(telefono: String): String? {
    // Se quita todo lo que no sea dígito (espacios, +, paréntesis, guiones)
    val soloDigitos = telefono.replace(Regex("\\D"), "")

    // 56 opcional, luego el 9 y dos grupos de 4 dígitos
    val regexTelefono = Regex("^(?:56)?(9)(\\d{4})(\\d{4})$")
    val resultado = regexTelefono.matchEntire(soloDigitos) ?: return null

    // Se arma el formato con los grupos capturados (siempre con +56)
    val (nueve, parte1, parte2) = resultado.destructured
    return "+56 ($nueve) $parte1-$parte2"
}

// Resumen completo del pedido listo para imprimir
fun generarResumen(idPedido: Int, pedido: Pedido, estado: String, repartidor: String): String {
    val lineaDoble = "=".repeat(50)
    val lineaSimple = "-".repeat(50)
    val resumen = StringBuilder()

    // Encabezado
    resumen.appendLine(lineaDoble)
    resumen.appendLine("            RESUMEN DEL PEDIDO N° $idPedido")
    resumen.appendLine(lineaDoble)

    // Cliente
    resumen.appendLine("CLIENTE")
    resumen.appendLine("  Nombre:    ${pedido.cliente.nombre}")
    resumen.appendLine("  Teléfono:  ${pedido.cliente.contacto}")
    resumen.appendLine("  Correo:    ${pedido.cliente.correo}")
    resumen.appendLine("  Dirección: ${pedido.cliente.direccion}")
    resumen.appendLine(lineaSimple)

    // Productos: una línea por cada producto distinto
    resumen.appendLine("PRODUCTOS")
    for ((nombreProducto, productosIguales) in pedido.productos.groupBy { it.nombre }) {
        val precioUnitario = productosIguales.first().precio
        val cantidad = productosIguales.size
        val subtotalLinea = precioUnitario * cantidad
        resumen.appendLine("  ${nombreProducto.padEnd(14)} ${formatearMonto(precioUnitario).padStart(9)} x $cantidad = ${formatearMonto(subtotalLinea).padStart(10)}")
    }
    resumen.appendLine(lineaSimple)

    // Totales
    resumen.appendLine("TOTALES")
    resumen.appendLine("  ${"Subtotal:".padEnd(18)}${formatearMonto(pedido.calcularSubtotal()).padStart(12)}")
    resumen.appendLine("  ${"Descuento:".padEnd(18)}${formatearMonto(pedido.calcularDescuento()).padStart(12)}")
    resumen.appendLine("  ${"Impuesto (19%):".padEnd(18)}${formatearMonto(pedido.calcularImpuesto()).padStart(12)}")
    resumen.appendLine("  ${"TOTAL:".padEnd(18)}${formatearMonto(pedido.calcularTotal()).padStart(12)}")
    // Si fue rechazado los montos son solo referenciales
    if (estado.startsWith("Rechazado")) {
        resumen.appendLine("  (Pedido rechazado: este monto NO se cobrará)")
    }
    resumen.appendLine(lineaSimple)

    // Fecha y promoción del pedido
    val textoPromocion = if (estaEnPromocion(pedido.fecha)) {
        "Aplicada (${(DESCUENTO_PROMOCION * 100).toInt()}% de descuento)"
    } else {
        "No aplica"
    }
    resumen.appendLine("  Fecha:      ${pedido.fecha}")
    resumen.appendLine("  Promoción:  $textoPromocion")

    // Estado, repartidor y notas
    resumen.appendLine("  Estado:     $estado")
    resumen.appendLine("  Repartidor: $repartidor")
    resumen.appendLine("  Notas:      ${pedido.notas ?: "Sin notas"}")
    resumen.appendLine(lineaSimple)

    // Pizzas y bebidas que pueden entrar en promociones, ordenadas por nombre
    val promocionables = productosPromocionables(Carta) + productosPromocionables(Bebidas)
    resumen.appendLine("  Promocionables: ${promocionables.map { it.nombre }.sorted().joinToString(", ")}")
    resumen.append(lineaDoble)

    return resumen.toString()
}

fun main() {
    println("===== PIZZERÍA KOTLIN =====")

    // ---------- 1. Variables ----------

    // Datos del cliente
    var nombre: String = ""
    var direccion: String = ""
    var telefono: String = "" // es String porque puede empezar con 0
    var correo: String = ""

    // Inventario de ingredientes (en unidades)
    val inventario = mutableMapOf(
        "masa" to 20,
        "salsa de tomate" to 20,
        "queso" to 20,
        "pepperoni" to 10,
        "tomate" to 20,
        "piña" to 10,
        "jamón" to 10,
        "verduras" to 15,
        "albahaca" to 10
    )

    // Carta que se muestra al cliente para elegir (pizzas del object Carta)
    val carta = listOf(Carta.pepperoni, Carta.napolitana, Carta.hawaiana, Carta.vegetariana, Carta.margarita)
    var pizzaElegida: Producto = carta[0]

    // Datos del pedido
    val idPedido: Int = 1001
    var cantidadPizzas: Int = 1
    var estadoPedido: String = ""

    // Repartidores con su distancia al local
    val repartidores = listOf(
        Repartidor("Ana", "911111111", 3.5, true),
        Repartidor("Pedro", "922222222", 1.2, false),
        Repartidor("Luis", "933333333", 2.0, true),
        Repartidor("Sofía", "944444444", 4.8, true)
    )
    var repartidorAsignado: Repartidor? = null
    var textoRepartidor: String = "Sin asignar" // lo que se muestra en el resumen

    // ---------- 2. Entrada / salida ----------

    // Leer nombre
    println("Ingrese su nombre:")
    val entradaNombre = readLine()
    if (entradaNombre == null || entradaNombre.trim().isEmpty()) {
        nombre = "Sin dato"
    } else {
        nombre = entradaNombre.trim()
    }

    // Leer dirección
    println("Ingrese su dirección:")
    val entradaDireccion = readLine()
    if (entradaDireccion == null || entradaDireccion.trim().isEmpty()) {
        direccion = "Sin dato"
    } else {
        direccion = entradaDireccion.trim()
    }

    // Leer teléfono: se repite la pregunta hasta que sea válido
    var telefonoValido = false
    while (!telefonoValido) {
        println("Ingrese su celular (ej: +56 9 6580 1007):")
        val entradaTelefono = readLine()

        // Si ya no hay entrada (fin de archivo) se evita un ciclo infinito
        if (entradaTelefono == null) {
            println("No se recibió el teléfono, se asigna valor por defecto.")
            telefono = "+56 (9) 0000-0000"
            break
        }

        // Se guarda el teléfono ya formateado
        val telefonoFormateado = formatearTelefono(entradaTelefono)
        if (telefonoFormateado != null) {
            telefono = telefonoFormateado
            telefonoValido = true
        } else {
            println("Error: teléfono inválido, debe ser un celular chileno, por ejemplo +56 9 6580 1007")
        }
    }

    // El correo es válido si tiene la forma usuario@dominio.ext
    val regexCorreo = Regex("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")

    // Leer correo: se repite la pregunta hasta que sea válido
    var correoValido = false
    while (!correoValido) {
        println("Ingrese su correo:")
        val entradaCorreo = readLine()

        // Si ya no hay entrada (fin de archivo) se evita un ciclo infinito
        if (entradaCorreo == null) {
            println("No se recibió el correo, se asigna valor por defecto.")
            correo = "correo@invalido.com"
            break
        }

        val textoCorreo = entradaCorreo.trim()
        if (regexCorreo.matches(textoCorreo)) {
            correo = textoCorreo
            correoValido = true
        } else {
            println("Error: correo inválido, debe tener la forma nombre@dominio.com")
        }
    }

    // Cliente real con los datos ya validados
    val cliente = Cliente(nombre, telefono, correo, direccion)

    // Mostrar la carta numerada
    println("\n--- CARTA ---")
    for ((indice, pizza) in carta.withIndex()) {
        println("${indice + 1}. ${pizza.nombre} - ${formatearMonto(pizza.precio)}")
    }

    // Leer la pizza elegida: se repite la pregunta hasta que la opción sea válida
    var opcionValida = false
    while (!opcionValida) {
        println("Elija una pizza (1 a ${carta.size}):")
        val entradaOpcion = readLine()

        // Si ya no hay entrada (fin de archivo) se elige la primera pizza
        if (entradaOpcion == null) {
            println("No se recibió la opción, se elige ${carta[0].nombre}.")
            pizzaElegida = carta[0]
            break
        }

        // La opción debe ser un número dentro del rango de la carta
        val opcion = entradaOpcion.trim().toIntOrNull()
        if (opcion == null) {
            println("Error: debe ingresar un número, por ejemplo 1.")
        } else if (opcion !in 1..carta.size) {
            println("Error: la opción debe estar entre 1 y ${carta.size}.")
        } else {
            pizzaElegida = carta[opcion - 1]
            opcionValida = true
        }
    }

    // Buscar en Carta la propiedad de la pizza elegida y leer su anotación (reflection)
    val promocion = Carta::class.memberProperties
        .find { it.getter.call(Carta) == pizzaElegida }
        ?.findAnnotation<Promocionable>()
    if (promocion != null) {
        println("La pizza ${pizzaElegida.nombre} es promocionable: ${promocion.motivo}")
    } else {
        println("La pizza ${pizzaElegida.nombre} no es promocionable.")
    }

    // Leer cantidad de pizzas: se repite la pregunta hasta que sea válida
    var cantidadValida = false
    while (!cantidadValida) {
        println("\n¿Cuántas pizzas ${pizzaElegida.nombre} desea? (entre ${RANGO_CANTIDAD.first} y ${RANGO_CANTIDAD.last}, precio unitario: $${pizzaElegida.precio})")
        val entradaCantidad = readLine()

        // Si ya no hay entrada (fin de archivo) se evita un ciclo infinito
        if (entradaCantidad == null) {
            println("No se recibió la cantidad, se asigna 1 pizza.")
            cantidadPizzas = 1
            break
        }

        try {
            val cantidadLeida = entradaCantidad.trim().toInt()
            // La cantidad debe estar dentro del rango permitido
            require(cantidadLeida in RANGO_CANTIDAD) {
                "Error: la cantidad debe estar entre ${RANGO_CANTIDAD.first} y ${RANGO_CANTIDAD.last}."
            }
            cantidadPizzas = cantidadLeida
            cantidadValida = true
        } catch (e: NumberFormatException) {
            // Va primero porque NumberFormatException es un tipo de IllegalArgumentException
            println("Error: debe ingresar un número entero, por ejemplo 2.")
        } catch (e: IllegalArgumentException) {
            println(e.message)
        }
    }

    // Leer notas opcionales: línea vacía o sin entrada quedan en null
    println("\n¿Desea agregar notas al pedido? (Enter para omitir)")
    val notasPedido: String? = readLine()?.trim()?.takeIf { it.isNotEmpty() }

    // Solo se muestra si hay notas
    notasPedido?.let { println("Nota recibida: $it") }

    // ---------- 3. Cálculo del total ----------

    // El pedido lleva la pizza repetida tantas veces como pidió el cliente
    // Los montos (subtotal, descuento, impuesto y total) los calcula el propio pedido
    val pedidoCliente = Pedido(cliente, List(cantidadPizzas) { pizzaElegida }, notasPedido)

    // ---------- 4. Verificación de stock y repartidor ----------

    // Ingredientes que necesita el pedido completo
    val necesarios = calcularIngredientesNecesarios(pizzaElegida.ingredientes, cantidadPizzas)

    // Revisar cada ingrediente necesario y guardar los que no alcanzan
    val ingredientesFaltantes = mutableListOf<String>()
    for ((ingrediente, necesario) in necesarios) {
        val disponible = inventario[ingrediente] ?: 0
        when {
            necesario <= disponible -> println("OK: $ingrediente (necesario: $necesario, disponible: $disponible)")
            else -> ingredientesFaltantes.add(ingrediente)
        }
    }

    val pedidoConfirmado: Boolean = ingredientesFaltantes.isEmpty()

    estadoPedido = if (pedidoConfirmado) {
        // Descontar del inventario lo que usa el pedido
        descontarInventario(inventario, necesarios)
        println("\nPedido confirmado. Inventario restante: $inventario")
        pedidoCliente.estado
    } else {
        println("\nEl pedido no puede completarse. Faltan los siguientes ingredientes:")
        for (ingrediente in ingredientesFaltantes) {
            val necesario = necesarios[ingrediente] ?: 0
            val disponible = inventario[ingrediente] ?: 0
            val faltan = necesario - disponible
            println("- $ingrediente: necesario $necesario, disponible $disponible")
            println("  Pasos para reabastecer:")
            println("  1. Contactar al proveedor.")
            println("  2. Solicitar $faltan unidades de $ingrediente.")
            println("  3. Actualizar el inventario.")
        }
        "Rechazado por falta de ingredientes"
    }

    // Se asigna repartidor solo si el pedido se confirmó
    if (pedidoConfirmado) {
        // El más cercano que esté disponible
        val repartidorCercano = repartidores.sortedBy { it.distanciaKm }.find { it.disponible }
        if (repartidorCercano != null) {
            repartidorAsignado = repartidorCercano
            textoRepartidor = repartidorCercano.nombre
            println("Repartidor asignado: ${repartidorCercano.nombre} (a ${repartidorCercano.distanciaKm} km)")
        } else {
            textoRepartidor = "Sin repartidores disponibles"
        }
    } else {
        textoRepartidor = "No asignado (pedido rechazado)"
    }

    // ---------- 5. Resumen final ----------

    println()
    println(generarResumen(idPedido, pedidoCliente, estadoPedido, textoRepartidor))

    // Notificación solo a clientes registrados (con correo válido) y si el pedido se confirmó
    if (pedidoConfirmado) {
        cliente.correo.takeIf { it != "correo@invalido.com" }?.let {
            println("\nNotificación enviada a $it: su pedido está en preparación.")
        } ?: run {
            println("\nNo se envió notificación: cliente sin correo válido.")
        }
    }

    // Polimorfismo: cada usuario se presenta a su manera
    val usuariosDelPedido = if (repartidorAsignado != null) {
        listOf<Usuario>(cliente, repartidorAsignado)
    } else {
        listOf<Usuario>(cliente)
    }

    println("\n--- Personas del pedido ---")
    for (usuario in usuariosDelPedido) {
        println(usuario.presentarse())
    }

    // Entrega solo si el pedido se confirmó y hay repartidor
    if (pedidoConfirmado && repartidorAsignado != null) {
        println(repartidorAsignado.entregarPedido(pedidoCliente))
    }

    // ---------- Análisis de clases con reflection ----------

    println("\n========== ANÁLISIS DE CLASES (reflection) ==========")
    val clasesAnalizadas = listOf(Cliente::class, Producto::class, Pedido::class, Bebidas::class)
    for (clase in clasesAnalizadas) {
        println(describirClase(clase))
        println("-".repeat(50))
    }

    // ---------- 6. Reporte de pedidos del día ----------

    // Pedidos de ejemplo del día (con las mismas pizzas de la carta)
    // El de María tiene fecha fuera de la promoción, para comparar
    val pedidosDelDia = arrayOf(
        Pedido(Cliente("María Soto", "987654321", "maria.soto@correo.cl", "Los Aromos 123"), listOf(Carta.pepperoni, Carta.pepperoni), fecha = LocalDate.of(2026, 10, 15)),
        Pedido(Cliente("Juan Pérez", "976543210", "juan.perez@correo.cl", "Av. Matta 456"), listOf(Carta.napolitana)),
        Pedido(Cliente("Carla Rojas", "965432109", "carla.rojas@correo.cl", "Pasaje Sur 789"), listOf(Carta.hawaiana, Carta.hawaiana, Carta.hawaiana)),
        Pedido(Cliente("Diego Muñoz", "954321098", "diego.munoz@correo.cl", "Calle Norte 321"), listOf(Carta.vegetariana, Carta.pepperoni)),
        Pedido(Cliente("Valentina Díaz", "943210987", "valentina.diaz@correo.cl", "Av. Grecia 654"), listOf(Carta.margarita, Carta.margarita, Carta.margarita, Carta.margarita))
    )

    // Todos parten "En preparación"; se actualizan los que ya avanzaron
    pedidosDelDia[1].cambiarEstado("Listo")
    pedidosDelDia[2].cambiarEstado("En camino")
    pedidosDelDia[4].cambiarEstado("Listo")

    // Reporte: una línea por pedido
    println("\n========== REPORTE DE PEDIDOS DEL DÍA ==========")
    for (pedido in pedidosDelDia) {
        println("Cliente: ${pedido.cliente.nombre} | Pizzas: ${pedido.resumirProductos()} | Total: $${pedido.calcularTotal()} | Estado: ${pedido.estado}")
    }

    // Separar pedidos completados y pendientes
    val pedidosCompletados = pedidosDelDia.filter { it.estado == "Listo" || it.estado == "En camino" }
    val pedidosPendientes = pedidosDelDia.filter { it.estado == "En preparación" }

    println("\nCompletados (${pedidosCompletados.size}):")
    for (pedido in pedidosCompletados) {
        println("- ${pedido.cliente.nombre}: ${pedido.resumirProductos()} (${pedido.estado})")
    }

    println("\nPendientes (${pedidosPendientes.size}):")
    for (pedido in pedidosPendientes) {
        println("- ${pedido.cliente.nombre}: ${pedido.resumirProductos()} (${pedido.estado})")
    }
    println("================================================")

    // ---------- 7. Demostración de manejo de errores ----------

    println("\n========== DEMOSTRACIÓN DE ERRORES ==========")

    // Precio inválido: el init de Producto lanza IllegalArgumentException
    println("\nCreando un producto con precio -100.0...")
    try {
        val productoPrueba = Producto("Pizza de prueba", -100.0, mapOf("masa" to 1), Categoria.PIZZA)
        println("Producto creado: ${productoPrueba.detallarProducto()}")
    } catch (e: IllegalArgumentException) {
        println("Error al crear el producto: ${e.message}. Corrija el precio e intente de nuevo.")
    } finally {
        println("Validación de precio terminada.")
    }

    // Reabastecimiento sobre una copia, para no alterar el inventario real
    val inventarioPrueba = inventario.toMutableMap()

    println("\nReabasteciendo pepperoni con -5 unidades...")
    try {
        reabastecerInventario(inventarioPrueba, "pepperoni", -5)
        println("Inventario actualizado: $inventarioPrueba")
    } catch (e: IllegalArgumentException) {
        println("Error al reabastecer: ${e.message}.")
    } finally {
        println("Validación de inventario terminada.")
    }

    println("\nReabasteciendo pepperoni con 15 unidades...")
    try {
        reabastecerInventario(inventarioPrueba, "pepperoni", 15)
        println("Inventario actualizado: $inventarioPrueba")
    } catch (e: IllegalArgumentException) {
        println("Error al reabastecer: ${e.message}.")
    } finally {
        println("Validación de inventario terminada.")
    }
    println("=============================================")
}
