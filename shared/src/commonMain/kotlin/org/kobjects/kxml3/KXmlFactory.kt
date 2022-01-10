package org.kobjects.kxml3

import org.kobjects.kxml3.io.KXmlParser
import org.xmlpull.v2.XmlPullFactory
import org.xmlpull.v2.XmlPullParser
import java.io.InputStream
import java.io.Reader


class KXmlFactory(
    override var validating: Boolean = false,
    override var namespaceAware: Boolean = false,
    override var entityResolver: (String) -> String? = { it -> null },
    override var relaxed: Boolean = false
) : XmlPullFactory {
    override fun createParser(reader: Reader) = KXmlParser(
        reader = reader,
        processNsp = namespaceAware,
        entityResolver = entityResolver,
        relaxed = relaxed)

    override fun createParser(inputStream: InputStream): XmlPullParser {
        TODO("Not yet implemented")
    }

}