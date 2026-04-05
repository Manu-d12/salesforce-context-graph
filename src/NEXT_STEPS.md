# NEXT_STEPS.md

## Salesforce Context Graph – Backend Evolution Roadmap

### From Graph Generation → Graph Intelligence Platform

---

## 1. Current State

You have already built:

* API 1: Full directed graph of Salesforce org metadata
* API 2: Selected metadata subgraph
* Using `MetadataComponentDependency`

This is a strong foundation.

---

## 2. Problem with Current Approach

`MetadataComponentDependency` is **not sufficient alone**.

It misses:

* Field-level permissions
* Profile & Permission Set effective access
* Role hierarchy
* Permission Set Groups & Muting
* Apex & LWC runtime dependencies
* Flow-level access
* Security exposure paths

---

## 3. Immediate Goal

Move from:

> "Graph generation"

To:

> "Graph intelligence & reasoning engine"

---

## 4. Phase 1 – Graph Foundation (Critical)

### 4.1 Multi-Source Graph Builder

Augment your graph with these sources:

* MetadataComponentDependency
* FieldPermissions
* ObjectPermissions
* SetupEntityAccess
* PermissionSetAssignment
* PermissionSetGroup
* MutingPermissionSet
* UserRole
* Group / Queue
* ApexClass
* ApexTrigger
* Flow
* Layout
* RecordType
* Profile
* CustomMetadata
* NamedCredential
* RemoteSiteSetting

---

### 4.2 Edge Taxonomy (Very Important)

Define strong edge types:

```
DEPENDS_ON
USES
REFERENCES
DECLARES
BELONGS_TO
CONTAINS
EXPOSES
GRANTS_READ
GRANTS_EDIT
GRANTS_DELETE
GRANTS_EXECUTE
ASSIGNED_TO
MEMBER_OF
INHERITS
MUTES
CALLS
QUERIES
UPDATES
VISIBLE_IN
CONFIGURES
```

---

### 4.3 Canonical Node Model

```java
class GraphNode {
    String id;
    String type;
    String apiName;
    String label;
    Map<String, Object> attributes;
    Set<String> tags;
}
```

### Tags (important for future)

* PII
* PCI
* SENSITIVE
* AUTH
* DATA_ACCESS
* UI
* SECURITY

---

## 5. Phase 2 – Dependency Intelligence

### 5.1 Missing Dependency API

Input:

```json
{
  "targetType": "LWC",
  "targetName": "paymentSummaryPanel"
}
```

Output:

```json
{
  "missing": [],
  "injectionOrder": []
}
```

---

### 5.2 Deployment Order (Topological Sort)

* Build ordered deployment plan
* Handle cycles safely
* Ensure deterministic output

---

### 5.3 Impact Analysis API

"What breaks if I change this?"

* Downstream traversal
* Blast radius detection

---

### 5.4 Unresolved Dependency Detection

* Identify missing references
* Detect broken metadata chains

---

## 6. Phase 3 – Security Graph & Exploit Analysis

### 6.1 Build Security Graph

#### Principal Nodes

* ROLE
* PROFILE
* PERMISSION_SET
* PERMISSION_SET_GROUP

#### Resource Nodes

* OBJECT
* FIELD
* APEX_CLASS

---

### 6.2 Security Edges

* ROLE → INHERITS → ROLE
* PERMISSION_SET → GRANTS → OBJECT
* PERMISSION_SET → GRANTS → FIELD
* PROFILE → GRANTS → OBJECT
* GROUP → ASSIGNED_TO → USER

---

### 6.3 APIs to Build

#### Effective Access

"Can X access Y?"

#### Attack Paths

"Show paths from support role → PCI field"

#### Privilege Escalation

"What new access appears?"

---

### 6.4 Risk Scoring

Severity:

```
CRITICAL
HIGH
MEDIUM
LOW
```

Example:

* Edit access to PCI → CRITICAL
* Read access to PII → HIGH

---

## 7. Phase 4 – Graph Diff (Very Powerful)

Compare:

* Current org graph
* Proposed deployment graph

### APIs

* `/graph/diff`
* `/analysis/risk-diff`
* `/analysis/dependency-diff`

### Outputs

* New dependencies
* New access paths
* Removed nodes
* Risk changes

---

## 8. Phase 5 – Code Intelligence

### 8.1 Apex Parsing

Extract:

* SOQL references
* field usage
* object usage
* callouts
* class calls

---

### 8.2 LWC Parsing

Extract:

* Apex imports
* schema imports
* labels
* static resources

---

### 8.3 Add Confidence Scoring

Each edge:

```java
confidence = HIGH | MEDIUM | LOW
source = METADATA | APEX_PARSE | INFERRED
```

---

## 9. Phase 6 – Resolver Architecture

Use strategy pattern:

```java
interface Resolver {
    boolean supports(NodeType type);
    Resolution resolve(GraphNode node);
}
```

Implement:

* FieldResolver
* LwcResolver
* ApexResolver
* FlowResolver
* PermissionResolver
* ProfileResolver

---

## 10. Phase 7 – Query Engine

Build APIs like:

* upstream dependencies
* downstream dependencies
* shortest path
* all paths between nodes
* orphan nodes
* cyclic dependencies

---

## 11. Phase 8 – Caching & Scale

* cache subgraphs
* cache metadata fragments
* TTL: 5–15 minutes
* avoid full org fetch repeatedly

---

## 12. Phase 9 – Policy Engine

Generate rules:

* block risky deployments
* detect privilege escalation
* enforce dependency completeness

Output:

* JSON
* OPA (Rego)
* pipeline-friendly format

---

## 13. Best Next 5 Features (Priority)

1. Edge taxonomy (typed edges)
2. Security graph (permissions + roles)
3. Missing dependency API
4. Exploit path API
5. Graph diff engine

---

## 14. Final Vision

The goal is NOT:

"Show Salesforce graph"

The goal is:

> "Given a change, predict dependencies, impact, and security risks BEFORE deployment."

---

## 15. Definition of Powerful System

Your system is powerful when it can:

* Predict missing metadata automatically
* Generate deployment order
* Detect hidden access paths
* Compare before vs after changes
* Block risky deployments in CI/CD
* Explain every decision with graph paths

---

## 16. Summary

You already built the hardest part:
✔ Graph extraction

Now build:

* intelligence
* reasoning
* security analysis
* deployment awareness

That transforms your project into a **DevSecOps intelligence platform for Salesforce**.

---
