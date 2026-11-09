package com.jobflow.algorithm;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchingSortingHashingTest {

    @Test
    void shouldSearchLinearAndBinaryArrays() {
        int[] values = {-8, -2, 0, 4, 9};
        assertEquals(3, SearchingAlgorithms.linearSearch(values, 4));
        assertEquals(-1, SearchingAlgorithms.linearSearch(values, 5));
        assertEquals(4, SearchingAlgorithms.binarySearch(values, 9));
        assertEquals(-1, SearchingAlgorithms.binarySearch(values, 5));
        assertEquals(-1, SearchingAlgorithms.binarySearch(new int[0], 5));
    }

    @Test
    void shouldSortWithAllAlgorithms() {
        assertSorted(SortingAlgorithms::bubbleSort);
        assertSorted(SortingAlgorithms::selectionSort);
        assertSorted(SortingAlgorithms::insertionSort);
        assertSorted(SortingAlgorithms::mergeSort);
    }

    @Test
    void shouldSolveHashingAlgorithms() {
        assertArrayEquals(new int[]{0, 1}, HashingAlgorithms.twoSumOptimized(new int[]{2, 7, 11}, 9));
        assertArrayEquals(new int[]{0, 1}, HashingAlgorithms.twoSumOptimized(new int[]{3, 3}, 6));
        assertArrayEquals(new int[0], HashingAlgorithms.twoSumOptimized(new int[]{1, 2}, 10));
        assertArrayEquals(new int[0], HashingAlgorithms.twoSumOptimized(
                new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE}, -2));

        assertTrue(HashingAlgorithms.containsDuplicate(new int[]{1, 2, 3, 2}));
        assertFalse(HashingAlgorithms.containsDuplicate(new int[]{1, 2, 3}));
        assertFalse(HashingAlgorithms.containsDuplicate(new int[0]));
        assertEquals(Map.of(1, 1, 2, 2, 3, 3),
                HashingAlgorithms.frequencyCount(new int[]{1, 2, 2, 3, 3, 3}));

        assertEquals('w', HashingAlgorithms.firstNonRepeating("swiss"));
        assertEquals('\0', HashingAlgorithms.firstNonRepeating("aabb"));
        assertEquals('\0', HashingAlgorithms.firstNonRepeating(""));

        assertEquals(List.of(
                        List.of("eat", "tea", "ate"),
                        List.of("tan", "nat"),
                        List.of("bat")),
                HashingAlgorithms.groupAnagrams(new String[]{"eat", "tea", "tan", "ate", "nat", "bat"}));
        assertEquals(List.of(), HashingAlgorithms.groupAnagrams(new String[0]));
    }

    private static void assertSorted(java.util.function.Consumer<int[]> sorter) {
        int[] values = {5, -1, 3, 3, 0, -8};
        sorter.accept(values);
        assertArrayEquals(new int[]{-8, -1, 0, 3, 3, 5}, values);

        int[] empty = new int[0];
        sorter.accept(empty);
        assertArrayEquals(new int[0], empty);

        int[] sorted = {-3, 0, 4, 7};
        sorter.accept(sorted);
        assertArrayEquals(new int[]{-3, 0, 4, 7}, sorted);
    }
}
