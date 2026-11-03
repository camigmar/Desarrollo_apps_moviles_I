// Mi primera app en Kotlin - Sistema de pedidos de una pizzería

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

// Un producto de la carta con su receta (cantidad de cada ingrediente por unidad)
class Producto(val nombre: String, val precio: Double, val ingredientes: Map<String, Int>) {

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

// Un pedido de un cliente con los productos que pidió
// Las notas son opcionales: si no se indican quedan en null
class Pedido(val cliente: Cliente, val productos: List<Producto>, val notas: String? = null) {

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

// Suma unidades de un ingrediente al inventario; la cantidad debe ser positiva
fun reabastecerInventario(inventario: MutableMap<String, Int>, ingrediente: String, cantidad: Int) {
    require(cantidad > 0) { "La cantidad a reabastecer debe ser mayor a 0 (se recibió $cantidad)" }
    inventario[ingrediente] = (inventario[ingrediente] ?: 0) + cantidad
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

    // Cliente real con los datos ya validados
    val cliente = Cliente(nombre, telefono, correo, direccion)

    // Leer cantidad de pizzas: se repite la pregunta hasta que sea válida
    var cantidadValida = false
    while (!cantidadValida) {
        println("\n¿Cuántas pizzas ${productoPepperoni.nombre} desea? (precio unitario: $${productoPepperoni.precio})")
        val entradaCantidad = readLine()

        // Si ya no hay entrada (fin de archivo) se evita un ciclo infinito
        if (entradaCantidad == null) {
            println("No se recibió la cantidad, se asigna 1 pizza.")
            cantidadPizzas = 1
            break
        }

        try {
            val cantidadLeida = entradaCantidad.trim().toInt()
            require(cantidadLeida >= 1) { "Error: la cantidad debe ser al menos 1." }
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
    val pedidoCliente = Pedido(cliente, List(cantidadPizzas) { productoPepperoni }, notasPedido)

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
    println("  Notas:      ${pedidoCliente.notas ?: "Sin notas"}")
    // ?. devuelve null si no hay notas, y ?: lo reemplaza por 0
    println("  Largo nota: ${pedidoCliente.notas?.length ?: 0} caracteres")
    println("  Repartidor: $textoRepartidor")
    println("========================================")

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

    // ---------- 6. Reporte de pedidos del día ----------

    // Otras pizzas de la carta, solo para los pedidos de ejemplo
    val productoNapolitana = Producto("Napolitana", 9490.0, mapOf("masa" to 1, "salsa de tomate" to 1, "queso" to 2, "tomate" to 2))
    val productoHawaiana = Producto("Hawaiana", 9490.0, mapOf("masa" to 1, "salsa de tomate" to 1, "queso" to 2, "piña" to 2, "jamón" to 2))
    val productoVegetariana = Producto("Vegetariana", 9490.0, mapOf("masa" to 1, "salsa de tomate" to 1, "queso" to 2, "verduras" to 3))
    val productoMargarita = Producto("Margarita", 8490.0, mapOf("masa" to 1, "salsa de tomate" to 1, "queso" to 2, "albahaca" to 1))

    // Pedidos de ejemplo del día
    val pedidosDelDia = arrayOf(
        Pedido(Cliente("María Soto", "987654321", "maria.soto@correo.cl", "Los Aromos 123"), listOf(productoPepperoni, productoPepperoni)),
        Pedido(Cliente("Juan Pérez", "976543210", "juan.perez@correo.cl", "Av. Matta 456"), listOf(productoNapolitana)),
        Pedido(Cliente("Carla Rojas", "965432109", "carla.rojas@correo.cl", "Pasaje Sur 789"), listOf(productoHawaiana, productoHawaiana, productoHawaiana)),
        Pedido(Cliente("Diego Muñoz", "954321098", "diego.munoz@correo.cl", "Calle Norte 321"), listOf(productoVegetariana, productoPepperoni)),
        Pedido(Cliente("Valentina Díaz", "943210987", "valentina.diaz@correo.cl", "Av. Grecia 654"), listOf(productoMargarita, productoMargarita, productoMargarita, productoMargarita))
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
        val productoPrueba = Producto("Pizza de prueba", -100.0, mapOf("masa" to 1))
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
