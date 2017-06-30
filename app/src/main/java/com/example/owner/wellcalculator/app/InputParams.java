package com.example.owner.wellcalculator.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.owner.wellcalculator.R;

/**
 * Created by Zhao Heng Wen on 2017-02-05.
 * This class is used for the Input Parameters page (accessed from Input Parameters button on main page)
 */
//TODO: Remove the hard-coded prints and limits - low priority
public class InputParams extends AppCompatActivity {


    private EditText edit_TVD;
    private EditText edit_SurfaceCasing;
    private EditText edit_IntermediateCasing;
    private EditText edit_SlottedLiner;
    //private EditText edit_WellLength;

    private TextView TVD;
    private TextView SurfaceCasing;
    private TextView IntermediateCasing;
    private TextView SlottedLiner;
    private TextView WellLength;

    private InputParamsDao inputParamsDao;
    private InputParamsData[] inputParams;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inputparams);

        TVD = (TextView) findViewById(R.id.display_TVD);
        SurfaceCasing = (TextView) findViewById(R.id.display_SurfaceCasing);
        IntermediateCasing = (TextView) findViewById(R.id.display_IntermediateCasing);
        SlottedLiner = (TextView) findViewById(R.id.display_SlottedLiner);
        WellLength = (TextView) findViewById(R.id.display_WellLength);

        edit_TVD = (EditText) findViewById(R.id.input_TVD);
        edit_SurfaceCasing = (EditText) findViewById(R.id.input_SurfaceCasing);
        edit_IntermediateCasing = (EditText) findViewById(R.id.input_IntermediateCasing);
        edit_SlottedLiner = (EditText) findViewById(R.id.input_SlottedLiner);
        //edit_WellLength = (EditText) findViewById(R.id.input_WellLength);

        inputParamsDao = new InputParamsDao(this);
        inputParams = inputParamsDao.readInputs();

        if (inputParams == null) {
            inputParams = new InputParamsData[5];
            inputParams[0] = new InputParamsData("TVD", 0.0f, 300f, 500f, "m", false);
            inputParams[1] = new InputParamsData("Surface Casing", 0.0f, 100f, 150f, "m", false);
            inputParams[2] = new InputParamsData("Intermediate Casing", 0.0f, 600f, 800f, "m", false);
            inputParams[3] = new InputParamsData("Slotted Liner", 0.0f, 800f, 1200f, "m", false);
            inputParams[4] = new InputParamsData("Well Length", 0.0f, 1500f, 2000f, "m", false);
        }

        TVD.setText(getString(R.string.displayTVD, Float.toString(inputParams[0].getFieldValue())));
        SurfaceCasing.setText(getString(R.string.displaySurfaceCasing, Float.toString(inputParams[1].getFieldValue())));
        IntermediateCasing.setText(getString(R.string.displayIntermediateCasing, Float.toString(inputParams[2].getFieldValue())));
        SlottedLiner.setText(getString(R.string.displaySlottedLiner, Float.toString(inputParams[3].getFieldValue())));
        WellLength.setText(getString(R.string.displayWellLength, Float.toString(inputParams[4].getFieldValue())));

        edit_TVD.setText(Float.toString(inputParams[0].getFieldValue()));
        edit_SurfaceCasing.setText(Float.toString(inputParams[1].getFieldValue()));
        edit_IntermediateCasing.setText(Float.toString(inputParams[2].getFieldValue()));
        edit_SlottedLiner.setText(Float.toString(inputParams[3].getFieldValue()));
        //edit_WellLength.setText(Float.toString(inputParams[4].getFieldValue()));


        edit_TVD.addTextChangedListener(new InputParamsTextWatcher(R.string.displayTVD, TVD));
        edit_SurfaceCasing.addTextChangedListener(new InputParamsTextWatcher(R.string.displaySurfaceCasing, SurfaceCasing));
        edit_IntermediateCasing.addTextChangedListener(new InputParamsTextWatcher(R.string.displayIntermediateCasing, IntermediateCasing));
        edit_SlottedLiner.addTextChangedListener(new InputParamsTextWatcher(R.string.displaySlottedLiner, SlottedLiner));
        //edit_WellLength.addTextChangedListener(new InputParamsTextWatcher(R.string.displayWellLength, WellLength));

    }

    public void goToParamsAdvanced(View view) {
        Intent intent = new Intent(this, InputParamsAdv.class);
        startActivity(intent);
    }

    public void save(View view) {
        //Input sanitization just in case it goes out of range
        float TVD_value = Float.parseFloat(edit_TVD.getText().toString());
        float SurfaceCasing_value = Float.parseFloat(edit_SurfaceCasing.getText().toString());
        float IntermediateCasing_value = Float.parseFloat(edit_IntermediateCasing.getText().toString());
        float SlottedLiner_value = Float.parseFloat(edit_SlottedLiner.getText().toString());
        float WellLength_value = SurfaceCasing_value + IntermediateCasing_value + SlottedLiner_value;
        String TVD_value_str = String.format("%.1f", TVD_value);
        String SurfaceCasing_value_str = String.format("%.1f", SurfaceCasing_value);
        String IntermediateCasing_value_str = String.format("%.1f", IntermediateCasing_value);
        String SlottedLiner_value_str = String.format("%.1f", SlottedLiner_value);
        String WellLength_value_str = String.format("%.1f", WellLength_value);

        //Change to updated Well Length display as data is being saved
        WellLength.setText(getString(R.string.displayWellLength, WellLength_value_str));

        View toast_container = findViewById(R.id.custom_toast_container);
        if (TVD_value < 0 || TVD_value > 500) {
            int toastText = R.string.message_TVD_limits;
            ResizeToast.makeText(getApplicationContext(), toastText, getLayoutInflater(), toast_container);
            return;
        }
        if (SurfaceCasing_value < 0 || SurfaceCasing_value > 150) {
            int toastText = R.string.message_SurfaceCasing_limits;
            ResizeToast.makeText(getApplicationContext(), toastText, getLayoutInflater(), toast_container);
            return;
        }
        if (IntermediateCasing_value < 0 || IntermediateCasing_value > 800) {
            int toastText = R.string.message_IntermediateCasing_limits;
            ResizeToast.makeText(getApplicationContext(), toastText, getLayoutInflater(), toast_container);
            return;
        }
        if (SlottedLiner_value < 0 || SlottedLiner_value > 1200) {
            int toastText = R.string.message_SlottedLiner_limits;
            ResizeToast.makeText(getApplicationContext(), toastText, getLayoutInflater(), toast_container);
            return;
        }
        if (WellLength_value < 0 || WellLength_value > 2000) {
            int toastText = R.string.message_WellLength_limits;
            ResizeToast.makeText(getApplicationContext(), toastText, getLayoutInflater(), toast_container);
            return;
        }

        inputParams[0].setFieldValue(Float.parseFloat(TVD_value_str));
        inputParams[1].setFieldValue(Float.parseFloat(SurfaceCasing_value_str));
        inputParams[2].setFieldValue(Float.parseFloat(IntermediateCasing_value_str));
        inputParams[3].setFieldValue(Float.parseFloat(SlottedLiner_value_str));
        inputParams[4].setFieldValue(Float.parseFloat(WellLength_value_str));
        inputParamsDao.writeInputs(inputParams);

        int toastText = R.string.message_saved;
        ResizeToast.makeText(getApplicationContext(), toastText, getLayoutInflater(), toast_container);
    }

    public class InputParamsTextWatcher implements TextWatcher {

        private int wrapperText; //Integer is index inside string.xml for string
        private TextView textView;

        public InputParamsTextWatcher(int wrapperText, TextView textView) {
            this.wrapperText = wrapperText;
            this.textView = textView;
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {
            //Do nothing
        }
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
            //Do nothing
        }
        @Override
        public void afterTextChanged(Editable s) {
            textView.setText(getString(wrapperText, s.toString()));

            //Updating specific field doesn't work here for some reason - no big deal though
//            float SurfaceCasing_value = Float.parseFloat(edit_SurfaceCasing.getText().toString());
//            float IntermediateCasing_value = Float.parseFloat(edit_IntermediateCasing.getText().toString());
//            float SlottedLiner_value = Float.parseFloat(edit_SlottedLiner.getText().toString());
//            float WellLength_value = SurfaceCasing_value + IntermediateCasing_value + SlottedLiner_value;
//            String WellLength_value_str = String.format("%.1f", WellLength_value);
//            WellLength.setText(getString(wrapperText, WellLength_value_str));
        }
    }

    @Override
    public void onBackPressed() { //Physical back button on device
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) { //For UP button ("back" button on screen)
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return(true);
        }
        return(super.onOptionsItemSelected(item));
    }
}


