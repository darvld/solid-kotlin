@file:Suppress("unused")

package solid.events

public open class SignalHandler(private val callback: () -> Unit) {
    internal var connectedTo: Signal? = null

    public val connected: Boolean get() = connectedTo != null
    public open operator fun invoke(): Unit = callback()

    public fun disconnect(): Boolean = connectedTo?.disconnect(this) ?: true
}