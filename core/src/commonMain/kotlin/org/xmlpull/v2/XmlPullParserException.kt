package org.xmlpull.v2

class XmlPullParserException(
    message: String,
    positionDescription: String,
    val lineNumber : Int = -1,
    val columnNumber : Int = -1,
) : RuntimeException("$message\nPosition: $positionDescription") {
}