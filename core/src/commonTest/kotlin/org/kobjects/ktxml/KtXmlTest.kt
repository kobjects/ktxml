package org.kobjects.ktxml

import org.kobjects.ktxml.api.EventType
import org.kobjects.ktxml.mini.MiniXmlPullParser
import kotlin.test.Test
import kotlin.test.assertEquals

class KtXmlTest {

    @Test
    fun testBasicParsing() {
        val parser = MiniXmlPullParser("<text>Hello&#32;World</text>")
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

    @Test
    fun testXmlDecl() {
        val parser = MiniXmlPullParser("<?XML version=\"1.0\" ?><test/>".iterator())
        assertEquals(EventType.START_DOCUMENT, parser.eventType)
        // XML_DECL reporting was fixed in version 0.3.0
        assertEquals(EventType.XML_DECL, parser.nextToken())
        assertEquals(EventType.START_TAG, parser.nextToken())
        assertEquals(EventType.END_TAG, parser.nextToken())
        assertEquals(EventType.END_DOCUMENT, parser.nextToken())
    }

    @Test
    fun testXmlDecl2() {
        val parser = MiniXmlPullParser("""<?xml version="1.0" encoding="UTF-8"?><rss version="2.0"
	xmlns:content="http://purl.org/rss/1.0/modules/content/"
	xmlns:wfw="http://wellformedweb.org/CommentAPI/"
	xmlns:dc="http://purl.org/dc/elements/1.1/"
	xmlns:atom="http://www.w3.org/2005/Atom"
	xmlns:sy="http://purl.org/rss/1.0/modules/syndication/"
	xmlns:slash="http://purl.org/rss/1.0/modules/slash/" />
""".iterator())
        assertEquals(EventType.START_DOCUMENT, parser.eventType)
        assertEquals(EventType.XML_DECL, parser.nextToken())
        assertEquals(EventType.START_TAG, parser.nextToken())
        assertEquals(EventType.END_TAG, parser.nextToken())
        assertEquals(EventType.END_DOCUMENT, parser.nextToken())
    }
}