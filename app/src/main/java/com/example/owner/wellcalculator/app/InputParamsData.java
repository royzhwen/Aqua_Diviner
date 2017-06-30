package com.example.owner.wellcalculator.app;

/**
 * Created by Zhao Heng Wen on 6/6/2016.
 * This JavaBean class is used by both InputParams (Input Parameters page) and InputParamsAdv (Input Parameters - Adv page)
 */
public class InputParamsData {
    private String fieldName;
    private float fieldMinValue;
    private float fieldValue;
    private float fieldMaxValue;
    private String unit;
    private boolean isAdvanced;
    public InputParamsData(String fieldName, float fieldMinValue, float fieldValue, float fieldMaxValue, String unit, boolean isAdvanced) {
        this.setFieldName(fieldName);
        this.setFieldMinValue(fieldMinValue);
        this.setFieldValue(fieldValue);
        this.setFieldMaxValue(fieldMaxValue);
        this.setIsAdvanced(isAdvanced);
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public float getFieldMinValue() {
        return fieldMinValue;
    }

    public void setFieldMinValue(float fieldValue) {
        this.fieldMinValue = fieldMinValue;
    }

    public float getFieldValue() {
        return fieldValue;
    }

    public void setFieldValue(float fieldValue) {
        this.fieldValue = fieldValue;
    }

    public float getFieldMaxValue() {
        return fieldValue;
    }

    public void setFieldMaxValue(float fieldValue) {
        this.fieldMaxValue = fieldMaxValue;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public boolean getIsAdvanced() {
        return isAdvanced;
    }

    public void setIsAdvanced(boolean isAdvanced) {
        this.isAdvanced = isAdvanced;
    }
}