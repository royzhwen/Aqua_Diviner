package com.example.owner.wellcalculator.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;
import com.example.owner.wellcalculator.R;
import com.example.owner.wellcalculator.backend.Calculator;

/**
 * Created by Zhao Heng Wen on 2017-03-13.
 * This class is used for the splash screen after the user press Calculate on the main page
 */

public class MainPage_CalculateScreen extends Activity {
    private int tsolve;
    private Context context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainpage_splashscreen);

        //Grab the tsolve value from MainPage
        Intent intent = this.getIntent();
        this.tsolve = intent.getIntExtra("tsolve", 10);
        this.context = this;

        //This allows animated GIFs to be displayed
        ImageView imageView = (ImageView) findViewById(R.id.loading_anim);
        GlideDrawableImageViewTarget imageViewTarget = new GlideDrawableImageViewTarget(imageView);
        Glide.with(this).load(R.drawable.loading_edited).into(imageViewTarget);

        startHeavyProcessing();
    }

    private void startHeavyProcessing(){
        new LongOperation().execute("");
    }

    private class LongOperation extends AsyncTask<String, Void, String> {

        @Override
        //Perform calculation here and pass any error data back to MainPage
        protected String doInBackground(String... params) {
            Intent intent = new Intent();
            try {
                long startTime = System.currentTimeMillis();

                //Call Calculator class to do calculations
                Calculator calculator = new Calculator();
                int returnCode = calculator.Well_Full_solution(context, tsolve);

                //For debugging only
                long elapsedTime = System.currentTimeMillis() - startTime;
                System.out.println("Elapsed time " + elapsedTime);

                //Toast pop-ups cannot be made within doInBackground so we pass out error strings to new Intent instead
                //TODO: The error messages passed back to MainPage were left hard-coded for now due to parsing troubles
                if (returnCode == 0) {
                    setResult(RESULT_OK, intent);
                } else if (returnCode == -1) {
                    intent.putExtra("error", "Please properly specify and save all the inputs first");
                    setResult(RESULT_CANCELED, intent);
                } else if (returnCode == -2){
                    intent.putExtra("error", "Calculaton failed due to internal error");
                    setResult(RESULT_CANCELED, intent);
                }
            } catch (Exception e) {
                intent.putExtra("error", "Calculaton failed due to error: " + e.getMessage());
                setResult(RESULT_CANCELED, intent);
            }
            return "dummy";
        }

        @Override
        protected void onPostExecute(String result) {
            finish();
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }
}
