package com.example.owner.wellcalculator.app;

import android.content.Context;
import android.widget.ToggleButton;

import com.example.owner.wellcalculator.R;

/**
 * Created by Zhao Heng Wen on 6/26/2016.
 * This class is used for each date button in the Calendar page
 */
public class CalendarButton extends ToggleButton {

    //States of a button
    public enum FlashEnum {
        ON, ON_BEGIN, ON_END, ON_SINGLE, HIGHLIGHT, OFF
    }
    private FlashEnum buttonState;


    public CalendarButton(Context context) {
        super(context);
        setState(FlashEnum.OFF);
    }

    //Change button BG image depending on state
    private void createDrawableState() {
        switch (buttonState) {
            case ON:
                setBackgroundResource(R.drawable.calendar_on);
                break;
            case ON_BEGIN:
                setBackgroundResource(R.drawable.calendar_on_begin);
                break;
            case ON_END:
                setBackgroundResource(R.drawable.calendar_on_end);
                break;
            case ON_SINGLE:
                setBackgroundResource(R.drawable.calendar_on_single);
                break;
            case HIGHLIGHT:
                setBackgroundResource(R.drawable.calendar_highlight);
                break;
            case OFF:
                setBackgroundResource(R.drawable.calendar_off);
                break;
        }
    }

    //Determine if a button is in a certain state
    public boolean getState(int n) {
        switch (n) {
            case 0:
                return (buttonState == FlashEnum.ON);
            case 1:
                return (buttonState == FlashEnum.ON_BEGIN);
            case 2:
                return (buttonState == FlashEnum.ON_END);
            case 3:
                return (buttonState == FlashEnum.ON_SINGLE);
            case 4:
                return (buttonState == FlashEnum.HIGHLIGHT);
            case 5:
                return (buttonState == FlashEnum.OFF);
            default:
                throw new IllegalArgumentException("Arg must be 0-3");
        }
    }

    //Set button state
    private void setState(FlashEnum state) {
        if(state == null)return;
        this.buttonState = state;
        createDrawableState();
    }

    //Change the button state to one of various "ON" states
    public void setOn(int n) {
        switch (n) {
            case 0:
                setState(FlashEnum.ON);
                break;
            case 1:
                setState(FlashEnum.ON_BEGIN);
                break;
            case 2:
                setState(FlashEnum.ON_END);
                break;
            case 3:
                setState(FlashEnum.ON_SINGLE);
                break;
            default:
                throw new IllegalArgumentException("Arg must be 0-3");
        }
    }

    //Change the button state to HIGHLIGHT
    public void setHighlighted() {
        setState(FlashEnum.HIGHLIGHT);
    }

    //Change the button state to OFF
    public void setOff() { setState(FlashEnum.OFF);}
}