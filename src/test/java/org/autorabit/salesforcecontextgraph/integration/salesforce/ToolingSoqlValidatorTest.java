package org.autorabit.salesforcecontextgraph.integration.salesforce;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ToolingSoqlValidatorTest {

    private final ToolingSoqlValidator validator = new ToolingSoqlValidator();

    @Test
    void shouldRejectOrderByClause() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validate("SELECT Id FROM ApexClass ORDER BY Name"));
        assertEquals("Tooling API queries do not support ORDER BY", ex.getMessage());
    }

    @Test
    void shouldRejectMetadataComponentNameFilter() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validate("""
                        SELECT MetadataComponentId
                        FROM MetadataComponentDependency
                        WHERE MetadataComponentName = 'TestClass'
                        """));
        assertEquals("MetadataComponentDependency queries cannot filter on MetadataComponentName", ex.getMessage());
    }

    @Test
    void shouldRejectUnsupportedMetadataDependencyOperator() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validate("""
                        SELECT MetadataComponentId
                        FROM MetadataComponentDependency
                        WHERE MetadataComponentId IN ('01p1', '01p2')
                        """));
        assertEquals(
                "MetadataComponentDependency queries only support =, !=, AND, and OR in WHERE clauses",
                ex.getMessage()
        );
    }

    @Test
    void shouldAllowSupportedMetadataDependencyQuery() {
        assertDoesNotThrow(() -> validator.validate("""
                SELECT MetadataComponentId, RefMetadataComponentId
                FROM MetadataComponentDependency
                WHERE MetadataComponentId = '01p1' OR RefMetadataComponentId = '01p1'
                """));
    }

    @Test
    void shouldAllowNonDependencyInClause() {
        assertDoesNotThrow(() -> validator.validate("""
                SELECT Id, Name
                FROM PermissionSet
                WHERE Id IN ('0PS1', '0PS2')
                """));
    }
}
