@file:Suppress("MemberVisibilityCanBePrivate")

package solid.events

/**Represents an event to be invoked at some later point (or not at all).
 *
 * You can [connect] to a signal in order to receive emission notifications. There are a few ways to do this:
 *  - By connecting an explicitly created [SignalHandler]
 *  - By using the lambda overload, which implicitly creates a new handler.
 *  - By using the [invoke] operator, which delegates to the previous method.
 *
 * You can also use [SignalHandler.disconnect] at any time to remove the handler.
 *
 * Signals have an [owner], which is passed on to every connected [SignalHandler] to provide a context
 * at the time of the emission.*/
public class Signal<T>(private val owner: T) {
    /**A set of [SignalHandler] instances to be notified on [emit].*/
    internal val handlers: MutableSet<SignalHandler<T>> = mutableSetOf()

    /**Trigger a signal emission, this will invoke every registered [SignalHandler].
     *
     * You should not call this yourself unless you are implementing a custom signal system, or if you need to simulate
     * a widget event manually.*/
    public fun emit() {
        for (handler in handlers) {
            handler(owner)
        }
    }

    /**Connects [handler] to this signal. This is the same as calling `handler.connect(this)`*/
    public inline fun connect(handler: SignalHandler<T>): Boolean = handler.connect(this)

    /**Creates a new [SignalHandler] using the given [handler] function. The resulting handler is then connected to
     * this signal.*/
    public inline fun connect(crossinline handler: (T) -> Unit): SignalHandler<T> {
        return object : SignalHandler<T>(this) {
            override fun invoke(context: T) = handler(context)
        }.also { connect(it) }
    }

    /**Disconnect all registered signal handlers.*/
    public fun clearHandlers() {
        for (handler in handlers) {
            handler.disconnect()
        }
    }

    /**Connects a new [SignalHandler], constructed with the given [handler] function. This is the same as calling
     * [connect] with [handler] as argument.*/
    public inline operator fun invoke(crossinline handler: (context: T) -> Unit): SignalHandler<T> = connect(handler)
}