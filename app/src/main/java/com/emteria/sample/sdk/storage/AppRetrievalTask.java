package com.emteria.sample.sdk.storage;

import android.content.Context;
import android.os.AsyncTask;

import com.emteria.storage.contract.managers.PackageMetadataManager;

public class AppRetrievalTask extends AsyncTask<PackageMetadataManager, Void, Void>
{
    private final Context mContext;
    private final String mRepoName;

    public AppRetrievalTask(Context context, String repoName)
    {
        mContext = context;
        mRepoName = repoName;
    }

    public AppRetrievalTask(Context context)
    {
        mContext = context;
        mRepoName = null;
    }

    @Override
    protected Void doInBackground(PackageMetadataManager... packageListManagers)
    {
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
