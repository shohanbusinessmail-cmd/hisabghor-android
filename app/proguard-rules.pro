# HisabGhor release rules
# Keep kotlinx.serialization generated serializers for backup/restore models.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
}
-keepclasseswithmembers class **$$serializer {
    *;
}
-keep class com.shohankhan.hisabghor.core.backup.** { *; }
# Room generated code is kept by its own consumer rules in most cases; be safe:
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**
