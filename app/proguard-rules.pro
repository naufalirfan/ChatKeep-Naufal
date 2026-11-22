# Keep Room generated classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# Keep Compose
-dontwarn androidx.compose.**

# Kotlin metadata
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions

# Keep model/entity fields
-keepclassmembers class id.my.naufalputra.chatkeep.data.** { *; }
