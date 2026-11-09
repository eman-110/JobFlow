package com.jobflow.algorithm;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

public final class ValidParentheses {
    private ValidParentheses() {
    }

    public static boolean isValid(String text) {
        Objects.requireNonNull(text, "Text cannot be null");
        Deque<Character> openings = new ArrayDeque<>();
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (current == '(' || current == '[' || current == '{') {
                openings.push(current);
            } else if (current == ')' || current == ']' || current == '}') {
                if (openings.isEmpty() || !matches(openings.pop(), current)) {
                    return false;
                }
            }
        }
        return openings.isEmpty();
    }

    private static boolean matches(char opening, char closing) {
        return (opening == '(' && closing == ')')
                || (opening == '[' && closing == ']')
                || (opening == '{' && closing == '}');
    }
}
