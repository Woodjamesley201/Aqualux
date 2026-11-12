# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-dontnote kotlinx.serialization.**

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

-keep class com.aqualux.browser.session.** { *; }
-keep class com.aqualux.browser.omnibox.** { *; }
-keep class com.aqualux.browser.errors.** { *; }
-keep class com.aqualux.browser.security.** { *; }

# WebView clients are referenced from the framework via the view.
-keep class * extends android.webkit.WebViewClient { *; }
-keep class * extends android.webkit.WebChromeClient { *; }
