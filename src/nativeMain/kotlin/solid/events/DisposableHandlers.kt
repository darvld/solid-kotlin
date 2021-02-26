package solid.events

/**Connects a *disposable* handler to this signal. The handler will automatically disconnect itself after the specified
 *  number of invocations. You can use [EventHandler.reconnect] to reset the use count after it has been disconnected.*/
public inline fun <T> Event<T>.connectDisposable(
    disconnectAfter: Int = 1,
    noinline handler: (T) -> Unit
): EventHandler<T> {
    return object : EventHandler<T>(this) {
        private var count: Int = 0

        override fun invoke(context: T) {
            handler(context)
            count++

            if (count >= disconnectAfter)
                disconnect()
        }

        override fun connect(to: Event<T>): Boolean {
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

public inline fun Signal.connectDisposable(disconnectAfter: Int = 1, noinline handler: () -> Unit): SignalHandler {
    return object : SignalHandler(handler) {
        private var invocations: Int = 0

        override operator fun invoke() {
            super.invoke()
            invocations++

            if (invocations >= disconnectAfter) disconnect()
        }
    }
}