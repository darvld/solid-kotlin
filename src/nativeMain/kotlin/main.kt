import solid.Widget

private inline fun <reified T> stub(): T = TODO()
private const val SIGNAL_EDIT = 4

private class SampleWidget : Widget() {
    val onEdit = registerSignal(SIGNAL_EDIT)
}

public fun main() {
    println("Started")

    val sample = SampleWidget()

    val handler = sample.onEdit {
        println("Edited: $it")
    }

    sample.handleEvent(SIGNAL_EDIT)
    handler.disconnect()
    println("Don't handle")

    sample.handleEvent(SIGNAL_EDIT)
    handler.reconnect()
    println("Didn't handle")

    sample.handleEvent(SIGNAL_EDIT)

    println("Finished")
}