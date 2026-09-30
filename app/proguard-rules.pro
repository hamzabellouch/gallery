# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\tknoh\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include flags and the other lines as needed.

# Keep Media3 ExoPlayer classes
-keep class androidx.media3.** { *; }

# Keep Coil 3 image loader classes
-keep class coil3.** { *; }
-keep class io.coil-kt.** { *; }

# Keep androidx.exifinterface
-keep class androidx.exifinterface.** { *; }

# Keep Gallery Application & Data Models
-keep class com.tkno.gallery.data.model.** { *; }
-keep class com.tkno.gallery.GalleryApplication { *; }
-keep class com.tkno.gallery.App { *; }
