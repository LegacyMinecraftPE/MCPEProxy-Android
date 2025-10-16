package net.eqozqq.mcpeproxy;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "MCPEProxyPrefs";
    private static final String PREF_ADDRESS = "address";
    private static final String PREF_PORT = "port";
    private static final String PREF_RUNNING = "running";
    private static final int NOTIFICATION_PERMISSION_CODE = 100;

    private EditText editTextAddress;
    private EditText editTextPort;
    private Button buttonToggleProxy;
    private TextView textViewStatus;

    private boolean proxyRunning = false;
    private SharedPreferences prefs;

    private BroadcastReceiver proxyStoppedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            proxyRunning = false;
            updateUI();
            saveState();
        }
    };

    private BroadcastReceiver statusResponseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isRunning = intent.getBooleanExtra(ProxyService.EXTRA_IS_RUNNING, false);
            proxyRunning = isRunning;
            updateUI();
            saveState();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_Holo_Light);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextAddress = findViewById(R.id.editTextAddress);
        editTextPort = findViewById(R.id.editTextPort);
        buttonToggleProxy = findViewById(R.id.buttonToggleProxy);
        textViewStatus = findViewById(R.id.textViewStatus);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        loadState();
        requestNotificationPermission();

        buttonToggleProxy.setOnClickListener(v -> toggleProxy());

        registerReceiverCompat();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkServiceStatus();
    }

    private void checkServiceStatus() {
        proxyRunning = ProxyService.isRunning();
        updateUI();
        saveState();
    }

    private void registerReceiverCompat() {
        IntentFilter stoppedFilter = new IntentFilter(ProxyService.ACTION_PROXY_STOPPED);
        IntentFilter statusFilter = new IntentFilter(ProxyService.ACTION_STATUS_RESPONSE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(proxyStoppedReceiver, stoppedFilter, Context.RECEIVER_NOT_EXPORTED);
            registerReceiver(statusResponseReceiver, statusFilter, Context.RECEIVER_NOT_EXPORTED);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(proxyStoppedReceiver, stoppedFilter, Context.RECEIVER_NOT_EXPORTED);
            registerReceiver(statusResponseReceiver, statusFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(proxyStoppedReceiver, stoppedFilter);
            registerReceiver(statusResponseReceiver, statusFilter);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    private void loadState() {
        String savedAddress = prefs.getString(PREF_ADDRESS, "");
        int savedPort = prefs.getInt(PREF_PORT, 19132);
        proxyRunning = prefs.getBoolean(PREF_RUNNING, false);

        editTextAddress.setText(savedAddress);
        editTextPort.setText(String.valueOf(savedPort));

        updateUI();
    }

    private void saveState() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_ADDRESS, editTextAddress.getText().toString().trim());
        try {
            editor.putInt(PREF_PORT, Integer.parseInt(editTextPort.getText().toString().trim()));
        } catch (NumberFormatException e) {
            editor.putInt(PREF_PORT, 19132);
        }
        editor.putBoolean(PREF_RUNNING, proxyRunning);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            editor.apply();
        } else {
            editor.commit();
        }
    }

    private void updateUI() {
        if (proxyRunning) {
            buttonToggleProxy.setText(R.string.disable_proxy);
            textViewStatus.setText(R.string.status_running);
            editTextAddress.setEnabled(false);
            editTextPort.setEnabled(false);
        } else {
            buttonToggleProxy.setText(R.string.enable_proxy);
            textViewStatus.setText(R.string.status_stopped);
            editTextAddress.setEnabled(true);
            editTextPort.setEnabled(true);
        }
    }

    private void toggleProxy() {
        if (proxyRunning) {
            stopProxy();
        } else {
            startProxy();
        }
    }

    private void startProxy() {
        String address = editTextAddress.getText().toString().trim();
        String portStr = editTextPort.getText().toString().trim();

        if (address.isEmpty()) {
            Toast.makeText(this, R.string.error_invalid_address, Toast.LENGTH_SHORT).show();
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
            if (port < 1 || port > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.error_invalid_port, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent serviceIntent = new Intent(this, ProxyService.class);
        serviceIntent.setAction(ProxyService.ACTION_START);
        serviceIntent.putExtra(ProxyService.EXTRA_ADDRESS, address);
        serviceIntent.putExtra(ProxyService.EXTRA_PORT, port);

        startServiceCompat(serviceIntent);

        proxyRunning = true;
        updateUI();
        saveState();
    }

    private void stopProxy() {
        Intent serviceIntent = new Intent(this, ProxyService.class);
        serviceIntent.setAction(ProxyService.ACTION_STOP);
        startService(serviceIntent);

        proxyRunning = false;
        updateUI();
        saveState();
    }

    private void startServiceCompat(Intent serviceIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(proxyStoppedReceiver);
            unregisterReceiver(statusResponseReceiver);
        } catch (IllegalArgumentException e) {
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveState();
    }
}