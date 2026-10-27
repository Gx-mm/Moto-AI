package com.gxmm.motoai;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(32, 32, 32, 32);
        layout.setBackgroundColor(Color.rgb(15, 18, 28));

        TextView title = new TextView(this);
        title.setText("MOTO AI");
        title.setTextSize(32);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView info = new TextView(this);
        info.setText("Voice assistant setup\n\nThis starter app does not listen for a wake word yet.\nNext: configure the on-device wake-word engine.");
        info.setTextSize(16);
        info.setTextColor(Color.LTGRAY);
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, 24, 0, 24);
        layout.addView(info);

        Button button = new Button(this);
        button.setText("Setup instructions");
        button.setOnClickListener(v -> info.setText("Next step: create a Picovoice AccessKey and a custom 'Hello Moto' keyword model. Keep your AccessKey private; do not commit it to GitHub."));
        layout.addView(button);
        setContentView(layout);
    }
}
