package org.kobjects.ktxml

import org.kobjects.ktxml.api.EventType
import org.kobjects.ktxml.mini.MiniXmlPullParser
import kotlin.test.Test
import kotlin.test.assertEquals

class KtXmlTest {

    @Test
    fun testBasicParsing() {
        val parser = MiniXmlPullParser("<text>Hello World</text>".iterator())
        assertEquals(EventType.START_DOCUMENT, parser.eventType)
        assertEquals(EventType.START_TAG, parser.next())
        assertEquals("Hello World", parser.nextText())
        assertEquals(EventType.END_TAG, parser.eventType)
        assertEquals(EventType.END_DOCUMENT, parser.next())
    }
}