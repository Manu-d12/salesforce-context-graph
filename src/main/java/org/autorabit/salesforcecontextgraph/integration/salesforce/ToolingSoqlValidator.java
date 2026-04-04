package org.autorabit.salesforcecontextgraph.integration.salesforce;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ToolingSoqlValidator {

    private static final Pattern COUNT_PATTERN = Pattern.compile("\\bCOUNT\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern ORDER_BY_PATTERN = Pattern.compile("\\bORDER\\s+BY\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern METADATA_COMPONENT_DEPENDENCY_PATTERN =
            Pattern.compile("\\bFROM\\s+MetadataComponentDependency\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern WHERE_CLAUSE_PATTERN =
            Pattern.compile("\\bWHERE\\b(.+?)(\\bLIMIT\\b|\\bOFFSET\\b|$)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public void validate(String soql) {
        if (soql == null || soql.isBlank()) {
            throw new IllegalArgumentException("soql is required");
        }

        if (ORDER_BY_PATTERN.matcher(soql).find()) {
            throw new IllegalArgumentException("Tooling API queries do not support ORDER BY");
        }
        if (COUNT_PATTERN.matcher(soql).find()) {
            throw new IllegalArgumentException("Tooling API queries do not support COUNT()");
        }
        if (METADATA_COMPONENT_DEPENDENCY_PATTERN.matcher(soql).find()) {
            validateMetadataComponentDependencyQuery(soql);
        }
    }

    private void validateMetadataComponentDependencyQuery(String soql) {
        Matcher whereMatcher = WHERE_CLAUSE_PATTERN.matcher(soql);
        if (!whereMatcher.find()) {
            return;
        }

        String whereClause = whereMatcher.group(1);
        String normalizedWhere = normalize(whereClause);

        if (normalizedWhere.contains("METADATACOMPONENTNAME")) {
            throw new IllegalArgumentException(
                    "MetadataComponentDependency queries cannot filter on MetadataComponentName");
        }
        if (normalizedWhere.contains("REFMETADATACOMPONENTNAME")) {
            throw new IllegalArgumentException(
                    "MetadataComponentDependency queries cannot filter on RefMetadataComponentName");
        }
        if (normalizedWhere.contains("REFMETADATACOMPONENTTYPE = 'STANDARDENTITY'")) {
            throw new IllegalArgumentException(
                    "MetadataComponentDependency queries cannot filter on RefMetadataComponentType = 'StandardEntity'");
        }
        if (normalizedWhere.contains("METADATACOMPONENTTYPE LIKE")
                || normalizedWhere.contains("REFMETADATACOMPONENTTYPE LIKE")) {
            throw new IllegalArgumentException(
                    "MetadataComponentDependency queries cannot use LIKE with MetadataComponentType or RefMetadataComponentType");
        }
        if (containsUnsupportedWhereOperator(normalizedWhere)) {
            throw new IllegalArgumentException(
                    "MetadataComponentDependency queries only support =, !=, AND, and OR in WHERE clauses");
        }
    }

    private boolean containsUnsupportedWhereOperator(String normalizedWhere) {
        return normalizedWhere.contains(" LIKE ")
                || normalizedWhere.contains(" IN ")
                || normalizedWhere.contains(" NOT IN ")
                || normalizedWhere.contains(" > ")
                || normalizedWhere.contains(" < ")
                || normalizedWhere.contains(" >= ")
                || normalizedWhere.contains(" <= ");
    }

    private String normalize(String value) {
        return value.toUpperCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
