package edu.hitsz.aircraftwar.android;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView status = new TextView(this);
        status.setText("Android Lab 1 环境骨架");
        status.setTextSize(22);
        status.setGravity(Gravity.CENTER);
        setContentView(status);
    }
}
