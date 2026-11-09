package com.jobflow.algorithm;

public final class SortingAlgorithms {
    private SortingAlgorithms() {
    }

    // Time Complexity: O(n^2) average/worst, O(n) best
    // Auxiliary Space Complexity: O(1)
    public static void bubbleSort(int[] arr) {
        for (int end = arr.length - 1; end > 0; end--) {
            boolean swapped = false;
            for (int i = 0; i < end; i++) {
                if (arr[i] > arr[i + 1]) {
                    swap(arr, i, i + 1);
                    swapped = true;
                }
            }
            if (!swapped) {
                break;
            }
        }
    }

    // Time Complexity: O(n^2) in all cases
    // Auxiliary Space Complexity: O(1)
    public static void selectionSort(int[] arr) {
        for (int i = 0; i < arr.length - 1; i++) {
            int minimumIndex = i;
            for (int j = i + 1; j < arr.length; j++) {
                if (arr[j] < arr[minimumIndex]) {
                    minimumIndex = j;
                }
            }
            if (minimumIndex != i) {
                swap(arr, i, minimumIndex);
            }
        }
    }

    // Time Complexity: O(n^2) average/worst, O(n) best
    // Auxiliary Space Complexity: O(1)
    public static void insertionSort(int[] arr) {
        for (int i = 1; i < arr.length; i++) {
            int value = arr[i];
            int position = i - 1;
            while (position >= 0 && arr[position] > value) {
                arr[position + 1] = arr[position];
                position--;
            }
            arr[position + 1] = value;
        }
    }

    // Time Complexity: O(n log n)
    // Auxiliary Space Complexity: O(n)
    public static void mergeSort(int[] arr) {
        if (arr.length < 2) {
            return;
        }
        mergeSort(arr, new int[arr.length], 0, arr.length - 1);
    }

    private static void mergeSort(int[] arr, int[] temporary, int left, int right) {
        if (left >= right) {
            return;
        }

        int middle = left + (right - left) / 2;
        mergeSort(arr, temporary, left, middle);
        mergeSort(arr, temporary, middle + 1, right);
        merge(arr, temporary, left, middle, right);
    }

    private static void merge(int[] arr, int[] temporary, int left, int middle, int right) {
        int first = left;
        int second = middle + 1;
        int destination = left;

        while (first <= middle && second <= right) {
            if (arr[first] <= arr[second]) {
                temporary[destination++] = arr[first++];
            } else {
                temporary[destination++] = arr[second++];
            }
        }
        while (first <= middle) {
            temporary[destination++] = arr[first++];
        }
        while (second <= right) {
            temporary[destination++] = arr[second++];
        }
        System.arraycopy(temporary, left, arr, left, right - left + 1);
    }

    private static void swap(int[] arr, int first, int second) {
        int value = arr[first];
        arr[first] = arr[second];
        arr[second] = value;
    }
}
