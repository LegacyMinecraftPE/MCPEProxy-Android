package net.eqozqq.mcpeproxy;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.content.pm.ServiceInfo;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;

public class ProxyService extends Service {

    public static final String ACTION_START = "net.eqozqq.mcpeproxy.ACTION_START";
    public static final String ACTION_STOP = "net.eqozqq.mcpeproxy.ACTION_STOP";
    public static final String ACTION_STOP_FROM_NOTIFICATION = "net.eqozqq.mcpeproxy.ACTION_STOP_FROM_NOTIFICATION";
    public static final String ACTION_PROXY_STOPPED = "net.eqozqq.mcpeproxy.ACTION_PROXY_STOPPED";
    public static final String ACTION_CHECK_STATUS = "net.eqozqq.mcpeproxy.ACTION_CHECK_STATUS";
    public static final String ACTION_STATUS_RESPONSE = "net.eqozqq.mcpeproxy.ACTION_STATUS_RESPONSE";
    public static final String EXTRA_IS_RUNNING = "is_running";

    public static final String EXTRA_ADDRESS = "address";
    public static final String EXTRA_PORT = "port";

    private static final String CHANNEL_ID = "MCPEProxyChannel";
    private static final int NOTIFICATION_ID = 1;

    private UdpProxy udpProxy;
    private Thread proxyThread;
    private PowerManager.WakeLock wakeLock;
    private static boolean isServiceRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        acquireWakeLock();
    }

    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MCPEProxy::ProxyWakeLock");
            wakeLock.acquire();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();

            if (ACTION_START.equals(action)) {
                String address = intent.getStringExtra(EXTRA_ADDRESS);
                int port = intent.getIntExtra(EXTRA_PORT, 19132);
                startProxy(address, port);
            } else if (ACTION_STOP.equals(action) || ACTION_STOP_FROM_NOTIFICATION.equals(action)) {
                stopProxy();
                stopForegroundCompat();
                isServiceRunning = false;
                stopSelf();

                Intent broadcastIntent = new Intent(ACTION_PROXY_STOPPED);
                sendBroadcast(broadcastIntent);
            } else if (ACTION_CHECK_STATUS.equals(action)) {
                Intent responseIntent = new Intent(ACTION_STATUS_RESPONSE);
                responseIntent.putExtra(EXTRA_IS_RUNNING, isServiceRunning);
                sendBroadcast(responseIntent);
            }
        }

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(getString(R.string.channel_description));

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                flags);

        Intent stopIntent = new Intent(this, ProxyService.class);
        stopIntent.setAction(ACTION_STOP_FROM_NOTIFICATION);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this,
                0,
                stopIntent,
                flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setSmallIcon(R.drawable.device_hub_48px)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            builder.addAction(android.R.drawable.ic_delete,
                    getString(R.string.notification_stop),
                    stopPendingIntent);
        }

        return builder.build();
    }

    private void startProxy(String address, int port) {
        if (udpProxy != null && udpProxy.isRunning()) {
            return;
        }

        startForegroundCompat();
        isServiceRunning = true;

        udpProxy = new UdpProxy(address, port);
        proxyThread = new Thread(new Runnable() {
            @Override
            public void run() {
                udpProxy.run();
            }
        });
        proxyThread.start();
    }

    private void stopProxy() {
        if (udpProxy != null) {
            udpProxy.stop();
            udpProxy = null;
        }
        if (proxyThread != null) {
            try {
                proxyThread.join(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            proxyThread = null;
        }
    }

    private void startForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, createNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(NOTIFICATION_ID, createNotification());
        } else {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(NOTIFICATION_ID, createNotification());
            }
        }
    }

    private void stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
            stopForeground(true);
        } else {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.cancel(NOTIFICATION_ID);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopProxy();
        isServiceRunning = false;
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        super.onDestroy();
    }

    public static boolean isRunning() {
        return isServiceRunning;
    }
}