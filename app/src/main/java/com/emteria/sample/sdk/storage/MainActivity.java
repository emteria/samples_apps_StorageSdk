package com.emteria.sample.sdk.storage;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.emteria.sample.sdk.storage.tasks.AppDownloadTask;
import com.emteria.sample.sdk.storage.tasks.AppInstallationTask;
import com.emteria.sample.sdk.storage.tasks.AppRetrievalTask;
import com.emteria.sample.sdk.storage.tasks.DeviceRegistrationTask;
import com.emteria.sample.sdk.storage.tasks.DeviceStatusTask;
import com.emteria.sample.sdk.storage.tasks.RegistrationDetailsTask;
import com.emteria.storage.contract.managers.DeviceRegistrationManager;
import com.emteria.storage.contract.managers.PackageDownloadManager;
import com.emteria.storage.contract.managers.PackageInstallationManager;
import com.emteria.storage.contract.managers.PackageMetadataManager;
import com.emteria.storage.contract.models.AppPackage;
import com.emteria.storage.contract.models.RegistrationDetails;

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

    private int mDownloadCounter = 0;
    private int mInstallCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPackageHandler = new PackageHandler();
        mInstallHandler = new InstallHandler();
        mDownloadHandler = new DownloadHandler();
        mRegistrationHandler = new RegistrationHandler();

        Button getFdroidPackages = findViewById(R.id.getPackages);
        getFdroidPackages.setOnClickListener(v ->
        {
            LinearLayout view = findViewById(R.id.scrollLayout);
            view.removeAllViews();
            AppRetrievalTask t = new AppRetrievalTask(getApplicationContext(), "emteria");
            t.execute(mPackageHandler);
        });

        Button getUploadedPackages = findViewById(R.id.getPackagesS3);
        getUploadedPackages.setOnClickListener(v ->
        {
            LinearLayout view = findViewById(R.id.scrollLayout);
            view.removeAllViews();
            AppRetrievalTask t = new AppRetrievalTask(getApplicationContext());
            t.execute(mPackageHandler);
        });

        LinearLayout view = findViewById(R.id.scrollLayout);

        Button downloadPackage = findViewById(R.id.downloadPackages);
        downloadPackage.setOnClickListener(v ->
        {
            if (mAvailablePackages.isEmpty())
            {
                Toast.makeText(getApplicationContext(), "No available packages", Toast.LENGTH_LONG).show();
                return;
            }

            List<AppPackage> toDownload = new ArrayList<>();
            for (int i = 0; i < view.getChildCount(); i++)
            {
                CheckBox box = (CheckBox) view.getChildAt(i);
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

            view.removeAllViews();
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
            for (int i = 0; i < view.getChildCount(); i++)
            {
                CheckBox box = (CheckBox) view.getChildAt(i);
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
            EditText universalLicense = findViewById(R.id.universalLicense);
            if (universalLicense.getText().toString().isEmpty())
            {
                Log.d(TAG, "universal license is empty");
                return;
            }
            DeviceRegistrationTask task = new DeviceRegistrationTask(getApplicationContext(), universalLicense.getText().toString());
            task.execute(mRegistrationHandler);
        });

        Button deviceStatus = findViewById(R.id.deviceStatus);
        deviceStatus.setOnClickListener(v ->
        {
            DeviceStatusTask task = new DeviceStatusTask(getApplicationContext());
            task.execute(mRegistrationHandler);
        });

        Button registrationDetails = findViewById(R.id.registrationInformation);
        registrationDetails.setOnClickListener(v ->
        {
            RegistrationDetailsTask task = new RegistrationDetailsTask(getApplicationContext());
            task.execute(mRegistrationHandler);
        });
    }

    private class PackageHandler extends PackageMetadataManager
    {
        @Override
        public void onReceive(HashMap<String, List<AppPackage>> packages)
        {
            Log.d(TAG, packages.toString());
            LinearLayout view = MainActivity.this.findViewById(R.id.scrollLayout);
            view.removeAllViews();
            mAvailablePackages.clear();
            for (List<AppPackage> apps : packages.values())
            {
                for (AppPackage app : apps)
                {
                    mAvailablePackages.put(app.getAppId(), app);
                    CheckBox c = new CheckBox(MainActivity.this.getApplicationContext());
                    c.setChecked(false);
                    c.setText((app.getApkName() != null) ? app.getApkName() : app.getPackageName());
                    view.addView(c);
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
            LinearLayout view = MainActivity.this.findViewById(R.id.scrollLayout);
            boolean found = false;
            for (int i = 0; i < view.getChildCount(); i++)
            {
                CheckBox c = null;
                try
                {
                    c = (CheckBox) view.getChildAt(i);
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
                view.addView(c);
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
            LinearLayout view = MainActivity.this.findViewById(R.id.scrollLayout);
            boolean found = false;
            AppPackage app = mAvailablePackages.get(appPackageId);

            for (int i = 0; i < view.getChildCount(); i++)
            {
                CheckBox c = null;
                try
                {
                    c = (CheckBox) view.getChildAt(i);
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
                view.addView(c);
            }
        }
    }

    private class InstallHandler extends PackageInstallationManager
    {
        @Override
        public void onInstallSuccessful(AppPackage appPackage)
        {
            LinearLayout view = MainActivity.this.findViewById(R.id.scrollLayout);
            for (int i = 0; i < view.getChildCount(); i++)
            {
                CheckBox c = null;
                try
                {
                    c = (CheckBox) view.getChildAt(i);
                }
                catch (ClassCastException e)
                {
                    continue;
                }

                if (c.getText().toString().contains(appPackage.getApkName()))
                {
                    view.removeView(c);
                }
            }
            TextView v = new TextView(MainActivity.this.getApplicationContext());
            v.setText("Package " + appPackage.getApkName() +  ": installation successful");
            view.addView(v);
            mInstallCounter--;
            if (mInstallCounter == 0)
            {
                mInstallHandler.unbind(getApplicationContext());
            }
        }

        @Override
        public void onInstallFailed(String appPackageId, String error)
        {
            LinearLayout view = MainActivity.this.findViewById(R.id.scrollLayout);
            for (int i = 0; i < view.getChildCount(); i++)
            {
                CheckBox c = null;
                try
                {
                    c = (CheckBox) view.getChildAt(i);
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
                            view.removeView(c);
                        }
                    }
                }
            }

            TextView v = new TextView(MainActivity.this.getApplicationContext());
            v.setText("Package with id " + appPackageId +  "installation failed");
            view.addView(v);

            mInstallCounter--;
            if (mInstallCounter == 0)
            {
                mInstallHandler.unbind(getApplicationContext());
            }
        }
    }

    private class RegistrationHandler extends DeviceRegistrationManager
    {
        @Override
        public void onRegistrationSuccess()
        {
            LinearLayout view = MainActivity.this.findViewById(R.id.scrollLayout);
            view.removeAllViews();
            TextView text = new TextView(getApplicationContext());
            text.setText("Device successfully registered");

            view.addView(text);
        }

        @Override
        public void onRegistrationFailure(String s)
        {
            LinearLayout view = MainActivity.this.findViewById(R.id.scrollLayout);
            view.removeAllViews();
            TextView text = new TextView(getApplicationContext());
            text.setText("Device register failed");

            view.addView(text);
        }

        @Override
        public void onRegistrationStatus(boolean b)
        {
            LinearLayout view = MainActivity.this.findViewById(R.id.scrollLayout);
            view.removeAllViews();
            TextView text = new TextView(getApplicationContext());
            if (b)
            {
                text.setText("Device is already registered");
                view.addView(text);
            }
            else
            {
                text.setText("Device is not registered");
                view.addView(text);
            }
        }

        @Override
        public void onRegistrationDetailsSuccess(RegistrationDetails reginfo)
        {
            LinearLayout view = MainActivity.this.findViewById(R.id.scrollLayout);
            view.removeAllViews();
            TextView text = new TextView(getApplicationContext());
            text.append(reginfo.getDeviceId() + "\n");
            text.append(reginfo.getDeviceName() + "\n");
            text.append(reginfo.getDeviceDescription() + "\n");
            text.append(reginfo.getGroupId() + "\n");
            text.append(reginfo.getGroupName() + "\n");
            text.append(reginfo.getDeviceDescription() + "\n");
            view.addView(text);
        }

        @Override
        public void onRegistrationDetailsFailure(String s)
        {
            LinearLayout view = MainActivity.this.findViewById(R.id.scrollLayout);
            view.removeAllViews();
            TextView text = new TextView(getApplicationContext());
            text.append("No registration details: " + s);
            view.addView(text);
        }
    }
}
