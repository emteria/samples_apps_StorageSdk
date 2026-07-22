# Emteria Storage SDK Sample

An Android app showing how to use the **emteria Storage SDK** to manage
application packages, transfer files, and register a device from a third-party
app. It talks to the storage services that ship with emteria OS and demonstrates
every capability the SDK exposes.

## What this sample demonstrates

- **App packages** — list available/installed packages, download one in the
  background, and silently install it.
- **File storage** — list remote files, download a file, and upload a file.
- **Device registration** — register the device with a universal license, query
  registration status, and read registration details.

The logic lives in
[`MainActivity.java`](app/src/main/java/com/emteria/sample/sdk/storage/MainActivity.java)
and the small `tasks/` helpers that drive each SDK manager off the UI thread.

## Prerequisites & compatibility

- **Runs only on emteria OS.** The sample binds to the storage services
  (`com.emteria.storage`), which are part of emteria OS. On stock Android / AOSP
  the binds fail and the operations do nothing.
- **Requires two emteria permissions** (both enforced by the storage services —
  see [Setup](#integrating-the-sdk-in-your-own-app)).
- **S3 / workspace operations require an activated device.** Listing workspace
  packages, files, and uploads authenticate via a device JWT, so the device must
  be registered/activated first.
- Build targets: `compileSdk 34`, `targetSdk 34`, `minSdk 26`.
- Bundled SDK: `app/src/main/libs/emteria-storage-sdk-v3.jar`.

## Integrating the SDK in your own app

1. Copy the SDK `.jar` from `app/src/main/libs` into your project.
2. Add the JAR to your module's `build.gradle.kts`:

   ```kotlin
   implementation(fileTree("src/main/libs") { include("*.jar") })
   ```

3. Declare service visibility and the required permissions in
   `AndroidManifest.xml`:

   ```xml
   <queries>
       <package android:name="com.emteria.storage" />
   </queries>

   <uses-permission android:name="emteria.permission.MANAGE_APP_UPDATES" />
   <uses-permission android:name="emteria.permission.MANAGE_DEVICE_REGISTRATION" />
   ```

   `MANAGE_APP_UPDATES` guards the app-package and file services;
   `MANAGE_DEVICE_REGISTRATION` guards the device-registration service. Both are
   enforced, so a caller without them cannot bind.

## Two API styles

The SDK is built on Android's bound-service **Messenger** IPC and exposes two
services, each with its own bind intent:

- `MessengerConfig.getAppManagementServiceBindIntent()` — app packages and files.
- `MessengerConfig.getDeviceManagementServiceBindIntent()` — device registration.

On top of that IPC there are two ways to call it:

- **Manager classes** (high level) — for app packages and device registration.
  You subclass an abstract `*Manager`, implement its callbacks, call
  `bindToAppManagement(context)` or `bindToDeviceManagement(context)`, invoke a
  request method, and receive results in your callbacks. Call `unbind(context)`
  when done. Every operation is asynchronous.
- **Contracts + Messenger** (low level) — for file operations. You bind the
  app-management service yourself, build a request with a `*Contract` class
  (attaching your reply `Messenger`), `send()` it, and dispatch the reply in a
  `Handler` by switching on `MessengerConfig.ResponseReason`.

The sample uses managers for packages/registration and the contract style for
files.

## App package management (managers)

### List packages — `PackageMetadataManager`

```java
class Handler extends PackageMetadataManager {
    @Override public void onReceive(HashMap<String, List<AppPackage>> packages) { /* ... */ }
    @Override public void onFailure(String error) { /* ... */ }
}

Handler h = new Handler();
h.bindToAppManagement(context);
h.getPackagesFromFDroid(repoName);   // packages from a hosted F-Droid repo
// or
h.getPackagesFromWorkspace();        // packages from the emteria workspace (S3, needs activation)
```

`onReceive` delivers a map of Android package name → list of `AppPackage`
(available and installed).

### Download a package — `PackageDownloadManager`

```java
class Handler extends PackageDownloadManager {
    @Override public void onDownloadFinished(AppPackage appPackage) { /* ... */ }
    @Override public void onDownloadFailed(String appPackageId, String error) { /* ... */ }
    @Override public void onProgressChanged(String appPackageId, int progress) { /* ... */ }
}

h.bindToAppManagement(context);
h.downloadPackage(appPackage.getAppId());   // download by app id
```

### Install a package — `PackageInstallationManager`

```java
class Handler extends PackageInstallationManager {
    @Override public void onInstallSuccessful(AppPackage appPackage) { /* ... */ }
    @Override public void onInstallFailed(String appPackageId, String error) { /* ... */ }
}

h.bindToAppManagement(context);
h.installPackage(appPackage.getAppId());    // install a previously downloaded package
```

## Device registration (manager)

### `DeviceRegistrationManager`

```java
class Handler extends DeviceRegistrationManager {
    @Override public void onRegistrationSuccess() { /* ... */ }
    @Override public void onRegistrationFailure(String error) { /* ... */ }
    @Override public void onRegistrationStatus(boolean registered) { /* ... */ }
    @Override public void onRegistrationDetailsSuccess(RegistrationDetails details) { /* ... */ }
    @Override public void onRegistrationDetailsFailure(String error) { /* ... */ }
}

Handler h = new Handler();
h.bindToDeviceManagement(context);

h.registerDevice(universalLicense);   // -> onRegistrationSuccess / onRegistrationFailure
h.isDeviceRegistered();               // -> onRegistrationStatus(boolean)
h.getRegistrationDetails(true);       // allowCache -> onRegistrationDetailsSuccess(RegistrationDetails)
```

## File storage (contracts + Messenger)

Bind the app-management service, then send requests built with the file
contracts and handle replies in your `Handler`.

```java
Intent bind = MessengerConfig.getAppManagementServiceBindIntent();
bindService(bind, connection, Context.BIND_AUTO_CREATE);
// on connect: requestMessenger = new Messenger(binder);
// responseMessenger = new Messenger(new CallbackHandler());
```

### List remote files

- **Request:** `FileListContract.ListRequest.buildMessage(responseMessenger)`
- **Result:** `LIST_FILES_SUCCESS` →
  `FileListContract.ListSuccessResponse.extractFiles(payload)` returns
  `ArrayList<RemoteFile>`.
- **Error:** `LIST_FILES_ERROR` → `FileListContract.ListErrorResponse.extractErrorMessage(payload)`.

### Download a file

- **Request:** `FileDownloadContract.DownloadRequest.buildMessage(responseMessenger, storageFileId)`
- **Result:** `DOWNLOAD_FILE_SUCCESS` →
  `FileDownloadContract.DownloadSuccessResponse.extractStorageFileId(payload)` and
  `extractFileDescriptor(payload)` (a `ParcelFileDescriptor`). Consume it off the
  main thread, e.g. with `FileDescriptorWrapper.copyToFile(pfd, destination)`.
- **Error:** `DOWNLOAD_FILE_ERROR` → `extractStorageFileId` + error message.

### Upload a file

- **Request:** `FileUploadContract.UploadRequest.buildMessage(responseMessenger, name, parcelFileDescriptor)`
  where the descriptor is opened `MODE_READ_ONLY` on the local file.
- **Result:** `UPLOAD_FILE_SUCCESS`.
- **Error:** `UPLOAD_FILE_ERROR` → `FileUploadContract.UploadErrorResponse.extractErrorMessage(payload)`.

## Data models

- **`AppPackage`** — `getAppId()`, `getStorageId()`, `getApkName()`,
  `getPackageName()`, `getVersionName()`, `getVersionCode()`, `getSize()`,
  `getHash()`, `getHashType()`, `getSigner()`, `getMinSdkVersion()`,
  `getTargetSdkVersion()`, `getNativeCode()`, `getUsedPermissions()`,
  `getTags()` (`List<AppTag>`), `isInstalled()`, `getLocalDownloadPath()`,
  `getRepoName()`, `getAdded()`.
- **`RemoteFile`** — `getStorageFileId()`, `getFilename()`, `getContentType()`,
  `getSize()`, `getCreatedDate()`, `getModifiedDate()`.
- **`RegistrationDetails`** — `getDeviceId()`, `getDeviceName()`,
  `getDeviceDescription()`, `getGroupId()`, `getGroupName()`,
  `getGroupDescription()`.
- **`FileDescriptorWrapper`** — helpers for the download descriptor:
  `copyToFile(pfd, file)`, `openInputStream(pfd)`, `getPFD()`.

## Response reasons (contract style)

File and other low-level replies arrive as `MessengerConfig.ResponseReason`
ordinals in `Message.what`:

| Reason | Meaning |
|---|---|
| `LIST_FILES_SUCCESS` / `LIST_FILES_ERROR` | Remote file listing |
| `DOWNLOAD_FILE_SUCCESS` / `DOWNLOAD_FILE_ERROR` | File download |
| `UPLOAD_FILE_SUCCESS` / `UPLOAD_FILE_ERROR` | File upload |
| `METADATA_FOUND` / `METADATA_ERROR` | Package listing |
| `DOWNLOAD_PACKAGE_PROGRESS_CHANGED` / `DOWNLOAD_PACKAGE_SUCCESS` / `DOWNLOAD_PACKAGE_ERROR` | Package download |
| `INSTALL_PACKAGE_SUCCESS` / `INSTALL_PACKAGE_ERROR` | Package install |
| `DEVICE_REGISTRATION_SUCCESS` / `DEVICE_REGISTRATION_ERROR` / `DEVICE_REGISTRATION_STATUS` | Registration |
| `DEVICE_REGISTRATION_DETAILS_SUCCESS` / `DEVICE_REGISTRATION_DETAILS_ERROR` | Registration details |

When using the manager classes you don't handle these directly — the manager
maps them to its callbacks for you.

## Build & run

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Launch the app on an emteria OS device. Note that workspace package listing,
file operations, and uploads require the device to be registered/activated
first — use the registration controls before those.

## Troubleshooting

- **"Service is not bound"** — the storage service could not be bound. Confirm
  you are on an emteria OS device, the `<queries>` entry is present, and the app
  holds the two permissions above.
- **Workspace listing / file operations fail with an auth error** — the device
  is not registered/activated; register it first.
- **Permission denied on bind** — the app is missing `MANAGE_APP_UPDATES` or
  `MANAGE_DEVICE_REGISTRATION`.

## License

See the repository for licensing information. For SDK questions, contact
[emteria support](https://emteria.com).
