package com.example.model

import java.io.File

/**
 * Represents an item rendered in the Reader UI (pages, inter-chapter separators, end-of-series).
 */
sealed interface ReaderItem {

    /**
     * An individual manga/manhwa page.
     */
    data class Page(
        val chapter: Chapter,
        val pageIndex: Int,
        val totalPages: Int,
        val file: File
    ) : ReaderItem {
        val uniqueKey: String get() = "${chapter.originalFileName}_page_${pageIndex}_${file.name}"
    }

    /**
     * Inter-chapter separator displayed between consecutive chapters in vertical continuous reading.
     */
    data class ChapterSeparator(
        val previousChapter: Chapter,
        val nextChapter: Chapter
    ) : ReaderItem {
        val uniqueKey: String get() = "separator_${previousChapter.originalFileName}_to_${nextChapter.originalFileName}"
    }

    /**
     * Displayed at the very end of the final available chapter.
     */
    data class SeriesEnd(
        val chapter: Chapter
    ) : ReaderItem {
        val uniqueKey: String get() = "series_end_${chapter.originalFileName}"
    }
}
