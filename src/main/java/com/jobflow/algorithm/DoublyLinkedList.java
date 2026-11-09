package com.jobflow.algorithm;

public class DoublyLinkedList {
    private Node head;
    private Node tail;

    public void insertFirst(int data) {
        Node node = new Node(data);
        node.next = head;
        if (head == null) {
            tail = node;
        } else {
            head.previous = node;
        }
        head = node;
    }

    public void insertLast(int data) {
        Node node = new Node(data);
        node.previous = tail;
        if (tail == null) {
            head = node;
        } else {
            tail.next = node;
        }
        tail = node;
    }

    public void deleteFirst() {
        if (head == null) {
            throw new IllegalStateException("List is empty");
        }
        head = head.next;
        if (head == null) {
            tail = null;
        } else {
            head.previous = null;
        }
    }

    public void deleteLast() {
        if (tail == null) {
            throw new IllegalStateException("List is empty");
        }
        tail = tail.previous;
        if (tail == null) {
            head = null;
        } else {
            tail.next = null;
        }
    }

    public boolean contains(int value) {
        for (Node current = head; current != null; current = current.next) {
            if (current.data == value) {
                return true;
            }
        }
        return false;
    }

    public void displayForward() {
        Node current = head;
        while (current != null) {
            System.out.print(current.data + " <-> ");
            current = current.next;
        }
        System.out.println("null");
    }

    public void displayBackward() {
        Node current = tail;
        while (current != null) {
            System.out.print(current.data + " <-> ");
            current = current.previous;
        }
        System.out.println("null");
    }

    private static class Node {
        private final int data;
        private Node previous;
        private Node next;

        private Node(int data) {
            this.data = data;
        }
    }
}
