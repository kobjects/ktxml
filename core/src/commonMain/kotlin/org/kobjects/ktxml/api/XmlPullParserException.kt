package org.kobjects.ktxml.api

class XmlPullParserException(
    message: String,
    positionDescription: String,
    val lineNumber : Int = -1,
    val columnNumber : Int = -1,
) : RuntimeException("$message\nPosition: $positionDescription") {
}