package com.jobflow.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CompanyEntityTest {

    @Test
    void shouldInitializeAllCompanyFields() {
        CompanyEntity company = new CompanyEntity(7, "Northstar Labs", null, "Germany");

        assertEquals(7, company.getId());
        assertEquals("Northstar Labs", company.getName());
        assertNull(company.getWebsite());
        assertEquals("Germany", company.getCountry());
    }

    @Test
    void shouldAllowAnOmittedGeneratedId() {
        CompanyEntity company = new CompanyEntity("Northstar Labs", "https://northstar.example", "Germany");

        assertNull(company.getId());
        assertEquals("https://northstar.example", company.getWebsite());
    }

    @Test
    void shouldRejectBlankNameAndCountry() {
        assertThrows(IllegalArgumentException.class,
                () -> new CompanyEntity(" ", null, "Germany"));
        assertThrows(IllegalArgumentException.class,
                () -> new CompanyEntity("Northstar Labs", null, ""));
    }

    @Test
    void shouldMapRequiredFieldsToDatabaseColumns() throws Exception {
        Table table = CompanyEntity.class.getAnnotation(Table.class);
        Column name = CompanyEntity.class.getDeclaredField("name").getAnnotation(Column.class);
        Column country = CompanyEntity.class.getDeclaredField("country").getAnnotation(Column.class);

        assertEquals("companies", table.name());
        assertEquals(100, name.length());
        assertEquals(false, name.nullable());
        assertEquals(false, country.nullable());
    }
}
