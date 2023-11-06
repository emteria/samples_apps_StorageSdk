package com.emteria.sample.app.update;

import android.content.Context;
import android.os.AsyncTask;

import com.emteria.storage.contract.AppPackage;
import com.emteria.storage.contract.manager.PackageDownloadManager;

import java.util.List;

public class DownloadTask extends AsyncTask<PackageDownloadManager, Void, Void>
{
    List<AppPackage> mAppPackages;
    Context mContext;

    public DownloadTask(Context context, List<AppPackage> appPackage)
    {
        mAppPackages = appPackage;
        mContext = context;
    }

    @Override
    protected Void doInBackground(PackageDownloadManager... packageDownloadManagers)
    {
        PackageDownloadManager p = packageDownloadManagers[0];
        p.bind(mContext);
        for (AppPackage app : mAppPackages)
        {
            p.downloadPackage(app);
        }

        return null;
    }
}
