package com.emteria.sample.sdk.storage.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.emteria.storage.contract.managers.FileUploadManager;

import java.io.File;
import java.io.FileNotFoundException;

public class FileUploadTask extends AsyncTask<Void, Void, Void>
{
    private static final String TAG = FileUploadTask.class.getSimpleName();

    private final Context mContext;
    private final FileUploadManager mUploadManager;
    private final String mFilePath;

    public FileUploadTask(Context context, FileUploadManager uploadManager, String filePath)
    {
        super();

        mContext = context;
        mUploadManager = uploadManager;
        mFilePath = filePath;
    }

    @Override
    protected Void doInBackground(Void... v)
    {
        try
        {
            File file = new File(mFilePath);
            String filename = file.getName();
            Log.i(TAG, "Sending file " + filename + " from " + file.getAbsolutePath());

            boolean exists = file.exists();
            boolean readable = file.canRead();
            boolean writable = file.canWrite();
            Log.i(TAG, "File exists: " + exists + ", readable: " + readable + ", writeable: " + writable);

            if (!mUploadManager.serviceIsBound())
            {
                mUploadManager.bindToAppManagement(mContext);
            }

            ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            mUploadManager.uploadFile(filename, pfd);
        }
        catch (FileNotFoundException e)
        {
            String message = e.getMessage();
            mUploadManager.onUploadError(message);
        }

        return null;
    }
}
