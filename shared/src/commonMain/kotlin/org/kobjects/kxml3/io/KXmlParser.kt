package org.kobjects.kxml3.io

import org.xmlpull.v2.EventType
import org.xmlpull.v2.XmlPullParser
import org.xmlpull.v2.XmlPullParserException
import kotlin.Boolean
import kotlin.String


class KXmlParser(
    val source: String,
    val processNsp: Boolean = false,
    val relaxed: Boolean = false,
    val entityResolver: (String) -> String? = { null }
)  : XmlPullParser {

    companion object {
        const val CARRIAGE_RETURN_CODE = 13
        const val NEWLINE_CODE = 10

        const val UNEXPECTED_EOF = "Unexpected EOF"
        const val ILLEGAL_TYPE = "Wrong event type";

        fun <T> arraycopy(src: Array<T>, srcPos: Int, dst: Array<T>, dstPos: Int, count: Int) {
            src.copyInto(dst, dstPos, srcPos, srcPos + count)
        }

        fun arraycopy(src: IntArray, srcPos: Int, dst: IntArray, dstPos: Int, count: Int) {
            src.copyInto(dst, dstPos, srcPos, srcPos + count)
        }

        fun arraycopy(src: CharArray, srcPos: Int, dst: CharArray, dstPos: Int, count: Int) {
            src.copyInto(dst, dstPos, srcPos, srcPos + count)
        }


        private fun ensureCapacity(arr: Array<String>, required: Int): Array<String> {
            if (arr.size >= required) return arr
            val bigger =Array<String>(required + 16) { "" }
            arraycopy(arr, 0, bigger, 0, arr.size)
            return bigger
        }
    }


    // general

    override var depth = 0
    private var elementStack = Array(16) {""}
    private var nspStack = Array(16) {""}
    private var nspCounts = IntArray(8)

    private var token = false
    private var unresolved = false

    // source

    private var srcPos = 0
    private val srcLen = source.length

    override var lineNumber = 0
    override var columnNumber = 0

    // text buffer

    private var txtBuf = CharArray(128)
    private var txtPos = 0

    // event-related

    override var eventType = EventType.START_DOCUMENT
    override var isWhitespace = false
    override var namespace = ""
    override var prefix = ""
    override var name = ""
    override var lastError = ""

    override var isEmptyElementTag = false
    override var attributeCount = 0

    private var attributes = Array(16) {""}

    private val peek = IntArray(2)
    private var peekCount = 0
    private var wasCR = false

    private fun adjustNsp(): Boolean {
        var any = false
        var i = 0
        while (i < attributeCount shl 2) {

            // * 4 - 4; i >= 0; i -= 4) {
            var attrName = attributes[i + 2]
            val cut = attrName.indexOf(':')
            var prefix: String
            if (cut != -1) {
                prefix = attrName.substring(0, cut)
                attrName = attrName.substring(cut + 1)
            } else if (attrName == "xmlns") {
                prefix = attrName
                attrName = ""
            } else {
                i += 4
                continue
            }
            if (prefix != "xmlns") {
                any = true
            } else {
                val j = nspCounts[depth]++ shl 1
                nspStack = ensureCapacity(nspStack, j + 2)
                nspStack[j] = attrName
                nspStack[j + 1] = attributes[i + 3]
                if (attrName != "" && attributes[i + 3] == "") {
                    error("illegal empty namespace")
                }

                arraycopy(
                    attributes,
                    i + 4,
                    attributes,
                    i,
                    (--attributeCount shl 2) - i
                )
                i -= 4
            }
            i += 4
        }
        if (any) {
            i = (attributeCount shl 2) - 4
            while (i >= 0) {
                var attrName = attributes[i + 2]
                val cut = attrName.indexOf(':')
                if (cut == 0 && !relaxed) throw RuntimeException(
                    "illegal attribute name: $attrName at $this"
                ) else if (cut != -1) {
                    val attrPrefix = attrName.substring(0, cut)
                    attrName = attrName.substring(cut + 1)
                    val attrNs = getNamespace(attrPrefix)
                    if (attrNs == null && !relaxed) throw RuntimeException(
                        "Undefined Prefix: $attrPrefix in $this"
                    )
                    attributes[i] = attrNs ?: ""
                    attributes[i + 1] = attrPrefix
                    attributes[i + 2] = attrName
                }
                i -= 4
            }
        }
        val cut = name.indexOf(':')
        if (cut == 0) error("illegal tag name: $name")
        if (cut != -1) {
            prefix = name.substring(0, cut)
            name = name.substring(cut + 1)
        }
        val namespace = getNamespace(prefix)
        if (namespace == null && prefix != "") {
            error("undefined prefix: $prefix")
        }
        this.namespace = namespace ?: ""
        return any
    }

    private fun error(message: String) {
        lastError = message
        if (!relaxed) {
            throw exception(message)
        }
    }

    private fun exception(message: String): XmlPullParserException {
        return XmlPullParserException(message, positionDescription,
            lineNumber, columnNumber
        )
    }


    /**
     * common base for next and nextToken. Clears the state, except from
     * txtPos and whitespace. Does not set the type variable
     */
    private fun nextImpl() {
        if (eventType == EventType.END_TAG) {
            depth--
        }
        while (true) {
            attributeCount = -1

            // degenerated needs to be handled before error because of possible
            // processor expectations(!)
            if (isEmptyElementTag) {
                isEmptyElementTag = false
                eventType = EventType.END_TAG
                return
            }

            prefix = ""
            name = ""
            namespace = ""

            eventType = peekType()
            when (eventType) {
                EventType.ENTITY_REF -> {
                    pushEntity()
                    return
                }
                EventType.START_TAG -> {
                    parseStartTag(false)
                    return
                }
                EventType.END_TAG -> {
                    parseEndTag()
                    return
                }
                EventType.END_DOCUMENT -> return
                EventType.TEXT -> {
                    pushText('<'.code, !token)
                    if (depth == 0) {
                        if (isWhitespace) {
                            eventType = EventType.IGNORABLE_WHITESPACE
                        }
                    }
                    return
                }
                else -> {
                    eventType = parseLegacy(token)
                    if (eventType != EventType.XML_DECL) return
                }
            }
        }
    }


    private fun parseLegacy(push: Boolean): EventType {
        var push = push
        var req = ""
        val term: Int
        val result: EventType
        var prev = 0
        read() // <
        var c: Int = read()
        if (c == '?'.code) {
            if ((peek(0) == 'x'.code || peek(0) == 'X'.code)
                && (peek(1) == 'm'.code || peek(1) == 'M'.code)
            ) {
                if (push) {
                    push(peek(0))
                    push(peek(1))
                }
                read()
                read()
                if ((peek(0) == 'l'.code || peek(0) == 'L'.code) && peek(1) <= ' '.code) {
                    if (lineNumber != 1 || columnNumber > 4) error("PI must not start with xml")
                    parseStartTag(true)
                    if (attributeCount < 1 || "version" != attributes[2]) error("version expected")
             //       version = attributes[3]
                    var pos = 1
                    if (pos < attributeCount
                        && "encoding" == attributes[2 + 4]
                    ) {
               //         encoding = attributes[3 + 4]
                        pos++
                    }
                    if (pos < attributeCount
                        && "standalone" == attributes[4 * pos + 2]
                    ) {
                        val st = attributes[3 + 4 * pos]
                        if ("yes" == st) {
                            // standalone = true
                        } else if ("no" == st) {
                           // standalone = false
                        } else {
                            error("illegal standalone value: $st")
                        }
                        pos++
                    }
                    if (pos != attributeCount) error("illegal xmldecl")
                    isWhitespace = true
                    txtPos = 0
                    return EventType.XML_DECL
                }
            }

            /*            int c0 = read ();
                        int c1 = read ();
                        int */term = '?'.code
            result = EventType.PROCESSING_INSTRUCTION
        } else if (c == '!'.code) {
            if (peek(0) == '-'.code) {
                result = EventType.COMMENT
                req = "--"
                term = '-'.code
            } else if (peek(0) == '['.code) {
                result = EventType.CDSECT
                req = "[CDATA["
                term = ']'.code
                push = true
            } else {
                result = EventType.DOCDECL
                req = "DOCTYPE"
                term = -1
            }
        } else {
            error("illegal: <$c")
            return EventType.COMMENT
        }
        for (i in 0 until req.length) read(req[i])
        if (result == EventType.DOCDECL) parseDoctype(push) else {
            while (true) {
                c = read()
                if (c == -1) {
                    error(UNEXPECTED_EOF)
                    return EventType.COMMENT
                }
                if (push) push(c)
                if ((term == '?'.code || c == term)
                    && peek(0) == term && peek(1) == '>'.code
                ) break
                prev = c
            }
            if (term == '-'.code && prev == '-'.code && !relaxed) error("illegal comment delimiter: --->")
            read()
            read()
            if (push && term != '?'.code) txtPos--
        }
        return result
    }


    /** precondition: &lt! consumed  */
    private fun parseDoctype(push: Boolean) {
        var nesting = 1
        var quoted = false

        // read();
        while (true) {
            val i: Int = read()
            when (i) {
                -1 -> {
                    error(UNEXPECTED_EOF)
                    return
                }
                '\''.code -> quoted = !quoted
                '<'.code -> if (!quoted) nesting++
                '>'.code -> if (!quoted) {
                    if (--nesting == 0) return
                }
            }
            if (push) push(i)
        }
    }


    /* precondition: &lt;/ consumed */
    private fun parseEndTag() {
        read() // '<'
        read() // '/'
        name = readName()
        skip()
        read('>')
        val sp = depth - 1 shl 2
        if (depth == 0) {
            error("element stack empty")
            eventType = EventType.COMMENT
            return
        }
        if (!relaxed) {
            if (name != elementStack[sp + 3]) {
                error("expected: /" + elementStack[sp + 3] + " read: " + name)

                // become case insensitive in relaxed mode

//            int probe = sp;
//            while (probe >= 0 && !name.toLowerCase().equals(elementStack[probe + 3].toLowerCase())) {
//                stackMismatch++;
//                probe -= 4;
//            }
//
//            if (probe < 0) {
//                stackMismatch = 0;
//                //			text = "unexpected end tag ignored";
//                eventType = COMMENT;
//                return;
//            }
            }
            namespace = elementStack[sp]
            prefix = elementStack[sp + 1]
            name = elementStack[sp + 2]
        }
    }


    private fun peekType(): EventType {
        return when (peek(0)) {
            -1 -> EventType.END_DOCUMENT
            '&'.code -> EventType.ENTITY_REF
            '<'.code -> when (peek(1)) {
                '/'.code -> EventType.END_TAG
                // Could be XML_DECL, too, but doesn't matter here.
                '?'.code -> EventType.PROCESSING_INSTRUCTION
                '!'.code -> if (peek(2) == '-'.code) EventType.COMMENT else EventType.DOCDECL
                else -> EventType.START_TAG
            }
            else -> EventType.TEXT
        }
    }

    private fun get(pos: Int): String {
        return txtBuf.concatToString(pos, txtPos - pos)
    }

    private fun push(c: Int) {
        isWhitespace = isWhitespace and (c <= ' '.code)
        if (txtPos + 1 >= txtBuf.size) { // +1 to have enough space for 2 surrogates, if needed
            val bigger = CharArray(txtPos * 4 / 3 + 4)
            arraycopy(txtBuf, 0, bigger, 0, txtPos)
            txtBuf = bigger
        }
        if (c > 0xffff) {
            // write high Unicode value as surrogate pair
            val offset = c - 0x010000
            txtBuf[txtPos++] = ((offset ushr 10) + 0xd800).toChar() // high surrogate
            txtBuf[txtPos++] = ((offset and 0x3ff) + 0xdc00).toChar() // low surrogate
        } else {
            txtBuf[txtPos++] = c.toChar()
        }
    }

    /** Sets name and attributes  */
    private fun parseStartTag(xmldecl: Boolean) {
        if (!xmldecl) read()
        name = readName()
        attributeCount = 0
        while (true) {
            skip()
            val c = peek(0)
            if (xmldecl) {
                if (c == '?'.code) {
                    read()
                    read('>')
                    return
                }
            } else {
                if (c == '/'.code) {
                    isEmptyElementTag = true
                    read()
                    skip()
                    read('>')
                    break
                }
                if (c == '>'.code && !xmldecl) {
                    read()
                    break
                }
            }
            if (c == -1) {
                error(UNEXPECTED_EOF)
                return
            }
            val attrName = readName()
            if (attrName.length == 0) {
                error("attr name expected")
                break
            }
            var i = attributeCount++ shl 2
            attributes = ensureCapacity(attributes, i + 4)
            attributes[i++] = ""
            attributes[i++] = ""
            attributes[i++] = attrName
            skip()
            if (peek(0) != '='.code) {
                if (!relaxed) {
                    error("Attr.value missing f. $attrName")
                }
                attributes[i] = attrName
            } else {
                read('=')
                skip()
                var delimiter = peek(0)
                if (delimiter != '\''.code && delimiter != '"'.code) {
                    if (!relaxed) {
                        error("attr value delimiter missing!")
                    }
                    delimiter = ' '.code
                } else read()
                val p = txtPos
                pushText(delimiter, true)
                attributes[i] = get(p)
                txtPos = p
                if (delimiter != ' '.code) read() // skip endquote
            }
        }
        val sp = depth++ shl 2
        elementStack = ensureCapacity(elementStack, sp + 4)
        elementStack[sp + 3] = name
        if (depth >= nspCounts.size) {
            val bigger = IntArray(depth + 4)
            arraycopy(nspCounts, 0, bigger, 0, nspCounts.size)
            nspCounts = bigger
        }
        nspCounts[depth] = nspCounts[depth - 1]

        if (processNsp) {
            adjustNsp()
        } else {
            namespace = ""
        }
        elementStack[sp] = namespace
        elementStack[sp + 1] = prefix
        elementStack[sp + 2] = name
    }

    /**
     * result: isWhitespace; if the setName parameter is set,
     * the name of the entity is stored in "name"
     */
    private fun pushEntity() {
        push(read()) // &
        val pos = txtPos
        while (true) {
            val c = peek(0)
            if (c == ';'.code) {
                read()
                break
            }
            if (c < 128 && (c < '0'.code || c > '9'.code)
                && (c < 'a'.code || c > 'z'.code)
                && (c < 'A'.code || c > 'Z'.code)
                && c != '_'.code && c != '-'.code && c != '#'.code
            ) {
                if (!relaxed) {
                    error("unterminated entity ref")
                }
                println("broken entitiy: " + get(pos - 1))

                //; ends with:"+(char)c);
//                if (c != -1)
//                    push(c);
                return
            }
            push(read())
        }
        val code = get(pos)
        txtPos = pos - 1
        if (token && eventType === EventType.ENTITY_REF) {
            name = code
        }
        if (code[0] == '#') {
            val c = if (code[1] == 'x') code.substring(2).toInt(16) else code.substring(1).toInt()
            push(c)
            return
        }
        val result = when (code) {
            "amp" -> "&"
            "apos" -> "'"
            "gt" -> ">"
            "lt" -> "<"
            "quot" -> "\""
            else -> entityResolver(code)
        }
        unresolved = result == null
        if (result == null) {
            if (!token) error("unresolved: &$code;")
        } else {
            for (i in 0 until result.length) push(result[i].code)
        }
    }

    /** types:
     * '<': parse to any token (for nextToken ())
     * '"': parse to quote
     * ' ': parse to whitespace or '>'
     */
    private fun pushText(delimiter: Int, resolveEntities: Boolean) {
        var next = peek(0)
        var cbrCount = 0
        while (next != -1 && next != delimiter) { // covers eof, '<', '"'
            if (delimiter == ' '.code) if (next <= ' '.code || next == '>'.code) break
            if (next == '&'.code) {
                if (!resolveEntities) break
                pushEntity()
            } else if (next == NEWLINE_CODE && eventType === EventType.START_TAG) {
                read()
                push(' '.code)
            } else push(read())
            if (next == '>'.code && cbrCount >= 2 && delimiter != ']'.code) {
                error("Illegal: ]]>")
            }
            if (next == ']'.code) cbrCount++ else cbrCount = 0
            next = peek(0)
        }
    }

    private fun read(c: Char) {
        val a = read()
        if (a != c.code) error("expected: '" + c + "' actual: '" + a.toChar() + "'")
    }

    // delegates to peek as peek handles cr/lf normalization.
    private fun read(): Int {
        val result: Int
        if (peekCount == 0) result = peek(0) else {
            result = peek[0]
            peek[0] = peek[1]
        }
        //		else {
        //			result = peek[0];
        //			System.arraycopy (peek, 1, peek, 0, peekCount-1);
        //		}
        peekCount--
        columnNumber++
        if (result == NEWLINE_CODE) {
            lineNumber++
            columnNumber = 1
        }
        return result
    }

    /** Does never read more than needed  */
    private fun peek(pos: Int): Int {
        while (pos >= peekCount) {
            val nw: Int = if (srcPos == srcLen) -1 else source[srcPos++].code
            if (nw == CARRIAGE_RETURN_CODE) {
                wasCR = true
                peek[peekCount++] = NEWLINE_CODE
            } else {
                if (nw == NEWLINE_CODE) {
                    if (!wasCR) {
                        peek[peekCount++] = NEWLINE_CODE
                    }
                } else {
                    peek[peekCount++] = nw
                }
                wasCR = false
            }
        }
        return peek[pos]
    }

    private fun readName(): String {
        val pos = txtPos
        var c = peek(0)
        if ((c < 'a'.code || c > 'z'.code)
            && (c < 'A'.code || c > 'Z'.code)
            && c != '_'.code && c != ':'.code && c < 0x0c0 && !relaxed
        ) error("name expected")
        do {
            push(read())
            c = peek(0)
        } while (c >= 'a'.code && c <= 'z'.code
            || c >= 'A'.code && c <= 'Z'.code
            || c >= '0'.code && c <= '9'.code
            || c == '_'.code || c == '-'.code || c == ':'.code || c == '.'.code || c >= 0x0b7
        )
        val result = get(pos)
        txtPos = pos
        return result
    }

    private fun skip() {
        while (true) {
            val c = peek(0)
            if (c > ' '.code || c == -1) break
            read()
        }
    }

    // public part starts here...

    /* turn into ctor!
    fun setInput(reader: Reader) {
        this.reader = reader
        line = 1
        column = 0
        eventType = EventType.START_DOCUMENT
        name = ""
        namespace = ""
        isEmptyElementTag = false
        attributeCount = -1

        if (reader == null) return
        srcPos = 0
        srcCount = 0
        peekCount = 0
        depth = 0
    }

    @Throws(XmlPullParserException::class)
    fun setInput(`is`: InputStream?, _enc: String) {
        srcPos = 0
        srcCount = 0
        var enc = _enc
        requireNotNull(`is`)
        try {
            if (enc == null) {
                // read four bytes
                var chk = 0
                while (srcCount < 4) {
                    val i: Int = `is`.read()
                    if (i == -1) break
                    chk = chk shl 8 or i
                    srcBuf[srcCount++] = i.toChar()
                }
                if (srcCount == 4) {
                    when (chk) {
                        0x00000FEFF -> {
                            enc = "UTF-32BE"
                            srcCount = 0
                        }
                        -0x20000 -> {
                            enc = "UTF-32LE"
                            srcCount = 0
                        }
                        0x03c -> {
                            enc = "UTF-32BE"
                            srcBuf[0] = '<'
                            srcCount = 1
                        }
                        0x03c000000 -> {
                            enc = "UTF-32LE"
                            srcBuf[0] = '<'
                            srcCount = 1
                        }
                        0x0003c003f -> {
                            enc = "UTF-16BE"
                            srcBuf[0] = '<'
                            srcBuf[1] = '?'
                            srcCount = 2
                        }
                        0x03c003f00 -> {
                            enc = "UTF-16LE"
                            srcBuf[0] = '<'
                            srcBuf[1] = '?'
                            srcCount = 2
                        }
                        0x03c3f786d -> {
                            while (true) {
                                val i: Int = `is`.read()
                                if (i == -1) break
                                srcBuf[srcCount++] = i.toChar()
                                if (i == '>'.code) {
                                    val s = String(srcBuf, 0, srcCount)
                                    var i0 = s.indexOf("encoding")
                                    if (i0 != -1) {
                                        while (s[i0] != '"'
                                            && s[i0] != '\''
                                        ) i0++
                                        val deli = s[i0++]
                                        val i1 = s.indexOf(deli, i0)
                                        enc = s.substring(i0, i1)
                                    }
                                    break
                                }
                            }
                            if (chk and -0x10000 == -0x1010000) {
                                enc = "UTF-16BE"
                                srcBuf[0] = (srcBuf[2] shl 8 or srcBuf[3]) as Char
                                srcCount = 1
                            } else if (chk and -0x10000 == -0x20000) {
                                enc = "UTF-16LE"
                                srcBuf[0] = (srcBuf[3] shl 8 or srcBuf[2]) as Char
                                srcCount = 1
                            } else if (chk and -0x100 == -0x10444100) {
                                enc = "UTF-8"
                                srcBuf[0] = srcBuf[3]
                                srcCount = 1
                            }
                        }
                        else -> if (chk and -0x10000 == -0x1010000) {
                            enc = "UTF-16BE"
                            srcBuf[0] = (srcBuf[2] shl 8 or srcBuf[3]) as Char
                            srcCount = 1
                        } else if (chk and -0x10000 == -0x20000) {
                            enc = "UTF-16LE"
                            srcBuf[0] = (srcBuf[3] shl 8 or srcBuf[2]) as Char
                            srcCount = 1
                        } else if (chk and -0x100 == -0x10444100) {
                            enc = "UTF-8"
                            srcBuf[0] = srcBuf[3]
                            srcCount = 1
                        }
                    }
                }
            }
            if (enc == null) enc = "UTF-8"
            val sc = srcCount
            setInput(InputStreamReader(`is`, enc))
            encoding = _enc
            srcCount = sc
        } catch (e: Exception) {
            throw XmlPullParserException(
                "Invalid stream or encoding: $e",
                this,
                e
            )
        }
    }
     */

    override fun getNamespaceCount(depth: Int): Int {
        if (depth > this.depth) throw IndexOutOfBoundsException()
        return nspCounts[depth]
    }

    override fun getNamespacePrefix(pos: Int): String {
        return nspStack[pos shl 1]
    }

    override fun getNamespaceUri(pos: Int): String {
        return nspStack[(pos shl 1) + 1]
    }

    override val positionDescription: String
        get() {
        val buf = StringBuilder(eventType.name)
        buf.append(' ')
        if (eventType == EventType.START_TAG || eventType == EventType.END_TAG) {
            if (isEmptyElementTag) {
                buf.append("(empty) ")
            }
            buf.append('<')
            if (eventType == EventType.END_TAG) buf.append('/')
            if (prefix != "") buf.append("{$namespace}$prefix:")
            buf.append(name)
            val cnt = attributeCount shl 2
            var i = 0
            while (i < cnt) {
                buf.append(' ')
                if (attributes[i + 1] != "") buf.append("{" + attributes[i] + "}" + attributes[i + 1] + ":")
                buf.append(attributes[i + 2] + "='" + attributes[i + 3] + "'")
                i += 4
            }
            buf.append('>')
        } else if (eventType == EventType.IGNORABLE_WHITESPACE) {

        } else if (eventType != EventType.TEXT) {
            buf.append(text)
        } else if (isWhitespace) buf.append("(whitespace)") else {
            var text = text
            if (text.length > 16) text = text.substring(0, 16) + "..."
            buf.append(text)
        }
        buf.append("@$lineNumber:$columnNumber")
        return buf.toString()
    }


    override val text: String
    get() {
        return if (eventType < EventType.TEXT
            || eventType == EventType.ENTITY_REF && unresolved
        ) "" else get(0)
    }

    override fun getAttributeType(index: Int): String {
        return "CDATA"
    }

    override fun isAttributeDefault(index: Int): Boolean {
        return false
    }

    override fun getAttributeNamespace(index: Int): String {
        if (index >= attributeCount) throw IndexOutOfBoundsException()
        return attributes[index shl 2]
    }

    override fun getAttributeName(index: Int): String {
        if (index >= attributeCount) throw IndexOutOfBoundsException()
        return attributes[(index shl 2) + 2]
    }

    override fun getAttributePrefix(index: Int): String {
        if (index >= attributeCount) throw IndexOutOfBoundsException()
        return attributes[(index shl 2) + 1]
    }

    override fun getAttributeValue(index: Int): String {
        if (index >= attributeCount) throw IndexOutOfBoundsException()
        return attributes[(index shl 2) + 3]
    }

    override fun getAttributeValue(namespace: String, name: String): String? {
        var i = (attributeCount shl 2) - 4
        while (i >= 0) {
            if (attributes[i + 2] == name
                && (namespace.isEmpty() || attributes[i].equals(namespace))
            ) return attributes[i + 3]
            i -= 4
        }
        return null
    }

    override operator fun next(): EventType {
        txtPos = 0
        isWhitespace = true
        var minType = EventType.XML_DECL
        token = false
        do {
            nextImpl()
            if (eventType < minType) {
                minType = eventType
            }
        } while (minType > EventType.ENTITY_REF // ignorable
            || minType >= EventType.TEXT && peekType() >= EventType.TEXT
        )
        eventType = minType
        if (eventType > EventType.TEXT) {
            eventType = EventType.TEXT
        }
        return eventType
    }

    override fun nextToken(): EventType {
        isWhitespace = true
        txtPos = 0
        token = true
        nextImpl()
        return eventType
    }


    //
    // utility methods to make XML parsing easier ...
    override fun nextTag(): EventType {
        next()
        if (eventType == EventType.TEXT && isWhitespace) next()
        if (eventType !== EventType.END_TAG && eventType !== EventType.START_TAG) {
            throw exception("unexpected event type")
        }
        return eventType
    }

    override fun require(type: EventType?, namespace: String?, name: String?) {
        if (type != null && eventType != type || namespace != null && namespace != this.namespace
            || name != null && name != this.name) {
            throw exception("expected: $type {$namespace}$name")
        }
    }

    override fun nextText(): String {
        if (eventType !== EventType.START_TAG) exception("precondition: START_TAG")
        next()
        val result: String
        if (eventType === EventType.TEXT) {
            result = text
            next()
        } else result = ""
        if (eventType !== EventType.END_TAG) exception("END_TAG expected")
        return result
    }


}