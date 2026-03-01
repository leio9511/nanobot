package com.google.clawminium.kernel;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AlertActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        String message = getIntent().getStringExtra("message");
        if (message == null) message = "Security Alert";

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.RED);
        layout.setPadding(60, 60, 60, 60);
        layout.setGravity(Gravity.CENTER);

        TextView text = new TextView(this);
        text.setText(message);
        text.setTextColor(Color.WHITE);
        text.setTextSize(24);
        text.setGravity(Gravity.CENTER);

        layout.addView(text);
        setContentView(layout);
        
        // Auto-close after 5 seconds
        new android.os.Handler().postDelayed(() -> finish(), 5000);
    }
}
