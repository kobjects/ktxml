package org.kobjects.ktxml

import org.kobjects.ktxml.api.EventType
import org.kobjects.ktxml.mini.MiniXmlPullParser
import kotlin.test.Test
import kotlin.test.assertEquals

class KtXmlTest {

    @Test
    fun testBasicParsing() {
        val parser = MiniXmlPullParser("<text>Hello&#32;World</text>".iterator())
        assertEquals(EventType.START_DOCUMENT, parser.eventType)
        assertEquals(EventType.START_TAG, parser.next())
        assertEquals("Hello World", parser.nextText())
        assertEquals(EventType.END_TAG, parser.eventType)
        assertEquals(EventType.END_DOCUMENT, parser.next())
    }

    @Test
    fun testCDATAParsing() {
        val parser = MiniXmlPullParser("<text><![CDATA[Hello World]]></text>".iterator())
        assertEquals(EventType.START_DOCUMENT, parser.eventType)
        assertEquals(EventType.START_TAG, parser.next())
        assertEquals("Hello World", parser.nextText())
        assertEquals(EventType.END_TAG, parser.eventType)
        assertEquals(EventType.END_DOCUMENT, parser.next())
    }

    @Test
    fun testAdvancedCDATAParsing() {
        val parser = MiniXmlPullParser("<test><![CDATA[Test 1</test><test><garbage/>Test 2]]></test>".iterator())
        assertEquals(EventType.START_DOCUMENT, parser.eventType)
        assertEquals(EventType.START_TAG, parser.next())
        assertEquals("Test 1</test><test><garbage/>Test 2", parser.nextText())
        assertEquals(EventType.END_TAG, parser.eventType)
        assertEquals(EventType.END_DOCUMENT, parser.next())
    }
}