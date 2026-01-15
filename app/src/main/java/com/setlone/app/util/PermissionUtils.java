package com.setlone.app.util;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.content.ContextCompat;

import com.setlone.app.widget.PermissionRationaleDialog;

public class PermissionUtils
{
    public static boolean requestPostNotificationsPermission(
        Activity activity,
        ActivityResultLauncher<String> requestPermissionLauncher
    )
    {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED)
            {
                // FCM SDK (and your app) can post notifications.
                return true;
            }
            else if (activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS))
            {
                PermissionRationaleDialog.show(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS,
                    ok -> requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS),
                    cancel -> {
                    }
                );
                return false;
            }
            else
            {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                return false;
            }
        }
        else
        {
            return true;
        }
    }

    /**
     * Get the appropriate storage permission based on Android version
     * Android 13+ (API 33+): READ_MEDIA_IMAGES
     * Android 12 and below: READ_EXTERNAL_STORAGE
     */
    public static String getStoragePermission()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            return Manifest.permission.READ_MEDIA_IMAGES;
        }
        else
        {
            return Manifest.permission.READ_EXTERNAL_STORAGE;
        }
    }

    /**
     * Check if storage permission is granted
     */
    public static boolean hasStoragePermission(Activity activity)
    {
        String permission = getStoragePermission();
        return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED;
    }
}
