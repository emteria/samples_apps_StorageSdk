package com.emteria.sample.app.update;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.emteria.storage.contract.manager.PackageListManager;

public class PackageTask extends AsyncTask<PackageListManager, Void, Void>
{
    Context mContext;
    String mRepoName;

    public PackageTask(Context context, String repoName)
    {
        this.mContext = context;
        this.mRepoName = repoName;
    }

    public PackageTask(Context context)
    {
        this.mContext = context;
        this.mRepoName = null;
    }

    @Override
    protected Void doInBackground(PackageListManager... packageListManagers)
    {
        Log.i("Package TASK","Started getPackages");
        PackageListManager mPackageHandler = packageListManagers[0];
        mPackageHandler.bind(mContext);
        if (mRepoName != null)
        {
            mPackageHandler.getPackages(mRepoName);
        }
        else
        {
            mPackageHandler.getPackages();
        }

        return null;
    }
}
