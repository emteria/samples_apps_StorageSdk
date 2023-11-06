package com.emteria.sample.app.update;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.emteria.storage.contract.manager.PackageListManager;

public class PackageTask extends AsyncTask<PackageListManager, Void, Void>
{
    Context mContext;

    public PackageTask(Context context)
    {
        this.mContext = context;
    }

    @Override
    protected Void doInBackground(PackageListManager... packageListManagers)
    {
        Log.i("Package TASK","Started getPackages");
        PackageListManager mPackageHandler = packageListManagers[0];
        mPackageHandler.bind(mContext);
        mPackageHandler.getPackages("emteria");
        return null;
    }
}
