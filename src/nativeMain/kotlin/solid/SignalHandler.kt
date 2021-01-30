package solid

import kotlin.native.ref.WeakReference

/**A handler that responds to [Signal] emissions.
 *
 * Handlers must be connected to a specific [signal] in order to receive events, which are then handled by
 * the [invoke] implementation.
 *
 * A [SignalHandler] can be manually disconnected and reconnected as many times as it is necessary through the*/
public abstract class SignalHandler<T> private constructor(private var signal: WeakReference<Signal<T>>) {
    public constructor(signal: Signal<T>) : this(signal.weakReference())

    /**When the [Signal] this handler is connected to emits an event, this method is used to handle it with the context
     * provided by the signal.*/
    public abstract operator fun invoke(context: T)

    /**Disconnects this handler so it no longer receives events. You can use [connect] to reattach it to a [Signal].
     *
     * Overriding implementations *must* call the base method.*/
    public open fun disconnect(): Boolean = signal.value?.handlers?.remove(this) ?: true

    /**Connects this handler to a [Signal], so it will receive emission events from it. To stop receiving events, use
     * [disconnect].
     *
     * Overriding implementations *must* call the base method.
     *
     * When overriding this method, do not store a reference to the signal argument to allow it and its owning widget
     * to be freed by the garbage collector.*/
    public open fun connect(to: Signal<T>): Boolean {
        signal = to.weakReference()
        return to.handlers.add(this)
    }

    /**Attempts to reconnect to the last [Signal] this handler was connected to.
     *
     * This method fails if the last [Signal]'s widget was already freed by the Garbage Collector, this avoids
     * useless connections to signals that will no longer emit.
     *
     * Overriding implementations *must* call the base method.
     * */
    public open fun reconnect(): Boolean {
        return signal.value?.let { connect(to = it) } ?: false
    }
}

