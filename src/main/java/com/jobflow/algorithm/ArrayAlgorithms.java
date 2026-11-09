package com.jobflow.algorithm;

public class ArrayAlgorithms {

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

    // Time Complexity: O(n)
    // Auxiliary Space Complexity: O(1)
    public static int findMax(int[] arr) {
        if (arr.length == 0) {
            throw new IllegalArgumentException("Array must not be empty");
        }

        int max = arr[0];
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > max) {
                max = arr[i];
            }
        }
        return max;
    }

    // Time Complexity: O(n)
    // Auxiliary Space Complexity: O(1)
    public static int findMin(int[] arr) {
        if (arr.length == 0) {
            throw new IllegalArgumentException("Array must not be empty");
        }

        int min = arr[0];
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] < min) {
                min = arr[i];
            }
        }
        return min;
    }

    // Time Complexity: O(n)
    // Auxiliary Space Complexity: O(1)
    public static void reverseArray(int[] arr) {
        int left = 0;
        int right = arr.length - 1;
        while (left < right) {
            int value = arr[left];
            arr[left] = arr[right];
            arr[right] = value;
            left++;
            right--;
        }
    }

    // Time Complexity: O(n)
    // Auxiliary Space Complexity: O(1)
    public static int calculateSum(int[] arr) {
        int sum = 0;
        for (int value : arr) {
            sum += value;
        }
        return sum;
    }

    // Time Complexity: O(n)
    // Auxiliary Space Complexity: O(1)
    public static int countEven(int[] arr) {
        int count = 0;
        for (int value : arr) {
            if (value % 2 == 0) {
                count++;
            }
        }
        return count;
    }

    // Time Complexity: O(n)
    // Auxiliary Space Complexity: O(1)
    public static double averageSalary(double[] salaries) {
        if (salaries.length == 0) {
            throw new IllegalArgumentException("Salaries must not be empty");
        }

        double sum = 0;
        for (double salary : salaries) {
            sum += salary;
        }
        return sum / salaries.length;
    }

    // Time Complexity: O(n)
    // Auxiliary Space Complexity: O(1)
    public static int secondLargest(int[] arr) {
        boolean hasLargest = false;
        boolean hasSecondLargest = false;
        int largest = 0;
        int secondLargest = 0;

        for (int value : arr) {
            if (!hasLargest || value > largest) {
                if (hasLargest) {
                    secondLargest = largest;
                    hasSecondLargest = true;
                }
                largest = value;
                hasLargest = true;
            } else if (value != largest && (!hasSecondLargest || value > secondLargest)) {
                secondLargest = value;
                hasSecondLargest = true;
            }
        }

        if (!hasSecondLargest) {
            throw new IllegalArgumentException("Array must contain at least two distinct values");
        }
        return secondLargest;
    }

    // Time Complexity: O(n^2)
    // Auxiliary Space Complexity: O(1)
    public static int[] twoSum(int[] arr, int target) {
        for (int i = 0; i < arr.length; i++) {
            for (int j = i + 1; j < arr.length; j++) {
                if ((long) arr[i] + arr[j] == target) {
                    return new int[]{i, j};
                }
            }
        }
        return new int[0];
    }
}
