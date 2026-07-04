package com.emteria.sample.sdk.storage.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.emteria.storage.contract.managers.FileListManager;

public class FileListingTask extends AsyncTask<FileListManager, Void, Void>
{
    private final Context mContext;

    public FileListingTask(Context context)
    {
        mContext = context;
    }

    @Override
    protected Void doInBackground(FileListManager... managers)
    {
        FileListManager manager = managers[0];
        if (!manager.serviceIsBound())
        {
            manager.bindToAppManagement(mContext);
        }

        manager.listFiles();
        return null;
    }
}
