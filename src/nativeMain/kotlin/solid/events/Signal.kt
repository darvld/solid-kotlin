@file:Suppress("unused")

package solid.events

public value class Signal(private val handlers: MutableSet<SignalHandler> = mutableSetOf()) {
    public fun emit(): Boolean {
        if(handlers.isEmpty()) return false

        for (handler in handlers)
            handler.invoke()

        return true
    }

    public fun connect(handler: SignalHandler): Boolean {
        if (!handlers.add(handler))
            return false

        handler.connectedTo = this
        return true
    }

    public inline fun connect(noinline handler: () -> Unit): SignalHandler = SignalHandler(handler).also { connect(it) }
    public inline operator fun invoke(noinline handler: () -> Unit): SignalHandler = connect(handler)

    public fun disconnect(handler: SignalHandler): Boolean {
        if (!handlers.remove(handler))
            return false

        handler.connectedTo = null
        return true
    }
}

