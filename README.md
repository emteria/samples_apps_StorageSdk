# Storage (Custom API)

The storage access can be controlled by third-party applications by using the following API methods from Emteria Storage SDK:

- Retrieve information about available application packages
- Invoke download of the chosen application in the background
- Invoke a silent installation of the chosen application in the background

## Retrieving a list of application packages

Third-party applications can retrieve a combined list of available and installed packages by inheriting from the following class and providing an implementation for its abstract methods:

```java
public abstract class PackageListManager
{
    /**
     * Callback that must be implemented to receive available packages.
     *
     * @param packages a HashMap where
     *  - the key is the android package name
     *  - the value is a list of corresponding AppPackage(s)
     */
    public abstract void onReceive(HashMap<String, List<AppPackage>> packages);

    /**
     * 
     * Callback that must be implemented to receive errors when trying to get available packages
     * @param error The error message
     */
    public abstract void onFailure(String error);
}
```

Use the following methods to connect to the external service and retrieve the list of application packages:
then the following method needs to be called:

```java
    /**
     * Establish the connection to the service.
     */
    public final boolean bind(Context context);

    /**
     * Start retrieving all available packages (installed on the device and downloadable)
     *
     * @param repoName the emteria hosted Fdroid repo name
     * @throws ServiceNotBoundException if the storage service is not bound
     */
    public void getPackages(String repoName) throws ServiceNotBoundException;

    /**
     * Start retrieving of all available packages for emteria S3 storage (installed on the device and downloadable)
     * the authentication will be done through jwt which requires an activated device
     *
     * @throws ServiceNotBoundException if the storage service is not bound
     */
    public void getPackages() throws ServiceNotBoundException

    /**
     * Terminate the connection to the service.
     */
    public final void unbind(Context context);
```

The `AppPackage` class provides information about the found application like the exact version string and used permissions:

```java
/**
 * Class that represents an Application that is or can be installed.
 */
public class AppPackage implements Serializable
{
    /**
     * If the file is stored on the Emteria S3 server it gets a unique id
     */
    @SerializedName("id")
    @Expose
    private String id = null;

    public String getId() 
    { 
        return id;
    }

    /**
     * Date when the AppPackage was added to the storage entity.
     */
    @SerializedName("added")
    @Expose
    private Long added = 0L;

    public Long getAdded()
    {
        return added;
    }

    /**
     * Name of the .apk file.
     */
    @SerializedName("apkName")
    @Expose
    private String apkName = null;

    public String getApkName()
    {
        return apkName;
    }

    /**
     * Hash value of the .apk file.
     */
    @SerializedName("hash")
    @Expose
    private String hash = null;

    public String getHash()
    {
        return hash;
    }

    /**
     * Type of the {@link #hash} value eg. sha256.
     */
    @SerializedName("hashType")
    @Expose
    private String hashType = null;

    public String getHashType()
    {
        return hashType;
    }

    /**
     * Minimal SDK Version required for this package.
     */
    @SerializedName("minSdkVersion")
    @Expose
    private int minSdkVersion  = -1;

    public int getMinSdkVersion()
    {
        return minSdkVersion;
    }

    /**
     * List of supported instruction sets eg. arm64-v8a, armeabi-v7a, x86, x86_64.
     */
    @SerializedName("nativecode")
    @Expose
    private List<String> nativeCode = null;

    public List<String> getNativeCode()
    {
        return nativeCode;
    }

    /**
     * The name of the package eg com.emteria.storage.
     */
    @SerializedName("packageName")
    @Expose
    private String packageName  = null;

    public String getPackageName()
    {
        return packageName;
    }

    /**
     * The signature of the package can be found as "Signer #1 certificate SHA-256 digest" from the output of the "apksigner verify --print-certs apk" command.
     */
    @SerializedName("signer")
    @Expose
    private String signer  = null;

    public String getSigner()
    {
        return signer;
    }

    /**
     * Size of the .apk file.
     */
    @SerializedName("size")
    @Expose
    private int size  = -1;

    public int getSize()
    {
        return size;
    }

    /**
     * The target sdk version of this package.
     */
    @SerializedName("targetSdkVersion")
    @Expose
    private int targetSdkVersion  = -1;

    public int getTargetSdkVersion()
    {
        return targetSdkVersion;
    }

    /**
     * List of required permissions first string is the permission and second string represents the sdk version.
     */
    @SerializedName("uses-permission")
    @Expose
    private List<List<String>> usedPermissions  = null;

    public List<List<String>> getUsedPermissions()
    {
        return usedPermissions;
    }

    /**
     * Integer version code.
     */
    @SerializedName("versionCode")
    @Expose
    private int versionCode  = -1;

    public int getVersionCode()
    {
        return versionCode;
    }

    /**
     * Version name to display.
     */
    @SerializedName("versionName")
    @Expose
    private String versionName  = null;

    public String getVersionName()
    {
        return versionName;
    }

    /**
     * Indicates if a package is installed on the system.
     */
    private boolean isInstalled = false;

    public boolean isInstalled()
    {
        return isInstalled;
    }

    /**
     * Path where the .apk file is downloaded. null for not downloaded .apk files.
     */
    private String localDownloadPath = null;

    public String getLocalDownloadPath()
    {
        return localDownloadPath;
    }

    /**
     * Contains auto- and user-generated metadata
     */
    private List<Tag> userdata = null;

    public List<Tag> getUserdata()
    {
        return userdata;
    }

    /**
     * Specify the repo name where the file comes from
     */
    private String repoName = null;

    public String getRepoName()
    {
        return repoName;
    }
}
```

The AppPackage class has a field userdata represented by a list of Tags. 
A Tag is defined here:  

```java
public static class Tag implements Serializable
{
    private String key = null;
   
    public String getKey()
    {
        return key;
    }

    private String type = null;

    public String getType()
    {
        return type;
    }

    private String value = null;

    public String getValue()
    {
        return value;
    }

}
``` 

## Invoking application download

Applications can be downloaded in the background without interfering with the normal OS operation. To invoke the download of a specific application package, third-party applications must provide an implementation inheriting the following class:

```java
public abstract class PackageDownloadManager
{
    /**
     * Establish the connection to the service.
     */
    public final boolean bind(Context context);

  /**
   * Starts the download request of a given AppPackage
   *
   * @param appPackage the Package to download
   * @throws ServiceNotBoundException if the StorageService is not bound before calling
   */
   public void downloadPackage(AppPackage appPackage) throws ServiceNotBoundException

    /**
     * Terminate the connection to the service.
     */
    public final void unbind(Context context);
}
```

Implement the following methods to handle download results:

```java
/**
  * Callback that must be implemented to receive the downloaded package
  *
  * @param appPackage The downloaded AppPackage
  */
public abstract void onDownloadFinished(AppPackage appPackage);

/**
  * Callback that must be implemented to react on failed downloads
  *
  * @param appPackage the AppPackage where the download failed
  * @param error The Message why the installation failed
  */
public abstract void onDownloadFailed(AppPackage appPackage, String error);

/**
  * Callback that must be implemented to react on Progress changes
  *
  * @param appPackage the package where the progress changed
  * @param progress value of the new progress value in %
  */
public abstract void onProgressChanged(AppPackage appPackage, int progress);
```

## Starting app installation

After a successful download, applications can be silently installed on the device. Third-party applications must implement a class extending the following class:

'''java
public abstract class PackageInstallManager extends AbstractManager
'''

The installation can be started by calling the following API method:

```java
    /**
     * Establish the connection to the service.
     */
    public final boolean bind(Context context);

/**
  * Start the installation of a given AppPackage
  *
  * @param appPackage The package to be installed
  * @throws ServiceNotBoundException if the storage service is not bound
  */
public void installPackage(AppPackage appPackage) throws ServiceNotBoundException;

    /**
     * Terminate the connection to the service.
     */
    public final void unbind(Context context);
```

To handle installation results the following methods need to be implemented:

```java
/**
  * Callback that must be implemented to get successful installation result
  *
  * @param appPackage the package that got installed
  */
public abstract void onInstallSuccessful(AppPackage appPackage);

/**
  * Callback that must be implemented to get notification about failed installation
  *
  * @param appPackage The package where the installation failed
  * @param error The Message why the installation failed
  */
public abstract void onInstallFailed(AppPackage appPackage, String error);
```
