package com.example.mockgps;


import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class TaskActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.task_activity);

        TextView taskId = findViewById(R.id.task_id);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
