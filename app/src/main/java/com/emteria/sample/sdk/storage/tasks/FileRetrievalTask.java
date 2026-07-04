package com.emteria.sample.sdk.storage.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.emteria.storage.contract.managers.FileDownloadManager;

public class FileRetrievalTask extends AsyncTask<FileDownloadManager, Void, Void>
{
    private final Context mContext;
    private final String mStorageFileId;

    public FileRetrievalTask(Context context, String storageFileId)
    {
        mContext = context;
        mStorageFileId = storageFileId;
    }

    @Override
    protected Void doInBackground(FileDownloadManager... managers)
    {
        FileDownloadManager manager = managers[0];
        if (!manager.serviceIsBound())
        {
            manager.bindToAppManagement(mContext);
        }

        manager.requestFile(mStorageFileId);
        return null;
    }
}
