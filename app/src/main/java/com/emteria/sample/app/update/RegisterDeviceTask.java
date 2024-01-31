package com.emteria.sample.app.update;

import android.content.Context;
import android.os.AsyncTask;

import com.emteria.storage.contract.managers.DeviceRegistrationManager;
import com.emteria.storage.contract.managers.PackageDownloadManager;


public class RegisterDeviceTask extends AsyncTask<DeviceRegistrationManager, Void, Void>
{
    Context mContext;
    String mUniversalLicense;
    public RegisterDeviceTask(Context context, String universalLicense)
    {
        this.mContext = context;
        this.mUniversalLicense = universalLicense;
    }

    @Override
    protected Void doInBackground(DeviceRegistrationManager... arg)
    {
        DeviceRegistrationManager p = arg[0];
        p.bind(mContext);

        p.registerDevice(mUniversalLicense);
        return null;
    }
}