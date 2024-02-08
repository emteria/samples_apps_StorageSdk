package com.emteria.sample.sdk.storage;

import android.content.Context;
import android.os.AsyncTask;

import com.emteria.storage.contract.managers.DeviceRegistrationManager;

public class DeviceStatusTask extends AsyncTask<DeviceRegistrationManager, Void, Void>
{
    Context mContext;

    public DeviceStatusTask(Context mContext)
    {
        this.mContext = mContext;
    }

    @Override
    protected Void doInBackground(DeviceRegistrationManager... deviceRegistrationManagers)
    {
        DeviceRegistrationManager p = deviceRegistrationManagers[0];
        p.bindToDeviceManagement(mContext);
        p.isDeviceRegistered();

        return null;
    }
}
