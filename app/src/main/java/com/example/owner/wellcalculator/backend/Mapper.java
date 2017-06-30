package com.example.owner.wellcalculator.backend;

import android.content.Context;
import android.util.Log;

import com.example.owner.wellcalculator.app.InputCalendarDao;
import com.example.owner.wellcalculator.app.InputCalendarData;
import com.example.owner.wellcalculator.app.InputParamsAdvDao;
import com.example.owner.wellcalculator.app.InputParamsDao;
import com.example.owner.wellcalculator.app.InputParamsData;

import java.util.List;

/**
 * Created by Zhao Heng Wen on 2017-03-04.
 * This class is used to map app data to back-end inputs
 */

public class Mapper {
    private List<InputCalendarData> calendarDataList;
    private InputParamsData inputParams[];
    private InputParamsData inputParams_advanced[];

    private InputCalendarDao inputCalendarDao;
    private InputParamsDao inputParamsDao;
    private InputParamsAdvDao inputParamsDao_advanced;
    public Mapper(Context context) {
        inputCalendarDao = new InputCalendarDao(context);
        inputParamsDao = new InputParamsDao(context);
        inputParamsDao_advanced = new InputParamsAdvDao(context);

        calendarDataList = inputCalendarDao.readInputs();
        inputParams = inputParamsDao.readInputs();
        inputParams_advanced = inputParamsDao_advanced.readInputs();
    }

    public int get_firstCalendarDay() {
        if (calendarDataList == null) return -1;
        Log.d("~~~~tstart ", "" + calendarDataList.get(0).getStartDay());
        return calendarDataList.get(0).getStartDay();
    }

    public int get_lastCalendarDay() {
        if (calendarDataList == null) return -1;
        Log.d("~~~~tmax ", "" + calendarDataList.get(calendarDataList.size()-1).getEndDay());
        return calendarDataList.get(calendarDataList.size()-1).getEndDay();
    }
    public Inlet[] getInlet() {
        if (calendarDataList == null || inputParams_advanced == null) return null;
        Inlet[] IC = new Inlet[100];
        double quality = inputParams_advanced[inputParams_advanced.length-1].getFieldValue();
        int count = 0;
        int firstDay = get_firstCalendarDay();
        int lastDay = get_lastCalendarDay();

        //Fill in Inlet data before first day with first day's data
        if (firstDay > 0) {
            for (int i = 1; i < firstDay; i++) {
                IC[count] = new Inlet();
                IC[count].P = calendarDataList.get(0).getPressure() * 1000000;
                IC[count].x = quality;
                IC[count].Flow = calendarDataList.get(0).getFlowRate();
                Log.d("~~~~IC ", count + " " + IC[count].P + " " + IC[count].x + " " + IC[count].Flow);
                count++;
            }
        }

        int prevEndDay = -1;
        double prevPressure = -1;
        double prevFlowRate = -1;
        for (InputCalendarData calendarData: calendarDataList) {
            int startDay = calendarData.getStartDay();
            int endDay = calendarData.getEndDay();

            //Fill Inlet gaps with same data as prevEndDay
            if (prevEndDay != -1 && startDay-prevEndDay > 0) {
                for (int i = prevEndDay+1; i < startDay; i++) {
                    IC[count] = new Inlet();
                    IC[count].P = prevPressure;
                    IC[count].x = quality;
                    IC[count].Flow = prevFlowRate;
                    Log.d("~+~~IC ", count + " " + IC[count].P + " " + IC[count].x + " " + IC[count].Flow);
                    count++;
                }
            }

            for (int i = startDay; i <= endDay; i++) {
                IC[count] = new Inlet();
                IC[count].P = calendarData.getPressure() * 1000000;
                IC[count].x = quality;
                IC[count].Flow = calendarData.getFlowRate();
                Log.d("~~~~IC ", count + " " + IC[count].P + " " + IC[count].x + " " + IC[count].Flow);
                count++;
            }

            prevEndDay = endDay;
            prevPressure = calendarData.getPressure() * 1000000;
            prevFlowRate = calendarData.getFlowRate();
        }

        //Fill in Inlet data after the last day with last day's data
        if (lastDay < 100) {
            for (int i = lastDay; i < 100; i++) {
                IC[count] = new Inlet();
                IC[count].P = calendarDataList.get(calendarDataList.size()-1).getPressure() * 1000000;
                IC[count].x = quality;
                IC[count].Flow = calendarDataList.get(calendarDataList.size()-1).getFlowRate();
                Log.d("~~~~IC ", count + " " + IC[count].P + " " + IC[count].x + " " + IC[count].Flow);
                count++;
            }
        }
        return IC;
    }

    public well_parameters getParameters() {
        if (inputParams == null || inputParams_advanced == null) return null;
        Log.d("~~~inputParams length ", "" + inputParams.length);
        Log.d("~~~inputParams_adv len ", "" + inputParams_advanced.length);
        int numParams = inputParams.length+inputParams_advanced.length;
        double[] wellParamaterList = new double[numParams];
        for (int i = 0; i < inputParams.length; i++) {
            wellParamaterList[i] = inputParams[i].getFieldValue();
            Log.d("~~~~wellParameterList ", i + " " + " " + inputParams[i].getFieldName() + " " + wellParamaterList[i]);
        }
        for (int i = inputParams.length; i < numParams; i++) {
            wellParamaterList[i] = inputParams_advanced[i-inputParams.length].getFieldValue();
            Log.d("~~~~wellParameterList ", i + " " + inputParams_advanced[i-inputParams.length].getFieldName() + " " + wellParamaterList[i]);
        }
        well_parameters well_params = new well_parameters();
        well_params.SetFromList(wellParamaterList);

        return well_params;
    }

    public void closeConns() {
        inputCalendarDao.close();
        inputParamsDao.close();
        inputParamsDao_advanced.close();
    }
}
