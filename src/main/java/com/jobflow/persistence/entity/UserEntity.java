package com.jobflow.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", length = 60)
    private String passwordHash;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.USER;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "user")
    private List<ApplicationEntity> applications = new ArrayList<>();

    protected UserEntity() {
    }

    public UserEntity(Integer id, String name, String email) {
        this.id = id;
        setName(name);
        setEmail(email);
    }

    public UserEntity(String name, String email) {
        this(null, name, email);
    }

    @PrePersist
    void initializeCreatedAt() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = requireText(name, 100, "User name");
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = requireText(email, 255, "Email");
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        if (passwordHash != null && (passwordHash.isBlank() || passwordHash.length() > 60)) {
            throw new IllegalArgumentException("Password hash must be non-blank and at most 60 characters.");
        }
        this.passwordHash = passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        if (role == null) {
            throw new IllegalArgumentException("User role is required.");
        }
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<ApplicationEntity> getApplications() {
        return applications;
    }

    public void addApplication(ApplicationEntity application) {
        if (application == null) {
            throw new IllegalArgumentException("Application is required.");
        }
        UserEntity previousUser = application.getUser();
        if (previousUser != null && previousUser != this) {
            previousUser.removeApplicationReference(application);
        }
        application.setUser(this);
        addApplicationReference(application);
    }

    void addApplicationReference(ApplicationEntity application) {
        if (!applications.contains(application)) {
            applications.add(application);
        }
    }

    void removeApplicationReference(ApplicationEntity application) {
        applications.remove(application);
    }

    private static String requireText(String value, int maxLength, String field) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw new IllegalArgumentException(field + " must be non-blank and at most "
                    + maxLength + " characters.");
        }
        return value;
    }
}
