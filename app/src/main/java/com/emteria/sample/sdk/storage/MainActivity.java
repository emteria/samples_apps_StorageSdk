package com.emteria.sample.sdk.storage;

import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.emteria.sample.sdk.storage.tasks.AppDownloadTask;
import com.emteria.sample.sdk.storage.tasks.AppInstallationTask;
import com.emteria.sample.sdk.storage.tasks.AppRetrievalTask;
import com.emteria.sample.sdk.storage.tasks.DeviceRegistrationTask;
import com.emteria.sample.sdk.storage.tasks.DeviceStatusTask;
import com.emteria.sample.sdk.storage.tasks.FileListingTask;
import com.emteria.sample.sdk.storage.tasks.FileRetrievalTask;
import com.emteria.sample.sdk.storage.tasks.FileUploadTask;
import com.emteria.sample.sdk.storage.tasks.RegistrationDetailsTask;
import com.emteria.storage.contract.managers.DeviceRegistrationManager;
import com.emteria.storage.contract.managers.FileDownloadManager;
import com.emteria.storage.contract.managers.FileListManager;
import com.emteria.storage.contract.managers.FileUploadManager;
import com.emteria.storage.contract.managers.PackageDownloadManager;
import com.emteria.storage.contract.managers.PackageInstallationManager;
import com.emteria.storage.contract.managers.PackageMetadataManager;
import com.emteria.storage.contract.models.AppPackage;
import com.emteria.storage.contract.models.RegistrationDetails;
import com.emteria.storage.contract.models.RemoteFile;
import com.emteria.storage.contract.utils.ParcelFileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "Emteria Storage SDK Sample";

    private final HashMap<String, AppPackage> mAvailablePackages = new HashMap<>();
    private final List<AppPackage> mDownloadedPackages = new ArrayList<>();

    private PackageHandler mPackageHandler;
    private DownloadHandler mDownloadHandler;
    private InstallHandler mInstallHandler;
    private RegistrationHandler mRegistrationHandler;
    private UploadHandler mUploadHandler;
    private FileListHandler mFileListHandler;
    private FileDownloadHandler mFileDownloadHandler;

    private final HashMap<String, RemoteFile> mAvailableFiles = new HashMap<>();

    private ScrollView mScrollView = null;
    private LinearLayout mResultsLayout = null;

    private int mDownloadCounter = 0;
    private int mInstallCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mScrollView = findViewById(R.id.scrollView);
        mResultsLayout = findViewById(R.id.resultsLayout);

        mPackageHandler = new PackageHandler();
        mInstallHandler = new InstallHandler();
        mDownloadHandler = new DownloadHandler();
        mRegistrationHandler = new RegistrationHandler();
        mUploadHandler = new UploadHandler();
        mFileListHandler = new FileListHandler();
        mFileDownloadHandler = new FileDownloadHandler();

        Button getFdroidPackages = findViewById(R.id.getPackages);
        getFdroidPackages.setOnClickListener(v ->
        {
            mResultsLayout.removeAllViews();

            EditText repoNameEdit = findViewById(R.id.fdroidRepoName);
            if (repoNameEdit.getText().toString().isEmpty())
            {
                Toast.makeText(this, "Repo name is required", Toast.LENGTH_SHORT).show();
                return;
            }

            AppRetrievalTask t = new AppRetrievalTask(getApplicationContext(), repoNameEdit.getText().toString());
            t.execute(mPackageHandler);
        });

        Button getUploadedPackages = findViewById(R.id.getPackagesS3);
        getUploadedPackages.setOnClickListener(v ->
        {
            mResultsLayout.removeAllViews();
            AppRetrievalTask t = new AppRetrievalTask(getApplicationContext());
            t.execute(mPackageHandler);
        });

        Button downloadPackage = findViewById(R.id.downloadPackages);
        downloadPackage.setOnClickListener(v ->
        {
            if (mAvailablePackages.isEmpty())
            {
                Toast.makeText(getApplicationContext(), "No available packages", Toast.LENGTH_LONG).show();
                return;
            }

            List<AppPackage> toDownload = new ArrayList<>();
            for (int i = 0; i < mResultsLayout.getChildCount(); i++)
            {
                CheckBox box = (CheckBox) mResultsLayout.getChildAt(i);
                if (box.isChecked())
                {
                    for (AppPackage a : mAvailablePackages.values())
                    {
                        if (box.getText().equals(a.getApkName()))
                        {
                            if (a.isInstalled())
                            {
                                Toast.makeText(getApplicationContext(), a.getApkName() + " is already installed", Toast.LENGTH_LONG).show();
                                break;
                            }
                            toDownload.add(a);
                            break;
                        }
                    }
                }
            }

            if (toDownload.isEmpty())
            {
                Toast.makeText(getApplicationContext(), "Nothing to download", Toast.LENGTH_LONG).show();
                return;
            }

            mResultsLayout.removeAllViews();
            AppDownloadTask t = new AppDownloadTask(getApplicationContext(), toDownload);
            t.execute(mDownloadHandler);

            mDownloadCounter = toDownload.size();
        });

        Button installPackages = findViewById(R.id.installPackages);
        installPackages.setOnClickListener(v ->
        {
            if (mDownloadedPackages.isEmpty())
            {
                Toast.makeText(getApplicationContext(), "No downloaded packages to install", Toast.LENGTH_LONG).show();
                return;
            }

            List<AppPackage> installablePackages = new ArrayList<>();
            for (int i = 0; i < mResultsLayout.getChildCount(); i++)
            {
                CheckBox box = (CheckBox) mResultsLayout.getChildAt(i);
                if (box.isChecked())
                {
                    for (AppPackage app : mDownloadedPackages)
                    {
                        if (app.isInstalled()) {continue;}
                        String name = box.getText().toString();
                        int end = name.indexOf("download finished") - 1;

                        if (name.substring(0, end).equals(app.getApkName()))
                        {
                            installablePackages.add(app);
                        }
                    }
                }
            }

            if (installablePackages.isEmpty())
            {
                Toast.makeText(getApplicationContext(), "All downloaded packages are already installed", Toast.LENGTH_LONG).show();
                return;
            }

            Log.d(TAG, "Installable apps are: " + installablePackages);
            AppInstallationTask task = new AppInstallationTask(getApplicationContext(), installablePackages);
            task.execute(mInstallHandler);

            mInstallCounter = installablePackages.size();
            mDownloadedPackages.clear();
        });

        Button registerDevice = findViewById(R.id.registerDevice);
        registerDevice.setOnClickListener(v ->
        {
            mResultsLayout.removeAllViews();

            EditText universalLicense = findViewById(R.id.universalLicense);
            if (universalLicense.getText().toString().isEmpty())
            {
                Log.e(TAG, "Universal license cannot be empty");
                Toast.makeText(this, "Universal license required", Toast.LENGTH_SHORT).show();
                return;
            }

            DeviceRegistrationTask task = new DeviceRegistrationTask(getApplicationContext(), universalLicense.getText().toString());
            task.execute(mRegistrationHandler);
        });

        Button deviceStatus = findViewById(R.id.deviceStatus);
        deviceStatus.setOnClickListener(v ->
        {
            mResultsLayout.removeAllViews();

            DeviceStatusTask task = new DeviceStatusTask(getApplicationContext());
            task.execute(mRegistrationHandler);
        });

        Button registrationDetails = findViewById(R.id.registrationInformation);
        registrationDetails.setOnClickListener(v ->
        {
            mResultsLayout.removeAllViews();

            RegistrationDetailsTask task = new RegistrationDetailsTask(getApplicationContext());
            task.execute(mRegistrationHandler);
        });

        Button uploadFile = findViewById(R.id.uploadFile);
        uploadFile.setOnClickListener(v ->
        {
            mResultsLayout.removeAllViews();

            EditText filePathEdit = findViewById(R.id.filePath);
            if (filePathEdit.getText().toString().isEmpty())
            {
                Toast.makeText(this, "File path is required", Toast.LENGTH_SHORT).show();
                return;
            }

            FileUploadTask task = new FileUploadTask(getApplicationContext(), mUploadHandler, filePathEdit.getText().toString());
            task.execute();
        });

        Button listFiles = findViewById(R.id.listFiles);
        listFiles.setOnClickListener(v ->
        {
            mResultsLayout.removeAllViews();

            FileListingTask task = new FileListingTask(getApplicationContext());
            task.execute(mFileListHandler);
        });

        Button downloadFile = findViewById(R.id.downloadFile);
        downloadFile.setOnClickListener(v ->
        {
            EditText storageFileIdEdit = findViewById(R.id.storageFileId);
            String storageFileId = storageFileIdEdit.getText().toString().trim();
            if (storageFileId.isEmpty())
            {
                Toast.makeText(this, "Storage file id is required", Toast.LENGTH_SHORT).show();
                return;
            }

            mResultsLayout.removeAllViews();

            FileRetrievalTask task = new FileRetrievalTask(getApplicationContext(), storageFileId);
            task.execute(mFileDownloadHandler);
        });
    }

    private class PackageHandler extends PackageMetadataManager
    {
        @Override
        public void onReceive(HashMap<String, List<AppPackage>> packages)
        {
            mResultsLayout.removeAllViews();
            mAvailablePackages.clear();

            for (List<AppPackage> apps : packages.values())
            {
                for (AppPackage app : apps)
                {
                    mAvailablePackages.put(app.getAppId(), app);
                    CheckBox c = new CheckBox(MainActivity.this.getApplicationContext());
                    c.setChecked(false);
                    c.setText((app.getApkName() != null) ? app.getApkName() : app.getPackageName());
                    mResultsLayout.addView(c);
                }
            }

            mPackageHandler.unbind(getApplicationContext());
        }

        @Override
        public void onFailure(String error)
        {
            Log.e(TAG, "Error " + error);
        }
    }

    private class DownloadHandler extends PackageDownloadManager
    {
        @Override
        public void onDownloadFinished(AppPackage appPackage)
        {
            mDownloadedPackages.add(appPackage);
            boolean found = false;
            for (int i = 0; i < mResultsLayout.getChildCount(); i++)
            {
                CheckBox c;
                try
                {
                    c = (CheckBox) mResultsLayout.getChildAt(i);
                }
                catch (ClassCastException e)
                {
                    continue;
                }

                if (c.getText().toString().contains(appPackage.getApkName()))
                {
                    c.setText(appPackage.getApkName() + " download finished");
                    found = true;
                }
            }

            if (!found)
            {
                CheckBox c = new CheckBox(MainActivity.this.getApplicationContext());
                c.setText(appPackage.getApkName() + " download finished");
                c.setChecked(false);
                mResultsLayout.addView(c);
            }

            mDownloadCounter--;
            if (mDownloadCounter == 0)
            {
                mDownloadHandler.unbind(getApplicationContext());
            }
        }

        @Override
        public void onDownloadFailed(String appPackageId, String error)
        {
            Log.d(MainActivity.class.getSimpleName(), "Download for " + appPackageId + " failed: " + error);
        }

        @Override
        public void onProgressChanged(String appPackageId, int progress)
        {
            boolean found = false;
            AppPackage app = mAvailablePackages.get(appPackageId);

            for (int i = 0; i < mResultsLayout.getChildCount(); i++)
            {
                CheckBox c;
                try
                {
                    c = (CheckBox) mResultsLayout.getChildAt(i);
                }
                catch (ClassCastException e)
                {
                    continue;
                }

                if (c.getText().toString().contains(app.getApkName()))
                {
                    c.setText(app.getApkName() + " download progress: " + progress + "%");
                    found = true;
                }
            }

            if (!found)
            {
                CheckBox c = new CheckBox(MainActivity.this.getApplicationContext());
                c.setText(app.getApkName() + " download progress: " + progress + "%");
                c.setChecked(false);
                mResultsLayout.addView(c);
            }
        }
    }

    private class InstallHandler extends PackageInstallationManager
    {
        @Override
        public void onInstallSuccessful(AppPackage appPackage)
        {
            for (int i = 0; i < mResultsLayout.getChildCount(); i++)
            {
                CheckBox c;
                try
                {
                    c = (CheckBox) mResultsLayout.getChildAt(i);
                }
                catch (ClassCastException e)
                {
                    continue;
                }

                if (c.getText().toString().contains(appPackage.getApkName()))
                {
                    mResultsLayout.removeView(c);
                }
            }
            TextView v = new TextView(MainActivity.this.getApplicationContext());
            v.setText("Package " + appPackage.getApkName() +  ": installation successful");
            mResultsLayout.addView(v);

            mInstallCounter--;
            if (mInstallCounter == 0)
            {
                mInstallHandler.unbind(getApplicationContext());
            }
        }

        @Override
        public void onInstallFailed(String appPackageId, String error)
        {
            for (int i = 0; i < mResultsLayout.getChildCount(); i++)
            {
                CheckBox c;
                try
                {
                    c = (CheckBox) mResultsLayout.getChildAt(i);
                }
                catch (ClassCastException e)
                {
                    continue;
                }

                for (AppPackage app : mDownloadedPackages)
                {
                    if (app.getAppId().equals(appPackageId))
                    {
                        if (c.getText().toString().contains(app.getApkName()))
                        {
                            mResultsLayout.removeView(c);
                        }
                    }
                }
            }

            TextView v = new TextView(MainActivity.this.getApplicationContext());
            v.setText("Package ID " + appPackageId +  ": installation failed");
            mResultsLayout.addView(v);

            mInstallCounter--;
            if (mInstallCounter == 0)
            {
                unbind(getApplicationContext());
            }
        }
    }

    private class RegistrationHandler extends DeviceRegistrationManager
    {
        @Override
        public void onRegistrationSuccess()
        {
            TextView text = new TextView(getApplicationContext());
            text.setText("Device registration successful");

            mResultsLayout.removeAllViews();
            mResultsLayout.addView(text);
            mScrollView.post(() -> mScrollView.fullScroll(TextView.FOCUS_DOWN));

            unbind(getApplicationContext());
        }

        @Override
        public void onRegistrationFailure(String s)
        {
            TextView text = new TextView(getApplicationContext());
            text.setText("Device registration failed: " + s);

            mResultsLayout.removeAllViews();
            mResultsLayout.addView(text);
            mScrollView.post(() -> mScrollView.fullScroll(TextView.FOCUS_DOWN));

            unbind(getApplicationContext());
        }

        @Override
        public void onRegistrationStatus(boolean b)
        {
            TextView text = new TextView(getApplicationContext());
            if (b)
            {
                text.setText("Device is registered");
            }
            else
            {
                text.setText("Device is NOT registered");
            }

            mResultsLayout.removeAllViews();
            mResultsLayout.addView(text);
            mScrollView.post(() -> mScrollView.fullScroll(TextView.FOCUS_DOWN));

            unbind(getApplicationContext());
        }

        @Override
        public void onRegistrationDetailsSuccess(RegistrationDetails reginfo)
        {
            TextView text = new TextView(getApplicationContext());
            text.append("Device ID: " + reginfo.getDeviceId() + "\n");
            text.append("Device name: " + reginfo.getDeviceName() + "\n");
            text.append("Device description: " + reginfo.getDeviceDescription() + "\n");
            text.append("Group ID: " + reginfo.getGroupId() + "\n");
            text.append("Group name: " + reginfo.getGroupName() + "\n");
            text.append("Group description: " + reginfo.getGroupDescription() + "\n");

            mResultsLayout.removeAllViews();
            mResultsLayout.addView(text);
            mScrollView.post(() -> mScrollView.fullScroll(TextView.FOCUS_DOWN));

            unbind(getApplicationContext());
        }

        @Override
        public void onRegistrationDetailsFailure(String s)
        {
            TextView text = new TextView(getApplicationContext());
            text.append("No registration details: " + s);

            mResultsLayout.removeAllViews();
            mResultsLayout.addView(text);
            mScrollView.post(() -> mScrollView.fullScroll(TextView.FOCUS_DOWN));

            unbind(getApplicationContext());
        }
    }

    private class UploadHandler extends FileUploadManager
    {
        @Override
        public void onUploadSuccess()
        {
            Runnable updateUi = () ->
            {
                TextView text = new TextView(getApplicationContext());
                text.setText("File upload successful");

                mResultsLayout.removeAllViews();
                mResultsLayout.addView(text);
                mScrollView.post(() -> mScrollView.fullScroll(TextView.FOCUS_DOWN));
            };

            runOnUiThread(updateUi);
            unbind(getApplicationContext());
        }

        @Override
        public void onUploadError(String s)
        {
            Runnable updateUi = () ->
            {
                TextView text = new TextView(getApplicationContext());
                text.setText("File upload failed: " + s);

                mResultsLayout.removeAllViews();
                mResultsLayout.addView(text);
                mScrollView.post(() -> mScrollView.fullScroll(TextView.FOCUS_DOWN));
            };

            runOnUiThread(updateUi);
            unbind(getApplicationContext());
        }
    }

    private class FileListHandler extends FileListManager
    {
        @Override
        public void onFilesReceived(List<RemoteFile> files)
        {
            runOnUiThread(() ->
            {
                mResultsLayout.removeAllViews();
                mAvailableFiles.clear();

                if (files == null || files.isEmpty())
                {
                    TextView text = new TextView(getApplicationContext());
                    text.setText("No files available");
                    mResultsLayout.addView(text);
                }
                else
                {
                    for (RemoteFile file : files)
                    {
                        mAvailableFiles.put(file.getStorageFileId(), file);

                        TextView text = new TextView(getApplicationContext());
                        text.setText(file.getFilename() + "\n  id=" + file.getStorageFileId() + "  size=" + file.getSize());
                        mResultsLayout.addView(text);
                    }
                }

                mScrollView.post(() -> mScrollView.fullScroll(TextView.FOCUS_DOWN));
            });

            unbind(getApplicationContext());
        }

        @Override
        public void onListError(String error)
        {
            runOnUiThread(() -> showResult("File listing failed: " + error));
            unbind(getApplicationContext());
        }
    }

    private class FileDownloadHandler extends FileDownloadManager
    {
        @Override
        public void onFileReceived(String storageFileId, ParcelFileDescriptor pfd)
        {
            // consume the descriptor off the main thread, then delete the local copy immediately
            new Thread(() ->
            {
                String message;
                File destination = new File(getCacheDir(), "received-" + storageFileId.replaceAll("[^A-Za-z0-9._-]", "_"));
                try
                {
                    long bytes = ParcelFileUtils.copyToFile(pfd, destination);
                    String md5 = computeMd5(destination);
                    message = "Download successful for id=" + storageFileId + "\n  bytes=" + bytes + "\n  md5=" + md5;
                }
                catch (Exception e)
                {
                    message = "Failed consuming file for id=" + storageFileId + ": " + e.getMessage();
                }
                finally
                {
                    if (destination.exists() && destination.delete())
                    {
                        Log.d(TAG, "Deleted local copy of " + storageFileId);
                    }
                }

                final String result = message;
                runOnUiThread(() -> showResult(result));
                unbind(getApplicationContext());
            }).start();
        }

        @Override
        public void onDownloadError(String storageFileId, String error)
        {
            runOnUiThread(() -> showResult("Download failed for id=" + storageFileId + ": " + error));
            unbind(getApplicationContext());
        }
    }

    private void showResult(String text)
    {
        mResultsLayout.removeAllViews();

        TextView view = new TextView(getApplicationContext());
        view.setText(text);
        mResultsLayout.addView(view);

        mScrollView.post(() -> mScrollView.fullScroll(TextView.FOCUS_DOWN));
    }

    private String computeMd5(File file) throws IOException, NoSuchAlgorithmException
    {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        try (FileInputStream input = new FileInputStream(file))
        {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1)
            {
                digest.update(buffer, 0, read);
            }
        }

        StringBuilder builder = new StringBuilder();
        for (byte b : digest.digest())
        {
            builder.append(String.format("%02x", b));
        }

        return builder.toString();
    }
}
