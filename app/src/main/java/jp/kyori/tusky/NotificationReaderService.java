package jp.kyori.tusky;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationManagerCompat;
import android.text.SpannableString;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

@TargetApi(22)
public class NotificationReaderService extends NotificationListenerService {
    private final String[] EXCLUDE_PACKAGE_NAMES = {"android", "com.android.systemui",
            "com.joaomgcd.autoinput", "net.dinglisch.android.taskerm",
            "com.mxtech.videoplayer.ad", "com.mxtech.videoplayer",
            "ch.pbos.android.SleepTimer", "com.sonymobile.smartcharger",
            "com.joaomgcd.autonotification"};

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        //Log.d("NotificationReader", "onNotificationPosted: " + (sbn.getNotification().extras.get(Notification.EXTRA_TITLE).equals("reload") ? "true" : "false"));
        if (sbn.getPackageName().equals("com.keylesspalace.tusky") && "reload".equals(sbn.getNotification().extras.get(Notification.EXTRA_TITLE))) {
            //Log.d("NotificationReader", "onNotificationPosted: Reload request received");
            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
            notificationManagerCompat.cancel(sbn.getTag(), sbn.getId());

            ArrayList<String[]> notificationList = new ArrayList<>();
            StatusBarNotification[] statusBarNotifications = getActiveNotifications();
            for (StatusBarNotification statusBarNotification : statusBarNotifications) {
                if (!statusBarNotification.isClearable() && !Arrays.asList(EXCLUDE_PACKAGE_NAMES).contains(statusBarNotification.getPackageName())) {
                    Notification notification = statusBarNotification.getNotification();
                    Bundle fang = notification.extras;
                    Bitmap icon = (Bitmap) fang.get(Notification.EXTRA_LARGE_ICON);
                    String iconStr = "";
                    if (icon != null) {
                        iconStr = convertBitmap2String(icon);
                    }
                    switch (statusBarNotification.getPackageName()) {
                        case "com.sauzask.nicoid": {
                            String[] item = {fang.getString(Notification.EXTRA_TITLE),
                                    "on #nicoid"};
                            notificationList.add(item);
                            break;
                        }
                        case "com.nanamusic.android": {
                            String title = "";
                            String text = "";
                            String subText = "";

                            Object cyclone = fang.get(Notification.EXTRA_TITLE);
                            if (cyclone instanceof String) {
                                title = (String) cyclone;
                            } else if (cyclone instanceof SpannableString) {
                                title = cyclone.toString();
                            }
                            Object joker = fang.get(Notification.EXTRA_TEXT);
                            if (joker instanceof String) {
                                text = (String) joker;
                            } else if (joker instanceof SpannableString) {
                                text = joker.toString();
                            }
                            Object heat = fang.get(Notification.EXTRA_SUB_TEXT);
                            if (heat instanceof String) {
                                subText = (String) heat;
                            } else if (heat instanceof SpannableString) {
                                subText = heat.toString();
                            }

                            String[] item = {title, "by " + text + " covered by " + subText, iconStr};
                            notificationList.add(item);

                            break;
                        }
                        default: {
                            String title = "";
                            String text = "";

                            Object cyclone = fang.get(Notification.EXTRA_TITLE);
                            if (cyclone instanceof String) {
                                title = (String) cyclone;
                            } else if (cyclone instanceof SpannableString) {
                                title = cyclone.toString();
                            }
                            Object joker = fang.get(Notification.EXTRA_TEXT);
                            if (joker instanceof String) {
                                text = (String) joker;
                            } else if (joker instanceof SpannableString) {
                                text = joker.toString();
                            }

                            String[] item = {title, "by " + text, iconStr};
                            notificationList.add(item);
                        }
                    }
                }
            }

            Intent intent = new Intent();
            intent.putExtra("list", notificationList);
            intent.setAction("NOTIFICATION_LIST");
            sendBroadcast(intent);
        }
    }

    private String convertBitmap2String(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] bytes = stream.toByteArray();
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }
}
