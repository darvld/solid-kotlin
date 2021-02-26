package solid.backend

public fun interface Scheduler {
    public fun schedule(block: () -> Unit)
}