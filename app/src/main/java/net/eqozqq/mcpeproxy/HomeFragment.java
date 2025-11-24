package net.eqozqq.mcpeproxy;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class HomeFragment extends Fragment {

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
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        editTextAddress = view.findViewById(R.id.editTextAddress);
        editTextPort = view.findViewById(R.id.editTextPort);
        buttonToggleProxy = view.findViewById(R.id.buttonToggleProxy);
        textViewStatus = view.findViewById(R.id.textViewStatus);

        prefs = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        loadState();
        requestNotificationPermission();

        buttonToggleProxy.setOnClickListener(v -> toggleProxy());

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        registerReceiverCompat();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        try {
            getActivity().unregisterReceiver(proxyStoppedReceiver);
            getActivity().unregisterReceiver(statusResponseReceiver);
        } catch (IllegalArgumentException e) {
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkServiceStatus();
    }

    private void checkServiceStatus() {
        Intent statusIntent = new Intent(getActivity(), ProxyService.class);
        statusIntent.setAction(ProxyService.ACTION_CHECK_STATUS);
        getActivity().startService(statusIntent);
    }

    private void registerReceiverCompat() {
        IntentFilter stoppedFilter = new IntentFilter(ProxyService.ACTION_PROXY_STOPPED);
        IntentFilter statusFilter = new IntentFilter(ProxyService.ACTION_STATUS_RESPONSE);
        Context context = getActivity();

        if (context != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(proxyStoppedReceiver, stoppedFilter, Context.RECEIVER_NOT_EXPORTED);
                context.registerReceiver(statusResponseReceiver, statusFilter, Context.RECEIVER_NOT_EXPORTED);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.registerReceiver(proxyStoppedReceiver, stoppedFilter, Context.RECEIVER_NOT_EXPORTED);
                context.registerReceiver(statusResponseReceiver, statusFilter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                context.registerReceiver(proxyStoppedReceiver, stoppedFilter);
                context.registerReceiver(statusResponseReceiver, statusFilter);
            }
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(),
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
        if (prefs == null) return;
        SharedPreferences.Editor editor = prefs.edit();
        if (editTextAddress != null) {
            editor.putString(PREF_ADDRESS, editTextAddress.getText().toString().trim());
        }
        if (editTextPort != null) {
            try {
                editor.putInt(PREF_PORT, Integer.parseInt(editTextPort.getText().toString().trim()));
            } catch (NumberFormatException e) {
                editor.putInt(PREF_PORT, 19132);
            }
        }
        editor.putBoolean(PREF_RUNNING, proxyRunning);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            editor.apply();
        } else {
            editor.commit();
        }
    }

    public void updateUI() {
        if (getView() == null) return;

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
            startProxyFromUI();
        }
    }

    private void startProxyFromUI() {
        String address = editTextAddress.getText().toString().trim();
        String portStr = editTextPort.getText().toString().trim();

        if (address.isEmpty()) {
            Toast.makeText(getActivity(), R.string.error_invalid_address, Toast.LENGTH_SHORT).show();
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
            if (port < 1 || port > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(getActivity(), R.string.error_invalid_port, Toast.LENGTH_SHORT).show();
            return;
        }

        startProxy(address, port);
    }

    public void startProxy(String address, int port) {
        Intent serviceIntent = new Intent(getActivity(), ProxyService.class);
        serviceIntent.setAction(ProxyService.ACTION_START);
        serviceIntent.putExtra(ProxyService.EXTRA_ADDRESS, address);
        serviceIntent.putExtra(ProxyService.EXTRA_PORT, port);

        startServiceCompat(serviceIntent);

        proxyRunning = true;
        updateUI();
        saveState();
    }

    private void stopProxy() {
        Intent serviceIntent = new Intent(getActivity(), ProxyService.class);
        serviceIntent.setAction(ProxyService.ACTION_STOP);
        getActivity().startService(serviceIntent);

        proxyRunning = false;
        updateUI();
        saveState();
    }

    private void startServiceCompat(Intent serviceIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getActivity().startForegroundService(serviceIntent);
        } else {
            getActivity().startService(serviceIntent);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        saveState();
    }
}