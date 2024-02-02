package com.emteria.sample.app.update;

import android.content.Context;
import android.os.AsyncTask;

import com.emteria.storage.contract.managers.DeviceRegistrationManager;
import com.emteria.storage.contract.managers.PackageDownloadManager;


public class RegisterDeviceTask extends AsyncTask<DeviceRegistrationManager, Void, Void>
{
    Context mContext;
    String mUniversalLicense = null;
    public RegisterDeviceTask(Context context, String universalLicense)
    {
        this.mContext = context;
        this.mUniversalLicense = universalLicense;
    }

    public RegisterDeviceTask(Context context)
    {
        this.mContext = context;
    }

    @Override
    protected Void doInBackground(DeviceRegistrationManager... arg)
    {
        DeviceRegistrationManager p = arg[0];
        p.bindToDeviceManagement(mContext);
        p.registerDevice(mUniversalLicense);

        return null;
    }
}
