# Reader — Minimal Offline Manga & Manhwa Reader

Reader is a high-performance, distraction-free native Android application built with Kotlin and Jetpack Compose, designed exclusively for reading manga, manhwa, and comics from local `.zip` and `.cbz` archives stored on your device.

It is **100% offline**, requiring zero internet connectivity, no user accounts, and zero cloud or telemetry services.

---

## 1. Initial Storage Setup

When Reader is launched:

1. **First-Launch Tutorial**: A 9-step interactive guide introduces you to the app's features, gestures, reading modes, and storage layout.
2. **Storage Location Setup**: Tapping **Select Storage Location** launches Android's system folder picker (Storage Access Framework).
3. **Automatic Reader Directory**:
   - You can select any directory (such as `Documents`, `Downloads`, or `Internal Storage`).
   - Reader automatically creates a folder named `Reader` inside your selected location (or reuses an existing `Reader` directory if present).
4. **Persistent Permission**:
   - Read and write permissions are persisted securely across device restarts and app relaunches without requesting broad device storage permissions.
5. **Folder Organization**:
   ```
   Internal Storage/
   └── Reader/
       ├── One Piece/
       │   ├── Chapter 001.cbz
       │   ├── Chapter 002.cbz
       │   └── Chapter 003.zip
       ├── Solo Leveling/
       │   ├── Chapter 001.cbz
       │   └── Chapter 002.zip
       └── Tower of God/
           ├── Chapter 001.zip
           └── Chapter 002.cbz
   ```
   - Each immediate subdirectory inside `Reader` represents one **Manga / Manhwa**.
   - Each `.zip` or `.cbz` file inside that folder represents one **Chapter**.

---

## 2. Key Features

### Local Storage & Archive Engine
- **Android Storage Access Framework (SAF)**: Zero-permission photo picker and privacy-compliant document tree access.
- **Direct Local Reading**: Extracts and streams pages directly from archives without copying, renaming, or modifying your original files.
- **Archive Formats**: Supports standard `.zip` and `.cbz` archives containing JPG, JPEG, PNG, WEBP, and GIF images while ignoring system metadata (`Thumbs.db`, `.DS_Store`, `__MACOSX`, `.txt`, `.xml`).
- **Natural Numeric Sorting**: Alphanumeric natural sorting ensures chapters and pages are ordered correctly (`1.jpg`, `2.jpg`, `10.jpg`, `Chapter 1`, `Chapter 2`, `Chapter 10`, `Chapter 1095`).
- **Automatic Cache Pruning**: Extracted pages are cached in app-internal cache and pruned to keep disk usage low.

### Automatic Cover Generation & Customization
- **Automatic Cover Extraction**: Inspects the first chapter's archive entries using natural numeric order, extracts the first image, downscales it into a thumbnail (`300x420` target size), and caches it locally.
- **Unified Cover Artwork Presentation**: Both automatic covers and manually selected custom covers share identical visual presentation—clean artwork with the manga title overlaid at the bottom—free from identifying source labels or badges.
- **Three Cover Management Actions** (via manga card menu):
  - **Choose Custom Cover**: Select any image from your device via the system photo picker.
  - **Restore Automatic Cover**: Clears manual overrides and generates or restores the first-chapter thumbnail.
  - **Regenerate Automatic Cover**: Re-reads the chapter archive and regenerates the cached thumbnail.
- **Rescan Safety**: Custom covers are preserved across library rescans and folder refreshes.
- **Graceful Fallbacks**: Renders a neutral placeholder if an archive contains no valid image files.

### Hidden Manga & Protected Library
- **Hide Manga**: Hide any title directly from the manga card options menu.
- **Complete Isolation**:
  - Hidden titles disappear immediately from the normal library grid and search.
  - Excluded from **Continue Reading**, **Reading History**, and **Bookmarks** queries.
  - Original archive files on disk remain untouched.
- **Developer-Mode Activation**:
  - Go to **Settings** and tap the **Reader** title/logo **7 consecutive times** to activate the protected library.
- **Cryptographic PIN Security (`SecurityManager`)**:
  - PINs are cryptographically hashed using PBKDF2WithHmacSHA256 (with salted multi-round SHA-256 fallback) with a secure 16-byte random salt.
  - Constant-time comparison prevents timing attacks. PINs are never stored in plaintext.
  - Prompts for initial PIN setup with confirmation; supports PIN verification and PIN changes.
- **Dedicated Hidden Library Screen**:
  - Browse and read hidden manga in an isolated space.
  - Actions to **Unhide Manga**, customize covers, **Lock Now**, or **Change PIN**.
  - Includes a clear privacy notice banner.

### Minimalist Chapter List
- **Clean Aesthetic**: Restrained, monochrome styling without distracting visual badges or neon colors.
- **Completed Chapters**: Displayed with subtle reduced opacity while keeping title, filename, and file size readable.
- **In-Progress Chapters**: Displayed at normal brightness with clean, neutral text (e.g. `Page 3 of 25`).
- **Unread Chapters**: Displayed at standard brightness without progress indicators.
- **Independent Bookmarks**: Manual chapter bookmarks remain fully functional and operate independently of completion status.

### Reading Modes, Navigation & In-Reader Settings
- **Single Page Mode**:
  - Fits images to screen width while maintaining the original aspect ratio (never cropped).
  - Multi-touch pinch-to-zoom (up to 5x) and pan navigation with double-tap reset.
  - Horizontal swipe pagination respecting reading direction.
- **Vertical Continuous Mode (Webtoon / Manhwa)**:
  - Continuous vertical layout with zero spacing between pages.
  - Lazy-loaded pages for memory-efficient scrolling.
- **Compact Page Indicator (`042 / 186`)**:
  - Clean numeric display in bottom overlay replacing the old horizontal slider.
  - Displays current page index and total chapter page count.
- **Numeric Wheel Picker**:
  - Long-press or tap the page indicator to open a vertically scrollable number wheel inspired by clock/timer scrolling wheels.
  - Scrolling upwards increases page numbers; scrolling downwards decreases them.
  - Clamped strictly within `1..totalPages`.
  - Tapping any number immediately navigates to that page and dismisses the picker without loading full-resolution chapter images into memory.
  - Quick step buttons (`-10`, `-1`, `+1`, `+10`) for jumping through long chapters.
- **In-Reader Settings Panel**:
  - Accessible directly inside the reader via the settings icon on the top overlay and bottom controls.
  - Compact `ModalBottomSheet` displaying context-aware controls for the active reading mode.
  - Single source of truth: all changes synchronize bidirectionally with general settings and persist in Room SQLite.
- **Configurable Paged Tap Zones**:
  - Default: **Right tap = Next page**, **Left tap = Previous page**.
  - Configurable actions: Right Tap (Next / Previous), Left Tap (Previous / Next), Reverse Navigation (On / Off).
  - Consistent RTL (Right-to-Left) and LTR (Left-to-Right) handling.
- **Configurable Vertical Scrolling**:
  - Tap-Scroll Distance / Speed: Configurable from **20% to 100% of viewport** (default: 50%).
  - Hold-to-Scroll Speed: Configurable from **100 to 2000 pixels/second** (default: 650 px/s).
  - Hold activation delay: Configurable from **150 ms to 800 ms** (default: 300 ms).
  - Reverse hold-scroll direction toggle.
- **Distraction-Free Controls Overlay**:
  - Hidden by default; single tap on the center screen toggles top and bottom controls.

### Interactive First-Launch Tutorial
- **9-Step Guided Onboarding**: Automatically launched on first startup to guide users through storage layout, archive formats, reading modes, gesture controls, bookmarks, covers, and hidden library features.
- **Step Controls**: Step indicator dots, step counter badge, **Previous**, **Next**, and **Skip** buttons.
- **Replay Tutorial**: Accessible at any time via **Settings → Help & About → Replay Tutorial** without altering reading progress, bookmarks, or database records.

---

## 3. Architecture & Technical Details

- **Language & Framework**: Kotlin with 100% Jetpack Compose and Material Design 3 (M3).
- **Architecture**: MVVM with Clean Architecture principles (`Model`, `Repository`, `ViewModel`, `ScreenState`, `UiState`).
- **Local Persistence (Room Database v4)**:
  - `MangaEntity` / `indexed_manga`: Stores indexed folders, custom cover URIs, and `isHidden` status.
  - `ChapterProgressEntity`: Tracks per-chapter reading progress and page counts.
  - `ReadingProgressEntity`: Stores the latest active chapter and page per manga.
  - `BookmarkEntity`: Stores user-created chapter bookmarks.
  - `AppSettingsEntity`: Stores user preferences (reading mode, direction, tap actions, scroll speed, hold delay).
  - **Migrations**: Non-destructive migrations (`MIGRATION_1_2`, `MIGRATION_2_3`, `MIGRATION_3_4`) ensure user data is never wiped or lost.
- **Preferences DataStore**: Used for non-relational client preferences such as tutorial completion and encrypted security metadata.
- **Strict Offline Guarantees**:
  - Zero Google Services or Firebase plugins.
  - Zero analytics, trackers, or network permission declarations (`android.permission.INTERNET` is omitted).

---

## 4. Build & Installation

### Prerequisites
- JDK 17 or higher
- Android SDK (API 36 compile SDK, min SDK 24)

### Building the Project
To compile the debug APK:
```bash
gradle assembleDebug
```
The resulting APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

To compile the release APK:
```bash
gradle assembleRelease
```

### Running Unit and Robolectric Tests
```bash
gradle :app:testDebugUnitTest
```

---

## 5. Test Coverage & Verification

The project includes unit and Robolectric tests covering all core features:

1. **`FeatureEnhancementsTest`**:
   - **Automatic Cover Extraction**: Verifies natural sorting of archive entries (`10.jpg`, `02.jpg`, `01.jpg` correctly selecting `01.jpg`).
   - **Cover Management**: Verifies custom cover override, restore automatic cover, and on-demand regeneration.
   - **SecurityManager**: Verifies PIN creation with 16-byte random salt, PBKDF2 hashing, constant-time validation, incorrect PIN rejection, PIN updates, and absence of plaintext PIN storage.
   - **Hidden Library Isolation**: Verifies hidden manga are excluded from normal library, search, continue reading, history, and bookmarks, and restored upon unhiding.
   - **Database Migration 3 → 4**: Verifies Room `MIGRATION_3_4` adds the `isHidden` column cleanly.
   - **Tutorial State**: Verifies initial launch detection, persistence, and non-destructive replay.

2. **`NaturalOrderComparatorTest`**:
   - Verifies natural alphanumeric sorting across image names, chapter strings, and edge cases.

3. **`ArchiveManagerTest`**:
   - Verifies supported image extensions, blacklist exclusion, archive extraction, and corrupted zip resilience.

4. **`ReaderDatabaseTest`**:
   - Verifies CRUD operations for reading progress, bookmarks, and settings.
