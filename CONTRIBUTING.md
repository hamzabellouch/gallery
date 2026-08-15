# Contributing

Thank you for your interest in contributing to **Gallery**! 

**Gallery** is an open-source, modern Android media browser and video player application built with Kotlin, Jetpack Compose, Material You (Material 3), Coil 3, and AndroidX Media3 ExoPlayer. It is engineered for fluid high-resolution photo browsing, deep pinch-to-zoom, fast date scrolling, EXIF inspection, and hardware-accelerated 4K video playback.

Before submitting a bug report or feature request, please search existing issues (including closed ones) to ensure it hasn't already been reported or discussed. If there are no duplicates, feel free to submit a new issue using the appropriate template.

**Please note:** Issues that do not use existing templates or lack sufficient detail may be closed without review.

For questions, feedback, or ideas to improve the project, you can contact us at:  
- **Email:** hamzabellouchcontact@gmail.com  
- **Website & Social Channels:** [hamzabellouch](https://sites.google.com/view/hamzabellouch/) | [GitHub](https://github.com/hamzabellouch/gallery)


## Disclaimer

Gallery is an active Android project focused on delivering a clean, fluid, secure, and privacy-first media experience. While we strive for top-tier code quality, smoothness (up to 120Hz), and reliability, community contributions and feedback are always welcome to improve performance and user experience.


## Bug Reports

When submitting a bug report, please make sure your issue contains **sufficient information** to reproduce the problem. Useful details include:

- Device model and Android OS version
- Gallery app version
- Media format or resolution (if related to specific image/video files, e.g., HEIF, GIF, SVG, 4K video)
- Granted permission states (Photos and videos / Storage)
- Steps to reproduce the bug
- System logs (`logcat`) or screenshots / screen recordings if applicable


## Feature Requests

Gallery aims to remain a fast, lightweight, elegant, and privacy-focused media viewer for Android. We welcome suggestions that enhance user experience, media rendering performance, UI responsiveness, or metadata inspection.

When suggesting a new feature, please consider:
- **Relevance:** Does the feature align with the core mission of media browsing, viewing, organizing, or video playback?
- **Privacy & On-Device Security:** Does it preserve 100% on-device processing and user privacy without requiring unnecessary permissions?
- **Performance & Simplicity:** We prioritize high frame rates, low memory footprint, and clean Material 3 design over bloated or clunky features.


## Pull Requests

If you wish to contribute directly by submitting code:

1. **Discuss First:** Leave a comment under an existing issue or open a new issue describing the changes you plan to make before writing code.
2. **Avoid Conflicts:** Comment on the issue to let others know you are working on it to prevent duplicate efforts.
3. **Follow Code Style:** Ensure your code follows modern Kotlin conventions, Jetpack Compose best practices, and clean architecture principles.


## New Contributors

If you are new to the project:
- Browse our open issues for beginner-friendly tasks marked as `good first issue` or `help wanted`.
- Feel free to ask clarifying questions directly on the issue thread!


## Building From Source

To build Gallery locally:

1. **Prerequisites:**
   - [Android Studio](https://developer.android.com/studio) (Ladybug or later recommended)
   - JDK 17 or higher
   - Android SDK (API Level 24+)

2. **Steps:**
   ```bash
   # Clone the repository
   git clone https://github.com/hamzabellouch/gallery.git
   cd gallery

   # Build debug APK via Gradle
   # On Linux / macOS:
   ./gradlew assembleDebug

   # On Windows:
   .\gradlew.bat assembleDebug
   ```

   The built APK will be located at:
   `app/build/outputs/apk/debug/app-debug.apk`
