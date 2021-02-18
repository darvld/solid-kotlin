@file:Suppress("MemberVisibilityCanBePrivate")

package solid.navigation

import kotlinx.cinterop.Arena
import kotlinx.cinterop.AutofreeScope
import kotlinx.cinterop.NativePointed
import solid.Bundle
import solid.widgets.Container
import solid.widgets.Widget

public abstract class Fragment : AutofreeScope() {

    public lateinit var host: NavigationHost
    internal set

    // region Memory Scope
    private var usedArena = false
    private val localArena: Arena by lazy { usedArena = true; Arena() }

    final override fun alloc(size: Long, align: Int): NativePointed = localArena.alloc(size, align)
    final override fun alloc(size: Int, align: Int): NativePointed = localArena.alloc(size, align)
    // endregion

    protected lateinit var root: Widget
    private set

    internal fun init(container: Container): Widget {
        onAttached()
        
        root = container.createView()
        return root
    }

    internal fun destroy() {
        onDestroy()

        // TODO: Destroy root widget

        // Clear the arena if it was used
        if (usedArena)
            localArena.clear()
    }

    public inline fun navigateTo(where: Destination, arguments: Bundle? = null) {
        host.navigateTo(where, arguments)
    }

    public inline fun navigateUp() {
        host.navigateUp()
    }

    // region lifecycle
    protected abstract fun Container.createView(): Widget

    protected open fun onAttached() {}

    public open fun onDisplay() {}

    public open fun onLeave() {}

    protected open fun onDestroy() {}
    // endregion
}