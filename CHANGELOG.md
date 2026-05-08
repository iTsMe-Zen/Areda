# Changelog

## v1.0.1

Bug fix and reader polish release.

### Added

- Added a compact "Go to" overlay from the reader footer.
- EPUB reader footer now shows the current chapter number.
- Tapping the EPUB chapter footer opens chapter jump input.
- PDF reader footer now shows the current page number.
- Tapping the PDF page footer opens page jump input.

### Improved

- Improved EPUB opening by avoiding full archive extraction up front.
- Improved PDF opening by moving renderer loading off the UI thread.
- Improved library/search indexing so it does not interfere with reader opening.
- Restored smoother cached/indexed search behavior.
- Improved Pin/Unpin action text behavior.
- Finalized custom launcher icon resources.

### Fixed

- Fixed launcher icon XML/resource build issue.
- Blocked empty, invalid, and out-of-range jump input to avoid crashes.
- Removed small unused UI import/residue.


## v1.0.0

Initial public release.

### Added

- EPUB reading support
- TXT reading support
- PDF reading support
- Basic plain-text note support
- Local library access using Android's Storage Access Framework
- Recent documents shelf
- Saved reading progress
- Reader settings for theme, font family, and font size
- Pinch-to-zoom reading for EPUB and PDF
- Offline-only design with no internet permission
- No ads, analytics, or tracking