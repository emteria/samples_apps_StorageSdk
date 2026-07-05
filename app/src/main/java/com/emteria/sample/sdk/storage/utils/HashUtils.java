package com.emteria.sample.sdk.storage.utils;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

public class HashUtils
{
    public static String computeMd5(File file)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            try (FileInputStream input = new FileInputStream(file))
            {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1)
                {
                    digest.update(buffer, 0, read);
                }
            }

            StringBuilder builder = new StringBuilder();
            for (byte b : digest.digest())
            {
                builder.append(String.format("%02x", b));
            }

            return builder.toString();
        }
        catch (Exception e)
        {
            return "";
        }
    }
}
