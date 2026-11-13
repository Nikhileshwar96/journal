# Room: keep generated database implementations and entities. Room ships consumer rules,
# but these are kept explicitly so a future library change can't silently break release builds.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-keepclassmembers @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# Keep line numbers so Play Console crash reports are readable after deobfuscation.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
