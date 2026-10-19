# FloatScript ProGuard rules.
# Keep Room generated classes and entity field names.
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
