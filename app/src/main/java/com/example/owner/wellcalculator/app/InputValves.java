package com.example.owner.wellcalculator.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.RotateDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.view.View.OnTouchListener;
import android.widget.TextView;
import android.widget.Toast;

import com.example.owner.wellcalculator.R;

import java.text.DecimalFormat;


/**
 * Created by Zhao Heng Wen on 7/3/2016.
 * This class is used for the Valves and Meters page accessed from the Calendar Page after "Edit"
 */
//TODO: For the newly implemented 1-finger valve turning, fix the rotation/value mapping - DONE by uncomment onWindowFocusChanged
//TODO: Pressing back currently cause changed but unsaved calendar data to be undone - HIGH PRIORITY - DONE
//TODO: Fix the wierd glitch for the 1-finger valve turning where spinning it for a while and then pressing on it cause valve/meter to disappear - HIGH PRIORITY - APPARENTLY FIXED AFTER LIMITING SPIN RANGE
//TODO: Fix image layout so there is no hard-code (ensure it works for different screen sizes) - APPLIES TO ALL PAGES
//TODO; Remove all the unnecessary margins once image layout size is fixed
//TODO: Fix valve so that you can use 1 finger to rotate it - DONE
//TODO: Remove the hard-coded prints and limits - low priority
public class InputValves extends AppCompatActivity {
    public final static String PRESSURE = "com.mycompany.myfirstapp.PRESSURE";
    public final static String FLOW_RATE = "com.mycompany.myfirstapp.FLOW_RATE";
    public String old_pressure = "0";
    public String old_flowRate = "0";
    public String pressure = "0";
    public String flowRate = "0";
    private TextView pressure_display;
    private TextView flowRate_display;
    private TextView testTV;

    // these matrices will be used to move and zoom image
    private ImageView valve_pressure;
    private ImageView meter_pressure;
    private Matrix matrix_p = new Matrix();
    private Matrix meter_matrix_p = new Matrix();
    private float[] lastEvent_p;
    private float oldRot_p = 0f;
    private float rotation_p = 0f;

    private ImageView valve_flowrate;
    private ImageView meter_flowrate;
    private Matrix matrix_f = new Matrix();
    private Matrix meter_matrix_f = new Matrix();
    private float[] lastEvent_f;
    private float oldRot_f = 0f;
    private float rotation_f = 0f;

    private void save_data(String pressure, String flowRate) {
        //For debugging only
        //Toast.makeText(this, pressure, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent();
        intent.putExtra(PRESSURE, pressure);
        intent.putExtra(FLOW_RATE, flowRate);

        setResult(RESULT_OK, intent);

        View toast_container = findViewById(R.id.custom_toast_container);
        int toastText = R.string.message_parameters;
        ResizeToast.makeText(this, toastText, getLayoutInflater(), toast_container);

        finish(); //TODO: Find a way to finish whenever the user wants (finish when user wants to go back to Calendar page)
    }

    public void save(View view) {

        //Input sanitization just in case it goes out of range
        float pressure_float = Float.parseFloat(pressure);
        float flowRate_float = Float.parseFloat(flowRate);

        //The valves/meters GUI should prevent this, but this is added just in case
        View toast_container = findViewById(R.id.custom_toast_container);
        if (pressure_float < 0.5 || pressure_float > 6) {
            int toastText = R.string.message_Pressure_limits;
            ResizeToast.makeText(getApplicationContext(), toastText, getLayoutInflater(), toast_container);
            return;
        }
        if (flowRate_float < 0 || flowRate_float > 12) {
            int toastText = R.string.message_FlowRate_limits;
            ResizeToast.makeText(getApplicationContext(), toastText, getLayoutInflater(), toast_container);
            return;
        }

        String pressure_str = String.format("%.2f", pressure_float);
        String flowRate_str = String.format("%.2f", flowRate_float);
        save_data(pressure_str, flowRate_str);
    }
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inputvalves);

        Intent intent = this.getIntent();

        //Initialize imageviews
        valve_pressure = (ImageView) findViewById(R.id.valve_pressure);
        meter_pressure = (ImageView) findViewById(R.id.meter_needle_pressure);
        valve_flowrate = (ImageView) findViewById(R.id.valve_flowrate);
        meter_flowrate = (ImageView) findViewById(R.id.meter_needle_flowrate);

        //Get data from Calendar
        pressure = Float.toString(intent.getFloatExtra(PRESSURE, 0f));
        flowRate = Float.toString(intent.getFloatExtra(FLOW_RATE, 0f));
        old_pressure = pressure;
        old_flowRate = flowRate;

        //Obtain rotations from data
        rotation_p = getRotFromPressure(Float.parseFloat(pressure));
        rotation_f = getRotFromFlowrate(Float.parseFloat(flowRate));

        //Set text boxes from data
        pressure_display = (TextView) findViewById(R.id.numericPressure);
        flowRate_display = (TextView) findViewById(R.id.numericFlowRate);
        pressure_display.setText(getString(R.string.labelValue, pressure, "(MPa)"));
        flowRate_display.setText(getString(R.string.labelValue, flowRate, "(m³/hr)"));

        //Test code
        testTV = (TextView) findViewById(R.id.textView1);
//        testTV.setText(Float.toString(rotation_p));

        valve_pressure.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ImageView valve = (ImageView) v; //Matrix "canvas" size is somehow based on size of original image size(500x500).
                //If the imageview length/height is too small, the size is actually fixed
                //as long as scaleType="matrix" and it would clip off. 333dp x 333dp is the
                //perfect size since there is a strange multiplier of 1.5 to get 500

                float midX = valve.getWidth() / 2;
                float midY = valve.getHeight() / 2;
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        lastEvent_p = new float[2];
                        lastEvent_p[0] = event.getX();
                        lastEvent_p[1] = event.getY();
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                    case MotionEvent.ACTION_UP:
                        lastEvent_p = null;
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                    case MotionEvent.ACTION_MOVE:
                        if (lastEvent_p != null) {
                            float r = 50 * rotation(event, lastEvent_p, midX, midY);
                            lastEvent_p[0] = event.getX();
                            lastEvent_p[1] = event.getY();
                            oldRot_p = rotation_p;
                            rotation_p = (rotation_p + r) % 360;
                            if (rotation_p < 0) rotation_p += 360;
//                            testTV.setText(rotation_p + "   " + Float.toString(r) + "   " + Float.toString(midX) + "   " + Float.toString(midY)
//                                + "   " + valve_pressure.getWidth() + "   " + valve_pressure.getHeight());//TEST CODE
                            if ((rotation_p >= 260 && rotation_p < 360) || (rotation_p >= 0 && rotation_p <= 120)) {
                                matrix_p.postRotate(r, midX, midY);
                                meter_matrix_p.postRotate(r, meter_pressure.getWidth() / 2, meter_pressure.getHeight() / 2);
                                pressure = String.format("%.2f", getPressure(rotation_p));
                                pressure_display.setText(getString(R.string.labelValue, pressure, "(MPa)"));
                            } else {
                                rotation_p = oldRot_p;
                            }
                        }
                        break;
                }

                valve.setImageMatrix(matrix_p);
                meter_pressure.setImageMatrix(meter_matrix_p);
                return true;
            }
        });
        valve_flowrate.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ImageView valve = (ImageView) v;
                float midX = valve.getWidth() / 2;
                float midY = valve.getHeight() / 2;
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        lastEvent_f = new float[2];
                        lastEvent_f[0] = event.getX();
                        lastEvent_f[1] = event.getY();
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                    case MotionEvent.ACTION_UP:
                        lastEvent_f = null;
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                    case MotionEvent.ACTION_MOVE:
                        if (lastEvent_f != null) {
                            float r = 50 * rotation(event, lastEvent_f, midX, midY);
                            lastEvent_f = new float[2];
                            lastEvent_f[0] = event.getX();
                            lastEvent_f[1] = event.getY();
                            oldRot_f = rotation_f;
                            rotation_f = (rotation_f + r) % 360;
                            if (rotation_f < 0) rotation_f += 360;
//                            testTV.setText(Float.toString(r) + "   " + Float.toString(midX) + "   " + Float.toString(midY)
//                                + "   " + valve_flowRate.getWidth() + "   " + valve_flowRate.getHeight());//TEST CODE
                            if ((rotation_f >= 240 && rotation_f < 360) || (rotation_f >= 0 && rotation_f <= 120)) {
                                matrix_f.postRotate(r, midX, midY);
                                meter_matrix_f.postRotate(r, meter_flowrate.getWidth() / 2, meter_flowrate.getHeight() / 2);
                                flowRate = String.format("%.2f", getFlowRate(rotation_f));
                                flowRate_display.setText(getString(R.string.labelValue, flowRate, "(m³/hr)"));
                            } else {
                                rotation_f = oldRot_f;
                            }
                        }
                    break;
                }

                valve.setImageMatrix(matrix_f);
                meter_flowrate.setImageMatrix(meter_matrix_f);
                return true;
            }
        });
    }

    //This is only called right when the page is loaded
    public void onWindowFocusChanged(boolean hasFocus) {
        //Set valve and meter angles from rotations
        valve_pressure.setImageMatrix(matrix_p);
        meter_pressure.setImageMatrix(meter_matrix_p);

        //For debugging only
        //Toast.makeText(this, valve_pressure.getWidth()+" "+valve_pressure.getHeight() + " " + meter_pressure.getWidth() + " " + meter_pressure.getHeight(),
        //Toast.LENGTH_SHORT).show();

        matrix_p.postRotate(rotation_p, valve_pressure.getWidth() / 2, valve_pressure.getHeight() / 2);
        meter_matrix_p.postRotate(rotation_p, meter_pressure.getWidth() / 2, meter_pressure.getHeight() / 2);
        valve_pressure.setImageMatrix(matrix_p);
        meter_pressure.setImageMatrix(meter_matrix_p);
        valve_flowrate.setImageMatrix(matrix_f);
        meter_flowrate.setImageMatrix(meter_matrix_f);
        matrix_f.postRotate(rotation_f, valve_flowrate.getWidth() / 2, valve_flowrate.getHeight() / 2);
        meter_matrix_f.postRotate(rotation_f,  meter_flowrate.getWidth() / 2,  meter_flowrate.getHeight() / 2);
        valve_flowrate.setImageMatrix(matrix_f);
        meter_flowrate.setImageMatrix(meter_matrix_f);
    }

    //Used for rotating with 1 finger - Only +ve degrees atm
    private float rotation(MotionEvent event, float[] lastEvent, float midX, float midY) {

        //Obtain rotation magnitude
        double a_squared = Math.pow((midX - lastEvent[0]), 2) + Math.pow((midY - lastEvent[1]), 2);
        double b_squared = Math.pow((midX - event.getX()), 2) + Math.pow((midY - event.getY()), 2);
        double c_squared = Math.pow((lastEvent[0] - event.getX()), 2) + Math.pow((lastEvent[1] - event.getY()), 2);
        double a = Math.sqrt(a_squared);
        double b = Math.sqrt(b_squared);
        float rotation_magnitude = (float) Math.acos((c_squared - a_squared - b_squared) / (-2*a*b));

        //Obtain rotation direction based on lastEvent's starting quadrant of image for touch
        //If there is more movement in X-axis
        if (Math.abs(event.getX() - lastEvent[0]) > Math.abs(event.getY() - lastEvent[1])) {
            //Top half of image
            if (lastEvent[1] < midY) {
                if (event.getX() > lastEvent[0]) return rotation_magnitude;
                else return -rotation_magnitude;
            }
            //Bottom half of image
            else {
                if (event.getX() < lastEvent[0]) return rotation_magnitude;
                else return -rotation_magnitude;
            }
        }
        //If there is more movement in Y-axis
        else {
            //Left half of image
            if (lastEvent[0] < midX) {
                if (event.getY() < lastEvent[1]) return rotation_magnitude;
                else return -rotation_magnitude;
            }
            //Right half of image
            else {
                if (event.getY() > lastEvent[1]) return rotation_magnitude;
                else return -rotation_magnitude;
            }
        }

    }

    //Convert between rotation and pressure/flowrate
    public float getPressure(float rotation_p) {
        if (rotation_p >= 0 && rotation_p < 120) {
            return (rotation_p / 40) + 3;
        } else {
            return (rotation_p - 240) / (120/3);
        }
    }
    public float getRotFromPressure(float pressure) {
        if (pressure >= 3) {
            return (pressure - 3) * 40;
        } else {
            return 240 + 120*pressure / 3;
        }
    }
    public float getFlowRate(float rotation_f) {
        if (rotation_f >= 0 && rotation_f < 120) {
            return (rotation_f / 20) + 6;
        } else {
            return (rotation_f - 240) / (120/6);
        }
    }
    public float getRotFromFlowrate(float flowrate) {
        if (flowrate >= 6) {
            return (flowrate - 6) * 20;
        } else {
            return 240 + 120*flowrate / 6;
        }
    }

    @Override
    public void onBackPressed() { //Physical back button on device
        //Input sanitization just in case it goes out of range
        float pressure_float = Float.parseFloat(old_pressure);
        float flowRate_float = Float.parseFloat(old_flowRate);

        //The valves/meters GUI should prevent this, but this is added just in case
        View toast_container = findViewById(R.id.custom_toast_container);
        if (pressure_float < 0.5 || pressure_float > 6) {
            int toastText = R.string.message_Pressure_limits;
            ResizeToast.makeText(getApplicationContext(), toastText, getLayoutInflater(), toast_container);
            return;
        }
        if (flowRate_float < 0 || flowRate_float > 12) {
            int toastText = R.string.message_FlowRate_limits;
            ResizeToast.makeText(getApplicationContext(), toastText, getLayoutInflater(), toast_container);
            return;
        }

        //Don't save, but save the changes made on the Calendar page
        Intent intent = new Intent();
        intent.putExtra(PRESSURE, old_pressure);
        intent.putExtra(FLOW_RATE, old_flowRate);
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