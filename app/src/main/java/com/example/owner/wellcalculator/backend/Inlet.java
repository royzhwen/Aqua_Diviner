package com.example.owner.wellcalculator.backend;
/**
 * Created by Lorenzo on 1/24/2017.
 */
public class Inlet {
    //CLASS PARAMETERS
    public double P;
    public double x; //Maybe advanced parameter?
    public double Flow;

    //CLASS METHODS
    // Creator
    public Inlet() {
        this.P = 0;
        this.x = 0;
        this.Flow = 0;
    }

    //Set From List
    public void setAll(String[] ListElements) {
        this.P = Double.parseDouble(ListElements[1]);
        this.x = Double.parseDouble(ListElements[2]);
        this.Flow = Double.parseDouble(ListElements[3]);
    }

}