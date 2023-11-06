package com.emteria.sample.app.update;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.emteria.storage.contract.AppPackage;
import com.emteria.storage.contract.manager.PackageDownloadManager;
import com.emteria.storage.contract.manager.PackageInstallManager;
import com.emteria.storage.contract.manager.PackageListManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "EmteriaExternalAppUpdateSample - MainActivity";

    HashMap<String, List<AppPackage>> availablePackages = new HashMap<>();
    List<AppPackage> downloadedPackages = new ArrayList<>();

    int mDownloadCounter = 0;
    int mInstallCounter = 0;

    PackageHandler mPackageHandler;
    DownloadHandler mDownloadHandler;
    InstallHandler mInstallHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button getPackages = findViewById(R.id.getPackages);

        mPackageHandler = new PackageHandler(this);
        mInstallHandler = new InstallHandler(this);
        mDownloadHandler = new DownloadHandler(this);

        getPackages.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                LinearLayout view = findViewById(R.id.sscrolLayout);
                view.removeAllViews();
                PackageTask t = new PackageTask(getApplicationContext());
                t.execute(mPackageHandler);
            }
        });

        LinearLayout view = findViewById(R.id.sscrolLayout);

        Button downloadPackage = findViewById(R.id.downloadPackages);
        downloadPackage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (availablePackages.isEmpty())
                {
                    Toast.makeText(getApplicationContext(), "No Available packages", Toast.LENGTH_LONG).show();
                    return;
                }
                List<AppPackage> toDownload = new ArrayList<>();
                for (int i = 0; i < view.getChildCount(); i++)
                {
                    CheckBox box = (CheckBox) view.getChildAt(i);
                    if (box.isChecked())
                    {
                        for (Map.Entry<String, List<AppPackage>> entry : availablePackages.entrySet())
                        {
                            for (AppPackage a : entry.getValue())
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
                }
                if (toDownload.isEmpty())
                {
                    Toast.makeText(getApplicationContext(), "Nothing to download", Toast.LENGTH_LONG).show();
                    return;
                }

                view.removeAllViews();
                DownloadTask t = new DownloadTask(getApplicationContext(), toDownload);
                t.execute(mDownloadHandler);
                mDownloadCounter = toDownload.size();
                availablePackages = new HashMap<>();

            }
        });

        Button installPackages = findViewById(R.id.installPackages);
        installPackages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (downloadedPackages.isEmpty())
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
                        for (AppPackage app : downloadedPackages)
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
                Log.d(TAG, "Installable apps are: " + installablePackages.toString());

                InstallTask task = new InstallTask(getApplicationContext(), installablePackages);
                task.execute(mInstallHandler);
                mInstallCounter = installablePackages.size();
                downloadedPackages = new ArrayList<>();
            }
        });
    }

    private class PackageHandler extends PackageListManager
    {
        private static final String TAG = "ExternalUpdateSample - PackageHandler";
        private final MainActivity activity;

        private PackageHandler(MainActivity activity) {
            this.activity = activity;
        }

        @Override
        public void onReceive(HashMap<String, List<AppPackage>> packages) {
            activity.availablePackages = packages;
            Log.d(TAG, packages.toString());
            LinearLayout view = activity.findViewById(R.id.sscrolLayout);
            view.removeAllViews();
            for (Map.Entry<String, List<AppPackage>> entry : packages.entrySet())
            {
                List<AppPackage> appPackages = entry.getValue();
                for (AppPackage p : appPackages)
                {
                    CheckBox c = new CheckBox(activity.getApplicationContext());
                    c.setChecked(false);
                    c.setText((p.getApkName() != null) ? p.getApkName() : p.getPackageName());
                    view.addView(c);
                }
            }
            mPackageHandler.unbind(getApplicationContext());
        }

        @Override
        public void onFailure(String error)
        {

        }
    }

    private class DownloadHandler extends PackageDownloadManager
    {
        private static final String TAG = "ExternalUpdateSample - DownloadHandler";
        private final MainActivity activity;

        private DownloadHandler(MainActivity activity) {
            this.activity = activity;
        }

        @Override
        public void onDownloadFinished(AppPackage appPackage)
        {
            downloadedPackages.add(appPackage);
            LinearLayout view = activity.findViewById(R.id.sscrolLayout);
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
                CheckBox c = new CheckBox(activity.getApplicationContext());
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
        public void onDownloadFailed(AppPackage appPackage, String error)
        {
            Log.d(MainActivity.class.getSimpleName(), "Download for " + appPackage.getApkName() + " failed because of:");
            Log.d(MainActivity.class.getSimpleName(), error);
        }

        @Override
        public void onProgressChanged(AppPackage appPackage, int progress)
        {
            LinearLayout view = activity.findViewById(R.id.sscrolLayout);
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
                    c.setText(appPackage.getApkName() + " download progress: " + progress + "%");
                    found = true;
                }
            }

            if (!found)
            {
                CheckBox c = new CheckBox(activity.getApplicationContext());
                c.setText(appPackage.getApkName() + " download progress: " + progress + "%");
                c.setChecked(false);
                view.addView(c);
            }
        }
    }

    private class InstallHandler extends PackageInstallManager
    {
        private static final String TAG = "ExternalUpdateSample - InstallHandler";
        private final MainActivity activity;

        private InstallHandler(MainActivity activity) {
            this.activity = activity;
        }

        @Override
        public void onInstallSuccessful(AppPackage appPackage)
        {
            LinearLayout view = activity.findViewById(R.id.sscrolLayout);
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
            TextView v = new TextView(activity.getApplicationContext());
            v.setText("Package " + appPackage.getApkName() +  "installation was successful");
            view.addView(v);
            mInstallCounter--;
            if (mInstallCounter == 0)
            {
                mInstallHandler.unbind(getApplicationContext());
            }
        }

        @Override
        public void onInstallFailed(AppPackage appPackage, String error)
        {
            LinearLayout view = activity.findViewById(R.id.sscrolLayout);
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
            TextView v = new TextView(activity.getApplicationContext());
            v.setText("Package " + appPackage.getApkName() +  "installation failed");
            view.addView(v);
            mInstallCounter--;
            if (mInstallCounter == 0)
            {
                mInstallHandler.unbind(getApplicationContext());
            }
        }
    }
}
