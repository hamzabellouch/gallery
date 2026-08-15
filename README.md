# Gallery
<h3 align="center">Pure Simplicity. Infinite Clarity. Maximum Fluidity.</h3>
<p align="center">
An open-source, modern Android gallery application built with Jetpack Compose, Material You (Material 3), and AndroidX Media3 ExoPlayer.
</p>

<div align="center">
  <img src="https://raw.githubusercontent.com/hamzabellouch/gallery/main/Images/Gallery.png" alt="Gallery" width="800"/>
</div>

## Overview

**Gallery** is a fast, clean, and beautifully designed media browser and player for Android.  
Built from the ground up with **Jetpack Compose** and **Material Design 3**, it delivers a seamless, fluid experience for viewing high-resolution photos, playing high-frame-rate 4K videos, organizing albums, and exploring detailed EXIF metadata.

Gallery follows the principles of **Modern Android Development (MAD)** to guarantee peak performance, minimal memory footprint, battery efficiency, and privacy-focused local media management.

The application focuses on simplicity, speed, and elegance—designed for both everyday casual browsing and demanding media viewing workflows.

## Screenshots

Gallery UI & Features :

<div align="center">
<div>
<img src="https://raw.githubusercontent.com/hamzabellouch/gallery/main/Images/Home.jpg" width="30%" />
<img src="https://raw.githubusercontent.com/hamzabellouch/gallery/main/Images/Albums.jpg" width="30%" />
<img src="https://raw.githubusercontent.com/hamzabellouch/gallery/main/Images/MediaViewer.jpg" width="30%" />
<img src="https://raw.githubusercontent.com/hamzabellouch/gallery/main/Images/VideoPlayer.jpg" width="30%" />
<img src="https://raw.githubusercontent.com/hamzabellouch/gallery/main/Images/ExifInfo.jpg" width="30%" />
<img src="https://raw.githubusercontent.com/hamzabellouch/gallery/main/Images/FastScrollbar.jpg" width="30%" />
<img src="https://raw.githubusercontent.com/hamzabellouch/gallery/main/Images/Look%20%26%20feel.jpg" width="30%" />
<img src="https://raw.githubusercontent.com/hamzabellouch/gallery/main/Images/Language.jpg" width="30%" />
<img src="https://raw.githubusercontent.com/hamzabellouch/gallery/main/Images/Auto%20update.jpg" width="30%" />
<img src="https://raw.githubusercontent.com/hamzabellouch/gallery/main/Images/About.jpg" width="30%" />
</div>
</div>

<br>

## ⭐ Features & Media Capabilities

Experience a rich set of features crafted for speed, convenience, and control :

| Feature / Format | Engine / Technology | Support Level | Capabilities & Performance Highlights |
| :--- | :--- | :--- | :--- |
| **Photos (JPEG, PNG, WEBP, HEIF)** | `Coil 3` & `Tiling/Subsampling` | **Full Support** | Deep pinch-to-zoom, sub-sampling for ultra-high resolutions, pan physics, and rapid async caching. |
| **Vector & Animations (SVG, GIF)** | `Coil 3 (SVG & GIF Decoders)` | **Full Support** | Sharp vector rendering, smooth animated GIF playback, and frame control. |
| **Video Playback (MP4, MKV, WebM)** | `AndroidX Media3 ExoPlayer` | **Full (Up to 4K 120fps)** | Hardware-accelerated decoding, gesture-based volume & brightness control, smooth scrub timeline. |
| **EXIF & Media Metadata** | `AndroidX ExifInterface` | **Full Inspection** | Camera model, ISO, shutter speed, aperture, focal length, resolution, file path, and date stamps. |
| **Fast Date Scrollbar** | Custom Compose Component | **Interactive** | Floating haptic-enabled fast scrollbar with real-time date indicators for thousands of items. |
| **Dynamic Theming (Material You)** | `Material Design 3` | **Adaptive** | Dynamic wallpaper colors, dark/light theme switching, and custom monochrome icon modes. |
| **Multi-Language Localization** | Android Resources & In-App Engine | **30+ Languages** | Full in-app language switcher supporting Arabic, English, French, Spanish, Japanese, and more. |
| **In-App GitHub Updates** | `OkHttp` & `Kotlinx Serialization` | **Automatic & Manual** | Checks GitHub Releases, downloads APKs with progress tracking, and launches installer directly. |

### <a name="TechStack"></a> 🛠 Tech Stack & Architecture

Gallery is engineered following clean architecture and modern Android development guidelines:

* `100% Kotlin & Coroutines / Flow`
* `Jetpack Compose` (Declarative UI)
* `Single Activity Architecture`
* `Material Design 3` & `Dynamic Colors` (Material You)
* `AndroidX Navigation Compose`
* `AndroidX Media3 ExoPlayer` (High-Performance Video Engine)
* `Coil 3` (Async Image, GIF, SVG, and Video Frame Loading)
* `AndroidX ExifInterface` (Comprehensive Image Metadata)
* `OkHttp 4` & `Kotlinx Serialization` (Seamless GitHub Releases Integration)
* `Gradle Version Catalogs` (`libs.versions.toml`)

## 🔥 Installation

1. Go to the Releases page:  
   https://github.com/hamzabellouch/gallery/releases

2. Download the latest `.apk` file.

3. Install the application on your Android device.

4. Make sure that **"Install from unknown sources"** is enabled in your Android settings if prompted.

## 🔨 Building from Source

To build Gallery locally, make sure you have Android Studio (Ladybug or later) and JDK 17+ installed.

1. Clone the repository:
   ```bash
   git clone https://github.com/hamzabellouch/gallery.git
   cd gallery
   ```

2. Open the project in Android Studio or build directly via terminal:
   - **On Linux / macOS:**
     ```bash
     ./gradlew assembleDebug
     ```
   - **On Windows:**
     ```powershell
     .\gradlew.bat assembleDebug
     ```

3. Find the built APK under:
   `app/build/outputs/apk/debug/app-debug.apk`

> [!WARNING]
> There is always a possibility of error or device-specific differences, so we assume no responsibility for any inaccuracies.

---

### <a name="Copyright©2026"></a> Copyright © 2026

Thank you for checking out Gallery. If you have any feedback or suggestions, feel free to contact us:  
**hamzabellouchcontact@gmail.com**

Stay connected and follow us on:  
[Facebook](https://facebook.com/hamzabellouch1) | [Instagram](https://instagram.com/hamzabellouch0) | [Twitter / X](https://x.com/hamzabellouch0) | [Telegram](https://t.me/hamzabellouch) | [LinkedIn](https://www.linkedin.com/in/hamzabellouch) | [YouTube](https://www.youtube.com/@hamzabellouch)
