# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\tknoh\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include flags and the other lines as needed.

# Keep Media3 ExoPlayer classes
-keep class androidx.media3.** { *; }

# Keep Coil image loader classes
-keep class io.coil-kt.** { *; }

# Keep androidx.exifinterface
-keep class androidx.exifinterface.** { *; }
