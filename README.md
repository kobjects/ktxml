# KtXml

Minimal platform-agnostic non-validating XML pull parser based on kxml2.

This is still work in progress and the API might still change.

The most significant changes are:

- Replaced the Reader as input with CharIterator support, as Java streams are not available for Kotlin native.
- Reduced the number of places where null is returned
- Factories removed

For a usage example, please take a look at the [test](https://github.com/kobjects/ktxml/blob/main/core/src/commonTest/kotlin/org/kobjects/ktxml/KtXmlTest.kt).

If you want to support this project, please support https://github.com/jitpack/jitpack.io/issues/3853

Brainstorming document: https://docs.google.com/document/d/1OXG5F5I-Gp-65cN8THWB1LMTZDnRS96CIBenPcusDDA/edit?usp=sharing
