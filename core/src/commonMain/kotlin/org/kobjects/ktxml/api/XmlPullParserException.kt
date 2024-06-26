package org.kobjects.ktxml.api

/** Exception thrown in case of parsing errors, containing position information. */
class XmlPullParserException(
    message: String,
    positionDescription: String,
    val lineNumber : Int = -1,
    val columnNumber : Int = -1,
) : RuntimeException("$message\nPosition: $positionDescription") {
}