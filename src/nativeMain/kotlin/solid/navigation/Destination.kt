package solid.navigation


public fun interface Destination {
    public fun navigate(host: NavigationHost, arguments: Bundle?): Fragment
}


/*
public class MainFragment(host: NavigationHost) : Fragment(host) {
    override fun createView(container: Container): Widget {
    }

    public companion object : Destination {
        override fun navigate(host: NavigationHost, graph: MutableList<Fragment>): Fragment {
            return graph.find { it is MainFragment } ?: MainFragment(host)
        }

    }
}*/