package org.xmlpull.v2

/**
 * The parser you are using will provide an
 *  Implementations may support additional features.
 */
interface XmlPullFactory {
    /**
     * Set to true to request validation. False by default.
     */
    var validating: Boolean

    /**
     * Set to true to request namespace processing. False by default.
     */
    var namespaceAware: Boolean

    /**
     * Custom function to resolve entities. The function returns null for all entities by default.
     */
    var entityResolver: (String) -> String?

    /**
     * Set to true to suppress exceptions during parsing. Might be useful to make sense
     * of HTML documents in limited circumstances where
     */
    var relaxed: Boolean

    /**
     * Creates a parser operating on the given string.
     */
    fun createParser(source: String): XmlPullParser

}