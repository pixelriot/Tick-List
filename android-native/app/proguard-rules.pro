# Debug builds are not minified. Keep the Supabase model names if release minify is enabled later.
-keep class com.pixelriot.ticklist.data.** { *; }
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
