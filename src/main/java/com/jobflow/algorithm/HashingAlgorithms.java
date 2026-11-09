package com.jobflow.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class HashingAlgorithms {
    private HashingAlgorithms() {
    }

    // Time Complexity: O(n) expected
    // Auxiliary Space Complexity: O(n)
    public static int[] twoSumOptimized(int[] arr, int target) {
        Map<Integer, Integer> indexesByValue = new HashMap<>();
        for (int i = 0; i < arr.length; i++) {
            long complement = (long) target - arr[i];
            if (complement >= Integer.MIN_VALUE && complement <= Integer.MAX_VALUE) {
                Integer previousIndex = indexesByValue.get((int) complement);
                if (previousIndex != null) {
                    return new int[]{previousIndex, i};
                }
            }
            indexesByValue.putIfAbsent(arr[i], i);
        }
        return new int[0];
    }

    // Time Complexity: O(n) expected
    // Auxiliary Space Complexity: O(n)
    public static boolean containsDuplicate(int[] arr) {
        Set<Integer> values = new HashSet<>();
        for (int value : arr) {
            if (!values.add(value)) {
                return true;
            }
        }
        return false;
    }

    // Time Complexity: O(n) expected
    // Auxiliary Space Complexity: O(n)
    public static Map<Integer, Integer> frequencyCount(int[] arr) {
        Map<Integer, Integer> frequencies = new LinkedHashMap<>();
        for (int value : arr) {
            frequencies.merge(value, 1, Integer::sum);
        }
        return frequencies;
    }

    // Time Complexity: O(n) expected
    // Auxiliary Space Complexity: O(k), where k is the number of distinct characters.
    public static char firstNonRepeating(String text) {
        Map<Character, Integer> frequencies = new HashMap<>();
        for (int i = 0; i < text.length(); i++) {
            frequencies.merge(text.charAt(i), 1, Integer::sum);
        }
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (frequencies.get(character) == 1) {
                return character;
            }
        }
        return '\0';
    }

    // Time Complexity: O(n * m log m), where m is the maximum word length.
    // Auxiliary Space Complexity: O(n * m)
    public static List<List<String>> groupAnagrams(String[] words) {
        Map<String, List<String>> groups = new LinkedHashMap<>();
        for (String word : words) {
            char[] characters = word.toCharArray();
            Arrays.sort(characters);
            String key = new String(characters);
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(word);
        }
        return new ArrayList<>(groups.values());
    }
}
