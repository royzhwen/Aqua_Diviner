package com.example.owner.wellcalculator.backend;

import android.content.Context;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.owner.wellcalculator.app.OutputDao;
import com.example.owner.wellcalculator.app.OutputData;

public class Calculator {

    //List Global Variables Here

    well_parameters well;
    well_node[] nodes;
    int ncount;
    Inlet[] IC;
    double ds;
    //double ret_frac; -Now unused
    double CplH2O;
    double Pres_tol;
    double xres_tol;
    double hres_tol;
    double[] mfr;

    public int Well_Full_solution(Context context, int tsolve){
        Mapper mapper = new Mapper(context);
        int tstart = 10;
        int tmax = 100;
        IC = mapper.getInlet();
        well = mapper.getParameters();
        mapper.closeConns();
        if (IC == null || well == null) {
            return -1;  //MainPage_CalculateScreen checks that -1 means null data
        }

        boolean Load_Iniitialization = false;
        boolean Save_Data = false;
        boolean single_run = true;
        //font=20;

        // Required inputs
        ds = 10; // nodal spacing
        //ret_frac = 0.9; // Fraction of water that returns (assume const for now) //TODO: This is the 26th parameter - DONE
        CplH2O = 4.14e3;
        Pres_tol = 1e2; // in Pa
        xres_tol = 1e-3;
        hres_tol = 1e3; // in J

        // Read in Well information
        //TODO: TO BE REPLACED WITH CALL TO THE OBJECT DATABASE - DONE
        //well = new well_parameters();
        //well.setFromList(wellParamaterList); //Only reading the 25 parameters
        //well=well_parameters(Param_File); % store well paraemters
        ncount = (int) Math.ceil((well.Length)*2/ds+2); // determine number of nodes
        nodes = new well_node[ncount]; // define grid spacing
        //mnodes?


        //Set OTHER Well Parameters manually
        //TODO: well.ret_frac is from advanced parameters - DONE
        //well.ret_frac = ret_frac;
        well.Formation_Temp = well.Formation_Temp-273.15;

        if (Load_Iniitialization) {
            //load(Guess_File);
        } else {
            Initialize_Nodes(tstart);
        }

        // trace=figure(3);
        // clf;
        // hold on;
        double[] Qtf_net = new double[tmax];
        double[] Qtf_net_2 = new double[tmax];
        double[] Qin = new double[tmax];
        double[] mfr_best = new double[tmax];
        mfr_best[tsolve-1-1] = IC[tsolve-1].Flow/3.6;
        if (single_run) {
            int t = tsolve; //TODO: here, mfr_best index -> change to tsolve (passed in) - DONE
            //disp(strcat('day ', num2str(t)));
            well.Time = t;
            OutputDao outputDao = new OutputDao(context);
            double vfr = Well_SS_Solution_SATURATED_H2O(t, mfr_best); //NOTE: WILL NEED TO ENSURE REQUIRED VARIABLES ARE GLOBAL
            OutputData[] outputData = new OutputData[nodes.length];
            for (int i = 0; i < outputData.length; i++) {
                outputData[i] = new OutputData(tsolve, vfr, nodes[i].P, nodes[i].T, nodes[i].h, nodes[i].x);
            }
            outputDao.writeOutputs(outputData);
            outputDao.close();
            return 0;
        } else {
            //THIS DOES NOT WORK PROPERLY FOR APP YET, ONLY single_run IS USED FOR APP
            for (int t = tstart; t <= tmax; t++) {
                //disp(strcat('day ', num2str(t)));
                well.Time = t;
                Well_SS_Solution_SATURATED_H2O(t, mfr_best);  //NOTE: WILL NEED TO ENSURE REQUIRED VARIABLES ARE GLOBAL
                Qin[t - 1] = nodes[1 - 1].h * mfr[1 - 1] * 86400 / 1e9;
                Qtf_net_2[t - 1] = Qin[t - 1] - nodes[ncount - 1].h * mfr[ncount - 1] * 86400 / 1e9;
            }

        }
        if (Save_Data) {
            //save(Guess_File, 'nodes');
        }
        return -2;
    }


    public double Deff(double Dh, double s, well_parameters well) {
        //This function caluclates effective diameter for an annalus
        //   Based on white eqn 6.76, 6.77
        double a;
        if ((2 * well.Length - s)<(well.Surface_Casing + well.Intermediate_Casing)) {
            a = well.ID_OuterTubing / 2;
        } else {
            a = well.ID_SlottedLiner / 2;
        }
        double b = well.OD_InnerTubing / 2;

        double zeta = Math.pow(a-b,2)*(Math.pow(a,2)-Math.pow(b,2))/(Math.pow(a,4)-Math.pow(b,4)-Math.pow(Math.pow(a,2)-Math.pow(b,2),2)/Math.log(a/b));
        return Dh / zeta;
    }

    public double[] Flow_Data(double s, double rho) {
        //This function returns the hydraulic diameter based on distance along well
        //   Detailed explanation goes here
        double flow = well.mfr_in / rho;
        double deltaz = (well.TVD - well.Surface_Casing);
        double arc_length = Math.min(well.Intermediate_Casing, deltaz * Math.PI / 2);
        double rad = 2 * arc_length / Math.PI;
        double dtheta = Math.PI * ds / (2 * arc_length);

        double Dh;
        double dz;
        double theta;
        double v;
        if (s <= (well.Length - well.Slotted_Liner)) { // travelling down pipe
            Dh = well.ID_InnerTubing;
            if (s <= well.Surface_Casing) {
                dz = -ds;
            } else {
                if (s - well.Surface_Casing < arc_length) {
                    theta = Math.PI * (s - well.Surface_Casing) / (2 * arc_length);
                    dz = -rad * (Math.sin(theta) - Math.sin(theta - dtheta));
                } else {
                    dz = 0;
                }
            }
            v = flow / (0.25 * Math.PI * Math.pow(well.ID_InnerTubing, 2));
        } else if (s <= (well.Length)) { // travelling in internal horizontal pipe
            Dh = well.ID_InnerTubing;
            dz = 0;
            v = flow / (0.25 * Math.PI * Math.pow(well.ID_InnerTubing, 2));
        } else if (s <= (well.Length + well.Slotted_Liner)) { //travelling in slotted pipe
            Dh = (well.ID_SlottedLiner - well.OD_InnerTubing); // From White pg 385
            dz = 0;
            flow = well.mfr_in / rho * (1 - (1 - well.ret_frac) * (s - well.Length) / well.Slotted_Liner); //assume linear distribution of mass lost to formation
            v = flow / (0.25 * Math.PI * (Math.pow(well.ID_SlottedLiner, 2) - Math.pow(well.OD_InnerTubing, 2)));
        } else {
            Dh = (well.ID_OuterTubing - well.OD_InnerTubing); // From White pg 385
            if ((2 * well.Length - s) < well.Surface_Casing) {
                dz = ds;
            } else {
                if (s + arc_length > 2 * well.Length - well.Surface_Casing) {
                    theta = Math.PI * (s - well.Length - well.Slotted_Liner - (well.Intermediate_Casing - arc_length)) / (2 * arc_length);
                    dz = rad * (Math.sin(theta) - Math.sin(theta - dtheta));
                } else {
                    dz = 0;
                }
            }
            flow = flow * well.ret_frac;
            v = flow / (0.25 * Math.PI * (Math.pow(well.ID_OuterTubing,2) - Math.pow(well.OD_InnerTubing ,2)));
        }

        double[] ArrayRet = new double[3];
        ArrayRet[0] = Dh;
        ArrayRet[1] = dz;
        ArrayRet[2] = v;
        return ArrayRet;
    }

    public void Initialize_Nodes(int t) {
        double[] ArrayRet;

        nodes[0] = new well_node();
        nodes[1-1].P = IC[t-1].P;
        nodes[1-1].x = IC[t-1].x;
        nodes[1-1].Q_tf = 0; nodes[1-1].Q_hx = 0; nodes[1-1].Q_conv = 0;
        nodes[1-1].T = Tsat_asf_P_H2O(nodes[1-1].P);
        ArrayRet = rhohmu_asf_Px_H2O(nodes[1-1].P,nodes[1-1].x);
        nodes[1-1].rho = ArrayRet[0]; nodes[1-1].h = ArrayRet[1]; nodes[1-1].mu = ArrayRet[2];

        well.mfr_in = IC[t-1].Flow/3.6;
        ArrayRet = Flow_Data(0,nodes[1-1].rho);
        nodes[1-1].v = ArrayRet[2];
        nodes[1-1].Ts = nodes[1-1].T;
        ArrayRet = rhohmu_asf_Px_H2O(nodes[1-1].P,0);
        nodes[1-1].h = ArrayRet[1];

        double Pout = 0.95*nodes[1-1].P;
        double xout = 0.95*nodes[1-1].x;

        for (int n=1; n <= ncount; n++) {
            if (n != 1) nodes[n - 1] = new well_node();
            nodes[n - 1].P = nodes[1 - 1].P - (nodes[1 - 1].P - Pout) / (ncount * ds) * (n * ds);
            nodes[n - 1].x = nodes[1 - 1].x - (nodes[1 - 1].x - xout) / (ncount * ds) * (n * ds);
            nodes[n - 1].Q_tf = 0;
            nodes[n - 1].Q_hx = 0;
            nodes[n - 1].Q_conv = 0;
            nodes[n - 1].T = Tsat_asf_P_H2O(nodes[n - 1].P);
            ArrayRet = rhohmu_asf_Px_H2O(nodes[n - 1].P, nodes[n - 1].x);
            nodes[n - 1].rho = ArrayRet[0];
            nodes[n - 1].h = ArrayRet[1];
            nodes[n - 1].mu = ArrayRet[2];
            ArrayRet = Flow_Data(0, nodes[n - 1].rho);
            nodes[n - 1].v = ArrayRet[2];
            nodes[n - 1].Ts = nodes[n - 1].T;
            ArrayRet = rhohmu_asf_Px_H2O(nodes[n - 1].P, 0);
            nodes[n - 1].hf = ArrayRet[1];
//            if (n <= ncount-2) {
//                mnodes[n-1] = nodes[n-1];
//            }
            //TODO: uncomment
        }
        int test=2;
    }

    public well_node[] navg_init() {
        int m=0;
        well_node[] mnodes = new well_node[ncount-1];

        for (int n = 1; n <= ncount-1; n++) {
            if (n < ncount/2) {
                m = n;
            } else if (n == ncount/2) {
                m = n;
                n = n+1;
            } else if (n > ncount/2) {
                m = n-1;
            }
            mnodes[m-1] = new well_node();
            mnodes[m-1].P = 0.5*(nodes[n-1].P+nodes[n+1-1].P);
            mnodes[m-1].T = 0.5*(nodes[n-1].T+nodes[n+1-1].T);
            mnodes[m-1].h = 0.5*(nodes[n-1].h+nodes[n+1-1].h);
            mnodes[m-1].x = 0.5*(nodes[n-1].x+nodes[n+1-1].x);
            mnodes[m-1].rho = 0.5*(nodes[n-1].rho+nodes[n+1-1].rho);
            mnodes[m-1].mu = 0.5*(nodes[n-1].mu+nodes[n+1-1].mu);
            mnodes[m-1].Ts = 0.5*(nodes[n-1].Ts+nodes[n+1-1].Ts);
            mnodes[m-1].hf = 0.5*(nodes[n-1].hf+nodes[n+1-1].hf);
        }
        return mnodes;
    }

    public void navg(well_node[] mnodes) {
        int m=0;
        for (int n = 1; n <= ncount-1; n++) {
            if (n < ncount/2) {
                m = n;
            } else if (n == ncount/2) {
                m = n;
                n = n+1;
            } else if (n > ncount/2) {
                m = n-1;
            }
            mnodes[m-1].P = 0.5*(nodes[n-1].P+nodes[n+1-1].P);
            mnodes[m-1].T = 0.5*(nodes[n-1].T+nodes[n+1-1].T);
            mnodes[m-1].h = 0.5*(nodes[n-1].h+nodes[n+1-1].h);
            mnodes[m-1].x = 0.5*(nodes[n-1].x+nodes[n+1-1].x);
            mnodes[m-1].rho = 0.5*(nodes[n-1].rho+nodes[n+1-1].rho);
            mnodes[m-1].mu = 0.5*(nodes[n-1].mu+nodes[n+1-1].mu);
            mnodes[m-1].Ts = 0.5*(nodes[n-1].Ts+nodes[n+1-1].Ts);
            mnodes[m-1].hf = 0.5*(nodes[n-1].hf+nodes[n+1-1].hf);
            mnodes[m-1].v = 0.5*(nodes[n-1].v+nodes[n+1-1].v);
        }
    }

    public double Pdrop(double Dh, double dz, well_node node_last, well_node node_avg) {
        //UNTITLED2 Summary of this function goes here
        //   Detailed explanation goes here
        double v = node_last.v;
        double rho = node_avg.rho;
        double epsilon = 0.002e-3; //New Stainless Steel Pipe, from White
        double g = 9.81;
        double Re = v * Dh * rho / node_avg.mu;
        double f;
        if (Re < 2300) {
            f = 64.0 / Re;
        } else {
            f = Math.pow(-1.8 * Math.log10(64 / Re + Math.pow(epsilon / (3.7 * Dh),1.11)),-2); //Haaland Correlation
        }
        return node_last.P - 1 / 2.0 * rho * Math.pow(v,2) * ds * f / Dh - g * rho * dz;
    }

    public double[] Prandtl(double P) {
        //UNTITLED4 Summary of this function goes here
        //   Detailed explanation goes here
        double PkPa = P/1000;
        double Pr = 9.4744*Math.pow(PkPa,-0.364);
        double Prg = 0.8151*Math.pow(PkPa,0.0419);
        double[] ArrayRet = new double[] {Pr, Prg};
        return ArrayRet;
    }

    public double q_formation(well_parameters well_parameters, double Ts, double s) {
        double Qf;
        //UNTITLED2 Summary of this function goes here
        //Detailed explanation goes here

	/*
	qreturn=%overal energy returning from the previous iteration.
	My previous model cuts up the inital overall energy return calculation into 7 parts
	by multiplying total energy returned by
	0.1,0.1,0.1,0.1,0.2,0.2,0.2. which correspond to the length of the
	nodes being analyzed. This idea will need to be dramatically
	improved especially since the number of nodes is not static
	*/

        Ts = Ts-273.15; // This code was written using T in C

        double Temperature_casing = (12.291 * Math.log(well_parameters.Time) + (Ts - 120));


        double dummyCH4_rho=0.421;
        double dummyCH4_cp=2260;
        double dummyCH4_k=0.035;
        double dummyCH4_mu=0.011e-3;

        double k_effective = 0;
        double Ra = (9.81 * (1/((Temperature_casing + Ts)/2 + 273.15)) * Math.pow(dummyCH4_rho,2) * dummyCH4_cp /(dummyCH4_k * dummyCH4_mu)) * Math.pow((well_parameters.ID_IntermediateCasing - well_parameters.OD_OuterTubing)/2,3) * (Ts-Temperature_casing);
        if (Ra > 6000 && Ra <= 200000) {

            k_effective = 0.13 * Math.pow(Ra,0.25) * dummyCH4_rho;

        } else if (Ra > 200000) {

            k_effective = 0.048 * Math.pow(Ra,1/3) * dummyCH4_rho;

        }

        double R_convection = k_effective/(well_parameters.OD_OuterTubing * Math.log(well_parameters.ID_IntermediateCasing / well_parameters.OD_OuterTubing));

        double R_radiation =(5.67*Math.pow(10,-8)*(Math.pow(Ts+273.15,2)+Math.pow(Temperature_casing+273.15,2))*(Ts+237.15+Temperature_casing+273.15))/(1/0.8+((well_parameters.OD_OuterTubing/well_parameters.ID_IntermediateCasing)*(1/0.8-1)));


        if (s <= well_parameters.Intermediate_Casing+well_parameters.Surface_Casing) {

            if (s <= well_parameters.Surface_Casing) {

                double R_metal_outer_tubing = ((Math.log(well_parameters.OD_OuterTubing / well_parameters.ID_OuterTubing)) / (2 * Math.PI * well_parameters.Steel)); // Thermal resistance of steel outer tubing
                double R_metal_intermediate_casing = ((Math.log(well_parameters.OD_IntermediateCasing / well_parameters.ID_IntermediateCasing)) / (2 * Math.PI * well_parameters.Steel)); // Thermal resistance of steel intermediate casing
                double R_concrete_intermediate_casing = ((Math.log(well_parameters.OD_ProductionCement / well_parameters.OD_IntermediateCasing)) / (2 * Math.PI * well_parameters.Steel)); // Thermal resistance of production concrete
                double R_metal_surface_casing = ((Math.log(well_parameters.OD_SurfaceCasing / well_parameters.ID_SurfaceCasing)) / (2 * Math.PI * well_parameters.Steel)); // Thermal resistance of steel surface casing
                double R_concrete_surface_casing = ((Math.log(well_parameters.OD_SurfaceCement / well_parameters.OD_SurfaceCasing)) / (2 * Math.PI * well_parameters.Steel)); // Thermal resistance of surface concrete
                double R_total = R_metal_outer_tubing + R_metal_intermediate_casing + R_concrete_intermediate_casing + R_metal_surface_casing + R_concrete_surface_casing + 1/(1/R_convection + 1/R_radiation);
                double Radius_effective = well_parameters.ID_IntermediateCasing * Math.exp( -2 * Math.PI * R_total * dummyCH4_k);
                double Z = Math.log((well_parameters.Soil / 2410000) * well_parameters.Time*86400 / Math.pow(Radius_effective,2));
                double q_dimensionless = Math.exp(-0.000629*Math.pow(Z,3) + 0.0203*Math.pow(Z,2) - 0.308*Z - 0.015); //dimensionless energy to formation
                Qf = q_dimensionless * 2 * Math.PI * well_parameters.Soil *(Temperature_casing - well_parameters.Formation_Temp)/1000; //energy to formation conduction
                //disp('surface')

            } else {

                double R_metal_outer_tubing = ((Math.log(well_parameters.OD_OuterTubing / well_parameters.ID_OuterTubing)) / (2 * Math.PI * well_parameters.Steel)); // Thermal resistance of steel outer tubing
                double R_metal_intermediate_casing = ((Math.log(well_parameters.OD_IntermediateCasing / well_parameters.ID_IntermediateCasing)) / (2 * Math.PI * well_parameters.Steel)); // Thermal resistance of steel intermediate casing
                double R_concrete_intermediate_casing = ((Math.log(well_parameters.OD_ProductionCement / well_parameters.OD_IntermediateCasing)) / (2 * Math.PI * well_parameters.Steel)); // Thermal resistance of production concrete
                double R_total = R_metal_outer_tubing + R_metal_intermediate_casing + R_concrete_intermediate_casing + 1/(1/R_convection + 1/R_radiation);
                double Radius_effective = well_parameters.ID_IntermediateCasing * Math.exp( -2 * Math.PI * R_total *dummyCH4_k);
                double Z = Math.log((well_parameters.Soil / 2410000) * well_parameters.Time*86400/ Math.pow(Radius_effective,2));
                double q_dimensionless = Math.exp(-0.000629*Math.pow(Z,3) + 0.0203*Math.pow(Z,2) - 0.308*Z - 0.015); //dimensionless energy to formation
                Qf = q_dimensionless * 2 * Math.PI * well_parameters.Soil *(Temperature_casing - well_parameters.Formation_Temp)/1000; //energy to formation conduction
                //disp('intermediate')

            }

        } else {

            double Z = Math.log((well_parameters.Soil / 2410000) * well_parameters.Time*86400*4 / Math.pow(well_parameters.ID_OuterTubing,2));
            double q_dimensionless = Math.exp(-0.000629*Math.pow(Z,3) + 0.0203*Math.pow(Z,2) - 0.308*Z - 0.015); //dimensionless energy to formation
            // Energy from convection is calculated in main code
            Qf = q_dimensionless * 2 * Math.PI * well_parameters.Soil *(Ts - well_parameters.Formation_Temp) / 1000; // Energy to formation conduction


        }
        Qf = Qf*1000; // Conversion to W
        return Qf;
    }

    public double qHX( well_node ni, well_node no, double s, well_parameters well ) {

        double[] ArrayRet;
        //Calculates heat exchange based on average poperties on ds

        double epsilon = 0.002e-3; //New Stainless Steel Pipe, from White
        // Inner Flow
        double Dh = well.ID_InnerTubing;
        double Re = ni.v*Dh*ni.rho/ni.mu;
        double f;
        if (Re<2300) {
            f = 64/Re;
        } else {
            f = Math.pow((-1.8*Math.log10(64/Re+Math.pow(epsilon/(3.7*Dh),1.11))),-2); // Haaland Correlation
        }

        ArrayRet = Prandtl(ni.P);
        double Pr = ArrayRet[0]; double Prg = ArrayRet[1];
        ArrayRet = ThermalCond(ni.P);
        double k = ArrayRet[0]; double kg = ArrayRet[1];
        // h for sat liquid flowing at 2 phase velocity
        double Nu = ((f/8)*(Re-1000)*Pr)/(1+12.7*Math.pow(f/8,0.5)*(Math.pow(Pr,2/3)-1)); //HT text eqn 8.62 --> Assume turbulent
        double hi = Nu*k/Dh;
        double Ri = 1/(Math.PI*well.ID_InnerTubing*hi);

        // Annulus Flow
        if (s<well.Length-well.Slotted_Liner) {
            Dh = well.ID_OuterTubing-well.OD_InnerTubing;
        } else {
            Dh = well.ID_SlottedLiner-well.OD_InnerTubing;
        }
        double De = Deff(Dh, (2*well.Length-s), well); // Deff uses path length while s is distance from surface
        Re = no.v*De*ni.rho/ni.mu;
        if (Re<2300) {
            f = 64/Re;
        } else {
            f = Math.pow(-1.8*Math.log10(64/Re+Math.pow(epsilon/(3.7*De),1.11)),-2); // Haaland Correlation
        }
        ArrayRet = Prandtl(no.P);
        Pr = ArrayRet[0]; Prg = ArrayRet[1];
        ArrayRet = ThermalCond(no.P);
        k = ArrayRet[0]; kg = ArrayRet[1];
        // HT text pg 555 says eqn 8.60 can be used for rough approx.
        // h is for sat liquid flowing at 2 phase velocity
        Nu = 0.023*Math.pow(Re,4/5)*Math.pow(Pr,0.4); //HT text eqn 8.60 assuming fluid turbulent and being heated
        //Nu=((f/8)*(Re-1000)*Pr)/(1+12.7*(f/8)^0.5*(Pr^(2/3)-1)); //HT text eqn 8.62 --> Assume turbulent
        double ho = Nu*k/Dh;
        double Ro = 1/(Math.PI*well.OD_InnerTubing*hi);

        // Conduction through Steel
        double ks = well.Steel;
        double Rs = Math.log(well.OD_InnerTubing/well.ID_InnerTubing)/(2*Math.PI*ks);

        // Thermal Resistance
        double Rtot = Ri+Rs+Ro;

        // Heat Transfer per unit length (inner to outer)
        double q = (ni.T-no.T)/Rtot;

        q = q/7; // Preliminary results show this is way too high currently

        return q;
    }

    public double Rhx( well_node ni, well_node no, double s) {
        //Calculates heat exchange based on average poperties on ds
        // Accounts for phase change (film condensation)
        // Assumes flow is always turbulent in inner tube (good assumption)

        double[] ArrayRet;

        double epsilon=0.002e-3; //New Stainless Steel Pipe, from White
        // Inner Flow
        ArrayRet = rhohmu_asf_Px_H2O(ni.P,0);
        double rhol = ArrayRet[0]; double mul = ArrayRet[2];
        ArrayRet = rhohmu_asf_Px_H2O(ni.P,1);
        double rhog = ArrayRet[0]; double mug = ArrayRet[2];
        ArrayRet = PrKCpliqH2O_asf_T( ni.T );
        double Pr = ArrayRet[0]; double k = ArrayRet[1]; double Cp = ArrayRet[2];
        double xi = ni.x;
        double Xtt = Math.pow((1-xi)/xi,0.9)*Math.pow(rhog/rhol,0.5)*Math.pow(mul/mug,0.1);
        double Dh = well.ID_InnerTubing;
        double Re = ni.v*Dh*rhol/rhol;

        // h for sat liquid flowing at 2 phase velocity
        double Nu = 0.023*Math.pow(Re,0.8)*Math.pow(Pr,0.3); //HT text eqn 10.51a --> Assume turbulent
        if (ni.x > 0) {
            Nu = Nu*(1+2.22/Math.pow(Xtt,0.89));
        }
        double hi = Nu*k/Dh;
        double Ri = 1.0/(Math.PI*well.ID_InnerTubing*hi);

        // Annulus Flow
        // Assume for now that same correlation holds with D=Deff (BAD ASSUMPTION)
        ArrayRet = rhohmu_asf_Px_H2O(no.P,0);
        rhol = ArrayRet[0]; mul = ArrayRet[2];
        ArrayRet = rhohmu_asf_Px_H2O(no.P,1);
        rhog = ArrayRet[0]; mug = ArrayRet[2];
        ArrayRet = PrKCpliqH2O_asf_T( no.T );
        Pr = ArrayRet[0]; k = ArrayRet[1]; Cp = ArrayRet[2];
        double xo = no.x;
        Xtt = Math.pow((1-xo)/xo,0.9)*Math.pow(rhog/rhol,0.5)*Math.pow(mul/mug,0.1);
        if (s < well.Length-well.Slotted_Liner) {
            Dh = well.ID_OuterTubing-well.OD_InnerTubing;
        } else {
            Dh = well.ID_SlottedLiner-well.OD_InnerTubing;
        }
        double De = Deff(Dh, (2.0*well.Length-s), well); // Deff uses path length while s is distance from surface

        // HT text pg 555 says eqn 8.60 can be used for rough approx.
        // h is for sat liquid flowing at 2 phase velocity
        Nu = 0.023*Math.pow(Re,0.8)*Math.pow(Pr,0.4); //HT text eqn 10.51a --> Assume turbulent
        if (no.x > 0) {
            Nu = Nu*(1+2.22/Math.pow(Xtt,0.89));
        }
        double ho = Nu*k/Dh;

        double Ro = 1.0/(Math.PI*well.OD_InnerTubing*ho);

        // Conduction through Steel
        double ks = well.Steel;
        double Rs = Math.log(well.OD_InnerTubing/well.ID_InnerTubing)/(2.0*Math.PI*ks);

        // Thermal Resistance
        double Rtot = Ri+Rs+Ro;

        // Heat Transfer per unit length (inner to outer)
        return Rtot;
    }

    public double[] KMuCpRhoCH4_asf_PT( double P, double T ) {
        // Returns K [W/m*K], Mu [Pa*s], Cp [kJ/kg*K], and Rho[kg/m^3] given P[MPa], and T [K]

        double K = 0.726497485140845+.91917353841367*Math.pow(P,3)-0.0611305112540873*Math.pow(P,2)*T+2.06936322992467*Math.pow(P,2)-0.00605900840929257*P*Math.pow(T,2)+1.06346583636454*P+3.91716163187304*P*T+0.000132982557016331*Math.pow(T,3)+0.00483977945444599*Math.pow(T,2)+101.601685305705*T;
        K = K/1000000;

        double Mu = 0.340780938748065+0.851513982975149*Math.pow(P,3)-0.0137508262186341*Math.pow(P,2)*T+0.82747157270463*Math.pow(P,2)-0.00159445461891883*P*Math.pow(T,2)+0.479441955982057*P+0.978978022594768*P*T+0.0000307781885744991*Math.pow(T,3)-0.0430004531195175*Math.pow(T,2)+47.7767186438867*T;
        Mu = Mu/1000000000;

        double Cp = 3.39602167394223-0.0000131458042255366*Math.pow(P,3)-0.00000307736285924988*Math.pow(P,2)*T+0.00153257259851214*Math.pow(P,2)+0.000000910347076849216*P*Math.pow(T,2)+0.251087650375154*P-0.000922979591574892*P*T-0.0000000264516563344078*Math.pow(T,3)+0.0000354175650194577*Math.pow(T,2)-0.0120978873755035*T;

        double Rho = 29.0036021069667-0.000729798993456982*Math.pow(P,3)-0.000264496992865383*Math.pow(P,2)*T+0.131609154310263*Math.pow(P,2)+0.0000422807910476635*P*Math.pow(T,2)+16.8506224474993*P-0.0470158230796167*P*T-0.00000044979968233829*Math.pow(T,3)+0.000546678338263273*Math.pow(T,2)-0.219213613590572*T;

        double[] ArrayRet = {K, Mu, Cp, Rho};
        return ArrayRet;
    }

    public double Tsat_asf_P_H2O(double P){
        P = P/1000000;
        return 0.0000244184384701362*Math.pow(P,11)-0.00108420385793794*Math.pow(P,10)+0.0213393996668494*Math.pow(P,9)-0.245441348757407*Math.pow(P,8)+1.83155777420884*Math.pow(P,7)-9.31359921993055*Math.pow(P,6)+33.017992751218*Math.pow(P,5)-82.2413113469587*Math.pow(P,4)+143.924021467235*Math.pow(P,3)-178.384597620861*Math.pow(P,2)+177.135171031893*P+367.291408078882;
    }

    public double[] rhohmu_asf_Px_H2O(double P, double x) {
        P=P/1000000;
        double rho_f = -0.0000164374384414826*Math.pow(P,11)+0.000730744194280826*Math.pow(P,10)  -0.0144048097542238*Math.pow(P,9)+    0.166008359882987*Math.pow(P,8)     -1.2420262620238*Math.pow(P,7)+       6.33826870447972*Math.pow(P,6)      -22.5852735870998*Math.pow(P,5)+      56.7023705766503*Math.pow(P,4)-      100.597925625923*Math.pow(P,3)+      128.216233273151*Math.pow(P,2)-      145.443027205176*P+      965.583385291686;
        double rhof =  -0.0000164374384414826*Math.pow(P,11)+0.00073074419428082600*Math.pow(P,10)-0.0144048097542238000*Math.pow(P,9) +0.16600835988298700000*Math.pow(P,8)-1.2420262620238000000*Math.pow(P,7) +6.33826870447972000000*Math.pow(P,6)-22.585273587099800000*Math.pow(P,5) +56.7023705766503000000*Math.pow(P,4)-100.59792562592300000*Math.pow(P,3) +128.216233273151000000*Math.pow(P,2)-145.44302720517600000*P +965.583385291686000000;
        double rho_g = 0.0000000717689409904053*Math.pow(P,11)-0.00000320691881722679*Math.pow(P,10)+0.0000636238926979434*Math.pow(P,9)-0.00073933558020256*Math.pow(P,8)+0.00559293366146635*Math.pow(P,7)-0.0289833150127242*Math.pow(P,6)+0.105639972311835*Math.pow(P,5)-0.274888784417655*Math.pow(P,4)+0.522536010125186*Math.pow(P,3)-0.690214552183696*Math.pow(P,2)+5.43280224037889*P+0.073255227678148;
        double rhog = 0.00000007176894099040530*Math.pow(P,11) -0.0000032069188172267900*Math.pow(P,10)+0.00006362389269794340000*Math.pow(P,9) -0.0007393355802025600000*Math.pow(P,8)+0.00559293366146635000000*Math.pow(P,7) -0.0289833150127242000000*Math.pow(P,6)+0.10563997231183500000000*Math.pow(P,5) -0.2748887844176550000000*Math.pow(P,4)+0.52253601012518600000000*Math.pow(P,3) -0.6902145521836960000000*Math.pow(P,2)+5.43280224037889000000000*P +0.07325522767814800000000;
        double Rho = 1/((1-x)/rho_f+x/rho_g);

        double h_f = 0.000102090088806141*Math.pow(P,11)-0.00453297190163926*Math.pow(P,10)+0.0892200330228671*Math.pow(P,9)-1.02621532719867*Math.pow(P,8)+7.65818293628704*Math.pow(P,7)-38.9442245922197*Math.pow(P,6)+138.07250434884*Math.pow(P,5)-343.952406700036*Math.pow(P,4)+602.067197527536*Math.pow(P,3)-746.550288341729*Math.pow(P,2)+751.609040409678*P+393.527728979599;
        double h_g = 0.0000420246880486199*Math.pow(P,11)-0.00186528252563506*Math.pow(P,10)+0.0366962291135094*Math.pow(P,9)-0.421825163983095*Math.pow(P,8)+3.14528328789198*Math.pow(P,7)-15.975853543941*Math.pow(P,6)+56.5388333256621*Math.pow(P,5)-140.419772910428*Math.pow(P,4)+244.345012307839*Math.pow(P,3)-298.676787407019*Math.pow(P,2)+258.050857876699*P+2670.50090323998;
        double h =  (1-x)*h_f + x*h_g;

        double mu_f = -0.0000000000671610054537775*Math.pow(P,11)+0.00000000297087089355553*Math.pow(P,10)-0.0000000582012605089572*Math.pow(P,9)+0.00000066546077581987*Math.pow(P,8)-0.00000492740950998712*Math.pow(P,7)+0.0000247925122993716*Math.pow(P,6)-0.0000865736185056412*Math.pow(P,5)+0.000210702922062651*Math.pow(P,4)-0.000354521494462581*Math.pow(P,3)+0.000405648415395281*Math.pow(P,2)-0.000315190595262965*P+0.000269681164435062;
        double mu_g = 0.000000000000846697451697828*Math.pow(P,11)-0.0000000000376009409704641*Math.pow(P,10)+0.000000000740226977427665*Math.pow(P,9)-0.0000000085162650372748*Math.pow(P,8)+0.0000000635731738538723*Math.pow(P,7)-0.000000323422345477177*Math.pow(P,6)+0.00000114728665774304*Math.pow(P,5)-0.0000028600979844575*Math.pow(P,4)+0.00000501102481139148*Math.pow(P,3)-0.00000620955631335585*Math.pow(P,2)+0.00000614174944290835*P+0.0000120588114082525;
        double Mu = 1/((1-x)/mu_f+x/mu_g);

        //Log.d("1", rhof + " " + rhog + " " + Rho + " " + h_f + " " + h_g + " " + h + " " + mu_f + " " + mu_g + " " + Mu);
        double[] ArrayRet = new double[3];
        ArrayRet[0] = Rho;
        ArrayRet[1] = h*1000;
        ArrayRet[2] = Mu;
        return ArrayRet;
    }

    public double x_asf_hP_H2O(double h, double P) {
        P = P/1000000;
        h = h/1000;
        double h_f = 0.000102090088806141*Math.pow(P,11)-0.00453297190163926*Math.pow(P,10)+0.0892200330228671*Math.pow(P,9)-1.02621532719867*Math.pow(P,8)+7.65818293628704*Math.pow(P,7)-38.9442245922197*Math.pow(P,6)+138.07250434884*Math.pow(P,5)-343.952406700036*Math.pow(P,4)+602.067197527536*Math.pow(P,3)-746.550288341729*Math.pow(P,2)+751.609040409678*P+393.527728979599;
        double h_g = 0.0000420246880486199*Math.pow(P,11)-0.00186528252563506*Math.pow(P,10)+0.0366962291135094*Math.pow(P,9)-0.421825163983095*Math.pow(P,8)+3.14528328789198*Math.pow(P,7)-15.975853543941*Math.pow(P,6)+56.5388333256621*Math.pow(P,5)-140.419772910428*Math.pow(P,4)+244.345012307839*Math.pow(P,3)-298.676787407019*Math.pow(P,2)+258.050857876699*P+2670.50090323998;

        if (h < h_f) h = h_f;
        return (h-h_f)/(h_g-h_f);
    }

    public double[] PrKCpliqH2O_asf_T( double T ) {
        double k = -0.0052*Math.pow(T,2)+4.2487*T-174.17;
        double Cp = 4e-5*Math.pow(T,2)-0.0303*T+10.29;
        double Pr = 4e-5*Math.pow(T,2)-0.0377*T+10.542;

        double[] ArrayRet = {Pr, k, Cp};
        return ArrayRet;
    }

    public double[] ThermalCond(double Psat) {
        double PkPa = Psat/1000;
        double k = 590.47*Math.pow(PkPa,0.0294);
        double kg = 18.115*Math.pow(PkPa,0.0709);

        double[] ArrayRet = {k, kg};
        return ArrayRet;
    }

    public double Rtf(double Ts, double s) {
	/*
	qreturn=//overal energy returning from the previous iteration.
	My previous model cuts up the inital overall energy return calculation into 7 parts
	by multiplying total energy returned by
	0.1,0.1,0.1,0.1,0.2,0.2,0.2. which correspond to the length of the
	nodes being analyzed. This idea will need to be dramatically
	improved especially since the number of nodes is not static
	*/

        double [] ArrayRet;
        ArrayRet = KMuCpRhoCH4_asf_PT(2,Ts); //2MPa is a typical pressure at well heel
        double kCH4 = ArrayRet[0]; double muCH4 = ArrayRet[1]; double cpCH4 = ArrayRet[2]; double rhoCH4 = ArrayRet[3];
        cpCH4 = cpCH4*1000; //Function outputs Cp in KJ

        Ts = Ts-273.15; // This code was written using T in C

        double Temperature_casing = (12.291 * Math.log(well.Time) + (Ts - 120));

        double Ra = (9.81 * (1/((Temperature_casing + Ts)/2 + 273.15)) * Math.pow(rhoCH4,2) * cpCH4 /(kCH4 * muCH4)) * Math.pow((well.ID_IntermediateCasing - well.OD_OuterTubing)/2,3) * (Ts-Temperature_casing);

        double k_effective = 0;
        if (Ra > 6000 && Ra <=200000) {

            k_effective = 0.13 * Math.pow(Ra,0.25) * rhoCH4;

        } else if (Ra > 200000) {

            k_effective = 0.048 * Math.pow(Ra,1.0/3) * rhoCH4;

        }

        double R_convection = k_effective/(well.OD_OuterTubing * Math.log(well.ID_IntermediateCasing / well.OD_OuterTubing));

        double R_radiation =(5.67*Math.pow(10,-8)*(Math.pow(Ts+273.15,2)+Math.pow(Temperature_casing+273.15,2))*(Ts+237.15+Temperature_casing+273.15))/(1/0.8+((well.OD_OuterTubing/well.ID_IntermediateCasing)*(1/0.8-1)));

        //Variable Decalrations
        double R_metal_outer_tubing;
        double R_metal_intermediate_casing;
        double R_concrete_intermediate_casing;
        double R_metal_surface_casing;
        double R_concrete_surface_casing;
        double R_total;
        double Radius_effective;
        double Z;
        double q_dimensionless;
        double R;


        if (s <= well.Intermediate_Casing + well.Surface_Casing) {

            if (s <= well.Surface_Casing) {

                R_metal_outer_tubing = ((Math.log(well.OD_OuterTubing / well.ID_OuterTubing)) / (2 * Math.PI * well.Steel)); // Thermal resistance of steel outer tubing
                R_metal_intermediate_casing = ((Math.log(well.OD_IntermediateCasing / well.ID_IntermediateCasing)) / (2 * Math.PI * well.Steel)); // Thermal resistance of steel intermediate casing
                R_concrete_intermediate_casing = ((Math.log(well.OD_ProductionCement / well.OD_IntermediateCasing)) / (2 * Math.PI * well.Steel)); // Thermal resistance of production concrete
                R_metal_surface_casing = ((Math.log(well.OD_SurfaceCasing / well.ID_SurfaceCasing)) / (2 * Math.PI * well.Steel)); // Thermal resistance of steel surface casing
                R_concrete_surface_casing = ((Math.log(well.OD_SurfaceCement / well.OD_SurfaceCasing)) / (2 * Math.PI * well.Steel)); // Thermal resistance of surface concrete
                R_total = R_metal_outer_tubing + R_metal_intermediate_casing + R_concrete_intermediate_casing + R_metal_surface_casing + R_concrete_surface_casing + 1 / (1 / R_convection + 1 / R_radiation);
                Radius_effective = well.ID_IntermediateCasing * Math.exp(-2 * Math.PI * R_total * kCH4); //kasfPTCH4(P));
                Z = Math.log((well.Soil / 2410000) * well.Time * 86400 / Math.pow(Radius_effective, 2));
                q_dimensionless = Math.exp(-0.000629 * Math.pow(Z, 3) + 0.0203 * Math.pow(Z, 2) - 0.308 * Z - 0.015); //dimensionless energy to formation
                R = 1 / (q_dimensionless * 2 * Math.PI * well.Soil); //energy to formation conduction

            } else {

                R_metal_outer_tubing = ((Math.log(well.OD_OuterTubing / well.ID_OuterTubing)) / (2 * Math.PI * well.Steel)); // Thermal resistance of steel outer tubing
                R_metal_intermediate_casing = ((Math.log(well.OD_IntermediateCasing / well.ID_IntermediateCasing)) / (2 * Math.PI * well.Steel)); // Thermal resistance of steel intermediate casing
                R_concrete_intermediate_casing = ((Math.log(well.OD_ProductionCement / well.OD_IntermediateCasing)) / (2 * Math.PI * well.Steel)); // Thermal resistance of production concrete
                R_total = R_metal_outer_tubing + R_metal_intermediate_casing + R_concrete_intermediate_casing + 1 / (1 / R_convection + 1 / R_radiation);
                Radius_effective = well.ID_IntermediateCasing * Math.exp(-2 * Math.PI * R_total * kCH4); //* kasfPTCH4(P));
                Z = Math.log((well.Soil / 2410000) * well.Time * 86400 / Math.pow(Radius_effective, 2));
                q_dimensionless = Math.exp(-0.000629 * Math.pow(Z, 3) + 0.0203 * Math.pow(Z, 2) - 0.308 * Z - 0.015); //dimensionless energy to formation
                R = 1 / (q_dimensionless * 2 * Math.PI * well.Soil); //energy to formation conduction
                //disp('intermediate')

            }

        } else {
            Z = Math.log((well.Soil / 2410000) * well.Time*86400*4 / Math.pow(well.ID_OuterTubing,2));
            q_dimensionless = Math.exp(-0.000629*Math.pow(Z,3) + 0.0203*Math.pow(Z,2) - 0.308*Z - 0.015); //dimensionless energy to formation
            // Energy from convection is calculated in main code
            R = 1/(q_dimensionless * 2 * Math.PI * well.Soil);
        }
        return R;
    }

    public double Well_SS_Solution_SATURATED_H2O(int t, double[] mfr_best) {
        double[] ArrayRet;
        well_node[] mnodes = navg_init();

        // Well Solution Parameters
        boolean solved = false;
        well.mfr_in = mfr_best[t-1-1]; // Well MFR

        double alpha = 0.01;//0.05; // Enthalpy Relaxation Factor
        double alphah=0.1;
        double alphaq=0.2;// Heat Transfer Relaxation Factor
        int h_loop=0; // Iteration counter for self-iteration of h matrix
        double Pres_rms; // Pressure Residual initialization
        double xres_rms; // Quality Residual initialization
        double Tf=273.15+well.Formation_Temp; // Well formation Temp

        double[] Pres_hist = new double[100]; //zeros(100,1);
        double[] xres_hist = new double[100]; //zeros(100,1);

        // Initialize Entry Node
        nodes[1-1].P=IC[t-1].P;
        nodes[1-1].x=IC[t-1].x;
        nodes[1-1].T=Tsat_asf_P_H2O(nodes[1-1].P);
        ArrayRet = rhohmu_asf_Px_H2O(nodes[1-1].P,nodes[1-1].x);
        nodes[1-1].rho = ArrayRet[0]; nodes[1-1].h = ArrayRet[1]; nodes[1-1].mu = ArrayRet[2];
        ArrayRet = Flow_Data(0, nodes[1-1].rho);
        nodes[1-1].v = ArrayRet[2];

        // Matrix Initialization
        double[][] H = new double[ncount][ncount]; // Coefficient Matrix
        double[] Q = new double[ncount];  // Solution Vector
        double[] Hsol = new double[ncount]; // Vector to store solution
        mfr = new double[ncount]; //zeros(ncount, 1); // Mass flow rate at each node

        while ( !solved ) {
            int iter = 0; // Well Iteration Counter
            h_loop=0; // Iteration counter for self-iteration of h matrix
            Pres_rms=2*Pres_tol; // Pressure Residual initialization
            xres_rms=2*xres_tol; // Quality Residual initialization
            // Last-iteration vectors
            double[] Tlast = new double[ncount]; //zeros(ncount,1);
            double[] hlast = new double[ncount]; // Used for convergence checking of enthalpy
            double[] hlast3 = new double[ncount];
            double[] hlast2 = new double[ncount]; // Used fo convergence checking locally
            double[] xlast = new double[ncount];

            int nb;
            int m = 0;
            int mb;

            // Find MFR at each node
            // Want to calculate this based on pressure (Darcy's Law)
            for (int n=1; n<=ncount; n++) {
                if ((n-1)*ds<=(well.Length+well.Slotted_Liner) && (n-1)*ds>well.Length) {
                    mfr[n-1]=well.mfr_in*(1-(1-well.ret_frac)*((n-1)*ds-well.Length)/well.Slotted_Liner); // outer flow inlet
                } else if ((n-1)*ds>well.Length) {
                    mfr[n-1]=well.mfr_in*well.ret_frac;
                } else {
                    mfr[n-1]=well.mfr_in;
                }
            }

            // Initialize Heat Fluxes
            for (int n=1 ; n <= ncount/2 - 1; n++) {
                nb=ncount-n+1;
                m=n;
                mb=ncount-n-1;

                // Conduction/Convection heat transfers
                double Reshx = Rhx(nodes[m-1], nodes[mb-1], n*ds); // positive out of inner flow
                double Restf = Rtf(nodes[mb-1].T,n*ds); // heat conduction to formation

                // Assign heat transfers to nodes for inter-iteration storage
                mnodes[m-1].Rhx = Reshx; mnodes[mb-1].Rhx = Reshx;
                mnodes[m-1].Q_tf = 0; mnodes[mb-1].Rtf = Restf;


            }

            while (Pres_rms>Pres_tol || xres_rms>xres_tol) {
                iter=iter+1; // Full well solution iteration counter
                Log.d("2", "Iteration "+ iter);
                Pres_rms=0;
                xres_rms=0;

                // Pressure and Saturated Temp Solution
                for (int n=1; n <= ncount - 1; n++) {
                    if (n<=ncount/2) {
                        m=n-1;
                    } else if (n>ncount/2) {
                        m=n-2;
                    }
                    Tlast[n]=nodes[n].T; // Store last T for convergence checking
                    double Plast = nodes[n].P; // Store last P for convergence checking
                    ArrayRet = Flow_Data((n-1)*ds, mnodes[m].rho);
                    double Dhf = ArrayRet[0]; double dzf= ArrayRet[1]; nodes[n].v = ArrayRet[2];
                    // Provision for finding effective D of annalus
                    // Check the formula using for this against message from Suncor
                    if (n>ncount/2) {
                        Dhf=Deff(Dhf,(n-1)*ds,well);
                    }
                    // Calculate pressure drop across nodes
                    nodes[n].P = Pdrop(Dhf,dzf, nodes[n-1], mnodes[m]);
                    // Find temperatures if not saturated
                    nodes[n].T = Tsat_asf_P_H2O(nodes[n].P); // Find Tsat of flow
                    ArrayRet = rhohmu_asf_Px_H2O(nodes[n].P,0);
                    nodes[n].hf = ArrayRet[1];
                    Pres_rms = Pres_rms+Math.pow(nodes[n].P-Plast,2); //Difference in inlet pressure

                }
                Pres_rms = Math.pow(Pres_rms/ncount,0.5);
                Pres_hist[iter-1]=Pres_rms;
                navg(mnodes); // find parameters at avg nodes
                // Enthalpy Solution
                double hres_max = hres_tol*2; // Ensure at least 1 iteration
                h_loop = 0; // Reset loop counter for local enthalpy iteration
                while (hres_max>hres_tol && h_loop<50) {
                    h_loop = h_loop+1;
                    for (int n=1; n <= ncount; n++) {
                        hlast[n-1] = nodes[n-1].h; // Store last h for convergence checking
                    }
                    hres_max = 0;

                    //disp(strcat('Enthalpy Iterataion=', num2str(h_loop))); // User Monitor
                    Log.d("3", "Enthalpy Iterataion= " + h_loop);
                    // Write Enthalpy Coefficient Matrix and Solution Vector
                    for (int n=1; n <= ncount/2-1; n++) {
                        m=n;
                        nb=ncount-n;
                        mb=nb-2;

                        //if (iter==1) {
                        double dm=mfr[nb-1-1]-mfr[nb-1];
                        // Equation 1 - Energy balance on internal pipe

                        H[n+1-1][n-1]=-mfr[n-1]; H[n+1-1][n+1-1]=mfr[n+1-1];
                        // Equation 2 - Energy balance on external pipe
                        H[nb][n-1]=mfr[n-1]; H[nb][n+1-1]=-mfr[n+1-1];
                        H[nb][nb-1]=mfr[nb-1]-0.5*dm;
                        H[nb][nb]=-mfr[nb]-0.5*dm;
                        //}

                        Q[n+1-1]=(ds/(mnodes[m-1].Rhx))*(mnodes[mb].T-mnodes[m-1].T);
                        Q[nb]=(ds/mnodes[mb].Rtf)*(mnodes[mb].T-Tf);
                    }

                    //if (iter==1) {
                    // Boundary conditions are known inlet and continuity at toe
                    // slide these equations in in the un-written part of the matrix
                    H[1-1][1-1]=1; Q[1-1]=nodes[1-1].h;
                    H[ncount/2+1-1][ncount/2-1]=1; H[ncount/2+1-1][ncount/2+1-1]=-1; Q[ncount/2+1-1]=0;

                    //}
                    Hsol = Lower_Triang_Matrix_Solver(H, Q);

                    // Fill in all nodal values
                    for (int n=1; n <= ncount; n++) {
                        // assign enthalpy to node
                        nodes[n-1].h=Hsol[n-1];
                        xlast[n-1]=nodes[n-1].x;
                        nodes[n-1].x=x_asf_hP_H2O(nodes[n-1].h,nodes[n-1].P);
                        xres_rms=xres_rms+Math.pow(nodes[n-1].x-xlast[n-1],2); //Difference in inlet pressure
                        ArrayRet = rhohmu_asf_Px_H2O(nodes[n-1].P, nodes[n-1].x);
                        nodes[n-1].rho = ArrayRet[0]; nodes[n-1].mu = ArrayRet[2];
                        hres_max = Math.max(hres_max,Math.abs(nodes[n-1].h-hlast[n-1]));
                        /*if (n<=ncount-2) {
                            mhlast[n-1]=mnodes[n-1].h;
                        }*/
                    }
                    navg(mnodes);
                    xres_rms = Math.pow(xres_rms/ncount,0.5);
                    xres_hist[iter-1]=xres_rms;

                    // Find new heat transfers
                    for (int n=2; n <= ncount/2; n++) {
                        nb=ncount-n+1;
                        m=n;
                        mb=ncount-n-1;
                        double dq_conv;
                        // heat loss from convection
                        if (nb*ds<(well.Length+well.Slotted_Liner) && nb*ds>well.Length) {
                            ArrayRet = rhohmu_asf_Px_H2O(mnodes[mb-1].P,mnodes[mb-1].x);
                            double hg = ArrayRet[1];
                            dq_conv = (mfr[nb+1-1]-mfr[nb-1])*hg/ds;
                        } else {
                            dq_conv=0;
                        }
                        // Conduction/Convection resistances
                        double Reshx=Rhx(mnodes[m-1],mnodes[mb],n*ds); // positive out of inner flow
                        double Restf=Rtf(mnodes[mb].T,n*ds); // heat conduction to formation
                        // Assign heat transfers to nodes for inter-iteration storage
                        mnodes[m-1].Rhx=Reshx; mnodes[mb].Rhx=Reshx;
                        mnodes[m-1].Q_tf=0; mnodes[mb].Rtf=Restf;

                        nodes[n-1].Q_hx=1/mnodes[m-1].Rhx*(mnodes[m-1].T-mnodes[mb-1].T);
                        nodes[nb-1].Q_hx=1/mnodes[m-1].Rhx*(mnodes[m-1].T-mnodes[mb-1].T);
                        nodes[nb-1].Q_tf=1/mnodes[mb-1].Rtf*(mnodes[mb-1].T-Tf);
                        if (mfr[nb-1-1]-mfr[nb-1] !=0) {
                            nodes[nb-1].Q_conv=-dq_conv;
                        }
                    }
                    //Parameter_Plot
                }
            }
            double xmin=1;
            for (int n=2; n <= ncount; n++) {
                if (nodes[n-1].x<xmin) {
                    xmin=nodes[n-1].x;
                }
            }
            if (xmin>0.2 && xmin<0.25) {
                solved=true;
            } else {
                if (xmin>0.25) {
                    well.mfr_in=well.mfr_in*Math.max((1-(xmin-0.22)),0.9);
                } else {
                    well.mfr_in=well.mfr_in*Math.min((1+(0.23-xmin)),1.1);
                }
            }
            //disp(strcat('Volumetric Flowrate=', num2str(well.mfr_in*3.6),'m^3/hr')); // User Monitor
            Log.d("4", "Volumetric Flowrate= " + well.mfr_in*3.6 + "m^3/hr");
            Log.d("5", "xmin " + xmin);
        }
        return well.mfr_in*3.6; //Return volumetric flowrate
    }

    public double[] Lower_Triang_Matrix_Solver( double[][] M, double[] B ) {
        // Solves a lower-triangular matrix
        for (int m=0; m< M[0].length; m++) {
            B[m] = B[m]/M[m][m];
            M[m][m] = 1;
            for (int n=m+1; n < M[0].length; n++) {
                if (M[n][m]!=0) {
                    B[n]=B[n]-B[m]*M[n][m]/M[m][m];
                    M[n][m]=M[n][m]-M[m][m]*M[n][m]/M[m][m];
                }
            }
        }
        return B;
    }

    public double max(double[] array) {
        double max = array[0];
        for (int i = 1; i< array.length; i++) {
            if (array[i] > max) max = array[i];
        }
        return max;
    }
    public double min(double[] array) {
        double min = array[0];
        for (int i = 1; i< array.length; i++) {
            if (array[i] < min) min = array[i];
        }
        return min;
    }
}// This closes the code

