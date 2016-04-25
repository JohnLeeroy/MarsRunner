package com.jli.marsrun.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.jli.marsrun.R;

/**
 * Created by john on 4/24/16.
 */
public class ResultActivity extends AppCompatActivity {

    private static final float EARTH_TO_MARS_GRAVITY_RATIO = .38f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        bindAndInitUi();
    }

    void bindAndInitUi() {
        //Get data from bundle
        Bundle args = getIntent().getExtras();
        String distance = args.getString("distance");
        String calories = args.getString("calories");
        String pace = args.getString("pace");

        //Set Earth TextViews
        TextView earthDistance = (TextView) findViewById(R.id.earth_distance);
        earthDistance.setText(distance);
        TextView earthCalories = (TextView) findViewById(R.id.earth_calories);
        earthCalories.setText(calories);
        TextView earthPace = (TextView) findViewById(R.id.earth_pace);
        earthPace.setText(pace);

        //Set Mars TextViews
        TextView marsDistance = (TextView) findViewById(R.id.mars_distance);
        double mDist = Double.valueOf(distance);
        mDist = mDist/EARTH_TO_MARS_GRAVITY_RATIO;
        marsDistance.setText(String.format("%.2f", mDist));

        TextView marsCalories = (TextView) findViewById(R.id.mars_calories);
        marsCalories.setText(calories);

        TextView marsPace = (TextView) findViewById(R.id.mars_pace);
        double mPace = Double.valueOf(pace);
        mPace = mPace/EARTH_TO_MARS_GRAVITY_RATIO;
        marsPace.setText(String.format("%.2f", mPace));

        final Button finishBtn = (Button) findViewById(R.id.finish_btn);
        finishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ResultActivity.this, MapsActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
