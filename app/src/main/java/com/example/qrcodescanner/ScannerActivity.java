package com.example.qrcodescanner;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class ScannerActivity extends AppCompatActivity {

    private static final String TAG = "QRCodeScanner";
    private TextView tvResult;
    private CameraManager cameraManager;
    private String cameraId;
    private boolean isFlashOn = false;
    private ImageView btnFlashlight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvResult = findViewById(R.id.tvResult);
        Button btnScan = findViewById(R.id.btnScan);
        btnFlashlight = findViewById(R.id.btnFlashlight);

        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            cameraId = getCameraId();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFlashlight();
                // Initialize the scanner
                IntentIntegrator integrator = new IntentIntegrator(ScannerActivity.this);
                integrator.setPrompt("Scan a QR Code");
                integrator.setOrientationLocked(false);
                integrator.initiateScan();
            }
        });

        btnFlashlight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFlashlight();
            }
        });
    }

    private String getCameraId() throws CameraAccessException {
        String[] cameraIds = cameraManager.getCameraIdList();
        for (String id : cameraIds) {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
            Boolean flashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            if (flashAvailable != null && flashAvailable) {
                return id;
            }
        }
        return null;
    }

    private void toggleFlashlight() {
        try {
            if (cameraId != null) {
                isFlashOn = !isFlashOn;
                cameraManager.setTorchMode(cameraId, isFlashOn);
                updateFlashlightIcon();
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updateFlashlightIcon() {
        if (isFlashOn) {
            btnFlashlight.setImageResource(R.drawable.baseline_flash_on_24);
        } else {
            btnFlashlight.setImageResource(R.drawable.baseline_flash_off_24);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check and request the CAMERA permission if not granted
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, 0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Turn off the flashlight when the activity is destroyed
        if (isFlashOn) {
            toggleFlashlight();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Receive scanning result from ZXing scanner
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                // Scanning canceled or no QR code found
                Toast.makeText(this, "Scan canceled", Toast.LENGTH_SHORT).show();
            } else {
                // QR code scanned successfully
                String qrCodeData = result.getContents();
                tvResult.setText(qrCodeData);

                // Check if the scanned data is a valid URL and open it in a web browser
                if (isUrlValid(qrCodeData)) {
                    openUrlInBrowser(qrCodeData);
                }
            }
        }
    }

    private boolean isUrlValid(String url) {
        // Simple URL validation logic (you may want to add more checks)
        return url != null && (url.startsWith("http://") || url.startsWith("https://"));
    }

    private void openUrlInBrowser(String url) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No web browser installed", Toast.LENGTH_SHORT).show();
        }
    }
}
