import solid.Bundle
import solid.events.Event
import solid.events.connectDisposable
import solid.widgets.Widget
import kotlin.native.internal.test.TestSuite
import kotlin.random.Random
import kotlin.test.Ignore
import kotlin.test.Test

private const val SIGNAL_CLICK = 1000

private class SomeWidget : Widget() {
    public val onClick by registerSignal(SIGNAL_CLICK)
}

@Test
public fun main() {
    val widget = SomeWidget()

    val handler = widget.onClick {
        println("Clicked")
    }

    val disposable = widget.onClick.connectDisposable {
        println("One shot")
    }
}