package org.kobjects.kxml3

import org.kobjects.kxml3.io.KXmlParser
import org.xmlpull.v2.XmlPullFactory


class KXmlFactory(
    override var validating: Boolean = false,
    override var namespaceAware: Boolean = false,
    override var entityResolver: (String) -> String? = { it -> null },
    override var relaxed: Boolean = false
) : XmlPullFactory {
    override fun createParser(source: String) = KXmlParser(
        source = source,
        processNsp = namespaceAware,
        entityResolver = entityResolver,
        relaxed = relaxed)


}