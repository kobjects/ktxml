# KtXml

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.kobjects.ktxml/core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.kobjects.ktxml/core)

Minimal platform-agnostic non-validating XML pull parser based on kxml2.

The most significant changes (relative to XmlPull / [KXml2](https://github.com/kobjects/kxml2)) are:

- Replaced the Reader as input with CharIterator support, as Java streams are not available for Kotlin native.
- Reduced the number of places where null is returned
- Factories removed

Import in `build.gradle.kts` for a "shared" KMP module:

```
(...)

kotlin {
  
      (...)
  
      sourceSets {
          val commonMain by getting {
              dependencies {
                  implementation("org.kobjects.ktxml:core:0.3.2")
                  (...)
              }
          }
          
      (...)  
              
```

KtXML is implemented in pure Kotlin, so it should work on all platforms supported by Kotlin.


## Usage

The most important calls in KtXml are `next()` and `nextToken()`. 

- `next()` moves to the next "high level" token and returns the token type. It skips over 
  comments and processing instructions and automatically decodes entity references. It also might
  aggregate text content.

- `nextToken()` moves to the next "low level" token including whitespace, processing instructions,
  comments and entity references.

After a call to `next()` or `nextToken()`, the details of the current token (such as tag attributes) 
can be queried from the parser; see [XmlPullParser](https://github.com/kobjects/ktxml/blob/main/core/src/commonMain/kotlin/org/kobjects/ktxml/api/XmlPullParser.kt)

Typically these calls are used to implement recursive descend parsing of a specific XML format.

Please always use the interface (XmlPullParser) and never the concrete parser implementation
(MiniXmlPullParser) when passing the parser around.

For a set of small code examples, please take a look at the [test](https://github.com/kobjects/ktxml/blob/main/core/src/commonTest/kotlin/org/kobjects/ktxml/KtXmlTest.kt).

For more information about the API, please refer to the [KtXml API documentation](https://kobjects.org/ktxml/dokka/).

## Use cases

- [Twine RSS Reader](https://github.com/msasikanth/twine/)
  ([Parsing Code](https://github.com/msasikanth/twine/tree/main/core/network/src/commonMain/kotlin/dev/sasikanth/rss/reader/core/network/parser))
- [MapLibre Compose](https://github.com/maplibre/maplibre-compose) via [HtmlConverterCompose](https://github.com/cbeyls/HtmlConverterCompose). 

## FAQ

### How can I stream data to the parser? 

See https://github.com/kobjects/ktxml/issues/6

### Where is Wbxml / kDom support from kXML2?

If you are missing anything from kXML2, please file a feature request by creating a corresponding
issue in the github issue tracker.

### I have a different problem!

Please file an issue using the github issue tracker.

### I have a Pull Request for an Improvement

Please file an issue before creating a PR.

Please try to keep PRs as "atomic" as possible, i.e. addressing one issue only. I can push library / Kotlin / Gradle versions up as needed beforehand where this helps.


# Background / Design

Brainstorming document: https://docs.google.com/document/d/1OXG5F5I-Gp-65cN8THWB1LMTZDnRS96CIBenPcusDDA/edit?usp=sharing
