// Mi primera app en Kotlin - Sistema de pedidos de una pizzería

// Clase temporal para los repartidores; se reemplazará por las clases reales más adelante
data class RepartidorTemp(val nombre: String, val distanciaKm: Double, val disponible: Boolean)

// Un producto de la carta con su receta (cantidad de cada ingrediente por unidad)
class Producto(val nombre: String, val precio: Double, val ingredientes: Map<String, Int>) {

    // Descripción con nombre, precio e ingredientes
    fun detallarProducto(): String {
        val textoIngredientes = ingredientes.map { "${it.key} x${it.value}" }.joinToString(", ")
        return "$nombre - $$precio | Ingredientes: $textoIngredientes"
    }
}

// Un pedido de un cliente con los productos que pidió
class Pedido(val cliente: String, val productos: List<Producto>) {

    // Solo se cambia con cambiarEstado() para que siempre sea un estado válido
    var estado: String = "En preparación"
        private set

    // Suma de los precios de todos los productos
    fun calcularSubtotal(): Double {
        return productos.sumOf { it.precio }
    }

    // Total con descuento e impuesto, usando las funciones existentes
    fun calcularTotal(): Double {
        val subtotal = calcularSubtotal()
        val descuento = calcularDescuento(subtotal, productos.size)
        val impuesto = calcularImpuesto(subtotal - descuento)
        return calcularTotal(subtotal, descuento, impuesto)
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
        "pepperoni" to 10
    )

    // Receta: unidades de cada ingrediente que usa UNA pizza Pepperoni
    val recetaPepperoni = mapOf(
        "masa" to 1,
        "salsa de tomate" to 1,
        "queso" to 2,
        "pepperoni" to 3
    )

    // Pizza que vende el programa
    val productoPepperoni = Producto("Pepperoni", 8990.0, recetaPepperoni)

    // Datos del pedido
    val idPedido: Int = 1001
    var cantidadPizzas: Int = 1
    var totalPedido: Double = 0.0
    var estadoPedido: String = ""

    // Repartidores con su distancia al local
    val repartidores = listOf(
        RepartidorTemp("Ana", 3.5, true),
        RepartidorTemp("Pedro", 1.2, false),
        RepartidorTemp("Luis", 2.0, true),
        RepartidorTemp("Sofía", 4.8, true)
    )
    var repartidorAsignado: String = "Sin asignar"

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

    // Leer teléfono
    println("Ingrese su teléfono (solo dígitos, entre 8 y 12):")
    val entradaTelefono = readLine()
    var textoTelefono: String = ""
    if (entradaTelefono != null) {
        textoTelefono = entradaTelefono.trim()
    }

    // El teléfono es válido si tiene solo dígitos, entre 8 y 12
    val regexTelefono = Regex("^\\d{8,12}$")
    if (regexTelefono.matches(textoTelefono)) {
        telefono = textoTelefono
    } else {
        println("Aviso: teléfono inválido, se asigna valor por defecto.")
        telefono = "(000) 000-0000"
    }

    // Leer correo
    println("Ingrese su correo:")
    val entradaCorreo = readLine()
    var textoCorreo: String = ""
    if (entradaCorreo != null) {
        textoCorreo = entradaCorreo.trim()
    }

    // El correo es válido si tiene la forma usuario@dominio.ext
    val regexCorreo = Regex("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")
    if (regexCorreo.matches(textoCorreo)) {
        correo = textoCorreo
    } else {
        println("Aviso: correo inválido, se asigna valor por defecto.")
        correo = "correo@invalido.com"
    }

    println("\n--- Datos del cliente ---")
    println("Nombre: $nombre")
    println("Dirección: $direccion")
    println("Teléfono: $telefono")
    println("Correo: $correo")

    // Leer cantidad de pizzas
    println("\n¿Cuántas pizzas ${productoPepperoni.nombre} desea? (precio unitario: $${productoPepperoni.precio})")
    val entradaCantidad = readLine()
    var textoCantidad: String = ""
    if (entradaCantidad != null) {
        textoCantidad = entradaCantidad.trim()
    }

    // Si no es un número, queda en 0
    try {
        cantidadPizzas = textoCantidad.toInt()
    } catch (e: NumberFormatException) {
        cantidadPizzas = 0
    }

    // Mínimo 1 pizza
    if (cantidadPizzas < 1) {
        println("Aviso: cantidad inválida, se asigna 1 pizza.")
        cantidadPizzas = 1
    }

    // ---------- 3. Cálculo del total ----------

    // El pedido lleva la pizza repetida tantas veces como pidió el cliente
    val pedidoCliente = Pedido(nombre, List(cantidadPizzas) { productoPepperoni })

    val subtotal: Double = pedidoCliente.calcularSubtotal()
    // Descuento e impuesto se calculan aparte solo para mostrarlos en el detalle
    val descuento: Double = calcularDescuento(subtotal, cantidadPizzas)
    // El IVA se calcula sobre el monto ya descontado
    val impuesto: Double = calcularImpuesto(subtotal - descuento)
    totalPedido = pedidoCliente.calcularTotal()

    println("\n--- Detalle del pago ---")
    println("Subtotal: $$subtotal")
    println("Descuento: $$descuento")
    println("Impuesto (19%): $$impuesto")
    println("Total: $$totalPedido")

    // ---------- 4. Verificación de stock y repartidor ----------

    // Ingredientes que necesita el pedido completo
    val necesarios = calcularIngredientesNecesarios(recetaPepperoni, cantidadPizzas)

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
            repartidorAsignado = repartidorCercano.nombre
            println("Repartidor asignado: ${repartidorCercano.nombre} (a ${repartidorCercano.distanciaKm} km)")
        } else {
            repartidorAsignado = "Sin repartidores disponibles"
        }
    } else {
        repartidorAsignado = "No asignado (pedido rechazado)"
    }

    // ---------- 5. Resumen final ----------

    println("\n========== RESUMEN DEL PEDIDO ==========")
    println("Pedido N°: $idPedido")
    println("\n[Cliente]")
    println("  Nombre:    ${nombre.uppercase()}")
    println("  Dirección: $direccion")
    println("  Teléfono:  $telefono")
    println("  Correo:    $correo")
    println("\n[Pizza]")
    println("  ${productoPepperoni.detallarProducto()}")
    println("\n[Pedido]")
    println("  Cantidad:   $cantidadPizzas")
    println("  Subtotal:   $${pedidoCliente.calcularSubtotal()}")
    println("  Impuesto (19%): $$impuesto")
    println("  Total:      $$totalPedido (descuento: $$descuento)")
    println("  Estado:     $estadoPedido")
    println("  Repartidor: $repartidorAsignado")
    println("========================================")

    // ---------- 6. Reporte de pedidos del día ----------

    // Otras pizzas de la carta, solo para los pedidos de ejemplo
    val productoNapolitana = Producto("Napolitana", 9490.0, mapOf("masa" to 1, "salsa de tomate" to 1, "queso" to 2, "tomate" to 2))
    val productoHawaiana = Producto("Hawaiana", 9490.0, mapOf("masa" to 1, "salsa de tomate" to 1, "queso" to 2, "piña" to 2, "jamón" to 2))
    val productoVegetariana = Producto("Vegetariana", 9490.0, mapOf("masa" to 1, "salsa de tomate" to 1, "queso" to 2, "verduras" to 3))
    val productoMargarita = Producto("Margarita", 8490.0, mapOf("masa" to 1, "salsa de tomate" to 1, "queso" to 2, "albahaca" to 1))

    // Pedidos de ejemplo del día
    val pedidosDelDia = arrayOf(
        Pedido("María Soto", listOf(productoPepperoni, productoPepperoni)),
        Pedido("Juan Pérez", listOf(productoNapolitana)),
        Pedido("Carla Rojas", listOf(productoHawaiana, productoHawaiana, productoHawaiana)),
        Pedido("Diego Muñoz", listOf(productoVegetariana, productoPepperoni)),
        Pedido("Valentina Díaz", listOf(productoMargarita, productoMargarita, productoMargarita, productoMargarita))
    )

    // Todos parten "En preparación"; se actualizan los que ya avanzaron
    pedidosDelDia[1].cambiarEstado("Listo")
    pedidosDelDia[2].cambiarEstado("En camino")
    pedidosDelDia[4].cambiarEstado("Listo")

    // Reporte: una línea por pedido
    println("\n========== REPORTE DE PEDIDOS DEL DÍA ==========")
    for (pedido in pedidosDelDia) {
        println("Cliente: ${pedido.cliente} | Pizzas: ${pedido.resumirProductos()} | Total: $${pedido.calcularTotal()} | Estado: ${pedido.estado}")
    }

    // Separar pedidos completados y pendientes
    val pedidosCompletados = pedidosDelDia.filter { it.estado == "Listo" || it.estado == "En camino" }
    val pedidosPendientes = pedidosDelDia.filter { it.estado == "En preparación" }

    println("\nCompletados (${pedidosCompletados.size}):")
    for (pedido in pedidosCompletados) {
        println("- ${pedido.cliente}: ${pedido.resumirProductos()} (${pedido.estado})")
    }

    println("\nPendientes (${pedidosPendientes.size}):")
    for (pedido in pedidosPendientes) {
        println("- ${pedido.cliente}: ${pedido.resumirProductos()} (${pedido.estado})")
    }
    println("================================================")
}
