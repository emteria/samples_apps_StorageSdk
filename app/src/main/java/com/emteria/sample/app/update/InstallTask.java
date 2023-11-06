package com.emteria.sample.app.update;

import android.content.Context;
import android.os.AsyncTask;

import com.emteria.storage.contract.AppPackage;
import com.emteria.storage.contract.manager.PackageInstallManager;

import java.util.List;

public class InstallTask extends AsyncTask<PackageInstallManager, Void, Void>
{
    List<AppPackage> mAppPackages;
    Context mContext;

    public InstallTask(Context context, List<AppPackage> appPackages)
    {
        mAppPackages = appPackages;
        mContext = context;
    }

    @Override
    protected Void doInBackground(PackageInstallManager... packageInstallManagers)
    {
        PackageInstallManager p = packageInstallManagers[0];
        p.bind(mContext);
        for (AppPackage app : mAppPackages)
        {
            p.installPackage(app);
        }


        return null;
    }
}
