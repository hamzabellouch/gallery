# Security Policy

## Supported Versions

The following versions of Gallery are currently supported with security, privacy, and stability updates.

| Version       | Supported          |
| ------------- | ------------------ |
| >= 1.0.0      | :white_check_mark: |
| < 1.0.0       | :x:                |



## Reporting a Vulnerability

If you discover a security vulnerability, privacy issue, unexpected behavior with storage permissions, or any issue that could negatively affect users or their media data safety, please report it responsibly.

### Before Reporting
Please make sure that:
- The issue is reproducible
- You are using the latest supported version of Gallery
- The issue is not caused by third-party modifications, custom ROMs, or unsupported Android environments

### How to Report
You can report vulnerabilities through:
- GitHub Issues (for general non-sensitive bugs and enhancement requests)
- Direct private contact channels / email for sensitive vulnerabilities: `hamzabellouchcontact@gmail.com`

When reporting, please include:
- Device model and Android OS version
- Gallery application version
- Granted permissions state (e.g., Photos and videos / Storage)
- Steps to reproduce the issue
- Screenshots or system logcat outputs if available
- A clear explanation of the potential security or privacy impact

### Response Policy
Security reports are reviewed as quickly as possible.  
If the issue is confirmed:
- The vulnerability will be investigated, verified, and patched
- A fix will be included in the next update release
- Credit may be given to the reporter if requested

If the report is invalid, incomplete, or not reproducible, it may be closed without action.



## Security & Architecture Notes

Gallery is designed with a strict **Privacy-First & On-Device Security Model**:

- **100% Local Execution:** Media loading, indexing, caching, and 4K video playback operate entirely locally on your device. No photos, videos, audio tracks, or metadata are transmitted to external servers.
- **Local Metadata Inspection:** EXIF metadata inspection is performed locally using `AndroidX ExifInterface`. No external web requests or analytics services are used during file inspection.
- **Root-Free Operation:** Gallery does not require root privileges. It operates securely within standard Android sandbox protections using official Android system APIs (`MediaStore`, `AndroidX Media3 ExoPlayer`, `Coil 3`).
- **Transient Memory Handling:** All decoded image bitmaps, video decoding buffers, and temporary memory structures held during media rendering are managed in volatile memory (RAM) and automatically recycled upon application pause or termination to keep the footprint minimal and secure.
