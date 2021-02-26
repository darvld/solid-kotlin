package solid.events

import solid.weakReference
import kotlin.native.ref.WeakReference

/**A handler that responds to [Event] emissions.
 *
 * Handlers must be connected to a specific [event] in order to receive events, which are then handled by
 * the [invoke] implementation.
 *
 * A [EventHandler] can be manually disconnected and reconnected as many times as it is necessary through the*/
public abstract class EventHandler<T> private constructor(private var event: WeakReference<Event<T>>) {
    public constructor(event: Event<T>) : this(event.weakReference())

    /**When the [Event] this handler is connected to emits an event, this method is used to handle it with the context
     * provided by the signal.*/
    public abstract operator fun invoke(context: T)

    /**Disconnects this handler so it no longer receives events. You can use [connect] to reattach it to a [Event].
     *
     * Overriding implementations *must* call the base method.*/
    public open fun disconnect(): Boolean = event.value?.handlers?.remove(this) ?: true

    /**Connects this handler to a [Event], so it will receive emission events from it. To stop receiving events, use
     * [disconnect].
     *
     * Overriding implementations *must* call the base method.
     *
     * When overriding this method, do not store a reference to the signal argument to allow it and its owning widget
     * to be freed by the garbage collector.*/
    public open fun connect(to: Event<T>): Boolean {
        event = to.weakReference()
        return to.handlers.add(this)
    }

    /**Attempts to reconnect to the last [Event] this handler was connected to.
     *
     * This method fails if the last [Event]'s widget was already freed by the Garbage Collector, this avoids
     * useless connections to signals that will no longer emit.
     *
     * Overriding implementations *must* call the base method.
     * */
    public open fun reconnect(): Boolean {
        return event.value?.let { connect(to = it) } ?: false
    }
}

