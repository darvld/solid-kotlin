@file:Suppress("MemberVisibilityCanBePrivate")

package solid.events

import solid.Bundle

/**Represents an event to be invoked at some later point (or not at all).
 *
 * You can [connect] to a signal in order to receive emission notifications. There are a few ways to do this:
 *  - By connecting an explicitly created [EventHandler]
 *  - By using the lambda overload, which implicitly creates a new handler.
 *  - By using the [invoke] operator, which delegates to the previous method.
 *
 * You can also use [EventHandler.disconnect] at any time to remove the handler.
 *
 * Signals provide a context to its handlers on emission, obtained through the [translateContext] method implementation.*/
public abstract class Event<T> {
    protected abstract fun translateContext(context: Bundle?): T

    /**A set of [EventHandler] instances to be notified on [emit].*/
    internal val handlers: MutableSet<EventHandler<T>> = mutableSetOf()

    /**Trigger a signal emission, this will invoke every registered [EventHandler].
     *
     * You should not call this yourself unless you are implementing a custom signal system.*/
    public fun emit(context: Bundle? = null) {
        for (handler in handlers) {
            handler(translateContext(context))
        }
    }

    /**Connects [handler] to this signal. This is the same as calling `handler.connect(this)`*/
    public inline fun connect(handler: EventHandler<T>): Boolean = handler.connect(this)

    /**Creates a new [EventHandler] using the given [handler] function. The resulting handler is then connected to
     * this signal.*/
    public inline fun connect(crossinline handler: (T) -> Unit): EventHandler<T> {
        return object : EventHandler<T>(this) {
            override fun invoke(context: T) = handler(context)
        }.also { connect(it) }
    }

    /**Disconnect all registered signal handlers.*/
    public fun clearHandlers() {
        for (handler in handlers) {
            handler.disconnect()
        }
    }

    /**Connects a new [EventHandler], constructed with the given [handler] function. This is the same as calling
     * [connect] with [handler] as argument.*/
    public inline operator fun invoke(crossinline handler: (context: T) -> Unit): EventHandler<T> = connect(handler)

    public companion object {
        public inline fun <T> create(crossinline contextProvider: (Bundle?) -> T): Event<T> = object : Event<T>(){
            override fun translateContext(context: Bundle?): T = contextProvider(context)
        }
    }
}