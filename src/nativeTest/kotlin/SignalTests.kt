import solid.Bundle
import solid.events.Scheduler
import solid.events.Signal
import kotlin.random.Random
import kotlin.test.Test

@Test
public fun main() {

    val onRandom = Signal.create {
        it!!.get<Int>("attempt") to Random.nextInt()
    }

    onRandom {
        println("[Attempt #${it.first}] Here's a random value: ${it.second}")
    }

    repeat(Random.nextInt(1, 5)) {
        onRandom.emit(Bundle.create { set("attempt", it) })
    }
}