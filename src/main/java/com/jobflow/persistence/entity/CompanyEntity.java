package com.jobflow.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "companies")
public class CompanyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String website;

    @Column(nullable = false, length = 100)
    private String country;

    protected CompanyEntity() {
    }

    public CompanyEntity(Integer id, String name, String website, String country) {
        this.id = id;
        setName(name);
        setWebsite(website);
        setCountry(country);
    }

    public CompanyEntity(String name, String website, String country) {
        this(null, name, website, country);
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
        this.name = requireText(name, 100, "Company name");
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        if (website != null && website.length() > 255) {
            throw new IllegalArgumentException("Website must not exceed 255 characters.");
        }
        this.website = website;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = requireText(country, 100, "Country");
    }

    private static String requireText(String value, int maxLength, String field) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw new IllegalArgumentException(field + " must be non-blank and at most "
                    + maxLength + " characters.");
        }
        return value;
    }
}
