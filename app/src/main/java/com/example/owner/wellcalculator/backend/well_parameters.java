package com.example.owner.wellcalculator.backend;


/**
 * Created by Lorenzo on 1/24/2017.
 */
public class well_parameters {
    //CLASS PARAMETERS
    //Well Geometry
    //All 5 well parameters
    public double TVD;
    public double Length;
    public double Surface_Casing;
    public double Intermediate_Casing;
    public double Slotted_Liner;

    //All 20 advanced parameters
    //Well Diameters
    public double ID_InnerTubing;
    public double OD_InnerTubing;
    public double ID_OuterTubing;
    public double OD_OuterTubing;
    public double ID_IntermediateCasing;
    public double OD_IntermediateCasing;
    public double OD_ProductionCement;
    public double ID_SurfaceCasing;
    public double OD_SurfaceCasing;
    public double OD_SurfaceCement;
    public double ID_LowerTubing;
    public double OD_LowerTubing;
    public double ID_SlottedLiner;
    public double OD_SlottedLiner;
    //Material Properties
    public double Steel;
    public double ClassGCement;
    public double Soil;
    public double SoilDry;
    public double Sand;
    public double Formation_Temp;

    public double ret_frac; //26th parameter (extra wierd 1)


    //These are set inside code
    //Time
    public double Time; //TODO: Inlet_conditions: day, pressure, quality, volumetric flow rate - pressure/flow rate I know from app
    //Time is day from inlet_conditions but it was inside the code - day is the calendar I know from app

    //Flow Variables
    public double mfr_in;  //volumetric flow rate - from app

    //CLASS METHODS
    // Creator
    public void SetFromList(double[] WellParamaterList) {
        //Well Geometry
        TVD = WellParamaterList[0];
        Surface_Casing = WellParamaterList[1];
        Intermediate_Casing = WellParamaterList[2];
        Slotted_Liner = WellParamaterList[3];
        Length = WellParamaterList[4];

        //Well Diameters
        ID_InnerTubing = WellParamaterList[5];
        OD_InnerTubing = WellParamaterList[6];
        ID_OuterTubing = WellParamaterList[7];
        OD_OuterTubing = WellParamaterList[8];
        ID_IntermediateCasing = WellParamaterList[9];
        OD_IntermediateCasing = WellParamaterList[10];
        OD_ProductionCement = WellParamaterList[11];
        ID_SurfaceCasing = WellParamaterList[12];
        OD_SurfaceCasing = WellParamaterList[13];
        OD_SurfaceCement = WellParamaterList[14];
        ID_LowerTubing = WellParamaterList[15];
        OD_LowerTubing = WellParamaterList[16];
        ID_SlottedLiner = WellParamaterList[17];
        OD_SlottedLiner = WellParamaterList[18];

        //Material Properties
        Steel = WellParamaterList[19];
        ClassGCement = WellParamaterList[20];
        Soil = WellParamaterList[21];
        SoilDry = WellParamaterList[22];
        Sand = WellParamaterList[23];
        Formation_Temp = WellParamaterList[24];// - 273.15;
        ret_frac = WellParamaterList[25];

        //Time
        //Time = Double.parseDouble(WellParamaterList[25]);

        //Flow Variables
        //mfr_in = Double.parseDouble(WellParamaterList[26]);
        //ret_frac = Double.parseDouble(WellParamaterList[27]);
    }

    public well_parameters() {
        this.TVD = 0;
        this.Length = 0;
        this.Surface_Casing = 0;
        this.Intermediate_Casing = 0;
        this.Slotted_Liner = 0;
        this.ID_InnerTubing = 0;
        this.OD_InnerTubing = 0;
        this.ID_OuterTubing = 0;
        this.OD_OuterTubing = 0;
        this.ID_IntermediateCasing = 0;
        this.OD_IntermediateCasing = 0;
        this.OD_ProductionCement = 0;
        this.ID_SurfaceCasing = 0;
        this.OD_SurfaceCasing = 0;
        this.OD_SurfaceCement = 0;
        this.ID_LowerTubing = 0;
        this.OD_LowerTubing = 0;
        this.ID_SlottedLiner = 0;
        this.OD_SlottedLiner = 0;
        this.Steel = 0;
        this.ClassGCement = 0;
        this.Soil = 0;
        this.SoilDry = 0;
        this.Sand = 0;
        this.Formation_Temp = 0;
        this.Time = 0;
        this.mfr_in = 0;
        this.ret_frac = 0;
    }
}