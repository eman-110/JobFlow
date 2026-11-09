package com.jobflow.algorithm;

public final class SearchingAlgorithms {
    private SearchingAlgorithms() {
    }

    // Time Complexity: O(n)
    // Auxiliary Space Complexity: O(1)
    public static int linearSearch(int[] arr, int target) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == target) {
                return i;
            }
        }
        return -1;
    }

    // Time Complexity: O(log n)
    // Auxiliary Space Complexity: O(1)
    // Requires arr to be sorted in ascending order.
    public static int binarySearch(int[] arr, int target) {
        int left = 0;
        int right = arr.length - 1;
        while (left <= right) {
            int middle = left + (right - left) / 2;
            if (arr[middle] == target) {
                return middle;
            }
            if (arr[middle] < target) {
                left = middle + 1;
            } else {
                right = middle - 1;
            }
        }
        return -1;
    }
}
