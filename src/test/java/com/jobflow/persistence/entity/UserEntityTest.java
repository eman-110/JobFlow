package com.jobflow.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserEntityTest {

    @Test
    void shouldInitializeUserFields() {
        UserEntity user = new UserEntity(5, "Alex Morgan", "alex@example.test");

        assertEquals(5, user.getId());
        assertEquals("Alex Morgan", user.getName());
        assertEquals("alex@example.test", user.getEmail());
    }

    @Test
    void shouldSetCreatedAtOnPersistenceAndPreserveAnExistingValue() {
        UserEntity user = new UserEntity("Alex Morgan", "alex@example.test");

        user.initializeCreatedAt();
        assertNotNull(user.getCreatedAt());

        LocalDateTime createdAt = LocalDateTime.of(2026, 2, 3, 4, 5);
        user.setCreatedAt(createdAt);
        user.initializeCreatedAt();
        assertEquals(createdAt, user.getCreatedAt());
    }

    @Test
    void shouldRejectBlankAndOverlongUserFields() {
        assertThrows(IllegalArgumentException.class, () -> new UserEntity(" ", "alex@example.test"));
        assertThrows(IllegalArgumentException.class,
                () -> new UserEntity("Alex", "x".repeat(256)));
    }

    @Test
    void shouldMapUserTableAndUniqueEmailColumn() throws Exception {
        Table table = UserEntity.class.getAnnotation(Table.class);
        Column name = UserEntity.class.getDeclaredField("name").getAnnotation(Column.class);
        Column email = UserEntity.class.getDeclaredField("email").getAnnotation(Column.class);
        Column createdAt = UserEntity.class.getDeclaredField("createdAt").getAnnotation(Column.class);

        assertEquals("users", table.name());
        assertEquals(100, name.length());
        assertEquals(false, name.nullable());
        assertEquals(255, email.length());
        assertEquals(false, email.nullable());
        assertEquals(true, email.unique());
        assertEquals(false, createdAt.updatable());
    }
}
