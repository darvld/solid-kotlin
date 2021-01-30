package solid

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlin.native.ref.WeakReference

public typealias SolidWidget = COpaquePointer?

public inline fun <reified T : Any> T.weakReference(): WeakReference<T> = WeakReference(this)