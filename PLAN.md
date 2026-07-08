# Architectural Update Plan: Private-First Config-Driven System

This plan transitions the application into a permissionless-by-default execution engine. The app operates completely within its private system sandbox by default and dynamically routes storage access to public external storage only when explicit storage modes are enabled via the Maintenance panel.

---

## Implementation Checklist

### Phase 1: Native Storage & Asset Router Foundations (Backend)
- [x] **Step 1.1: Refactor `AppConfig.java`**
  - Add explicit choice identifiers (`STORAGE_MODE_SANDBOX` / `STORAGE_MODE_PUBLIC`).
  - Update `saveMaintenanceSettings` to store the active strategy state to disk.
  - Append the chosen parameter output into `getMaintenanceConfigJson()` to feed the web interface.
- [x] **Step 1.2: Refactor `MyWebViewClient.java` & `ConfigWebViewClient.java`**
  - Update the core `resolveAssetStream` routing method.
  - If strategy is set to Sandbox, completely skip shared external storage evaluations. Route asset delivery exclusively through internal application files cache data trees and standard project assets fallbacks.
- [x] **Step 1.3: Expand `StorageManager.java` with Controlled Two-Way Migration Workers**
  - Scrub out historical automatic data replication routines that overwrite file trees implicitly.
  - Implement an explicit `migrateSandboxToPublic()` tool to duplicate sandbox contents into public documents directories.
  - Implement an explicit `migratePublicToSandbox()` tool to ingest custom modified public file changes securely back down to isolated internal cache sectors.
- [x] **Step 1.4: Refactor `UpdateManager.java` Pipeline Rules**
  - Ensure automated archive retrieval extractions land strictly within private device cache partitions.
  - Condition subsequently downstream public directory storage alignment updates to execute ONLY when the strategy parameter indicates public storage access is active.

### Phase 2: Administrative Control panel & Endpoint Bindings (Vue + Rest API)
- [x] **Step 2.1: Update `MaintenanceController.java` Core Mapping Hooks**
  - Adjust the `/api/maintenance/save` route to extract, sanitize, and pass the explicit storage configuration string parameters.
  - Establish two new endpoint mapping listeners:
    - `POST /api/maintenance/migrate-to-public`
    - `POST /api/maintenance/migrate-to-sandbox`
- [x] **Step 2.2: Update Vue State Drivers (`src/utils/maintenanceApi.js`)**
  - Incorporate the custom state variable into the reactive configuration model data schemas.
  - Append the attribute map within the serialisation URL template layout inside `sendNativeState()`.
  - Expose two new asynchronous dispatch functions targeting the manual backend translation operations.
- [x] **Step 2.3: Build UI Component Selectors (`src/App.vue`)**
  - Replace raw switches with clean drop-down selection cards or radio rows mapping out Private Sandbox vs Public External choices explicitly.
  - Scrub out the old "Sync SD Card" single-action buttons. Inject two standalone alternative operator interfaces: **"Export to Public"** and **"Import to Sandbox"**.

### Phase 3: Decoupling Resource Tooling Utilities (`FsController` & `ArcController`)
- [x] **Step 3.1: Refactor `FsController.java` Workspace Roots**
  - Adjust the absolute base directory translation mechanism within `getStorageRoot()`. Look up directory mapping dynamically from configurations instead of referencing static external locations.
  - Verify that standard folder inspection lookups (`/api/fs/list`), writes (`/api/fs/write`), and diagnostic charts handle absolute tree mapping seamlessly without breaking existing JavaScript path formatting rules.
- [x] **Step 3.2: Refactor `ArcController.java` Operation Boundaries**
  - Apply the identical dynamic asset tree root redirection rules cleanly across your compression, extraction, and file index listing utilities.

### Phase 4: Dynamic Just-In-Time Permissions Framework
- [x] **Step 4.1: Integrate `PermissionsController.java` Event Bus**
  - Deploy standard endpoints supporting `/api/permissions/request` and `/api/permissions/status`.
  - Wire them to receive background calls and respond seamlessly to non-blocking JavaScript loop structures. 
  - Ensure prompt windows inflate on-demand right when an operation requires validation.
- [x] **Step 4.2: finalise Manifest Capability Rules (`AndroidManifest.xml`)**
  - Define the application capabilities limits and permission pools cleanly in the manifest block.
  - Confirm that no native code routines execute runtime check evaluations on application startup.

## TODO:

Later remove the temporary public void syncSandboxToExternal() wrapper function from StoreManager.java

Later remove export const triggerSDCardSync = async () => { ... } from ahm-asset-maintenance/src/utils/maintenanceApi.js
