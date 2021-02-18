package solid.events

public fun interface Scheduler {
    public fun schedule(block: () -> Unit)
}