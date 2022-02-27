# kxml3

The goal of this project is to provide a minimal platform-agnostic non-validating XML pull parser based on kxml2 and to explore/shape a kotlin version of the xmlpull api.

This is work in progress and the API is likely to change.

The most significant changes are:

- Replaced the Reader as input with CharIterator (and String) support, as Java streams are not available for Kotlin native.
- Reduced the number of places where null is returned

[MavenCentral](https://search.maven.org/artifact/org.kobjects.kxml3/core/0.1.2/jar) artifact for Gradle:
```
implementation 'org.kobjects.kxml3:core:0.1.2'
```
Kotlin DSL:
```
implementation("org.kobjects.kxml3:core:0.1.2")
```

The xmlpull v2 api will me migrated to a separate repository when finished.

Brainstorming document: https://docs.google.com/document/d/1OXG5F5I-Gp-65cN8THWB1LMTZDnRS96CIBenPcusDDA/edit?usp=sharing
