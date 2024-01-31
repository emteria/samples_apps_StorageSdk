package com.emteria.sample.app.update;

import android.content.Context;
import android.os.AsyncTask;

import com.emteria.storage.contract.models.AppPackage;
import com.emteria.storage.contract.managers.PackageDownloadManager;

import java.util.List;

public class DownloadTask extends AsyncTask<PackageDownloadManager, Void, Void>
{
    private final List<AppPackage> mAppPackages;
    private final Context mContext;

    public DownloadTask(Context context, List<AppPackage> appPackage)
    {
        mAppPackages = appPackage;
        mContext = context;
    }

    @Override
    protected Void doInBackground(PackageDownloadManager... packageDownloadManagers)
    {
        PackageDownloadManager p = packageDownloadManagers[0];
        p.bindToAppManagement(mContext);
        for (AppPackage app : mAppPackages)
        {
            p.downloadPackage(app);
        }

        return null;
    }
}
