package com.emteria.sample.app.update;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.emteria.storage.contract.managers.PackageMetadataManager;

public class PackageTask extends AsyncTask<PackageMetadataManager, Void, Void>
{
    private final Context mContext;
    private final String mRepoName;

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
    protected Void doInBackground(PackageMetadataManager... packageListManagers)
    {
        Log.i("Package TASK","Started getPackages");

        PackageMetadataManager mPackageHandler = packageListManagers[0];

        mPackageHandler.bindToAppManagement(mContext);
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
