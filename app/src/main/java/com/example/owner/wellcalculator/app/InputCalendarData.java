package com.example.owner.wellcalculator.app;

/**
 * Created by Zhao Heng Wen on 7/3/2016.
 */
public class InputCalendarData implements Comparable<InputCalendarData> {
    private int startDay;
    private int endDay;
    private float pressure;
    private float flowRate;
    public InputCalendarData(int startDay, int endDay, float pressure, float flowRate) {
        this.setStartDay(startDay);
        this.setEndDay(endDay);
        this.setPressure(pressure);
        this.setFlowRate(flowRate);
    }

    public int getStartDay() {
        return startDay;
    }

    public void setStartDay(int startDay) {
        this.startDay = startDay;
    }

    public int getEndDay() {
        return endDay;
    }

    public void setEndDay(int endDay) {
        this.endDay = endDay;
    }

    public float getPressure() {
        return pressure;
    }

    public void setPressure(float pressure) {
        this.pressure = pressure;
    }

    public float getFlowRate() {
        return flowRate;
    }

    public void setFlowRate(float flowRate) {
        this.flowRate = flowRate;
    }

    @Override
    public int compareTo(InputCalendarData another) {
        return startDay - another.startDay;
    }

    public boolean isEqualTo(int startDay, int endDay) {
        return (this.startDay == startDay && this.endDay == endDay);
    }
}
