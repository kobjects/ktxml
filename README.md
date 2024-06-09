# KtXml

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.kobjects.ktxml/core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.kobjects.ktxml/core)

Minimal platform-agnostic non-validating XML pull parser based on kxml2.

The most significant changes (relative to XmlPull / KXml2) are:

- Replaced the Reader as input with CharIterator support, as Java streams are not available for Kotlin native.
- Reduced the number of places where null is returned
- Factories removed

For a code example, please take a look at the [test](https://github.com/kobjects/ktxml/blob/main/core/src/commonTest/kotlin/org/kobjects/ktxml/KtXmlTest.kt).

Import in `build.gradle.kts` for a "shared" KMM module:

```
(...)

kotlin {
  
      (...)
  
      sourceSets {
          val commonMain by getting {
              dependencies {
                  implementation("org.kobjects.ktxml:core:0.2.4")
                  (...)
              }
          }
          
      (...)  
              
```


## Use cases

- [Twine RSS Reader](https://github.com/msasikanth/twine/tree/main/core/network/src/commonMain/kotlin/dev/sasikanth/rss/reader/core/network/parser)

## FAQ

### How can I stream data to the parser? 

See https://github.com/kobjects/ktxml/issues/6

# Backgrond / Design

Brainstorming document: https://docs.google.com/document/d/1OXG5F5I-Gp-65cN8THWB1LMTZDnRS96CIBenPcusDDA/edit?usp=sharing
