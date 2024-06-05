package com.asterinet.react.bgactions;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.jstasks.HeadlessJsTaskConfig;

final public class RNBackgroundActionsTask extends HeadlessJsTaskService {

    public static final int SERVICE_NOTIFICATION_ID = 92901;
    private static final String CHANNEL_ID = "RN_BACKGROUND_ACTIONS_CHANNEL";

    @SuppressLint("UnspecifiedImmutableFlag")
    @NonNull
    public static Notification buildNotification(@NonNull Context context, @NonNull final BackgroundTaskOptions bgOptions) {
        // Get info
        final String taskTitle = bgOptions.getTaskTitle();
        final String taskDesc = bgOptions.getTaskDesc();
        final int iconInt = bgOptions.getIconInt();
        final int color = bgOptions.getColor();
        final String linkingURI = bgOptions.getLinkingURI();
        Intent notificationIntent;
        if (linkingURI != null) {
            notificationIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(linkingURI));
        } else {
            //as RN works on single activity architecture - we don't need to find current activity on behalf of react context
            notificationIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        }
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        
        
        try {
            // 리플렉션을 사용하여 FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT 존재 여부 확인
            Field flagField = PendingIntent.class.getField("FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT");
            flags |= (Integer) flagField.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flags |= PendingIntent.FLAG_MUTABLE;
            }
        }

        final PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, flags);
        
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(taskTitle)
                .setContentText(taskDesc)
                .setSmallIcon(iconInt)
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setColor(color);

        final Bundle progressBarBundle = bgOptions.getProgressBar();
        if (progressBarBundle != null) {
            final int progressMax = (int) Math.floor(progressBarBundle.getDouble("max"));
            final int progressCurrent = (int) Math.floor(progressBarBundle.getDouble("value"));
            final boolean progressIndeterminate = progressBarBundle.getBoolean("indeterminate");
            builder.setProgress(progressMax, progressCurrent, progressIndeterminate);
        }
        return builder.build();
    }

    @Override
    protected @Nullable
    HeadlessJsTaskConfig getTaskConfig(Intent intent) {
        final Bundle extras = intent.getExtras();
        if (extras != null) {
            return new HeadlessJsTaskConfig(extras.getString("taskName"), Arguments.fromBundle(extras), 0, true);
        }
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final Bundle extras = intent.getExtras();
        if (extras == null) {
            throw new IllegalArgumentException("Extras cannot be null");
        }
        final BackgroundTaskOptions bgOptions = new BackgroundTaskOptions(extras);
        createNotificationChannel(bgOptions.getTaskTitle(), bgOptions.getTaskDesc()); // Necessary creating channel for API 26+
        // Create the notification
        final Notification notification = buildNotification(this, bgOptions);

        startForeground(SERVICE_NOTIFICATION_ID, notification);
        return super.onStartCommand(intent, flags, startId);
    }

    private void createNotificationChannel(@NonNull final String taskTitle, @NonNull final String taskDesc) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final int importance = NotificationManager.IMPORTANCE_LOW;
            final NotificationChannel channel = new NotificationChannel(CHANNEL_ID, taskTitle, importance);
            channel.setDescription(taskDesc);
            final NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
