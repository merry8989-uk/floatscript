# FloatScript R8 rules.
#
# Most libraries we use (Compose, Room, CameraX, DataStore) ship their own
# consumer rules, so these are the extras needed on top of those.

# Keep useful information in crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin metadata / annotations (Compose and Room rely on these).
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-keepclassmembers class kotlin.Metadata { public <methods>; }

# --- Room -------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# --- DataStore --------------------------------------------------------------
-dontwarn androidx.datastore.**

# --- CameraX ----------------------------------------------------------------
# CameraX references optional/absent classes on some devices.
-dontwarn androidx.camera.**

# --- App entry points -------------------------------------------------------
# Referenced from AndroidManifest.xml; keep them whole.
-keep class com.floatscript.app.FloatScriptApp { *; }
-keep class com.floatscript.app.MainActivity { *; }
-keep class com.floatscript.app.overlay.OverlayService { *; }
