package com.jobflow.algorithm;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataStructuresTest {

    @Test
    void shouldInsertDeleteReverseAndFindMiddleInSinglyLinkedList() {
        SinglyLinkedList list = new SinglyLinkedList();
        list.insertLast(10);
        list.insertLast(20);
        list.insertLast(30);
        list.insertFirst(5);
        list.insertAt(2, 15);

        assertEquals(5, list.size());
        assertTrue(list.contains(20));
        assertEquals("5 -> 10 -> 15 -> 20 -> 30 -> null", captureOutput(list::display));
        assertTrue(list.deleteValue(20));
        assertFalse(list.deleteValue(20));
        assertEquals(4, list.size());
        assertEquals(15, list.findMiddle());

        list.reverse();
        assertEquals(10, list.findMiddle());
        assertEquals("30 -> 15 -> 10 -> 5 -> null", captureOutput(list::display));
        list.deleteLast();
        list.deleteFirst();
        assertEquals(2, list.size());
    }

    @Test
    void shouldInsertAtBoundariesAndRejectInvalidSinglyLinkedListOperations() {
        SinglyLinkedList list = new SinglyLinkedList();
        list.insertAt(0, 1);
        list.insertAt(1, 2);
        assertEquals(2, list.findMiddle());
        assertThrows(IndexOutOfBoundsException.class, () -> list.insertAt(3, 3));

        list.deleteLast();
        list.deleteFirst();
        assertThrows(IllegalStateException.class, list::deleteFirst);
        assertThrows(IllegalStateException.class, list::deleteLast);
        assertThrows(IllegalStateException.class, list::findMiddle);
        assertEquals(0, list.size());
    }

    @Test
    void shouldKeepDoublyLinkedListEndsConsistentAcrossEmptyAndSingleNodeOperations() {
        DoublyLinkedList list = new DoublyLinkedList();
        assertThrows(IllegalStateException.class, list::deleteFirst);
        assertThrows(IllegalStateException.class, list::deleteLast);

        list.insertFirst(10);
        assertTrue(list.contains(10));
        list.deleteLast();
        assertFalse(list.contains(10));
        list.insertLast(20);
        list.insertFirst(10);
        list.insertLast(30);
        assertEquals("10 <-> 20 <-> 30 <-> null", captureOutput(list::displayForward));
        assertEquals("30 <-> 20 <-> 10 <-> null", captureOutput(list::displayBackward));
        list.deleteFirst();
        list.deleteLast();
        assertEquals("20 <-> null", captureOutput(list::displayForward));
        list.deleteFirst();
        assertThrows(IllegalStateException.class, list::deleteFirst);
    }

    @Test
    void shouldEnforceArrayStackCapacityAndEmptyState() {
        ArrayStack stack = new ArrayStack(1);
        assertTrue(stack.isEmpty());
        assertThrows(IllegalStateException.class, stack::pop);
        assertThrows(IllegalStateException.class, stack::peek);
        stack.push(7);
        assertEquals(7, stack.peek());
        assertThrows(IllegalStateException.class, () -> stack.push(8));
        assertEquals(7, stack.pop());
        assertTrue(stack.isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new ArrayStack(-1));
        assertThrows(IllegalStateException.class, () -> new ArrayStack(0).push(1));
    }

    @Test
    void shouldWrapArrayQueueAndHandleZeroCapacity() {
        ArrayQueue queue = new ArrayQueue(2);
        assertTrue(queue.isEmpty());
        assertThrows(IllegalStateException.class, queue::dequeue);
        assertThrows(IllegalStateException.class, queue::peek);
        queue.enqueue(1);
        queue.enqueue(2);
        assertThrows(IllegalStateException.class, () -> queue.enqueue(3));
        assertEquals(1, queue.dequeue());
        queue.enqueue(3);
        assertEquals(2, queue.peek());
        assertEquals(2, queue.dequeue());
        assertEquals(3, queue.dequeue());
        assertTrue(queue.isEmpty());

        ArrayQueue zeroCapacityQueue = new ArrayQueue(0);
        assertThrows(IllegalStateException.class, () -> zeroCapacityQueue.enqueue(1));
        assertThrows(IllegalStateException.class, zeroCapacityQueue::dequeue);
        assertThrows(IllegalArgumentException.class, () -> new ArrayQueue(-1));
    }

    @Test
    void shouldValidateParenthesesAndBrackets() {
        assertTrue(ValidParentheses.isValid("()"));
        assertTrue(ValidParentheses.isValid("({[]})"));
        assertTrue(ValidParentheses.isValid("text (with [brackets])"));
        assertFalse(ValidParentheses.isValid("(]"));
        assertFalse(ValidParentheses.isValid("([)]"));
        assertFalse(ValidParentheses.isValid("((("));
    }

    private static String captureOutput(Runnable action) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream capturedOut = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(capturedOut);
            action.run();
        } finally {
            System.setOut(originalOut);
        }
        return output.toString(StandardCharsets.UTF_8).trim();
    }
}
