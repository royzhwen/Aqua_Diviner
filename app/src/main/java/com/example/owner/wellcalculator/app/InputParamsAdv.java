package com.example.owner.wellcalculator.app;

/**
 * Created by Zhao Heng Wen on 6/3/2016.
 *  This class is used for the Input Parameters - Advanced page (accessed from Advanced Parameters button on the Input Parameters page)
 */
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.owner.wellcalculator.R;

import java.io.BufferedReader;

public class InputParamsAdv extends AppCompatActivity {

    private String[] arrText; //Field names
    private float[] arrTemp; //Field values
    private float[] arrTempMin; //Minimum field values
    private float[] arrTempMax; //Maximum field values
    private String[] arrUnit;
    private InputParamsData inputData[];
    private InputParamsAdvDao inputDao;
    private Context context;
    @Override
    //Activity receives the intent with the message, then renders the message.
    //This method defines the activity layout with the setContentView() method.
    //This is where the activity performs the initial setup of the activity components.
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inputparamsadv);
        this.context = this;

        BufferedReader br = null;
        String cvsSplitBy = ",";

        String[] advanced_inputs = getResources().getStringArray(R.array.advanced_inputs);
        arrText = new String[advanced_inputs.length];
        arrTemp = new float[advanced_inputs.length];
        arrTempMin = new float[advanced_inputs.length];
        arrTempMax = new float[advanced_inputs.length];
        arrUnit = new String[advanced_inputs.length];
        for (int i = 0; i < advanced_inputs.length; i++) {

            // use comma as separator
            String[] field = advanced_inputs[i].split(cvsSplitBy);
            arrUnit[i] = (!field[4].equals("n/a")) ? field[4] : "";
            arrText[i] = field[0];
            arrTempMin[i] = (!field[1].equals("")) ? Float.parseFloat(field[1]) : 0f;
            arrTemp[i] = (!field[2].equals("")) ? Float.parseFloat(field[2]) : 0f;
            arrTempMax[i] = (!field[3].equals("")) ? Float.parseFloat(field[3]) : 0f;

        }

        /*
        int numParams = 50;
        TextView[] param = new TextView[50];
        EditText[] paramInput = new EditText[50];
        for (int i = 0; i < numParams; i++) {
            param[i] = new TextView(this);
            paramInput[i] = new EditText(this);

            RelativeLayout layout = (RelativeLayout) findViewById(R.id.inputs);
            layout.addView(param[i]);
        }*/
        //TODO: Make the parameter names NOT hard coded and fix the logics dealing with inputData (read then overwrite atm?)
        //TODO: Think about whether it's necessary to get rid of the ID column for InputParamsAdv data
        //TODO: Fix code so that only updated parameter names or data get written to DB, not all of them - low priority
        //TODO: Think about the InputParamsdao objects use lists rather than arrays (Current hardcoded input has to be array)
        //TODO: Fix the XML IDs of the EditText and TextView (currently called edittext1 and textview1)
        inputDao = new InputParamsAdvDao(this);

        inputData = inputDao.readInputs();

        //Read from DB
        if (inputData != null) {
            for (int i = 0; i < inputData.length; i++) {
                arrTemp[i] = inputData[i].getFieldValue();
            }
        } else { //Default
            inputData = new InputParamsData[advanced_inputs.length];
            for (int i = 0; i < inputData.length; i++) {
                inputData[i] = new InputParamsData(arrText[i], arrTempMin[i], arrTemp[i], arrTempMax[i], arrUnit[i], true);
                //inputData[i] = new InputParamsData(arrText[i], 0f, 0f, 0f, "m", true); //TODO: Remove placeholder code
            }
        }
        MyListAdapter myListAdapter = new MyListAdapter();
        ListView listView = (ListView) findViewById(R.id.list_InputParams);
        listView.setAdapter(myListAdapter);
    }

    /*public void clear(View v) { //Does not work, may or may not needed depending on input length
        EditText paramInput = (EditText) findViewById(v.getId());
        paramInput.setText("");
    }*/
    public void storeInputs(View view) {
        View toast_container = findViewById(R.id.custom_toast_container);
        for (int i = 0; i < arrTemp.length; i++) {
            if (arrTemp[i] < arrTempMin[i] || arrTemp[i] > arrTempMax[i]) {
                String toastText = String.format("The %s must be between %.2f to %.2f %s",
                        arrText[i], arrTempMin[i], arrTempMax[i], arrUnit[i]);
                ResizeToast.makeText(getApplicationContext(), toastText, getLayoutInflater(), toast_container);
                return;
            }
        }

        inputDao.writeInputs(inputData);

        int toastText = R.string.message_saved;
        ResizeToast.makeText(getApplicationContext(), toastText, getLayoutInflater(), toast_container);

    }
    private class MyListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            //Auto-generated method stub
            if(arrText != null && arrText.length != 0){
                return arrText.length;
            }
            return 0;
        }

        @Override
        public Object getItem(int position) {
            //Auto-generated method stub
            return (!arrUnit[position].equals("n")) ?
                    arrText[position] + " (" + arrUnit[position] + ")" : arrText[position];
        }

        @Override
        public long getItemId(int position) {
            //Auto-generated method stub
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            //ViewHolder holder = null;
            final ViewHolder holder;
            if (convertView == null) {

                holder = new ViewHolder();
                LayoutInflater inflater = InputParamsAdv.this.getLayoutInflater();
                convertView = inflater.inflate(R.layout.inputparams_inputs, null);
                holder.textView1 = (TextView) convertView.findViewById(R.id.textView1);
                holder.editText1 = (EditText) convertView.findViewById(R.id.editText1);

                convertView.setTag(holder);

            } else {

                holder = (ViewHolder) convertView.getTag();
            }

            holder.ref = position;

            holder.textView1.setText((!arrUnit[position].equals("")) ?
                    arrText[position] + " (" + arrUnit[position] + ")" : arrText[position]);
            holder.editText1.setText(Float.toString(arrTemp[position]));
            holder.editText1.addTextChangedListener(new TextWatcher() {

                @Override
                public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                    //Auto-generated method stub

                }

                @Override
                public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
                                              int arg3) {
                    //Auto-generated method stub

                }

                @Override
                public void afterTextChanged(Editable arg0) {
                    // TODO: Do error checking for if arg0 is not float
                    String input = arg0.toString();
                    try {
                        arrTemp[holder.ref] = Float.parseFloat(arg0.toString());
                    } catch (NumberFormatException e) {
                        arrTemp[holder.ref] = 0;
                    }
                    inputData[holder.ref].setFieldValue(arrTemp[holder.ref]);
                }
            });

            return convertView;
        }

        private class ViewHolder {
            TextView textView1;
            EditText editText1;
            int ref;
        }
    }
    @Override
    public void onBackPressed() { //Physical back button on device
        //Don't save, but save the changes made on the previous page
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
