package com.example.owner.wellcalculator.app;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.owner.wellcalculator.R;

/**
 * Created by Zhao Heng Wen on 2017-03-20.
 * This class is used to resize toast messages
 */

public class ResizeToast {

    //Call this if the text to be displayed is simply string
    public static void makeText(Context context, String text, LayoutInflater inflater, View v) {
        View layout = inflater.inflate(R.layout.customtoast, (ViewGroup) v);

        TextView toastText = (TextView) layout.findViewById(R.id.toastText);
        toastText.setText(text);

        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();

    }

    //Call this if the text set in XML so you grab the string ID of it
    public static void makeText(Context context, int textID, LayoutInflater inflater, View v) {
        View layout = inflater.inflate(R.layout.customtoast, (ViewGroup) v);

        TextView toastText = (TextView) layout.findViewById(R.id.toastText);
        toastText.setText(textID);

        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();

    }
}
