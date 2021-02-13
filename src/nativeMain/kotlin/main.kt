import solid.navigation.Bundle
import solid.navigation.Destination
import solid.navigation.Fragment
import solid.navigation.NavigationHost
import solid.widgets.Container
import solid.widgets.Widget

private inline fun <reified T> stub(vararg args: Any?): T = TODO(args.toString())
private const val SIGNAL_EDIT = 4

private inline fun Container.sample(setup: SampleWidget.()->Unit): SampleWidget = stub()

private class SampleWidget : Widget() {
    val onEdit = registerSignal(SIGNAL_EDIT)
}

private class SettingsFragment : Fragment() {
    override fun onAttached() {
        TODO()
    }

    override fun Container.createView(): Widget = sample {

    }

    override fun onDisplay() {
        TODO()
    }

    override fun onDestroy() {
        TODO()
    }

    companion object : Destination {
        override fun navigate(host: NavigationHost, arguments: Bundle?): Fragment {
            return host.findOrCreate(::SettingsFragment)
        }
    }
}

public fun main() {
    println("Started")
    /*val root: Container = stub()

    val home: Destination = SettingsFragment*/

    println("Finished")
}