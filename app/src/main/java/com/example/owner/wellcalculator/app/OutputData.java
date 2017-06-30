package com.example.owner.wellcalculator.app;

/**
 * Created by Zhao Heng Wen on 2017-03-11.
 */

public class OutputData {
    private int tsolve;
    private double volumetricFlowRate;
    private double P; //Pascal
    private double T; //Kelvin
    private double h; //J/kg
    private double x; //no unit
    public OutputData(int tsolve, double volumetricFlowRate, double P, double T, double h, double x) {
        this.setTsolve(tsolve);
        this.setVolumetricFlowRate(volumetricFlowRate);
        this.setP(P);
        this.setT(T);
        this.setH(h);
        this.setX(x);
    }

    public double getVolumetricFlowRate() {
        return volumetricFlowRate;
    }

    public void setVolumetricFlowRate(double volumetricFlowRate) {
        this.volumetricFlowRate = volumetricFlowRate;
    }

    public int getTsolve() {
        return tsolve;
    }

    public void setTsolve(int tsolve) {
        this.tsolve = tsolve;
    }

    public double getP() { return P; }

    public void setP(double p) {
        P = p;
    }

    public double getT() {
        return T;
    }

    public void setT(double t) {
        T = t;
    }

    public double getH() { return h; }

    public void setH(double h) {
        this.h = h;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }
}
