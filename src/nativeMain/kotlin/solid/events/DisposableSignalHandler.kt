package solid.events

/**Connects a *disposable* handler to this signal. The handler will automatically disconnect itself after the specified
 *  number of invocations. You can use [SignalHandler.reconnect] to reset the use count after it has been disconnected.*/
public inline fun <T> Signal<T>.connectDisposable(
    disconnectAfter: Int = 1,
    noinline handler: (T) -> Unit
): SignalHandler<T> {
    return object : SignalHandler<T>(this) {
        private var count: Int = 0

        override fun invoke(context: T) {
            handler(context)
            count++

            if (count >= disconnectAfter)
                disconnect()
        }

        override fun connect(to: Signal<T>): Boolean {
            // reset count
            count = 0
            return super.connect(to)
        }

        override fun reconnect(): Boolean {
            // reset count
            count = 0
            return super.reconnect()
        }

    }.also { connect(it) }
}