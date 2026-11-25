package com.example.loginapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_about);
        ScreenInsets.apply(this);
        findViewById(R.id.aboutBanner).setClipToOutline(true);
        findViewById(R.id.btnAboutBack).setOnClickListener(v -> finish());
    }
}
