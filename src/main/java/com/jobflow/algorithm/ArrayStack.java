package com.jobflow.algorithm;

public class ArrayStack {
    private final int[] values;
    private int top = -1;

    public ArrayStack(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity cannot be negative");
        }
        values = new int[capacity];
    }

    public void push(int value) {
        if (top == values.length - 1) {
            throw new IllegalStateException("Stack is full");
        }
        values[++top] = value;
    }

    public int pop() {
        if (isEmpty()) {
            throw new IllegalStateException("Stack is empty");
        }
        return values[top--];
    }

    public int peek() {
        if (isEmpty()) {
            throw new IllegalStateException("Stack is empty");
        }
        return values[top];
    }

    public boolean isEmpty() {
        return top == -1;
    }
}
