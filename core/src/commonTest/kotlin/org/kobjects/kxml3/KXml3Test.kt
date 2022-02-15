package org.kobjects.kxml3

import org.xmlpull.v2.EventType
import kotlin.test.Test
import kotlin.test.assertEquals

class KXml3Test {

    @Test
    fun testBasicParsing() {
        val parser = KXmlFactory().createParser("<text>Hello World</text>")
        assertEquals(EventType.START_DOCUMENT, parser.eventType)
        assertEquals(EventType.START_TAG, parser.next())
        assertEquals("Hello World", parser.nextText())
        assertEquals(EventType.END_TAG, parser.eventType)
        assertEquals(EventType.END_DOCUMENT, parser.next())
    }
}