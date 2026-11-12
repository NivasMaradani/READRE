package com.example.util

import java.util.regex.Pattern

/**
 * Natural numerical order comparator for chapters and image filenames.
 * Correctly sorts: 1.jpg, 2.jpg, 10.jpg, 11.jpg
 * and "Chapter 2", "Chapter 10", "Chapter 1095" etc.
 */
object NaturalOrderComparator : Comparator<String> {

    private val CHUNK_PATTERN = Pattern.compile("(\\d+)|(\\D+)")

    override fun compare(s1: String?, s2: String?): Int {
        if (s1 == null && s2 == null) return 0
        if (s1 == null) return -1
        if (s2 == null) return 1

        val m1 = CHUNK_PATTERN.matcher(s1)
        val m2 = CHUNK_PATTERN.matcher(s2)

        while (m1.find()) {
            if (!m2.find()) {
                return 1 // s1 has more chunks
            }

            val chunk1 = m1.group()
            val chunk2 = m2.group()

            val isDigit1 = Character.isDigit(chunk1[0])
            val isDigit2 = Character.isDigit(chunk2[0])

            val result = if (isDigit1 && isDigit2) {
                // Compare numerically
                // Remove leading zeroes for fair comparison, but keep track if length differs
                val num1 = chunk1.trimStart('0')
                val num2 = chunk2.trimStart('0')

                val lenDiff = num1.length - num2.length
                if (lenDiff != 0) {
                    lenDiff
                } else {
                    val comp = num1.compareTo(num2)
                    if (comp != 0) comp else chunk1.length - chunk2.length
                }
            } else {
                chunk1.compareTo(chunk2, ignoreCase = true)
            }

            if (result != 0) {
                return result
            }
        }

        return if (m2.find()) -1 else s1.compareTo(s2)
    }
}
