// Mi primera app en Kotlin - Sistema de pedidos de una pizzería
fun main() {
    println("===== PIZZERÍA KOTLIN =====")

    // ---------- 1. Variables ----------

    // Datos del cliente
    var nombre: String = ""
    var direccion: String = ""
    var telefono: String = "" // es String porque puede empezar con 0

    // Datos de la pizza
    val nombrePizza: String = "Pepperoni"
    val precioUnitario: Double = 8990.0

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

    // Datos del pedido
    val idPedido: Int = 1001
    var cantidadPizzas: Int = 1
    var totalPedido: Double = 0.0
    var estadoPedido: String = ""

    // Repartidor
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

    // El teléfono es válido si tiene entre 8 y 12 caracteres y es un número
    if (textoTelefono.length >= 8 && textoTelefono.length <= 12 && textoTelefono.toLongOrNull() != null) {
        telefono = textoTelefono
    } else {
        println("Aviso: teléfono inválido, se asigna valor por defecto.")
        telefono = "000000000"
    }

    println("\n--- Datos del cliente ---")
    println("Nombre: $nombre")
    println("Dirección: $direccion")
    println("Teléfono: $telefono")

    // Leer cantidad de pizzas
    println("\n¿Cuántas pizzas $nombrePizza desea? (precio unitario: $$precioUnitario)")
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

    val subtotal: Double = precioUnitario * cantidadPizzas
    // 10% de descuento si son más de 3 pizzas
    val descuento: Double = if (cantidadPizzas > 3) subtotal * 0.10 else 0.0
    totalPedido = subtotal - descuento

    println("\n--- Detalle del pago ---")
    println("Subtotal: $$subtotal")
    println("Descuento: $$descuento")
    println("Total: $$totalPedido")

    // ---------- 4. Verificación de stock y repartidor ----------

    // Revisar cada ingrediente de la receta y guardar los que no alcanzan
    val ingredientesFaltantes = mutableListOf<String>()
    for ((ingrediente, cantidadPorPizza) in recetaPepperoni) {
        val necesario = cantidadPorPizza * cantidadPizzas
        val disponible = inventario[ingrediente] ?: 0
        when {
            necesario <= disponible -> println("OK: $ingrediente (necesario: $necesario, disponible: $disponible)")
            else -> ingredientesFaltantes.add(ingrediente)
        }
    }

    val pedidoConfirmado: Boolean = ingredientesFaltantes.isEmpty()

    estadoPedido = if (pedidoConfirmado) {
        // Descontar del inventario lo que usa el pedido
        for ((ingrediente, cantidadPorPizza) in recetaPepperoni) {
            inventario[ingrediente] = (inventario[ingrediente] ?: 0) - cantidadPorPizza * cantidadPizzas
        }
        println("\nPedido confirmado. Inventario restante: $inventario")
        "Preparación"
    } else {
        println("\nEl pedido no puede completarse. Faltan los siguientes ingredientes:")
        for (ingrediente in ingredientesFaltantes) {
            val necesario = (recetaPepperoni[ingrediente] ?: 0) * cantidadPizzas
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
        repartidorAsignado = when (idPedido % 3) {
            0 -> "Ana"
            1 -> "Pedro"
            else -> "Luis"
        }
    }

    // ---------- 5. Resumen final ----------

    println("\n========== RESUMEN DEL PEDIDO ==========")
    println("Pedido N°: $idPedido")
    println("\n[Cliente]")
    println("  Nombre:    $nombre")
    println("  Dirección: $direccion")
    println("  Teléfono:  $telefono")
    println("\n[Pizza]")
    println("  Nombre: $nombrePizza")
    println("  Precio: $$precioUnitario")
    println("\n[Pedido]")
    println("  Cantidad:   $cantidadPizzas")
    println("  Total:      $$totalPedido (descuento: $$descuento)")
    println("  Estado:     $estadoPedido")
    println("  Repartidor: $repartidorAsignado")
    println("========================================")
}
