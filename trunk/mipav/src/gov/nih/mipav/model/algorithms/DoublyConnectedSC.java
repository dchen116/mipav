package gov.nih.mipav.model.algorithms;

import gov.nih.mipav.view.MipavUtil;

public class DoublyConnectedSC extends AlgorithmBase {
	// This is a port of the FORTRAN ACM TOMS Algorithm 785, collected
	// algorithms form ACM.  The original ACM work was published in Transactions
	// on Mathematical Software, Vol. 24, No. 3, September, 1998, pp. 317-333.
	// The original FORTRAN code was written by Dr. Chenglie Hu as described in
	// his article "A Software Package for Computing Schwarz-Christoffel Conformal
	// Transformation for Doubly Connected Polyhgonal Regions."
	
	//************* DSCPACK *************************************************
	// THIS IS A COLLECTION OF SUBROUTINES TO SOLVE PARAMETER PROBLEM OF   *
	// THE SCHWARZ-CHRISTOFFEL TRANSFORMATION FOR DOUBLY CONNECTED REGIONS *
    // A DRIVER IS NEEDED FOR A/AN PARTICULAR PROBLEM OR APPLICATION.      *
	// AUTHOR:                                                             *
	// CHENGLIE HU   APRIL,1994 (REVISED JULY,1995) AT WICHITA STATE UNIV. *
	//               A FURTHER REVISION WAS DONE AT FHSU (JULY, 1997)      *
	// REFERENCES:1. H.DAEPPEN, DIE SCHWARZ-CRISTOFFEL ABBILDUNG FUER      *
	//               ZWEIFACH ZUSAMMENHAENGENDE GEBIETE MIT ANWENDUNGEN.   *
	//               PH.D. DISSERTATION, ETH ZUERICH.                      *
	//            2. L.N.TREFETHEN, SCPACK USER'S GUIDE(MIT REPORT 1989)   *
	//            3. HENRICI,APPLIED & COMPUTATIONAL COMPLEX ANALYSIS,VOL.3*
	//            4. C.HU, APPLICATION OF COMPUTATIONAL COMPLEX ANALYSIS TO*
	//               SOME FREE BOUNDARY AND VORTEX FLOWS.PH.D. DISS. 1995  *
	//**********************************************************************

	
	// geometries of the polygon region in the 7 test routines
	private final int SQUARE_SYMMETRIC_REGION = 1;
	private final int MILDLY_CROWDED_INFINITE_REGION = 2;
	private final int HEAVILY_CROWDED_REGION = 3;
	private final int CHINESE_CHARACTER_STRUCTURED_REGION = 4;
	private final int FOUR_DIRECTION_INFINITE_REGION = 5;
	private final int EXAMPLE_GIVEN_FOR_CHECKING_INVERSE_MAP = 6;
	private final int UPPER_HALF_PLANE_WITH_HORIZONTAL_SLIT = 7;
	
	private final int dscfun = 1;
	
	// Below are the 5 variables in the common block PARAM1
	private double W02[][] = new double[30][2];
	private double W12[][] = new double[30][2];
	private double Z02[][] = new double[30][2];
	private double Z12[][] = new double[30][2];
	private double C2[] = new double[2];
	
	// Below are the 6 variables in the common block PARAM2
	private double U2[] = new double[1];
	private double PHI02[] = new double[30];
	private double PHI12[] = new double[30];
	private double ALFA02[] = new double[30];
	private double ALFA12[] = new double[30];
	private double QWORK2[] = new double[1660];
	
	// Below are the 7 variables in the common block PARAM3
	private int M2, N2, NPTQ2, ISHAPE2, LINEARC2,  NSHAPE;
	private int IND[] = new int[20];
	
	// Below are the 4 variables in the common block PARAM4
	private double UARY[] = new double[8];
	private double VARY[] = new double[3];
	private double DLAM;
	private int IU;
	
	// Below are the 2 variables in the common block PARAM5:
	// Screen display of the residual of the system as the iteration goes on, 1 for "YES", 2 for "NO"
	private int ISPRT;
	private int ICOUNT;
	
	// Common blocks ..
    // COMMON /PARAM1/W02,W12,Z02,Z12,C2
    // COMMON /PARAM2/U2,PHI02,PHI12,ALFA02,ALFA12,QWORK2
    // COMMON /PARAM3/M2,N2,NPTQ2,ISHAPE2,LINEARC2,NSHAPE,IND
    // COMMON /PARAM4/UARY,VARY,DLAM,IU
    // COMMON /PARAM5/ISPRT,ICOUNT
	
	private SchwarzChristoffelMapping scm = new SchwarzChristoffelMapping();
	
	// Specifies the geometry of the polygon region for 7 test examples
	private int IPOLY;
	// The number of Gauss-Jacobi points
	// Recommended values for NPTQ are 2-8.
	private int NPTQ;
	// 1 for solving the nonlinear system, 2 for not solving the nonlinear system
	private int ISOLV;
	private boolean testRoutine = false;
	private double MACHEP = 2.2204460E-16;
	
	public DoublyConnectedSC() {
		
	}
	
	public DoublyConnectedSC(int IPOLY, int NPTQ, int ISOLV, int ISPRT) {
		this.IPOLY = IPOLY;
		this.NPTQ = NPTQ;
		this.ISOLV = ISOLV;
		this.ISPRT = ISPRT;
		testRoutine = true;
	}
	
	public void runAlgorithm() {
		int M[] = new int[1];
		int N[] = new int[1];
		double Z0[][] = new double[30][2];
		double Z1[][] = new double[30][2];
		double ALFA0[] = new double[30];
		double ALFA1[] = new double[30];
		double QWORK[] = new double[1660];
		int ISHAPE;
		int IGUESS;
		int LINEARC;
		double TOL;
		double U[] = new double[1];
		double C[] = new double[2];
		double W0[][] = new double[30][2];
		double W1[][] = new double[30][2];
		double PHI0[] = new double[30];
		double PHI1[] = new double[30];
		
double neweps;
		
		// MACHEP is a machine dependent parameter specifying the relative precision of
		// floating point arithmetic.
		 // epsilon = D1MACH(4)
        // Machine epsilon is the smallest positive epsilon such that
        // (1.0 + epsilon) != 1.0.
        // epsilon = 2**(1 - doubleDigits) = 2**(1 - 53) = 2**(-52)
        // epsilon = 2.2204460e-16
        // epsilon is called the largest relative spacing
        MACHEP = 1.0;
        neweps = 1.0;

        while (true) {

            if (1.0 == (1.0 + neweps)) {
                break;
            } else {
                MACHEP = neweps;
                neweps = neweps / 2.0;
            }
        } // while(true)
		
		if (testRoutine) {
			DSCDATA(IPOLY, M, N, Z0, Z1, ALFA0, ALFA1);
			ANGLES(N[0], Z1, ALFA1, 1);
			if ((IPOLY == 1) || (IPOLY == 3) || (IPOLY == 4) || (IPOLY == 6)) {
				ANGLES(M[0], Z0, ALFA0, 0);
			}
			
			// Generate the Gauss-Jacobi weights & nodes and check the input
			QINIT(M[0],N[0],ALFA0, ALFA1, NPTQ, QWORK);
			ISHAPE = 0;
			if ((IPOLY == 2) || (IPOLY == 5) || (IPOLY == 7)) {
				ISHAPE = 1;
			}
			CHECK(ALFA0, ALFA1, M[0], N[0], ISHAPE);
			
			// Specify some parameters of the calling sequence of DSCSOLV:
			IGUESS = 1;
			LINEARC = 1;
			TOL = 1.0E-10;
			
			// Solve the accessory parameter problem
			if (ISOLV == 1) {
				DSCSOLV(TOL, IGUESS, M[0], N[0], U, C, W0, W1, PHI0, PHI1,
						Z0, Z1, ALFA0, ALFA1, NPTQ, QWORK, ISHAPE, LINEARC);
			}
		} // if (testRoutine)
		
		
	}
	
	private void DSCDATA(int IPOLY, int M[], int N[], double Z0[][], double Z1[][], double ALFA0[], double ALFA1[]) {
		// DSCDATA generates data.
		double Q;
		double S;
		if (IPOLY == 1) {
			M[0] = 4;
			N[0] = 4;
			Q = Math.sqrt(2.0);
			Z0[0][0] = 1.0 + Q;
			Z0[0][1] = 1.0 + Q;
			Z0[1][0] = -1.0 - Q;
			Z0[1][1] = 1.0 + Q;
			Z0[2][0] = -1.0 - Q;
			Z0[2][1] = -1.0 - Q;
			Z0[3][0] = 1.0 + Q;
			Z0[3][1] = -1.0 - Q;
			Z1[0][0] = Q;
			Z1[0][1] = 0.0;
			Z1[1][0] = 0.0;
			Z1[1][1] = Q;
			Z1[2][0] = -Q;
			Z1[2][1] = 0.0;
			Z1[3][0] = 0.0;
			Z1[3][1] = -Q;
		} // if (IPOLY == 1)
		else if (IPOLY == 2) {
			M[0] = 12;
			N[0] = 6;
			Z0[0][0] = 0.6875;
			Z0[0][1] = 0.875;
			Z0[1][0] = 0.0;
			Z0[1][1] = 0.0;
			Z0[2][0] = 1.0;
			Z0[2][1] = 0.0;
			Z0[3][0] = 0.875;
			Z0[3][1] = 0.875;
			Z0[4][0] = 1.125;
			Z0[4][1] = 1.375;
			Z0[5][0] = 2.0;
			Z0[5][1] = 1.375;
			Z0[6][0] = 1.25;
			Z0[6][1] = 2.0;
			Z0[7][0] = 2.25;
			Z0[7][1] = 2.0;
			Z0[8][0] = 2.375;
			Z0[8][1] = 2.75;
			Z0[9][0] = 1.625;
			Z0[9][1] = 2.25;
			Z0[10][0] = 1.125;
		    Z0[10][1] = 2.625;
		    Z0[11][0] = -0.5;
		    Z0[11][1] = 2.75;
		    Z1[0][0] = 0.375;
		    Z1[0][1] = 1.875;
		    Z1[1][0] = 0.5;
		    Z1[1][1] = 2.0;
		    Z1[2][0] = 1.0;
		    Z1[2][1] = 1.5;
		    Z1[3][0] = 0.5;
		    Z1[3][1] = 2.1875;
		    Z1[4][0] = 0.5;
		    Z1[4][1] = 2.5;
		    Z1[5][0] = Z1[3][0];
		    Z1[5][1] = Z1[3][1];
		    ALFA0[0] = 1.39169261159339475;
            ALFA0[1] = 0.28801540784794967;
            ALFA0[2] = 0.454832764699133488;
            ALFA0[3] = 1.19275085295129979;
            ALFA0[4] = 1.35241638234956651;
            ALFA0[5] = 0.0;
            ALFA0[6] = 2.0;
            ALFA0[7] = 0.552568456711253445;
            ALFA0[8] = 0.260264501477747753;
            ALFA0[9] = 1.39199980651013222;
            ALFA0[10] = 0.819604487273064009;
            ALFA0[11] = 0.295854728586457991;
		} // else if (IPOLY == 2)
		else if (IPOLY == 3) {
			M[0] = 11;
			N[0] = 6;
			Z0[0][0] = 0.5;
			Z0[0][1] = 2.5;
			Z0[1][0] = 0.5;
			Z0[1][1] = 0.5;
			Z0[2][0] = 1.0;
			Z0[2][1] = 0.5;
			Z0[3][0] = 1.0;
			Z0[3][1] = 1.0;
			Z0[4][0] = 1.0;
			Z0[4][1] = 0.5;
			Z0[5][0] = 0.5;
			Z0[5][1] = 0.5;
			Z0[6][0] = 0.5;
			Z0[6][1] = 2.5;
			Z0[7][0] = 0.0;
			Z0[7][1] = 2.5;
			Z0[8][0] = 0.0;
			Z0[8][1] = 0.0;
			Z0[9][0] = 2.0;
			Z0[9][1] = 0.0;
			Z0[10][0] = 2.0;
			Z0[10][1] = 2.5;
			Z1[0][0] = 1.0;
			Z1[0][1] = 2.0;
			Z1[1][0] = 1.0;
			Z1[1][1] = 1.5;
			Z1[2][0] = 1.0;
			Z1[2][1] = 2.0;
			Z1[3][0] = 1.5;
			Z1[3][1] = 2.0;
			Z1[4][0] = 1.5;
			Z1[4][1] = 0.5;
			Z1[5][0] = 1.5;
			Z1[5][0] = 2.0;
			ALFA0[0] = 1.0/2.0;
            ALFA0[1] = 1.0/2.0;
            ALFA0[2] = 1.0/2.0;
            ALFA0[3] = 2.0;
            ALFA0[4] = 3.0/2.0;
            ALFA0[5] = 3.0/2.0;
            ALFA0[6] = 1.0/2.0;
            ALFA0[7] = 1.0/2.0;
            ALFA0[8] = 1.0/2.0;
            ALFA0[9] = 1.0/2.0;
            ALFA0[10] = 1.0/2.0;
            ALFA1[0] = 3.0/2.0;
            ALFA1[1] = 2.0;
            ALFA1[2] = 1.0/2.0;
            ALFA1[3] = 1.0/2.0;
            ALFA1[4] = 2.0;
            ALFA1[5] = 3.0/2.0;
		} // else if (IPOLY == 3)
		else if (IPOLY == 4) {
		    M[0] = 4;
		    N[0] = 17;
		    Z0[0][0] = -1.0;
		    Z0[0][1] = -1.0;
		    Z0[1][0] = 1.0;
		    Z0[1][1] = -1.0;
		    Z0[2][0] = 1.0;
		    Z0[2][1] = 1.0;
		    Z0[3][0] = -1.0;
		    Z0[3][1] = 1.0;
		    Z1[0][0] = 0.0;
		    Z1[0][1] = 0.5;
		    Z1[1][0] = 0.0;
		    Z1[1][1] = 0.0;
		    Z1[2][0] = -0.5;
		    Z1[2][1] = 0.0;
		    Z1[3][0] = Z1[1][0];
		    Z1[3][1] = Z1[1][1];
		    Z1[4][0] = 0.0;
		    Z1[4][1] = -0.5;
		    Z1[5][0] = -0.5;
		    Z1[5][1] = -0.5;
		    Z1[6][0] = 0.5;
		    Z1[6][1] = -0.5;
		    Z1[7][0] = 0.25;
		    Z1[7][1] = -0.5;
		    Z1[8][0] = 0.25;
		    Z1[8][1] = -0.25;
		    Z1[9][0] = Z1[7][0];
		    Z1[9][1] = Z1[7][1];
		    Z1[10][0] = Z1[4][0];
		    Z1[10][1] = Z1[4][1];
		    Z1[11][0] = Z1[1][0];
		    Z1[11][1] = Z1[1][1];
		    Z1[12][0] = 0.5;
		    Z1[12][1] = 0.0;
		    Z1[13][0] = Z1[1][0];
		    Z1[13][1] = Z1[1][1];
		    Z1[14][0] = Z1[0][0];
		    Z1[14][1] = Z1[0][1];
		    Z1[15][0] = 0.5;
		    Z1[15][1] = 0.5;
		    Z1[16][0] = -0.5;
		    Z1[16][1] = 0.5;
		} // else if (IPOLY == 4)
		else if (IPOLY == 5) {
			M[0] = 12;
			N[0] = 4;
			S = 1.0/6.0;
			Z0[0][0] = 0.0;
			Z0[0][1] = -8.0*S;
			Z0[1][0] = 0.0;
			Z0[1][1] = 0.0;
			Z0[2][0] = 1.0;
			Z0[2][1] = -1.5;
			Z0[3][0] = 1.0;
			Z0[3][1] = -0.5;
			Z0[4][0] = 0.0;
			Z0[4][1] = 0.0;
			Z0[5][0] = 0.0;
			Z0[5][1] = 2.0*S;
			Z0[6][0] = 0.0;
			Z0[6][1] = 0.0;
			Z0[7][0] = -1.0;
			Z0[7][1] = 0.0;
			Z0[8][0] = 0.0;
			Z0[8][1] = 0.0;
			Z0[9][0] = -7.0*S;
			Z0[9][1] = -1.0;
			Z0[10][0] = 0.0;
			Z0[10][1] = 0.0;
			Z0[11][0] = -2.0*S;
			Z0[11][1] = -10.0*S;
			ALFA0[0] = 1.75;
            ALFA0[1] = 0.0;
            ALFA0[2] = 1.0;
            ALFA0[3] = 1.0;
            ALFA0[4] = (Math.PI-3.5)/Math.PI;
            ALFA0[5] = 3.5/Math.PI;
            ALFA0[6] = 1.5;
            ALFA0[7] = 1.0;
            ALFA0[8] = 0.0;
            ALFA0[9] = 1.75;
            ALFA0[10] = 0.0;
            ALFA0[11] = 1.0;
            Z1[0][0] = 2.0*S;
            Z1[0][1] = -0.5;
            Z1[1][0] = -S;
            Z1[1][1] = -2.0*S;
            Z1[2][0] = -4.0*S;
            Z1[2][1] = -5.0*S;
            Z1[3][0] = 0.0;
            Z1[3][1] = -1.0;
		} // else if (IPOLY == 5)
		else if (IPOLY == 6) {
		    M[0] = 7;
		    N[0] = 2;
		    Z0[0][0] = -2.0;
		    Z0[0][1] = -1.0;
		    Z0[1][0] = 2.0;
		    Z0[1][1] = -1.0;
		    Z0[2][0] = 2.0;
		    Z0[2][1] = 2.0;
		    Z0[3][0] = -0.8;
		    Z0[3][1] = 2.0;
		    Z0[4][0] = 1.0;
		    Z0[4][1] = 0.5;
		    Z0[5][0] = -1.0;
		    Z0[5][1] = 2.0;
		    Z0[6][0] = -2.0;
		    Z0[6][1] = 2.0;
		    Z1[0][0] = 0.0;
		    Z1[0][1] = 0.0;
		    Z1[1][0] = -1.0;
		    Z1[1][1] = 0.0;
		} // else if (IPOLY == 6)
		else {
			M[0] = 3;
			N[0] = 2;
			Z0[0][0] = 1.01;
			Z0[0][1] = 0.0;
			Z0[1][0] = 100.0;
			Z0[1][1] = 100.0;
			Z0[2][0] = -1.01;
			Z0[2][1] = 0.0;
			Z1[0][0] = 0.0;
			Z1[0][1] = 2.0;
			Z1[1][0] = 0.0;
			Z1[1][1] = 1.0;
			ALFA0[0] = 1.0;
			ALFA0[1] = -1.0;
			ALFA0[2] = 1.0;
		} // else		
	}
	
	private void THDATA(double U[]) {
	    //    ----------------------
	    //    GENERATES DATA RELATED ONLY TO INNER RADIUS
	    //    U AND USED IN COMPUTING THE THETA-FUNCTION.

		// .. Local Scalars ..
	    double PI;
	    int  K,N;
	
	    //    .. Common blocks ..
	    // COMMON /PARAM4/UARY,VARY,DLAM,IU
	
	    PI = Math.PI;
	    if (U[0] >= 0.63) {
	    	VARY[0] = Math.exp(PI*PI/Math.log(U[0]));
	    	DLAM = -Math.log(U[0])/PI;
	    	return;
	    }
	    if (U[0] < 0.06) {
	        IU = 3;
	    }
	    else if (U[0] < 0.19) {
	        IU = 4;
	    }
	    else if (U[0] < 0.33) {
	        IU = 5;
	    }
	    else if (U[0] < 0.45) {
	        IU = 6;
	    }
	    else if (U[0] < 0.55) {
	        IU = 7;
	    }
	    else {
	        IU = 8;
	    }

	    for (K = 1; K <= IU; K++) {
	          N = K*K;
	          UARY[K-1] = Math.pow(U[0], N);
	    } // for (K = 1; K <= IU; K++)
	    return;

	}
	
	private double[] WTHETA(double U, double W[]) {
	    //  -------------------------
	    //    EVALUATES THETA-FUNCTION AT W,WHERE U IS THE
	    //    INNER RADIUS OF THE ANNULUS.THE DEFINITION OF
	    //    THETA-FUNCTION CAN BE FOUND IN REFERENCE 3.
	
	    //     .. Scalar Arguments ..
	    //  DOUBLE COMPLEX W

	    //     .. Scalars in Common ..
	    // DOUBLE PRECISION DLAM
	    // INTEGER IU

	    //    .. Arrays in Common ..
	    // DOUBLE PRECISION UARY(8),VARY(3)

	    //     .. Local Scalars ..
		double result[] = new double[2];
		double cr[] = new double[1];
		double ci[] = new double[1];
		double WT[] = new double[2];
		double WWN[] = new double[2];
		double WWN0[] = new double[2];
		double WWP[] = new double[2];
		double WWP0[] = new double[2];
		double ZI[] = new double[2];
	    // DOUBLE COMPLEX WT,WWN,WWN0,WWP,WWP0,ZI
	    double PI;
	    int K;
	
	    //     .. Common blocks ..
	    // COMMON /PARAM4/UARY,VARY,DLAM,IU
	
	    WWP[0] = 1.0;
	    WWP[1] = 0.0;
	    WWN[0] = 1.0;
	    WWN[1] = 0.0;
	    result[0] = 1.0;
	    result[1] = 0.0;
	    if (U < 0.63) {
	        WWP0[0] = -W[0];
	        WWP0[1] = -W[1];
	        scm.zdiv(-1.0, 0.0, W[0], W[1], cr, ci);
	        WWN0[0] = cr[0];
	        WWN0[1] = ci[0];
	        for (K = 1; K <= IU; K++) {
	        	scm.zmlt(WWP[0], WWP[1], WWP0[0], WWP0[1], cr, ci);
	        	WWP[0] = cr[0];
	        	WWP[1] = ci[0];
	            scm.zmlt(WWN[0], WWN[1], WWN0[0], WWN0[1], cr, ci);
	            WWN[0] = cr[0];
	            WWN[1] = ci[0];
	            result[0] = result[0] + UARY[K-1]* (WWP[0]+WWN[0]);
	            result[1] = result[1] + UARY[K-1]* (WWP[1]+WWN[1]);
	        } // for (K = 1; K <= IU; K++)
	        return result;
	   } // if (U < 0.63)

	   PI = Math.PI;
	   ZI[0] = 0.0;
	   ZI[1] = 1.0;
	   double logr = Math.log(scm.zabs(-W[0], -W[1]));
	   double logi = Math.atan2(-W[1], -W[0]);
	   scm.zmlt(-ZI[0], -ZI[1], logr, logi, cr, ci);
	   WT[0] = cr[0];
	   WT[1] = ci[0];
	   if (U < 0.94) {
		  double base = Math.exp(WT[0]/DLAM);
		  double arg = WT[1]/DLAM;
		  WWP[0] = base * Math.cos(arg);
		  WWP[1] = base * Math.sin(arg);
		  scm.zdiv(1.0, 0.0, WWP[0], WWP[1], cr, ci);
	      WWN[0] = cr[0];
	      WWN[1] = ci[0];
	      result[0] = result[0] + VARY[0]* (WWP[0]+WWN[0]); 
	      result[1] = result[1] + VARY[0]* (WWP[1]+WWN[1]);   
	   } // if (U < 0.94)
	   scm.zmlt(WT[0], WT[1], WT[0], WT[1], cr, ci);
	   double argr = -cr[0]/(4.0 * PI * DLAM);
	   double argi = -ci[0]/(4.0 * PI * DLAM);
	   double base = Math.exp(argr);
	   double expr = base * Math.cos(argi);
	   double expi = base * Math.sin(argi);
	   scm.zmlt(expr, expi, result[0], result[1], cr, ci);
	   result[0] = cr[0]/Math.sqrt(DLAM);
	   result[1] = ci[0]/Math.sqrt(DLAM);
	   return result;
	}


	
	private double[] WPROD(double W[], int M, int N, double U,
			double W0[][], double W1[][], double ALFA0[], double ALFA1[]) {
	//   --------------------------------------------
	//    COMPUTES THE PRODUCT (D-SC INTEGRAND):
	// M                                   N
	//PROD THETA(W/U*W0(K))**(ALFA0(K)-1)*PROD THETA(U*W/W1(K))**(ALFA1(K)-1)
	// K=1                                 K=1
	//    THE CALLING SEQUENCE IS EXPLAINED IN THE ABOVE FORMULA.
	
	//     .. Scalar Arguments ..
	//      DOUBLE COMPLEX W
	     
	//     .. Array Arguments ..
	//      DOUBLE COMPLEX W0(M),W1(N)
	//      DOUBLE PRECISION ALFA0(M),ALFA1(N)
	
	//     .. Scalars in Common ..
	//      DOUBLE PRECISION DLAM
	//      INTEGER IU
	
	//     .. Arrays in Common ..
	//      DOUBLE PRECISION UARY(8),VARY(3)
	
	//     .. Local Scalars ..
		double result[] = new double[2];
		double WSUM[] = new double[2];
		double WTH[] = new double[2];
		double cr[] = new double[1];
		double ci[] = new double[1];
		double win[] = new double[2];
		double wout[];
		double base;
	//      DOUBLE COMPLEX WSUM,WTH
	      int K;

	//     .. External Functions ..
	//      DOUBLE COMPLEX WTHETA
	//      EXTERNAL WTHETA
	
	//     .. Common blocks ..
	//      COMMON /PARAM4/UARY,VARY,DLAM,IU
	      WSUM[0] = 0.0;
	      WSUM[1] = 0.0;
	      for (K = 1; K <= M; K++) {
	    	  scm.zdiv(W[0], W[1], U*W0[K][0], U*W0[K][1], cr, ci);
	    	  win[0] = cr[0];
	    	  win[1] = ci[0];
	    	  wout = WTHETA(U, win);
	    	  WTH[0] = Math.log(scm.zabs(wout[0], wout[1]));
	    	  WTH[1] = Math.atan2(wout[1], wout[0]);
	          WSUM[0] = WSUM[0] + (ALFA0[K-1]-1.0)*WTH[0];
	          WSUM[1] = WSUM[1] + (ALFA0[K-1]-1.0)*WTH[1];
	      } // for (K = 1; K <= M; K++)
	      for (K = 1; K <= N; K++) {
	    	  scm.zdiv(U*W[0], U*W[1], W1[K][0], W1[K][1], cr, ci);
	    	  win[0] = cr[0];
	    	  win[1] = ci[0];
	    	  wout = WTHETA(U, win);
	    	  WTH[0] = Math.log(scm.zabs(wout[0], wout[1]));
	    	  WTH[1] = Math.atan2(wout[1], wout[0]);
	          WSUM[0] = WSUM[0] + (ALFA1[K-1]-1.0)*WTH[0];
	          WSUM[1] = WSUM[1] + (ALFA1[K-1]-1.0)*WTH[1];
	      } // for (K = 1; K <= N; K++)
	      base = Math.exp(WSUM[0]);
	      result[0] = base * Math.cos(WSUM[1]);
	      result[1] = base * Math.sin(WSUM[1]);
	      return result;
	}


	
	private double[] WQSUM(double WA[], double PHIA,int KWA, int IC, double WB[],
			double PHIB, double RADIUS,int M,int N, double U, double W0[][], double W1[][],
			double ALFA0[], double ALFA1[], int NPTQ,double QWORK[], int LINEARC) {
		//   ------------------------------------------------------------
		//   CALCULATES THE  COMPLEX  INTEGRAL  FROM WA TO WB ALONG  A
		//   LINE SEGMENT (LINEARC=0)OR A CIRCULAR ARC (LINEARC=1)WITH
		//   POSSIBLE SINGULARITY AT WA, WHERE  KWA IS THE INDEX OF WA
		//   IN W0 (IC=0) OR IN W1 (IC=1). KWA=0 AND IC=2 ( NO OTHER
		//   VALUES PERMITTED ) IF WA IS NOT A PREVERTEX, WHICH THEN
		//   INDICATES THAT ONLY PURE GAUSSIAN QUARATURE IS NEEDED.
		//   PHIA & PHIB  ARE  ARGUMENTS OF  WA & WB  RESPECTIVELY. IF
		//   INTEGRATING ALONG A CIRCULAR ARC,RADIUS SHOULD BE ASSIGNED
		//   TO BE EITHER 1 OR U. ANY VALUE,HOWEVER,CAN BE ASSIGNED TO
		//   RADIUS IF INTEGRATING ALONG A LINE SEGMENT.SEE DOCUMENTATIONS
		//   OF WPROD AND QINIT FOR THE REST OF CALLING SEQUENCE.
		
		//     .. Scalar Arguments ..
		//    DOUBLE COMPLEX WA,WB
		
		//     .. Array Arguments ..
		//      DOUBLE COMPLEX W0(M),W1(N)
		//      DOUBLE PRECISION ALFA0(M),ALFA1(N),QWORK(NPTQ* (2* (M+N)+3))
		
		//     .. Local Scalars ..
		double result[] = new double[2];
		double W[] = new double[2];
		double WC[] = new double[2];
		double WH[] = new double[2];
		double ZI[] = new double[2];
		//     DOUBLE COMPLEX W,WC,WH,ZI
		double PWC,PWH;
		int I,IOFFST,IWT1,IWT2;
		double prod[] = null;
		double base;
		double arg;
		double cr[] = new double[1];
		double ci[] = new double[1];

		//     .. External Functions ..
		//      DOUBLE COMPLEX WPROD
		//      EXTERNAL WPROD
		
		//     .. Common blocks ..
		//      COMMON /PARAM4/UARY,VARY,DLAM,IU
	    result[0] = 0.0;
		result[1] = 0.0;
		
		//   INDEX ARRANGEMENT:
		IWT1 = NPTQ* (IC*M+KWA-1) + 1;
		if (KWA == 0) {
		    IWT1 = NPTQ* (M+N) + 1;
		}
	    IWT2 = IWT1 + NPTQ - 1;
		IOFFST = NPTQ* (M+N+1);
		
		//   COMPUTE GAUSS-JACOBI SUM(W(J)*PROD(X(J))):
		if (LINEARC != 1) {
		
		    //   INTEGRATE ALONG A LINE SEGMENT:
		    WH[0] = (WB[0]-WA[0])/2.0;
		    WH[1] = (WB[1]-WA[1])/2.0;
		    WC[0] = (WA[0]+WB[0])/2.0;
		    WC[1] = (WA[1]+WB[1])/2.0;
		    for (I = IWT1; I <= IWT2; I++) {
		          W[0] = WC[0] + WH[0]*QWORK[I-1];
		          W[1] = WC[1] + WH[1]*QWORK[I-1];
		          prod = WPROD(W,M,N,U,W0,W1,ALFA0,ALFA1);
		          result[0] = result[0] + QWORK[IOFFST + I-1]*prod[0];
		          result[1] = result[1] + QWORK[IOFFST + I-1]*prod[1];
		    } // for (I = IWT1; I <= IWT2; I++)
		    scm.zmlt(result[0], result[1], WH[0], WH[1], cr, ci);
		    result[0] = cr[0];
		    result[1] = ci[0];
		    return result;
	    } if (LINEARC != 1)
		
		//   INTEGRATE ALONG A CIRCULAR ARC:
	    ZI[0] = 0.0;
	    ZI[1] = 1.0;
		PWH = (PHIB-PHIA)/2.0;
		PWC = (PHIB+PHIA)/2.0;
		for (I = IWT1; I <= IWT2; I++) {
			base = RADIUS*Math.exp(ZI[0]* (PWC+PWH*QWORK[I-1]));
			arg = ZI[1]* (PWC+PWH*QWORK[I-1]);
			W[0] = base * Math.cos(arg);
			W[1] = base * Math.sin(arg);
		    prod = WPROD(W,M,N,U,W0,W1,ALFA0,ALFA1);
		    scm.zmlt(W[0], W[1], prod[0], prod[1], cr, ci);
		    result[0] = result[0] + QWORK[IOFFST+I-1] * cr[0];
		    result[1] = result[1] + QWORK[IOFFST+I-1] * ci[0];
		} // for (I = IWT1; I <= IWT2; I++)
		scm.zmlt(result[0], result[1], PWH*ZI[0], PWH*ZI[1], cr, ci);
		result[0] = cr[0];
		result[1] = ci[0];
		return result;
	}

	
	private double[] WQUAD1(double WA[], double PHIA, int KWA,int IC, double WB[],
			double PHIB, double RADIUS,int M,int N, double U, double W0[][], double W1[][],
			double ALFA0[], double ALFA1[], int NPTQ, double QWORK[], int LINEARC) {
		//   -------------------------------------------------------------
		//   CALCULATES THE COMPLEX INTEGRAL OF WPROD FROM WA TO WB ALONG
		//   EITHER A CIRCULAR ARC OR A LINE-EGMENT.  COMPOUND ONE-SIDED
		//   GAUSS-JACOBI QUDRATURE IS USED.SEE SUBROUTINE WQSUM FOR THE
		//   CALLING SEQUENCE.
		
		//   CHECK FOR ZERO-LENGTH INTEGRAL:
		//     .. Scalar Arguments ..
		// DOUBLE COMPLEX WA,WB
		     
		//     .. Array Arguments ..
		//      DOUBLE COMPLEX W0(M),W1(N)
		//      DOUBLE PRECISION ALFA0(M),ALFA1(N),QWORK(NPTQ* (2* (N+M)+3))
		
		//     .. Local Scalars ..
		double WAA[] = new double[2];
		double WBB[] = new double[2];
		double ZI[] = new double[2];
		//    DOUBLE COMPLEX WAA,WBB,ZI
		double PHAA,PHBB,R;
		double result[] = new double[2];
		double result2[] = new double[2];
		double expb;
		double arg;
		     
		//     .. External Functions ..
		//      DOUBLE COMPLEX WQSUM
		//      DOUBLE PRECISION DIST
		//      EXTERNAL WQSUM,DIST
		
		//     .. Common blocks ..
		//      COMMON /PARAM4/UARY,VARY,DLAM,IU
	
	    if (scm.zabs(WA[0]-WB[0], WA[1]-WB[1]) <= 0.0) {
	        result[0] = 0.0;
	    	result[1] = 0.0;
	        return result;
	    }
	    
	    ZI[0] = 0.0;
	    ZI[1] = 1.0;
	    if (LINEARC != 1) {
		
		    //   LINE SEGMENT INTEGRATION PATH IS CONCERNED BELOW:
		    // STEP1:ONE-SIDED G-J QUADRATURE FOR LEFT ENDPT WA:
		    R = Math.min(1.0,DIST(M,N,W0,W1,WA,KWA,IC)/scm.zabs(WB[0]-WA[0],WB[1]-WA[1]));
		    WAA[0] = WA[0] + R* (WB[0]-WA[0]);
		    WAA[1] = WA[1] + R* (WB[1]-WA[1]); 
		    result = WQSUM(WA,0.0,KWA,IC,WAA,0.0,0.0,M,N,U,W0,W1,ALFA0,
		              ALFA1,NPTQ,QWORK,LINEARC);
		
		    //   STEP2:ADJOIN INTERVALS OF PURE GAUSS QUADRATURE IF NECESSARY:
		   while (R != 1.0) {
		      R = Math.min(1.0,DIST(M,N,W0,W1,WAA,0,IC)/scm.zabs(WAA[0]-WB[0], WAA[1] - WB[1]));
		      WBB[0] = WAA[0] + R* (WB[0]-WAA[0]);
		      WBB[1] = WAA[1] + R* (WB[1]-WAA[1]);
		      result2 = WQSUM(WAA,0.0,0,2,WBB,0.0,0.0,M,N,U,W0,W1,
		              ALFA0,ALFA1,NPTQ,QWORK,LINEARC);
		      result[0] = result[0] + result2[0];
		      result[1] = result[1] + result2[1];
		      WAA[0] = WBB[0];
		      WAA[1] = WBB[1];
		   }
		   return result;
	    } // if (LINEARC != 1)
		
		//   CIRCULAR ARC INTEGRATION PATH IS CONCERNED BELOW:
		//   STEP1:ONE-SIDED G-J QUADRATURE FOR LEFT ENDPT WA:
		R = Math.min(1.0,DIST(M,N,W0,W1,WA,KWA,IC)/scm.zabs(WB[0]-WA[0],WB[1]-WA[1]));
		PHAA = PHIA + R* (PHIB-PHIA);
		expb = RADIUS * Math.exp(ZI[0]*PHAA);
		arg = ZI[1] * PHAA;
		WAA[0] = expb * Math.cos(arg);
		WAA[1] = expb * Math.sin(arg);
		result = WQSUM(WA,PHIA,KWA,IC,WAA,PHAA,RADIUS,M,N,U,W0,W1,ALFA0,
		              ALFA1,NPTQ,QWORK,LINEARC);
		
		//   STEP2:ADJOIN INTERVALS OF PURE GAUSS QUADRATURE IF NECESSARY:
		while (R != 1.0) {
		      R = Math.min(1.0,DIST(M,N,W0,W1,WAA,0,IC)/scm.zabs(WAA[0]-WB[0],WAA[1]-WB[1]));
		      PHBB = PHAA + R* (PHIB-PHAA);
		      expb = RADIUS * Math.exp(ZI[0]*PHBB);
		      arg = ZI[1] * PHBB;
		      WBB[0] = expb * Math.cos(arg);
		      WBB[1] = expb * Math.sin(arg);
		      result2 = WQSUM(WAA,PHAA,0,2,WBB,PHBB,RADIUS,M,N,U,W0,W1,
		             ALFA0,ALFA1,NPTQ,QWORK,LINEARC);
		      result[0] = result[0] + result2[0];
		      result[1] = result[1] + result2[1];
		      PHAA = PHBB;
		      WAA[0] = WBB[0];
		      WAA[1] = WBB[1];
		} //while (R != 1.0)
		return result;
	}

	
	private double[] WQUAD(double WA[], double PHIA,int KWA,int ICA, double WB[],
			double PHIB, int KWB,int ICB,double RADIUS,int M,int N, double U, double W0[][],
		    double W1[][], double ALFA0[], double ALFA1[], int NPTQ, double QWORK[],
		    int LINEARC,int IEVL) {
		//   ---------------------------------------------------------------
		//   CALCULATES THE COMPLEX INTEGRAL OF WPROD FROM WA TO WB
		//   ALONG A CIRCULAR ARC OR A LINE-SEGMENT.FUNCTION WQUAD1
		//   IS CALLED FOUR TIMES,ONE FOR EACH 1/4 OF THE INTERVAL.
		//   NOTE:  WQUAD1 ALLOWS  ONLY THE LEFT ENDPOINT  TO  BE A
		//   POSSIBLE SINGULARITY. SEE ROUTINE WQSUM FOR THE CALLING
	    //   SEQUENCE.
		
		//     .. Scalar Arguments ..
		//    DOUBLE COMPLEX WA,WB
		     
		//     .. Array Arguments ..
		//     DOUBLE COMPLEX W0(M),W1(N)
		//     DOUBLE PRECISION ALFA0(M),ALFA1(N),QWORK(NPTQ* (2* (M+N)+3))
	
		//     .. Local Scalars 
		double WMID[] = new double[2];
		double WMIDA[] = new double[2];
		double WMIDB[] = new double[2];
		double WQA[] = new double[2];
		double WQA1[] = new double[2];
		double WQA2[] = new double[2];
		double WQB[] = new double[2];
		double WQB1[] = new double[2];
		double WQB2[] = new double[2];
		double ZI[] = new double[2];
		// DOUBLE COMPLEX WMID,WMIDA,WMIDB,WQA,WQB,ZI
		double PHMID,PHMIDA,PHMIDB,PI;
		double expb;
		double arg;
		double result[] = new double[2];
		//     .. Common blocks ..
		//     COMMON /PARAM4/UARY,VARY,DLAM,IU
		
		PI = Math.PI;
		ZI[0] = 0.0;
		ZI[1] = 1.0;
		
		//   DETERMINE MIDPTS ON A LINE SEGMENT OR ON A CIRCULAR ARC:
		if (LINEARC == 0) {
			WMID[0] = (WA[0] + WB[0])/2.0;
			WMID[1] = (WA[1] + WB[1])/2.0;
		    WMIDA[0] = (WA[0] + WMID[0])/2.0;
		    WMIDA[1] = (WA[1] + WMID[1])/2.0;
		    WMIDB[0] = (WB[0] + WMID[0])/2.0;
		    WMIDB[1] = (WB[1] + WMID[1])/2.0;
		    PHMID = 0.0;
		    PHMIDA = 0.0;
		    PHMIDB = 0.0;
		}
		else {
	        if (IEVL != 1) {
		        if (PHIB < PHIA) {
		            PHIA = PHIA - 2.0*PI;
		        }
	        } // if (IEVL != 1)
		   PHMID = (PHIA+PHIB)/2.0;
		   expb = RADIUS * Math.exp(ZI[0] * PHMID);
		   arg = ZI[1] * PHMID;
		   WMID[0] = expb * Math.cos(arg);
		   WMID[1] = expb * Math.sin(arg);
		   PHMIDA = (PHIA+PHMID)/2.0;
		   expb = RADIUS * Math.exp(ZI[0] * PHMIDA);
		   arg = ZI[1] * PHMIDA;
		   WMIDA[0] = expb * Math.cos(arg);
		   WMIDA[1] = expb * Math.sin(arg);
		   PHMIDB = (PHIB+PHMID)/2.0;
		   expb = RADIUS * Math.exp(ZI[0]*PHMIDB);
		   arg = ZI[1] * PHMIDB;
		   WMIDB[0] = expb * Math.cos(arg);
		   WMIDB[1] = expb * Math.sin(arg);
		}
		
		//   COMPOUND GAUSS-JACOBI PROCESS ACCORDING TO ONE-QUATER RULE:
        WQA1 = WQUAD1(WA,PHIA,KWA,ICA,WMIDA,PHMIDA,RADIUS,M,N,U,W0,W1,
           ALFA0,ALFA1,NPTQ,QWORK,LINEARC);
        WQA2 = WQUAD1(WMID,PHMID,0,2,WMIDA,PHMIDA,RADIUS,M,N,U,W0,W1,ALFA0,
           ALFA1,NPTQ,QWORK,LINEARC);
        WQA[0] = WQA1[0] - WQA2[0];
        WQA[1] = WQA1[1] - WQA2[1];
        WQB1 = WQUAD1(WB,PHIB,KWB,ICB,WMIDB,PHMIDB,RADIUS,M,N,U,W0,W1,
           ALFA0,ALFA1,NPTQ,QWORK,LINEARC);
        WQB2 = WQUAD1(WMID,PHMID,0,2,WMIDB,PHMIDB,RADIUS,M,N,U,W0,W1,ALFA0,
           ALFA1,NPTQ,QWORK,LINEARC);
        WQB[0] = WQB1[0] - WQB2[0];
        WQB[1] = WQB1[1] - WQB2[1];
        result[0] = WQA[0] - WQB[0];
        result[1] = WQA[1] - WQB[1];
		return result;
	}


	
	private void XWTRAN(int M,int N, double X[], double U[],
			double C[], double W0[][], double W1[][], double PHI0[],
			double PHI1[]) {
		//   -----------------------------------------------
		//  TRANSFORMS X[K-1](UNCONSTRAINED PARAMETERS) TO ACTUAL
		//  D-SC PARAMETERS:U,C,W0,W1.PHI0 & PHI1 ARE ARGUMENTS
		//  OF THE PREVERTICES CONTAINED IN W0 & W1.
		
		//     .. Scalar Arguments ..
		//      DOUBLE COMPLEX C
		      
		//     .. Array Arguments ..
		//      DOUBLE COMPLEX W0(M),W1(N)
		//      DOUBLE PRECISION PHI0(M),PHI1(N),X(M+N+2)
	
		//     .. Local Scalars ..
	      double DPH, PH, PHSUM, PI;
	      int  I;
	
	      PI = Math.PI;
	      if (Math.abs(X[0]) <= 1.0E-14) {
	          U[0] = 0.5;
	      }
	      else {
	          U[0] = (X[0]-2.0-Math.sqrt(0.9216*X[0]*X[0]+4.0))/ (2.0*X[0]);
	          U[0] = (0.0196*X[0]-1.0)/ (U[0]*X[0]);
	      }

	      C[0] = X[1];
	      C[1] = X[2];
	      if (Math.abs(X[N+2]) <= 1.0E-14) {
	          PHI1[N-1] = 0.0;
	      }
	      else {
	          PH = (1.0+Math.sqrt(1.0+PI*PI*X[N+2]*X[N+2]))/X[N+2];
	          PHI1[N-1] = PI*PI/PH;
	      }

	      DPH = 1.0;
	      PHSUM = DPH;
	      
	      for (I = 1; I <= N - 1; I++) {
	          DPH = DPH/Math.exp(X[2+I]);
	          PHSUM = PHSUM + DPH;
	      } // for (I = 1; I <= N - 1; I++)
	      DPH = 2.0*PI/PHSUM;
	      PHI1[0] = PHI1[N-1] + DPH;
	      W1[0][0] = U[0] * Math.cos(PHI1[0]);
	      W1[0][1] = U[0] * Math.sin(PHI1[0]);
	      W1[N-1][0] = U[0] * Math.cos(PHI1[N-1]);
	      W1[N-1][1] = U[0] * Math.sin(PHI1[N-1]);
	      PHSUM = PHI1[0];
	      for (I = 1; I <= N - 2; I++) {
	          DPH = DPH/Math.exp(X[2+I]);
	          PHSUM = PHSUM + DPH;
	          PHI1[I] = PHSUM;
	          W1[I][0] = U[0] * Math.cos(PHSUM);
	          W1[I][1] = U[0] * Math.sin(PHSUM);
	      } // for (I = 1; I <= N - 2; I++)
	      DPH = 1.0;
	      PHSUM = DPH;
	      for (I = 1; I <= M - 1; I++) {
	          DPH = DPH/Math.exp(X[N+2+I]);
	          PHSUM = PHSUM + DPH;
	      } // for (I = 1; I <= M - 1; I++)
	      DPH = 2.0*PI/PHSUM;
	      PHSUM = DPH;
	      PHI0[0] = DPH;
	      W0[0][0] = Math.cos(DPH);
	      W0[0][1] = Math.sin(DPH);
	      for (I = 1; I <= M - 2; I++) {
	          DPH = DPH/Math.exp(X[N+2+I]);
	          PHSUM = PHSUM + DPH;
	          PHI0[I] = PHSUM;
	          W0[I][0] = Math.cos(PHSUM);
	          W0[I][1] = Math.sin(PHSUM);
	      } // for (I = 1; I <= M - 2; I++)
		  return;
	}
	
	private double FMAX(int MN, double FVAL[]) {
	    //   -------------------------
	    //    DETERMINES THE MAXIMUM-NORM OF THE VECTOR FVAL.
	
	    //     .. Array Arguments ..
	    // DOUBLE PRECISION FVAL(MN)
		
	    //      .. Local Scalars ..
		double maxVal;
	    double FV;
	    int K;
	
	    maxVal = 0.0;
	    for (K = 0; K < MN; K++) {
	        FV = Math.abs(FVAL[K]);
	        if (FV > maxVal) {
	        	maxVal = FV;
	        }
	    } // for (K = 0; K < MN; K++)
	    return maxVal;
	}

	
	private void DSCFUN(int NDIM, double X[], double FVAL[],int IFLAG[]) {
	    //    --------------------------------------
	    //  FORMS THE NONLINEAR SYSTEM SATISFIED BY D-SC PARAMETERS.THE
	    //  SUBROUTINE WILL BE CALLED BY NONLINEAR SYSTEM SOLVER HYBRD.
	    //  SEE ROUTINE DSCSOLV FOR THE VARIABLES IN THE COMMON BLOCKS.
	    //  NDIM: THE DIMENSION OF THE SYSTEM.
	    //  X:    UNKNOWNS
	    //  FVAL: THE VECTOR DEFINED BY THE SYSTEM.
	    //  IFLAG:(ON OUTPUT)=1,THE ITERATION WAS SUCCESSFULLY COMPLETED.
	    //         =2,3,OR 4, UNSUCCESSFUL TERMINATION OF THE ITERATION.
	    //         =5 MAY INDICATE THE TOLERANCE GIVEN IN THE CALLING
	    //         SEQUENCE OF HYBRD IS TOO SMALL.
	
	
	    //  TRANSFORM UNCONSTRAINED X(K) TO ACTUAL D-SC PARAMETERS
	    //  AND COMPUTE DATA TO BE USED IN WTHETA:
	
	    //     .. Scalar Arguments ..
	    //  INTEGER IFLAG,NDIM
	    //     ..
	    //     .. Array Arguments ..
	    //  DOUBLE PRECISION FVAL(NDIM),X(NDIM)
	
	    
	     //.. Local Scalars ..
		double cr[] = new double[1];
		double ci[] = new double[1];
	    double wout[];
	    double base;
	    double diffR;
	    double diffI;
		double WA[] = new double[2];
		double WAI[] = new double[2];
		double WARC[] = new double[2];
		double WB[] = new double[2];
		double WBI[] = new double[2];
		double WCIRCLE[] = new double[2];
		double WIN1[] = new double[2];
		double WIN2[] = new double[2];
		double WIN3[] = new double[2];
		double WIN4[] = new double[2];
		double WINT1[] = new double[2];
		double WINT2[] = new double[2];
		double WINT3[] = new double[2];
		double WLINE[] = new double[2];
		double WLINE1[] = new double[2];
		double WLINE2[] = new double[2];
		double WX[] = new double[2];
		double ZI[] = new double[2];
	    //  DOUBLE COMPLEX WA,WAI,WARC,WB,WBI,WCIRCLE,WIN1,WIN2,WIN3,WIN4,
	    //               WINT1,WINT2,WINT3,WLINE,WLINE1,WLINE2,WX,ZI
	    double FMAXN,PHIA,PHIB,RADIUS,TEST1;
	    int I,J,K;
	
	    //     .. External Functions ..
	    // DOUBLE COMPLEX WQUAD
	    // DOUBLE PRECISION FMAX
	    // EXTERNAL WQUAD,FMAX
	
	    //     .. External Subroutines ..
	    // EXTERNAL THDATA,XWTRAN
	
	    //     .. Common blocks ..
	    // COMMON /PARAM1/W02,W12,Z02,Z12,C2
	    // COMMON /PARAM2/U2,PHI02,PHI12,ALFA02,ALFA12,QWORK2
	    // COMMON /PARAM3/M2,N2,NPTQ2,ISHAPE2,LINEARC2,NSHAPE,IND
	    // COMMON /PARAM4/UARY,VARY,DLAM,IU
	    // COMMON /PARAM5/ISPRT,ICOUNT

	      XWTRAN(M2,N2,X,U2,C2,W02,W12,PHI02,PHI12);
	      THDATA(U2);
	      ZI[0] = 0.0;
	      ZI[1] = 1.0;
	      ICOUNT = ICOUNT + 1;
	
	      //  TWO EQUATIONS TO ELIMINATE POSSIBLE ROTATION OF THE INNER POLYGON:
	      wout = WQUAD(W12[N2-1],PHI12[N2-1],N2,1,W12[0],PHI12[0],1,
	    		     1,U2[0],M2,N2,U2[0],W02,W12,ALFA02,ALFA12,NPTQ2,QWORK2,LINEARC2,2);
	      scm.zmlt(C2[0], C2[1], wout[0], wout[1], cr, ci);
	      WIN1[0] = Z12[0][0] - Z12[N2-1][0] - cr[0];
	      WIN1[1] = Z12[0][1] - Z12[N2-1][1] - ci[0];
	      scm.zmlt(ZI[0], ZI[1], WIN1[0], WIN1[1], cr, ci);
	      FVAL[0] = ci[0];
	      FVAL[1] = WIN1[1];
	
	      //  N-1 SIDE LENGTH CONDITIONS FOR THE INNER POLYGON:
	      for (I = 1; I <= N2 - 1; I++) {
	          WINT1 = WQUAD(W12[I-1],PHI12[I-1],I,1,W12[I],PHI12[I],I+1,1,U2[0],M2,N2,
	                 U2[0],W02,W12,ALFA02,ALFA12,NPTQ2,QWORK2,LINEARC2,2);
	          scm.zmlt(C2[0], C2[1], WINT1[0], WINT1[1], cr, ci);
	          FVAL[I+1] = scm.zabs(Z12[I][0]-Z12[I-1][0],Z12[I][1]-Z12[I-1][1]) - scm.zabs(cr[0], ci[0]);
	      } // for (I = 1; I <= N2 - 1; I++)
	
	      //  TWO EQUATIONS TO FIX THE RELATIVE POSITION OF THE INNER POLYGON:
	      TEST1 = Math.cos(PHI12[N2-1]);
	      if (TEST1 < U2[0]) {
	
	          // IF THE LINE PATH FROM W02[M2-1] TO W12[N2-1] IS OUT OF DOMAIN,THE
              //  COMBINATION OF TWO DIFFERENT PATHS WILL BE USED INSTEAD:
	    	  WX[0] = U2[0];
	    	  WX[1] = 0.0;
	          WLINE = WQUAD(W02[M2-1],0.0,M2,0,WX,0.0,0,2,0.0,M2,N2,U2[0],W02,W12,ALFA02,
	             ALFA12,NPTQ2,QWORK2,0,2);
	          if (PHI12[N2-1] <= 0.0) {
	              WARC = WQUAD(W12[N2-1],PHI12[N2-1],N2,1,WX,0.0,0,2,U2[0],M2,N2,U2[0],W02,W12,
	                ALFA02,ALFA12,NPTQ2,QWORK2,1,2);
	              WIN2[0] = WLINE[0] - WARC[0];
	              WIN2[1] = WLINE[1] - WARC[1];
	          }
	          else {
	              WARC = WQUAD(WX,0.0,0,2,W12[N2-1],PHI12[N2-1],N2,1,U2[0],M2,N2,U2[0],W02,W12,
	                ALFA02,ALFA12,NPTQ2,QWORK2,1,2);
	              WIN2[0] = WLINE[0] + WARC[0];
	              WIN2[1] = WLINE[1] + WARC[1];
	          }
	      } // if (TEST1 < U2[0])
	      else {

	          WIN2 = WQUAD(W02[M2-1],0.0,M2,0,W12[N2-1],0.0,N2,1,0.0,M2,N2,U2[0],W02,W12,ALFA02,
	            ALFA12,NPTQ2,QWORK2,0,2);
	      }
	      scm.zmlt(C2[0], C2[1], WIN2[0], WIN2[1], cr, ci);
	      diffR = Z12[N2-1][0] - Z02[M2-1][0] - cr[0];
	      diffI = Z12[N2-1][1] - Z02[M2-1][1] - ci[0];
	      scm.zmlt(ZI[0], ZI[1], diffR, diffI, cr, ci);
	      FVAL[N2+1] = ci[0];
	      FVAL[N2+2] = diffI;
	
	      //  TWO EQUATIONS TO ELIMINATE POSSIBLE ROTATION OF THE OUTER POLYGON:
	      wout = WQUAD(W02[M2-1],PHI02[M2-1],M2,0,W02[0],PHI02[0],1,
	    		            0,1.0,M2,N2,U2[0],W02,W12,ALFA02,ALFA12,NPTQ2,QWORK2,LINEARC2,2);
	      scm.zmlt(C2[0], C2[1], wout[0], wout[1], cr, ci);
	      WIN3[0] = Z02[0][0] - Z02[M2-1][0] - cr[0];
	      WIN3[1] = Z02[0][1] - Z02[M2-1][1] - ci[0];
	      scm.zmlt(ZI[0],ZI[1], WIN3[0], WIN3[1], cr, ci);
	      FVAL[N2+3] = ci[0];
	      FVAL[N2+4] = WIN3[1];
	
	      if (M2 == 3) {
	
	          // CALCULATE THE MAXIMUM-NORM OF THE FUNCTION FVAL:
	          if (ISPRT == 1) {
	              FMAXN = FMAX(NDIM,FVAL);
	              System.out.println("# of iterations = " + ICOUNT);
	              System.out.println("Maximum norm of the residuals = " + FMAXN);
	          } // if (ISPRT == 1)
	          return;

	      } // if (M2 == 3)

	      if (ISHAPE2 != 1) {
	
	          //  M2-3 SIDE LENGTH CONDITIONS OF THE OUTER POLYGON:
	          for (J = 1; J <= M2 - 3; J++) {
	              WINT2 = WQUAD(W02[J-1],PHI02[J-1],J,0,W02[J],PHI02[J],J+1,0,1.0,
	                  M2,N2,U2[0],W02,W12,ALFA02,ALFA12,NPTQ2,QWORK2,LINEARC2,2);
	              scm.zmlt(C2[0], C2[1], WINT2[0], WINT2[1], cr, ci);
	              FVAL[N2+4+J] = scm.zabs(Z02[J][0]-Z02[J-1][0], Z02[J][1]-Z02[J-1][1]) - scm.zabs(cr[0], ci[0]);
	          } // for (J = 1; J <= M2 - 3; J++)
	
	          //  CALCULATE THE MAXIMUM-NORM OF THE FUNCTION FVAL:
	          if (ISPRT == 1) {
	              FMAXN = FMAX(NDIM,FVAL);
	              System.out.println("# of iterations = " + ICOUNT);
	              System.out.println("Maximum norm of the residuals = " + FMAXN);
	          } // if (ISPRT == 1)
	          return;
	      } // if (ISHAPE2 != 1)
	
	      //  OUTER POLYGON CONTAINS SOME INFINITE VERTICES & FOR EACH OF THEM
	      //  TWO LENGTH CONDITIONS WILL BE REPLACED BY A COMPLEX INTEGRAL:
	      for (K = 1; K <= NSHAPE - 1; K++) {
	          if (IND[K] != 1 && IND[K-1] < IND[K]-2) {
	              for (J = IND[K-1] + 2; J <= IND[K] - 1; J++) {
	                  WINT3 = WQUAD(W02[J-1],PHI02[J-1],J,0,W02[J],PHI02[J],J+1,0,
	                     1.0,M2,N2,U2[0],W02,W12,ALFA02,ALFA12,NPTQ2,QWORK2,LINEARC2,2);
	                  scm.zmlt(C2[0], C2[1], WINT3[0], WINT3[1], cr, ci);
	                  FVAL[N2+4+J] = scm.zabs(Z02[J][0]-Z02[J-1][0], Z02[J][1]-Z02[J-1][1]) - scm.zabs(cr[0], ci[0]);
	              } // for (J = IND[K-1] + 2; J <= IND[K] - 1; J++) 
	          } // if (IND[K] != 1 && IND[K-1] < IND[K]-2)
	          if (K == NSHAPE-1 || IND[K] == M2-2) {
	        	  continue;
	          }
	
	          //  THE COMBINATION  OF THREE DIFFERENT PATHS  IS USED TO INTEGRATE
	          //  FROM WA TO WB TO AVOID DOMAIN PROBLEM.THE BY-PRODUCT OF THIS IS
	          //  THAT IT IS NUMERICALLY  MORE EFFICIENT THAN A SINGLE LINE PATH:
	          WA[0] = W02[IND[K]-1][0];
	          WA[1] = W02[IND[K]-1][1];
	          WB[0] = W02[IND[K]+1][0];
	          WB[1] = W02[IND[K]+1][1];
	          PHIA = PHI02[IND[K]-1];
	          PHIB = PHI02[IND[K]+1];
	          RADIUS = (1.0+U2[0])/2.0;
	          scm.zmlt(ZI[0], ZI[1], PHIA, 0.0, cr, ci);
	          base = RADIUS * Math.exp(cr[0]);
	          WAI[0] = base * Math.cos(ci[0]);
	          WAI[1] = base * Math.sin(ci[0]);
	          scm.zmlt(ZI[0], ZI[1], PHIB, 0.0, cr, ci);
	          base = RADIUS * Math.exp(cr[0]);
	          WBI[0] = base * Math.cos(ci[0]);
	          WBI[1] = base * Math.sin(ci[0]);
	          WLINE1 = WQUAD(WA,0.0,IND[K]-1,0,WAI,0.0,0,2,0.0,M2,N2,U2[0],
	                     W02,W12,ALFA02,ALFA12,NPTQ2,QWORK2,0,2);
	          WLINE2 = WQUAD(WB,0.0,IND[K]+1,0,WBI,0.0,0,2,0.0,M2,N2,U2[0],
	                     W02,W12,ALFA02,ALFA12,NPTQ2,QWORK2,0,2);
	          WCIRCLE = WQUAD(WAI,PHIA,0,2,WBI,PHIB,0,2,RADIUS,M2,N2,U2[0],W02,W12,
	                   ALFA02,ALFA12,NPTQ2,QWORK2,1,2);
	          scm.zmlt(C2[0], C2[1], (WLINE1[0]+WCIRCLE[0]-WLINE2[0]),
	        		  (WLINE1[1]+WCIRCLE[1]-WLINE2[1]), cr, ci);
	          WIN4[0] = cr[0];
	          WIN4[1] = ci[0];
	          diffR = Z02[IND[K]+1][0]-Z02[IND[K]-1][0]-WIN4[0];
	          diffI = Z02[IND[K]+1][1]-Z02[IND[K]-1][1]-WIN4[1];
	          scm.zmlt(ZI[0], ZI[1], diffR, diffI, cr, ci);
	          FVAL[N2+5+IND[K]-1] = ci[0];
	          FVAL[N2+5+IND[K]] = diffI;
	      } // for (K = 1; K <= NSHAPE - 1; K++)
	
	      //  CALCULATE THE MAXIMUM-NORM OF THE FUNCTION FVAL:
	      if (ISPRT == 1) {
              FMAXN = FMAX(NDIM,FVAL);
              System.out.println("# of iterations = " + ICOUNT);
              System.out.println("Maximum norm of the residuals = " + FMAXN);
          } // if (ISPRT == 1)
          return;
	}


	
	private void DSCSOLV(double TOL,int IGUESS,int M,int N, double U[],
			double C[], double W0[][], double W1[][], double PHI0[],
			double PHI1[], double Z0[][],  double Z1[][], double ALFA0[],
		   double ALFA1[], int NPTQ, double QWORK[], int ISHAPE, int LINEARC) {
	//  -----------------------------------------------------------------
	//  SOLVES THE NONLINEAR SYSTEM FOR D-SC PARAMETERS.
	//  CALLING SEQUENCE:
	//  TOL       A TOLERANCE TO CONTROL THE CONVERGENCE IN HYBRD
	//  IGUESS    (=0 )A NON-EQUALLY SPACED INITIAL GUESS OR(=1)THE
	//            OTHER  EQUALLY-SPACED  INITIAL GUESS PROVIDED, OR
	//            (=2)USER-SUPPLIED INITIAL GUESS WHICH IS REQUIRED
	//            TO BE THE ARGUMENTS OF THE INITIAL PREVERTICES.
	//            ROUTINE ARGUM MAY BE USED FOR COMPUTING ARGUMENTS
	//            IF NEEDED. NOTE: C WILL BE COMPUTED IN THE ROUTINE
	//            (NOT SUPPLIED!)
	//  M,N,U,C,W0,W1,PHI0,PHI1,ALFA0,ALFA1,Z0,Z1
	//            CONSTITUTE THE GEOMETRY OF THE POLYGONAL REGION
	//            AND THE MAPPING FUNCTION. ON RETURN U,C,W0,& W1
	//            WILL  CONTAIN COMPUTED PARAMETERS. (PHI0 & PHI1
	//            WILL CONTAIN THE ARGUMENTS OF THE PREVERTICES.)
	//  QWORK     SEE CALLING SEQUENCE DOCUMENTATION IN QINIT.THE ARRAY
	//            MUST HAVE BEEN FILLED BY QINIT BEFORE CALLING DSCSOLV.
	//  NPTQ      THE NUMBER OF GAUSS-JACOBI POINTS USED
	//  ISHAPE    INDICATES  THAT  OUTER POLYGON  CONTAINS NO INFINITE
	//            VERTICES (ISHAPE=0) OR IT HAS SOME INFINITE VERTICES
	//            (ISHAPE=1).
	//  LINEARC   INTEGER VARIABLE TO CONTROL INTEGRATION PATH.IN PATICULAR:
	//            LINEARC=0:INTEGRATING ALONG LINE SEGMENT WHENEVER POSSIBLE
	//            LINEARC=1:INTEGRATING ALONG CIRCULAR ARC WHENEVER POSSIBLE
	//  THE DISCRIPTION OF ARRAY IND & VARIABLE NSHAPE NEEDED IN DSCFUN:
	//  IND       CONTAINS INDICES  CORRESPONDING TO  THOSE INFINITE
	//            VERTICES,BUT THE FIRST & THE LAST ENTRIS MUST BE -1
	//           & M-2(M IS THE # OF VERTICES ON THE OUTER POLYGON).
	//  NSHAPE    THE DIMENSION OF THE INTERGE ARRAY IND(NSHAPE)
	
	
	//     .. Scalar Arguments ..
	//      DOUBLE COMPLEX C
	      
	//     .. Array Arguments ..
	//      DOUBLE COMPLEX W0(M),W1(N),Z0(M),Z1(N)
	//      DOUBLE PRECISION ALFA0(M),ALFA1(N),PHI0(M),PHI1(N),
	//                      QWORK(NPTQ* (2* (N+M)+3))
		
		// Local scalars
		double cr[] = new double[1];
		double ci[] = new double[1];
		double C1[] = new double[2];
		double WINT[];
		double ZI[] = new double[2];
		double AVE, BOTM, DSTEP, FACTOR, PI, TOP;
		int INFO[] = new int[1];
		int NFEV[] = new int[1];
		int I, K, KM, KN, MAXFUN, NM, NWDIM;
		
		// Local arrays
		double DIAG[] = new double[42];
		double FJAC[][] = new double[42][42];
		double FVAL[] = new double[42];
		double QW[] = new double[1114];
		double X[] = new double[42];
		
	    // Common blocks ..
	    // COMMON /PARAM1/W02,W12,Z02,Z12,C2
	    // COMMON /PARAM2/U2,PHI02,PHI12,ALFA02,ALFA12,QWORK2
	    // COMMON /PARAM3/M2,N2,NPTQ2,ISHAPE2,LINEARC2,NSHAPE,IND
	    // COMMON /PARAM4/UARY,VARY,DLAM,IU
	    // COMMON /PARAM5/ISPRT,ICOUNT

		ZI[0] = 0.0;
		ZI[1] = 1.0;
		PI = Math.PI;
		ICOUNT = 0;
		if (ISHAPE != 0) {
			NSHAPE = 1;
			for (I = 2; I <= M-1; I++) {
			    if (ALFA0[I-1] >= 0.0) {
			    	continue;
			    }
			    NSHAPE = NSHAPE + 1;
			    IND[NSHAPE-1] = I-1;
			} // for (I = 2; I <= M-1; I++)
			IND[0] = -1;
			NSHAPE = NSHAPE + 1;
			IND[NSHAPE-1] = M-2;
		} // if (ISHAPE != 0)
		
		// Fix one prevertex
		W0[M-1][0] = 1.0;
		W0[M-1][1] = 0.0;
		PHI0[M-1] = 0.0;
		
		// Following two value assignments are to satisfy the compiler WATFOR77:
		X[1] = 0.0;
		X[2] = 0.0;
		// Initial guess (IGUESS = 0):
		if (IGUESS == 0) {
		    X[0] = 1.0/0.5 - 1.0/0.46;
		    AVE = 2.0 * PI/(double)N;
		    for (I = 1; I <= N-2; I++) {
		    	X[2 + I] = Math.log((AVE + 0.0001*I)/(AVE + 0.0001*(I+1)));
		    } // for (I = 1; I <= N-2; I++)
		    X[N+1] = Math.log((AVE+0.0001*(N-1))/(2.0*PI-(N-1)* (AVE+N*0.00005)));
		    X[N+2] = 1.0/ (4.0-0.1) - 1.0/ (4.0+0.1);
		    AVE = 2.0*PI/M;
		    for (I = 1; I <= M - 2; I++) {
		        X[N+2+I] = Math.log((AVE+0.0001*(I-1))/(AVE+0.0001*I));
		    } // for (I = 1; I <= M - 2; I++)
		    X[M+N+1] = Math.log((AVE+0.0001*(M-2))/(2.0*PI-(M-1)* (AVE+(M-2)*0.00005)));
		} // if (IGUESS == 0)
		else if (IGUESS == 1) {
			//  INITIAL GUESS (IGUESS=1):
		    X[0] = 1.0/0.53 - 1.0/0.43;
		    for (I = 1; I <= N - 1; I++) {
		        X[2+I] = 0.0;
		    } // for (I = 1; I <= N - 1; I++)
		    X[N+2] = 1.0/ (4.0-0.1) - 1.0/ (4.0+0.1);
		    for (I = 1; I <= M - 1; I++) {
		        X[N+2+I] = 0.0;
		    } // for (I = 1; I <= M - 1; I++)
		} // else if (IGUESS == 1)
		else {
			X[0] = 1.0/ (0.98-U[0]) - 1.0/ (U[0]-0.02);
			for (K = 1; K <= N - 1; K++) {
			    KN = K - 1;
			    if (KN == 0) {
			    	KN = N;
			    }
			    TOP = PHI1[K-1] - PHI1[KN-1];
			    if (TOP < 0.0) {
			    	TOP = TOP + 2.0*PI;
			    }
			    BOTM = PHI1[K] - PHI1[K-1];
			    if (BOTM < 0.0) {
			    	BOTM = BOTM + 2.0*PI;
			    }
			    X[2+K] = Math.log(TOP) - Math.log(BOTM);
			} // for (K = 1; K <= N - 1; K++)
			X[N+2] = 1.0/ (PI-PHI1[N-1]) - 1.0/ (PI+PHI1[N-1]);
			for (K = 1; K <= M - 1; K++) {
			    KM = K - 1;
			    if (KM == 0) {
			    	KM = M;
			    }
			    TOP = PHI0[K-1] - PHI0[KM-1];
			    if (TOP < 0.0) {
			    	TOP = TOP + 2.0*PI;
			    }
			    BOTM = PHI0[K] - PHI0[K-1];
			    if (BOTM < 0.0) {
			    	BOTM = BOTM + 2.0*PI;
			    }
			    X[N+2+K] = Math.log(TOP) - Math.log(BOTM);
			} // for (K = 1; K <= M - 1; K++)
		} // else
		
		// CALCULATE THE INITIAL GUESS X[1] & X[2] TO MATCH
		// THE CHOICE FOR X[0],X[3],...,X[M+N+1]:
		XWTRAN(M,N,X,U,C,W0,W1,PHI0,PHI1);
        THDATA(U);
        WINT = WQUAD(W0[M-1],0.0,M,0,W1[N-1],0.0,N,1,0.0,M,N,U[0],W0,W1,ALFA0,
        	            ALFA1,NPTQ,QWORK,0,2);
        scm.zdiv(Z1[N-1][0] - Z0[M-1][0], Z1[N-1][1] - Z0[M-1][1], WINT[0], WINT[1], cr, ci);
        C1[0] = cr[0];
        C1[1] = ci[0];
        scm.zmlt(ZI[0], ZI[1], C1[0], C1[1], cr, ci);
        X[1] = ci[0];
        X[2] = C1[1];
        
        //  HYBRD CONTROL PARAMETERS:
        DSTEP = 1.0E-7;
        MAXFUN = 200* (M+N);
        FACTOR = 2.0;
        NM = M + N + 2;
      
        //  COPY RELEVANT DATA TO THE COMMON BLOCK IN DSCFUN:
        M2 = M;
        N2 = N;
        ISHAPE2 = ISHAPE;
        LINEARC2 = LINEARC;
        NPTQ2 = NPTQ;
        for (K = 0; K < M; K++) {
            W02[K][0] = W0[K][0];
            W02[K][1] = W0[K][1];
            PHI02[K] = PHI0[K];
            Z02[K][0] = Z0[K][0];
            Z02[K][1] = Z0[K][1];
            ALFA02[K] = ALFA0[K];
        } // for (K = 0; K < M; K++) 
        for (K = 0; K < N; K++) {
            W12[K][0] = W1[K][0];
            W12[K][1] = W1[K][1];
            PHI12[K] = PHI1[K];
            Z12[K][0] = Z1[K][0];
            Z12[K][1] = Z1[K][1];
            ALFA12[K] = ALFA1[K];
        } // for (K = 0; K < N; K++)
        NWDIM = NPTQ* (2* (M+N)+3);
        for (I = 0; I < NWDIM; I++) {
            QWORK2[I] = QWORK[I];
        } // for (I = 0; I < NWDIM; I++)
        
        if (ISPRT != 1) {
            System.out.println("Time for obtaining a converged result may range");
            System.out.println("from several seconds to several minutes or longer");
            System.out.println("depending on the complexity of the geometry of the");
            System.out.println("region and the local computing system");
            System.out.println("so be patient");
        }
        
        // Solve nonlinear system with hybrid
        double QTF[] = new double[NM];
        double WA1[] = new double[NM];
        double WA2[] = new double[NM];
        double WA3[] = new double[NM];
        double WA4[] = new double[NM];
        HYBRD(dscfun, NM, X, FVAL, TOL, MAXFUN, NM, NM, DSTEP, DIAG, 1, FACTOR,
        	  -1, INFO, NFEV, FJAC, 42, QW, 903, QTF, WA1, WA2, WA3, WA4);
        for (I = 0; I < NM; I++) {
        	QW[I+903] = QTF[I];
        }
	}

	
	private void ANGLES(int MN, double Z01[][], double ALFA01[], int I01) {
	    // Computes the interior angles of a doubly
		// connected and bounded polygonal region.
		// I01 := 0(outer polygon) := 1(inner polygon)
		// MN: The number of vertices
		// Z01: Array containing vertices
		// ALFA01: Array containing interior turning angles on return
		
		int K, KL, KR;
		double cr[] = new double[1];
		double ci[] = new double[1];
		
		for (K = 1; K <= MN; K++) {
			KL = (K+MN-2)%MN + 1;
			KR = K%MN + 1;
			scm.zdiv(Z01[KL-1][0] - Z01[K-1][0], Z01[KL-1][1] - Z01[K-1][1], 
					 Z01[KR-1][0] - Z01[K-1][0], Z01[KR-1][1] - Z01[K-1][1], cr, ci);
			ALFA01[K-1] = Math.atan2(ci[0], cr[0])/Math.PI;
			if (ALFA01[K-1] <= 0.0) {
				ALFA01[K-1] = ALFA01[K-1] + 2.0;
			}
		} // for (K = 1; K <= MN; K++)
		if (I01 == 1) {
			for (K = 1; K <= MN; K++) {
			    if (ALFA01[K-1] != 2.0) {
			    	ALFA01[K-1] = 2.0 - ALFA01[K-1];
			    }
			} // for (K = 1; K <= MN; K++)
		} // if (I01 == 1)
		return;
	}
	
	private double DIST(int M,int N, double W0[][], double W1[][],
			double W[], int KWA, int IC) {
	//   -----------------------------------
	//    DETERMINES THE DISTANCE FROM W TO THE NEAREST SINGULARITY
	//    OTHER THAN W ITSELF.( W COULD BE ONE OF THE PREVERTICES.)
	//    KWA IS THE INDEX OF W IN W0 (IF IC=0) OR W1 (IF IC=1), OR
	//    WK COULD BE 0 (IF ONE CHOOSES) WHEN W IS NOT A PREVERTEX.
    //
	//     .. Scalar Arguments ..
	//     DOUBLE COMPLEX W
	
	//     .. Array Arguments ..
	//      DOUBLE COMPLEX W0(M),W1(N)
		
	//     .. Local Scalars ..
	      double D;
	      int I;
	      double result;
	
	      result = 2.0;
	      for (I = 1; I <= M; I++) {
	          D = scm.zabs(W[0]-W0[I-1][0],W[1]-W0[I-1][1]);
	          if (I == KWA && IC == 0) {
	        	  continue;
	          }
	          result = Math.min(result,D);
	      } // for (I = 1; I <= M; I++)
	      for (I = 1; I <= N; I++) {
	          D = scm.zabs(W[0]-W1[I-1][0], W[1]-W1[I-1][1]);
	          if (I == KWA && IC == 1) {
	        	  continue;
	          }
	          result = Math.min(result,D);
	      } // for (I = 1; I <= N; I++)
	      return result;

	}

	
	private void CHECK(double ALFA0[], double ALFA1[], int M, int N, int ISHAPE) {
		//   CHECKS IF THE INPUT DATA AND PARAMETERS ARE CORRECT.
		//   NOTE1:    ANGLE-CHECKING MAKES SENSE ONLY IF ANGLES
		//             ARE USER-PROVIDED.
		//   NOTE2:    USERS ARE RESPONSIBLE FOR CHECKING THE FOLLOWING:
		//   1. EACH COMPONENT OF THE OUTER POLYGON CONTAINS AT LEAST ONE
		//      FINITE VERTEX.
		//   2. Z1(N) IS THE CLOSEST (OR ALMOST CLOSEST)
		//      INNER VERTEX TO Z1(M).
		//   3. COUNTERCLOCKWISE ORIENTATION OF THE VERTICES.
		
		// ALFA0(M), ALFA1(N)
		double EPS, SUM;
		int K, MK;
		
		if (!((M >= 3) && (M <= 30) && (N <= 30) && (M+N <= 40))) {
			System.err.println("M must be no less than 3");
			System.err.println("M, N must be no greater than 30");
			System.err.println("M+N must be no greater than 40");
			System.exit(-1);
		}
	
		EPS = 0.00001;
		SUM = 0.0;
		MK = 0;
		for (K = 0; K <= M; K++) {
			if (ALFA0[K] > 0.0) {
				MK = MK + 1;
			}
			SUM = SUM + ALFA0[K];
		} // for (K = 0; K <= M; K++)
		if (!((MK < M && ISHAPE == 1) || (MK == M && ISHAPE == 0))) {
			System.err.println("For finite regions ISHAPE must be 0");
			System.err.println("and for infinite regions ISHAPE must be 1");
			System.exit(-1);
		}
		
		if (Math.abs(SUM - (M-2)) >= EPS) {
			System.err.println("Some angles for outer polygon are wrong");
			System.exit(-1);
		}
		
		SUM = 0.0;
		for (K = 0; K < N; K++) {
			SUM = SUM + ALFA1[K];
		}
		if (Math.abs(SUM - (N+2)) >= EPS) {
			System.err.println("Some angles for inner polygon are wrong");
			System.exit(-1);
		}
		
		if (ALFA0[0] <= 0.0) {
			System.err.println("Z0[0] must be finite");
			System.exit(-1);
		}
		
		if (ALFA0[M-1] <= 0.0) {
			System.err.println("Z0[M-1] must be finite");
			System.exit(-1);
		}
		
		if (ALFA0[M-3] <= 0.0) {
			System.err.println("Z0[M-3] must be finite");
			System.exit(-1);
		}
		
		if (ALFA0[M-2] >= 2.0-EPS) {
			System.err.println("Z0[M-2] must no be a re-entrant corner");
			System.exit(-1);
		}
		
		if ((ALFA0[M-2] >= 1.0-EPS) && (ALFA0[M-2] <= 1.0+EPS)) {
			System.err.println("Z0[M-2] must not be an artificial corner");
			System.exit(-1);
		}
		
		System.out.println("Inputs have been checked with no error being found");
		return;
	}
	
	private void HYBRD(int FCN,int N, double X[], double FVEC[], double XTOL, int MAXFEV,
			int ML, int MU, double EPSFCN, double DIAG[], int MODE,
		    double FACTOR, int NPRINT,int INFO[], int NFEV[], double FJAC[][],
		    int LDFJAC, double R[],int LR, double QTF[], double WA1[],
		    double WA2[], double WA3[], double WA4[]) {
		//     ***********
		//
		//     SUBROUTINE HYBRD
		//
		//     THE PURPOSE OF HYBRD IS TO FIND A ZERO OF A SYSTEM OF
		//     N NONLINEAR FUNCTIONS IN N VARIABLES BY A MODIFICATION
		//     OF THE POWELL HYBRID METHOD. THE USER MUST PROVIDE A
		//     SUBROUTINE WHICH CALCULATES THE FUNCTIONS. THE JACOBIAN IS
		//     THEN CALCULATED BY A FORWARD-DIFFERENCE APPROXIMATION.
		//
		//     THE SUBROUTINE STATEMENT IS
		//
		//       SUBROUTINE HYBRD(FCN,N,X,FVEC,XTOL,MAXFEV,ML,MU,EPSFCN,
		//                        DIAG,MODE,FACTOR,NPRINT,INFO,NFEV,FJAC,
		//                        LDFJAC,R,LR,QTF,WA1,WA2,WA3,WA4)
		//
		//     WHERE
	    //      dscfun = 1 for DSCFUN
		//       FCN IS THE NAME OF THE USER-SUPPLIED SUBROUTINE WHICH
		//         CALCULATES THE FUNCTIONS. FCN MUST BE DECLARED
		//         IN AN EXTERNAL STATEMENT IN THE USER CALLING
		//         PROGRAM, AND SHOULD BE WRITTEN AS FOLLOWS.
		//
		//         SUBROUTINE FCN(N,X,FVEC,IFLAG)
		//         INTEGER N,IFLAG
		//         DOUBLE PRECISION X(N),FVEC(N)
		//         ----------
		//         CALCULATE THE FUNCTIONS AT X AND
		//         RETURN THIS VECTOR IN FVEC.
		//         ---------
		//         RETURN
		//         END
		//
		//         THE VALUE OF IFLAG SHOULD NOT BE CHANGED BY FCN UNLESS
		//         THE USER WANTS TO TERMINATE EXECUTION OF HYBRD.
		//         IN THIS CASE SET IFLAG TO A NEGATIVE INTEGER.
		//
		//       N IS A POSITIVE INTEGER INPUT VARIABLE SET TO THE NUMBER
		//         OF FUNCTIONS AND VARIABLES.
		//
		//       X IS AN ARRAY OF LENGTH N. ON INPUT X MUST CONTAIN
		//         AN INITIAL ESTIMATE OF THE SOLUTION VECTOR. ON OUTPUT X
		//         CONTAINS THE FINAL ESTIMATE OF THE SOLUTION VECTOR.
		//
		//       FVEC IS AN OUTPUT ARRAY OF LENGTH N WHICH CONTAINS
		//         THE FUNCTIONS EVALUATED AT THE OUTPUT X.
		//
		//       XTOL IS A NONNEGATIVE INPUT VARIABLE. TERMINATION
		//         OCCURS WHEN THE RELATIVE ERROR BETWEEN TWO CONSECUTIVE
		//         ITERATES IS AT MOST XTOL.
		//
		//       MAXFEV IS A POSITIVE INTEGER INPUT VARIABLE. TERMINATION
		//         OCCURS WHEN THE NUMBER OF CALLS TO FCN IS AT LEAST MAXFEV
		//         BY THE END OF AN ITERATION.
		//
		//       ML IS A NONNEGATIVE INTEGER INPUT VARIABLE WHICH SPECIFIES
		//         THE NUMBER OF SUBDIAGONALS WITHIN THE BAND OF THE
		//         JACOBIAN MATRIX. IF THE JACOBIAN IS NOT BANDED, SET
		//         ML TO AT LEAST N - 1.
		//
		//       MU IS A NONNEGATIVE INTEGER INPUT VARIABLE WHICH SPECIFIES
		//         THE NUMBER OF SUPERDIAGONALS WITHIN THE BAND OF THE
		//         JACOBIAN MATRIX. IF THE JACOBIAN IS NOT BANDED, SET
		//         MU TO AT LEAST N - 1.
		//
		//       EPSFCN IS AN INPUT VARIABLE USED IN DETERMINING A SUITABLE
		//         STEP LENGTH FOR THE FORWARD-DIFFERENCE APPROXIMATION. THIS
		//         APPROXIMATION ASSUMES THAT THE RELATIVE ERRORS IN THE
		//         FUNCTIONS ARE OF THE ORDER OF EPSFCN. IF EPSFCN IS LESS
		//         THAN THE MACHINE PRECISION, IT IS ASSUMED THAT THE RELATIVE
		//         ERRORS IN THE FUNCTIONS ARE OF THE ORDER OF THE MACHINE
		//         PRECISION.
		//
		//       DIAG IS AN ARRAY OF LENGTH N. IF MODE = 1 (SEE
		//         BELOW), DIAG IS INTERNALLY SET. IF MODE = 2, DIAG
		//         MUST CONTAIN POSITIVE ENTRIES THAT SERVE AS
		//         MULTIPLICATIVE SCALE FACTORS FOR THE VARIABLES.
		//
		//       MODE IS AN INTEGER INPUT VARIABLE. IF MODE = 1, THE
		//         VARIABLES WILL BE SCALED INTERNALLY. IF MODE = 2,
		//         THE SCALING IS SPECIFIED BY THE INPUT DIAG. OTHER
		//         VALUES OF MODE ARE EQUIVALENT TO MODE = 1.
		//
		//       FACTOR IS A POSITIVE INPUT VARIABLE USED IN DETERMINING THE
		//         INITIAL STEP BOUND. THIS BOUND IS SET TO THE PRODUCT OF
		//         FACTOR AND THE EUCLIDEAN NORM OF DIAG*X IF NONZERO, OR ELSE
		//         TO FACTOR ITSELF. IN MOST CASES FACTOR SHOULD LIE IN THE
		//         INTERVAL (.1,100.). 100. IS A GENERALLY RECOMMENDED VALUE.
		//
		//       NPRINT IS AN INTEGER INPUT VARIABLE THAT ENABLES CONTROLLED
		//         PRINTING OF ITERATES IF IT IS POSITIVE. IN THIS CASE,
		//         FCN IS CALLED WITH IFLAG = 0 AT THE BEGINNING OF THE FIRST
		//         ITERATION AND EVERY NPRINT ITERATIONS THEREAFTER AND
		//         IMMEDIATELY PRIOR TO RETURN, WITH X AND FVEC AVAILABLE
		//         FOR PRINTING. IF NPRINT IS NOT POSITIVE, NO SPECIAL CALLS
		//         OF FCN WITH IFLAG = 0 ARE MADE.
		//
		//       INFO IS AN INTEGER OUTPUT VARIABLE. IF THE USER HAS
		//         TERMINATED EXECUTION, INFO IS SET TO THE (NEGATIVE)
		//         VALUE OF IFLAG. SEE DESCRIPTION OF FCN. OTHERWISE,
		//         INFO IS SET AS FOLLOWS.
		//
		//         INFO = 0   IMPROPER INPUT PARAMETERS.
		//
		//         INFO = 1   RELATIVE ERROR BETWEEN TWO CONSECUTIVE ITERATES
		//                    IS AT MOST XTOL.
		//
		//         INFO = 2   NUMBER OF CALLS TO FCN HAS REACHED OR EXCEEDED
		//                    MAXFEV.
		//
		//         INFO = 3   XTOL IS TOO SMALL. NO FURTHER IMPROVEMENT IN
		//                    THE APPROXIMATE SOLUTION X IS POSSIBLE.
		//
		//         INFO = 4   ITERATION IS NOT MAKING GOOD PROGRESS, AS
		//                    MEASURED BY THE IMPROVEMENT FROM THE LAST
		//                    FIVE JACOBIAN EVALUATIONS.
		//
		//         INFO = 5   ITERATION IS NOT MAKING GOOD PROGRESS, AS
		//                    MEASURED BY THE IMPROVEMENT FROM THE LAST
		//                    TEN ITERATIONS.
		//
		//       NFEV IS AN INTEGER OUTPUT VARIABLE SET TO THE NUMBER OF
		//         CALLS TO FCN.
		//
		//       FJAC IS AN OUTPUT N BY N ARRAY WHICH CONTAINS THE
		//         ORTHOGONAL MATRIX Q PRODUCED BY THE QR FACTORIZATION
		//         OF THE FINAL APPROXIMATE JACOBIAN.
		//
		//       LDFJAC IS A POSITIVE INTEGER INPUT VARIABLE NOT LESS THAN N
		//         WHICH SPECIFIES THE LEADING DIMENSION OF THE ARRAY FJAC.
		//
		//       R IS AN OUTPUT ARRAY OF LENGTH LR WHICH CONTAINS THE
		//         UPPER TRIANGULAR MATRIX PRODUCED BY THE QR FACTORIZATION
		//         OF THE FINAL APPROXIMATE JACOBIAN, STORED ROWWISE.
		//
		//       LR IS A POSITIVE INTEGER INPUT VARIABLE NOT LESS THAN
		//         (N*(N+1))/2.
		//
		//       QTF IS AN OUTPUT ARRAY OF LENGTH N WHICH CONTAINS
		//         THE VECTOR (Q TRANSPOSE)*FVEC.
		//
		//       WA1, WA2, WA3, AND WA4 ARE WORK ARRAYS OF LENGTH N.
		//
		//     SUBPROGRAMS CALLED
		//
		//       USER-SUPPLIED ...... FCN
		//
		//       MINPACK-SUPPLIED ... DOGLEG,DPMPAR,ENORM,FDJAC1,
		//                            QFORM,QRFAC,R1MPYQ,R1UPDT
		//
		//       FORTRAN-SUPPLIED ... DABS,DMAX1,DMIN1,MIN0,MOD
		//
		//     ARGONNE NATIONAL LABORATORY. MINPACK PROJECT. MARCH 1980.
		//     BURTON S. GARBOW, KENNETH E. HILLSTROM, JORGE J. MORE
		//
		//     **********
		//     .. Scalar Arguments ..
		//      DOUBLE PRECISION EPSFCN,FACTOR,XTOL
		//      INTEGER INFO,LDFJAC,LR,MAXFEV,ML,MODE,MU,N,NFEV,NPRINT
		//     ..
		//     .. Array Arguments ..
		//      DOUBLE PRECISION DIAG(N),FJAC(LDFJAC,N),FVEC(N),QTF(N),R(LR),
		//                      WA1(N),WA2(N),WA3(N),WA4(N),X(N)
		//     ..
		//     .. Subroutine Arguments ..
		//      EXTERNAL FCN
		//     ..
		//     .. Local Scalars ..
		      double ACTRED,EPSMCH,FNORM,FNORM1,ONE,P0001,P001,
		                      P1,P5,PNORM,PRERED,RATIO,SUM,TEMP,XNORM,ZERO;
		      double DELTA = 0.0;
		      int IFLAG[] = new int[1];
		      int I,ITER,J,JM1,L,MSUM,NCFAIL,NCSUC,NSLOW1,NSLOW2;
		      boolean JEVAL,SING;
		//     ..
	    //     .. Local Arrays ..
		      int IWA[] = new int[1];
		//     ..
		//     .. External Functions ..
		//      DOUBLE PRECISION DPMPAR,ENORM
		//      EXTERNAL DPMPAR,ENORM
		//     ..
		//     .. External Subroutines ..
		//      EXTERNAL DOGLEG,FDJAC1,QFORM,QRFAC,R1MPYQ,R1UPDT
		//     ..
		//     .. Intrinsic Functions ..
		//      INTRINSIC DABS,DMAX1,DMIN1,MIN0,MOD
		//     ..
	    //     .. Data statements ..
		//
		ONE = 1.0;
		P1 = 1.0E-1;
		P5 = 5.0E-1;
		P001 = 1.0E-3;
		P0001 = 1.0E-4;
		ZERO = 0.0;
		//
		//     ..
		//
		//     EPSMCH IS THE MACHINE PRECISION.
		//
		EPSMCH = MACHEP;
		//
		INFO[0] = 0;
		IFLAG[0] = 0;
		NFEV[0] = 0;
		//
		//     CHECK THE INPUT PARAMETERS FOR ERRORS.
		//
		if (N <= 0 || XTOL < ZERO || MAXFEV <= 0 || ML < 0 ||
		         MU < 0 || FACTOR <= ZERO || LDFJAC < N ||
		         LR < (N* (N+1))/2) {
			if (NPRINT > 0) {
				if (FCN == dscfun) {
					DSCFUN(N, X, FVEC, IFLAG);
				}
				return;
			} // if (NPRINT > 0)
			return;
		}
		if (MODE == 2) {
		    for (J = 0; J < N; J++) {
		        if (DIAG[J] <= ZERO) {
		        	if (NPRINT > 0) {
						if (FCN == dscfun) {
							DSCFUN(N, X, FVEC, IFLAG);
						}
					} // if (NPRINT > 0)
		        	return;
		        } // if (DIAG[J] <= ZERO)
		    } // for (J = 0; J < N; J++)
		} // if (mode == 2)  
		//
		//     EVALUATE THE FUNCTION AT THE STARTING POINT
		//     AND CALCULATE ITS NORM.
		//
		IFLAG[0] = 1;
		if (FCN == dscfun) {
		    DSCFUN(N,X,FVEC,IFLAG);
		}
		NFEV[0] = 1;
		if (IFLAG[0] < 0) {
		    INFO[0] = IFLAG[0];
		    IFLAG[0] = 0;
		    if (NPRINT > 0) {
				if (FCN == dscfun) {
					DSCFUN(N, X, FVEC, IFLAG);
				}
			} // if (NPRINT > 0)
        	return;
		} // if (IFLAG[0] < 0)
		FNORM = ENORM(N,FVEC);
		
		// DETERMINE THE NUMBER OF CALLS TO FCN NEEDED TO COMPUTE
		// THE JACOBIAN MATRIX.
		
		MSUM = Math.min(ML+MU+1,N);
		
	    // INITIALIZE ITERATION COUNTER AND MONITORS.
		
		ITER = 1;
		NCSUC = 0;
		NCFAIL = 0;
		NSLOW1 = 0;
		NSLOW2 = 0;
		
		// BEGINNING OF THE OUTER LOOP.
		
		outer: while (true) {
		    JEVAL = true;
		
            // CALCULATE THE JACOBIAN MATRIX.
		
		    IFLAG[0] = 2;
		    FDJAC1(FCN,N,X,FVEC,FJAC,LDFJAC,IFLAG,ML,MU,EPSFCN,WA1,WA2);
		    NFEV[0] = NFEV[0] + MSUM;
		    if (IFLAG[0] < 0) {
		    	break;
		    }
		
		    // COMPUTE THE QR FACTORIZATION OF THE JACOBIAN.
		
		    QRFAC(N,N,FJAC,LDFJAC,false,IWA,1,WA1,WA2,WA3);
		
		    // ON THE FIRST ITERATION AND IF MODE IS 1, SCALE ACCORDING
		    // TO THE NORMS OF THE COLUMNS OF THE INITIAL JACOBIAN.
		
		    if (ITER == 1) {
		        if (MODE != 2) {
		            for (J = 0; J < N; J++) {
		                DIAG[J] = WA2[J];
		                if (WA2[J] == ZERO){ 
		                	DIAG[J] = ONE;	
		                }
		            } // for (J = 0; J < N; J++)
		        } // if (MODE != 2)
		
		        // ON THE FIRST ITERATION, CALCULATE THE NORM OF THE SCALED X
		        // AND INITIALIZE THE STEP BOUND DELTA.
		
		        for (J = 0; J < N; J++) {
		            WA3[J] = DIAG[J]*X[J];
		        } // for (J = 0; J < N; J++)
		        XNORM = ENORM(N,WA3);
		        DELTA = FACTOR*XNORM;
		        if (DELTA == ZERO) {
		        	DELTA = FACTOR;
		        }
		    } // if (ITER == 1)
		
		    // FORM (Q TRANSPOSE)*FVEC AND STORE IN QTF.
		
		    for (I = 0; I < N; I++) {
		          QTF[I] = FVEC[I];
		    } // for (I = 0; I < N; I++)
		    for (J = 0; J < N; J++) {
		        if (FJAC[J][J] == ZERO) {
		        	continue;
		        }
		        SUM = ZERO;
		        for (I = J; I < N; I++) {
		            SUM = SUM + FJAC[I][J]*QTF[I];
		        } // for (I = J; I < N; I++)
		        TEMP = -SUM/FJAC[J][J];
		        for (I = J; I < N; I++) {
		            QTF[I] = QTF[I] + FJAC[I][J]*TEMP;
		        } // for (I = J; I < N; I++)
		    } // for (J = 0; J < N; J++)
		
		    // COPY THE TRIANGULAR FACTOR OF THE QR FACTORIZATION INTO R.
		
		      SING = false;
		      for (J = 1; J <= N; J++) {
		          L = J;
		          JM1 = J - 1;
		          if (JM1 >= 1) {
		              for (I = 1; I <= JM1; I++) {
		                  R[L-1] = FJAC[I-1][J-1];
		                  L = L + N - I;
		              } // for (I = 1; I <= JM1; I++)
		          } // if (JM1 >= 1) 
		          R[L-1] = WA1[J-1];
		          if (WA1[J-1] == ZERO) {
		        	  SING = true;
		          }
		      } // for (J = 1; J <= N; J++)
		
	          // ACCUMULATE THE ORTHOGONAL FACTOR IN FJAC.
		
		      QFORM(N,N,FJAC,LDFJAC,WA1);
		
		      // RESCALE IF NECESSARY.
		
		     if (MODE != 2) {
		         for (J = 0; J < N; J++) {
		              DIAG[J] = Math.max(DIAG[J],WA2[J]);
		         } // for (J = 0; J < N; J++)
		     } // if (MODE != 2)
		
		     // BEGINNING OF THE INNER LOOP.
		
		     while (true) {
		
		         // IF REQUESTED, CALL FCN TO ENABLE PRINTING OF ITERATES.
		
		         if  (NPRINT > 0) {
		             IFLAG[0] = 0;
		             if ((ITER-1%NPRINT) == 0) {
		            	 if (FCN == dscfun) {
		            	     DSCFUN(N,X,FVEC,IFLAG);
		            	 }
		             }
		             if (IFLAG[0] < 0) {
		            	 break outer;
		             }
		         } // if (NPRINT > 0)
		
		         // DETERMINE THE DIRECTION P.
		
		         DOGLEG(N,R,LR,DIAG,QTF,DELTA,WA1,WA2,WA3);
		
		         // STORE THE DIRECTION P AND X + P. CALCULATE THE NORM OF P.
		
		         for (J = 0; J < N; J++) {
		             WA1[J] = -WA1[J];
		             WA2[J] = X[J] + WA1[J];
		             WA3[J] = DIAG[J]*WA1[J];
		         } // for (J = 0; J < N; J++)
		         PNORM = ENORM(N,WA3);
		
		         // ON THE FIRST ITERATION, ADJUST THE INITIAL STEP BOUND.
		
		         if (ITER == 1) {
		        	 DELTA = Math.min(DELTA,PNORM);
		         }
		
		         // EVALUATE THE FUNCTION AT X + P AND CALCULATE ITS NORM.
		
		         IFLAG[0] = 1;
		         if (FCN == dscfun) {
		             DSCFUN(N,WA2,WA4,IFLAG);
		         }
		         NFEV[0] = NFEV[0] + 1;
		         if (IFLAG[0] < 0) {
		        	 break outer;
		         }
		         FNORM1 = ENORM(N,WA4);
		
		         // COMPUTE THE SCALED ACTUAL REDUCTION.
		
		         ACTRED = -ONE;
		      /*IF (FNORM1.LT.FNORM) ACTRED = ONE - (FNORM1/FNORM)**2
		C
		C           COMPUTE THE SCALED PREDICTED REDUCTION.
		C
		      L = 1
		      DO 220 I = 1,N
		          SUM = ZERO
		          DO 210 J = I,N
		              SUM = SUM + R(L)*WA1(J)
		              L = L + 1
		  210     CONTINUE
		          WA3(I) = QTF(I) + SUM
		  220 CONTINUE
		      TEMP = ENORM(N,WA3)
		      PRERED = ZERO
		      IF (TEMP.LT.FNORM) PRERED = ONE - (TEMP/FNORM)**2
		C
		C           COMPUTE THE RATIO OF THE ACTUAL TO THE PREDICTED
		C           REDUCTION.
		C
		      RATIO = ZERO
		      IF (PRERED.GT.ZERO) RATIO = ACTRED/PRERED
		C
		C           UPDATE THE STEP BOUND.
		C
		      IF (RATIO.GE.P1) GO TO 230
		      NCSUC = 0
		      NCFAIL = NCFAIL + 1
		      DELTA = P5*DELTA
		      GO TO 240

		  230 CONTINUE
		      NCFAIL = 0
		      NCSUC = NCSUC + 1
		      IF (RATIO.GE.P5 .OR. NCSUC.GT.1) DELTA = DMAX1(DELTA,PNORM/P5)
		      IF (DABS(RATIO-ONE).LE.P1) DELTA = PNORM/P5
		  240 CONTINUE
		C
		C           TEST FOR SUCCESSFUL ITERATION.
		C
		      IF (RATIO.LT.P0001) GO TO 260
		C
		C           SUCCESSFUL ITERATION. UPDATE X, FVEC, AND THEIR NORMS.
		C
		      DO 250 J = 1,N
		          X(J) = WA2(J)
		          WA2(J) = DIAG(J)*X(J)
		          FVEC(J) = WA4(J)
		  250 CONTINUE
		      XNORM = ENORM(N,WA2)
		      FNORM = FNORM1
		      ITER = ITER + 1
		  260 CONTINUE
		C
		C           DETERMINE THE PROGRESS OF THE ITERATION.
		C
		      NSLOW1 = NSLOW1 + 1
		      IF (ACTRED.GE.P001) NSLOW1 = 0
		      IF (JEVAL) NSLOW2 = NSLOW2 + 1
		      IF (ACTRED.GE.P1) NSLOW2 = 0
		C
		C           TEST FOR CONVERGENCE.
		C
		      IF (DELTA.LE.XTOL*XNORM .OR. FNORM.EQ.ZERO) INFO = 1
		      IF (INFO.NE.0) GO TO 300
		C
		C           TESTS FOR TERMINATION AND STRINGENT TOLERANCES.
		C
		      IF (NFEV.GE.MAXFEV) INFO = 2
		      IF (P1*DMAX1(P1*DELTA,PNORM).LE.EPSMCH*XNORM) INFO = 3
		      IF (NSLOW2.EQ.5) INFO = 4
		      IF (NSLOW1.EQ.10) INFO = 5
		      IF (INFO.NE.0) GO TO 300
		C
		C           CRITERION FOR RECALCULATING JACOBIAN APPROXIMATION
		C           BY FORWARD DIFFERENCES.
		C
		      IF (NCFAIL.EQ.2) GO TO 290
		C
		C           CALCULATE THE RANK ONE MODIFICATION TO THE JACOBIAN
		C           AND UPDATE QTF IF NECESSARY.
		C
		      DO 280 J = 1,N
		          SUM = ZERO
		          DO 270 I = 1,N
		              SUM = SUM + FJAC(I,J)*WA4(I)
		  270     CONTINUE
		          WA2(J) = (SUM-WA3(J))/PNORM
		          WA1(J) = DIAG(J)* ((DIAG(J)*WA1(J))/PNORM)
		          IF (RATIO.GE.P0001) QTF(J) = SUM
		  280 CONTINUE
		C
		C           COMPUTE THE QR FACTORIZATION OF THE UPDATED JACOBIAN.
		C
		      CALL R1UPDT(N,N,R,LR,WA1,WA2,WA3,SING)
		      CALL R1MPYQ(N,N,FJAC,LDFJAC,WA2,WA3)
		      CALL R1MPYQ(1,N,QTF,1,WA2,WA3)*/
		
		         // END OF THE INNER LOOP.
		
		          JEVAL = false;
		     } // while (true)
		
		    // END OF THE OUTER LOOP.
		
		} // while (true)

		//     TERMINATION, EITHER NORMAL OR USER IMPOSED.
		
		if (IFLAG[0] < 0) {
			INFO[0] = IFLAG[0];
		}
		IFLAG[0] = 0;
		if (NPRINT > 0) {
			if (FCN == dscfun) {
			    DSCFUN(N,X,FVEC,IFLAG);
			}
		}
		return;
      }
	
	private void DOGLEG(int N, double R[], int LR, double DIAG[], double QTB[],
			double DELTA, double X[], double WA1[], double WA2[]) {
	    //     **********
	
	    // SUBROUTINE DOGLEG
	
	    // GIVEN AN M BY N MATRIX A, AN N BY N NONSINGULAR DIAGONAL
	    // MATRIX D, AN M-VECTOR B, AND A POSITIVE NUMBER DELTA, THE
	    // PROBLEM IS TO DETERMINE THE CONVEX COMBINATION X OF THE
	    // GAUSS-NEWTON AND SCALED GRADIENT DIRECTIONS THAT MINIMIZES
	    // (A*X - B) IN THE LEAST SQUARES SENSE, SUBJECT TO THE
        // RESTRICTION THAT THE EUCLIDEAN NORM OF D*X BE AT MOST DELTA.
	
        // THIS SUBROUTINE COMPLETES THE SOLUTION OF THE PROBLEM
	    // IF IT IS PROVIDED WITH THE NECESSARY INFORMATION FROM THE
	    // QR FACTORIZATION OF A. THAT IS, IF A = Q*R, WHERE Q HAS
	    // ORTHOGONAL COLUMNS AND R IS AN UPPER TRIANGULAR MATRIX,
	    // THEN DOGLEG EXPECTS THE FULL UPPER TRIANGLE OF R AND
	    // THE FIRST N COMPONENTS OF (Q TRANSPOSE)*B.
	
	    // THE SUBROUTINE STATEMENT IS
	
	    // SUBROUTINE DOGLEG(N,R,LR,DIAG,QTB,DELTA,X,WA1,WA2)
	
	    // WHERE
	
	    // N IS A POSITIVE INTEGER INPUT VARIABLE SET TO THE ORDER OF R.
	
	    // R IS AN INPUT ARRAY OF LENGTH LR WHICH MUST CONTAIN THE UPPER
	    //   TRIANGULAR MATRIX R STORED BY ROWS.
	
	    // LR IS A POSITIVE INTEGER INPUT VARIABLE NOT LESS THAN
	    //    (N*(N+1))/2.
	
	    // DIAG IS AN INPUT ARRAY OF LENGTH N WHICH MUST CONTAIN THE
	    //   DIAGONAL ELEMENTS OF THE MATRIX D.
	
	    // QTB IS AN INPUT ARRAY OF LENGTH N WHICH MUST CONTAIN THE FIRST
	    //   N ELEMENTS OF THE VECTOR (Q TRANSPOSE)*B.
	
	    // DELTA IS A POSITIVE INPUT VARIABLE WHICH SPECIFIES AN UPPER
	    //   BOUND ON THE EUCLIDEAN NORM OF D*X.
	
	    // X IS AN OUTPUT ARRAY OF LENGTH N WHICH CONTAINS THE DESIRED
	    //   CONVEX COMBINATION OF THE GAUSS-NEWTON DIRECTION AND THE
	    //   SCALED GRADIENT DIRECTION.
	
	    // WA1 AND WA2 ARE WORK ARRAYS OF LENGTH N.
	
	    // SUBPROGRAMS CALLED
	
	    // MINPACK-SUPPLIED ... DPMPAR,ENORM
	
	    // ARGONNE NATIONAL LABORATORY. MINPACK PROJECT. MARCH 1980.
	    // BURTON S. GARBOW, KENNETH E. HILLSTROM, JORGE J. MORE
	
	    //     **********
	    // .. Scalar Arguments ..
	    // DOUBLE PRECISION DELTA
	    // INTEGER LR,N
	
	    //.. Array Arguments ..
	    // DOUBLE PRECISION DIAG(N),QTB(N),R(LR),WA1(N),WA2(N),X(N)
	
	    // .. Local Scalars ..
	    double ALPHA,BNORM,EPSMCH,GNORM,ONE,QNORM,SGNORM,SUM,TEMP,ZERO;
	    double ratio, ratio2, ratio3;
	    int I,J,JJ,JP1,K,L;

	    // .. External Functions ..
	    // DOUBLE PRECISION DPMPAR,ENORM
	    // EXTERNAL DPMPAR,ENORM
	
	    // .. Data statements ..
	    ONE = 1.0;
	    ZERO = 0.0;
	
	    // EPSMCH IS THE MACHINE PRECISION.
	
	      EPSMCH = MACHEP;
	
	      // FIRST, CALCULATE THE GAUSS-NEWTON DIRECTION.
	
	      JJ = (N* (N+1))/2 + 1;
	      for (K = 1; K <= N; K++) {
	          J = N - K + 1;
	          JP1 = J + 1;
	          JJ = JJ - K;
	          L = JJ + 1;
	          SUM = ZERO;
	          if (N >= JP1) {
	              for (I = JP1; I <= N; I++) {
	                  SUM = SUM + R[L-1]*X[I-1];
	                  L = L + 1;
	              } // for (I = JP1; I <= N; I++)
	          } // if (N >= JP1)
	          TEMP = R[JJ-1];
	          if (TEMP == ZERO) {
	              L = J;
	              for (I = 1; I <= J; I++) {
	                  TEMP = Math.max(TEMP,Math.abs(R[L-1]));
	                  L = L + N - I;
	              } // for (I = 1; I <= J; I++)
	              TEMP = EPSMCH*TEMP;
	              if (TEMP == ZERO) {
	            	  TEMP = EPSMCH;
	              }
	          } // if (TEMP == ZERO)
	          X[J-1] = (QTB[J-1]-SUM)/TEMP;
	      } // for (K = 1; K <= N; K++)
	
	      // TEST WHETHER THE GAUSS-NEWTON DIRECTION IS ACCEPTABLE.
	
	      for (J = 0; J < N; J++) {
	          WA1[J] = ZERO;
	          WA2[J] = DIAG[J]*X[J];
	      } // for (J = 0; J < N; J++)
	      QNORM = ENORM(N,WA2);
	      if (QNORM <= DELTA) {
	    	  return;
	      }
	
	      // THE GAUSS-NEWTON DIRECTION IS NOT ACCEPTABLE.
	      // NEXT, CALCULATE THE SCALED GRADIENT DIRECTION.
	
	      L = 0;
	      for (J = 0; J < N; J++) {
	          TEMP = QTB[J];
	          for (I = J; I < N; I++) {
	              WA1[I] = WA1[I] + R[L]*TEMP;
	              L = L + 1;
	          } // for (I = J; I < N; I++)
	          WA1[J] = WA1[J]/DIAG[J];
	      } // for (J = 0; J < N; J++)
	
	      // CALCULATE THE NORM OF THE SCALED GRADIENT AND TEST FOR
	      // THE SPECIAL CASE IN WHICH THE SCALED GRADIENT IS ZERO.
	
	      GNORM = ENORM(N,WA1);
	      SGNORM = ZERO;
	      ALPHA = DELTA/QNORM;
	      if (GNORM != ZERO) {
	
	          // CALCULATE THE POINT ALONG THE SCALED GRADIENT
	          // AT WHICH THE QUADRATIC IS MINIMIZED.
	
	          for (J = 0; J < N; J++) {
	              WA1[J] = (WA1[J]/GNORM)/DIAG[J];
	          } // for (J = 0; J < N; J++)
	          L = 0;
	          for (J = 0; J < N; J++) {
	              SUM = ZERO;
	              for (I = J; I < N; I++) {
	                  SUM = SUM + R[L]*WA1[I];
	                  L = L + 1;
	              } // for (I = J; I < N; I++)
	              WA2[J] = SUM;
	          } // for (J = 0; J < N; J++)
	          TEMP = ENORM(N,WA2);
	          SGNORM = (GNORM/TEMP)/TEMP;
	
	          // TEST WHETHER THE SCALED GRADIENT DIRECTION IS ACCEPTABLE.
	
	          ALPHA = ZERO;
	          if  (SGNORM < DELTA) {
	
	              // THE SCALED GRADIENT DIRECTION IS NOT ACCEPTABLE.
	              // FINALLY, CALCULATE THE POINT ALONG THE DOGLEG
	              // AT WHICH THE QUADRATIC IS MINIMIZED.
	
	              BNORM = ENORM(N,QTB);
	              TEMP = (BNORM/GNORM)* (BNORM/QNORM)* (SGNORM/DELTA);
	              ratio = SGNORM/DELTA;
	              ratio2 = TEMP - (DELTA/QNORM);
	              ratio3 = DELTA/QNORM;
	              TEMP = TEMP - (DELTA/QNORM)* ratio*ratio +
	                  Math.sqrt(ratio2*ratio2+(ONE- ratio3*ratio3)* (ONE- ratio*ratio));
	              ALPHA = ((DELTA/QNORM)* (ONE- ratio*ratio))/TEMP;
	          } // if  (SGNORM < DELTA)
	      } // if (GNORM != ZERO)
	
	      // FORM APPROPRIATE CONVEX COMBINATION OF THE GAUSS-NEWTON
	      // DIRECTION AND THE SCALED GRADIENT DIRECTION.
	
	      TEMP = (ONE-ALPHA)*Math.min(SGNORM,DELTA);
	      for (J = 0; J < N; J++) {
	          X[J] = TEMP*WA1[J] + ALPHA*X[J];
	      } // for (J = 0; J < N; J++)
	      return;
    }

	
	private void QINIT(int M, int N, double ALFA0[], double ALFA1[], int NPTQ, double QWORK[]) {
	    // Computes the Gauss-Jacobi nodes & weights for Gauss-Jacobi quadrature.  Work array
		// QWORK must be dimensioned at least NPTQ*(2(M+N) + 3).  It is divided up into
		// 2(M+N)+3 vectors of length NPTQ:  The first M+N+1 contain quadrature nodes on output,
		// the next M+N+1 contain the corresponding weights on output, and the last one is a scratch
		// vector used by GAUSSJ.  NPTQ is the number of G-J nodes (same as weights) used.  See
		// comment on routine WPROD for the rest of the calling sequence.
		
		// For each finite vertex, compute nodes & weights
		// for one-sided Gauss-Jacobi quadrature:
		
		// ALFA0(M), ALFA1(N), QWORK(1660)
		double ALPHA;
		int INODES, ISCR, IWTS, J, K;
		int i;
		// B is used for input scratch array, T  for nodes output and W for weights output
		double B[] = new double[NPTQ];
		double T[] = new double[NPTQ];
		double W[] = new double[NPTQ];
		
		ISCR = NPTQ * (2 * (M+N) + 2) + 1;
		for (K = 1; K <= M + N; K++) {
		    INODES = NPTQ * (K-1) + 1;
		    IWTS = NPTQ * (M+N+K) + 1;
		    if (K <= M) {
		    	ALPHA = ALFA0[K-1] - 1.0;
		    	if (ALFA0[K-1] > 0.0) {
		    	    GAUSSJ(NPTQ, 0.0, ALPHA, B, T, W);
		    	    for (i = 0; i < NPTQ; i++) {
		    	    	QWORK[INODES-1+i] = T[i];
		    	    	QWORK[IWTS-1+i] = W[i];
		    	    }
		    	} // if (ALFA0[K-1] > 0.0)
		    	else {
		    		for (J = 0; J < NPTQ; J++) {
		    			QWORK[IWTS+J-1] = 0.0;
		    			QWORK[INODES+J-1] = 0.0;
		    		}
		    	} // else
		    } // if (K <= M)
		    else {
		    	ALPHA = ALFA1[K-M-1] - 1.0;
		    	GAUSSJ(NPTQ, 0.0, ALPHA, B, T, W);
	    	    for (i = 0; i < NPTQ; i++) {
	    	    	QWORK[INODES-1+i] = T[i];
	    	    	QWORK[IWTS-1+i] = W[i];
	    	    }
		    } // else
		    
		    // Take singularities into account in advance for the purpose of saving
		    // certain amount of calculation in WQSUM:
		    for (J = 0; J < NPTQ; J++) {
		    	QWORK[IWTS+J-1] = QWORK[IWTS+J-1] * Math.pow((1.0 + QWORK[INODES+J-1]), (-ALPHA));
		    }
		} // for (K = 1; K <= M + N; K++)
		
		// Compute weights and nodes for pure Gaussian quadrature:
		INODES = NPTQ *(M+N) + 1;
		IWTS = NPTQ * (2* (M+N) + 1) + 1;
		GAUSSJ(NPTQ, 0.0, 0.0, B, T, W);
		for (i = 0; i < NPTQ; i++) {
	    	QWORK[INODES-1+i] = T[i];
	    	QWORK[IWTS-1+i] = W[i];
	    }
		return;
	}
	
	private void GAUSSJ(int N, double ALPHA, double BETA, double B[], double T[], double W[]) {
	    // B(N), T(N), W(N)
	    //        THIS ROUTINE COMPUTES THE NODES T(J) AND WEIGHTS
		//        W(J) FOR GAUSS-JACOBI QUADRATURE FORMULAS.
		//        THESE ARE USED WHEN ONE WISHES TO APPROXIMATE
		//
		//                 INTEGRAL (FROM A TO B)  F(X) W(X) DX
		//
		//                              N
		//        BY                   SUM W  F(T )
		//                             J=1  J    J
		//
		//        (W(X) AND W(J) HAVE NO CONNECTION WITH EACH OTHER)
		//        WHERE W(X) IS THE WEIGHT FUNCTION
		//
		//                   W(X) = (1-X)**ALPHA * (1+X)**BETA
		//
		//        ON (-1, 1), ALPHA, BETA .GT. -1.
		//
		//     INPUT:
		//
		//        N        THE NUMBER OF POINTS USED FOR THE QUADRATURE RULE
		//        ALPHA    SEE ABOVE
		//        BETA     SEE ABOVE
		//        B        REAL SCRATCH ARRAY OF LENGTH N
		//
		//     OUTPUT PARAMETERS (BOTH DOUBLE PRECISION ARRAYS OF LENGTH N)
		//
		//        T        WILL CONTAIN THE DESIRED NODES.
		//        W        WILL CONTAIN THE DESIRED WEIGHTS W(J).
		//
		//     SUBROUTINES REQUIRED: CLASS, IMTQL2
		//
		//     REFERENCE:
		//
		//        THE ROUTINE HAS BEEN ADAPTED FROM THE MORE GENERAL
		//        ROUTINE GAUSSQ BY GOLUB AND WELSCH.  SEE
		//        GOLUB, G. H., AND WELSCH, J. H., "CALCULATION OF GAUSSIAN
		//        QUADRATURE RULES," MATHEMATICS OF COMPUTATION 23 (APRIL,
		//        1969), PP. 221-230.
		
		double MUZERO[] = new double[1];
		int I;
		int IERR[] = new int[1];
		
		CLASS(N, ALPHA, BETA, B, T, MUZERO);
        W[0] = 1.0;
        for (I = 1; I < N; I++) {
        	W[I] = 0.0;
        }
        IMTQL2(N, T, B, W, IERR);
        for (I = 0; I < N; I++) {
        	W[I] = MUZERO[0]*W[I]*W[I];
        }
        return;
	}
	
	private void CLASS(int N, double ALPHA, double BETA, double B[], double A[], double MUZERO[]) {
	//
	//           THIS PROCEDURE SUPPLIES THE COEFFICIENTS A(J), B(J) OF THE
	//        RECURRENCE RELATION
	//
	//             B P (X) = (X - A ) P   (X) - B   P   (X)
	//              J J            J   J-1       J-1 J-2
	//
	//        FOR THE VARIOUS CLASSICAL (NORMALIZED) ORTHOGONAL POLYNOMIALS,
	//        AND THE ZERO-TH MOMENT
	//
	//             MUZERO = INTEGRAL W(X) DX
	//
	//        OF THE GIVEN POLYNOMIAL'S WEIGHT FUNCTION W(X).  SINCE THE
	//        POLYNOMIALS ARE ORTHONORMALIZED, THE TRIDIAGONAL MATRIX IS
	//        GUARANTEED TO BE SYMMETRIC.
	
	//      DOUBLE PRECISION A(N),B(N)
	
	      double A2B2,AB,ABI;
	      int I,NM1;
	//
	//     .. External Functions ..
	//     double DGAMMA
	//      EXTERNAL DGAMMA
	
	      NM1 = N - 1;
	//
	      AB = ALPHA + BETA;
	      ABI = 2.0 + AB;
	      MUZERO[0] = Math.pow(2.0, (AB+1.0))*DGAMMA(ALPHA+1.0)*
	              DGAMMA(BETA+1.0)/DGAMMA(ABI);
	      A[0] = (BETA-ALPHA)/ABI;
	      B[0] = Math.sqrt(4.0* (1.0+ALPHA)* (1.0+BETA)/
	            ((ABI+1.0)*ABI*ABI));
	      A2B2 = BETA*BETA - ALPHA*ALPHA;
	      for (I = 2; I <= NM1; I++) {
	          ABI = 2.0*I + AB;
	          A[I-1] = A2B2/ ((ABI-2.0)*ABI);
	          B[I-1] = Math.sqrt(4.0*I* (I+ALPHA)* (I+BETA)* (I+AB)/
	                ((ABI*ABI-1)*ABI*ABI));
	      } // for (I = 2; I <= NM1; I++)
	      ABI = 2.0*N + AB;
	      A[N-1] = A2B2/ ((ABI-2.0)*ABI);
	      return;
	}
	
	private void IMTQL2(int N, double D[], double E[], double Z[], int IERR[]) {
		// This is a modified version of the EISPACK routine IMTQL2.
		// It finds the eigenvalues and first components of the eigenvectors
		// of a symmetric tridiagonal matrix by the implicit QL method.
		
		// D(N), E(N), Z(N)
		double B, C, F, G, P, R, S;
		int I, II, K, L, M, MML;
		int J = 0;
		boolean zeroJ = true;
		IERR[0] = 0;
		if (N == 1) {
			return;
		}
		
		E[N-1] = 0.0;
		for (L = 1; L <= N; L++) {
			if (zeroJ) {
		        J = 0;
			}
			else {
				zeroJ = true;
			}
		    // Look for small sub-diagonal element
		    for (M = L; M <= N; M++) {
		        if (M == N) {
		        	break;
		        }
		        if (Math.abs(E[M-1]) <= MACHEP * (Math.abs(D[M-1] + Math.abs(D[M])))) {
		        	break;
		        }
		    } // for (M = L; M <= N; M++)
		    
		    P = D[L-1];
		    if (M == L) {
		    	continue;
		    }
		    if (J == 30) {
		    	// Set error -- no convergence to an eigenvalue after 30 iterations.
		    	IERR[0] = L;
		    	return;
		    }
		    J = J+1;
		    // Form shift
		    G = (D[L] - P)/(2.0*E[L-1]);
		    R = Math.sqrt(G*G + 1.0);
		    if (G >= 0) {
		    	G = D[M-1] - P + E[L-1]/(G + Math.abs(R));
		    }
		    else {
		    	G = D[M-1] - P + E[L-1]/(G - Math.abs(R));	
		    }
		    S = 1.0;
		    C = 1.0;
		    P = 0.0;
		    MML = M - L;
		    // For I=M-1 step -1 until L do
		    for (II = 1; II <= MML; II++) {
		        I = M - II;
		        F = S * E[I-1];
		        B = C * E[I-1];
		        if (Math.abs(F) >= Math.abs(G)) {
		            C = G/F;
		            R = Math.sqrt(C*C + 1.0);
		            E[I] = F*R;
		            S = 1.0/R;
		            C = C*S;
		        }
		        else {
		            S= F/G;
		            R = Math.sqrt(S*S + 1.0);
		            E[I] = G*R;
		            C = 1.0/R;
		            S = S * C;
		        }
		        G = D[I] - P;
		        R = (D[I-1] - G)*S + 2.0*C*B;
		        P = S*R;
		        D[I] = G + P;
		        G = C*R - B;
		        // Form first component of vector
		        F = Z[I];
		        Z[I] = S * Z[I-1] + C * F;
		        Z[I-1] = C * Z[I-1] - S * F;
		    } // for (II = 1; II <= MML; II++)
		    
		    D[L-1] = D[L-1] - P;
		    E[L-1] = G;
		    E[M-1] = 0.0;
		    zeroJ = false;
		} // for (L = 1; L <= N; L++)
		
		// Order eigenvalues and eigenvectors
		for (II = 2; II <= N; II++) {
		    I = II - 1;
		    K = I;
		    P = D[I-1];
		    
		    for (J = II; J <= N; J++) {
		        if (D[J-1] >= P) {
		        	continue;
		        }
		        K = J;
		        P = D[J-1];
		    } // for (J = II; J <= N; J++)
		    
		    if (K == I) {
		    	continue;
		    }
		    D[K-1] = D[I-1];
		    D[I-1] = P;
		    P = Z[I-1];
		    Z[I-1] = Z[K-1];
		    Z[K-1] = P;
		} // for (II = 2; II <= N; II++)
		return;
	}

	private double DGAMMA(double X) {
		//
		// COMPUTES THE GAMMA FUNCTION VIA A SERIES EXPANSION FROM
		// ABRAMOWITZ & STEGUN
		//
		// L. N. TREFETHEN, 1/13/84
		//
	    double FAC, G, Y;
	    int I, II;
	    double result;
	    
	    double C[] = new double[]{1.0000000000000000,.5772156649015329,
	    	          -.6558780715202538,-.0420026350340952,
	    	          .1665386113822915,-.0421977345555443,
	    	          -.0096219715278770,.0072189432466630,
	    	          -.0011651675918591,-.0002152416741149,
	    	          .0001280502823882,-.0000201348547807,
	    	          -.0000012504934821,.0000011330272320,
	    	          -.0000002056338417,.0000000061160950,.0000000050020075,
	    	          -.0000000011812746,.0000000001043427,.0000000000077823,
	    	          -.0000000000036968,.0000000000005100,
	    	          -.0000000000000206,-.0000000000000054,
	    	          .0000000000000014,.0000000000000001};
	    
	    // ARGUMENT REDUCTION:
	        FAC = 1.0;
	        Y = X;
	        while (Y > 1.5) {
	            Y = Y - 1.0;
	            FAC = FAC*Y;
	        }

	     while (Y < 0.5) {
	        FAC = FAC/Y;
	        Y = Y + 1.0;
	     }
	  
	  // SERIES:
	     G = C[25];
	     for (I = 1; I <= 25; I++) {
	            II = 26 - I;
	            G = Y*G + C[II-1];
	     }
         G = Y*G;
         result = FAC/G;
         return result;
	}
	
	private double ENORM(int N, double X[]) {
	    //     **********
	    //
	    //     FUNCTION ENORM
	
	    // GIVEN AN N-VECTOR X, THIS FUNCTION CALCULATES THE
	    // EUCLIDEAN NORM OF X.
	
	    // THE EUCLIDEAN NORM IS COMPUTED BY ACCUMULATING THE SUM OF
	    // SQUARES IN THREE DIFFERENT SUMS. THE SUMS OF SQUARES FOR THE
	    // SMALL AND LARGE COMPONENTS ARE SCALED SO THAT NO OVERFLOWS
	    // OCCUR. NON-DESTRUCTIVE UNDERFLOWS ARE PERMITTED. UNDERFLOWS
	    // AND OVERFLOWS DO NOT OCCUR IN THE COMPUTATION OF THE UNSCALED
	    // SUM OF SQUARES FOR THE INTERMEDIATE COMPONENTS.
	    // THE DEFINITIONS OF SMALL, INTERMEDIATE AND LARGE COMPONENTS
	    // DEPEND ON TWO CONSTANTS, RDWARF AND RGIANT. THE MAIN
	    // RESTRICTIONS ON THESE CONSTANTS ARE THAT RDWARF**2 NOT
	    // UNDERFLOW AND RGIANT**2 NOT OVERFLOW. THE CONSTANTS
	    // GIVEN HERE ARE SUITABLE FOR EVERY KNOWN COMPUTER.
	
	    // THE FUNCTION STATEMENT IS
	
	    // DOUBLE PRECISION FUNCTION ENORM(N,X)
	
        // WHERE
	
	    // N IS A POSITIVE INTEGER INPUT VARIABLE.
	
	    // X IS AN INPUT ARRAY OF LENGTH N.
	
	    // SUBPROGRAMS CALLED
	
	    // FORTRAN-SUPPLIED ... DABS,DSQRT
	
	    // ARGONNE NATIONAL LABORATORY. MINPACK PROJECT. MARCH 1980.
	    // BURTON S. GARBOW, KENNETH E. HILLSTROM, JORGE J. MORE
	
	    // **********
	    // Scalar Arguments ..
	    // INTEGER N

	    // .. Array Arguments ..
	    // DOUBLE PRECISION X(N)

	    // .. Local Scalars ..
		double result = 0.0;
	    double AGIANT,FLOATN,S1,S2,S3,X1MAX,
	                      X3MAX,XABS,ratio;
	    int I;
	
	      // .. Data statements ..
	    double ONE = 1.0;
	    double ZERO = 0.0;
	    double RDWARF = 3.834E-20;
	    double RGIANT = 1.304E19;

	    S1 = ZERO;
	    S2 = ZERO;
	    S3 = ZERO;
	    X1MAX = ZERO;
	    X3MAX = ZERO;
	    FLOATN = N;
	    AGIANT = RGIANT/FLOATN;
	    for (I = 0; I < N; I++) {
	        XABS = Math.abs(X[I]);
	          if (XABS <= RDWARF || XABS >= AGIANT) {
	              if (XABS > RDWARF) {
	
	                  // SUM FOR LARGE COMPONENTS.
	
	                  if  (XABS > X1MAX) {
	                	  ratio = X1MAX/XABS;
	                      S1 = ONE + S1* ratio * ratio;
	                      X1MAX = XABS;
	                  }
	                  else {
	                	  ratio = XABS/X1MAX;
	                      S1 = S1 + ratio * ratio;
	                  }
	                  continue;

	              } //  if (XABS > RDWARF) 
	
	              // SUM FOR SMALL COMPONENTS.
	
	              if (XABS > X3MAX) {
	            	  ratio = X3MAX/XABS;
	                  S3 = ONE + S3* ratio * ratio;
	                  X3MAX = XABS;
	          }
	          else {
	              if (XABS != ZERO) {
	            	  ratio = XABS/X3MAX;
	            	  S3 = S3 + ratio * ratio;
	              }
	          }
	          continue;

	          } // if (XABS <= RDWARF || XABS >= AGIANT)
	
	          // SUM FOR INTERMEDIATE COMPONENTS.
	
	          S2 = S2 + XABS * XABS;
	    } // for (I = 0; I < N; I++)
	
        // CALCULATION OF NORM.
	
	      if (S1 != ZERO) {
	          result = X1MAX*Math.sqrt(S1+ (S2/X1MAX)/X1MAX);
	      }
	      else {
	          if (S2 != ZERO) {
	              if (S2 >= X3MAX) result = Math.sqrt(S2* (ONE+ (X3MAX/S2)* (X3MAX*S3)));
	              if (S2 < X3MAX) result = Math.sqrt(X3MAX* ((S2/X3MAX)+ (X3MAX*S3)));
	          }
	          else {
	              result = X3MAX*Math.sqrt(S3);
	          } // else
	      } // else
	      return result;
	}
	
	private void FDJAC1(int FCN,int N, double X[], double FVEC[], double FJAC[][], int LDFJAC,
			int IFLAG[] ,int ML,int MU,double EPSFCN, double WA1[], double WA2[]) {
        // **********
		
		//     SUBROUTINE FDJAC1
		
		//     THIS SUBROUTINE COMPUTES A FORWARD-DIFFERENCE APPROXIMATION
		//     TO THE N BY N JACOBIAN MATRIX ASSOCIATED WITH A SPECIFIED
		//     PROBLEM OF N FUNCTIONS IN N VARIABLES. IF THE JACOBIAN HAS
		//     A BANDED FORM, THEN FUNCTION EVALUATIONS ARE SAVED BY ONLY
		//     APPROXIMATING THE NONZERO TERMS.
		
		//     THE SUBROUTINE STATEMENT IS
		
		//       SUBROUTINE FDJAC1(FCN,N,X,FVEC,FJAC,LDFJAC,IFLAG,ML,MU,EPSFCN,
		//                         WA1,WA2)
		
		//     WHERE
        //         dscfun = 1 for DSCFUN
		//         FCN IS THE NAME OF THE USER-SUPPLIED SUBROUTINE WHICH
		//         CALCULATES THE FUNCTIONS. FCN MUST BE DECLARED
		//         IN AN EXTERNAL STATEMENT IN THE USER CALLING
		//         PROGRAM, AND SHOULD BE WRITTEN AS FOLLOWS.
		
		//         SUBROUTINE FCN(N,X,FVEC,IFLAG)
		//         INTEGER N,IFLAG
		//         DOUBLE PRECISION X(N),FVEC(N)
		//         ----------
		//         CALCULATE THE FUNCTIONS AT X AND
		//         RETURN THIS VECTOR IN FVEC.
		//         ----------
		//         RETURN
		//         END
		
		//         THE VALUE OF IFLAG SHOULD NOT BE CHANGED BY FCN UNLESS
		//         THE USER WANTS TO TERMINATE EXECUTION OF FDJAC1.
		//         IN THIS CASE SET IFLAG TO A NEGATIVE INTEGER.
		
		//       N IS A POSITIVE INTEGER INPUT VARIABLE SET TO THE NUMBER
		//         OF FUNCTIONS AND VARIABLES.
		
		//       X IS AN INPUT ARRAY OF LENGTH N.
		
		//       FVEC IS AN INPUT ARRAY OF LENGTH N WHICH MUST CONTAIN THE
		//         FUNCTIONS EVALUATED AT X.
		
		//       FJAC IS AN OUTPUT N BY N ARRAY WHICH CONTAINS THE
		//         APPROXIMATION TO THE JACOBIAN MATRIX EVALUATED AT X.
		
		//       LDFJAC IS A POSITIVE INTEGER INPUT VARIABLE NOT LESS THAN N
		//         WHICH SPECIFIES THE LEADING DIMENSION OF THE ARRAY FJAC.
		
		//       IFLAG IS AN INTEGER VARIABLE WHICH CAN BE USED TO TERMINATE
		//         THE EXECUTION OF FDJAC1. SEE DESCRIPTION OF FCN.
		
		//       ML IS A NONNEGATIVE INTEGER INPUT VARIABLE WHICH SPECIFIES
		//         THE NUMBER OF SUBDIAGONALS WITHIN THE BAND OF THE
		//         JACOBIAN MATRIX. IF THE JACOBIAN IS NOT BANDED, SET
		//         ML TO AT LEAST N - 1.
		
		//       EPSFCN IS AN INPUT VARIABLE USED IN DETERMINING A SUITABLE
		//         STEP LENGTH FOR THE FORWARD-DIFFERENCE APPROXIMATION. THIS
		//         APPROXIMATION ASSUMES THAT THE RELATIVE ERRORS IN THE
		//         FUNCTIONS ARE OF THE ORDER OF EPSFCN. IF EPSFCN IS LESS
		//         THAN THE MACHINE PRECISION, IT IS ASSUMED THAT THE RELATIVE
		//         ERRORS IN THE FUNCTIONS ARE OF THE ORDER OF THE MACHINE
		//         PRECISION.
		
		//       MU IS A NONNEGATIVE INTEGER INPUT VARIABLE WHICH SPECIFIES
		//         THE NUMBER OF SUPERDIAGONALS WITHIN THE BAND OF THE
		//         JACOBIAN MATRIX. IF THE JACOBIAN IS NOT BANDED, SET
		//         MU TO AT LEAST N - 1.
		
		//       WA1 AND WA2 ARE WORK ARRAYS OF LENGTH N. IF ML + MU + 1 IS AT
		//         LEAST N, THEN THE JACOBIAN IS CONSIDERED DENSE, AND WA2 IS
		//         NOT REFERENCED.
		
		//     SUBPROGRAMS CALLED
		//
		//       MINPACK-SUPPLIED ... DPMPAR
		
		//     ARGONNE NATIONAL LABORATORY. MINPACK PROJECT. MARCH 1980.
		//     BURTON S. GARBOW, KENNETH E. HILLSTROM, JORGE J. MORE
		
		//     **********
		//     .. Scalar Arguments ..
		//      DOUBLE PRECISION EPSFCN
		//      INTEGER IFLAG,LDFJAC,ML,MU,N
		
		//     .. Array Arguments ..
		//      DOUBLE PRECISION FJAC(LDFJAC,N),FVEC(N),WA1(N),WA2(N),X(N)
		
		//     .. Subroutine Arguments ..
		//      EXTERNAL FCN
		
		//     .. Local Scalars ..
		double EPS,EPSMCH,H,TEMP,ZERO;
		int I,J,K,MSUM;
		
		ZERO = 0.0;
		
		//     EPSMCH IS THE MACHINE PRECISION.
		
		EPSMCH = MACHEP;
		
		EPS = Math.sqrt(Math.max(EPSFCN,EPSMCH));
		MSUM = ML + MU + 1;
		if (MSUM >= N) {
		
		    // COMPUTATION OF DENSE APPROXIMATE JACOBIAN.
		
		    for (J = 0; J < N; J++) {
		        TEMP = X[J];
		        H = EPS*Math.abs(TEMP);
		        if (H == ZERO) H = EPS;
		        X[J] = TEMP + H;
		        if (FCN == dscfun) {
		            DSCFUN(N,X,WA1,IFLAG);
		        }
		        if (IFLAG[0] < 0) {
		        	return;
		        }
		        X[J] = TEMP;
		        for (I = 0; I < N; I++) {
		              FJAC[I][J] = (WA1[I]-FVEC[I])/H;
		        } // for (I = 0; I < N; I++)
		    } // for (J = 0; J < N; J++)
		    return;

		} // if (MSUM >= N)
		
		// COMPUTATION OF BANDED APPROXIMATE JACOBIAN.
		
		for (K = 1; K <= MSUM; K++) {
		    for (J = K; J <= N; J += MSUM) {
		        WA2[J-1] = X[J-1];
		        H = EPS*Math.abs(WA2[J-1]);
		        if (H == ZERO) {
		        	H = EPS;
		        }
		        X[J-1] = WA2[J-1] + H;
		    } // for (J = K; J <= N; J += MSUM)
		    if (FCN == dscfun) {
		        DSCFUN(N,X,WA1,IFLAG);
		    }
		    if (IFLAG[0] < 0) {
		    	return;
		    }
		    for (J = K; J <= N; J += MSUM) {
		        X[J-1] = WA2[J-1];
		        H = EPS*Math.abs(WA2[J-1]);
		        if (H == ZERO) {
		        	H = EPS;
		        }
		        for (I = 1; I <= N; I++) {
		            FJAC[I-1][J-1] = ZERO;
		            if (I >= J-MU && I <= J+ML) {
		            	FJAC[I-1][J-1] = (WA1[I-1] - FVEC[I-1])/H;
		            }
		        } // for (I = 1; I <= N; I++)
		    } // for (J = K; J <= N; J += MSUM)
		} // for (K = 1; K <= MSUM; K++)
		return;
	}
	
	private void QFORM(int M,int N, double Q[][], int LDQ, double WA[]) {
	    //     **********
	
	    // SUBROUTINE QFORM
	
	    // THIS SUBROUTINE PROCEEDS FROM THE COMPUTED QR FACTORIZATION OF
	    // AN M BY N MATRIX A TO ACCUMULATE THE M BY M ORTHOGONAL MATRIX
	    // Q FROM ITS FACTORED FORM.
	
	    // THE SUBROUTINE STATEMENT IS
	
	    // SUBROUTINE QFORM(M,N,Q,LDQ,WA)
	
	    // WHERE
	
	    // M IS A POSITIVE INTEGER INPUT VARIABLE SET TO THE NUMBER
	    //   OF ROWS OF A AND THE ORDER OF Q.
	
	    // N IS A POSITIVE INTEGER INPUT VARIABLE SET TO THE NUMBER
	    //   OF COLUMNS OF A.
	
	    // Q IS AN M BY M ARRAY. ON INPUT THE FULL LOWER TRAPEZOID IN
        //   THE FIRST MIN(M,N) COLUMNS OF Q CONTAINS THE FACTORED FORM.
	    //   ON OUTPUT Q HAS BEEN ACCUMULATED INTO A SQUARE MATRIX.
	
	    // LDQ IS A POSITIVE INTEGER INPUT VARIABLE NOT LESS THAN M
	    //   WHICH SPECIFIES THE LEADING DIMENSION OF THE ARRAY Q.
	
	    // WA IS A WORK ARRAY OF LENGTH M.
	
	    // SUBPROGRAMS CALLED
	
	    // ARGONNE NATIONAL LABORATORY. MINPACK PROJECT. MARCH 1980.
	    // BURTON S. GARBOW, KENNETH E. HILLSTROM, JORGE J. MORE
	
	    //      **********
        // .. Scalar Arguments ..
	    // INTEGER LDQ,M,N
	
	    // .. Array Arguments ..
	    // DOUBLE PRECISION Q(LDQ,M),WA(M)

	    //      .. Local Scalars ..
	    double ONE,SUM,TEMP,ZERO;
	    int I,J,JM1,K,L,MINMN,NP1;
	
	    //     .. Data statements ..
	    ONE = 1.0;
	    ZERO = 0.0;
	
	    // ZERO OUT UPPER TRIANGLE OF Q IN THE FIRST MIN(M,N) COLUMNS.
	
	    MINMN = Math.min(M,N);
	    if (MINMN >= 2) {
	        for (J = 2; J <= MINMN; J++) {
	          JM1 = J - 1;
	          for (I = 1; I <= JM1; I++) {
	              Q[I-1][J-1] = ZERO;
	          } // for (I = 1; I <= JM1; I++)
	        } // for (J = 2; J <= MINMN; J++)
	    } // if (MINMN >= 2) 
	
        // INITIALIZE REMAINING COLUMNS TO THOSE OF THE IDENTITY MATRIX.
	
	    NP1 = N + 1;
	    if (M >= NP1) {
	        for (J = NP1; J <= M; J++) {
	            for (I = 1; I <= M; I++) {
	              Q[I-1][J-1] = ZERO;
	            } // for (I = 1; I <= M; I++)
	            Q[J-1][J-1] = ONE;
	        } // for (J = NP1; J <= M; J++)
	    } // if (M >= NP1)
	
	    // ACCUMULATE Q FROM ITS FACTORED FORM.
	
	    for (L = 1; L <= MINMN; L++) {
	        K = MINMN - L + 1;
	        for (I = K; I <= M; I++) {
	            WA[I-1] = Q[I-1][K-1];
	            Q[I-1][K-1] = ZERO;
	        } // for (I = K; I <= M; I++)
	        Q[K-1][K-1] = ONE;
	        if (WA[K-1] == ZERO) {
	        	continue;
	        }
	        for (J = K; J <= M; J++) {
	            SUM = ZERO;
	            for (I = K; I <= M; I++) {
	                SUM = SUM + Q[I-1][J-1]*WA[I-1];
	            } // for (I = K; I <= M; I++)
	            TEMP = SUM/WA[K-1];
	            for (I = K; I <= M; I++) {
	                Q[I-1][J-1] = Q[I-1][J-1] - TEMP*WA[I-1];
	            } // for (I = K; I <= M; I++)
	        } // for (J = K; J <= M; J++)
	    } // for (L = 1; L <= MINMN; L++)
	      return;
	}

	
	private void QRFAC(int M,int N, double A[][], int LDA, boolean PIVOT,
			int IPVT[], int LIPVT, double RDIAG[], double ACNORM[], double WA[]) {
	    //     **********
	
	    // SUBROUTINE QRFAC
	
	    // THIS SUBROUTINE USES HOUSEHOLDER TRANSFORMATIONS WITH COLUMN
	    // PIVOTING (OPTIONAL) TO COMPUTE A QR FACTORIZATION OF THE
	    // M BY N MATRIX A. THAT IS, QRFAC DETERMINES AN ORTHOGONAL
	    // MATRIX Q, A PERMUTATION MATRIX P, AND AN UPPER TRAPEZOIDAL
	    // MATRIX R WITH DIAGONAL ELEMENTS OF NONINCREASING MAGNITUDE,
	    // SUCH THAT A*P = Q*R. THE HOUSEHOLDER TRANSFORMATION FOR
	    // COLUMN K, K = 1,2,...,MIN(M,N), IS OF THE FORM
	
	    //                           T
	    //           I - (1/U(K))*U*U
	
        // WHERE U HAS ZEROS IN THE FIRST K-1 POSITIONS. THE FORM OF
	    // THIS TRANSFORMATION AND THE METHOD OF PIVOTING FIRST
	    // APPEARED IN THE CORRESPONDING LINPACK SUBROUTINE.
	
	    // THE SUBROUTINE STATEMENT IS
	
	    // SUBROUTINE QRFAC(M,N,A,LDA,PIVOT,IPVT,LIPVT,RDIAG,ACNORM,WA)
	
	    // WHERE
	
	    // M IS A POSITIVE INTEGER INPUT VARIABLE SET TO THE NUMBER
	    //   OF ROWS OF A.
	
	    // N IS A POSITIVE INTEGER INPUT VARIABLE SET TO THE NUMBER
	    //   OF COLUMNS OF A.
	
	    // A IS AN M BY N ARRAY. ON INPUT A CONTAINS THE MATRIX FOR
	    //   WHICH THE QR FACTORIZATION IS TO BE COMPUTED. ON OUTPUT
	    // THE STRICT UPPER TRAPEZOIDAL PART OF A CONTAINS THE STRICT
	    // UPPER TRAPEZOIDAL PART OF R, AND THE LOWER TRAPEZOIDAL
	    // PART OF A CONTAINS A FACTORED FORM OF Q (THE NON-TRIVIAL
	    // ELEMENTS OF THE U VECTORS DESCRIBED ABOVE).
	
	    // LDA IS A POSITIVE INTEGER INPUT VARIABLE NOT LESS THAN M
	    //     WHICH SPECIFIES THE LEADING DIMENSION OF THE ARRAY A.
	
	    // PIVOT IS A LOGICAL INPUT VARIABLE. IF PIVOT IS SET TRUE,
	    //   THEN COLUMN PIVOTING IS ENFORCED. IF PIVOT IS SET FALSE,
	    //   THEN NO COLUMN PIVOTING IS DONE.
	
	    // IPVT IS AN INTEGER OUTPUT ARRAY OF LENGTH LIPVT. IPVT
	    //   DEFINES THE PERMUTATION MATRIX P SUCH THAT A*P = Q*R.
	    //   COLUMN J OF P IS COLUMN IPVT(J) OF THE IDENTITY MATRIX.
	    //   IF PIVOT IS FALSE, IPVT IS NOT REFERENCED.
	
	    // LIPVT IS A POSITIVE INTEGER INPUT VARIABLE. IF PIVOT IS FALSE,
	    //   THEN LIPVT MAY BE AS SMALL AS 1. IF PIVOT IS TRUE, THEN
	    //   LIPVT MUST BE AT LEAST N.
	
	    // RDIAG IS AN OUTPUT ARRAY OF LENGTH N WHICH CONTAINS THE
	    //   DIAGONAL ELEMENTS OF R.
	
        // ACNORM IS AN OUTPUT ARRAY OF LENGTH N WHICH CONTAINS THE
	    //   NORMS OF THE CORRESPONDING COLUMNS OF THE INPUT MATRIX A.
	    //   IF THIS INFORMATION IS NOT NEEDED, THEN ACNORM CAN COINCIDE
	    //   WITH RDIAG.
	
	    // WA IS A WORK ARRAY OF LENGTH N. IF PIVOT IS FALSE, THEN WA
	    //   CAN COINCIDE WITH RDIAG.
	
	    // SUBPROGRAMS CALLED
	
	    // MINPACK-SUPPLIED ... DPMPAR,ENORM
	
	    // ARGONNE NATIONAL LABORATORY. MINPACK PROJECT. MARCH 1980.
	    // BURTON S. GARBOW, KENNETH E. HILLSTROM, JORGE J. MORE
	
	    //     **********
	    //      .. Scalar Arguments ..
	    // INTEGER LDA,LIPVT,M,N
	    // LOGICAL PIVOT
	
	    // .. Array Arguments ..
	    // DOUBLE PRECISION A(LDA,N),ACNORM(N),RDIAG(N),WA(N)
	    // INTEGER IPVT(LIPVT)
	
	    //      .. Local Scalars ..
		double ratio;
	    double AJNORM,EPSMCH,ONE,P05,SUM,TEMP,ZERO;
	    double vec[];
	    int I,J,JP1,K,KMAX,MINMN;
	
	    //     .. External Functions ..
	    //      DOUBLE PRECISION DPMPAR,ENORM
	    //      EXTERNAL DPMPAR,ENORM
	
	    //     .. Data statements ..
	    ONE = 1.0;
	    P05 = 5.0E-2;
	    ZERO = 0.0;
	
        // EPSMCH IS THE MACHINE PRECISION.
	
	    EPSMCH = MACHEP;
	
	    // COMPUTE THE INITIAL COLUMN NORMS AND INITIALIZE SEVERAL ARRAYS.
	
	    for (J = 1; J <= N; J++) {
	    	vec = new double[M];
	    	for (I = 0; I < M; I++) {
	    		vec[I] = A[I][J-1];
	    	}
	        ACNORM[J-1] = ENORM(M,vec);
	        RDIAG[J-1] = ACNORM[J-1];
	        WA[J-1] = RDIAG[J-1];
	        if (PIVOT) {
	        	IPVT[J-1] = J;
	        }
	    } // for (J = 1; J <= N; J++)
	
	    // REDUCE A TO R WITH HOUSEHOLDER TRANSFORMATIONS.
	
	    MINMN = Math.min(M,N);
	    for (J = 1; J <= MINMN; J++) {
	        if (PIVOT) {
	
	            // BRING THE COLUMN OF LARGEST NORM INTO THE PIVOT POSITION.
	
	            KMAX = J;
	            for (K = J; K <= N; K++) {
	                if (RDIAG[K-1] > RDIAG[KMAX-1]) {
	                	KMAX = K;
	                }
	            } // for (K = J; K <= N; K++)
	            if  (KMAX != J) {
	                for (I = 1; I <= M; I++) {
	                    TEMP = A[I-1][J-1];
	                    A[I-1][J-1] = A[I-1][KMAX-1];
	                    A[I-1][KMAX-1] = TEMP;
	                } // for (I = 1; I <= M; I++)
	                RDIAG[KMAX-1] = RDIAG[J-1];
	                WA[KMAX-1] = WA[J-1];
	                K = IPVT[J-1];
	                IPVT[J-1] = IPVT[KMAX-1];
	                IPVT[KMAX-1] = K;
	            } // if (KMAX != J)
	        } // if (PIVOT)
	
	        // COMPUTE THE HOUSEHOLDER TRANSFORMATION TO REDUCE THE
	        // J-TH COLUMN OF A TO A MULTIPLE OF THE J-TH UNIT VECTOR.
	
	        vec = new double[M-J+1];
	        for (I = 0; I < M-J+1; I++) {
	        	vec[I] = A[J-1+I][J-1];
	        }
	        AJNORM = ENORM(M-J+1,vec);
	        if (AJNORM != 0) {
	            if (A[J-1][J-1] < ZERO) {
	            	AJNORM = -AJNORM;
	            }
	            for (I = J; I <= M; I++) {
	              A[I-1][J-1] = A[I-1][J-1]/AJNORM;
	            } // for (I = J; I <= M; I++)
	            A[J-1][J-1] = A[J-1][J-1] + ONE;
	
	            // APPLY THE TRANSFORMATION TO THE REMAINING COLUMNS
	            // AND UPDATE THE NORMS.
	
	            JP1 = J + 1;
	            if (N >= JP1) {
	                for (K = JP1; K <= N; K++) {
	                    SUM = ZERO;
	                    for (I = J; I <= M; I++) {
	                        SUM = SUM + A[I-1][J-1]*A[I-1][K-1];
	                    } // for (I = J; I <= M; I++)
	                    TEMP = SUM/A[J-1][J-1];
	                    for (I = J; I <= M; I++) {
	                        A[I-1][K-1] = A[I-1][K-1] - TEMP*A[I-1][J-1];
	                    } // for (I = J; I <= M; I++)
	                    if (PIVOT && RDIAG[K-1] != ZERO) {
	                        TEMP = A[J-1][K-1]/RDIAG[K-1];
	                        RDIAG[K-1] = RDIAG[K-1]*Math.sqrt(Math.max(ZERO,ONE-TEMP*TEMP));
	                        ratio = RDIAG[K-1]/WA[K-1];
	                        if (P05*ratio*ratio <= EPSMCH) {
	                        	vec = new double[M-J];
	                        	for (I = 0; I < M-J; I++) {
	                        		vec[I] = A[JP1-1+I][K-1];
	                        	}
	                            RDIAG[K-1] = ENORM(M-J,vec);
	                            WA[K-1] = RDIAG[K-1];
	                        } // if (P05*ratio*ratio <= EPSMCH)
	                    } // if (PIVOT && RDIAG[K-1] != ZERO)
	                } // for (K = JP1; K <= N; K++)
	            } // if (N >= JP1)
	        } // if (AJNORM != 0)
	        RDIAG[J-1] = -AJNORM;
	    } // for (J = 1; J <= MINMN; J++)
	      return;
	}



}