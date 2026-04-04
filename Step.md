# Salesforce Context Graph: Architecture and Postman Test Guide

## 1. What the application does

This Spring Boot service builds a runtime graph from Salesforce metadata and exposes APIs to:

- authenticate to Salesforce without using the CLI
- run Tooling API queries
- analyze metadata dependencies
- analyze access/exploit-style paths
- persist jobs and results in MySQL

The graph is built in memory for each request. The database stores only analysis jobs and analysis results.

## 2. High-level architecture

### Request flow

1. A client sends an analysis request to the backend.
2. The request is validated.
3. The orchestrator creates a job row in MySQL.
4. The Salesforce integration layer authenticates with OAuth.
5. The backend fetches metadata from Salesforce Tooling API.
6. Metadata is normalized into graph nodes and edges.
7. The graph is built in memory.
8. The selected analysis runs:
   - `DEPENDENCY`
   - `EXPLOIT`
9. The result is serialized and stored in MySQL.
10. The API returns the job and result payload.

### Main components

- `AnalysisController`
  - Main API for analysis jobs.
- `SalesforceController`
  - Utility API for validating Salesforce auth and running Tooling SOQL from Postman.
- `AnalysisOrchestratorAgent`
  - Coordinates validation, fetch, graph build, analysis, persistence.
- `SalesforceOAuthService`
  - Authenticates against Salesforce OAuth token endpoint.
- `ToolingApiClient`
  - Calls Salesforce Tooling API over HTTP.
- `SalesforceFetchAgent`
  - Fetches live metadata.
  - For dependency analysis it uses `MetadataComponentDependency`.
  - For exploit/access analysis it uses `EntityDefinition`, `FieldDefinition`, `ObjectPermissions`, and `FieldPermissions`.
- `NormalizationAgent`
  - Converts fetched metadata into `GraphNode` and `GraphEdge`.
- `GraphBuilderAgent`
  - Builds the in-memory adjacency graph.
- `DependencyAgent`
  - Traverses `DEPENDS_ON` style edges.
- `ExploitAgent`
  - Traverses permission/access paths.
- `RiskAgent`
  - Assigns severity.
- `ExportAgent`
  - Serializes result JSON.
- `AuditAgent`
  - Persists job and result rows.

## 3. Current dependency architecture

### Dependency analysis source

Dependency analysis is primarily driven by Salesforce Tooling API object:

- `MetadataComponentDependency`

### How it works

1. The backend resolves the requested target into a Salesforce metadata component ID.
2. It queries `MetadataComponentDependency` using that component ID.
3. It reads both:
   - rows where the target is `MetadataComponentId`
   - rows where the target is `RefMetadataComponentId`
4. It turns related metadata into graph nodes.
5. It creates `DEPENDS_ON` edges from the target to related metadata.
6. The dependency traversal returns those linked metadata items as dependencies.

### Supported dependency target types today

- `FIELD`
- `CUSTOM_OBJECT`
- `STANDARD_OBJECT`
- `LAYOUT`
- `FLEXIPAGE`
- `FLOW`
- `APEX_CLASS`
- `CUSTOM_TAB`
- `RECORD_TYPE`
- `VALIDATION_RULE`

If you send a dependency request for another target type, the API currently returns an error until that resolver is added.

## 4. Current exploit/access architecture

Exploit-style analysis does not use `MetadataComponentDependency`.

It currently builds an access graph using:

- `EntityDefinition`
- `FieldDefinition`
- `ObjectPermissions`
- `FieldPermissions`
- `PermissionSet`

This is a first usable slice for access-path analysis. It is not yet a full role/profile/BloodHound model.

## 5. Database model

### `analysis_job`

Stores:

- org id
- analysis type
- target type
- target name
- status
- error message
- created timestamp

### `analysis_result`

Stores:

- job reference
- serialized JSON result
- created timestamp

## 6. Environment configuration

Set these before starting the app.

### Database

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`

Current defaults are in `src/main/resources/application.properties`.

### Salesforce

- `SALESFORCE_LOGIN_URL`
  - Example: `https://login.salesforce.com`
  - Sandbox example: `https://test.salesforce.com`
- `SALESFORCE_CLIENT_ID`
- `SALESFORCE_CLIENT_SECRET`
- `SALESFORCE_API_VERSION`
  - Default: `v65.0`

Use either:

- `SALESFORCE_REFRESH_TOKEN`

or:

- `SALESFORCE_USERNAME`
- `SALESFORCE_PASSWORD`
- `SALESFORCE_SECURITY_TOKEN`

### Fetch mode

- `salesforce.fetch-mode=live`
  - uses real Salesforce
- `salesforce.fetch-mode=stub`
  - test mode only

## 7. Running the application

Example:

```bash
export SALESFORCE_CLIENT_ID="your_connected_app_client_id"
export SALESFORCE_CLIENT_SECRET="your_connected_app_client_secret"
export SALESFORCE_USERNAME="your_user@example.com"
export SALESFORCE_PASSWORD="your_password"
export SALESFORCE_SECURITY_TOKEN="your_security_token"
./mvnw spring-boot:run
```

If you prefer refresh token auth:

```bash
export SALESFORCE_CLIENT_ID="your_connected_app_client_id"
export SALESFORCE_CLIENT_SECRET="your_connected_app_client_secret"
export SALESFORCE_REFRESH_TOKEN="your_refresh_token"
./mvnw spring-boot:run
```

Default local base URL:

```text
http://localhost:8080
```

## 8. Postman setup

Create a Postman environment with:

- `baseUrl` = `http://localhost:8080`

Optional local variables for convenience:

- `analysisJobId`

## 9. Endpoints

### 9.1 Validate Salesforce auth

`GET /api/salesforce/session`

Purpose:

- checks whether the backend can authenticate to Salesforce
- returns instance URL and API version

Postman:

- Method: `GET`
- URL: `{{baseUrl}}/api/salesforce/session`
- Body: none

Success response example:

```json
{
  "instanceUrl": "https://your-org.my.salesforce.com",
  "idUrl": "https://login.salesforce.com/id/00D.../005...",
  "apiVersion": "v65.0"
}
```

Error example:

```json
{
  "error": "Missing Salesforce configuration: salesforce.client-id"
}
```

### 9.2 Run any Tooling API query

`POST /api/salesforce/tooling/query`

Purpose:

- lets you test direct Tooling API access from Postman
- useful for validating Salesforce metadata visibility before running analysis

Postman:

- Method: `POST`
- URL: `{{baseUrl}}/api/salesforce/tooling/query`
- Header: `Content-Type: application/json`

Body:

```json
{
  "soql": "SELECT MetadataComponentId, MetadataComponentName, MetadataComponentType, RefMetadataComponentId, RefMetadataComponentName, RefMetadataComponentType FROM MetadataComponentDependency LIMIT 10"
}
```

Success response example:

```json
[
  {
    "attributes": {
      "type": "MetadataComponentDependency",
      "url": "/services/data/v65.0/tooling/sobjects/MetadataComponentDependency/000000000000000AAA"
    },
    "MetadataComponentId": "01rg500000AjxKjAAJ",
    "MetadataComponentName": "Archive_Data_View",
    "MetadataComponentType": "CustomTab",
    "RefMetadataComponentId": "0M0g5000002IFttCAG",
    "RefMetadataComponentName": "Archive_Data_View",
    "RefMetadataComponentType": "FlexiPage"
  }
]
```

### 9.3 Create an analysis job

`POST /api/analysis`

Purpose:

- creates and runs an analysis immediately
- persists job and result in MySQL

Postman:

- Method: `POST`
- URL: `{{baseUrl}}/api/analysis`
- Header: `Content-Type: application/json`

#### Dependency request example for a field

```json
{
  "orgId": "dev-org",
  "analysisType": "DEPENDENCY",
  "targetType": "FIELD",
  "targetName": "Case.Product"
}
```

#### Dependency request example for an object

```json
{
  "orgId": "dev-org",
  "analysisType": "DEPENDENCY",
  "targetType": "STANDARD_OBJECT",
  "targetName": "Account"
}
```

#### Dependency request example for a flexipage

```json
{
  "orgId": "dev-org",
  "analysisType": "DEPENDENCY",
  "targetType": "FLEXIPAGE",
  "targetName": "Account_Record_Page"
}
```

#### Exploit request example

```json
{
  "orgId": "dev-org",
  "analysisType": "EXPLOIT",
  "targetType": "FIELD",
  "targetName": "Case.Product"
}
```

Success response example:

```json
{
  "jobId": 1,
  "orgId": "dev-org",
  "analysisType": "DEPENDENCY",
  "targetType": "FIELD",
  "targetName": "Case.Product",
  "status": "COMPLETED",
  "errorMessage": null,
  "createdAt": "2026-04-04T10:20:00",
  "result": {
    "dependencies": [
      "Case (Sales) Layout",
      "Case (Marketing) Layout"
    ],
    "injectionOrder": [
      "Case (Sales) Layout",
      "Case (Marketing) Layout"
    ],
    "path": [],
    "severity": "LOW",
    "metadata": {
      "target": "Case.Product",
      "nodeCount": 3,
      "edgeCount": 2,
      "graphNodes": [
        "00Ng5000006mibKEAQ",
        "00hg5000002DakWAAS",
        "00hg5000002DakVAAS"
      ],
      "graphEdgeCount": 2
    }
  }
}
```

### 9.4 Fetch a previously created analysis job

`GET /api/analysis/{jobId}`

Purpose:

- retrieve a stored job and its result

Postman:

- Method: `GET`
- URL: `{{baseUrl}}/api/analysis/{{analysisJobId}}`

Example:

```text
GET http://localhost:8080/api/analysis/1
```

## 10. Exact request field meanings

### `analysisType`

Allowed values:

- `DEPENDENCY`
- `EXPLOIT`

### `targetType`

Known enum values:

- `CUSTOM_OBJECT`
- `STANDARD_OBJECT`
- `FIELD`
- `PERMISSION_SET`
- `PROFILE`
- `ROLE`
- `APEX_CLASS`
- `APEX_TRIGGER`
- `LWC`
- `AURA_COMPONENT`
- `FLOW`
- `VALIDATION_RULE`
- `LAYOUT`
- `RECORD_TYPE`
- `CUSTOM_METADATA_TYPE`
- `CUSTOM_METADATA_RECORD`
- `CUSTOM_SETTINGS`
- `NAMED_CREDENTIAL`
- `REMOTE_SITE_SETTING`
- `PERMISSION_SET_GROUP`
- `SHARING_RULE`
- `QUEUE`
- `STATIC_RESOURCE`
- `EMAIL_TEMPLATE`
- `CUSTOM_TAB`
- `FLEXIPAGE`
- `CUSTOM_APPLICATION`

Important:

- not every enum is implemented for live dependency resolution yet
- currently the supported dependency target types are listed in section 3

### `targetName`

Examples:

- Object: `Account`
- Custom object: `Payment__c`
- Field: `Case.Product`
- Custom field: `Payment__c.CardNumber__c`
- FlexiPage: `Account_Record_Page`
- Layout: `Case (Sales) Layout`

## 11. Recommended Postman test order

### Step 1

Call:

```text
GET {{baseUrl}}/api/salesforce/session
```

If this fails, fix Salesforce auth env vars first.

### Step 2

Call:

```text
POST {{baseUrl}}/api/salesforce/tooling/query
```

Body:

```json
{
  "soql": "SELECT MetadataComponentId, MetadataComponentName, MetadataComponentType, RefMetadataComponentId, RefMetadataComponentName, RefMetadataComponentType FROM MetadataComponentDependency LIMIT 10"
}
```

If this fails, the app is authenticated but your connected app or user may not have the needed metadata access.

### Step 3

Create a dependency analysis:

```json
{
  "orgId": "dev-org",
  "analysisType": "DEPENDENCY",
  "targetType": "FIELD",
  "targetName": "Case.Product"
}
```

### Step 4

Copy the returned `jobId` and call:

```text
GET {{baseUrl}}/api/analysis/{jobId}
```

## 12. Postman collection ideas

You can create these requests in a collection:

1. `Salesforce Session Check`
2. `Tooling Query - MetadataComponentDependency`
3. `Create Dependency Analysis`
4. `Get Analysis Result`
5. `Create Exploit Analysis`

## 13. Common failure cases

### Missing OAuth config

Example:

```json
{
  "error": "Missing Salesforce configuration: salesforce.client-id"
}
```

Fix:

- set missing env vars

### Invalid Salesforce login

Example:

```json
{
  "error": "Salesforce authentication failed: { ... }"
}
```

Fix:

- check login URL
- check client id and secret
- check username/password/security token
- or use a valid refresh token

### Unsupported dependency target type

Example:

```json
{
  "error": "MetadataComponentDependency resolution is not implemented for target type: ..."
}
```

Fix:

- test with a supported type first
- extend the resolver in code for the missing type

### Metadata component not found

Example:

```json
{
  "error": "No Salesforce metadata component found for: Account_Record_Page"
}
```

Fix:

- verify the exact metadata API name or label in Salesforce
- use `/api/salesforce/tooling/query` to discover the correct name

## 14. Important implementation notes

- Dependency analysis currently returns first-hop relationships from `MetadataComponentDependency`.
- It does not yet recursively expand all transitive dependencies across multiple hops.
- Exploit analysis is still a basic permission-set access model.
- No Salesforce CLI is used by the application runtime.
- OAuth is done directly via Salesforce REST token endpoint.

## 15. Useful Tooling SOQL examples for Postman

### Metadata dependencies

```json
{
  "soql": "SELECT MetadataComponentId, MetadataComponentName, MetadataComponentType, RefMetadataComponentId, RefMetadataComponentName, RefMetadataComponentType FROM MetadataComponentDependency LIMIT 20"
}
```

### Find a field definition

```json
{
  "soql": "SELECT DurableId, QualifiedApiName, DataType FROM FieldDefinition WHERE EntityDefinition.QualifiedApiName = 'Case' AND QualifiedApiName = 'Product' LIMIT 5"
}
```

### Find an object definition

```json
{
  "soql": "SELECT DurableId, QualifiedApiName, Label FROM EntityDefinition WHERE QualifiedApiName = 'Account' LIMIT 5"
}
```

### Find object permissions

```json
{
  "soql": "SELECT ParentId, SObjectType, PermissionsRead, PermissionsCreate, PermissionsEdit, PermissionsDelete FROM ObjectPermissions WHERE SObjectType = 'Case' LIMIT 20"
}
```

### Find field permissions

```json
{
  "soql": "SELECT ParentId, SObjectType, Field, PermissionsRead, PermissionsEdit FROM FieldPermissions WHERE SObjectType = 'Case' AND Field = 'Case.Product' LIMIT 20"
}
```

## 16. Files to inspect in the codebase

- `src/main/java/org/autorabit/salesforcecontextgraph/api/controller/AnalysisController.java`
- `src/main/java/org/autorabit/salesforcecontextgraph/api/controller/SalesforceController.java`
- `src/main/java/org/autorabit/salesforcecontextgraph/service/AnalysisOrchestratorAgent.java`
- `src/main/java/org/autorabit/salesforcecontextgraph/integration/salesforce/SalesforceOAuthService.java`
- `src/main/java/org/autorabit/salesforcecontextgraph/integration/salesforce/ToolingApiClient.java`
- `src/main/java/org/autorabit/salesforcecontextgraph/integration/salesforce/SalesforceFetchAgent.java`
- `src/main/resources/application.properties`

## 17. Next recommended improvements

If you want this backend to get closer to a production-grade Salesforce dependency engine, the next steps should be:

1. Expand `MetadataComponentDependency` traversal recursively.
2. Add target resolution for more metadata types.
3. Add caching of OAuth access tokens.
4. Add rate-limit handling and retries for Salesforce API calls.
5. Add better distinction between:
   - dependency direction
   - consumer direction
6. Add async job execution instead of synchronous request execution.
