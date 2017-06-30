package com.example.owner.wellcalculator.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

import com.example.owner.wellcalculator.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Zhao Heng Wen on 6/25/2016.
 * This class is used for the Calendar page after the user enters here through the Calculate button on main page
 */
//TODO: Add the ability for user to save date range, pressure, and flow rate data - DONE
//TODO: Add buttons: View Parameters, Edit, Save, Clear (clear selected), Clear All - DONE
//TODO: Do not let the user click button if state is OneDate or DateRangeAndDate - DONE
public class InputCalendar extends AppCompatActivity {

    //These strings are simply used as the key for the key/value pair passed to the Valves and Meters page
    public final static String PRESSURE = "com.mycompany.myfirstapp.PRESSURE";
    public final static String FLOW_RATE = "com.mycompany.myfirstapp.FLOW_RATE";

    private Context context;
    private Intent intentValves;

    //States for the entire Calendar itself
    private enum FlashEnum {
        NoDate, OneDate, DateRangeOnly, LockDateRange, DateRangeAndDate
    }
    private FlashEnum calendarState;

    //This is the primary data set for Calendar inputs
    private List<InputCalendarData> calendarDataList;

    //Buttons
    CalendarButton[] calendarButtons = new CalendarButton[100];
    Button[] controlButtons = new Button[5];

    //Temporary variables used to determine states
    private int otherDay;  //Denotes the first selection of day when selecting a date range
    private int selectedStartDay;
    private int selectedEndDay;

    private InputCalendarDao inputCalendarDao;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setState(FlashEnum.NoDate);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inputcalendar);
        this.context = this;
        this.intentValves = new Intent(context, InputValves.class);
        addCalendarButtons();
        addControlButtons();

        //Create new empty list of date ranges or grab existing ones from DB
        inputCalendarDao = new InputCalendarDao(this);
        calendarDataList = inputCalendarDao.readInputs();
        if (calendarDataList == null || calendarDataList.size() == 0) {
            setState(FlashEnum.NoDate);
            calendarDataList = new ArrayList<InputCalendarData>();
        } else {
            setState(FlashEnum.DateRangeOnly);
            for (InputCalendarData daterange: calendarDataList) {
                resetDateRange(calendarButtons, daterange.getStartDay(), daterange.getEndDay());
            }
        }
    }

    //Add all the "day" buttons ranging from day 1 to day 100
    //TODO: Some parameters of each button are hard coded!
    private void addCalendarButtons() {
        TableLayout layout = (TableLayout) findViewById(R.id.inputcalendar);
        TableRow[] rows = new TableRow[10];
        int i = 0;
        for (int j = 0; j < 10; j++) {
            rows[j] = new TableRow(this);
            rows[j].setLayoutParams(
                    new TableRow.LayoutParams(
                            TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
            for (int k = 0; k < 10; k++) {
                final int day = i+1;
                calendarButtons[i] = new CalendarButton(this);
                calendarButtons[i].setText(Integer.toString(day));
                calendarButtons[i].setTextSize(24);

                //The Text, TextOff, and TextOn all must be explicitly set for some reason
                calendarButtons[i].setTextOff(Integer.toString(day));
                calendarButtons[i].setTextOn(Integer.toString(day));
                calendarButtons[i].setWidth(144);
                calendarButtons[i].setHeight(98);
                calendarButtons[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        calendarFSM(calendarButtons, day);
                    }
                });
                rows[j].addView(calendarButtons[i]);
                i++;
            }
            layout.addView(rows[j]);
        }
    }

    //Add the 5 control buttons below the date buttons
    private void addControlButtons() {
        TableLayout layout = (TableLayout) findViewById(R.id.inputcalendar);
        TableRow row = new TableRow(this);
        TableRow.LayoutParams tableRowParams = new
                TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
        tableRowParams.span = 2; //TODO: The table row parameters are hardcoded
        row.setLayoutParams(tableRowParams);
        int i = 0;
        for (int k = 0; k < 5; k++) {
            controlButtons[i] = new Button(this);
            controlButtons[i].setTextSize(TypedValue.COMPLEX_UNIT_SP, 18); //TODO: The 12 is hardcoded
            controlButtons[i].setLayoutParams(tableRowParams);
            setControlButton(k, controlButtons[i]);
            row.addView(controlButtons[i]);
            i++;
        }
        layout.addView(row);
    }

    //Add the functionalities of the control buttons
    private void setControlButton(int placement, Button button) {
        switch (placement) {
            case 0:
                button.setText(R.string.button_view_parameters); //Display corresponding pressure/flowrate of a selected (highlighted/locked) date range
                button.setOnClickListener(new View.OnClickListener() {
                    @Override // NoDate, OneDate, DateRangeOnly, LockDateRange, DateRangeAndDates
                    public void onClick(View v) {
                        FlashEnum state = getState();

                        //Display error Toast if no date is selected
                        if (state != FlashEnum.LockDateRange) {
                            msg_UnspecifiedDateRange(state);
                        } else {
                            View toast_container = findViewById(R.id.custom_toast_container);
                            String toastText = displayValveParams();
                            ResizeToast.makeText(context, toastText, getLayoutInflater(), toast_container);
                        }
                    }
                });
                break;
            case 1:
                button.setText(R.string.button_edit); //This button allows the Calendar page to go to InputValves page
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        FlashEnum state = getState();

                        //Display error Toast if no date is selected
                        if (state != FlashEnum.LockDateRange) {
                            msg_UnspecifiedDateRange(state);
                        } else { //Open Valves and Meters page for date range selected
                            for (InputCalendarData daterange: calendarDataList) {
                                if (daterange.isEqualTo(selectedStartDay, selectedEndDay)) {
                                    intentValves.putExtra(PRESSURE, daterange.getPressure());
                                    intentValves.putExtra(FLOW_RATE, daterange.getFlowRate());
                                    break;
                                }
                            }
                            startActivityForResult(intentValves, 0);
                        }
                    }
                });
                break;
            case 2:
                button.setText(R.string.button_save);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        storeCalendarInputs();
                    }
                });
                break;
            case 3:
                button.setText(R.string.button_clear); //Get rid of selected (highlighted/locked) date range
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        FlashEnum state = getState();

                        //Display error Toast if no date is selected
                        if (state != FlashEnum.LockDateRange) {
                            msg_UnspecifiedDateRange(state);
                        } else {
                            //Clear date range selected
                            for (InputCalendarData daterange: calendarDataList) {
                                if (daterange.isEqualTo(selectedStartDay, selectedEndDay)) {
                                    calendarDataList.remove(daterange);
                                    //For debugging only
                                    //Toast.makeText(context, Integer.toString(calendarDataList.size()),
                                    //       Toast.LENGTH_SHORT).show();
                                    for (int k = selectedStartDay; k <= selectedEndDay; k++) {
                                        calendarButtons[k-1].setOff();
                                    }
                                    break;
                                }
                            }
                            if (calendarDataList.isEmpty()) {
                                setState(FlashEnum.NoDate);
                            } else {
                                setState(FlashEnum.DateRangeOnly);
                            }
                        }
                    }
                });
                break;
            case 4: //Get rid of all date ranges
                button.setText(R.string.button_clear_all);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        calendarDataList.clear();
                        //For debugging only
                        //Toast.makeText(context, Integer.toString(calendarDataList.size()),
                        //        Toast.LENGTH_SHORT).show();
                        for (int k = 1; k <= 100; k++) {
                            calendarButtons[k - 1].setOff();
                        }
                        setState(FlashEnum.NoDate);
                    }
                });
                break;
            default:
                break;
        }
    }

    //Set the visual display of the passed in Calendar button day based on state
    private void calendarFSM(CalendarButton[] buttons, int day) {
        switch (calendarState) {
            case NoDate:    //No date to single day (not done selecting a date range yet)
                setState(FlashEnum.OneDate);
                buttons[day-1].setOn(0);
                otherDay = day;
                break;
            case OneDate:
                setState(FlashEnum.DateRangeOnly);
                setDateRange(buttons, day, otherDay);
                break;
            case DateRangeOnly:  //Date range to single data (not done selecting second date range yet)
                for(InputCalendarData calendarData: calendarDataList) { //Iterates all date ranges, if a date from any range is selected, lock it (highlight it red)
                    int start = calendarData.getStartDay();
                    int end = calendarData.getEndDay();
                    if (day >= start && day <= end) {
                        setState(FlashEnum.LockDateRange);
                        selectedStartDay = start;
                        selectedEndDay = end;
                        for (int k = start; k <= end; k++) {
                            buttons[k-1].setHighlighted();
                        }
                        return;
                    }
                }
                setState(FlashEnum.DateRangeAndDate); //In this new state, a day outside the selected date ranges is selected
                buttons[day-1].setOn(0);
                otherDay = day;
                break;
            case LockDateRange: //Unselect (unhighlight/unlock) the selected date range if a button within this range is touched
                if (day >= selectedStartDay && day <= selectedEndDay) {
                    setState(FlashEnum.DateRangeOnly);
                    resetDateRange(buttons, selectedStartDay, selectedEndDay);
                }
                break;
            case DateRangeAndDate: //Finishing selecting another date range to have 2+ date ranges in total
                int prevEnd = 0;     //Case1: At this point, the single date (otherDay) is before the
                                     //start date of the first date range
                Collections.sort(calendarDataList);
                for(InputCalendarData calendarData: calendarDataList) {
                    //Case2: If single date (otherDay) is after last date of prev iteration (date range)
                    //and before the start date, only then is the date range set
                    int start = calendarData.getStartDay();
                    int end = calendarData.getEndDay();
                    if (otherDay < start && otherDay > prevEnd) {
                        if (day < start && day > prevEnd) {
                            setState(FlashEnum.DateRangeOnly);
                            setDateRange(buttons, day, otherDay);
                            return;
                        }
                    }
                    prevEnd = end;
                }
                if (otherDay > prevEnd && day > prevEnd) { //Case3: At this point if there is a single date (otherDay), it would be after the last date of
                    //final date range
                    setState(FlashEnum.DateRangeOnly);
                    setDateRange(buttons, day, otherDay);
                }
                break;
        }
    }

    //Display various error text based on various Calendar states
    private void msg_UnspecifiedDateRange(FlashEnum state) {
        View toast_container = findViewById(R.id.custom_toast_container);
        if (state == FlashEnum.NoDate || state == FlashEnum.DateRangeOnly) {
            int toastText = R.string.message_please_specify;
            ResizeToast.makeText(context, toastText, getLayoutInflater(), toast_container);
        } else if (state == FlashEnum.OneDate || state == FlashEnum.DateRangeAndDate) {
            int toastText = R.string.message_please_select;
            ResizeToast.makeText(context, toastText, getLayoutInflater(), toast_container);
        }
    }

    //Change the button state of each button in the date range (ranging from day1 to day2)
    private void setDateRange(CalendarButton[] buttons, int day1, int day2) {
        if (day1 > day2) {
            for (int k = day2+1; k <= day1-1; k++) {
                buttons[k-1].setOn(0);
            }
            buttons[day2-1].setOn(1);
            buttons[day1-1].setOn(2);
            calendarDataList.add(new InputCalendarData(day2, day1, 0.5f, 0f));
        } else if (day1 < day2) {
            for (int k = day1+1; k <= day2-1; k++) {
                buttons[k-1].setOn(0);
            }
            buttons[day1-1].setOn(1);
            buttons[day2-1].setOn(2);
            calendarDataList.add(new InputCalendarData(day1, day2, 0.5f, 0f));
        } else {
            buttons[day1-1].setOn(3);
            calendarDataList.add(new InputCalendarData(day1, day1, 0.5f, 0f));
        }
        otherDay = 0;
    }

    //This is used for unselecting (unlock/unhighlight) a date range, so you don't add duplicate range ranges
    //This is also used to redisplay the calendar button states when the data is read from database
    private void resetDateRange(CalendarButton[] buttons, int day1, int day2) {
        if (day1 > day2) {
            for (int k = day2+1; k <= day1-1; k++) {
                buttons[k-1].setOn(0);
            }
            buttons[day2-1].setOn(1);
            buttons[day1-1].setOn(2);
        } else if (day1 < day2) {
            for (int k = day1+1; k <= day2-1; k++) {
                buttons[k-1].setOn(0);
            }
            buttons[day1-1].setOn(1);
            buttons[day2-1].setOn(2);
        } else {
            buttons[day1-1].setOn(3);
        }
        otherDay = 0;
    }

    //Return pressure and flow rate of the selected day range (ranging from selectedStartDay to selectedEndDay)
    private String displayValveParams() {
        for (InputCalendarData calendarData :calendarDataList) {
            if (selectedStartDay == calendarData.getStartDay()) {
                if (selectedEndDay == calendarData.getEndDay()) {
                    return "Pressure: " + calendarData.getPressure() + " (MPa)" +
                            "\nFlow rate: " + calendarData.getFlowRate() + " (m³/hr)";
                }
            }
        }
        return null;
    }

    @Override
    //This is the call back function from calling the InputValves page
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Retrieve data in the intent
        Float pressure;
        Float flowRate;
        if (resultCode == RESULT_OK && requestCode == 0) { //In this case, requestCode was 0 when calling InputValves
            //For debug only
            //Toast.makeText(this, data.getStringExtra(InputValves.FLOW_RATE),
            //        Toast.LENGTH_SHORT).show();

            try { // TODO: Do error checking for if arg0 is not float
                pressure = Float.parseFloat(data.getStringExtra(InputValves.PRESSURE));
                flowRate = Float.parseFloat(data.getStringExtra(InputValves.FLOW_RATE));
            } catch (NumberFormatException e) {
                pressure = 0f;
                flowRate = 0f;
            }

            //Set the pressure/flow rate for the selected date range based on retrieved data from InputValves
            for (InputCalendarData calendarData :calendarDataList) {
                if (selectedStartDay == calendarData.getStartDay()) {
                    if (selectedEndDay == calendarData.getEndDay()) {
                        calendarData.setPressure(pressure);
                        calendarData.setFlowRate(flowRate);
                        break;
                    }
                }
            }
        }
    }

    //Write list of InputCalendarData to DB
    private void storeCalendarInputs() {
        inputCalendarDao.writeInputs(calendarDataList);

        View toast_container = findViewById(R.id.custom_toast_container);
        int toastText = R.string.message_saved;
        ResizeToast.makeText(context, toastText, getLayoutInflater(), toast_container);
    }

    //Getter/setter for calendar states
    private FlashEnum getState() {
        return this.calendarState;
    }
    private void setState(FlashEnum state) {
        if(state == null)return;
        this.calendarState = state;
    }

    //Finish() allows this page to go back to the already opened MainPage activity rather than reopening the activity
    @Override
    public void onBackPressed() { //Physical back button on device
        finish();
    }   //"Back" button on physical device
    @Override
    public boolean onOptionsItemSelected(MenuItem item) { //UP button ("back" button on screen)
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return(true);
        }
        return(super.onOptionsItemSelected(item));
    }

}