package com.emteria.sample.sdk.storage;

import android.content.Context;
import android.os.AsyncTask;

import com.emteria.storage.contract.managers.DeviceRegistrationManager;

public class DeviceRegistrationTask extends AsyncTask<DeviceRegistrationManager, Void, Void>
{
    private final Context mContext;
    private final String mUniversalLicense;

    public DeviceRegistrationTask(Context context, String universalLicense)
    {
        mContext = context;
        mUniversalLicense = universalLicense;
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
