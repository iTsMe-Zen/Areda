# Project-specific keep rules can be added here if the app grows beyond its
# current minimal EPUB/PDF-only footprint.

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
