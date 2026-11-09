package com.jobflow.algorithm;

public class ArrayQueue {
    private final int[] values;
    private int front;
    private int rear;
    private int size;

    public ArrayQueue(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity cannot be negative");
        }
        values = new int[capacity];
    }

    public void enqueue(int value) {
        if (size == values.length) {
            throw new IllegalStateException("Queue is full");
        }
        values[rear] = value;
        rear = (rear + 1) % values.length;
        size++;
    }

    public int dequeue() {
        if (isEmpty()) {
            throw new IllegalStateException("Queue is empty");
        }
        int value = values[front];
        front = (front + 1) % values.length;
        size--;
        return value;
    }

    public int peek() {
        if (isEmpty()) {
            throw new IllegalStateException("Queue is empty");
        }
        return values[front];
    }

    public boolean isEmpty() {
        return size == 0;
    }
}
