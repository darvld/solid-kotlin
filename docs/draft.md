# <img src="G:\Kotlin\projects\solid-kotlin\docs\resources\solid_logo.png" alt="solid_logo" style="zoom:40%;" />  Solid-Kotlin

A **Kotlin/Native** library binding for the **Solid** UI backend

[toc]

## The Widget system

Solid’s class structure is based on “widgets”: objects with or without graphic representation that compose the User Interface. Widgets are the building blocks of Solid and its base library: **FLTK**.

### Loading UI definition files

If you need to share the same UI hierarchy and design between multiple languages and applications, you can use Solid’s UI definition file format, and instantiate the widgets in runtime using the WidgetBuilder interface. You can then find any widget in the hierarchy by their name and path, as defined in the UI file.

### Creating UI from code

The widget design API for Solid-Kotlin is meant to blend in seamlessly with Kotlin-style code: the ui components are declared and configured using a custom DSL, so they look pretty much like Qt’s QML.

```kotlin
/** Create a basic window with a couple of controls*/
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
fun createUI(container: Container) = container.apply { // this: Container
    button("Ok", name="ok_button"){
        TODO()
    }
    val user = textField("Username:", name="username_field") {
        type = InputType.EMAIL
    }
}

fun useUI(window: Window) {
	createUI(window)
    
    val button: Button = window.getChild("ok_button")
}
```

A widget’s name is always the last parameter for a constructor (except for lambdas). This is done by design to avoid confusing label-related parameters with the widget’s name parameter.

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
    val handler: SignalHandler = textField("Say something").onClick {
        // Implicit parameter "it" of type TextField
        println("You said: $it.text")
    }
    
    // You can create disposable handlers: they will automatically disconnect
    // after a certain amount of calls (defaults to 1):
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

Keep in mind that in runtime, Kotlin prohibits type checks and casts on generic types like `Signal<T>` and `SignalHandler<T>`, so if your code needs to dynamically handle different signal and handler types, there are a few restrictions. The best way to get around the issue is to use type projections like this: 

 ```kotlin
// incorrect: invariant type parameter
val badSignals: MutableList<Signal<Widget>> = mutableListOf()

// correct: projected type parameter
val signals: MutableList<Signal<out Widget>> = mutableListOf()

val button = button("Click me!")
 
// Error: Signal<Widget> expected but inferred type is Signal<Button>
badSignals.add(button.onClick)

// This is ok
signals.add(button.onClick)
 ```

### Sharing widgets with other language bindings

Since **Solid** is a multiplatform, poliglot library, interaction between bindings created by different languages are part of its API. However, special care must be taken when working in a multi-language environment, or an environment with multiple runtimes not allowing instance sharing, like **Kotlin/Native**'s runtime.

In their original C++ class definitions, Solid's widgets reserve a void pointer field for language-specific wrappers, Kotlin bindings use that field to store a `StableRef`.
This creates a problem, though: if a Kotlin widget wrapper is created around a pointer to a widget that was previously wrapped by another language (or a different Kotlin runtime), the wrapping/unwrapping of the `StableRef `will fail. To solve this, Kotlin wrappers can be constructed in three different ways:

1. By manually using a constructor call that creates a *new* widget. Widgets created this way are wrapped automatically, and so their pointer fields are used by the Kotlin runtime that created them (the owner).

2. By using `Widget.wrap(COpaquePointer)` companion function or one of its variants, they take a widget pointer and attempt to wrap around it:
    - If the bindingPointer field is `null`, a `StableRef` is stored in it and the event handler is setup. The widget is then officially owned by this runtime.
    - If the bindingPointer or the eventHandler fields are not `null`, the function fails with an exception of type `WidgetWrappingException`. These exceptions store the original widget’s pointer, so you can try a different approach if you catch it.

    As an alternative, `Widget.tryWrap(COpaquePointer)` could be used to avoid the exception, however the returned value will be of type `Widget?` (nullable). This is the method used by most functions and value getters in the library, to avoid returning wrappers to widgets not owned by the runtime.
    
    Another way is to use `Widget.maybeWrap(COpaquePointer)`, which attempts to wrap the widget pointer and upon failure, returns an unwrapped widget, as described below. To find out whether the bindings were created and stored, use the ` Widget.isWrapper` property.
    
6. By calling a constructor which takes only a pointer as argument. This way, no wrappers will be stored and you can use the widget, but you won't receive any events. This allows to respect other language bindings while still using the widget's methods and properties.

```kotlin
fun createWidget(pointer: COpaquePointer?, example: WrapperExample): Widget {
    return when(example) {
        Create -> {
            // Always wraps the widget, it is owned by this runtime
            MyWidget("Hello")
        }
        Wrap -> {
            // Returns the Widget or fails with a WidgetBindingException if
            // a wrapper from a different runtime/language already owns the widget
            Widget.wrap(pointer)
        }
        TryWrap -> {
            // Returns null if the wrapping fails, so a null cast is required
            // to return Widget here
            Widget.tryWrap(pointer)!!
        }
        MaybeWrap -> {
            // Always returns a Widget, but if wrapping failed we won't be able 
            // to receive events, use this only for internal/final parts of your
            // code, or otherwise warn the code's user of the limitations
            Widget.maybeWrap(pointer).also { it: Widget->
                // Use this to check whether the widget was successfully wrapped
                if(it.isWrapper)
                	TODO()
                else
                    TODO()
            }
        }
        NoWrap -> {
            // Note that this widget wrapper will not receive any events. Some
            // other limitations may apply in the future since the Kotlin runtime
            // does not own the widget
            MyWidget(pointer)
        }
    }
}
```

> Do not try to take over widgets you don't own, this can cause the original owner's code to misbehave. As a general rule, if you need to transfer ownership, be polite and establish an API to exchange/share widgets instead.

## Navigation and the Fragment system