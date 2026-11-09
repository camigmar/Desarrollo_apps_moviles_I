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

    // Dos clientes son iguales si tienen el mismo nombre y el mismo correo
    // (el correo sin importar mayúsculas; el teléfono y la dirección no cuentan)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true          // es el mismo objeto
        if (other !is Cliente) return false      // no es un Cliente
        return nombre == other.nombre && correo.equals(other.correo, ignoreCase = true)
    }

    // Se calcula con los mismos datos que equals (correo en minúsculas), para que sea coherente
    override fun hashCode(): Int {
        return 31 * nombre.hashCode() + correo.lowercase().hashCode()
    }

    // Desestructuración: val (nombre, correo, telefono) = cliente

    // Primer valor: el nombre
    operator fun component1(): String = nombre

    // Segundo valor: el correo
    operator fun component2(): String = correo

    // Tercer valor: el teléfono (se guarda en contacto)
    operator fun component3(): String = contacto
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

    // Dos productos son iguales si tienen el mismo nombre y la misma categoría
    // (el precio y los ingredientes no cuentan)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true          // es el mismo objeto
        if (other !is Producto) return false     // no es un Producto
        return nombre == other.nombre && categoria == other.categoria
    }

    // Se calcula con los mismos datos que equals, para que sea coherente
    override fun hashCode(): Int {
        return 31 * nombre.hashCode() + categoria.hashCode()
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

    // Operador +: junta dos pedidos del mismo cliente en uno nuevo (los originales no cambian)
    // Los montos se recalculan solos porque dependen de la lista de productos y la fecha
    operator fun plus(otro: Pedido): Pedido {
        require(cliente.correo == otro.cliente.correo) {
            "Solo se pueden combinar pedidos del mismo cliente (${cliente.nombre} y ${otro.cliente.nombre} son distintos)"
        }

        // Notas: ambas unidas con " | ", o la que exista, o null si no hay ninguna
        val notasCombinadas = when {
            notas != null && otro.notas != null -> "$notas | ${otro.notas}"
            notas != null -> notas
            else -> otro.notas
        }

        // Se usa la fecha más reciente de los dos pedidos
        val fechaMasReciente = if (fecha.isAfter(otro.fecha)) fecha else otro.fecha

        return Pedido(cliente, productos + otro.productos, notasCombinadas, fechaMasReciente)
    }

    // Cambia el estado solo si es uno de los permitidos y el cambio sigue el orden correcto
    fun cambiarEstado(nuevoEstado: String) {
        val estadosValidos = listOf("En preparación", "Listo", "En camino")

        // A qué estados se puede pasar desde cada uno ("En camino" es el estado final)
        val cambiosPermitidos = mapOf(
            "En preparación" to listOf("Listo"),
            "Listo" to listOf("En camino"),
            "En camino" to emptyList()
        )
        val siguientes = cambiosPermitidos[estado] ?: emptyList()

        if (nuevoEstado in estadosValidos) {
            if (nuevoEstado in siguientes) {
                estado = nuevoEstado
            } else if (siguientes.isEmpty()) {
                // El pedido ya llegó al estado final
                println("Aviso: el pedido ya está \"$estado\" (estado final), no tiene más cambios.")
            } else {
                // El estado existe, pero no se puede saltar a él desde el actual
                val textoSiguientes = siguientes.joinToString(", ") { "\"$it\"" }
                println("Aviso: no se puede pasar de \"$estado\" a \"$nuevoEstado\". Siguiente estado permitido: $textoSiguientes.")
            }
        } else {
            println("Aviso: estado \"$nuevoEstado\" no válido. El pedido sigue \"$estado\".")
        }
    }

    // Desestructuración: val (cliente, productos, total) = pedido

    // Primer valor: el cliente del pedido
    operator fun component1(): Cliente = cliente

    // Segundo valor: la lista de productos
    operator fun component2(): List<Producto> = productos

    // Tercer valor: el total a pagar (se calcula al momento)
    operator fun component3(): Double = calcularTotal()
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

// Agrega un cliente al registro; el Set no acepta repetidos (usa equals y hashCode)
// Devuelve true si se registró o false si ya existía
fun registrarCliente(registro: MutableSet<Cliente>, cliente: Cliente): Boolean {
    val agregado = registro.add(cliente)
    if (agregado) {
        println("Cliente registrado: ${cliente.nombre} (${cliente.correo})")
    } else {
        println("Aviso: el cliente ${cliente.nombre} (${cliente.correo}) ya estaba registrado.")
    }
    return agregado
}

// Agrega un producto al registro; el Set no acepta repetidos (usa equals y hashCode)
// Devuelve true si se registró o false si ya existía
fun registrarProducto(registro: MutableSet<Producto>, producto: Producto): Boolean {
    val agregado = registro.add(producto)
    if (agregado) {
        println("Producto registrado: ${producto.nombre} (${producto.categoria})")
    } else {
        println("Aviso: el producto ${producto.nombre} (${producto.categoria}) ya estaba registrado.")
    }
    return agregado
}

// Resumen final de la jornada con datos reales: pedidos combinados, desestructuración,
// productos promocionables e igualdad de objetos
// Si el cliente tiene menos de 2 pedidos confirmados, se usa como ejemplo la suma de los dos pedidos de ejemplo
fun generarResumenFinal(
    cliente: Cliente,
    pedidosReales: List<Pedido>,
    pedidoEjemplo1: Pedido,
    pedidoEjemplo2: Pedido,
    registroClientes: Set<Cliente>,
    registroProductos: Set<Producto>
): String {
    val lineaDoble = "=".repeat(60)
    val lineaSimple = "-".repeat(60)
    val resumen = StringBuilder()

    // Encabezado con la fecha de hoy
    resumen.appendLine(lineaDoble)
    resumen.appendLine("          RESUMEN FINAL DE LA JORNADA (${LocalDate.now()})")
    resumen.appendLine(lineaDoble)

    // 1) Pedidos combinados con el operador +
    resumen.appendLine("1) PEDIDOS COMBINADOS (operador +)")
    // Con 2 o más pedidos reales se combinan todos; si no, se usa el ejemplo
    val pedidosACombinar = if (pedidosReales.size >= 2) pedidosReales else listOf(pedidoEjemplo1, pedidoEjemplo2)
    if (pedidosReales.size < 2) {
        resumen.appendLine("  (Ejemplo: el cliente tiene menos de 2 pedidos confirmados)")
    }
    for ((indice, pedido) in pedidosACombinar.withIndex()) {
        resumen.appendLine("  Pedido ${indice + 1} (${pedido.cliente.nombre}): ${pedido.resumirProductos()} | Total: ${formatearMonto(pedido.calcularTotal())}")
    }
    val combinado = pedidosACombinar.reduce { a, b -> a + b }
    resumen.appendLine("  Combinado: ${combinado.resumirProductos()}")
    resumen.appendLine("  Descuento: ${formatearMonto(combinado.calcularDescuento())} | Total: ${formatearMonto(combinado.calcularTotal())}")
    resumen.appendLine(lineaSimple)

    // 2) Cliente separado en sus partes
    resumen.appendLine("2) CLIENTE DESESTRUCTURADO")
    val (nombre, correo, telefono) = cliente
    resumen.appendLine("  Nombre:   $nombre")
    resumen.appendLine("  Correo:   $correo")
    resumen.appendLine("  Teléfono: $telefono")
    resumen.appendLine(lineaSimple)

    // 3) Productos marcados con @Promocionable (motivo leído con reflection)
    resumen.appendLine("3) PRODUCTOS PROMOCIONABLES (anotación @Promocionable)")
    for (fuente in listOf(Carta, Bebidas)) {
        resumen.appendLine("  ${fuente::class.simpleName}:")
        for (propiedad in fuente::class.memberProperties) {
            val promocion = propiedad.findAnnotation<Promocionable>() ?: continue
            val producto = propiedad.getter.call(fuente) as? Producto ?: continue
            resumen.appendLine("    - ${producto.nombre.padEnd(14)} ${formatearMonto(producto.precio).padStart(7)} | ${promocion.motivo}")
        }
    }
    resumen.appendLine(lineaSimple)

    // 4) Igualdad de objetos: todos los resultados se calculan aquí
    resumen.appendLine("4) IGUALDAD DE OBJETOS")
    val copiaCliente = Cliente(cliente.nombre, cliente.contacto, cliente.correo, "Otra dirección 123")
    resumen.appendLine("  Cliente == copia con otra dirección:      ${cliente == copiaCliente}")

    // Si María ya está en el registro, un duplicado suyo sería rechazado
    val mariaDuplicada = Cliente("María Soto", "912345678", "maria.soto@correo.cl", "Otra Calle 999")
    val textoMaria = if (mariaDuplicada in registroClientes) "rechazada" else "aceptada"
    resumen.appendLine("  María duplicada en el registro:           $textoMaria")

    val pepperoniOtroPrecio = Producto("Pepperoni", 9990.0, mapOf("masa" to 1), Categoria.PIZZA)
    val pepperoniBebida = Producto("Pepperoni", 8990.0, emptyMap(), Categoria.BEBIDA)
    resumen.appendLine("  Pepperoni == Pepperoni con otro precio:   ${Carta.pepperoni == pepperoniOtroPrecio}")
    resumen.appendLine("  Pepperoni (PIZZA) == Pepperoni (BEBIDA):  ${Carta.pepperoni == pepperoniBebida}")
    resumen.appendLine("  Clientes distintos registrados:           ${registroClientes.size}")
    resumen.appendLine("  Productos distintos registrados:          ${registroProductos.size}")
    resumen.append(lineaDoble)

    return resumen.toString()
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

// Muestra la carta numerada y pide una opción hasta que sea válida
// También indica si la pizza elegida es promocionable (leyendo su anotación con reflection)
fun elegirPizza(carta: List<Producto>): Producto {
    var pizzaElegida: Producto = carta[0]

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

    return pizzaElegida
}

// Pide la cantidad de pizzas hasta que sea un número dentro de RANGO_CANTIDAD
fun leerCantidadPizzas(pizza: Producto): Int {
    var cantidadPizzas = 1

    // Se repite la pregunta hasta que la cantidad sea válida
    var cantidadValida = false
    while (!cantidadValida) {
        println("\n¿Cuántas pizzas ${pizza.nombre} desea? (entre ${RANGO_CANTIDAD.first} y ${RANGO_CANTIDAD.last}, precio unitario: ${formatearMonto(pizza.precio)})")
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

    return cantidadPizzas
}

// Pide notas opcionales: línea vacía o sin entrada quedan en null
fun leerNotas(): String? {
    println("\n¿Desea agregar notas al pedido? (Enter para omitir)")
    val notasPedido: String? = readLine()?.trim()?.takeIf { it.isNotEmpty() }

    // Solo se muestra si hay notas
    notasPedido?.let { println("Nota recibida: $it") }

    return notasPedido
}

// Revisa si hay ingredientes para todo el pedido
// Si alcanzan, los descuenta del inventario y devuelve true; si no, muestra lo que falta y devuelve false
fun verificarStock(pedido: Pedido, inventario: MutableMap<String, Int>): Boolean {
    // Ingredientes que necesita el pedido completo: se suman los de cada producto distinto
    val necesarios = mutableMapOf<String, Int>()
    for ((producto, productosIguales) in pedido.productos.groupBy { it }) {
        val necesariosProducto = calcularIngredientesNecesarios(producto.ingredientes, productosIguales.size)
        for ((ingrediente, cantidad) in necesariosProducto) {
            necesarios[ingrediente] = (necesarios[ingrediente] ?: 0) + cantidad
        }
    }

    // Revisar cada ingrediente necesario y guardar los que no alcanzan
    val ingredientesFaltantes = mutableListOf<String>()
    for ((ingrediente, necesario) in necesarios) {
        val disponible = inventario[ingrediente] ?: 0
        when {
            necesario <= disponible -> println("OK: $ingrediente (necesario: $necesario, disponible: $disponible)")
            else -> ingredientesFaltantes.add(ingrediente)
        }
    }

    if (ingredientesFaltantes.isEmpty()) {
        // Descontar del inventario lo que usa el pedido
        descontarInventario(inventario, necesarios)
        println("\nPedido confirmado. Inventario restante: $inventario")
        return true
    }

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
    return false
}

// Devuelve el repartidor disponible más cercano (o null si no hay ninguno)
fun asignarRepartidor(repartidores: List<Repartidor>): Repartidor? {
    val repartidorCercano = repartidores.sortedBy { it.distanciaKm }.find { it.disponible }
    if (repartidorCercano != null) {
        println("Repartidor asignado: ${repartidorCercano.nombre} (a ${repartidorCercano.distanciaKm} km)")
    }
    return repartidorCercano
}

// Hace todo el flujo de UN pedido: elegir, verificar stock, asignar repartidor y mostrar el resumen
// Devuelve el pedido si se confirmó, o null si se rechazó por falta de ingredientes
fun realizarPedido(
    idPedido: Int,
    cliente: Cliente,
    carta: List<Producto>,
    inventario: MutableMap<String, Int>,
    repartidores: List<Repartidor>
): Pedido? {
    // Elegir la pizza, la cantidad y las notas del pedido
    val pizzaElegida = elegirPizza(carta)
    val cantidadPizzas = leerCantidadPizzas(pizzaElegida)
    val notasPedido = leerNotas()

    // El pedido lleva la pizza repetida tantas veces como pidió el cliente
    // Los montos (subtotal, descuento, impuesto y total) los calcula el propio pedido
    val pedido = Pedido(cliente, List(cantidadPizzas) { pizzaElegida }, notasPedido)

    // Revisa los ingredientes; si alcanzan, se descuentan del inventario
    val pedidoConfirmado: Boolean = verificarStock(pedido, inventario)

    val estadoPedido = if (pedidoConfirmado) {
        pedido.estado
    } else {
        "Rechazado por falta de ingredientes"
    }

    // Se asigna repartidor solo si el pedido se confirmó
    var repartidorAsignado: Repartidor? = null
    val textoRepartidor: String // lo que se muestra en el resumen
    if (pedidoConfirmado) {
        // El más cercano que esté disponible
        repartidorAsignado = asignarRepartidor(repartidores)
        textoRepartidor = repartidorAsignado?.nombre ?: "Sin repartidores disponibles"
    } else {
        textoRepartidor = "No asignado (pedido rechazado)"
    }

    // Resumen del pedido
    println()
    println(generarResumen(idPedido, pedido, estadoPedido, textoRepartidor))

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
        println(repartidorAsignado.entregarPedido(pedido))
    }

    return if (pedidoConfirmado) pedido else null
}

// Pregunta si se quiere hacer otro pedido hasta recibir "s" o "n" (sin importar mayúsculas)
// Si ya no hay entrada (fin de archivo) devuelve false para no quedar en un ciclo infinito
fun preguntarOtroPedido(): Boolean {
    while (true) {
        println("\n¿Desea hacer otro pedido? (s/n)")
        val respuesta = readLine() ?: return false

        when (respuesta.trim().lowercase()) {
            "s" -> return true
            "n" -> return false
            else -> println("Error: responda \"s\" para sí o \"n\" para no.")
        }
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
        "pepperoni" to 10,
        "tomate" to 20,
        "piña" to 10,
        "jamón" to 10,
        "verduras" to 15,
        "albahaca" to 10
    )

    // Carta que se muestra al cliente para elegir (pizzas del object Carta)
    val carta = listOf(Carta.pepperoni, Carta.napolitana, Carta.hawaiana, Carta.vegetariana, Carta.margarita)

    // Repartidores con su distancia al local
    val repartidores = listOf(
        Repartidor("Ana", "911111111", 3.5, true),
        Repartidor("Pedro", "922222222", 1.2, false),
        Repartidor("Luis", "933333333", 2.0, true),
        Repartidor("Sofía", "944444444", 4.8, true)
    )

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

    // ---------- 3. Pedidos del usuario ----------

    // Se repite mientras el usuario quiera hacer otro pedido
    // El inventario es el mismo para todos, así que se va descontando
    val pedidosReales = mutableListOf<Pedido>() // solo los confirmados
    var idPedido = 1001
    do {
        val pedidoReal = realizarPedido(idPedido, cliente, carta, inventario, repartidores)
        if (pedidoReal != null) {
            pedidosReales.add(pedidoReal)
        }
        idPedido++
    } while (preguntarOtroPedido())

    // ---------- Desestructuración del cliente ----------

    println("\n========== DESESTRUCTURACIÓN DE CLIENTE ==========")
    // Se separan los datos del cliente en tres variables de una vez
    val (nombreCliente, correoCliente, telefonoCliente) = cliente
    println("Nombre:   $nombreCliente")
    println("Correo:   $correoCliente")
    println("Teléfono: $telefonoCliente")
    println("=".repeat(50))

    // ---------- Análisis de clases con reflection ----------

    println("\n========== ANÁLISIS DE CLASES (reflection) ==========")
    val clasesAnalizadas = listOf(Cliente::class, Producto::class, Pedido::class, Bebidas::class)
    for (clase in clasesAnalizadas) {
        println(describirClase(clase))
        println("-".repeat(50))
    }

    // ---------- Pedidos combinados con el operador + ----------

    println("\n========== PEDIDOS COMBINADOS (operador +) ==========")
    val clienteMaria = Cliente("María Soto", "987654321", "maria.soto@correo.cl", "Los Aromos 123")
    val pedidoMaria1 = Pedido(clienteMaria, listOf(Carta.pepperoni, Carta.pepperoni), fecha = LocalDate.of(2026, 10, 15))
    val pedidoMaria2 = Pedido(clienteMaria, listOf(Carta.hawaiana, Carta.hawaiana), fecha = LocalDate.now())

    println("Pedido 1 (${pedidoMaria1.fecha}): ${pedidoMaria1.resumirProductos()} | Total: ${formatearMonto(pedidoMaria1.calcularTotal())}")
    println("Pedido 2 (${pedidoMaria2.fecha}): ${pedidoMaria2.resumirProductos()} | Total: ${formatearMonto(pedidoMaria2.calcularTotal())}")

    // Se suman con + y se obtiene un pedido nuevo
    val pedidoCombinado = pedidoMaria1 + pedidoMaria2
    println("Combinado (${pedidoCombinado.fecha}): ${pedidoCombinado.resumirProductos()} | Descuento: ${formatearMonto(pedidoCombinado.calcularDescuento())} | Total: ${formatearMonto(pedidoCombinado.calcularTotal())}")

    // Prueba de error: no se pueden sumar pedidos de clientes distintos
    println("-".repeat(50))
    println("Combinando pedidos de clientes distintos...")
    try {
        val clienteJuan = Cliente("Juan Pérez", "976543210", "juan.perez@correo.cl", "Av. Matta 456")
        val pedidoJuan = Pedido(clienteJuan, listOf(Carta.napolitana))
        val pedidoInvalido = pedidoMaria1 + pedidoJuan
        println("Pedido combinado: ${pedidoInvalido.resumirProductos()}")
    } catch (e: IllegalArgumentException) {
        println("Error al combinar: ${e.message}.")
    }
    println("=".repeat(53))

    // ---------- Comparación de productos con == ----------

    println("\n========== COMPARACIÓN DE PRODUCTOS (==) ==========")
    val pepperoniOtroPrecio = Producto("Pepperoni", 9990.0, mapOf("masa" to 1), Categoria.PIZZA)
    val pepperoniBebida = Producto("Pepperoni", 8990.0, emptyMap(), Categoria.BEBIDA)

    // Mismo nombre y categoría, distinto precio: son iguales
    println("Pepperoni == Pepperoni a $9990 (PIZZA): ${Carta.pepperoni == pepperoniOtroPrecio}")
    // Distinto nombre: no son iguales
    println("Pepperoni == Napolitana: ${Carta.pepperoni == Carta.napolitana}")
    // Mismo nombre, distinta categoría: no son iguales
    println("Pepperoni (PIZZA) == Pepperoni (BEBIDA): ${Carta.pepperoni == pepperoniBebida}")
    // === revisa si es el mismo objeto en memoria, no si son iguales
    println("Pepperoni === Pepperoni a $9990 (PIZZA): ${Carta.pepperoni === pepperoniOtroPrecio}")
    println("=".repeat(51))

    // ---------- 6. Reporte de pedidos del día ----------

    // Pedidos de ejemplo del día (con las mismas pizzas de la carta)
    // El de María tiene fecha fuera de la promoción, para comparar
    // Es una lista mutable para poder agregar después el pedido real del usuario
    val pedidosDelDia = mutableListOf<Pedido>(
        Pedido(Cliente("María Soto", "987654321", "maria.soto@correo.cl", "Los Aromos 123"), listOf(Carta.pepperoni, Carta.pepperoni), fecha = LocalDate.of(2026, 10, 15)),
        Pedido(Cliente("Juan Pérez", "976543210", "juan.perez@correo.cl", "Av. Matta 456"), listOf(Carta.napolitana)),
        Pedido(Cliente("Carla Rojas", "965432109", "carla.rojas@correo.cl", "Pasaje Sur 789"), listOf(Carta.hawaiana, Carta.hawaiana, Carta.hawaiana)),
        Pedido(Cliente("Diego Muñoz", "954321098", "diego.munoz@correo.cl", "Calle Norte 321"), listOf(Carta.vegetariana, Carta.pepperoni)),
        Pedido(Cliente("Valentina Díaz", "943210987", "valentina.diaz@correo.cl", "Av. Grecia 654"), listOf(Carta.margarita, Carta.margarita, Carta.margarita, Carta.margarita))
    )

    // Todos parten "En preparación"; se actualizan los que ya avanzaron
    // El de Carla pasa primero por "Listo" y luego a "En camino"
    pedidosDelDia[1].cambiarEstado("Listo")
    pedidosDelDia[2].cambiarEstado("Listo")
    pedidosDelDia[2].cambiarEstado("En camino")
    pedidosDelDia[4].cambiarEstado("Listo")

    // Demostración: un salto no permitido no cambia el estado y muestra un aviso
    println("\nIntentando pasar el pedido de ${pedidosDelDia[0].cliente.nombre} directo a \"En camino\"...")
    pedidosDelDia[0].cambiarEstado("En camino")

    // Se suman al día los pedidos reales confirmados (los rechazados no se van a preparar)
    pedidosDelDia.addAll(pedidosReales)

    // ---------- Desestructuración de pedidos ----------
    // Va aquí porque necesita que pedidosDelDia ya exista

    println("\n========== DESESTRUCTURACIÓN DE PEDIDO ==========")
    // Último pedido real confirmado separado en cliente, productos y total
    val ultimoPedidoReal = pedidosReales.lastOrNull()
    if (ultimoPedidoReal == null) {
        println("No hay pedidos reales confirmados")
    } else {
        val (clientePedido, productosPedido, totalPedido) = ultimoPedidoReal
        println("Cliente:   ${clientePedido.nombre}")
        println("Productos: ${productosPedido.size}")
        println("Total:     ${formatearMonto(totalPedido)}")

        // Desestructuración anidada: del cliente solo se usa el nombre (_ ignora el resto)
        val (nombreDelCliente, _, _) = clientePedido
        println("Nombre obtenido del cliente del pedido: $nombreDelCliente")
    }

    // Recorrer los pedidos del día separando cada uno en sus partes
    println("-".repeat(49))
    for ((clienteDelDia, productosDelDia, totalDelDia) in pedidosDelDia) {
        println("${clienteDelDia.nombre} | Productos: ${productosDelDia.size} | Total: ${formatearMonto(totalDelDia)}")
    }
    println("=".repeat(49))

    // ---------- Registro sin duplicados ----------

    println("\n========== REGISTRO SIN DUPLICADOS (equals y hashCode) ==========")

    // Clientes: el del usuario, los del día y un duplicado de María que debe rechazarse
    val registroClientes = mutableSetOf<Cliente>()
    registrarCliente(registroClientes, cliente)
    // Se omite el cliente real (mismo objeto), porque ya se registró arriba
    for (pedido in pedidosDelDia.filter { it.cliente !== cliente }) {
        registrarCliente(registroClientes, pedido.cliente)
    }
    val mariaDuplicada = Cliente("María Soto", "912345678", "maria.soto@correo.cl", "Otra Calle 999")
    registrarCliente(registroClientes, mariaDuplicada)
    println("Clientes registrados: ${registroClientes.size}")
    println("-".repeat(65))

    // Productos: las 5 pizzas, una Pepperoni repetida (otro precio) y una bebida
    val registroProductos = mutableSetOf<Producto>()
    for (pizza in listOf(Carta.pepperoni, Carta.napolitana, Carta.hawaiana, Carta.vegetariana, Carta.margarita)) {
        registrarProducto(registroProductos, pizza)
    }
    registrarProducto(registroProductos, Producto("Pepperoni", 9990.0, mapOf("masa" to 1), Categoria.PIZZA))
    registrarProducto(registroProductos, Bebidas.jugoNatural)
    println("Productos registrados: ${registroProductos.size}")
    println("-".repeat(65))

    // Dos listas son iguales si sus elementos son iguales (usa el equals de Producto)
    val listaCarta = listOf(Carta.pepperoni, Carta.margarita)
    val listaParecida = listOf(Producto("Pepperoni", 1.0, emptyMap(), Categoria.PIZZA), Producto("Margarita", 2.0, emptyMap(), Categoria.PIZZA))
    println("Lista [Pepperoni, Margarita] == lista con otros precios: ${listaCarta == listaParecida}")

    // Comparar con null no falla: simplemente da false
    val clienteSinDatos: Cliente? = null
    println("Cliente en null == ${cliente.nombre}: ${clienteSinDatos == cliente}")
    println("=".repeat(65))

    // Reporte: una línea por pedido
    println("\n========== REPORTE DE PEDIDOS DEL DÍA ==========")
    for (pedido in pedidosDelDia) {
        println("Cliente: ${pedido.cliente.nombre} | Pizzas: ${pedido.resumirProductos()} | Total: ${formatearMonto(pedido.calcularTotal())} | Estado: ${pedido.estado}")
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

    // ---------- 8. Resumen final de la jornada ----------

    // Es lo último que se imprime
    println()
    println(generarResumenFinal(cliente, pedidosReales, pedidoMaria1, pedidoMaria2, registroClientes, registroProductos))
}
