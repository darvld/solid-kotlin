@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package solid.widgets

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.staticCFunction
import solid.WidgetWrapException
import solid.events.Signal

@SolidDsl
public abstract class Widget {
    private val signals: MutableMap<Int, Signal<out Widget>> = mutableMapOf()

    internal inline fun handleEvent(eventId: Int) {
        signals[eventId]?.emit()
    }

    /**Registers a new signal for the given event [id]. When incoming events are handled, the signal assigned to the
     * corresponding event id is emitted.
     *
     * Please note that events and event ids are defined at the low level Solid C++ backend. To create your own custom
     * events you will need to modify those sources and then register the appropriate signal.*/
    protected fun <W : Widget> W.registerSignal(id: Int): Signal<W> {
        return Signal(this).also { signals[id] = it }
    }

    public companion object {
        internal val WidgetEventHandler = staticCFunction { event: Int, widget: COpaquePointer? ->
            tryWrap(widget)?.handleEvent(event) != null
        }

        /**Creates a [Widget] wrapper from a given C pointer.
         * - If the bindingPointer field is `null`, a [StableRef][kotlinx.cinterop.StableRef] is stored in it and the
         * event handler is setup. The widget is then officially owned by this runtime.
         * - If the bindingPointer or the eventHandler fields are not `null`, the function fails with
         * [WidgetWrapException]. If you catch the exception, you can still access the pointer so you can do other
         * things with it.
         *
         * If you don't intend to use the exception, use [tryWrap] or [maybeWrap] instead.
         * */
        public fun wrap(pointer: COpaquePointer?): Widget {
            throw WidgetWrapException(pointer, "Not yet implemented")
        }

        /**Attempts to wrap the [pointer], returning null on failure (if the widget is owned by another runtime).
         *
         * This is the method most commonly used by library functionalities, since it avoids clashing with other
         * runtimes/language bindings. It is also the recommended way to wrap widgets manually if you plan to interact
         * with a different runtime/language. If your program runs completely on its own, you can safely use [wrap].
         *
         * If you don't need to actually respond to events from the widget, you can use [maybeWrap], which on failure
         * still returns a [Widget], allowing you to modify it without handling events. Other restrictions may apply
         * in the future.*/
        @Suppress("UNUSED_PARAMETER")
        public fun tryWrap(pointer: COpaquePointer?): Widget? {
            return null
        }

        /**Attempts to wrap the [pointer], returning an non-wrapped [Widget] on failure. Non-wrapped widgets cannot
         * receive events.
         *
         * This method is highly discouraged unless you know exactly what your doing. Use [wrap] or [tryWrap].*/
        @Suppress("UNUSED_PARAMETER")
        public fun maybeWrap(pointer: COpaquePointer?): Widget = TODO()
    }
}