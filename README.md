# Areada

Minimal Android reader focused on two formats only:

- EPUB
- PDF

## What is included

- Jetpack Compose UI with a monochrome visual theme
- Storage Access Framework folder picker so the user chooses exactly which library paths are visible
- Recent documents shelf stored locally
- Saved reading progress so reopened files resume where you left off
- Reader settings for theme, font family, and font size
- EPUB extraction and chapter rendering without a heavy external reader SDK
- Pinch-to-zoom reading for EPUB and PDF
- PDF rendering through Android's built-in `PdfRenderer`
- Offline-only reading with no internet permission and no device-wide scanning

## Build note

This machine did not have `java`, `javac`, or `gradle` available during setup, so the project includes Gradle configuration and wrapper properties but not a generated `gradle-wrapper.jar`.

To build in Android Studio:

1. Open `Areada`.
2. Let Android Studio install or point to a JDK if needed.
3. Regenerate the Gradle wrapper or sync with a local Gradle/JDK setup.

## Package

`app.areada`
