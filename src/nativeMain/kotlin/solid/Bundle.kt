package solid

public class Bundle {
    @PublishedApi internal val data: MutableMap<String, Any?> = mutableMapOf()

    public inline operator fun <reified T : Any?> get(key: String): T {
        return data[key] as T
    }

    public inline operator fun <reified T : Any?> set(key: String, value: T) {
        data[key] = value
    }

    public companion object {
        public inline fun create(setup: Bundle.()->Unit): Bundle = Bundle().apply(setup)
    }
}