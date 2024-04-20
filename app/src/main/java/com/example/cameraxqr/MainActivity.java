package com.example.cameraxqr;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.cameraxqr.databinding.ActivityMainBinding;


/*
RF8KC2EQTVL
com.example.cameraxqr
adb -s RF8KC2EQTVL shell am start -W -a android.intent.action.VIEW -d "https://www.youtube.com/main2" com.example.cameraxqr
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.ToCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                intentLauncher.launch(intent);
            }
        });
    }

    ActivityResultLauncher<Intent> intentLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if (result.getResultCode() == RESULT_OK) {
                                String qrinfo = result.getData().getStringExtra("QRINFO");
                                Toast.makeText(MainActivity.this, qrinfo, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
}