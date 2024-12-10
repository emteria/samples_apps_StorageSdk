package com.emteria.sample.sdk.storage.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.emteria.storage.contract.managers.DeviceRegistrationManager;

public class RegistrationDetailsTask extends AsyncTask<DeviceRegistrationManager, Void, Void>
{
    Context mContext;

    public RegistrationDetailsTask(Context mContext)
    {
        this.mContext = mContext;
    }

    @Override
    protected Void doInBackground(DeviceRegistrationManager... deviceRegistrationManagers)
    {
        DeviceRegistrationManager p = deviceRegistrationManagers[0];
        p.bindToDeviceManagement(mContext);
        p.getRegistrationDetails(false);

        return null;
    }
}
