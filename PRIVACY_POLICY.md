# Privacy Policy - Gallery

Last Updated: August 15, 2026


### 1. Executive Summary & Overview

**Gallery** is an open-source, modern media browser and video player application for Android, built with Jetpack Compose, Material You (Material 3), Coil 3, and AndroidX Media3 ExoPlayer.

#### Core Privacy Commitment:
Gallery operates **100% on-device**. We do not collect, store, transmit, track, share, or sell any personal data, media files (photos, videos, audio), albums, EXIF metadata, usage logs, or device identifiers to external analytics servers or third parties.


### 2. Information Processed On-Device

To provide fluid media browsing, full-resolution zooming, and 4K video playback, Gallery reads and processes the following information **locally on your device**:

- **Local Media Files (Photos, Videos, Audio, Vectors, GIFs):** Indexed and loaded directly from your device storage using Android MediaStore APIs. Decoding, caching, subsampling, and video playback take place strictly within volatile memory (RAM). Your files are never uploaded, analyzed remotely, or sent off your device.
- **EXIF & Media Metadata:** Inspected locally using `AndroidX ExifInterface` (camera model, ISO, aperture, focal length, exposure, resolution, timestamps, and geolocation tags stored inside files). This metadata is processed locally only to display info to you and is never transmitted.
- **Local Preferences & Settings:** User preferences (such as dark/light theme, dynamic colors, monochrome icons, language selection, and favorite media items) are saved strictly locally on your device using Android `SharedPreferences`.


### 3. Permissions Used & Their Purposes

Gallery requests specific Android permissions strictly to deliver its core media viewing functionality:

**A. Read Media Images & Videos** (`android.permission.READ_MEDIA_IMAGES` & `android.permission.READ_MEDIA_VIDEO`)
- **Purpose:** Enables Gallery to discover, display, and play images and videos on devices running Android 13 (API 33) and above.
- **Scope:** Used strictly for local media gallery browsing, image viewing, and video playback.

**B. Read External Storage** (`android.permission.READ_EXTERNAL_STORAGE` - Android 12 and below)
- **Purpose:** Allows Gallery to read media files from local device storage on older Android versions.
- **Scope:** Read-only access to local media files.

**C. Write External Storage** (`android.permission.WRITE_EXTERNAL_STORAGE` - Android 10 and below)
- **Purpose:** Allows modifying or deleting media files upon explicit user confirmation on legacy Android versions.
- **Scope:** Strict user-initiated local file actions only.


### 4. Media & Playback Safety Policy

Gallery complies fully with privacy guidelines and modern Android security best practices:

- **Strict Local Isolation:** Media files are never accessed in the background or transferred to external servers.
- **Zero Telemetry & Tracking:** No user profiling, no analytics SDKs, no behavioral tracking, and no hidden data collection.
- **Full User Control:** Media and storage access requires explicit runtime permission prompts and can be revoked at any time through Android Settings.


### 5. Data Sharing, Analytics & Advertising

- **No Data Sharing:** No photos, videos, audio, metadata, or personal data ever leaves your device.
- **No Third-Party Analytics / Tracking:** Gallery contains no telemetry, analytics SDKs, or tracker libraries.
- **No Advertisements:** Gallery is 100% ad-free with no banner ads, interstitials, or tracking cookies.


### 6. Data Retention & Lifecycle

- **Volatile Cache Handling:** Decoded bitmaps, video render buffers, and thumbnail caches are stored in volatile memory and are cleared when the app is paused, closed, or when the system requests memory.
- **Complete Data Removal:** Uninstalling Gallery permanently deletes all local preferences and app configuration stored on the device. Your original photos and videos in storage remain completely untouched.


### 7. Managing Permissions & User Rights

You maintain full control over the permissions granted to Gallery. You may manage or revoke permissions at any time via Android Settings:

**Disable Media / Storage Permissions:**  
`Settings > Apps > Gallery > Permissions > Photos and videos (or Storage) > Don't Allow`


### 8. Contact & Support

If you have any questions or feedback regarding this Privacy Policy or permission usage, please contact us at:
- **Email:** hamzabellouchcontact@gmail.com
- **GitHub Repository:** https://github.com/hamzabellouch/gallery


-------------------------------------------



# سياسة الخصوصية - Gallery

آخر تحديث: ١٥ أغسطس ٢٠٢٦


### ١. الملخص التنفيذي والنظرة العامة:

تطبيق **Gallery** هو تطبيق مفتوح المصدر وحديث لتصفح الصور وتشغيل مقاطع الفيديو على نظام Android، تم بناؤه باستخدام تقنيات Jetpack Compose و Material You (Material 3) و Coil 3 و AndroidX Media3 ExoPlayer.

#### الالتزام الأساسي بالخصوصية:
يعمل Gallery بنسبة **١٠٠٪ محلياً على الجهاز**. نحن لا نجمع، ولا نخزن، ولا ننقل، ولا نتتبع، ولا نشارك، ولا نبيع أي بيانات شخصية، أو ملفات وسائط (صور، مقاطع فيديو، صوتيات)، أو ألبومات، أو بيانات وصفية (EXIF)، أو سجلات استخدام، أو معرّفات للجهاز إلى أي خوادم تحليلات خارجية أو أطراف ثالثة.


### ٢. المعلومات التي تتم معالجتها على الجهاز:

لتقديم تجربة تصفح سريعة للوسائط، وتكبير فائق الدقة، وتشغيل سلس للفيديوهات بدقة تصل إلى 4K، يقوم Gallery بقراءة ومعالجة المعلومات التالية **محلياً على جهازك فقط**:

* **ملفات الوسائط المحلية (الصور، الفيديوهات، الصوتيات، الرسوم الشعاعية، وصور GIF):** يتم فهرستها وتحميلها مباشرة من وحدة تخزين جهازك باستخدام واجهات برمجة تطبيقات Android MediaStore. تتم جميع عمليات فك التشفير، والتخزين المؤقت، وتشغيل الفيديو داخل الذاكرة المؤقتة (RAM) فقط، ولا يتم أبداً رفع ملفاتك أو تحليلها عن بُعد أو إرسالها خارج جهازك.
* **بيانات EXIF والمعلومات الوصفية:** يتم فحصها محلياً عبر مكتبة `AndroidX ExifInterface` (طراز الكاميرا، حساسية ISO، فتحة العدسة، البعد البؤري، التعريض، الدقة، الطوابع الزمنية، وإحداثيات الموقع إن وجدت داخل الملف). تُعالج هذه البيانات محلياً فقط لعرضها لك ولا يتم إرسالها لأي جهة.
* **التفضيلات والإعدادات المحلية:** يتم حفظ تفضيلاتك (مثل المظهر الداكن/الفاتح، الألوان الديناميكية، الأيقونات أحادية اللون، واختيار اللغة، وقائمة العناصر المفضلة) محلياً بشكل صارم على جهازك باستخدام Android `SharedPreferences`.


### ٣. الأذونات المستخدمة وأغراضها:

يطلب Gallery أذونات Android محددة فقط لتقديم وظائفه الأساسية لتصفح الوسائط:

**أ. إذن قراءة الصور ومقاطع الفيديو (`READ_MEDIA_IMAGES` و `READ_MEDIA_VIDEO`)**
* **الغرض:** يمكّن Gallery من اكتشاف وعرض وتشغيل الصور ومقاطع الفيديو على الأجهزة التي تعمل بنظام Android 13 (API 33) فما فوق.
* **النطاق:** يُستخدم حصرياً لتصفح المعرض المحلي، وعرض الصور، وتشغيل الفيديو.

**ب. إذن قراءة التخزين الخارجي (`READ_EXTERNAL_STORAGE` - لنظام Android 12 وما قبله)**
* **الغرض:** يسمح لـ Gallery بقراءة ملفات الوسائط من تخزين الجهاز المحلي في إصدارات Android القديمة.
* **النطاق:** وصول للقراءة فقط للوسائط المحلية.

**ج. إذن كتابة التخزين الخارجي (`WRITE_EXTERNAL_STORAGE` - لنظام Android 10 وما قبله)**
* **الغرض:** يتيح تعديل أو حذف ملفات الوسائط بناءً على طلب وإجراء صريح من المستخدم في إصدارات Android السابقة.
* **النطاق:** عمليات محلية على الملفات بتوجيه من المستخدم فقط.


### ٤. سياسة أمان الوسائط والتشغيل:

يلتزم تطبيق Gallery بشكل كامل بإرشادات الخصوصية وأحدث معايير الأمان لنظام Android:

* **عزل محلي صارم:** لا يتم الوصول إلى ملفات الوسائط في الخلفية أو مزامنتها مع أي سحابة تخزين خارجية.
* **انعدام التتبع والتحليلات:** لا يوجد أي جمع للبيانات السلوكية، ولا تُدمج أي مكتبات تحليلية أو أدوات تتبع (No Telemetry).
* **تحكم كامل للمستخدم:** يتطلب الوصول إلى الوسائط إذناً صريحاً في وقت التشغيل، ويمكن إلغاؤه في أي وقت من إعدادات Android.


### ٥. مشاركة البيانات والتحليلات والإعلانات:

* **عدم مشاركة البيانات:** لا تغادر أي صور، أو مقاطع فيديو، أو صوتيات، أو بيانات وصفية، أو بيانات شخصية جهازك مطلقاً.
* **خالٍ من أدوات التتبع الخارجية:** لا يحتوي التطبيق على أي SDKs للتتبع أو التحليلات أو الإحصائيات.
* **خالٍ تماماً من الإعلانات:** التطبيق مجاني وخالٍ ١٠٠٪ من أي إعلانات أو لافتات أو ملفات تعريف ارتباط تسويقية.


### ٦. الاحتفاظ بالبيانات ودورة حياتها:

* **معالجة الذاكرة المؤقتة:** يتم الاحتفاظ بالصور المفكوكة ومخازن تشغيل الفيديو المؤقتة في الذاكرة العشوائية (RAM) وتُمسح تلقائياً عند إيقاف التطبيق أو إغلاقه أو طلب النظام تفريغ الذاكرة.
* **الحذف الكامل للبيانات:** يؤدي إلغاء تثبيت تطبيق Gallery إلى حذف جميع التفضيلات والإعدادات المحلية المخزنة للتطبيق، مع بقاء صورك ومقاطع الفيديو الأصلية في تخزين هاتفك دون أي مساس.


### ٧. إدارة الأذونات وحقوق المستخدم:

تحتفظ بالتحكم الكامل في الأذونات الممنوحة لتطبيق Gallery، ويمكنك تعديلها أو إلغاؤها في أي وقت من خلال إعدادات Android:

**إلغاء أذونات الوسائط / التخزين:**  
`الإعدادات > التطبيقات > Gallery > الأذونات > الصور ومقاطع الفيديو (أو التخزين) > عدم السماح`


### ٨. التواصل والدعم:

إذا كانت لديك أي استفسارات أو ملاحظات بخصوص سياسة الخصوصية هذه، يُرجى التواصل معنا عبر:
* **البريد الإلكتروني:** hamzabellouchcontact@gmail.com
* **مستودع GitHub:** https://github.com/hamzabellouch/gallery
