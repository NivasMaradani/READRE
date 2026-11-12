package com.example

import com.example.util.NaturalOrderComparator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NaturalOrderComparatorTest {

    @Test
    fun `test natural numeric sorting of images`() {
        val input = listOf("10.jpg", "1.jpg", "2.jpg", "11.jpg", "3.jpg", "20.jpg", "21.jpg")
        val sorted = input.sortedWith { a, b -> NaturalOrderComparator.compare(a, b) }
        val expected = listOf("1.jpg", "2.jpg", "3.jpg", "10.jpg", "11.jpg", "20.jpg", "21.jpg")
        assertEquals(expected, sorted)
    }

    @Test
    fun `test natural sorting of chapter filenames`() {
        val input = listOf(
            "Chapter 1100.cbz",
            "Chapter 2.cbz",
            "Chapter 1095.cbz",
            "Chapter 10.zip",
            "Chapter 1.cbz",
            "Chapter 3.zip"
        )
        val sorted = input.sortedWith { a, b -> NaturalOrderComparator.compare(a, b) }
        val expected = listOf(
            "Chapter 1.cbz",
            "Chapter 2.cbz",
            "Chapter 3.zip",
            "Chapter 10.zip",
            "Chapter 1095.cbz",
            "Chapter 1100.cbz"
        )
        assertEquals(expected, sorted)
    }

    @Test
    fun `test natural sorting with leading zeroes`() {
        val input = listOf("003.png", "001.png", "002.png", "010.png")
        val sorted = input.sortedWith { a, b -> NaturalOrderComparator.compare(a, b) }
        val expected = listOf("001.png", "002.png", "003.png", "010.png")
        assertEquals(expected, sorted)
    }

    @Test
    fun `test natural sorting handles nulls and ties gracefully`() {
        assertTrue(NaturalOrderComparator.compare(null, "a") < 0)
        assertTrue(NaturalOrderComparator.compare("a", null) > 0)
        assertEquals(0, NaturalOrderComparator.compare(null, null))
        assertEquals(0, NaturalOrderComparator.compare("Chapter 1", "Chapter 1"))
    }
}
