package com.jobflow.algorithm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArrayAlgorithmsTest {

    @Test
    void shouldSearchAndCalculateArrayValues() {
        int[] numbers = {10, 40, 20, 50, 30};

        assertEquals(2, ArrayAlgorithms.linearSearch(numbers, 20));
        assertEquals(-1, ArrayAlgorithms.linearSearch(numbers, 99));
        assertEquals(50, ArrayAlgorithms.findMax(numbers));
        assertEquals(10, ArrayAlgorithms.findMin(numbers));
        assertEquals(150, ArrayAlgorithms.calculateSum(numbers));
        assertEquals(5, ArrayAlgorithms.countEven(numbers));
    }

    @Test
    void shouldReverseOriginalArray() {
        int[] numbers = {1, 2, 3, 4};

        ArrayAlgorithms.reverseArray(numbers);

        assertArrayEquals(new int[]{4, 3, 2, 1}, numbers);
    }

    @Test
    void shouldRejectEmptyArraysForMaxAndMin() {
        assertThrows(IllegalArgumentException.class, () -> ArrayAlgorithms.findMax(new int[0]));
        assertThrows(IllegalArgumentException.class, () -> ArrayAlgorithms.findMin(new int[0]));
    }

    @Test
    void shouldCalculateAverageSalary() {
        double[] salaries = {120000, 115000, 140000, 110000, 95000};

        assertEquals(116000.0, ArrayAlgorithms.averageSalary(salaries));
        assertThrows(IllegalArgumentException.class, () -> ArrayAlgorithms.averageSalary(new double[0]));
    }

    @Test
    void shouldFindSecondLargestDistinctValue() {
        assertEquals(40, ArrayAlgorithms.secondLargest(new int[]{10, 40, 20, 50, 30}));
        assertEquals(40, ArrayAlgorithms.secondLargest(new int[]{50, 50, 40, 20}));
        assertThrows(IllegalArgumentException.class, () -> ArrayAlgorithms.secondLargest(new int[]{7, 7}));
        assertThrows(IllegalArgumentException.class, () -> ArrayAlgorithms.secondLargest(new int[0]));
    }

    @Test
    void shouldReturnTwoSumIndexes() {
        assertArrayEquals(new int[]{0, 1}, ArrayAlgorithms.twoSum(new int[]{2, 7, 11, 15}, 9));
        assertArrayEquals(new int[0], ArrayAlgorithms.twoSum(new int[]{2, 7}, 20));
    }
}
