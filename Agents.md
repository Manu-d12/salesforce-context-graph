# AGENTS.md

## Salesforce Context Graph - Backend Agent Guide

### Spring Boot + MySQL

### Runtime Metadata Fetch, In-Memory Graph

---

## 1. Purpose

This backend enables:

### Dependency Prediction

* Identify required metadata dependencies
* Detect missing components
* Generate deployment injection order

### Exploit Analysis

* Detect privilege escalation paths
* Identify sensitive data exposure
* Integrate with BloodHound (optional)

---

## 2. Architecture

* Salesforce = **Source of Truth**
* Metadata fetched **at runtime**
* Graph built **in memory**
* MySQL stores only:

    * jobs
    * results
    * audit logs
    * exports

❌ Do NOT store full graph in DB

---

## 3. Supported Node Types

```
CUSTOM_OBJECT
STANDARD_OBJECT
FIELD
PERMISSION_SET
PROFILE
ROLE
APEX_CLASS
APEX_TRIGGER
LWC
AURA_COMPONENT
FLOW
VALIDATION_RULE
LAYOUT
RECORD_TYPE
CUSTOM_METADATA_TYPE
CUSTOM_METADATA_RECORD
CUSTOM_SETTINGS
NAMED_CREDENTIAL
REMOTE_SITE_SETTING
PERMISSION_SET_GROUP
SHARING_RULE
QUEUE
STATIC_RESOURCE
EMAIL_TEMPLATE
CUSTOM_TAB
FLEXIPAGE
CUSTOM_APPLICATION
```

---

## 4. Request Model

```json
{
  "orgId": "dev-org",
  "analysisType": "DEPENDENCY",
  "targetType": "FIELD",
  "targetName": "Payment__c.CardNumber__c"
}
```

---

## 5. Agents

### 5.1 Request Validation Agent

* Validate input
* Validate target type
* Normalize request

---

### 5.2 Orchestrator Agent

* Create job
* Trigger metadata fetch
* Build graph
* Run analysis
* Save result

---

### 5.3 Salesforce Fetch Agent

* Fetch metadata dynamically
* Use REST / Tooling / Metadata API
* Fetch minimal required scope

---

### 5.4 Normalization Agent

Convert raw metadata into:

```java
class GraphNode {
    String id;
    String type;
    String name;
}
```

```java
class GraphEdge {
    String from;
    String to;
    String type;
}
```

---

### 5.5 Graph Builder Agent

In-memory structures:

```java
Map<String, GraphNode> nodes;
Map<String, List<GraphEdge>> edges;
```

* Build directed graph
* Deduplicate nodes

---

### 5.6 Dependency Agent

* Traverse graph
* Find dependencies
* Generate order

Output:

```json
{
  "dependencies": [],
  "injectionOrder": []
}
```

---

### 5.7 Exploit Agent

* Traverse permission graph
* Find access paths

Example:

```
ROLE → PERMISSION_SET → OBJECT → FIELD
```

Output:

```json
{
  "path": [],
  "severity": "HIGH"
}
```

---

### 5.8 BloodHound Agent (Optional)

* Export graph
* Query attack paths

---

### 5.9 Risk Agent

Severity:

```
CRITICAL
HIGH
MEDIUM
LOW
```

---

### 5.10 Export Agent

Outputs:

* JSON
* dependency plan
* exploit paths

---

### 5.11 Audit Agent

* Log jobs
* Log failures
* Store metadata

---

## 6. MySQL Tables

### analysis_job

```sql
CREATE TABLE analysis_job (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    org_id VARCHAR(100),
    analysis_type VARCHAR(50),
    target_type VARCHAR(100),
    target_name VARCHAR(255),
    status VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

### analysis_result

```sql
CREATE TABLE analysis_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_id BIGINT,
    result_json JSON,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

### audit_log

```sql
CREATE TABLE audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    action_type VARCHAR(100),
    details_json JSON,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

## 7. Package Structure

```
com.company.sfgraph
├── api
├── orchestration
├── salesforce
├── graph
├── dependency
├── exploit
├── persistence
├── audit
└── config
```

---

## 8. Endpoints

```
POST /api/dependency
POST /api/exploit
GET  /api/job/{id}
```

---

## 9. Key Rules

* No full graph persistence
* Always fetch from Salesforce
* Graph is request-scoped
* Results must be explainable
* Keep logic modular per node type

---

## 10. Done Criteria

* Metadata fetched from Salesforce
* Graph built in memory
* Dependency works for any node type
* Exploit path works
* Results stored in MySQL
* API returns valid JSON

---

## 11. Summary

This backend:

* builds graph at runtime
* supports all metadata types
* predicts dependencies
* detects exploit paths
* integrates with security tools
* uses MySQL only for operational data

---
