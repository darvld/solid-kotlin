# <img src="resources\solid_logo.png" alt="solid_logo" style="zoom:40%;" />  Solid-Kotlin

A **Kotlin/Native** library binding for the **Solid** UI backend

[toc]

## The Widget system

Solid’s class structure is based on “widgets”: objects with or without graphic representation that compose the User Interface. Widgets are the building blocks of Solid and its base library: **FLTK**.

### Loading UI definition files

If you need to share the same UI hierarchy and design between multiple languages and applications, you can use Solid’s UI definition file format, and instantiate the widgets in runtime using the WidgetBuilder interface. You can then find any widget in the hierarchy by their name and path, as defined in the UI file.

### Creating UI from code

The widget design API for Solid-Kotlin is meant to blend in seamlessly with Kotlin-style code: the ui components are declared and configured using a custom DSL, so they look pretty much like Qt’s QML.

```kotlin
/** Create a basic window with a couple of controls */
fun showSomething(): Widget = window("Hello World!") { // this: Window
	val foo = label("This is a Solid-Kotlin example")
    
    button("Click me!") { // this: Button
        // Customize the widget
        size = 90 by 20
        
        // Connect to events
        onClick {
            println("Made you click!")
            foo.text = "You clicked the button!"
        }
    }
}
```

###### Naming widgets

As seen in the previous example, the code defining a UI hierarchy looks straightforward and readable enough. We can also name  Widgets so we can retrieve them later by searching the hierarchy:

```kotlin
fun createUI(container: Container) = container { // this: Container
    button("Ok", name="ok_button"){
        onClick { doAccept() }
    }
    val user = textField("Username:", type=InputType.EMAIL, name="username_field") {
        onEdit { // it: textField
            verifyEmail(it.text)
        }
    }
}

fun useUI(window: Window) {
	createUI(window)
    
    val button: Button = window.getChild("ok_button")
}
```

A widget’s name is always the last parameter for a constructor (except for lambdas). This is done by design to avoid confusing other String parameters with the widget’s name parameter.

###### Handling events

Internally, the library handles the C interface, so you don’t need to worry about interpreting raw event-codes or pointers. Instead, you can connect signal handlers like the following:

```kotlin
fun createUI(container: Container) = container.apply {
    // Connect the handler directly, this results in cleaner code in case
    // you don't need to actually modify the widget's properties
    button("Hello").onClick {
        println("Hello World!")
    }
    
    // You can store the connected handler, so you can manually disconnect
    // it later, and even re-connect it
    val handler: SignalHandler = textField("Say something").onEdit {
        // Implicit parameter "it" of type TextField
        println("You said: $it.text")
    }
    
    // You can create disposable handlers: they will automatically 
    // disconnect after a certain amount of calls (defaults to 1):
    button("Disconnect").onClick.connectDisposable(disconnectAfter=3) {
        if(handler.connected)
	    	handler.disconnect()
        else
        	handler.reconnect()
    }
}
```

Widget events are implemented as properties: the `Event<T>` class exposes a simple `connect` method, which takes an `EventHandler<T>` instance as single argument (or a `(T)->Unit` lambda). The `invoke` operator also delegates to `connect` to make the code more readable.

Events and handlers take a generic `T` parameter which specifies the context in which the handler is invoked. This context is usually the type of widget that owns the signal, although it can be used for other purposes, like implementing a custom signal system. This way, when you connect a handler, the implicit `it` parameter of the lambda is in fact a reference to the owner of the signal the handler is connected to.

It is possible to disconnect all handlers for a specific `Event` by calling `Event.clearHandlers()`.  You can also store a reference to an `Event` to connect it at a later time, without having to keep track of the widget, though you should use the `weakReference()` extension for this, to allow the owning widget to be freed by the Garbage Collector and avoid connecting useless event handlers.

Keep in mind that in runtime, Kotlin prohibits type checks and casts on generic types like `Signal<T>` and `SignalHandler<T>`, so if your code needs to dynamically handle different signal and handler types, there are a few restrictions. Instead of this:

 ```kotlin
// incorrect: invariant type parameter
val signals: MutableList<Signal<Widget>> = mutableListOf()
val button = button("Click me!")

// Error: Signal<Widget> expected but inferred type is Signal<Button>
signals.add(button.onClick)
 ```

Use this:

```kotlin
// correct: projected type parameter
val signals: MutableList<Signal<out Widget>> = mutableListOf()
val button = button("Click me!")

// This is ok
signals.add(button.onClick)
```



### Sharing widgets with other language bindings

Since **Solid** is a multiplatform, poliglot library, interaction between bindings created by different languages are part of its API. However, special care must be taken when working in a multi-language environment, or an environment with multiple runtimes not allowing instance sharing, like **Kotlin/Native**'s runtime.

In their original C++ class definitions, Solid's widgets reserve a void pointer field for language-specific wrappers, Kotlin bindings use that field to store a `StableRef`.
This creates a problem, though: if a Kotlin widget wrapper is created around a pointer to a widget that was previously wrapped by another language (or a different Kotlin runtime), the wrapping/unwrapping of the `StableRef ` will fail. To solve this, Kotlin wrappers can be constructed in three different ways:

1. By manually using a constructor call that creates a *new* widget. Widgets created this way are wrapped automatically, and so their pointer fields are used by the Kotlin runtime that created them (the owner).

2. By using  the method `Widget.wrap(COpaquePointer)` available in Widget’s companion object. It takes a widget pointer and attempts to wrap around it:
    - If the `bindingPointer` field is `null`, a `StableRef` is stored in it and the event handler is setup. The widget is then officially owned by this runtime.
    - If the `bindingPointer` is not `null`, it is unwrapped as a `StableRef<Widget>`. If the pointer cannot be unwrapped the function fails with an exception of type `WidgetWrappingException`. These exceptions store the original widget’s pointer, so you can still use the pointer if you catch them.

    As an alternative, `Widget.tryWrap(COpaquePointer)` could be used to avoid the exception, however the returned value will be of type `Widget?` (nullable). This is the method used by most functions and value getters in the library, to avoid returning wrappers to widgets not owned by the runtime.
    
    Another way is to use `Widget.maybeWrap(COpaquePointer)`, which attempts to wrap the widget pointer and upon failure, returns an unwrapped widget, as described below. To find out whether the bindings were created and stored, use the ` Widget.isWrapper` property.
    
6. By calling a constructor which takes only a pointer as argument. This way, no wrappers will be stored and you can use the widget, but you won't receive any events. This allows to respect other language bindings while still using the widget's methods and properties.

```kotlin
fun createWidget(pointer: COpaquePointer?, example: ExampleCase): Widget {
    return when(example) {
        Create -> {
            // Always wraps the widget, it is owned by this runtime
            MyWidget("Hello")
        }
        Wrap -> {
            // Returns the Widget or fails with a WidgetBindingException if
            // a wrapper from a different runtime already owns the widget
            Widget.wrap(pointer)
        }
        TryWrap -> {
            // Returns null if the wrapping fails
            Widget.tryWrap(pointer)!!
        }
        MaybeWrap -> {
            // Always returns a Widget, but if wrapping fails we won't be 
            // able to receive events, use this only for internal/final 
            // parts of your code
            Widget.maybeWrap(pointer).also { it: Widget->
                // Use this to check whether the widget was successfully
                // wrapped
                if(it.isWrapper)
                	TODO()
                else
                    TODO()
            }
        }
        NoWrap -> {
            // Note that this widget wrapper will not receive any events. 
            // Some other limitations may apply in the future
            MyWidget(pointer)
        }
    }
}
```

> Do not try to take over widgets you don't own, this can cause the original owner's code to misbehave. As a general rule, if you need to transfer ownership, be polite and establish an API to exchange/share widgets instead.

## Navigation and the Fragment system

###### Destinations

The key concept in the whole navigation system is the `Destination` component. It is a functional interface that provides a `Fragment` to display to the user. You can create destinations using the SAM converter:

```kotlin
// Create the destination
val helpScreen = Destination { host: NavigationHost, args: Bundle? ->
    // Instantiate a fragment and return it
    return HelpScreen(host, args!!.get("topic"))
}

// Setup the arguments
val arguments = Bundle { set("topic", "navigation") }

// Navigate
host.navigateTo(helpScreen, arguments)
```

Or you can make your custom fragment’s Companion object implement the interface:

```kotlin
class HelpScreen(topic: String) : Fragment() {
    override fun Container.createUI(): Widget {
        /*Your UI code*/
    }
    
    companion object : Destination {
        // Create a new instance only if there isn't one in the navigation stack
    	override fun navigate(host: NavigationHost, args: Bundle?): Fragment {
            return host.findOrCreate { HelpScreen(args!!.get("topic")) }
        }
    }
}
```

So you can instead use the class name as destination:

```kotlin
// The companion object implements `Destination`, so we can use it like this
host.navigateTo(HelpScreen)
```

Either way, you can customize what happens when the user tries to reach a certain screen in your app. Generally, you should implement all major destinations as custom Fragment subclasses with companion objects implementing `Destination`, and use the SAM constructor for dynamically-generated destinations.

###### Navigation Hosts

To allow the user to navigate through different screens without spawning new windows or creating tab layouts, we can use a `NavigationHost` component: a container widget that manages a collection of `Fragment` instances, usually referred to as “the stack” or the “navigation graph”.

When creating a navigation host, you need to provide a “home” destination, which will be the entry point for user navigation.

```kotlin
class MainWindow : Window("Hello World", 1280 by 720) {
    // Let the host handle the UX implementation.
	val host = navigationHost(HomeFragment)
    
    // other top-level app logic
    /* .... */
}
```

You can then use `navigateTo(Destination)` or `navigateUp()` to make your way around the graph, both methods are also available inside a fragment subclass without having to reference the host.

###### Fragments

A `Fragment` is a component that can be used in a modular way to allow the user to navigate through the application. The concept is similar to Android’s own Fragments. After they are no longer needed, fragments are destroyed, freeing any memory allocated using its local arena scope and destroying the root widget. Typically, a fragment implementation looks like this:

```kotlin
class SettingsFragment : Fragment() {
    
    private val username: Setting<String> by App.settings
    
    // Fragment implements AutoFreeScope, memory is freed once the fragment
    // is destroyed.
    private val dataStruct: CustomDataStruct = alloc { /*Init struct*/ }
    
    // Called the first time this fragment is attached to a graph
    override fun onAttached(){
        App.userNotices("terms_and_conditions").promptIfNeeded()
        CApi.callFunction(dataStruct)
    }
    
    // Called the first time this fragment is shown
    // The returned widget becomes the value for the "root" property
    override fun Container.createUI() = column {
        title("Settings")
        
        // Read/Write automatically from/to the username setting
        textField("username").bindTo(username)
        
        primaryButton("Ok").onClick { 
			saveSettings()
            // Instructs the navigation host to go back in the stack
            navigateUp()
        }
        
        button("Advanced").onClick {
            // Sets a new destination for the navigation host
            navigateTo(AdvancedSettings)
        }
    }
    
    // Called every time the user navigates to this fragment
    override fun onDisplay() {
        // Load the username from a database asynchronously
        Backend.load(username)
    }
    
    // Called when the user navigates out from the fragment
    override fun onLeave() {
        log("user left settings")
    }
    
    // Called once the fragment is no longer present in the
    // navigation stack.
    override fun onDestroy() {
        saveSettings()
		/* cleanup */
    }
    
    private fun saveSettings() {
		// Save the username to the database
        Backend.save(username)
    }
    
    // Expose the companion object as a destination
    public companion object : Destination {
        // Navigate to existing SettingsFragment or create one if none
        // is found.
        override fun navigate(
            host: NavigationHost,
            params: Bundle?
        ): Fragment {
            return host.findOrCreate { SettingsFragment() }
        }
    }
}
```

The life-cycle of a `Fragment` is managed by its host: first, the `NavigationHost` is asked to navigate to a new `Destination`, which produces a `Fragment` instance (or retrieves an existing one from the stack). You can use the life-cycle functions to respond to events throughout the life of the fragment.

- `onAttached`**:** The fragment has been added to a navigation stack. This method is called *only once* when the fragment is “attached” to the stack, but *before* the fragment’s `createUI` implementation. You may not access the `root` property within this method, since it has not been initialized yet.
- `createUI`**:** The navigation host asks the fragment to provide a UI rooted to its container. The `Widget` returned by this method becomes the value of the `root` property, accessible at any time from within the fragment. 
- `onDisplay`**:** The fragment is currently visible, the UI hierarchy has been setup and everything is ready for user interaction. Use this for last-moment UX checks and updates.
- `onLeave`**:** The user has navigated to a different fragment, this is also called before `onDestroy`. You can use this method to stop any functionality that is purely visual or meaningless to execute without the user’s attention.
- `onDestroy`**:** This fragment has been completely removed from the navigation stack and it will be destroyed to free memory and other resources. At this point, all memory allocated using the `AutoFreeScope` provided by the fragment is cleared.

## Settings and Preferences

Keeping track of user preferences and important data between sessions of your application can be largely automated without too much effort by integrating the Preferences API into your project. The idea is to define a series of `Setting` instances to be loaded from and saved to a database, which can be shared with the rest of your app-related data storage.

### Using the settings builder

`Setting` is an abstract class designed to adapt to your needs, so you can add your own validation and transformation logic to your project-specific settings, or you can use built-in utilities to create them in an idiomatic way:

```kotlin
val account by settings.register<String>("user_name")
	.default("New Account")

val password by settings.register<String>()
	.onValidate { ensureStrongPassword(it) }
	.onLoad { hashToPassword(it); log("Loading password...") }
	.onSave { passwordToHash(it); log("Saving password...") }
	.default { userNotices.credentials.prompt() }
```

The builder style allows to fine-tune each one of your settings to avoid the cumbersome manual implementation.

### Defining custom `Setting` sub-classes

**TBD**

### Persistence and storage methods

The `SettingsStorage` interface provides a base for the implementation of settings persistence through any desirable serialization method. Currently, only the `SQLiteStorage` and `JsonStorage` implementations are available.

#### Using SQLite

You can store your app settings in an automatically defined and managed table inside an existing database or a dedicated one. If you are already using an SQLite database with the `kotlin-sqlite` package, you will probably want to integrate both features to avoid the extra clutter.

**TBD: Using `SQLiteStorage`**

#### Using the Json format

To store your app settings using Json, simply import the corresponding package:

```kotlin
implementation("com.github.darvld:solid-settings-json:<version>")
```

And initialize a `JsonStorage` instance in your application class, passing in the path to the settings file. Since file I/O operations can potentially block the main thread, it is recommended to assign the `ioScheduler` to your `Backend` implementation, though it is not required (the default value is null, which simply blocks the thread until the operation is complete).

```kotlin
class MyApp : Application() {
    val settings = JsonStorage("/data/settings.json", ioScheduler=MyBackend)
    
    private val username by settings.register<String>("account_name")
    
    private fun saveUser() {
        settings.save(username)
    }
    
    private fun applyChanges() {
        settings.saveAll()
    }
}
```

`SettingsStorage.register` is an extension that provides a `Setting` builder and automatically registers it with the storage manager, so it is automatically saved and loaded when calling `SettingsStorage.loadAll()` or `SettingsStorage.saveAll()`. You can also use the `SettingsStorage` instance as a provider of registered settings, to avoid having to expose the individual properties to your fragments:

```kotlin
class SettingsFragment : Fragment() {
    private val username: Setting<String> by App.settings
    private val password: Setting<String> by App.settings
}
```

