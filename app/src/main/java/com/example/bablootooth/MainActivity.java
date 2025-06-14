package com.example.bablootooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.S)
public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;

    private RecyclerView deviceRecyclerView;
    private DeviceAdapter deviceAdapter;
    private final List<String> deviceList = new ArrayList<>();

    private final ActivityResultLauncher<String[]> permissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            Boolean scanGranted = result.getOrDefault(Manifest.permission.BLUETOOTH_SCAN, false);
            Boolean locationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);

            if (Boolean.TRUE.equals(scanGranted) && Boolean.TRUE.equals(locationGranted)) {
                scanNearbyDevices(); // Retry after permissions are granted
            } else {
                Toast.makeText(this, "Required permissions denied", Toast.LENGTH_SHORT).show();
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Button connectButton = findViewById(R.id.btnConnect);
        Button scanButton = findViewById(R.id.btnScan);

        deviceRecyclerView = findViewById(R.id.deviceRecyclerView);
        deviceRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        deviceAdapter = new DeviceAdapter(deviceList);
        deviceRecyclerView.setAdapter(deviceAdapter);

        connectButton.setOnClickListener(view -> {
            if (bluetoothAdapter == null) {
                Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
                return;
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(new String[]{
                        Manifest.permission.BLUETOOTH_CONNECT}
                );
            } else {
                connectToBluetooth();
            }
        });

        scanButton.setOnClickListener(v -> {
            if (bluetoothAdapter == null) {
                Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
                return;
            }

            scanNearbyDevices();
        });
    }

    private void connectToBluetooth() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Bluetooth permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT}
            );
            return;
        }

        try {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(enableBtIntent);
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE)
                        != PackageManager.PERMISSION_GRANTED) {
                    permissionLauncher.launch(new String[]{
                            Manifest.permission.BLUETOOTH_ADVERTISE}
                    );
                } else {
                    makeDeviceDiscoverable();
                }
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Permission denied while accessing Bluetooth", Toast.LENGTH_SHORT).show();
        }
    }

    private void makeDeviceDiscoverable() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "BLUETOOTH_ADVERTISE permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
            Toast.makeText(this, "Bluetooth is ON. Ready to pair.", Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(this, "Permission denied: BLUETOOTH_ADVERTISE", Toast.LENGTH_SHORT).show();
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    @RequiresApi(api = Build.VERSION_CODES.S)
    private void scanNearbyDevices() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            permissionLauncher.launch(new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            });
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth is disabled. Turn it on first.", Toast.LENGTH_SHORT).show();
            return;
        }

// 🔽 ADD THIS CHECK BELOW
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Please enable location services (GPS)", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
            return;
        }

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        deviceList.clear();
        deviceAdapter.notifyDataSetChanged();

        // Register receiver only once
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device != null) {
                        String name = "Unknown";
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            try {
                                name = device.getName();
                            } catch (SecurityException e) {
                                // Handle gracefully
                                name = "Permission Denied";
                            }
                        }
                        String address = device.getAddress(); // No permission needed
                        String displayText = name + " (" + address + ")";
                        if (!deviceList.contains(displayText)) {
                            deviceList.add(displayText);
                            deviceAdapter.notifyItemInserted(deviceList.size() - 1);
                        }
                    }
                }
            }
        }, filter);

        boolean started = bluetoothAdapter.startDiscovery();
        if (started) {
            Toast.makeText(this, "Scanning for devices...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to start scanning", Toast.LENGTH_SHORT).show();
        }
    }
}