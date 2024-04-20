package com.example.cameraxqr;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import com.example.cameraxqr.databinding.ActivityCameraBinding;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class CameraActivity extends AppCompatActivity {

    ActivityCameraBinding binding;
    ImageCapture imageCapture;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCameraBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (allPermissionGranted()) {
            startCamera();
        }
        else {
            askPermission();
        }

        binding.TakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });
    }

    private void takePhoto() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
        try {
            File file = File.createTempFile(dateFormat.format(new Date()), ".jpg", getDirectoryName());
            ImageCapture.OutputFileOptions outputFileOptions =
                    new ImageCapture.OutputFileOptions.Builder(file).build();
            imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this),
                    new ImageCapture.OnImageSavedCallback() {
                        @Override
                        public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                            Toast.makeText(CameraActivity.this, "Saved OK", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            Toast.makeText(CameraActivity.this, "Saved BAD", Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private File getDirectoryName() {
        //return getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return Environment.getExternalStoragePublicDirectory("DCIM/Camera");
    }

    public void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .build();
        preview.setSurfaceProvider(binding.PV.getSurfaceProvider());

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        imageCapture = new ImageCapture.Builder()
                .build();
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new QRAnalyser(new QRCodeListener() {
            @Override
            public void onQRFound(String qrcode) {
                Intent intent = new Intent();
                intent.putExtra("QRINFO", qrcode);
                setResult(RESULT_OK, intent);
                finish();
            }

            @Override
            public void onQRNotFound() {

            }
        }));
        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis);
    }

    String[] PERMISSIONS = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
    private ActivityResultLauncher<String[]> requestLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                AtomicBoolean permissionGranted = new AtomicBoolean(true); // может быть только в одном состоянии (при многопоточности нужно)
                permissions.forEach((key, value) -> { // foreach исп-ет принцип конкурентности? короче использует потоки для прохода
                    if (Arrays.asList(PERMISSIONS).contains(key) && !value){ // из-за этого нужен atomicboolean
                        Toast.makeText(this,key+" "+value,Toast.LENGTH_SHORT).show();
                        permissionGranted.set(false);
                    }
                });
                if(!permissionGranted.get()){
                    Toast.makeText(this,"Permission request denied",Toast.LENGTH_SHORT).show();
                } else {
                    startCamera();
                }
            });

    public boolean allPermissionGranted() {
        AtomicBoolean good = new AtomicBoolean(true);
        Arrays.asList(PERMISSIONS).forEach(it -> {
            if (ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_DENIED) {
                good.set(false);
            }
        });
        return good.get();
    }

    public void askPermission() {
        requestLauncher.launch(PERMISSIONS);
    }
}