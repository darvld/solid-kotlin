@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package solid.navigation

import solid.widgets.Container

public class NavigationHost(root: Destination) {
    public val stack: MutableList<Fragment> = mutableListOf()
    private var currentLocation: Int = 0

    private fun provideFragmentRoot(): Container {
        TODO()
    }

    public val activeFragment: Fragment
        get() = stack[currentLocation]

    init {
        navigateTo(root)
    }

    public fun popStack(): Fragment? {
        if(currentLocation <= 0) return null
        return stack.removeLast()
    }

    public fun navigateUp() {
        // Cannot navigate up from the root fragment
        if (currentLocation <= 0) return

        stack.removeLastOrNull()?.let { removed ->
            currentLocation -= 1
            activeFragment.onDisplay()

            removed.destroy()
        }
    }

    public inline fun <reified T : Fragment> find(): T? {
        return stack.find { it is T }?.let { it as T }
    }

    public inline fun <reified T : Fragment> findOrCreate(factory: () -> T): T {
        return stack.find { it is T }?.let { it as? T } ?: factory()
    }

    public inline fun navigateTo(where: Destination, arguments: Bundle? = null) {
        navigateTo(where.navigate(this, arguments))
    }

    public fun navigateTo(destination: Fragment) {
        // Avoid triggering onDisplay if the fragment is already active
        if (destination === activeFragment) return
        destination.host = this

        if (destination !in stack)
            destination.init(provideFragmentRoot())

        stack.add(destination)
        activeFragment.onLeave()
        currentLocation = stack.lastIndex

        destination.onDisplay()
    }
}