package com.emteria.sample.app.update;

import android.content.Context;
import android.os.AsyncTask;

import com.emteria.storage.contract.models.AppPackage;
import com.emteria.storage.contract.managers.PackageInstallationManager;

import java.util.List;

public class InstallTask extends AsyncTask<PackageInstallationManager, Void, Void>
{
    private final List<AppPackage> mAppPackages;
    private final Context mContext;

    public InstallTask(Context context, List<AppPackage> appPackages)
    {
        mAppPackages = appPackages;
        mContext = context;
    }

    @Override
    protected Void doInBackground(PackageInstallationManager... packageInstallManagers)
    {
        PackageInstallationManager p = packageInstallManagers[0];
        p.bind(mContext);
        for (AppPackage app : mAppPackages)
        {
            p.installPackage(app);
        }


        return null;
    }
}
