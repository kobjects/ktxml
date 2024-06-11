package org.kobjects.ktxml.api

/**
 * KtXmplParser is an interface that defines parsing functionality based on the XMLPULL V2 API,
 * adapted to Kotlin.
 *
 * There are two key methods: [next] and [nextToken]. While next() provides access to high level
 * parsing events, nextToken() allows access to lower level tokens.
 *
 * The current event state of the parser can be determined by reading the [eventType] property.
 * Initially, the parser is in the [START_DOCUMENT] state.
 *
 * The method next() advances the parser to the next event. The enum value returned from next()
 * determines the current parser state and is identical to the value returned from following calls
 * to eventType.
 *
 * Th following event types are seen by next():
 *
 * - [EventType.START_TAG]: An XML start tag was read.
 * - [EventType.TEXT]: Text content was read. The text content can be retrieved using the [text]
 *      property (when in validating mode, [next] will not report ignorable whitespaces, use
 *      [nextToken] instead).
 * - [EventType.END_TAG]: An end tag was read.
 * - [EventType.END_DOCUMENT]: No more events are available
 *
 *
 * @author [Stefan Haustein](http://www.stefan-haustein.com)
 * @author [Aleksander Slominski](http://www.extreme.indiana.edu/~aslom/)
 */
interface XmlPullParser {
    val lastError: String

    /**
     * Returns the numbers of elements in the namespace stack for the give depth.
     * If namespaces are not enabled, 0 is returned.
     *
     * **NOTE:** when the parser is on END_TAG, then it is allowed to call
     * this function with getDepth()+1 argument to retrieve positions of namespace
     * prefixes and URIs that were declared on corresponding START_TAG.
     *
     * **NOTE:** to retrieve list of namespaces declared in current element:
     * ```
     * val pp = ...
     * val nsStart = pp.getNamespaceCount(pp.getDepth()-1)
     * val nsEnd = pp.getNamespaceCount(pp.getDepth())
     * for (i in nsStart until nsEnd) {
     *   val prefix = pp.getNamespacePrefix(i);
     *   val ns = pp.getNamespaceUri(i);
     * // ...
     * }
     * ```
     */
    fun getNamespaceCount(depth: Int): Int

    /**
     * Returns the namespace prefix for the given position in the namespace stack.
     * Default namespace declaration (xmlns='...') will have an empty prefix.
     * If the given index is out of range, an exception is thrown.
     *
     * **Please note:** when the parser is on an END_TAG, namespace prefixes that were declared
     * in the corresponding START_TAG are still accessible although they are no longer in scope.
     */
    fun getNamespacePrefix(pos: Int): String

    /**
     * Returns the namespace URI for the given position in the namespace stack.
     * If the position is out of range, an exception is thrown.
     *
     * **NOTE:** when parser is on END_TAG then namespace prefixes that were declared
     * in corresponding START_TAG are still accessible even though they are not in scope
     */
    fun getNamespaceUri(pos: Int): String

    /**
     * Returns the URI corresponding to the given prefix, depending on current state of the parser.
     *
     * If the prefix was not declared in the current scope, null is returned. The default namespace
     * is included in the namespace table and is available via getNamespace(null).
     *
     * This method is a convenience method for
     * ```
     * for (int i = getNamespaceCount(getDepth ())-1; i >= 0; i--) {
     *   if (getNamespacePrefix(i).equals( prefix )) {
     *     return getNamespaceUri(i);
     *   }
     * }
     * return null;
     * ```
     *
     * **Please note:** parser implementations may provide more efficient lookup, e.g. using a
     * Hashtable. The 'xml' prefix is bound to "http://www.w3.org/XML/1998/namespace", as
     * defined in the [Namespaces in XML](http://www.w3.org/TR/REC-xml-names/#ns-using)
     * specification. Analogous, the 'xmlns' prefix is resolved to
     * [http://www.w3.org/2000/xmlns/](http://www.w3.org/2000/xmlns/)
     *
     * @see [getNamespaceCount]
     *
     * @see [getNamespacePrefix]
     *
     * @see [getNamespaceUri]
     */
    fun getNamespace(prefix: String): String? {
        for (i in getNamespaceCount(depth) - 1 downTo 0) {
            if (getNamespacePrefix(i) == prefix) {
                return getNamespaceUri(i);
            }
        }
        return when (prefix) {
            "xml" -> "http://www.w3.org/XML/1998/namespace"
            "xmlns" -> "http://www.w3.org/2000/xmlns/"
            else -> null
        }
    }

    // --------------------------------------------------------------------------
    // miscellaneous reporting methods
    /**
     * Returns the current depth of the element. Outside the root element, the depth is 0. The
     * depth is incremented by 1 when a start tag is reached. The depth is decremented AFTER the
     * end tag event was observed.
     *
     * ```
     * <!-- outside -->     0
     * <root>               1
     * sometext             1
     * <foobar>             2
     * </foobar>            2
     * </root>              1
     * <!-- outside -->     0
     * ```
     */
    val depth: Int

    /**
     * Returns a short text describing the current parser state, including the position, a
     * description of the current event and the data source if known. This method is especially
     * useful to provide meaningful error messages and for debugging purposes.
     */
    val positionDescription: String

    /**
     * Returns the current line number, starting from 1.
     *
     * When the parser does not know the current line number or can not determine it, -1 is returned
     * (e.g. for WBXML).
     *
     * @return current line number or -1 if unknown.
     */
    val lineNumber: Int

    /**
     * Returns the current column number, starting from 0. When the parser does not know the
     * current column number or can not determine it, -1 is returned (e.g. for WBXML).
     *
     * @return current column number or -1 if unknown.
     */
    val columnNumber: Int

    // --------------------------------------------------------------------------
    // TEXT related methods & properties
    /**
     * Checks whether the current TEXT event contains only whitespace characters.
     *
     * - For IGNORABLE_WHITESPACE, this is always true.
     * - For TEXT and CDSECT, false is returned when the current event text
     *   contains at least one non-white space character. For any other
     *   event type an exception is thrown.
     *
     * **Please note:** non-validating parsers are not able to distinguish whitespace and ignorable
     * whitespace, except from whitespace outside the root element. Ignorable  whitespace is
     * reported as separate event, which is exposed via nextToken only.
     */
    val isWhitespace: Boolean

    /**
     * Returns the text content of the current event as String.
     *
     * The value returned depends on current event type, for example for TEXT event it is element
     * content (this is typical case when next() is used).
     *
     * See description of nextToken() for detailed description of possible returned values for
     * different types of events.
     *
     * **NOTE:** in case of ENTITY_REF, this method returns
     * the entity replacement text (or null if not available). This is
     * the only case where text and getTextCharacters() return different values.
     *
     * @see .eventType
     *
     * @see .next
     *
     * @see .nextToken
     */
    val text: String

    // --------------------------------------------------------------------------
    // START_TAG / END_TAG shared methods
    /**
     * Returns the namespace URI of the current element. The default namespace is represented
     * as empty string. If namespaces are not enabled, an empty string ("") is always returned.
     * The current event must be START_TAG or END_TAG; otherwise, an empty string is returned.
     */
    val namespace: String

    /**
     * For START_TAG or END_TAG events, the (local) name of the current element is returned when
     * namespaces are enabled. When namespace processing is disabled, the raw name is returned.
     * For ENTITY_REF events, the entity name is returned. If the current event is not START_TAG,
     * END_TAG, or ENTITY_REF, an empty string is returned.
     *
     * **Please note:** To reconstruct the raw element name
     * when namespaces are enabled and the prefix is not empty,
     * you will need to  add the prefix and a colon to localName.
     */
    val name: String

    /**
     * Returns the prefix of the current element.
     *
     * If the element is in the default namespace (has no prefix), an empty string is returned.
     *
     * If namespaces are not enabled, or the current event is not START_TAG or END_TAG, an empty
     * string is returned.
     */
    val prefix: String

    /**
     * Returns true if the current event is START_TAG and the tag is degenerated
     * (e.g. &lt;foobar/&gt;); false otherwise.
     */
    val isEmptyElementTag: Boolean

    // --------------------------------------------------------------------------
    // START_TAG Attributes retrieval methods
    /**
     * Returns the number of attributes of the current start tag, or
     * -1 if the current event type is not START_TAG
     *
     * @see .getAttributeNamespace
     *
     * @see .getAttributeName
     *
     * @see .getAttributePrefix
     *
     * @see .getAttributeValue
     */
    val attributeCount: Int

    /**
     * Returns the namespace URI of the attribute with the given index (starts from 0).
     *
     * Returns an empty string ("") if namespaces are not enabled or the attribute has no namespace.
     * Throws an IndexOutOfBoundsException if the index is out of range or the current event type is
     * not START_TAG.
     *
     * **NOTE:** if FEATURE_REPORT_NAMESPACE_ATTRIBUTES is set
     * then namespace attributes (xmlns:ns='...') must be reported
     * with namespace
     * [http://www.w3.org/2000/xmlns/](http://www.w3.org/2000/xmlns/)
     * (visit this URL for description!).
     * The default namespace attribute (xmlns="...") will be reported with empty namespace.
     *
     * **NOTE:**The xml prefix is bound as defined in
     * [Namespaces in XML](http://www.w3.org/TR/REC-xml-names/#ns-using)
     * specification to "http://www.w3.org/XML/1998/namespace".
     *
     * @param index zero-based index of attribute
     * @return attribute namespace,
     * empty string ("") is returned  if namesapces processing is not enabled or
     * namespaces processing is enabled but attribute has no namespace (it has no prefix).
     */
    fun getAttributeNamespace(index: Int): String?

    /**
     * Returns the local name of the specified attribute if namespaces are enabled or just
     * attribute name if namespaces are disabled.
     *
     * Throws an IndexOutOfBoundsException if the index is out of range or current event type is
     * not START_TAG.
     *
     * @param index zero-based index of attribute
     * @return attribute name (null is never returned)
     */
    fun getAttributeName(index: Int): String

    /**
     * Returns the prefix of the specified attribute.
     *
     * Returns an empty string if the element has no prefix.
     * If namespaces are disabled it will always return an empty string.
     *
     * Throws an IndexOutOfBoundsException if the index is out of range
     * or current event type is not START_TAG.
     *
     * @param index zero-based index of attribute
     * @return attribute prefix or null if namespaces processing is not enabled.
     */
    fun getAttributePrefix(index: Int): String

    /**
     * Returns the type of the specified attribute.
     *
     * If parser is non-validating it MUST return CDATA.
     *
     * @param zero based index of attribute
     * @return attribute type (null is never returned)
     */
    fun getAttributeType(index: Int): String

    /**
     * Returns if the specified attribute was not in input was declared in XML.
     * If parser is non-validating it MUST always return false.
     * This information is part of XML infoset:
     *
     * @param zero based index of attribute
     * @return false if attribute was in input
     */
    fun isAttributeDefault(index: Int): Boolean

    /**
     * Returns the given attributes value.
     * Throws an IndexOutOfBoundsException if the index is out of range
     * or current event type is not START_TAG.
     *
     *
     * **NOTE:** attribute value must be normalized
     * (including entity replacement text if PROCESS_DOCDECL is false) as described in
     * [XML 1.0 section
 * 3.3.3 Attribute-Value Normalization](http://www.w3.org/TR/REC-xml#AVNormalize)
     *
     * @see defineEntityReplacementText
     *
     *
     * @param zero based index of attribute
     * @return value of attribute (null is never returned)
     */
    fun getAttributeValue(index: Int): String

    /**
     * Returns the attributes value identified by namespace URI and namespace localName.
     * If namespaces are disabled namespace must be null.
     * If current event type is not START_TAG then IndexOutOfBoundsException will be thrown.
     *
     *
     * **NOTE:** attribute value must be normalized
     * (including entity replacement text if PROCESS_DOCDECL is false) as described in
     * [XML 1.0 section
 * 3.3.3 Attribute-Value Normalization](http://www.w3.org/TR/REC-xml#AVNormalize)
     *
     * @see .defineEntityReplacementText
     *
     *
     * @param namespace Namespace of the attribute if namespaces are enabled otherwise must be empty
     * @param name If namespaces enabled local name of attribute otherwise just attribute name
     * @return value of attribute or null if attribute with given name does not exist
     */
    fun getAttributeValue(
        namespace: String,
        name: String
    ): String?
    // --------------------------------------------------------------------------
    // actual parsing methods
    /**
     * Returns the type of the current event (START_TAG, END_TAG, TEXT, etc.)
     *
     * @see .next
     * @see .nextToken
     */
    val eventType: EventType

    /**
     * Get next parsing event - element content wil be coalesced and only one
     * TEXT event must be returned for whole element content
     * (comments and processing instructions will be ignored and emtity references
     * must be expanded or exception mus be thrown if entity reerence can not be exapnded).
     * If element content is empty (content is "") then no TEXT event will be reported.
     *
     *
     * **NOTE:** empty element (such as &lt;tag/>) will be reported
     * with  two separate events: START_TAG, END_TAG - it must be so to preserve
     * parsing equivalency of empty element to &lt;tag>&lt;/tag>.
     * (see isEmptyElementTag ())
     *
     * @see .isEmptyElementTag
     *
     * @see .START_TAG
     *
     * @see .TEXT
     *
     * @see .END_TAG
     *
     * @see .END_DOCUMENT
     */
    fun next(): EventType

    /**
     * This method works similarly to next() but will expose
     * additional event types (COMMENT, CDSECT, DOCDECL, ENTITY_REF, PROCESSING_INSTRUCTION, or
     * IGNORABLE_WHITESPACE) if they are available in input.
     *
     *
     * If special feature
     * [FEATURE_XML_ROUNDTRIP](http://xmlpull.org/v1/doc/features.html#xml-roundtrip)
     * (identified by URI: http://xmlpull.org/v1/doc/features.html#xml-roundtrip)
     * is enabled it is possible to do XML document round trip ie. reproduce
     * exectly on output the XML input using getText():
     * returned content is always unnormalized (exactly as in input).
     * Otherwise returned content is end-of-line normalized as described
     * [XML 1.0 End-of-Line Handling](http://www.w3.org/TR/REC-xml#sec-line-ends)
     * and. Also when this feature is enabled exact content of START_TAG, END_TAG,
     * DOCDECL and PROCESSING_INSTRUCTION is available.
     *
     *
     * Here is the list of tokens that can be  returned from nextToken()
     * and what getText() and getTextCharacters() returns:<dl>
     * <dt>START_DOCUMENT</dt><dd>null
    </dd> * <dt>END_DOCUMENT</dt><dd>null
    </dd> * <dt>START_TAG</dt><dd>null unless FEATURE_XML_ROUNDTRIP
     * enabled and then returns XML tag, ex: &lt;tag attr='val'>
    </dd> * <dt>END_TAG</dt><dd>null unless FEATURE_XML_ROUNDTRIP
     * id enabled and then returns XML tag, ex: &lt;/tag>
    </dd> * <dt>TEXT</dt><dd>return element content.
     * <br></br>Note: that element content may be delivered in multiple consecutive TEXT events.
    </dd> * <dt>IGNORABLE_WHITESPACE</dt><dd>return characters that are determined to be ignorable white
     * space. If the FEATURE_XML_ROUNDTRIP is enabled all whitespace content outside root
     * element will always reported as IGNORABLE_WHITESPACE otherise rteporting is optional.
     * <br></br>Note: that element content may be delevered in multiple consecutive IGNORABLE_WHITESPACE events.
    </dd> * <dt>CDSECT</dt><dd>
     * return text *inside* CDATA
     * (ex. 'fo&lt;o' from &lt;!CDATA[fo&lt;o]]>)
    </dd> * <dt>PROCESSING_INSTRUCTION</dt><dd>
     * if FEATURE_XML_ROUNDTRIP is true
     * return exact PI content ex: 'pi foo' from &lt;?pi foo?>
     * otherwise it may be exact PI content or concatenation of PI target,
     * space and data so for example for
     * &lt;?target    data?> string &quot;target data&quot; may
     * be returned if FEATURE_XML_ROUNDTRIP is false.
    </dd> * <dt>COMMENT</dt><dd>return comment content ex. 'foo bar' from &lt;!--foo bar-->
    </dd> * <dt>ENTITY_REF</dt><dd>getText() MUST return entity replacement text if PROCESS_DOCDECL is false
     * otherwise getText() MAY return null,
     * additionally getTextCharacters() MUST return entity name
     * (for example 'entity_name' for &amp;entity_name;).
     * <br></br>**NOTE:** this is the only place where value returned from getText() and
     * getTextCharacters() **are different**
     * <br></br>**NOTE:** it is user responsibility to resolve entity reference
     * if PROCESS_DOCDECL is false and there is no entity replacement text set in
     * defineEntityReplacementText() method (getText() will be null)
     * <br></br>**NOTE:** character entities (ex. &amp;#32;) and standard entities such as
     * &amp;amp; &amp;lt; &amp;gt; &amp;quot; &amp;apos; are reported as well
     * and are **not** reported as TEXT tokens but as ENTITY_REF tokens!
     * This requirement is added to allow to do roundtrip of XML documents!
    </dd> * <dt>DOCDECL</dt><dd>
     * if FEATURE_XML_ROUNDTRIP is true or PROCESS_DOCDECL is false
     * then return what is inside of DOCDECL for example it returns:<pre>
     * &quot; titlepage SYSTEM "http://www.foo.bar/dtds/typo.dtd"
     * [&lt;!ENTITY % active.links "INCLUDE">]&quot;</pre>
     *
     * for input document that contained:<pre>
     * &lt;!DOCTYPE titlepage SYSTEM "http://www.foo.bar/dtds/typo.dtd"
     * [&lt;!ENTITY % active.links "INCLUDE">]></pre>
     * otherwise if FEATURE_XML_ROUNDTRIP is false and PROCESS_DOCDECL is true
     * then what is returned is undefined (it may be even null)
    </dd> *
    </dl> *
     *
     *
     * **NOTE:** there is no gurantee that there will only one TEXT or
     * IGNORABLE_WHITESPACE event from nextToken() as parser may chose to deliver element content in
     * multiple tokens (dividing element content into chunks)
     *
     *
     * **NOTE:** whether returned text of token is end-of-line normalized
     * is depending on FEATURE_XML_ROUNDTRIP.
     *
     *
     * **NOTE:** XMLDecl (&lt;?xml ...?&gt;) is not reported but its content
     * is available through optional properties (see class description above).
     *
     * @see .next
     *
     * @see .START_TAG
     *
     * @see .TEXT
     *
     * @see .END_TAG
     *
     * @see .END_DOCUMENT
     *
     * @see .COMMENT
     *
     * @see .DOCDECL
     *
     * @see .PROCESSING_INSTRUCTION
     *
     * @see .ENTITY_REF
     *
     * @see .IGNORABLE_WHITESPACE
     */
    fun nextToken(): EventType
    //-----------------------------------------------------------------------------
    // utility methods to mak XML parsing easier ...
    /**
     * Test if the current event is of the given type and if the
     * namespace and name do match. null will match any namespace
     * and any name. If the test is not passed, an exception is
     * thrown. The exception text indicates the parser position,
     * the expected event and the current event that is not meeting the
     * requirement.
     *
     *
     * Essentially it does this
     * <pre>
     * if (type != eventType
     * || (namespace != null &amp;&amp;  !namespace.equals( getNamespace () ) )
     * || (name != null &amp;&amp;  !name.equals( getName() ) ) )
     * throw new XmlPullParserException( "expected "+ TYPES[ type ]+getPositionDescription());
    </pre> *
     */
    fun require(type: EventType?, namespace: String?, name: String?)

    /**
     * If current event is START_TAG then if next element is TEXT then element content is returned
     * or if next event is END_TAG then empty string is returned, otherwise exception is thrown.
     * After calling this function successfully parser will be positioned on END_TAG.
     *
     *
     * The motivation for this function is to allow to parse consistently both
     * empty elements and elements that has non empty content, for example for input:
     *  1. &lt;tag&gt;foo&lt;/tag&gt;
     *  1. &lt;tag&gt;&lt;/tag&gt; (which is equivalent to &lt;tag/&gt;
     * both input can be parsed with the same code:
     * <pre>
     * p.nextTag()
     * p.requireEvent(p.START_TAG, "", "tag");
     * String content = p.nextText();
     * p.requireEvent(p.END_TAG, "", "tag");
    </pre> *
     * This function together with nextTag make it very easy to parse XML that has
     * no mixed content.
     *
     *
     *
     * Essentially it does this
     * <pre>
     * if(eventType != START_TAG) {
     * throw new XmlPullParserException(
     * "parser must be on START_TAG to read next text", this, null);
     * }
     * int eventType = next();
     * if(eventType == TEXT) {
     * String result = getText();
     * eventType = next();
     * if(eventType != END_TAG) {
     * throw new XmlPullParserException(
     * "event TEXT it must be immediately followed by END_TAG", this, null);
     * }
     * return result;
     * } else if(eventType == END_TAG) {
     * return "";
     * } else {
     * throw new XmlPullParserException(
     * "parser must be on START_TAG or TEXT to read text", this, null);
     * }
    </pre> *
     */
    fun nextText(): String

    /**
     * Call next() and return event if it is START_TAG or END_TAG
     * otherwise throw an exception.
     * It will skip whitespace TEXT before actual tag if any.
     *
     *
     * essentially it does this
     * <pre>
     * int eventType = next();
     * if(eventType == TEXT &amp;&amp;  isWhitespace()) {   // skip whitespace
     * eventType = next();
     * }
     * if (eventType != START_TAG &amp;&amp;  eventType != END_TAG) {
     * throw new XmlPullParserException("expected start or end tag", this, null);
     * }
     * return eventType;
    </pre> *
     */
    fun nextTag(): EventType



    /**
     * Skip sub tree that is currently porser positioned on.
     * <br></br>NOTE: parser must be on START_TAG and when funtion returns
     * parser will be positioned on corresponding END_TAG.
     */
    //	Implementation copied from Alek's mail...
    fun skipSubTree() {
        require(EventType.START_TAG, null, null)
        var level = 1
        while (level > 0) {
            val eventType = next()
            if (eventType == EventType.END_TAG) {
                --level
            } else if (eventType == EventType.START_TAG) {
                ++level
            }
        }
    }


}