package solid

import kotlinx.cinterop.COpaquePointer

public class WidgetWrapException(
    public val externPointer: COpaquePointer?,
    message: String? = null,
    cause: Throwable? = null
) : Throwable(message, cause)