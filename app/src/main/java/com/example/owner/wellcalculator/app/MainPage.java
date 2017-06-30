package com.example.owner.wellcalculator.app;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.app.AppCompatActivity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;

import com.example.owner.wellcalculator.R;

//TODO: If the finger drags outside the image, the line-drawer should not be cut off trying to follow it - DONE
/*
This class is used to display the Main Page right when the app is opened and after the Terms & Conditions are accepted
 */
public class MainPage extends AppCompatActivity {
    // @Override
    private Context context;

    //Data access objects
    private InputParamsDao inputParamsDao;
    private OutputDao outputDao;

    //Arrays of input/output data
    private InputParamsData[] inputParams;
    private OutputData[] outputData;

    //Output views
    private TextView volumetricFlow_output;
    private EditText tsolve_str;
    private TextView pressureTV;
    private TextView temperatureTV;
    private TextView enthalpyTV;
    private TextView qualityTV;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainpage);
        this.context = this;

        //Display terms and conditions
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.disclaimer);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        final CheckBox accept_toggle = (CheckBox) dialog.findViewById(R.id.radio_accept);
        Button continue_button = (Button) dialog.findViewById(R.id.button_continue);
        continue_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (accept_toggle.isChecked()) {
                    dialog.dismiss();
                } else {
                    View toast_container = findViewById(R.id.custom_toast_container);
                    int toastText = R.string.message_please_agree;
                    ResizeToast.makeText(getApplicationContext(), toastText, getLayoutInflater(), toast_container);
                }
            }
        });
        dialog.show();

        //Grab well parameters and output data from database
        inputParamsDao = new InputParamsDao(context);
        outputDao = new OutputDao(context);
        inputParams = inputParamsDao.readInputs();
        outputData = outputDao.readOutputs();

        //Volumetric flowrate and Tsolve display
        volumetricFlow_output = (TextView) findViewById(R.id.display_VolumetricFlowrate);
        tsolve_str = (EditText) findViewById(R.id.input_tsolve);

        //Textviews for dynamic well data based on well position
        pressureTV = (TextView) findViewById(R.id.pressureTV);
        temperatureTV = (TextView) findViewById(R.id.temperatureTV);
        enthalpyTV = (TextView) findViewById(R.id.enthalpyTV);
        qualityTV = (TextView) findViewById(R.id.qualityTV);

        final ImageView wellImage = (ImageView) findViewById(R.id.wellimage);

        //Used for debug text on screen
        //final TextView debug = (TextView) findViewById(R.id.debugText);
        //debug.setText(getApplicationContext().getFilesDir().getAbsolutePath());

        final Bitmap bitMap = ((BitmapDrawable)wellImage.getDrawable()).getBitmap();

        //Get relative coordinates of the well image
        final int widthBM = bitMap.getWidth();
        final int heightBM = bitMap.getHeight();

        //This is where data gets updated based on where the user touch the well
        wellImage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                //Set up bitmap copy and canvas to draw line from well to output box
                Bitmap mutableBitmap = bitMap.copy(Bitmap.Config.ARGB_8888, true);
                Canvas canvas = new Canvas(mutableBitmap);

                int widthImg = wellImage.getWidth();
                int heightImg = wellImage.getHeight();

                float xRatio = (float)widthBM / (float)widthImg;
                float yRatio = (float)heightBM / (float)heightImg;

                //Bitmap coordinates used for mapping are xImg and yImg
                int x = (int) event.getX();
                int y = (int) event.getY();
                int xImg = Math.round(x*xRatio);
                int yImg = Math.round(y*yRatio);

                // Set up the lines that are drawn from well image to output box
                Paint paint = new Paint();
                paint.setColor(Color.BLACK);
                paint.setStyle(Paint.Style.STROKE);
                // Line width in pixels
                paint.setStrokeWidth(1);
                paint.setAntiAlias(true);

                try {
                    //For debug only
                    //debug.setText(xImg + " " + yImg + " " + widthBM + " " + heightBM +" " + Boolean.toString((xImg > widthBM) || (yImg > heightBM)));

                    //Don't do anything if the user probes outside the well (screen is white outside of the well)
                    if (bitMap.getPixel(xImg, yImg) == Color.WHITE) {
                        return true;
                    }
                } catch (IllegalArgumentException e) {
                    return true;
                }

                //TODO: Try to get rid of the hardcodes!!
                canvas.drawLine( //Line 1
                        xImg,
                        yImg,
                        187,
                        265,
                        paint
                );
                canvas.drawLine( //Line2
                        xImg,
                        yImg,
                        505,
                        265,
                        paint
                );

                wellImage.setImageBitmap(mutableBitmap);

                if (inputParams != null && outputData != null) {
                    int index = mapWellNodeIndex(xImg, yImg, inputParams);
                    if (index < 0) index = 0;
                    if (index > outputData.length - 1) index = outputData.length - 1;

                    //Old code used to check positions
                    //TextView xPosTV = (TextView) findViewById(R.id.xPos);
                    //TextView yPosTV = (TextView) findViewById(R.id.yPos);

                    //Setting text for the textviews - getString is used to put dynamic string inside static string wrapper
                    //Unit is Pascal but is divided by 1000000 to show MPa
                    pressureTV.setText(getString(R.string.displayPressure, String.format("%.2f", outputData[index].getP() / 1000000)));
                    //Unit is Kelvin but is subtracted by -273.15 to get Celcius
                    temperatureTV.setText(getString(R.string.displayTemperature, String.format("%.2f", outputData[index].getT() - 273.15)));
                    //Unit is J/kg but is divided by 1000 to get kJ/kg
                    enthalpyTV.setText(getString(R.string.displayEnthalpy, String.format("%.2f", outputData[index].getH() / 1000)));
                    qualityTV.setText(getString(R.string.displayQuality, String.format("%.2f", outputData[index].getX())));
                }
                return true;
            }
        });

        //Right when the app opens, the lines from the well image to the output box isn't shown yet, so it's better to set output
        //as ??? rather than some random number, since these outputs are based on position of well (denoted by lines' pinpoints)
        if (outputData != null) {
            tsolve_str.setText(Integer.toString(outputData[0].getTsolve())); //tsolve is the same value no matter what index
            String volumetricFlowOutput = String.format("%.3f", outputData[0].getVolumetricFlowRate());
            volumetricFlow_output.setText(getString(R.string.displayVolumetricFlowrate, volumetricFlowOutput));
            pressureTV.setText(getString(R.string.displayPressure, "???"));
            temperatureTV.setText(getString(R.string.displayTemperature, "???"));
            enthalpyTV.setText(getString(R.string.displayEnthalpy, "???"));
            qualityTV.setText(getString(R.string.displayQuality, "???"));
        } else { //If there is no data in the database, all outputs are set as 0.
            volumetricFlow_output.setText(getString(R.string.displayVolumetricFlowrate, "0"));
            pressureTV.setText(getString(R.string.displayPressure, "0"));
            temperatureTV.setText(getString(R.string.displayTemperature, "0"));
            enthalpyTV.setText(getString(R.string.displayEnthalpy, "0"));
            qualityTV.setText(getString(R.string.displayQuality, "0"));
        }
    }

    //This is called after pressing the Calculate button
    public void calculate(View view) {

        //Sanitize Tsolve before calculating
        int tsolve = Integer.parseInt(tsolve_str.getText().toString());
        if (tsolve < 10 || tsolve > 100) {
            View toast_container = findViewById(R.id.custom_toast_container);
            int toastText = R.string.message_tsolve_range;
            ResizeToast.makeText(getApplicationContext(), toastText, getLayoutInflater(), toast_container);
            return;
        }

        //Calculation is handled by MainPage_CalculateScreen splashscreen
        Intent calculationIntent = new Intent(this, MainPage_CalculateScreen.class);
        calculationIntent.putExtra("tsolve", tsolve);
        startActivityForResult(calculationIntent, 1); //1 as arbitrary req code, different from 0 for Calendar to Valves page
    }

    //TODO: Place hardcoded values in a config
    //Obtain index of well_node well based on coordinates on screen and some well parameters
    public int mapWellNodeIndex(int x, int y, InputParamsData[] inputParams) {
        //Get well parameters
        double tvd = inputParams[0].getFieldValue();
        double surface_Casing = inputParams[1].getFieldValue();
        double intermediate_Casing = inputParams[2].getFieldValue();
        double wellLength = inputParams[4].getFieldValue();

        //Get key numeric values
        double deltaz = tvd-surface_Casing;
        double arc_length= Math.min(intermediate_Casing, deltaz*(Math.PI/2));
        double s;
        int xmin = 265;
        int xmax = 755;
        int ymin = 30;
        int ymax = 300;
        int ds = 10;
        if (y < ymax) {
            double dpvert = surface_Casing/(ymax-ymin); //meters per pixel
            s = (y-ymin)*dpvert;
        } else if (x > xmin) {
            double dphor = (wellLength-(arc_length+surface_Casing))/(xmax-xmin); //meters per pixel
            s=(x-xmin)*dphor+(arc_length+surface_Casing);
        } else {
            double rad=2*arc_length/Math.PI; //Radius of the arc in meters
            double centerx=xmin;
            double centery=ymax; //Center of the arc position in pixels
            double theta=Math.atan((y-centery)/(centerx-x)); //angle along radius from 0 (vertical) to pi/2 (horizontal)
            s=surface_Casing+theta*rad;
        }
        return (int) Math.ceil((2*wellLength-s)/ds);
    }

    //This is called when the user presses the Input Parameters button
    public void goToParams(View view) {
        //Intent takes in Context as first parameter, MainPage is a subclass of Context
        Intent intent = new Intent(this, InputParams.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        //Intent carry data types as key-value pairs called extras. Putextra() takes key name and value
        //The system receives this call and starts an instance of the Activity specified by the Intent.

        //An Intent is an object that provides runtime binding between separate components
        //(such as two activities). The Intent represents an app’s "intent to do something."
        // You can use intents for a wide variety of tasks, but most often they’re used to start another activity.

        startActivity(intent);
    }

    //This is called when the user presses the Calendar button
    public void goToCalendar(View view) {
        Intent intent = new Intent(this, InputCalendar.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        startActivity(intent);
    }

    @Override
    //int count = 1; //For debug only

    //This is the callback after the MainPage_CalculateScreen exits
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        // check if the request code is same as what is passed here it is 2
        if(requestCode==1)
        {
            //For debug only
            //TextView debug = (TextView) findViewById(R.id.debugText);
            //long startTime = System.currentTimeMillis();

            //If the calculation is unsuccessful, display the error as toast
            View toast_container = findViewById(R.id.custom_toast_container);
            if (resultCode == RESULT_CANCELED) {
                String toastText = data.getStringExtra("error");
                ResizeToast.makeText(getApplicationContext(), toastText, getLayoutInflater(), toast_container);
                return;
            }

            //Grab newly updated DB data after calculation is done
            outputData = outputDao.readOutputs();
            inputParams = inputParamsDao.readInputs();
            if (outputData == null) {
                int toastText = R.string.message_calculation_fail_missing_data;
                ResizeToast.makeText(getApplicationContext(), toastText, getLayoutInflater(), toast_container);
                return;
            }

            //Right when the calculation is done, the data could suddenly be different, so it's better to set it as ??? first
            //and then change it based on user probe rather than leave the old output data up
            String volumetricFlowOutput = String.format("%.3f", outputData[0].getVolumetricFlowRate()); //tsolve is the same value no matter what index
            volumetricFlow_output.setText(getString(R.string.displayVolumetricFlowrate, volumetricFlowOutput));
            pressureTV.setText(getString(R.string.displayPressure, "???"));
            temperatureTV.setText(getString(R.string.displayTemperature, "???"));
            enthalpyTV.setText(getString(R.string.displayEnthalpy, "???"));
            qualityTV.setText(getString(R.string.displayQuality, "???"));

            //For debug only
//            long elapsedTime = System.currentTimeMillis() - startTime;
//            debug.setText(out + " " + elapsedTime / 1000 + " " + count);
//            count++;
        }
    }
}
