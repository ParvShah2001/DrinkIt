# Keep Wear OS components & receivers
-keep class com.example.waterreminder.receiver.** { *; }
-keep class com.example.waterreminder.reminder.** { *; }
-keep class com.example.waterreminder.presentation.** { *; }
-keep class com.example.waterreminder.data.** { *; }

# Keep Compose rules
-keepclassmembers class * extends androidx.compose.runtime.State { *; }
