package com.jli.marsrun.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Doesn't do anything right now other than redirects to MapsActivity.
        // It will be expanded upon in the future.
        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
        finish();
    }
}
