package gov.nih.mipav.model.algorithms;


import gov.nih.mipav.view.MipavUtil;
import gov.nih.mipav.view.Preferences;

/**
/ M. Ullner and M. Lund, 2012-2016

Copyright notice
----------------

~~~~
Copyright (c) 1991-95 Paul J Turner, Portland, OR
Copyright (c) 1996-98 ACE/gr Development Team

Currently maintained by Evgeny Stambulchik, Rehovot, Israel

                             All Rights Reserved

Permission  to  use, copy, modify, and  distribute  this software  and  its
documentation  for any purpose and  without fee is hereby granted, provided
that  the above copyright notice  appear in  all copies and  that both that
copyright  notice  and   this  permission  notice   appear  in   supporting
documentation.

PAUL J TURNER AND OTHER CONTRIBUTORS DISCLAIM ALL WARRANTIES WITH REGARD TO
THIS SOFTWARE, INCLUDING,  BUT  NOT LIMITED  TO, ALL  IMPLIED WARRANTIES OF
MERCHANTABILITY  AND  FITNESS. IN NO EVENT SHALL PAUL J TURNER  OR  CURRENT
MAINTAINER  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR
ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER
IN AN ACTION OF CONTRACT, NEGLIGENCE OR  OTHER TORTUOUS ACTION, ARISING OUT
OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
*/

/**
 * Correct values for Cephes functions are taken from hcephes-master/test/src/hcephes.c provided under the MIT license
 * The MIT License (MIT)
=====================

Copyright (c) 2018 Danilo Horta

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 *  */

/**
 * Correct values for ndtri are taken from scipy-main/scipy/special/tests/test_ndtr.py under the BSD-3 license:
 * Copyright (c) 2001-2002 Enthought, Inc. 2003-2022, SciPy Developers.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above
   copyright notice, this list of conditions and the following
   disclaimer in the documentation and/or other materials provided
   with the distribution.

3. Neither the name of the copyright holder nor the names of its
   contributors may be used to endorse or promote products derived
   from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

public class Cephes {
	
	public final static int CHDTR = 1;
	public final static int CHDTRC = 2;
	public final static int CHDTRI = 3;
	public final static int ELLIE = 4;
	public final static int ELLPE = 5;
	public final static int ELLPK = 6;
	public final static int ERF = 7;
	public final static int ERFC = 8;
	public final static int IGAM = 9;
	public final static int IGAMI = 10;
	public final static int IGAMC = 11;
	public final static int NDTR = 12;
	public final static int NDTRI = 13;
	public final static int POLEVL = 14;
	public final static int P1EVL = 15;
	// For IEEE arithmetic (IBMPC):
    private final static double MACHEP =  1.11022302462515654042E-16; // 2**-53
    private final static double MAXLOG =  7.09782712893383996843E2;   // log(2**1024)
    private final static double MINLOG = -7.08396418532264106224E2;   // log(2**-1022)
    private final static double MAXNUM =  Double.MAX_VALUE; // 1.7976931348623158E308 2**1024
    private final static double big = 4.503599627370496e15;
	private final static double biginv =  2.22044604925031308085e-16;
	/* sqrt(2pi) */
	private final static double s2pi = 2.50662827463100050242E0;
	private final static double PIO2   =  1.57079632679489661923; // pi/2
	private final static double SQRTH  =  7.07106781186547524401E-1; // sqrt(2)/2
	// For ndtr:
	private final static double P[] = new double[]{
		 2.46196981473530512524E-10,
		 5.64189564831068821977E-1,
		 7.46321056442269912687E0,
		 4.86371970985681366614E1,
		 1.96520832956077098242E2,
		 5.26445194995477358631E2,
		 9.34528527171957607540E2,
		 1.02755188689515710272E3,
		 5.57535335369399327526E2
		};
	private final static double Q[] = new double[]{
			/* 1.00000000000000000000E0,*/
		 1.32281951154744992508E1,
		 8.67072140885989742329E1,
		 3.54937778887819891062E2,
		 9.75708501743205489753E2,
		 1.82390916687909736289E3,
		 2.24633760818710981792E3,
		 1.65666309194161350182E3,
		 5.57535340817727675546E2
		};
    private final static double R[] = new double[]{
		 5.64189583547755073984E-1,
		 1.27536670759978104416E0,
		 5.01905042251180477414E0,
		 6.16021097993053585195E0,
		 7.40974269950448939160E0,
		 2.97886665372100240670E0
		};
	private final static double S[] = new double[]{
		/* 1.00000000000000000000E0,*/
		 2.26052863220117276590E0,
		 9.39603524938001434673E0,
		 1.20489539808096656605E1,
		 1.70814450747565897222E1,
		 9.60896809063285878198E0,
		 3.36907645100081516050E0
		};
	private final static double T[] = new double[]{
		 9.60497373987051638749E0,
		 9.00260197203842689217E1,
		 2.23200534594684319226E3,
		 7.00332514112805075473E3,
		 5.55923013010394962768E4
		};
	private final static double U[] = new double[]{
		/* 1.00000000000000000000E0,*/
		 3.35617141647503099647E1,
		 5.21357949780152679795E2,
		 4.59432382970980127987E3,
		 2.26290000613890934246E4,
		 4.92673942608635921086E4
		};
	// P and Q approximations for ndtri
	/* approximation for 0 <= |y - 0.5| <= 3/8 */
	private final static double P0[] = new double[]{
	-5.99633501014107895267E1,
	 9.80010754185999661536E1,
	-5.66762857469070293439E1,
	 1.39312609387279679503E1,
	-1.23916583867381258016E0,
	};
	private final static double Q0[] = new double[]{
	/* 1.00000000000000000000E0,*/
	 1.95448858338141759834E0,
	 4.67627912898881538453E0,
	 8.63602421390890590575E1,
	-2.25462687854119370527E2,
	 2.00260212380060660359E2,
	-8.20372256168333339912E1,
	 1.59056225126211695515E1,
	-1.18331621121330003142E0,
	};
	
	/* Approximation for interval z = sqrt(-2 log y ) between 2 and 8
	 * i.e., y between exp(-2) = .135 and exp(-32) = 1.27e-14.
	 */
	private final static double P1[] = new double[]{
	 4.05544892305962419923E0,
	 3.15251094599893866154E1,
	 5.71628192246421288162E1,
	 4.40805073893200834700E1,
	 1.46849561928858024014E1,
	 2.18663306850790267539E0,
	-1.40256079171354495875E-1,
	-3.50424626827848203418E-2,
	-8.57456785154685413611E-4,
	};
	private final static double Q1[] = new double[]{
	/*  1.00000000000000000000E0,*/
	 1.57799883256466749731E1,
	 4.53907635128879210584E1,
	 4.13172038254672030440E1,
	 1.50425385692907503408E1,
	 2.50464946208309415979E0,
	-1.42182922854787788574E-1,
	-3.80806407691578277194E-2,
	-9.33259480895457427372E-4,
	};
	
	
	/* Approximation for interval z = sqrt(-2 log y ) between 8 and 64
	 * i.e., y between exp(-32) = 1.27e-14 and exp(-2048) = 3.67e-890.
	 */

	private final static double P2[] = new double[]{
	  3.23774891776946035970E0,
	  6.91522889068984211695E0,
	  3.93881025292474443415E0,
	  1.33303460815807542389E0,
	  2.01485389549179081538E-1,
	  1.23716634817820021358E-2,
	  3.01581553508235416007E-4,
	  2.65806974686737550832E-6,
	  6.23974539184983293730E-9,
	};
	private final static double Q2[] = new double[]{
	/*  1.00000000000000000000E0,*/
	  6.02427039364742014255E0,
	  3.67983563856160859403E0,
	  1.37702099489081330271E0,
	  2.16236993594496635890E-1,
	  1.34204006088543189037E-2,
	  3.28014464682127739104E-4,
	  2.89247864745380683936E-6,
	  6.79019408009981274425E-9,
	};
	
	// For ellpe:
	private final static double PELLPE[] = new double[]{
		  1.53552577301013293365E-4,
		  2.50888492163602060990E-3,
		  8.68786816565889628429E-3,
		  1.07350949056076193403E-2,
		  7.77395492516787092951E-3,
		  7.58395289413514708519E-3,
		  1.15688436810574127319E-2,
		  2.18317996015557253103E-2,
		  5.68051945617860553470E-2,
		  4.43147180560990850618E-1,
		  1.00000000000000000299E0
		};
	private final static double QELLPE[] = new double[]{
		  3.27954898576485872656E-5,
		  1.00962792679356715133E-3,
		  6.50609489976927491433E-3,
		  1.68862163993311317300E-2,
		  2.61769742454493659583E-2,
		  3.34833904888224918614E-2,
		  4.27180926518931511717E-2,
		  5.85936634471101055642E-2,
		  9.37499997197644278445E-2,
		  2.49999999999888314361E-1
		};
	
	// For ellpk:
	private final static double C1 = 1.3862943611198906188E0; /* log(4) */
	private final static double PELLPK[] = new double[]{
		 1.37982864606273237150E-4,
		 2.28025724005875567385E-3,
		 7.97404013220415179367E-3,
		 9.85821379021226008714E-3,
		 6.87489687449949877925E-3,
		 6.18901033637687613229E-3,
		 8.79078273952743772254E-3,
		 1.49380448916805252718E-2,
		 3.08851465246711995998E-2,
		 9.65735902811690126535E-2,
		 1.38629436111989062502E0
		};

	private final static double QELLPK[] = new double[]{
		 2.94078955048598507511E-5,
		 9.14184723865917226571E-4,
		 5.94058303753167793257E-3,
		 1.54850516649762399335E-2,
		 2.39089602715924892727E-2,
		 3.01204715227604046988E-2,
		 3.73774314173823228969E-2,
		 4.88280347570998239232E-2,
		 7.03124996963957469739E-2,
		 1.24999999999870820058E-1,
		 4.99999999999999999821E-1
		};
	private double result[];
	
	private int version;
	
	private double par1;
	
	private double par2;
	
	private double[] par3;
	
	private int par4;
	
	public void testCephes() {
		// The test for chdtr(4,5) passed
		// The test for chdtrc(4,5) passed
		// The test for chdtri(4,0.3) passed
		// The test for ellie(-5.3, 0.12) passed
		// The test for igam(1,2) passed
		// The test for igmac(2,1) passed
		// The test for igami(2,0.3) passed
		// The test for ndtr(0.0) passed
		// The test for ndtr(0.3) passed
		// The test for ndtr(1) passed
		// The test for ndtri(0.5) passed
		// The test for ndtri(0.6) passed
		result = new double[1];
		chdtr(4,5);
		if (Math.abs(result[0] - 0.7127025048163542) < 1.0E-7) {
	    	System.out.println("The test for chdtr(4,5) passed");
	    }
	    else {
	    	System.out.println("The test for chdtr(4,5) failed");
	    	System.out.println("Implemented chdtr gave " + result[0]);
	    	System.out.println("Correct answer is 0.7127025048163542");
	    }
		chdtrc(4,5);
		if (Math.abs(result[0] - 0.2872974951836458) < 1.0E-7) {
	    	System.out.println("The test for chdtrc(4,5) passed");
	    }
	    else {
	    	System.out.println("The test for chdtrc(4,5) failed");
	    	System.out.println("Implemented chdtrc gave " + result[0]);
	    	System.out.println("Correct answer is 0.2872974951836458");
	    }
	    chdtri(4,0.3);
	    if (Math.abs(result[0] - 4.8784329665604087) < 1.0E-7) {
	    	System.out.println("The test for chdtri(4,0.3) passed");
	    }
	    else {
	    	System.out.println("The test for chdtri(4,0.3) failed");
	    	System.out.println("Implemented chdtri gave " + result[0]);
	    	System.out.println("Correct answer is 4.8784329665604087");
	    }
	    
	    ellie(-5.3, 0.12);
	    if (Math.abs(result[0] + 5.12290521194) < 1.0E-7) {
	    	System.out.println("The test for ellie(-5.3, 0.12) passed");
	    }
	    else {
	    	System.out.println("The test for ellie(-5.3, 0.12) failed");
	    	System.out.println("Implemented ellie gave " + result[0]);
	    	System.out.println("Correct answer is -5.12290521194");
	    }
	    
	    // ellpe and ellpk answers here from hcephes versions which
	    // start with a line not present in cephes version
	    // line added by Danilo x = 1.0 - x;
	    /*ellpe(0.12);
	    if (Math.abs(result[0] - 1.522555369217904) < 1.0E-7) {
	    	System.out.println("The test for ellpe(0.12) passed");
	    }
	    else {
	    	System.out.println("The test for ellpe(0.12) failed");
	    	System.out.println("Implemented ellpe gave " + result[0]);
	    	System.out.println("Correct answer is 1.522555369217904");
	    }
	    
	    ellpk(0.12);
	    if (Math.abs(result[0] - 1.621393137980658) < 1.0E-7) {
	    	System.out.println("The test for ellpk(0.12) passed");
	    }
	    else {
	    	System.out.println("The test for ellpk(0.12) failed");
	    	System.out.println("Implemented ellpk gave " + result[0]);
	    	System.out.println("Correct answer is 1.621393137980658");
	    }
	    */
	    
	    igam(1,2);
	    if (Math.abs(result[0] - 0.8646647167633873) < 1.0E-7) {
	    	System.out.println("The test for igam(1,2) passed");
	    }
	    else {
	    	System.out.println("The test for igam(1,2) failed");
	    	System.out.println("Implemented igam gave " + result[0]);
	    	System.out.println("Correct answer is 0.8646647167633873");
	    }
	    
	    igamc(2,1);
	    if (Math.abs(result[0] - 0.7357588823428847) < 1.0E-7) {
	    	System.out.println("The test for igmac(2,1) passed");
	    }
	    else {
	    	System.out.println("The test for igmac(2,1) failed");
	    	System.out.println("Implemented igmac gave " + result[0]);
	    	System.out.println("Correct answer is 0.7357588823428847");
	    }
	    
	    igami(2,0.3);
	    if (Math.abs(result[0] - 2.439216483280204) < 1.0E-7) {
	    	System.out.println("The test for igami(2,0.3) passed");
	    }
	    else {
	    	System.out.println("The test for igami(2,0.3) failed");
	    	System.out.println("Implemented igami gave " + result[0]);
	    	System.out.println("Correct answer is 2.439216483280204");
	    }
	    
	    ndtr(0.0);
	    if (result[0] == 0.5) {
	    	System.out.println("The test for ndtr(0.0) passed");
	    }
	    else {
	    	System.out.println("The test for ndtr(0.0) failed");
	    	System.out.println("Implemented ndtr gave " + result[0]);
	    	System.out.println("Correct answer is 0.5");
	    }
	    
	    ndtr(0.3);
	    if (Math.abs(result[0] - 0.61791142218895256) < 1.0E-7) {
	    	System.out.println("The test for ndtr(0.3) passed");
	    }
	    else {
	    	System.out.println("The test for ndtr(0.3) failed");
	    	System.out.println("Implemented ndtr gave " + result[0]);
	    	System.out.println("Correct answer is 0.61791142218895256");
	    }
	    
	    ndtr(1);
	    if (Math.abs(result[0] - 0.8413447460685429) < 1.0E-7) {
	    	System.out.println("The test for ndtr(1) passed");
	    }
	    else {
	    	System.out.println("The test for ndtr(1) failed");
	    	System.out.println("Implemented ndtr gave " + result[0]);
	    	System.out.println("Correct answer is 0.8413447460685429");
	    }
	    
	    ndtri(0.5);
	    if (result[0] == 0.0) {
	    	System.out.println("The test for ndtri(0.5) passed");
	    }
	    else {
	    	System.out.println("The test for ndtri(0.5) failed");
	    	System.out.println("Implemented ndtri gave " + result[0]);
	    	System.out.println("Correct answer is 0.0");
	    }
	    
	    ndtri(0.6);
	    if (Math.abs(result[0] - 0.25334710313579972) < 1.0E-7) {
	    	System.out.println("The test for ndtri(0.6) passed");
	    }
	    else {
	    	System.out.println("The test for ndtri(0.6) failed");
	    	System.out.println("Implemented ndtri gave " + result[0]);
	    	System.out.println("Correct answer is 0.25334710313579972");
	    }
	    
	}
	
	public Cephes() {
		
	}
	
	public Cephes(double par1, int version, double result[]) {
		this.par1 = par1;
		this.version = version;
		this.result = result;
	}
	
	public Cephes(double par1, double par2, int version, double result[]) {
		this.par1 = par1;
		this.par2 = par2;
		this.version = version;
		this.result = result;
	}
	
	public Cephes(double par1, double par3[], int par4, int version, double result[]) {
		this.par1 = par1;
		this.par3 = par3;
		this.par4 = par4;
		this.version = version;
		this.result = result;
	}
	
	public void run() {
		if (version == CHDTR) {
			chdtr(par1, par2);
		}
	    else if (version == CHDTRC) {
			chdtrc(par1, par2);
		}
	    else if (version == CHDTRI) {
			chdtri(par1, par2);
		}
	    else if (version == ELLIE) {
	    	ellie(par1, par2);
	    }
	    else if (version == ELLPE) {
	    	ellpe(par1);
	    }
	    else if (version == ELLPK)
	    	ellpk(par1);
	    else if (version == ERF) {
	    	erf(par1);
	    }
	    else if (version == ERFC) {
	    	erfc(par1);
	    }
		else if (version == IGAMI) {
			igami(par1, par2);
		}
		else if (version == IGAMC) {
			igamc(par1, par2);
		}
		else if (version == IGAM) {
			igam(par1,par2);
		}
		else if (version == NDTR) {
			ndtr(par1);
		}
		else if (version == NDTRI) {
			ndtri(par1);
		}
		else if (version == POLEVL) {
			polevl(par1, par3, par4);
		}
		else if (version == P1EVL) {
			p1evl(par1, par3, par4);
		}
	}
	
	/*							chdtr.c
	 *
	 *	Chi-square distribution
	 *
	 *
	 *
	 * SYNOPSIS:
	 *
	 * double df, x, y, chdtr();
	 *
	 * y = chdtr( df, x );
	 *
	 *
	 *
	 * DESCRIPTION:
	 *
	 * Returns the area under the left hand tail (from 0 to x)
	 * of the Chi square probability density function with
	 * v degrees of freedom.
	 *
	 *
	 *                                  inf.
	 *                                    -
	 *                        1          | |  v/2-1  -t/2
	 *  P( x | v )   =   -----------     |   t      e     dt
	 *                    v/2  -       | |
	 *                   2    | (v/2)   -
	 *                                   x
	 *
	 * where x is the Chi-square variable.
 *
 * The incomplete gamma integral is used, according to the
 * formula
 *
 *	y = chdtr( v, x ) = igam( v/2.0, x/2.0 ).
 *
 *
 * The arguments must both be positive.
 *
 *
 *
 * ACCURACY:
 *
 * See igam().
 *
 * ERROR MESSAGES:
 *
 *   message         condition      value returned
 * chdtr domain   x < 0 or v < 1        0.0
 */
	
	private void chdtr(double df, double x) {

	if( (x < 0.0) || (df < 1.0) )
		{
		    MipavUtil.displayError("Domain error in chdtr()");
		    result[0] = 0.0;
		    return;
		}
        igam( df/2.0, x/2.0 );
        return;
	}
	
	/*							chdtrc()
	 *
	 *	Complemented Chi-square distribution
	 *
	 *
	 *
	 * SYNOPSIS:
	 *
	 * double v, x, y, chdtrc();
	 *
	 * y = chdtrc( v, x );
	 *
	 *
	 *
	 * DESCRIPTION:
	 *
	 * Returns the area under the right hand tail (from x to
	 * infinity) of the Chi square probability density function
	 * with v degrees of freedom:
	 *
	 *
	 *                                  inf.
	 *                                    -
	 *                    v/2  -       | |
	 *                   2    | (v/2)   -
	 *                                   x
	 *
	 * where x is the Chi-square variable.
	 *
	 * The incomplete gamma integral is used, according to the
	 * formula
	 *
	 *	y = chdtr( v, x ) = igamc( v/2.0, x/2.0 ).
	 *
	 *
	 * The arguments must both be positive.
	 *
	 *
	 *
	 * ACCURACY:
	 *
	 * See igamc().
	 *
	 * ERROR MESSAGES:
	 *
	 *   message         condition      value returned
	 * chdtrc domain  x < 0 or v < 1        0.0
	 */
	private void chdtrc(double df, double x)
	{

	if( (x < 0.0) || (df < 1.0) )
		{
		MipavUtil.displayError("Domain error in chdtrc()");
		result[0] = 0;
		}
	    igamc( df/2.0, x/2.0 );
	    return;
	}
	
	/*							chdtri()
	 *
	 *	Inverse of complemented Chi-square distribution
	 *
	 *
	 *
	 * SYNOPSIS:
	 *
	 * double df, x, y, chdtri();
	 *
	 * x = chdtri( df, y );
	 *
	 *
	 *
	 *
	 * DESCRIPTION:
	 *
	 * Finds the Chi-square argument x such that the integral
	 * from x to infinity of the Chi-square density is equal
	 * to the given cumulative probability y.
	 *
	 * This is accomplished using the inverse gamma integral
	 * function and the relation
	 *
	 *    x/2 = igami( df/2, y );
	 *
	 *
	 *
	 *
	 * ACCURACY:
	 * 
	 * See igami.c.
	 *
	 * ERROR MESSAGES:
	 *
	 *   message         condition      value returned
	 * chdtri domain   y < 0 or y > 1        0.0
	 *                     v < 1
	 *
	 */
	
	/*
	Cephes Math Library Release 2.0:  April, 1987
	Copyright 1984, 1987 by Stephen L. Moshier
	Direct inquiries to 30 Frost Street, Cambridge, MA 02140
	*/
	private void chdtri(double df, double y) {
	    double x;
	    if ((y < 0) || ( y > 1.0) || (df < 1.0)) {
	    	MipavUtil.displayError("Domain error in chdtri()");
	    	result[0] = 0.0;
	    	return;
	    }
	    
	    igami( 0.5 * df, y );
	    x = result[0];
	    result[0] = (2.0 * x );
	    
	}
	
	/*							erf.c
	 *
	 *	Error function
	 *
	 *
	 *
	 * SYNOPSIS:
	 *
	 * double x, y, erf();
	 *
	 * y = erf( x );
	 *
	 *
	 *
	 * DESCRIPTION:
	 *
	 * The integral is
	 *
	 *                           x 
	 *                            -
	 *                 2         | |          2
	 *   erf(x)  =  --------     |    exp( - t  ) dt.
	 *              sqrt(pi)   | |
	 *                          -
	 *                           0
	 *
	 * The magnitude of x is limited to 9.231948545 for DEC
	 * arithmetic; 1 or -1 is returned outside this range.
	 *
	 * For 0 <= |x| < 1, erf(x) = x * P4(x**2)/Q5(x**2); otherwise
	 * erf(x) = 1 - erfc(x).
	 *
	 *
	 *
	 * ACCURACY:
	 *
	 *                      Relative error:
	 * arithmetic   domain     # trials      peak         rms
	 *    DEC       0,1         14000       4.7e-17     1.5e-17
	 *    IEEE      0,1         30000       3.7e-16     1.0e-16
	 *
	 */
	private void erf(double x)
	{
	double pol, p1, z;

	if( Math.abs(x) > 1.0 ) {
		erfc(x);
		result[0] = 1.0 - result[0];
		return;
	}
	z = x * x;
	polevl(z, T, 4);
	pol = result[0];
	p1evl(z, U, 5);
	p1 = result[0];
	result[0] = x * pol / p1;
	return;
	}
	
	/*							erfc.c
	 *
	 *	Complementary error function
	 *
	 *
	 *
	 * SYNOPSIS:
	 *
	 * double x, y, erfc();
	 *
	 * y = erfc( x );
	 *
	 *
	 *
	 * DESCRIPTION:
	 *
	 *
	 *  1 - erf(x) =
	 *
	 *                           inf. 
	 *                             -
	 *                  2         | |          2
	 *   erfc(x)  =  --------     |    exp( - t  ) dt
	 *               sqrt(pi)   | |
	 *                           -
	 *                            x
	 *
	 *
	 * For small x, erfc(x) = 1 - erf(x); otherwise rational
	 * approximations are computed.
	 *
	 *
	 *
	 * ACCURACY:
	 *
	 *                      Relative error:
	 * arithmetic   domain     # trials      peak         rms
	 *    DEC       0, 9.2319   12000       5.1e-16     1.2e-16
	 *    IEEE      0,26.6417   30000       5.7e-14     1.5e-14
	 *
	 *
	 * ERROR MESSAGES:
	 *
	 *   message         condition              value returned
	 * erfc underflow    x > 9.231948545 (DEC)       0.0
	 *
	 *
	 */
	private void erfc(double a)
	{
	double p,q,x,y,z;


	if( a < 0.0 )
		x = -a;
	else
		x = a;

	if( x < 1.0 ) {
		erf(a);
		result[0] = 1.0 - result[0];
		return;
	}

	z = -a * a;

	if( z < -MAXLOG )
		{
	under:
			System.err.println("Underflow in erfc()");
			if( a < 0 ) {
				result[0] = 2.0;
				return;
			}
			else {
				result[0] = 0.0;
				return;
			}
		
		} // if( z < -MAXLOG )

	z = Math.exp(z);

	if( x < 8.0 )
		{
		polevl( x, P, 8 );
		p = result[0];
		p1evl( x, Q, 8 );
		q = result[0];
		}
	else
		{
		polevl( x, R, 5 );
		p = result[0];
		p1evl( x, S, 6 );
		q = result[0];
		}
	y = (z * p)/q;

	if( a < 0 )
		y = 2.0 - y;

	if ( y == 0.0 ) {
		System.err.println("Underflow in erfc()");
		if( a < 0 ) {
			result[0] = 2.0;
			return;
		}
		else {
			result[0] = 0.0;
			return;
		}	
	} // if ( y == 0.0 )

	result[0] = y;
	return;
	}
	
	/*							ndtr.c
	 *
	 *	Normal distribution function
	 *
	 *
	 *
	 * SYNOPSIS:
	 *
	 * double x, y, ndtr();
	 *
	 * y = ndtr( x );
	 *
	 *
	 *
	 * DESCRIPTION:
	 *
	 * Returns the area under the Gaussian probability density
	 * function, integrated from minus infinity to x:
	 *
	 *                            x
	 *                             -
	 *                   1        | |          2
	 *    ndtr(x)  = ---------    |    exp( - t /2 ) dt
	 *               sqrt(2pi)  | |
	 *                           -
	 *                          -inf.
	 *
	 *             =  ( 1 + erf(z) ) / 2
	 *             =  erfc(z) / 2
	 *
	 * where z = x/sqrt(2). Computation is via the functions
	 * erf and erfc.
	 *
	 *
	 * ACCURACY:
	 *
	 *                      Relative error:
	 * arithmetic   domain     # trials      peak         rms
	 *    DEC      -13,0         8000       2.1e-15     4.8e-16
	 *    IEEE     -13,0        30000       3.4e-14     6.7e-15
	 *
	 *
	 * ERROR MESSAGES:
	 *
	 *   message         condition         value returned
	 * erfc underflow    x > 37.519379347       0.0
	 *
	 */
	private void ndtr(double a)
	{
	double x, y, z;

	x = a * SQRTH;
	z = Math.abs(x);

	if( z < SQRTH ) {
		erf(x);
		y = 0.5 + 0.5 * result[0];
	}

	else
		{
		erfc(z);
		y = 0.5 * result[0];

		if( x > 0 )
			y = 1.0 - y;
		}

	result[0] = y;
	return;
	}
	
	/*							igami()
	 *
	 *      Inverse of complemented imcomplete gamma integral
	 *
	 *
	 *
	 * SYNOPSIS:
	 *
	 * double a, x, p, igami();
	 *
	 * x = igami( a, p );
	 *
	 * DESCRIPTION:
	 *
	 * Given p, the function finds x such that
	 *
	 *  igamc( a, x ) = p.
	 *
	 * Starting with the approximate value
	 *
	 *         3
	 *  x = a t
	 *
	 *  where
	 *
	 *  t = 1 - d - ndtri(p) sqrt(d)
	 * 
	 * and
	 *
	 *  d = 1/9a,
	 *
	 * the routine performs up to 10 Newton iterations to find the
	 * root of igamc(a,x) - p = 0.
	 *
	 * ACCURACY:
	 *
	 ** Tested at random a, p in the intervals indicated.
	 *
	 *                a        p                      Relative error:
	 * arithmetic   domain   domain     # trials      peak         rms
	 *    IEEE     0.5,100   0,0.5       100000       1.0e-14     1.7e-15
	 *    IEEE     0.01,0.5  0,0.5       100000       9.0e-14     3.4e-15
	 *    IEEE    0.5,10000  0,0.5        20000       2.3e-13     3.8e-14
	 */
	
	/*
	Cephes Math Library Release 2.3:  March, 1995
	Copyright 1984, 1987, 1995 by Stephen L. Moshier
	*/
	private void igami(double a, double y0) {
		double x0, x1, x, yl, yh, y, d, lgm, dithresh;
		int i, dir;

		/* bound the solution */
		x0 = MAXNUM;
		yl = 0;
		x1 = 0;
		yh = 1.0;
		dithresh = 5.0 * MACHEP;
		
		/* approximation to inverse function */
		d = 1.0/(9.0*a);
		ndtri(y0);
		y = ( 1.0 - d - result[0] * Math.sqrt(d) );
		x = a * y * y * y;
		
		double ansG[] = new double[1];
		Gamma gam = new Gamma(a, 0, ansG);
		gam.run();
		lgm = ansG[0];

		for( i=0; i<10; i++ )
		{
		if( x > x0 || x < x1 )
			break;
		igamc(a,x);
		y = result[0];
		if( y < yl || y > yh )
			break;
		if( y < y0 )
			{
			x0 = x;
			yl = y;
			}
		else
			{
			x1 = x;
			yh = y;
			}
	/* compute the derivative of the function at this point */
		d = (a - 1.0) * Math.log(x) - x - lgm;
		if( d < -MAXLOG )
			break;
		d = -Math.exp(d);
	/* compute the step to the next approximation of x */
		d = (y - y0)/d;
		if( Math.abs(d/x) < MACHEP ) {
			result[0] = x;
			return;
		}
		x = x - d;
		} // for( i=0; i<10; i++ )
		
		/* Resort to interval halving if Newton iteration did not converge. */

		d = 0.0625;
		if( x0 == MAXNUM )
			{
			if( x <= 0.0 )
				x = 1.0;
			while( x0 == MAXNUM )
				{
				x = (1.0 + d) * x;
				igamc( a, x );
				y = result[0];
				if( y < y0 )
					{
					x0 = x;
					yl = y;
					break;
					}
				d = d + d;
				}
			}
		d = 0.5;
		dir = 0;

		for( i=0; i<400; i++ )
			{
			x = x1  +  d * (x0 - x1);
			igamc( a, x );
			y = result[0];
			lgm = (x0 - x1)/(x1 + x0);
			if( Math.abs(lgm) < dithresh )
				break;
			lgm = (y - y0)/y0;
			if( Math.abs(lgm) < dithresh )
				break;
			if( x <= 0.0 )
				break;
			if( y >= y0 )
				{
				x1 = x;
				yh = y;
				if( dir < 0 )
					{
					dir = 0;
					d = 0.5;
					}
				else if( dir > 1 )
					d = 0.5 * d + 0.5; 
				else
					d = (y0 - yl)/(yh - yl);
				dir += 1;
				}
			else
				{
				x0 = x;
				yl = y;
				if( dir > 0 )
					{
					dir = 0;
					d = 0.5;
					}
				else if( dir < -1 )
					d = 0.5 * d;
				else
					d = (y0 - yl)/(yh - yl);
				dir -= 1;
				}
			}
		if( x == 0.0 ) {
			MipavUtil.displayError( "igami UNDERFLOW ERROR");
			result[0] = 0.0;
			return;
		}
        result[0] = x;
		return;
	}
	
	/*							igamc()
	 *
	 *	Complemented incomplete gamma integral
	 *
	 *
	 *
	 * SYNOPSIS:
	 *
	 * double a, x, y, igamc();
	 *
	 * y = igamc( a, x );
	 *
	 * DESCRIPTION:
	 *
	 * The function is defined by
	 *
	 *
	 *  igamc(a,x)   =   1 - igam(a,x)
	 *
	 *                            inf.
	 *                              -
	 *                     1       | |  -t  a-1
	 *               =   -----     |   e   t   dt.
	 *                    -      | |
	 *                   | (a)    -
	 *                             x
	 *
	 *
	 * In this implementation both arguments must be positive.
	 * The integral is evaluated by either a power series or
	 * continued fraction expansion, depending on the relative
	 * values of a and x.
	 *
	 * ACCURACY:
	 *
	 * Tested at random a, x.
	 *                a         x                      Relative error:
	 * arithmetic   domain   domain     # trials      peak         rms
	 *    IEEE     0.5,100   0,100      200000       1.9e-14     1.7e-15
	 *    IEEE     0.01,0.5  0,100      200000       1.4e-13     1.6e-15
	 */
	
	/*
	Cephes Math Library Release 2.0:  April, 1987
	Copyright 1985, 1987 by Stephen L. Moshier
	Direct inquiries to 30 Frost Street, Cambridge, MA 02140
	*/

	private void igamc(double  a, double x )
	{
	double ans, ax, c, yc, r, t, y, z;
	double pk, pkm1, pkm2, qk, qkm1, qkm2;

	if( (x <= 0) || ( a <= 0) ) {
		result[0] = 1.0;
		return;
	}

	if( (x < 1.0) || (x < a) ) {
		igam(a,x);
		result[0] = 1.0 - result[0];
		return;
	}

	double ansG[] = new double[1];
	Gamma gam = new Gamma(a, 0, ansG);
	gam.run();
	double lgm = ansG[0];
	ax = a * Math.log(x) - x - lgm;
	if( ax < -MAXLOG )
		{
		MipavUtil.displayError("igamc UNDERFLOW");
		result[0] = 0.0;
		return;
		}
	ax = Math.exp(ax);

	/* continued fraction */
	y = 1.0 - a;
	z = x + y + 1.0;
	c = 0.0;
	pkm2 = 1.0;
	qkm2 = x;
	pkm1 = x + 1.0;
	qkm1 = z * x;
	ans = pkm1/qkm1;

	do
		{
		c += 1.0;
		y += 1.0;
		z += 2.0;
		yc = y * c;
		pk = pkm1 * z  -  pkm2 * yc;
		qk = qkm1 * z  -  qkm2 * yc;
		if( qk != 0 )
			{
			r = pk/qk;
			t = Math.abs( (ans - r)/r );
			ans = r;
			}
		else
			t = 1.0;
		pkm2 = pkm1;
		pkm1 = pk;
		qkm2 = qkm1;
		qkm1 = qk;
		if( Math.abs(pk) > big )
			{
			pkm2 *= biginv;
			pkm1 *= biginv;
			qkm2 *= biginv;
			qkm1 *= biginv;
			}
		}
	while( t > MACHEP );
     
	result[0] = ans * ax;
	return;
	}

	/*							igam.c
	 *
	 *	Incomplete gamma integral
	 *
	 *
	 *
	 * SYNOPSIS:
	 *
	 * double a, x, y, igam();
	 *
	 * y = igam( a, x );
	 *
	 * DESCRIPTION:
	 *
	 * The function is defined by
	 *
	 *                           x
	 *                            -
	 *                   1       | |  -t  a-1
	 *  igam(a,x)  =   -----     |   e   t   dt.
	 *                  -      | |
	 *                 | (a)    -
	 *                           0
	 *
	 *
	 * In this implementation both arguments must be positive.
	 * The integral is evaluated by either a power series or
	 * continued fraction expansion, depending on the relative
	 * values of a and x.
	 *
	 * ACCURACY:
	 *
	 *                      Relative error:
	 * arithmetic   domain     # trials      peak         rms
	 *    IEEE      0,30       200000       3.6e-14     2.9e-15
	 *    IEEE      0,100      300000       9.9e-14     1.5e-14
	 */
	
	/* left tail of incomplete gamma function:
	 *
	 *          inf.      k
	 *   a  -x   -       x
	 *  x  e     >   ----------
	 *           -     -
	 *          k=0   | (a+k+1)
	 *
	 */

	private void igam(double a, double x)
	{
	double ans, ax, c, r;

	if( (x <= 0) || ( a <= 0) ) {
		result[0] = 0.0;
		return;
	}

	if( (x > 1.0) && (x > a ) ) {
		igamc(a,x);
		result[0] = 1.0 - result[0];
		return;
	}

	/* Compute  x**a * exp(-x) / gamma(a)  */
	double ansG[] = new double[1];
	Gamma gam = new Gamma(a, 0, ansG);
	gam.run();
	double lgm = ansG[0];
	ax = a * Math.log(x) - x - lgm;
	if( ax < -MAXLOG )
		{
		MipavUtil.displayError( "igam UNDERFLOW");
		result[0] = 0.0;
		return;
		}
	ax = Math.exp(ax);

	/* power series */
	r = a;
	c = 1.0;
	ans = 1.0;

	do
		{
		r += 1.0;
		c *= x/r;
		ans += c;
		}
	while( c/ans > MACHEP );

	result[0] = ans * ax/a;
	return;
	}

	/*							ndtri.c
	 *
	 *	Inverse of Normal distribution function
	 *
	 *
	 *
	 * SYNOPSIS:
	 *
	 * double x, y, ndtri();
	 *
	 * x = ndtri( y );
	 *
	 *
	 *
	 * DESCRIPTION:
	 *
	 * Returns the argument, x, for which the area under the
	 * Gaussian probability density function (integrated from
	 * minus infinity to x) is equal to y.
	 *
	 *
	 * For small arguments 0 < y < exp(-2), the program computes
	 * z = sqrt( -2.0 * log(y) );  then the approximation is
	 * x = z - log(z)/z  - (1/z) P(1/z) / Q(1/z).
	 * There are two rational functions P/Q, one for 0 < y < exp(-32)
	 * and the other for y up to exp(-2).  For larger arguments,
	 * w = y - 0.5, and  x/sqrt(2pi) = w + w**3 R(w**2)/S(w**2)).
	 *
	 *
	 * ACCURACY:
	 *
	 *                      Relative error:
	 * arithmetic   domain        # trials      peak         rms
	 *    DEC      0.125, 1         5500       9.5e-17     2.1e-17
	 *    
 *    DEC      6e-39, 0.135     3500       5.7e-17     1.3e-17
 *    IEEE     0.125, 1        20000       7.2e-16     1.3e-16
 *    IEEE     3e-308, 0.135   50000       4.6e-16     9.8e-17
 *
 *
 * ERROR MESSAGES:
 *
 *   message         condition    value returned
 * ndtri domain       x <= 0        -MAXNUM
 * ndtri domain       x >= 1         MAXNUM
 *
 */
	private void ndtri(double y0) {
		double x, y, z, y2, x0, x1;
		int code;
		double pol, p1;

		if( y0 <= 0.0 )
			{
			MipavUtil.displayError( "ndtri DOMAIN error");
			result[0] = -MAXNUM;
			return;
			}
		if( y0 >= 1.0 )
			{
			MipavUtil.displayError( "ndtri DOMAIN error");
			result[0] = MAXNUM;
			return;
			}
		code = 1;
		y = y0;
		if( y > (1.0 - 0.13533528323661269189) ) /* 0.135... = exp(-2) */
			{
			y = 1.0 - y;
			code = 0;
			}

		if( y > 0.13533528323661269189 )
			{
			y = y - 0.5;
			y2 = y * y;
			polevl(y2, P0, 4);
			pol = result[0];
			p1evl(y2, Q0, 8);
			p1 = result[0];
			x = y + y * (y2 * pol/p1);
			x = x * s2pi; 
			result[0] = x;
			return;
			}

		x = Math.sqrt( -2.0 * Math.log(y) );
		x0 = x - Math.log(x)/x;

		z = 1.0/x;
		if( x < 8.0 ) /* y > exp(-32) = 1.2664165549e-14 */ {
			polevl(z, P1, 8);
			pol = result[0];
			p1evl(z, Q1, 8);
			p1 = result[0];
			x1 = z * pol/p1;
		}
		else {
			polevl(z, P2, 8);
			pol = result[0];
			p1evl(z, Q2, 8);
			p1 = result[0];
			x1 = z * pol/p1;
		}
		x = x0 - x1;
		if( code != 0 )
			x = -x;
		result[0] = x;
		return;
	}
	
	/*							polevl.c
	 *							p1evl.c
	 *
	 *	Evaluate polynomial
	 *
	 *
	 *
	 * SYNOPSIS:
	 *
	 * int N;
	 * double x, y, coef[N+1], polevl[];
	 *
	 * y = polevl( x, coef, N );
	 *
	 *
	 *
	 * DESCRIPTION:
	 *
	 * Evaluates polynomial of degree N:
	 *
	 *                     2          N
	 * y  =  C  + C x + C x  +...+ C x
	 *        0    1     2          N
	 *
	 * Coefficients are stored in reverse order:
	 *
	 * coef[0] = C  , ..., coef[N] = C  .
	 *            N                   0
	 *
	 *  The function p1evl() assumes that coef[N] = 1.0 and is
	 * omitted from the array.  Its calling arguments are
	 * otherwise the same as polevl().
	 *
	 *
	 * SPEED:
	 * *
	 * In the interest of speed, there are no checks for out
	 * of bounds arithmetic.  This routine is used by most of
	 * the functions in the library.  Depending on available
	 * equipment features, the user may wish to rewrite the
	 * program in microcode or assembly language.
	 *
	 */
	
	/*
	Cephes Math Library Release 2.1:  December, 1988
	Copyright 1984, 1987, 1988 by Stephen L. Moshier
	Direct inquiries to 30 Frost Street, Cambridge, MA 02140
	*/
	
	private void polevl(double x, double coef[], int N ) {
		double ans;
		int i;
		int coefindex = 0;

		ans = coef[coefindex++];
		i = N;

		do {
			ans = ans * x  +  coef[coefindex++];
			--i;
		} while (i > 0);
		

		result[0] = ans;
		return;
	}
	
	private void p1evl(double x, double coef[], int N ) {
		double ans;
		int coefindex = 0;
		int i;

		ans = x + coef[coefindex++];
		i = N-1;

		do {
			ans = ans * x  + coef[coefindex++];
			--i;
		} while(i > 0);

		result[0] = ans;
		return;
	}
	
	/*							ellie.c
	 *
	 *	Incomplete elliptic integral of the second kind
	 *
	 *
	 *
	 * SYNOPSIS:
	 *
	 * double phi, m, y, ellie();
	 *
	 * y = ellie( phi, m );
	 *
	 *
	 *
	 * DESCRIPTION:
	 *
	 * Approximates the integral
	 *
	 *
	 *                phi
	 *                 -
	 *                | |
	 *                |                   2
	 * E(phi_\m)  =    |    sqrt( 1 - m sin t ) dt
	 *                |
	 *              | |    
	 *               -
	 *                0
	 *
	 * of amplitude phi and modulus m, using the arithmetic -
	 * geometric mean algorithm.
	 *
	 *
	 *
	 * ACCURACY:
	 *
	 * Tested at random arguments with phi in [-10, 10] and m in
	 * [0, 1].
	 *                      Relative error:
	 * arithmetic   domain     # trials      peak         rms
	 *    DEC        0,2         2000       1.9e-16     3.4e-17
	 *    IEEE     -10,10      150000       3.3e-15     1.4e-16
	 *
	 *
	 */
	

	/*
	Cephes Math Library Release 2.0:  April, 1987
	Copyright 1984, 1987, 1993 by Stephen L. Moshier
	Direct inquiries to 30 Frost Street, Cambridge, MA 02140
	*/

	/*	Incomplete elliptic integral of second kind	*/

	private void ellie(double phi, double m )
	{
	double a, b, c, e, temp;
	double lphi, t, E;
	int d, mod, npio2, sign;

	if( m == 0.0 ) {
		result[0] = phi;
		return;
	}
	lphi = phi;
	npio2 =(int)Math.floor( lphi/PIO2 );
	if(( npio2 & 1 ) != 0)
		npio2 += 1;
	lphi = lphi - npio2 * PIO2;
	if( lphi < 0.0 )
		{
		lphi = -lphi;
		sign = -1;
		}
	else
		{
		sign = 1;
		}
	a = 1.0 - m;
	ellpe(a);
	E = result[0];
	if( a == 0.0 )
		{
		temp = Math.sin( lphi );
		if( sign < 0 )
			temp = -temp;
		temp += npio2 * E;
		result[0] = temp;
		return;
		}
	t = Math.tan( lphi );
	b = Math.sqrt(a);
	/* Thanks to Brian Fitzgerald <fitzgb@mml0.meche.rpi.edu>
	   for pointing out an instability near odd multiples of pi/2.  */
	if( Math.abs(t) > 10.0 )
		{
		/* Transform the amplitude */
		e = 1.0/(b*t);
		/* ... but avoid multiple recursions.  */
		if( Math.abs(e) < 10.0 )
			{
			e = Math.atan(e);
			ellie(e,m);
			temp = E + m * Math.sin( lphi ) * Math.sin( e ) - result[0];
			if( sign < 0 )
				temp = -temp;
			temp += npio2 * E;
			result[0] = temp;
			return;
			}
		}
	c = Math.sqrt(m);
	a = 1.0;
	d = 1;
	e = 0.0;
	mod = 0;

	while( Math.abs(c/a) > MACHEP )
		{
		temp = b/a;
		lphi = lphi + Math.atan(t*temp) + mod * Math.PI;
		mod = (int)((lphi + PIO2)/Math.PI);
		t = t * ( 1.0 + temp )/( 1.0 - temp * t * t );
		c = ( a - b )/2.0;
		temp = Math.sqrt( a * b );
		a = ( a + b )/2.0;
		b = temp;
		d += d;
		e += c * Math.sin(lphi);
		}

	ellpk(1.0 - m);
	temp = E / result[0];
	temp *= (Math.atan(t) + mod * Math.PI)/(d * a);
	temp += e;

	done:

	if( sign < 0 )
		temp = -temp;
	temp += npio2 * E;
	result[0] = temp;
	return;
	}
	
	/*							ellpe.c
	 *
	 *	Complete elliptic integral of the second kind
	 *
	 *
	 *
	 * SYNOPSIS:
	 *
	 * double m1, y, ellpe();
	 *
	 * y = ellpe( m1 );
	 *
	 *
	 *
	 * DESCRIPTION:
	 *
	 * Approximates the integral
	 *
	 *
	 *            pi/2
	 *             -
	 *            | |                 2
	 * E(m)  =    |    sqrt( 1 - m sin t ) dt
	 *          | |    
	 *           -
	 *            0
	 *
	 * Where m = 1 - m1, using the approximation
	 *
	 *      P(x)  -  x log x Q(x).
	 *
	 * Though there are no singularities, the argument m1 is used
	 * rather than m for compatibility with ellpk().
	 *
	 * E(1) = 1; E(0) = pi/2.
	 *
	 *
	 * ACCURACY:
	 *
	 *                      Relative error:
	 * arithmetic   domain     # trials      peak         rms
	 *    DEC        0, 1       13000       3.1e-17     9.4e-18
	 *    IEEE       0, 1       10000       2.1e-16     7.3e-17
	 *
	 *
	 * ERROR MESSAGES:
	 *
	 *   message         condition      value returned
	 * ellpe domain      x<0, x>1            0.0
	 *
	 */
	
	/*							ellpe.c		*/

	/* Elliptic integral of second kind */

	/*
	Cephes Math Library, Release 2.1:  February, 1989
	Copyright 1984, 1987, 1989 by Stephen L. Moshier
	Direct inquiries to 30 Frost Street, Cambridge, MA 02140
	*/
	
	private void ellpe(double x)
	{
    double pol1, pol2;
    // hcephes_ellpe adds line by danilo x = 1.0 - x
	if( (x <= 0.0) || (x > 1.0) )
		{
		if( x == 0.0 ) {
			result[0] = 1.0;
			return;
		}
		MipavUtil.displayError("Domain error in ellpe()");
		result[0] = 0.0;
		return;
		}
	polevl(x,PELLPE,10);
	pol1 = result[0];
	polevl(x,QELLPE,9);
	pol2 = result[0];
	result[0] = pol1 - Math.log(x) * (x * pol2);
	return;
	}
	
	/*							ellpk.c
	 *
	 *	Complete elliptic integral of the first kind
	 *
	 *
	 *
	 * SYNOPSIS:
	 *
	 * double m1, y, ellpk();
	 *
	 * y = ellpk( m1 );
	 *
	 *
	 *
	 * DESCRIPTION:
	 *
	 * Approximates the integral
	 *
	 *
	 *
	 *            pi/2
	 *             -
	 *            | |
	 *            |           dt
	 * K(m)  =    |    ------------------
	 *            |                   2
	 *          | |    sqrt( 1 - m sin t )
	 *           -
	 *            0
	 *
	 * where m = 1 - m1, using the approximation
	 *
	 *     P(x)  -  log x Q(x).
	 *
	 * The argument m1 is used rather than m so that the logarithmic
	 * singularity at m = 1 will be shifted to the origin; this
	 * preserves maximum accuracy.
	 *
	 * K(0) = pi/2.
	 *
	 * ACCURACY:
	 *
	 *                      Relative error:
	 * arithmetic   domain     # trials      peak         rms
	 *    DEC        0,1        16000       3.5e-17     1.1e-17
	 *    IEEE       0,1        30000       2.5e-16     6.8e-17
	 *
	 * ERROR MESSAGES:
	 *
	 *   message         condition      value returned
	 * ellpk domain       x<0, x>1           0.0
	 *
	 */
	
	/*							ellpk.c */


	/*
	Cephes Math Library, Release 2.0:  April, 1987
	Copyright 1984, 1987 by Stephen L. Moshier
	Direct inquiries to 30 Frost Street, Cambridge, MA 02140
	*/

	private void ellpk(double x)
	{
		double pol1, pol2;
		 // hcephes_ellpk adds line by danilo x = 1.0 - x

	if( (x < 0.0) || (x > 1.0) )
		{
		MipavUtil.displayError("Domain error in ellpk()");
		result[0] = 0.0;
		return;
		}

	if( x > MACHEP )
		{
		polevl(x,PELLPK,10);
		pol1 = result[0];
		polevl(x,QELLPK,10);
		pol2 = result[0];
		result[0] = pol1 - Math.log(x) * pol2;
		}
	else
		{
		if( x == 0.0 )
			{
			MipavUtil.displayError("Singularity in ellpk()");
			result[0] = MAXNUM;
			return;
			}
		else
			{
			result[0] = C1 - 0.5 * Math.log(x);
			return;
			}
		}
	}
}