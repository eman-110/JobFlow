package com.jobflow.algorithm;

public class SinglyLinkedList {
    private Node head;
    private Node tail;
    private int size;

    public void insertFirst(int data) {
        Node node = new Node(data);
        node.next = head;
        head = node;
        if (tail == null) {
            tail = node;
        }
        size++;
    }

    public void insertLast(int data) {
        Node node = new Node(data);
        if (tail == null) {
            head = node;
        } else {
            tail.next = node;
        }
        tail = node;
        size++;
    }

    public void insertAt(int index, int data) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", size: " + size);
        }
        if (index == 0) {
            insertFirst(data);
            return;
        }
        if (index == size) {
            insertLast(data);
            return;
        }

        Node previous = nodeAt(index - 1);
        Node node = new Node(data);
        node.next = previous.next;
        previous.next = node;
        size++;
    }

    public void deleteFirst() {
        if (head == null) {
            throw new IllegalStateException("List is empty");
        }
        head = head.next;
        size--;
        if (head == null) {
            tail = null;
        }
    }

    public void deleteLast() {
        if (tail == null) {
            throw new IllegalStateException("List is empty");
        }
        if (head == tail) {
            head = null;
            tail = null;
        } else {
            Node previous = nodeAt(size - 2);
            previous.next = null;
            tail = previous;
        }
        size--;
    }

    public boolean deleteValue(int value) {
        if (head == null) {
            return false;
        }
        if (head.data == value) {
            deleteFirst();
            return true;
        }

        Node previous = head;
        while (previous.next != null && previous.next.data != value) {
            previous = previous.next;
        }
        if (previous.next == null) {
            return false;
        }

        if (previous.next == tail) {
            tail = previous;
        }
        previous.next = previous.next.next;
        size--;
        return true;
    }

    public boolean contains(int value) {
        for (Node current = head; current != null; current = current.next) {
            if (current.data == value) {
                return true;
            }
        }
        return false;
    }

    public int size() {
        return size;
    }

    public void reverse() {
        Node previous = null;
        Node current = head;
        tail = head;
        while (current != null) {
            Node next = current.next;
            current.next = previous;
            previous = current;
            current = next;
        }
        head = previous;
    }

    public void display() {
        Node current = head;
        while (current != null) {
            System.out.print(current.data + " -> ");
            current = current.next;
        }
        System.out.println("null");
    }

    public int findMiddle() {
        if (head == null) {
            throw new IllegalStateException("List is empty");
        }

        Node slow = head;
        Node fast = head;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }
        return slow.data;
    }

    private Node nodeAt(int index) {
        Node current = head;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }
        return current;
    }

    private static class Node {
        private final int data;
        private Node next;

        private Node(int data) {
            this.data = data;
        }
    }
}
