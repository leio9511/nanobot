package com.google.clawminium.godconsole;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

public class MainActivity extends AppCompatActivity {

    private ConstraintLayout mainLayout;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainLayout = findViewById(R.id.main_layout);
        statusText = findViewById(R.id.status_text);

        Button btnSave = findViewById(R.id.btn_save);
        Button btnDestroy = findViewById(R.id.btn_destroy);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveWorld();
            }
        });

        btnDestroy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                destroyWorld();
            }
        });

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();
        if ("com.clawminium.intent.action.SAVE_WORLD".equals(action)) {
            saveWorld();
        } else if ("com.clawminium.intent.action.DESTROY_WORLD".equals(action)) {
            destroyWorld();
        }
    }

    private void saveWorld() {
        mainLayout.setBackgroundColor(Color.GREEN);
        statusText.setText("Thank you for saving the world!");
        statusText.setTextColor(Color.BLACK);
    }

    private void destroyWorld() {
        mainLayout.setBackgroundColor(Color.RED);
        statusText.setText("You just destroyed the world!");
        statusText.setTextColor(Color.WHITE);
    }
}
