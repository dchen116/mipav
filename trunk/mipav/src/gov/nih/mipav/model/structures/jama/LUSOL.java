package gov.nih.mipav.model.structures.jama;


import java.text.DecimalFormat;

import gov.nih.mipav.view.Preferences;
import gov.nih.mipav.view.ViewUserInterface;

/** LUSOL maintains LU factors of a square or rectangular sparse matrix.
LUSOL is maintained by
*     Michael Saunders, Systems Optimization Laboratory,
*     Dept of Management Science & Engineering, Stanford University.
*     (saunders@stanford.edu)

Professor Michael Saunders has kindly granted the NIH MIPAV project permission to port LUSOL from FORTRAN to Java
under a BSD license.
Porting performed by William Gandler.

The website for LUSOL is: [http://www.stanford.edu/group/SOL/software/lusol.html][LUSOL]
LUSOL: Sparse LU for Ax = b

    AUTHOR: Michael Saunders
    CONTRIBUTORS: Philip Gill, Walter Murray, Margaret Wright, Michael O'Sullivan, Kjell Eikland, Yin Zhang, Nick Henderson, Ding Ma
    CONTENTS: A sparse LU factorization for square and rectangular matrices A, with Bartels-Golub-Reid updates for column replacement
     and other rank-1 modifications. Typically used for a sequence of linear equations as in the simplex method:

            Solve Ax = b and/or A'y = c
            Replace a column of A
            Repeat with different b, c
        

    The matrix A may have any shape and rank. Rectangular LU factors may be used to form a sparse null-space matrix operator.

    Special feature 1: Three sparse pivoting options in the Factor routine:

            Threshold partial pivoting (TPP)
            Threshold rook pivoting    (TRP)
            Threshold complete pivoting (TCP)
        

    All options choose row and column permutations as they go, balancing sparsity and stability according to different rules.
    TPP is normally most efficient for solving Ax = b. TRP and TCP are rank-revealing factorizations. In practice, TRP is
    an effective method for estimating rank(A). TCP tends to be too dense and expensive to be useful, although MINOS and SNOPT
    switch from TPP to TRP and even TCP if necessary in case of persistent numerical difficulty.

    Special feature 2: Multiple update routines:

            Add, delete, or replace a column of A
            Add, delete, or replace a row    of A
            Add a general (sparse) rank-1 matrix to A
        

    Numerical stability: LUSOL maintains LU factors with row and column permutations P, Q such that A = LU with PLP' lower
    triangular (with unit diagonals) and PUQ upper triangular. The condition of L is controlled throughout by maintaining
    |Lij| <= factol (= 10 or 5 or 2 or 1.1, ...), so that U tends to reflect the condition of A. This is essential for
    subsequent Bartels-Golub-type updates (which are implemented in a manner similar to John Reid's LA05 and LA15 packages
    in the HSL library).

    If a fresh factorization is thought of as A = LDU (with unit diagonals on PLP' and PUQ), then TRP and TCP control the
    condition of both L and U by maintaining |Lij| <= factol and |Uij| <= factol, so that D reflects the condition of A.
    This is why TRP and TCP have rank-revealing properties.

    Proven applications: LUSOL is the basis factorization package (BFP) for MINOS, SQOPT, SNOPT, lp_solve, AMPL/PATH, GAMS/PATH.

    Shortcomings:
    Factor: No special handling of dense columns.
    Solve: No special treatment of sparse right-hand sides.
    Documentation: No user's manual. Primary documentation is in-line comments within the f77 source code (and the more recent f90 version).
    REFERENCES:

    J. K. Reid (1982). A sparsity-exploiting variant of the Bartels-Golub decomposition for linear programming bases,
    Mathematical Programming 24, 55-69.

    P. E. Gill, W. Murray, M. A. Saunders and M. H. Wright (1987). Maintaining LU factors of a general sparse matrix,
    Linear Algebra and its Applications 88/89, 239-270.

    P. E. Gill, W. Murray and M. A. Saunders (2005). SNOPT: An SQP algorithm for large-scale constrained optimization,
    SIGEST article, SIAM Review 47(1), 99-131. (See sections 4 and 5.) 
    
## Basic usage

In Matlab:

```
% get L and U factors
[L U P Q] = lusol(A);
```

See `>>> help lusol`.

## Advanced usage

In Matlab:

```
% create lusol object
mylu = lusol_obj(A);

% solve with lusol object (ie x = A\b)
x = mylu.solveA(b);

% update factorization to replace a column
mylu.repcol(v,1);

% solve again with updated factorization
x1 = mylu.solveA(b);
```

See `>>> help lusol_obj`.


All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
 conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer
 in the documentation and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived
 from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

public class LUSOL implements java.io.Serializable {
    private ViewUserInterface UI = ViewUserInterface.getReference();
	
	DecimalFormat nf = new DecimalFormat("0.00000E0");
	private int ip = 8;
	private int rp = 8;
	private long ip_huge = Long.MAX_VALUE;
	private double huge = Double.MAX_VALUE;
	private double eps = 2.22044604925031308E-16;
	
	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// File:       lusol.f90
	
	// Contains    lu1fac   lu1fad   lu1gau   lu1mar   lu1mRP   lu1mCP   lu1mSP
	// Contains    lu1pen   lu1mxc   lu1mxr   lu1or1   lu1or2   lu1or3   lu1or4
	// Contains    lu1pq1   lu1pq2   lu1pq3   lu1rec   lu1slk
	//             lu1ful   lu1DPP   lu1DCP
	// Contains    Hbuild   Hchange  Hdelete  Hdown    Hinsert  Hup
	// Contains    lu6sol   lu6L     lu6Lt    lu6U     Lu6Ut    lu6LD    lu6chk
	// Contains    lu7add   lu7cyc   lu7elm   lu7for   lu7rnk   lu7zap
	// Contains    lu8rpc
	
	// Contains    jdamax
	
	// This file is an f90 version of most parts of the f77 sparse LU package LUSOL
	// (the parts needed by MINOS, SQOPT and SNOPT).  The parts included are
	
	//    lusol1.f    Factor a given matrix A from scratch (lu1fac).
	//    lusol2.f    Heap-management routines for lu1fac.
	//    lusol6a.f   Solve with the current LU factors.
	//    lusol7a.f   Utilities for all update routines.
	//    lusol8a.f   Replace a column (Bartels-Golub update).
	
	// 10 Jan 2010: First f90 version.
	// 12 Dec 2011: Had to change ip, iq to p, q to avoid clash with ip, rp.
	// 17 Dec 2011: BLAS idamax replaced by private jdamax (taken from sn17util.f90).
	//              Note: jdamax( lencol, a(k,k), 1 ) has to become
	//                    jdamax( lencol,a(k:m,k),1 )
	// 03 Feb 2012: It's ok to have a(k,k) above, but a(k:m) is more illuminating.
	// 03 Feb 2012: Bug fixed in lu1DPP and lu1DCP (translation of call daxpy).
	// 09 Mar 2013: Begin project for improving efficiency of TRP.
	//              Mostly this needs a new version of lu1mxr.
	//              Ding Ma and Michael Saunders, Stanford University.
	// 03 Apr 2013: New lu1mxr finds max element Amaxr(i) in each modified row i
	//              much more efficiently.  Three new local arrays needed:
	//              markc(n) and markr(m) in lu1fad, and cols(n) in lu1mxr.
	//              This is easy in f90.
	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

	//module lusol
	//  use  lusol_precision, only : ip, rp
	// use  snModuleIO,        only : snPRNT
	// use  snModuleWork,      only : snAj  , snWork
	// use  sn15blas,          only : idamax

	 // implicit none
	 // private
	 // public    :: lu1fac, lu6sol, lu8rpc
	 // private   :: jdamax
	 // intrinsic :: abs, int, max, min, real

	// contains

	  //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	  // History from lusol1.f
	  //
	  // 26 Apr 2002: TCP implemented using heap data structure.
	  // 01 May 2002: lu1DCP implemented.
	  // 07 May 2002: lu1mxc must put 0.0 at top of empty columns.
	  // 09 May 2002: lu1mCP implements Markowitz with cols searched
	  //              in heap order.
	  //              Often faster (searching 20 or 40 cols) but more dense.
	  // 11 Jun 2002: TRP implemented.
	  //              lu1mRP implements Markowitz with Threshold Rook Pivoting.
	  //              lu1mxc maintains max col elements.  (Previously lu1max.)
	  //              lu1mxr maintains max row elements.
	  // 12 Jun 2002: lu1mCP seems too slow on big problems (e.g. memplus).
	  //              Disabled it for the moment.  (Use lu1mar + TCP.)
	  // 14 Dec 2002: TSP implemented.
	  //              lu1mSP implements Markowitz with
	  //              Threshold Symmetric Pivoting.
	  // 07 Mar 2003: character*1, character*2 changed to f90 form.
	  //              Comments changed from * in column to ! in column 1.
	  //              Comments kept within column 72 to avoid compiler warning.
	  // 19 Dec 2004: Hdelete(...) has new input argument Hlenin.
	  // 21 Dec 2004: Print Ltol and Lmax with e10.2 instead of e10.1.
	  // 26 Mar 2006: lu1fad: Ignore nsing from lu1ful.
	  //              lu1DPP: nsing redefined (but not used by lu1fad).
	  //              lu1DCP: nsing redefined (but not used by lu1fad).
	  //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

	  public void lu1fac(int m, int n, int nelem, int lena , int luparm[], double parmlu[],
	                     double a[], int indc[], int indr[], int p[], int q[],
	                     int lenc[], int lenr[], int locc[], int locr[],
	                     int iploc[], int iqloc[], int ipinv[], int iqinv[], double w[], int inform[]) {

	    // integer(ip),   intent(in)    :: m, n, nelem, lena
	    // integer(ip),   intent(inout) :: luparm(30)
	    // integer(ip),   intent(out)   :: inform
	    // real(rp),      intent(inout) :: parmlu(30), a(lena), w(n)
	    // integer(ip),   intent(inout) :: indc(lena), indr(lena), &
	    //                               p(m)      , q(n)      , &
	    //                                lenc(n)   , lenr(m)   , &
	    //                                iploc(n)  , iqloc(m)  , &
	    //                                ipinv(m)  , iqinv(n)  , &
	    //                                locc(n)   , locr(m)

	    //------------------------------------------------------------------
	    // lu1fac computes a factorization A = L*U, where A is a sparse
	    // matrix with m rows and n columns, P*L*P' is lower triangular
	    // and P*U*Q is upper triangular for certain permutations P, Q
	    // (which are returned in the arrays p, q).
	    // Stability is ensured by limiting the size of the elements of L.
	    
	    // The nonzeros of A are input via the parallel arrays a, indc, indr,
	    // which should contain nelem entries of the form    aij,    i,    j
	    // in any order.  There should be no duplicate pairs         i,    j.
	    
	    // ******************************************************************
	    // *        Beware !!!   The row indices i must be in indc,         *
	    // *              and the column indices j must be in indr.         *
	    // *              (Not the other way round!)                        *
	    // ******************************************************************
	    
	    // It does not matter if some of the entries in a(*) are zero.
	    // Entries satisfying  abs( a(i) ) .le. parmlu(3)  are ignored.
	    // Other parameters in luparm and parmlu are described below.
	    
	    // The matrix A may be singular.  On exit, nsing = luparm(11) gives
	    // the number of apparent singularities.  This is the number of
	    // "small" diagonals of the permuted factor U, as judged by
	    // the input tolerances Utol1 = parmlu(4) and  Utol2 = parmlu(5).
	    // The diagonal element diagj associated with column j of A is
	    // "small" if
	    //                 abs( diagj ) .le. Utol1
	    // or
	    //                 abs( diagj ) .le. Utol2 * max( uj ),
	    
	    // where max( uj ) is the maximum element in the j-th column of U.
	    // The position of such elements is returned in w(*).  In general,
	    // w(j) = + max( uj ),  but if column j is a singularity,
	    // w(j) = - max( uj ).  Thus, w(j) .le. 0 if column j appears to be
	    // dependent on the other columns of A.
	    //
	    // NOTE: lu1fac (like certain other sparse LU packages) does not
	    // treat dense columns efficiently.  This means it will be slow
	    // on "arrow matrices" of the form
	    //                  A = (x       a)
	    //                      (  x     b)
	    //                      (    x   c)
	    //                      (      x d)
	    //                      (x x x x e)
	    // if the numerical values in the dense column allow it to be
	    // chosen LATE in the pivot order.
	    
	    // With TPP (Threshold Partial Pivoting), the dense column is
	    // likely to be chosen late.
	    
	    // With TCP (Threshold Complete Pivoting), if any of a,b,c,d
	    // is significantly larger than other elements of A, it will
	    // be chosen as the first pivot and the dense column will be
	    // eliminated, giving reasonably sparse factors.
	    // However, if element e is so big that TCP chooses it, the factors
	    // will become dense.  (It's hard to win on these examples!)
	    // ==================================================================
	    
	    
	    // Notes on the array names
	    // ------------------------
	    
	    // During the LU factorization, the sparsity pattern of the matrix
	    // being factored is stored twice: in a column list and a row list.
	    
	    // The column list is ( a, indc, locc, lenc )
	    // where
	    //       a(*)    holds the nonzeros,
	    //       indc(*) holds the indices for the column list,
	    //       locc(j) points to the start of column j in a(*) and indc(*),
	    //       lenc(j) is the number of nonzeros in column j.
	    
	    // The row list is    (    indr, locr, lenr )
	    // where
	    //       indr(*) holds the indices for the row list,
	    //       locr(i) points to the start of row i in indr(*),
	    //       lenr(i) is the number of nonzeros in row i.
	    
	    
	    // At all stages of the LU factorization, p contains a complete
	    // row permutation.  At the start of stage k,  p(1), ..., p(k-1)
	    // are the first k-1 rows of the final row permutation P.
	    // The remaining rows are stored in an ordered list
	    //                    ( p, iploc, ipinv )
	    // where
	    //       iploc(nz) points to the start in p(*) of the set of rows
	    //                 that currently contain nz nonzeros,
	    //       ipinv(i)  points to the position of row i in p(*).
	    //
	    // For example,
	    //       iploc(1) = k   (and this is where rows of length 1 begin),
	    //       iploc(2) = k+p  if there are p rows of length 1
	    //                      (and this is where rows of length 2 begin).
	    
	    // Similarly for q, iqloc, iqinv.
	    //==================================================================
	    
	    
	    // 00 Jun 1983  Original version.
	    // 00 Jul 1987  nrank  saved in luparm(16).
	    // 12 Apr 1989  ipinv, iqinv added as workspace.
	    // 26 Apr 1989  maxtie replaced by maxcol in Markowitz search.
	    // 16 Mar 1992  jumin  saved in luparm(19).
	    // 10 Jun 1992  lu1fad has to move empty rows and cols to the bottom
	    //              (via lu1pq3) before doing the dense LU.
	    // 12 Jun 1992  Deleted dense LU (lu1ful, lu1vlu).
	    // 25 Oct 1993  keepLU implemented.
	    // 07 Feb 1994  Added new dense LU (lu1ful, lu1den).
	    // 21 Dec 1994  Bugs fixed in lu1fad (nrank) and lu1ful (ipvt).
	    // 08 Aug 1995  Use p instead of w as parameter to lu1or3 (for F90).
	    // 13 Sep 2000  TPP and TCP options implemented.
	    // 17 Oct 2000  Fixed troubles due to A = empty matrix (Todd Munson).
	    // 01 Dec 2000  Save Lmax, Umax, etc. after both lu1fad and lu6chk.
	    //              lu1fad sets them when keepLU = false.
	    //              lu6chk sets them otherwise, and includes items
	    //              from the dense LU.
	    // 11 Mar 2001  lu6chk now looks at diag(U) when keepLU = false.
	    // 26 Apr 2002  New TCP implementation using heap routines to
	    //              store largest element in each column.
	    //              New workspace arrays Ha, Hj, Hk required.
	    //              For compatibility, borrow space from a, indc, indr
	    //              rather than adding new input parameters.
	    // 01 May 2002  lu1den changed to lu1DPP (dense partial  pivoting).
	    //              lu1DCP implemented       (dense complete pivoting).
	    //              Both TPP and TCP now switch to dense mode and end.
	   
	    // 10 Jan 2010: First f90 version.
	    // ---------------------------------------------------------------------
	    
	    
	    //  INPUT PARAMETERS
	    
	    //  m      (not altered) is the number of rows in A.
	    //  n      (not altered) is the number of columns in A.
	    //  nelem  (not altered) is the number of matrix entries given in
	    //         the arrays a, indc, indr.
	    //  lena   (not altered) is the dimension of  a, indc, indr.
	    //         This should be significantly larger than nelem.
	    //         Typically one should have
	    //            lena > max( 2*nelem, 10*m, 10*n, 10000 )
	    //         but some applications may need more.
	    //         On machines with virtual memory it is safe to have
	    //         lena "far bigger than necessary", since not all of the
	    //         arrays will be used.
	    //  a      (overwritten) contains entries   Aij  in   a(1:nelem).
	    //  indc   (overwritten) contains the indices i in indc(1:nelem).
	    //  indr   (overwritten) contains the indices j in indr(1:nelem).
	    
	    //  luparm input parameters:                                Typical value
	    
	    //  luparm( 1) = nout     File number for printed messages.         6
	    
	    //  luparm( 2) = lprint   Print level.                              0
	    //                   <  0 suppresses output.
	    //                   =  0 gives error messages.
	    //                  >= 10 gives statistics about the LU factors.
	    //                  >= 50 gives debug output from lu1fac
	    //                        (the pivot row and column and the
	    //                        no. of rows and columns involved at
	    //                        each elimination step).
	    
	    //  luparm( 3) = maxcol   lu1fac: maximum number of columns         5
	    //                        searched allowed in a Markowitz-type
	    //                        search for the next pivot element.
	    //                        For some of the factorization, the
	    //                        number of rows searched is
	    //                        maxrow = maxcol - 1.
	    
	    //  luparm( 6) = 0    =>  TPP: Threshold Partial   Pivoting.        0
	    //             = 1    =>  TRP: Threshold Rook      Pivoting.
	    //             = 2    =>  TCP: Threshold Complete  Pivoting.
	    //             = 3    =>  TSP: Threshold Symmetric Pivoting.
	    //             = 4    =>  TDP: Threshold Diagonal  Pivoting.
	    //                             (TDP not yet implemented).
	    //                        TRP and TCP are more expensive than TPP but
	    //                        more stable and better at revealing rank.
	    //                        Take care with setting parmlu(1), especially
	    //                        with TCP.
	    //                        NOTE: TSP and TDP are for symmetric matrices
	    //                        that are either definite or quasi-definite.
	    //                        TSP is effectively TRP for symmetric matrices.
	    //                        TDP is effectively TCP for symmetric matrices.
	    
	    //  luparm( 8) = keepLU   lu1fac: keepLU = 1 means the numerical    1
	    //                        factors will be computed if possible.
	    //                        keepLU = 0 means L and U will be discarded
	    //                        but other information such as the row and
	    //                        column permutations will be returned.
	    //                        The latter option requires less storage.
	    
	    //  parmlu input parameters:                                Typical value
	    
	    //  parmlu( 1) = Ltol1    Max Lij allowed during Factor.
	    //                                                  TPP     10.0 or 100.0
	    //                                                  TRP      4.0 or  10.0
	    //                                                  TCP      5.0 or  10.0
	    //                                                  TSP      4.0 or  10.0
	    //                        With TRP and TCP (Rook and Complete Pivoting),
	    //                        values less than 25.0 may be expensive
	    //                        on badly scaled data.  However,
	    //                        values less than 10.0 may be needed
	    //                        to obtain a reliable rank-revealing
	    //                        factorization.
	    //  parmlu( 2) = Ltol2    Max Lij allowed during Updates.            10.0
	    //                        during updates.
	    //  parmlu( 3) = small    Absolute tolerance for       eps**0.8 = 3.0d-13
	    //                        treating reals as zero.
	    //  parmlu( 4) = Utol1    Absolute tol for flagging    eps**0.67= 3.7d-11
	    //                        small diagonals of U.
	    //  parmlu( 5) = Utol2    Relative tol for flagging    eps**0.67= 3.7d-11
	    //                        small diagonals of U.
	    //                        (eps = machine precision)
	    //  parmlu( 6) = Uspace   Factor limiting waste space in  U.      3.0
	    //                        In lu1fac, the row or column lists
	    //                        are compressed if their length
	    //                        exceeds Uspace times the length of
	    //                        either file after the last compression.
	    //  parmlu( 7) = dens1    The density at which the Markowitz      0.3
	    //                        pivot strategy should search maxcol
	    //                        columns and no rows.
	    //                        (Use 0.3 unless you are experimenting
	    //                        with the pivot strategy.)
	    //  parmlu( 8) = dens2    the density at which the Markowitz      0.5
	    //                        strategy should search only 1 column,
	    //                        or (if storage is available)
	    //                        the density at which all remaining
	    //                        rows and columns will be processed
	    //                        by a dense LU code.
	    //                        For example, if dens2 = 0.1 and lena is
	    //                        large enough, a dense LU will be used
	    //                        once more than 10 per cent of the
	    //                        remaining matrix is nonzero.
	    
	    
	    //   OUTPUT PARAMETERS
	    
	    //  a, indc, indr     contain the nonzero entries in the LU factors of A.
	    //         If keepLU = 1, they are in a form suitable for use
	    //         by other parts of the LUSOL package, such as lu6sol.
	    //         U is stored by rows at the start of a, indr.
	    //         L is stored by cols at the end   of a, indc.
	    //         If keepLU = 0, only the diagonals of U are stored, at the
	    //         end of a.
	    //  p, q   are the row and column permutations defining the
	    //         pivot order.  For example, row p(1) and column q(1)
	    //         defines the first diagonal of U.
	    //  lenc(1:numl0) contains the number of entries in nontrivial
	    //         columns of L (in pivot order).
	    //  lenr(1:m) contains the number of entries in each row of U
	    //         (in original order).
	    //  locc(1:n) = 0 (ready for the LU update routines).
	    //  locr(1:m) points to the beginning of the rows of U in a, indr.
	    //  iploc, iqloc, ipinv, iqinv  are undefined.
	    //  w      indicates singularity as described above.
	    //  inform = 0 if the LU factors were obtained successfully.
	    //         = 1 if U appears to be singular, as judged by lu6chk.
	    //         = 3 if some index pair indc(l), indr(l) lies outside
	    //             the matrix dimensions 1:m , 1:n.
	    //         = 4 if some index pair indc(l), indr(l) duplicates
	    //             another such pair.
	    //         = 7 if the arrays a, indc, indr were not large enough.
	    //             Their length "lena" should be increase to at least
	    //             the value "minlen" given in luparm(13).
	    //         = 8 if there was some other fatal error.  (Shouldn't happen!)
	    //         = 9 if no diagonal pivot could be found with TSP or TDP.
	    //             The matrix must not be sufficiently definite
	    //             or quasi-definite.
	    
	    //  luparm output parameters:
	    
	    //  luparm(10) = inform   Return code from last call to any LU routine.
	    //  luparm(11) = nsing    No. of singularities marked in the
	    //                        output array w(*).
	    //  luparm(12) = jsing    Column index of last singularity.
	    //  luparm(13) = minlen   Minimum recommended value for  lena.
	    //  luparm(14) = maxlen   ?
	    //  luparm(15) = nupdat   No. of updates performed by the lu8 routines.
	    //  luparm(16) = nrank    No. of nonempty rows of U.
	    //  luparm(17) = ndens1   No. of columns remaining when the density of
	    //                        the matrix being factorized reached dens1.
	    //  luparm(18) = ndens2   No. of columns remaining when the density of
	    //                        the matrix being factorized reached dens2.
	    //  luparm(19) = jumin    The column index associated with DUmin.
	    //  luparm(20) = numL0    No. of columns in initial  L.
	    //  luparm(21) = lenL0    Size of initial  L  (no. of nonzeros).
	    //  luparm(22) = lenU0    Size of initial  U.
	    //  luparm(23) = lenL     Size of current  L.
	    //  luparm(24) = lenU     Size of current  U.
	    //  luparm(25) = lrow     Length of row file.
	    //  luparm(26) = ncp      No. of compressions of LU data structures.
	    //  luparm(27) = mersum   lu1fac: sum of Markowitz merit counts.
	    //  luparm(28) = nUtri    lu1fac: triangular rows in U.
	    //  luparm(29) = nLtri    lu1fac: triangular rows in L.
	    //  luparm(30) =
	    
	    
	    
	    //  parmlu output parameters:
	    
	    //  parmlu(10) = Amax     Maximum element in  A.
	    //  parmlu(11) = Lmax     Maximum multiplier in current  L.
	    //  parmlu(12) = Umax     Maximum element in current  U.
	    //  parmlu(13) = DUmax    Maximum diagonal in  U.
	    //  parmlu(14) = DUmin    Minimum diagonal in  U.
	    //  parmlu(15) = Akmax    Maximum element generated at any stage
	    //                        during TCP factorization.
	    //  parmlu(16) = growth   TPP: Umax/Amax    TRP, TCP, TSP: Akmax/Amax
	    //  parmlu(17) =
	    //  parmlu(18) =
	    //  parmlu(19) =
	    //  parmlu(20) = resid    lu6sol: residual after solve with U or U'.
	    //  ...
	    //  parmlu(30) =
	    //---------------------------------------------------------------------

	    char mnkey;
	    String kPiv[] = new String[4];
	    int lena2 = 0;
	    int lenH = 0;
	    int lmaxr = 0;
	    int locH = 0;
	    int i, idummy, j, jsing, jumin,
	        k, l, l2, 
	        lenLk, lenUk,
	        ll, llsave, lm,
	        lprint, lPiv, lrow, ltopl,
	        lu, nbump,
	        ncp,
	        nmove, nout,
	        nsing, numl0;
	    int lenL[] = new int[1];
	    int lenU[] = new int[1];
	    int lerr[] = new int[1];
	    int mersum[] = new int[1];
	    int minlen[] = new int[1];
	    int ndens1[] = new int[1];
	    int ndens2[] = new int[1];
	    int nLtri[] = new int[1];
	    int nrank[] = new int[1];
	    int numnz[] = new int[1];
	    int nUtri[] = new int[1];
	    boolean  keepLU, TCP, TPP, TRP, TSP;
	    double Agrwth, avgmer,
	           condU, delem, densty, dincr,
	           dm, dn, growth,
	           Ltol, small, Ugrwth;
	    double Akmax[] = new double[1];
	    double Amax[] = new double[1];
	    double Lmax[] = new double[1];
	    double Umax[] = new double[1];
	    double DUmax[] = new double[1];
	    double DUmin[] = new double[1];
	    final int i1 = 1;
	    final double zero = 0.0;
	    final double one = 1.0;

	    // Grab relevant input parameters.

	    nout   = luparm[0];
	    lprint = luparm[1];
	    lPiv   = luparm[5];
	    keepLU = luparm[7] != 0;

	    Ltol   = parmlu[0];  // Limit on size of Lij
	    small  = parmlu[2];  // Drop tolerance

	    TPP    = lPiv == 0;  // Threshold Partial   Pivoting (normal).
	    TRP    = lPiv == 1;  // Threshold Rook      Pivoting
	    TCP    = lPiv == 2;  // Threshold Complete  Pivoting.
	    TSP    = lPiv == 3;  // Threshold Symmetric Pivoting.
	    kPiv[0] = "PP";
	    kPiv[1] = "RP";
	    kPiv[2] = "CP";
	    kPiv[3] = "SP";

	    // Initialize output parameters.

	    inform[0] = 0;
	    minlen[0] = nelem + 2*(m + n);
	    numl0  = 0;
	    lenL[0]   = 0;
	    lenU[0]   = 0;
	    lrow   = 0;
	    mersum[0] = 0;
	    nUtri[0]  = m;
	    nLtri[0]  = 0;
	    ndens1[0] = 0;
	    ndens2[0] = 0;
	    nrank[0]  = 0;
	    nsing  = 0;
	    jsing  = 0;
	    jumin  = 0;

	    Amax[0]   = zero;
	    Lmax[0]   = zero;
	    Umax[0]   = zero;
	    DUmax[0]  = zero;
	    DUmin[0]  = zero;
	    Akmax[0]  = zero;

	    if (m > n) {
	        mnkey  = '>';
	    }
	    else if (m == n) {
	        mnkey  = '=';
	    }
	    else {
	        mnkey  = '<';
	    }

	    // Float version of dimensions.

	    dm     = m;
	    dn     = n;
	    delem  = nelem;

	    // Initialize workspace parameters.
 
	    luparm[25] = 0;             // ncp
	    while (true) {
		    if (lena < minlen[0]) {
		    	inform[0] = 7;
		    	if (lprint >= 0) {
		    		UI.setDataText("lena = " + lena + " minlen[0] = " + minlen[0] + "\n");
		    	}
		    	break;
		    } // if (lena < minlen[0])
	
		    // -------------------------------------------------------------------
		    // Organize the  aij's  in  a, indc, indr.
		    // lu1or1  deletes small entries, tests for illegal  i,j's,
		    //         and counts the nonzeros in each row and column.
		    // lu1or2  reorders the elements of  A  by columns.
		    // lu1or3  uses the column list to test for duplicate entries
		    //         (same indices  i,j).
		    // lu1or4  constructs a row list from the column list.
		    //-------------------------------------------------------------------
		    lu1or1( m   , n    , nelem, lena , small,
		                 a   , indc , indr , lenc , lenr, 
		                 Amax, numnz, lerr , inform );
	
		    if (nout > 0  &&  lprint >= 10) {
		       densty = 100.0 * delem / (dm * dn);
		       UI.setDataText("m = " + m + " mnkey = " + mnkey + " n = " + n + "\n");
		       UI.setDataText("nelem = " + nelem + " Amax[0] = " + nf.format(Amax[0]) + 
		    		   "densty = " + nf.format(densty) + "\n");
		    }
		    if (inform[0] != 0) {
		    	inform[0] = 3;
		    	if (lprint >= 0) {
		    	    UI.setDataText("lerr[0] = " + lerr[0] + " indc[lerr[0] - 1] = " + indc[lerr[0]-1] +
		    	    		" indr[lerr[0] - 1] = " + indr[lerr[0]-1] + "\n");	
		    	}
		    	break;
		    } // if (inform[0] != 0)
	
		// nelem  = numnz     !!! Don't change nelem.
		// nelem is now numnz below (it might be less than the input value).
	
		    lu1or2( n, numnz[0], lena, a, indc, indr, lenc, locc );
		    lu1or3( m, n, lena, indc, lenc, locc, p, lerr, inform );
	
		    if (inform[0] != 0) {
		    	inform[0] = 4;
		    	if (lprint >= 0) {
		    	    UI.setDataText("lerr[0] = " + lerr[0] + " indc[lerr[0] - 1] = " + indc[lerr[0]-1] +
		    	    		" indr[lerr[0] - 1] = " + indr[lerr[0]-1] + "\n");	
		    	}
		    	break;
		    } // if (inform[0] != 0)
	
		    lu1or4( m, n, numnz[0], lena, indc, indr, lenc, lenr, locc, locr);
	
		    // ------------------------------------------------------------------
		    // Set up lists of rows and columns with equal numbers of nonzeros,
		    // using  indc(*)  as workspace.
		    // ------------------------------------------------------------------
		    int num[] = new int[n];
		    lu1pq1( m, n, lenr, p, iploc, ipinv, num);
		    num = new int[m];
		    lu1pq1( n, m, lenc, q, iqloc, iqinv, num);
	
		    // ------------------------------------------------------------------
		    // For TCP, allocate Ha, Hj, Hk at the end of a, indc, indr.
		    // Then compute the factorization  A = L*U.
		    // ------------------------------------------------------------------
		    if (TPP || TSP) {
		       lenH   = 1;
		       lena2  = lena;
		       locH   = lena;
		       lmaxr  = 1;
		    }
		    else if (TRP) {
		       lenH   = 1;             // Dummy
		       lena2  = lena  - m;     // Reduced length of      a
		       locH   = lena;          // Dummy
		       lmaxr  = lena2 + 1;     // Start of Amaxr      in a
		    }
		    else if (TCP) {
		       lenH   = n;             // Length of heap
		       lena2  = lena  - lenH;  // Reduced length of      a, indc, indr
		       locH   = lena2 + 1;     // Start of Ha, Hj, Hk in a, indc, indr
		       lmaxr  = 1;             // Dummy
		    }
	
		    double Ha[] = new double[lenH];
		    int Hj[] = new int[lenH];
		    int Hk[] = new int[lenH];
		    double Amaxr[] = new double[m];
		    for (i = 0; i < lenH; i++) {
		    	Ha[i] = a[locH - 1 + i];
		    	Hj[i] = indc[locH-1+i];
		    	Hk[i] = indr[locH-1+i];
		    }
		    for (i = 0; i < m; i++) {
		    	Amaxr[i] = a[lmaxr-1+i];
		    }
		    lu1fad( m, n, numnz[0] , lena2, luparm, parmlu,
		                 a     , indc  , indr  , p     , q,
		                 lenc  , lenr  , locc  , locr,
		                 iploc , iqloc , ipinv , iqinv , w,
		                 lenH, Ha, Hj, Hk, Amaxr,
		                 inform, lenL  , lenU  , minlen, mersum,
		                 nUtri , nLtri , ndens1, ndens2, nrank ,
		                 Lmax  , Umax  , DUmax , DUmin , Akmax );
		    for (i = 0; i < lenH; i++) {
		    	a[locH - 1 + i] = Ha[i];
		    	indc[locH-1+i] = Hj[i];
		    	indr[locH-1+i] = Hk[i];
		    }
		    for (i = 0; i < m; i++) {
		    	a[lmaxr-1+i] = Amaxr[i];
		    }
	
		    luparm[15] = nrank[0];
		    luparm[22] = lenL[0];
		    if (inform[0] == 7) {
		    	if (lprint >= 0) {
		    		UI.setDataText("lena = " + lena + " minlen[0] = " + minlen[0] + "\n");
		    	}
		    	break;
		    }
		    if (inform[0] == 9) {
		        if (lprint >= 0) {
		        	UI.setDataText("lu1fac error... TSP used but diagonal pivot cold not be found\n");
		        }
		        break;
		    }
		    if (inform[0] >  0) {
		    	inform[0] = 8;
		    	if (lprint >= 0) {
		    		UI.setDataText("lu1fac error... fatal bug sorry --- this should never happen\n");
		    	}
		    	break;
		    }
	
		    if ( keepLU ) {
		       // ---------------------------------------------------------------
		       // The LU factors are at the top of  a, indc, indr,
		       // with the columns of  L  and the rows of  U  in the order
		       //
		       // ( free )   ... ( u3 ) ( l3 ) ( u2 ) ( l2 ) ( u1 ) ( l1 ).
		       //
		       // Starting with ( l1 ) and ( u1 ), move the rows of  U  to the
		       // left and the columns of  L  to the right, giving
		       //
		       // ( u1 ) ( u2 ) ( u3 ) ...   ( free )   ... ( l3 ) ( l2 ) ( l1 ).
		       //
		       // Also, set  numl0 = the number of nonempty columns of L.
		       // ---------------------------------------------------------------
		       lu     = 0;
		       ll     = lena  + 1;
		       lm     = lena2 + 1;
		       ltopl  = ll - lenL[0] - lenU[0];
		       lrow   = lenU[0];
	
		       for (k = 1; k <= nrank[0]; k++) {
		          i       =   p[k-1];
		          lenUk   = - lenr[i-1];
		          lenr[i-1] =   lenUk;
		          j       =   q[k-1];
		          lenLk   = - lenc[j-1] - 1;
		          if (lenLk > 0) {
		             numl0        = numl0 + 1;
		             iqloc[numl0-1] = lenLk;
		          }
	
		          if (lu + lenUk < ltopl) {
		             // =========================================================
		             // There is room to move ( uk ).  Just right-shift ( lk ).
		             // =========================================================
		             for (idummy = 1; idummy <= lenLk; idummy++) {
		                ll       = ll - 1;
		                lm       = lm - 1;
		                a[ll-1]    = a[lm-1];
		                indc[ll-1] = indc[lm-1];
		                indr[ll-1] = indr[lm-1];
		             } // for (idummy = 1; idummy <= lenLk; idummy++)
		          } // if (lu + lenUk < ltopl)
		          else {
		             // =========================================================
		             // There is no room for ( uk ) yet.  We have to
		             // right-shift the whole of the remaining LU file.
		             // Note that ( lk ) ends up in the correct place.
		             // =========================================================
		             llsave = ll - lenLk;
		             nmove  = lm - ltopl;
	
		             for (idummy = 1; idummy <= nmove; idummy++) {
		                ll       = ll - 1;
		                lm       = lm - 1;
		                a[ll-1]    = a[lm-1];
		                indc[ll-1] = indc[lm-1];
		                indr[ll-1] = indr[lm-1];
		             } // for (idummy = 1; idummy <= nmove; idummy++)
	
		             ltopl  = ll;
		             ll     = llsave;
		             lm     = ll;
		          } // else
	
		          // ======================================================
		          // Left-shift ( uk ).
		          // ======================================================
		          locr[i-1] = lu + 1;
		          l2      = lm - 1;
		          lm      = lm - lenUk;
	
		          for (l = lm; l <= l2; l++) {
		             lu       = lu + 1;
		             a[lu-1]    = a[l-1];
		             indr[lu-1] = indr[l-1];
		          } // for (l = lm; l <= l2; l++) 
		       } // for (k = 1; k <= nrank[0]; k+)
	
		       // ---------------------------------------------------------------
		       // Save the lengths of the nonempty columns of  L,
		       // and initialize  locc(j)  for the LU update routines.
		       // ---------------------------------------------------------------
		       for (k = 1; k <= numl0; k++) {
		          lenc[k-1] = iqloc[k-1];
		       }
	
		       for (j = 1; j <= n; j++) {
		          locc[j-1] = 0;
		       }
	
		       // ---------------------------------------------------------------
		       // Test for singularity.
		       // lu6chk  sets  nsing, jsing, jumin, Lmax, Umax, DUmax, DUmin
		       // (including entries from the dense LU).
		       // input      i1 = 1 means we're calling lu6chk from LUSOL.
		       // output inform = 1 if there are singularities (nsing > 0).
		       // ---------------------------------------------------------------
		       lu6chk( i1, m, n, w, lena, luparm, parmlu,
		                    a, indc, indr, p, q,
		                    lenc, lenr, locc, locr, inform);
		       nsing  = luparm[10];
		       jsing  = luparm[11];
		       jumin  = luparm[18];
		       Lmax[0]   = parmlu[10];
		       Umax[0]   = parmlu[11];
		       DUmax[0]  = parmlu[12];
		       DUmin[0]  = parmlu[13];
		    } // if (keepLU)
	
		    else {
		       // ---------------------------------------------------------------
		       // keepLU = 0.  L and U were not kept, just the diagonals of U.
		       // lu1fac will probably be called again soon with keepLU = .true.
		       // 11 Mar 2001: lu6chk revised.  We can call it with keepLU = 0,
		       //              but we want to keep Lmax, Umax from lu1fad.
		       // 05 May 2002: Allow for TCP with new lu1DCP.  Diag(U) starts
		       //              below lena2, not lena.  Need lena2 in next line.
		       // ---------------------------------------------------------------
		       lu6chk( i1, m, n, w, lena2, luparm, parmlu,
		                    a, indc, indr, p, q,
		                    lenc, lenr, locc, locr, inform );
		       nsing  = luparm[10];
		       jsing  = luparm[11];
		       jumin  = luparm[18];
		       DUmax[0]  = parmlu[12];
		       DUmin[0]  = parmlu[13];
		    } // else 
	
		    break;
	    } // while (true)

	    

	    // Store output parameters.

	    luparm[9] = inform[0];
	    luparm[10] = nsing;
	    luparm[11] = jsing;
	    luparm[12] = minlen[0];
	    luparm[14] = 0;
	    luparm[15] = nrank[0];
	    luparm[16] = ndens1[0];
	    luparm[17] = ndens2[0];
	    luparm[18] = jumin;
	    luparm[19] = numl0;
	    luparm[20] = lenL[0];
	    luparm[21] = lenU[0];
	    luparm[22] = lenL[0];
	    luparm[23] = lenU[0];
	    luparm[24] = lrow;
	    luparm[26] = mersum[0];
	    luparm[27] = nUtri[0];
	    luparm[28] = nLtri[0];

	    parmlu[9] = Amax[0];
	    parmlu[10] = Lmax[0];
	    parmlu[11] = Umax[0];
	    parmlu[12] = DUmax[0];
	    parmlu[13] = DUmin[0];
	    parmlu[14] = Akmax[0];

	    Agrwth = Akmax[0]  / (Amax[0] + 1.0e-20);
	    Ugrwth = Umax[0]   / (Amax[0] + 1.0e-20);
	    if ( TPP ) {
	        growth = Ugrwth;
	    }
	    else { // TRP or TCP or TSP
	        growth = Agrwth;
	    }
	    parmlu[15] = growth;

	    /*!------------------------------------------------------------------
	    ! Print statistics for the LU factors.
	    !------------------------------------------------------------------
	    ncp    = luparm(26)
	    condU  = DUmax / max( DUmin, 1.0e-20_rp )
	    dincr  = lenL + lenU - nelem
	    dincr  = dincr * 100.0_rp / max( delem, one )
	    avgmer = mersum
	    avgmer = avgmer / dm
	    nbump  = m - nUtri - nLtri

	    if (nout > 0  .and.  lprint >= 10) then
	       if ( TPP ) then
	          write(nout, 1100) avgmer, lenL, lenL+lenU, ncp, dincr, &
	                            nUtri, lenU, Ltol, Umax, Ugrwth,     &
	                            nLtri, ndens1, Lmax

	       else
	          write(nout, 1120) kPiv(lPiv), avgmer,                  &
	                            lenL, lenL+lenU, ncp, dincr,         &
	                            nUtri, lenU, Ltol, Umax, Ugrwth,     &
	                            nLtri, ndens1, Lmax, Akmax, Agrwth
	       end if

	       write(nout, 1200) nbump, ndens2, DUmax, DUmin, condU
	    end if

	    return

	1000 format(' m', i12, ' ', a, 'n', i12, '  Elems', i9,    &
	            '  Amax', es10.1, '  Density', f7.2)
	1100 format(' Merit', f8.1, '  lenL', i9, '  L+U', i11,    &
	            '  Cmpressns', i5, '  Incres', f8.2            &
	      /     ' Utri', i9, '  lenU', i9, '  Ltol', es10.2,   &
	            '  Umax', es10.1, '  Ugrwth', es8.1            &
	      /     ' Ltri', i9, '  dense1', i7, '  Lmax', es10.2)
	1120 format(' Mer', a2, f8.1, '  lenL', i9, '  L+U', i11,  &
	            '  Cmpressns', i5, '  Incres', f8.2            &
	      /     ' Utri', i9, '  lenU', i9, '  Ltol', es10.2,   &
	            '  Umax', es10.1, '  Ugrwth', es8.1            &
	      /     ' Ltri', i9, '  dense1', i7, '  Lmax', es10.2, &
	            '  Akmax', es9.1, '  Agrwth', es8.1)
	1200 format(' bump', i9, '  dense2', i7, '  DUmax', es9.1, &
	            '  DUmin', es9.1, '  condU', es9.1)
	1300 format(/ ' lu1fac  error...  entry  a(', i8, ')  has an illegal', &
	              ' row or column index' &
	            //' indc, indr =', 2i8)
	1400 format(/ ' lu1fac  error...  entry  a(', i8, ')  has the same', &
	              ' indices as an earlier entry' &
	            //' indc, indr =', 2i8)
	1700 format(/ ' lu1fac  error...  insufficient storage' &
	            //' Increase  lena  from', i10, '  to at least', i10)
	1800 format(/ ' lu1fac  error...  fatal bug', &
	              '   (sorry --- this should never happen)')
	1900 format(/ ' lu1fac  error...  TSP used but', &
	              ' diagonal pivot could not be found')*/

	  } // lu1fqac

    private void lu1or1( int m, int n, int nelem, int lena, double small,
              double a[], int indc[], int indr[], int lenc[], int lenr[],
              double Amax[], int numnz[], int lerr[], int inform[] ) {

//integer(ip),   intent(in)    :: m, n, nelem, lena
//integer(ip),   intent(out)   :: lerr, inform
//real(rp),      intent(in)    :: small
//real(rp),      intent(inout) :: a(lena)
//integer(ip),   intent(inout) :: indc(lena), indr(lena)
//integer(ip),   intent(out)   :: lenc(n), lenr(m)

//------------------------------------------------------------------
// lu1or1  organizes the elements of an  m by n  matrix  A  as
// follows.  On entry, the parallel arrays   a, indc, indr,
// contain  nelem  entries of the form     aij,    i,    j,
// in any order.  nelem  must be positive.

// Entries not larger than the input parameter  small  are treated as
// zero and removed from   a, indc, indr.  The remaining entries are
// defined to be nonzero.  numnz  returns the number of such nonzeros
// and  Amax  returns the magnitude of the largest nonzero.
// The arrays  lenc, lenr  return the number of nonzeros in each
// column and row of  A.

// inform = 0  on exit, except  inform = 1  if any of the indices in
// indc, indr  imply that the element  aij  lies outside the  m by n
// dimensions of  A.

// xx Feb 1985: Original version.
// 17 Oct 2000: a, indc, indr now have size lena to allow nelem = 0.
//
// 10 Jan 2010: First f90 version.
// 12 Dec 2011: Declare intent and local variables.
// ------------------------------------------------------------------

int i, j, l;
final double zero = 0.0;

for (i = 0; i < m; i++) {
	lenr[i] = 0;
}
for (i = 0; i < n; i++) {
	lenc[i] = 0;
}

lerr[0]   = 0;
Amax[0]   = zero;
numnz[0]  = nelem;

for (l = nelem; l >= 1; l--) {
if (Math.abs(a[l-1]) > small) {
   i      = indc[l-1];
   j      = indr[l-1];
   Amax[0]   = Math.max( Amax[0], Math.abs(a[l-1]) );
   if (i < 1  ||  i > m) {
	   lerr[0] = l;
	   inform[0] = 1;
	   return;
   }
   if (j < 1 ||  j > n) {
	   lerr[0] = l;
	   inform[0] = 1;
	   return;   
   }
   lenr[i-1] = lenr[i-1] + 1;
   lenc[j-1] = lenc[j-1] + 1;
}
else {

   // Replace a negligible element by last element.  Since
  //! we are going backwards, we know the last element is ok.

   a[l-1]    = a[numnz[0]-1];
   indc[l-1] = indc[numnz[0]-1];
   indr[l-1] = indr[numnz[0]-1];
   numnz[0]   = numnz[0] - 1;
} // else
} // for (l = nelem; l >= 1; l--)

inform[0] = 0;
return;
    } // lu1or1

    private void lu1or2(int n, int numa, int lena, double a[], int inum[], int jnum[], int lenc[], int locc[]) {

    //integer(ip),   intent(in)    :: n, numa, lena
    //integer(ip),   intent(in)    :: lenc(n)
    //integer(ip),   intent(inout) :: inum(lena), jnum(lena)
    //integer(ip),   intent(out)   :: locc(n)
    //real(rp),      intent(inout) :: a(lena)

    // ------------------------------------------------------------------
    // lu1or2  sorts a list of matrix elements  a(i,j)  into column
    // order, given  numa  entries  a(i,j),  i,  j  in the parallel
    // arrays  a, inum, jnum  respectively.  The matrix is assumed
    // to have n columns and an arbitrary number of rows.
    //
    // On entry, lenc(*) must contain the length of each column.
    
    // On exit,  a(*) and inum(*)  are sorted,  jnum(*) = 0,  and
    // locc(j)  points to the start of column j.
    
    // lu1or2  is derived from mc20ad, a routine in the Harwell
    // Subroutine Library, author J. K. Reid.

    // xx Feb 1985: Original version.
    // 17 Oct 2000: a, inum, jnum now have size lena to allow nelem = 0.
    
    // 10 Jan 2010: First f90 version.
    // 12 Dec 2011: Declare intent and local variables.
    // ------------------------------------------------------------------

    int i, ice, icep, j, ja, jb, jce, jcep, l;
    double ace, acep;

    // Set  loc(j)  to point to the beginning of column  j.

    l = 1;
    for (j  = 1; j <= n; j++) {
       locc[j-1] = l;
       l       = l + lenc[j-1];
    }

    // Sort the elements into column order.
    // The algorithm is an in-place sort and is of order  numa.

    for (i = 1; i <= numa; i++) {
       // Establish the current entry.
       jce     = jnum[i-1];
       if (jce == 0) {
    	   continue;
       }
       ace     = a[i-1];
       ice     = inum[i-1];
       jnum[i-1] = 0;

       // Chain from current entry.

       for (j = 1; j <= numa; j++) {

          // The current entry is not in the correct position.
          // Determine where to store it.

          l         = locc[jce-1];
          locc[jce-1] = locc[jce-1] + 1;

          // Save the contents of that location.

          acep = a[l-1];
          icep = inum[l-1];
          jcep = jnum[l-1];

          // Store current entry.

          a[l-1]    = ace;
          inum[l-1] = ice;
          jnum[l-1] = 0;

          // If next current entry needs to be processed,
          // copy it into current entry.

          if (jcep == 0) {
        	  break;
          }
          ace = acep;
          ice = icep;
          jce = jcep;
       } // for (j = 1; j <= numa; j++)
    } // for (i = 1; i <= numa; i++)

    // Reset loc(j) to point to the start of column j.

    ja = 1;
    for (j  = 1; j <= n; j++) {
       jb      = locc[j-1];
       locc[j-1] = ja;
       ja      = jb;
    } // for (j  = 1; j <= n; j++)

    } // lu10r2

    private void lu1or3(int m, int n, int lena, int indc[], int lenc[], int locc[], int iw[], int lerr[], int inform[]) {

    //integer(ip),   intent(in)    :: m, n, lena
    //integer(ip),   intent(out)   :: lerr, inform
    //integer(ip),   intent(in)    :: indc(lena), lenc(n), locc(n)
    //integer(ip),   intent(out)   :: iw(m)

    //------------------------------------------------------------------
    // lu1or3  looks for duplicate elements in an m by n matrix A
    // defined by the column list  indc, lenc, locc.
    // iw  is used as a work vector of length  m.
    
    // xx Feb 1985: Original version.
    // 17 Oct 2000: indc, indr now have size lena to allow nelem = 0.
    
    // 10 Jan 2010: First f90 version.
    // 12 Dec 2011: Declare intent and local variables.
    // ------------------------------------------------------------------

    int i, j, l, l1, l2;

    for (i = 0; i < m; i++) {
    	iw[i] = 0;
    }
    lerr[0]    = 0;

    for (j = 1; j <= n; j++) {
       if (lenc[j-1] > 0) {
          l1   = locc[j-1];
          l2   = l1 + lenc[j-1] - 1;

          for (l = l1; l <= l2; l++) {
             i = indc[l-1];
             if (iw[i-1] == j) {
            	 lerr[0] = l;
            	 inform[0] = 1;
            	 return;
             }
             iw[i-1] = j;
          } // for (l = l1; l <= l2; l++)
       } // if (lenc[j-1] > 0)
    } // for (j = 1; j <= n; j++)

    inform[0] = 0;
    return;
    } // lu1or3

    private void lu1or4(int m, int n, int nelem, int lena, int indc[], int indr[], int lenc[], int lenr[], int locc[], int locr[]) {

    //integer(ip),   intent(in)    :: m, n, nelem, lena
    //integer(ip),   intent(in)    :: indc(lena), lenc(n), locc(n)
    //integer(ip),   intent(out)   :: indr(lena), lenr(m), locr(m)

    // ------------------------------------------------------------------
    // lu1or4     constructs a row list  indr, locr
    // from a corresponding column list  indc, locc,
    // given the lengths of both columns and rows in  lenc, lenr.
    //
    // xx Feb 1985: Original version.
    // 17 Oct 2000: indc, indr now have size lena to allow nelem = 0.
    //
    // 10 Jan 2010: First f90 version.
    // 12 Dec 2011: Declare intent and local variables.
    // ------------------------------------------------------------------

    int i, j, jdummy, l, l1, l2, lr;

    // Initialize  locr(i)  to point just beyond where the
    // last component of row  i  will be stored.

    l      = 1;
    for (i = 1; i <= m; i++) {
       l       = l + lenr[i-1];
       locr[i-1] = l;
    }

    // By processing the columns backwards and decreasing  locr(i)
    // each time it is accessed, it will end up pointing to the
    // beginning of row  i  as required.

    l2     = nelem;
    j      = n + 1;

    for (jdummy = 1; jdummy <= n; jdummy++) {
       j  = j - 1;
       if (lenc[j-1] > 0) {
          l1 = locc[j-1];

          for (l = l1; l <= l2; l++) {
             i        = indc[l-1];
             lr       = locr[i-1] - 1;
             locr[i-1]  = lr;
             indr[lr-1] = j;
          } // for (l = l1; l <= l2; l++)

          l2     = l1 - 1;
    } // if (lenc[j-1] > 0)
    } // for (jdummy = 1; jdummy <= n; jdummy++)

    } // lu1or4
    
    private void lu1pq1(int m, int n, int len[], int iperm[], int loc[], int inv[], int num[]) {

    //integer(ip),   intent(in)    :: m, n
    //integer(ip),   intent(in)    :: len(m)
    //integer(ip),   intent(out)   :: iperm(m), loc(n), inv(m)
    //integer(ip),   intent(out)   :: num(n) ! workspace

    // ------------------------------------------------------------------
    // lu1pq1  constructs a permutation  iperm  from the array  len.
    
    // On entry:
    // len(i)  holds the number of nonzeros in the i-th row (say)
    //         of an m by n matrix.
    // num(*)  can be anything (workspace).
    
    // On exit:
    // iperm   contains a list of row numbers in the order
    //         rows of length 0,  rows of length 1,..., rows of length n.
    // loc(nz) points to the first row containing  nz  nonzeros,
    //         nz = 1, n.
    // inv(i)  points to the position of row i within iperm(*).
    //
    // 10 Jan 2010: First f90 version.
    // 12 Dec 2011: Declare intent and local variables.
    // ------------------------------------------------------------------

    int i, l, nz, nzero;

    // Count the number of rows of each length.

    nzero    = 0;
    for (i = 0; i < n; i++) {
    	num[i] = 0;
    	loc[i] = 0;
    }

    for (i = 1; i <= m; i++) {
       nz      = len[i-1];
       if (nz == 0) {
          nzero   = nzero   + 1;
       }
       else {
          num[nz-1] = num[nz-1] + 1;
       }
    } // for (i = 1; i <= m; i++)

    // Set starting locations for each length.

    l      = nzero + 1;
    for (nz  = 1; nz <= n; nz++) {
       loc[nz-1] = l;
       l       = l + num[nz-1];
       num[nz-1] = 0;
    } // for (nz  = 1; nz <= n; nz++)

    // Form the list.

    nzero  = 0;
    for (i = 1; i <= m; i++) {
       nz = len[i-1];
       if (nz == 0) {
          nzero = nzero + 1;
          iperm[nzero-1] = i;
       }
       else {
          l        = loc[nz-1] + num[nz-1];
          iperm[l-1] = i;
          num[nz-1]  = num[nz-1] + 1;
       }
    } // for (i = 1; i <= m; i++)

    // Define the inverse of iperm.

    for (l = 1; l <= m; l++) {
       i      = iperm[l-1];
       inv[i-1] = l;
    }

    } // lu1pq1

    private void lu1fad(int m, int n, int nelem, int lena, int luparm[], double parmlu[],
            double a[], int indc[], int indr[], int p[], int q[],
            int lenc[], int lenr[], int locc[], int locr[],
            int iploc[], int iqloc[], int ipinv[], int iqinv[], double w[],
            int lenH  , double Ha[], int Hj[], int Hk[], double Amaxr[],
            int inform[], int lenL[], int lenU[], int minlen[], int mersum[],
            int nUtri[] , int nLtri[], int ndens1[], int ndens2[], int nrank[],
            double Lmax[], double Umax[], double DUmax[], double DUmin[], double Akmax[]) {

/*integer(ip),   intent(in)    :: m, n, nelem, lena, lenH
integer(ip),   intent(inout) :: luparm(30)
real(rp),      intent(inout) :: parmlu(30), a(lena), Amaxr(m), w(n), Ha(lenH)
integer(ip),   intent(inout) :: indc(lena), indr(lena), p(m)    , q(n)    , &
                           lenc(n)   , lenr(m)   , locc(n) , locr(m) , &
                           iploc(n)  , iqloc(m)  , ipinv(m), iqinv(n), &
                           Hj(lenH)  , Hk(lenH)
integer(ip),   intent(out)   :: inform, lenL  , lenU  , minlen, mersum,     &
                           nUtri , nLtri , ndens1, ndens2, nrank
real(rp),      intent(out)   :: Lmax, Umax, DUmax, DUmin, Akmax

!------------------------------------------------------------------
! lu1fad  is a driver for the numerical phase of lu1fac.
! At each stage it computes a column of  L  and a row of  U,
! using a Markowitz criterion to select the pivot element,
! subject to a stability criterion that bounds the elements of  L.
!
! 00 Jan 1986  Version documented in LUSOL paper:
!              Gill, Murray, Saunders and Wright (1987),
!              Maintaining LU factors of a general sparse matrix,
!              Linear algebra and its applications 88/89, 239-270.
!
! 02 Feb 1989  Following Suhl and Aittoniemi (1987), the largest
!              element in each column is now kept at the start of
!              the column, i.e. in position locc(j) of a and indc.
!              This should speed up the Markowitz searches.
!              To save time on highly triangular matrices, we wait
!              until there are no further columns of length 1
!              before setting and maintaining that property.
!
! 12 Apr 1989  ipinv and iqinv added (inverses of p and q)
!              to save searching p and q for rows and columns
!              altered in each elimination step.  (Used in lu1pq2)
!
! 19 Apr 1989  Code segmented to reduce its size.
!              lu1gau does most of the Gaussian elimination work.
!              lu1mar does just the Markowitz search.
!              lu1mxc moves biggest elements to top of columns.
!              lu1pen deals with pending fill-in in the row list.
!              lu1pq2 updates the row and column permutations.
!
! 26 Apr 1989  maxtie replaced by maxcol, maxrow in the Markowitz
!              search.  maxcol, maxrow change as density increases.
!
! 25 Oct 1993  keepLU implemented.
!
! 07 Feb 1994  Exit main loop early to finish off with a dense LU.
!              densLU tells lu1fad whether to do it.
! 21 Dec 1994  Bug fixed.  nrank was wrong after the call to lu1ful.
! 12 Nov 1999  A parallel version of dcopy gave trouble in lu1ful
!              during left-shift of dense matrix D within a(*).
!              Fixed this unexpected problem here in lu1fad
!              by making sure the first and second D don't overlap.
!
! 13 Sep 2000  TCP (Threshold Complete Pivoting) implemented.
!              lu2max added
!              (finds aijmax from biggest elems in each col).
!              Utri, Ltri and Spars1 phases apply.
!              No switch to Dense CP yet.  (Only TPP switches.)
! 14 Sep 2000  imax needed to remember row containing aijmax.
! 22 Sep 2000  For simplicity, lu1mxc always fixes
!              all modified cols.
!              (TPP spars2 used to fix just the first maxcol cols.)
! 08 Nov 2000: Speed up search for aijmax.
!              Don't need to search all columns if the elimination
!              didn't alter the col containing the current aijmax.
! 21 Nov 2000: lu1slk implemented for Utri phase with TCP
!              to guard against deceptive triangular matrices.
!              (Utri used to have aijtol >= 0.9999 to include
!              slacks, but this allows other 1s to be accepted.)
!              Utri now accepts slacks, but applies normal aijtol
!              test to other pivots.
! 28 Nov 2000: TCP with empty cols must call lu1mxc and lu2max
!              with ( lq1, n, ... ), not just ( 1, n, ... ).
! 23 Mar 2001: lu1fad bug with TCP.
!              A col of length 1 might not be accepted as a pivot.
!              Later it appears in a pivot row and temporarily
!              has length 0 (when pivot row is removed
!              but before the column is filled in).  If it is the
!              last column in storage, the preceding col also thinks
!              it is "last".  Trouble arises when the preceding col
!              needs fill-in -- it overlaps the real "last" column.
!              (Very rarely, same trouble might have happened if
!              the drop tolerance caused columns to have length 0.)
!
!              Introduced ilast to record the last row in row file,
!                         jlast to record the last col in col file.
!              lu1rec returns ilast = indr(lrow + 1)
!                          or jlast = indc(lcol + 1).
!              (Should be an output parameter, but didn't want to
!              alter lu1rec's parameter list.)
!              lu1rec also treats empty rows or cols safely.
!              (Doesn't eliminate them!)
!
! 26 Apr 2002: Heap routines added for TCP.
!              lu2max no longer needed.
!              imax, jmax used only for printing.
! 01 May 2002: lu1DCP implemented (dense complete pivoting).
!              Both TPP and TCP now switch to dense LU
!              when density exceeds dens2.
! 06 May 2002: In dense mode, store diag(U) in natural order.
! 09 May 2002: lu1mCP implemented (Markowitz TCP via heap).
! 11 Jun 2002: lu1mRP implemented (Markowitz TRP).
! 28 Jun 2002: Fixed call to lu1mxr.
! 14 Dec 2002: lu1mSP implemented (Markowitz TSP).
! 15 Dec 2002: Both TPP and TSP can grab cols of length 1
!              during Utri.
! 19 Dec 2004: Hdelete(...) has new input argument Hlenin.
! 26 Mar 2006: lu1fad returns nrank  = min( mrank, nrank )
!              and ignores nsing from lu1ful
!
! 10 Jan 2010: First f90 version.
! 03 Apr 2013: lu1mxr recoded to improve efficiency of TRP.
!------------------------------------------------------------------

logical                :: Utri, Ltri, spars1, spars2, dense,       &
                     densLU, keepLU, TCP, TPP, TRP, TSP
real(rp)               :: abest, aijmax, aijtol, amax, &
                     dens1, dens2, diag,          &
                     Lij, Ltol, small, Uspace
integer(ip), parameter :: i1 = 1
real(rp),    parameter :: zero = 0.0,  one = 1.0


!------------------------------------------------------------------
! Local variables
!---------------
!
! lcol   is the length of the column file.  It points to the last
!        nonzero in the column list.
! lrow   is the analogous quantity for the row file.
! lfile  is the file length (lcol or lrow) after the most recent
!        compression of the column list or row list.
! nrowd  and  ncold  are the number of rows and columns in the
!        matrix defined by the pivot column and row.  They are the
!        dimensions of the submatrix D being altered at this stage.
! melim  and  nelim  are the number of rows and columns in the
!        same matrix D, excluding the pivot column and row.
! mleft  and  nleft  are the number of rows and columns
!        still left to be factored.
! nzchng is the increase in nonzeros in the matrix that remains
!        to be factored after the current elimination
!        (usually negative).
! nzleft is the number of nonzeros still left to be factored.
! nspare is the space we leave at the end of the last row or
!        column whenever a row or column is being moved to the end
!        of its file.  nspare = 1 or 2 might help reduce the
!        number of file compressions when storage is tight.
!
! The row and column ordering permutes A into the form
!
!                        ------------------------
!                         \                     |
!                          \         U1         |
!                           \                   |
!                            --------------------
!                            |\
!                            | \
!                            |  \
!            P A Q   =       |   \
!                            |    \
!                            |     --------------
!                            |     |            |
!                            |     |            |
!                            | L1  |     A2     |
!                            |     |            |
!                            |     |            |
!                            --------------------
!
! where the block A2 is factored as  A2 = L2 U2.
! The phases of the factorization are as follows.
!
! Utri   is true when U1 is being determined.
!        Any column of length 1 is accepted immediately (if TPP).
!
! Ltri   is true when L1 is being determined.
!        lu1mar exits as soon as an acceptable pivot is found
!        in a row of length 1.
!
! spars1 is true while the density of the (modified) A2 is less
!        than the parameter dens1 = parmlu(7) = 0.3 say.
!        lu1mar searches maxcol columns and maxrow rows,
!        where  maxcol = luparm(3),  maxrow = maxcol - 1.
!        lu1mxc is used to keep the biggest element at the top
!        of all remaining columns.
!
! spars2 is true while the density of the modified A2 is less
!        than the parameter dens2 = parmlu(8) = 0.6 say.
!        lu1mar searches maxcol columns and no rows.
!        lu1mxc could fix up only the first maxcol cols (with TPP).
!        22 Sep 2000:  For simplicity, lu1mxc fixes all modified cols.
!
! dense  is true once the density of A2 reaches dens2.
!        lu1mar searches only 1 column (the shortest).
!        lu1mxc could fix up only the first column (with TPP).
!        22 Sep 2000:  For simplicity, lu1mxc fixes all modified cols.
!------------------------------------------------------------------

integer(ip)       :: Hlen, Hlenin, hops, h,                &
                i, ibest, ilast, imax,                &
                j, jbest, jlast, jmax, lPiv,          &
                k, kbest, kk, l, last, lc, lc1, lcol, &
                lD, ldiagU, lenD, leni, lenj,         &
                lfile, lfirst, lfree, limit,          &
                ll, ll1, lpivc, lpivc1, lpivc2,       &
                lpivr, lpivr1, lpivr2, lprint,        &
                lq, lq1, lq2, lr, lr1,                &
                lrow, ls, lsave, lu, lu1,             &
                mark, maxcol, maxmn, maxrow, mbest,   &
                melim, minfre, minmn, mleft,          &
                mrank, ncold, nelim, nfill,           &
                nfree, nleft, nout, nrowd, nrowu,     &
                nsing, nspare, nzchng, nzleft
integer(ip)       :: markc(n), markr(m)
real(rp)          :: v

nout   = luparm(1)
lprint = luparm(2)
maxcol = luparm(3)
lPiv   = luparm(6)
keepLU = luparm(8) /= 0

TPP    = lPiv == 0  ! Threshold Partial   Pivoting (normal).
TRP    = lPiv == 1  ! Threshold Rook      Pivoting
TCP    = lPiv == 2  ! Threshold Complete  Pivoting.
TSP    = lPiv == 3  ! Threshold Symmetric Pivoting.

densLU = .false.
maxrow = maxcol - 1
ilast  = m                 ! Assume row m is last in the row file.
jlast  = n                 ! Assume col n is last in the col file.
lfile  = nelem
lrow   = nelem
lcol   = nelem
minmn  = min( m, n )
maxmn  = max( m, n )
nzleft = nelem
nspare = 1

if ( keepLU ) then
lu1    = lena   + 1
else
! Store only the diagonals of U in the top of memory.
ldiagU = lena   - n
lu1    = ldiagU + 1
end if

Ltol   = parmlu(1)
small  = parmlu(3)
Uspace = parmlu(6)
dens1  = parmlu(7)
dens2  = parmlu(8)
Utri   = .true.
Ltri   = .false.
spars1 = .false.
spars2 = .false.
dense  = .false.

! Check parameters.

Ltol   = max( Ltol, 1.0001_rp )
dens1  = min( dens1, dens2 )

! Initialize output parameters.
! lenL, lenU, minlen, mersum, nUtri, nLtri, ndens1, ndens2, nrank
! are already initialized by lu1fac.

Lmax   = zero
Umax   = zero
DUmax  = zero
DUmin  = 1.0e+20_rp
if (nelem == 0) Dumin = zero
Akmax  = zero
hops   = 0

! More initialization.

if (TPP .or. TSP) then ! Don't worry yet about lu1mxc.
aijmax = zero
aijtol = zero
Hlen   = 1

else ! TRP or TCP
! Move biggest element to top of each column.
! Set w(*) to mark slack columns (unit vectors).

call lu1mxc( i1, n, q, a, indc, lenc, locc )
call lu1slk( m, n, lena, q, iqloc, a, locc, w )
end if

if (TRP) then
! Find biggest element in each row.

mark = 0
call lu1mxr( mark, i1, m, m, n, lena,               &
           a, indc, lenc, locc, indr, lenr, locr, &
           p, markc, markr, Amaxr )
end if

if (TCP) then
! Set Ha(1:Hlen) = biggest element in each column,
! Hj(1:Hlen) = corresponding column indices.

Hlen  = 0
do kk = 1, n
 Hlen     = Hlen + 1
 j        = q(kk)
 lc       = locc(j)
 Ha(Hlen) = abs( a(lc) )
 Hj(Hlen) = j
 Hk(j)    = Hlen
end do

! Build the heap, creating new Ha, Hj and setting Hk(1:Hlen).

call Hbuild( Ha, Hj, Hk, Hlen, Hlen, hops )
end if

!------------------------------------------------------------------
! Start of main loop.
!------------------------------------------------------------------
mleft  = m + 1
nleft  = n + 1

do 800 nrowu = 1, minmn

! mktime = (nrowu / ntime) + 4
! eltime = (nrowu / ntime) + 9
mleft  = mleft - 1
nleft  = nleft - 1

! Bail out if there are no nonzero rows left.

if (iploc(1) > m) go to 900

! For TCP, the largest Aij is at the top of the heap.

if ( TCP ) then
 aijmax = Ha(1)      ! Marvelously easy !
 Akmax  = max( Akmax, aijmax )
 aijtol = aijmax / Ltol
end if

!===============================================================
! Find a suitable pivot element.
!===============================================================

if ( Utri ) then
 !------------------------------------------------------------
 ! So far all columns have had length 1.
 ! We are still looking for the (backward) triangular part of A
 ! that forms the first rows and columns of U.
 !------------------------------------------------------------

 lq1    = iqloc(1)
 lq2    = n
 if (m   >   1) lq2 = iqloc(2) - 1

 if (lq1 <= lq2) then  ! There are more cols of length 1.
    if (TPP .or. TSP) then
       jbest  = q(lq1)   ! Grab the first one.

    else ! TRP or TCP    ! Scan all columns of length 1.
       jbest  = 0

       do lq = lq1, lq2
          j      = q(lq)
          if (w(j) > zero) then ! Accept a slack
             jbest  = j
             go to 250
          end if

          lc     = locc(j)
          amax   = abs( a(lc) )
          if (TRP) then
             i      = indc(lc)
             aijtol = Amaxr(i) / Ltol
          end if

          if (amax >= aijtol) then
             jbest  = j
             go to 250
          end if
       end do
    end if

250          if (jbest > 0) then
       lc     = locc(jbest)
       ibest  = indc(lc)
       mbest  = 0
       go to 300
    end if
 end if

 ! This is the end of the U triangle.
 ! We will not return to this part of the code.
 ! TPP and TSP call lu1mxc for the first time
 ! (to move biggest element to top of each column).

 if (lprint >= 50) then
    write(nout, 1100) 'Utri ended.  spars1 = true'
 end if
 Utri   = .false.
 Ltri   = .true.
 spars1 = .true.
 nUtri  =  nrowu - 1
 if (TPP .or. TSP) then
    call lu1mxc( lq1, n, q, a, indc, lenc, locc )
 end if
end if

if ( spars1 ) then
 !------------------------------------------------------------
 ! Perform a Markowitz search.
 ! Search cols of length 1, then rows of length 1,
 ! then   cols of length 2, then rows of length 2, etc.
 !------------------------------------------------------------
 ! if (TPP) then ! 12 Jun 2002: Next line disables lu1mCP below
 if (TPP .or. TCP) then
    call lu1mar( m    , n     , lena  , maxmn,          &
                 TCP  , aijtol, Ltol  , maxcol, maxrow, &
                 ibest, jbest , mbest ,                 &
                 a    , indc  , indr  , p     , q,      &
                 lenc , lenr  , locc  , locr  ,         &
                 iploc, iqloc )

 else if (TRP) then
    call lu1mRP( m    , n     , lena  , maxmn,  &
              Ltol , maxcol, maxrow,            &
              ibest, jbest , mbest ,            &
              a    , indc  , indr  , p    , q,  &
              lenc , lenr  , locc  , locr ,     &
              iploc, iqloc , Amaxr )

    ! else if (TCP) then ! Disabled by test above
    ! call lu1mCP( m    , n     , lena  , aijtol, &
    !              ibest, jbest , mbest ,         &
    !              a    , indc  , indr  ,         &
    !              lenc , lenr  , locc  ,         &
    !              Hlen , Ha    , Hj    )

 else if (TSP) then
    call lu1mSP( m    , n     , lena  , maxmn, &
                 Ltol , maxcol, &
                 ibest, jbest , mbest , &
                 a    , indc  , q    , locc , iqloc )
    if (ibest == 0) go to 990
 end if

 if ( Ltri ) then

    ! So far all rows have had length 1.
    ! We are still looking for the (forward) triangle of A
    ! that forms the first rows and columns of L.

    if (mbest > 0) then
       Ltri   = .false.
       nLtri  =  nrowu - 1 - nUtri
       if (lprint >= 50) then
          write(nout, 1100) 'Ltri ended.'
       end if
    end if

 else    ! See if what's left is as dense as dens1.

    if (nzleft  >=  (dens1 * mleft) * nleft) then
       spars1 = .false.
       spars2 = .true.
       ndens1 =  nleft
       maxrow =  0
       if (lprint >= 50) then
          write(nout, 1100) 'spars1 ended.  spars2 = true'
       end if
    end if
 end if

else if ( spars2 .or. dense ) then
 !------------------------------------------------------------
 ! Perform a restricted Markowitz search,
 ! looking at only the first maxcol columns.  (maxrow = 0.)
 !------------------------------------------------------------
! if (TPP) then ! 12 Jun 2002: Next line disables lu1mCP below
 if (TPP .or. TCP) then
    call lu1mar( m    , n     , lena  , maxmn,          &
                 TCP  , aijtol, Ltol  , maxcol, maxrow, &
                 ibest, jbest , mbest ,                 &
                 a    , indc  , indr  , p     , q,      &
                 lenc , lenr  , locc  , locr  ,         &
                 iploc, iqloc )

 else if (TRP) then
    call lu1mRP( m    , n     , lena  , maxmn,     &
                 Ltol , maxcol, maxrow,            &
                 ibest, jbest , mbest ,            &
                 a    , indc  , indr  , p    , q,  &
                 lenc , lenr  , locc  , locr ,     &
                 iploc, iqloc , Amaxr )

    ! else if (TCP) then ! Disabled by test above
    ! call lu1mCP( m    , n     , lena  , aijtol, &
    !              ibest, jbest , mbest ,         &
    !              a    , indc  , indr  ,         &
    !              lenc , lenr  , locc  ,         &
    !              Hlen , Ha    , Hj    )

 else if (TSP) then
    call lu1mSP( m    , n     , lena  , maxmn, &
                 Ltol , maxcol,                &
                 ibest, jbest , mbest ,        &
                 a    , indc  , q    , locc , iqloc )
    if (ibest == 0) go to 985
 end if

 ! See if what's left is as dense as dens2.

 if ( spars2 ) then
    if (nzleft  >=  (dens2 * mleft) * nleft) then
       spars2 = .false.
       dense  = .true.
       ndens2 =  nleft
       maxcol =  1
       if (lprint >= 50) then
          write(nout, 1100) 'spars2 ended.  dense = true'
       end if
    end if
 end if
end if

!---------------------------------------------------------------
! See if we can finish quickly.
!---------------------------------------------------------------
if ( dense  ) then
 lenD   = mleft * nleft
 nfree  = lu1 - 1

 if (nfree >= 2 * lenD) then

    ! There is room to treat the remaining matrix as
    ! a dense matrix D.
    ! We may have to compress the column file first.
    ! 12 Nov 1999: D used to be put at the
    !              beginning of free storage (lD = lcol + 1).
    !              Now put it at the end     (lD = lu1 - lenD)
    !              so the left-shift in lu1ful will not
    !              involve overlapping storage
    !              (fatal with parallel dcopy).

    densLU = .true.
    ndens2 = nleft
    lD     = lu1 - lenD
    if (lcol >= lD) then
       call lu1rec( n, .true., luparm, lcol, lena, a, indc, lenc, locc )
       lfile  = lcol
       jlast  = indc(lcol + 1)
    end if

    go to 900
 end if
end if

!===============================================================
! The best  aij  has been found.
! The pivot row  ibest  and the pivot column  jbest
! define a dense matrix  D  of size  nrowd x ncold.
!===============================================================
300    ncold  = lenr(ibest)
nrowd  = lenc(jbest)
melim  = nrowd  - 1
nelim  = ncold  - 1
mersum = mersum + mbest
lenL   = lenL   + melim
lenU   = lenU   + ncold
if (lprint >= 50) then
 if (nrowu == 1) then
    write(nout, 1100) 'lu1fad debug:'
 end if
 if ( TPP .or. TRP .or. TSP ) then
    write(nout, 1200) nrowu, ibest, jbest, nrowd, ncold
 else ! TCP
    jmax   = Hj(1)
    imax   = indc(locc(jmax))
    write(nout, 1200) nrowu, ibest, jbest, nrowd, ncold, &
                      imax , jmax , aijmax
 end if
end if

!===============================================================
! Allocate storage for the next column of  L  and next row of  U.
! Initially the top of a, indc, indr are used as follows:
!
!            ncold       melim       ncold        melim
!
! a      |...........|...........|ujbest..ujn|li1......lim|
!
! indc   |...........|  lenr(i)  |  lenc(j)  |  markl(i)  |
!
! indr   |...........| iqloc(i)  |  jfill(j) |  ifill(i)  |
!
!       ^           ^             ^           ^            ^
!       lfree   lsave             lu1         ll1          oldlu1
!
! Later the correct indices are inserted:
!
! indc   |           |           |           |i1........im|
!
! indr   |           |           |jbest....jn|ibest..ibest|
!
!===============================================================
if ( keepLU ) then
 ! relax
else
 ! Always point to the top spot.
 ! Only the current column of L and row of U will
 ! take up space, overwriting the previous ones.
 lu1    = ldiagU + 1
end if
ll1    = lu1   - melim
lu1    = ll1   - ncold
lsave  = lu1   - nrowd
lfree  = lsave - ncold

! Make sure the column file has room.
! Also force a compression if its length exceeds a certain limit.

limit  = int(Uspace*real(lfile))  +  m  +  n  +  1000
minfre = ncold  + melim
nfree  = lfree  - lcol
if (nfree < minfre  .or.  lcol > limit) then
 call lu1rec( n, .true., luparm, lcol, lena, a, indc, lenc, locc )
 lfile  = lcol
 jlast  = indc(lcol + 1)
 nfree  = lfree - lcol
 if (nfree < minfre) go to 970
end if

! Make sure the row file has room.

minfre = melim + ncold
nfree  = lfree - lrow
if (nfree < minfre  .or.  lrow > limit) then
 call lu1rec( m, .false., luparm, lrow, lena, a, indr, lenr, locr )
 lfile  = lrow
 ilast  = indr(lrow + 1)
 nfree  = lfree - lrow
 if (nfree < minfre) go to 970
end if

!===============================================================
! Move the pivot element to the front of its row
! and to the top of its column.
!===============================================================
lpivr  = locr(ibest)
lpivr1 = lpivr + 1
lpivr2 = lpivr + nelim

do l = lpivr, lpivr2
 if (indr(l) == jbest) exit
end do

indr(l)     = indr(lpivr)
indr(lpivr) = jbest

lpivc  = locc(jbest)
lpivc1 = lpivc + 1
lpivc2 = lpivc + melim

do l = lpivc, lpivc2
 if (indc(l) == ibest) exit
end do

indc(l)     = indc(lpivc)
indc(lpivc) = ibest
abest       = a(l)
a(l)        = a(lpivc)
a(lpivc)    = abest

if ( keepLU ) then
 ! relax
else
 ! Store just the diagonal of U, in natural order.
 !!!   a(ldiagU + nrowu) = abest ! This was in pivot order.
 a(ldiagU + jbest) = abest
end if

!==============================================================
! Delete pivot col from heap.
! Hk tells us where it is in the heap.
!==============================================================
if ( TCP ) then
 kbest  = Hk(jbest)
 Hlenin = Hlen
 call Hdelete( Ha, Hj, Hk, Hlenin, Hlen, n, kbest, h )
 hops   = hops + h
end if

!===============================================================
! Delete the pivot row from the column file
! and store it as the next row of  U.
! set  indr(lu) = 0     to initialize jfill ptrs on columns of D,
! indc(lu) = lenj  to save the original column lengths.
!===============================================================
a(lu1)    = abest
indr(lu1) = jbest
indc(lu1) = nrowd
lu        = lu1

diag      = abs( abest )
Umax      = max(  Umax, diag )
DUmax     = max( DUmax, diag )
DUmin     = min( DUmin, diag )

do lr = lpivr1, lpivr2
 lu      = lu + 1
 j       = indr(lr)
 lenj    = lenc(j)
 lenc(j) = lenj - 1
 lc1     = locc(j)
 last    = lc1 + lenc(j)

 do l = lc1, last
    if (indc(l) == ibest) exit
 end do

 a(lu)      = a(l)
 indr(lu)   = 0
 indc(lu)   = lenj
 Umax       = max( Umax, abs( a(lu) ) )
 a(l)       = a(last)
 indc(l)    = indc(last)
 indc(last) = 0       ! Free entry
 !??? if (j == jlast) lcol = lcol - 1
end do

!===============================================================
! Delete the pivot column from the row file
! and store the nonzeros of the next column of  L.
! Set  indc(ll) = 0      to initialize markl(*) markers,
! indr(ll) = 0           to initialize ifill(*) row fill-in cntrs,
! indc(ls) = leni        to save the original row lengths,
! indr(ls) = iqloc(i)    to save parts of  iqloc(*),
! iqloc(i) = lsave - ls  to point to the nonzeros of  L
!          = -1, -2, -3, ... in mark(*).
!===============================================================
indc(lsave) = ncold
if (melim == 0) go to 700

ll     = ll1 - 1
ls     = lsave
abest  = one / abest

do lc = lpivc1, lpivc2
 ll       = ll + 1
 ls       = ls + 1
 i        = indc(lc)
 leni     = lenr(i)
 lenr(i)  = leni - 1
 lr1      = locr(i)
 last     = lr1 + lenr(i)

 do l = lr1, last
    if (indr(l) == jbest) exit
 end do

 indr(l)    = indr(last)
 indr(last) = 0       ! Free entry
 !???  if (i == ilast) lrow = lrow - 1

 a(ll)      = - a(lc) * abest
 Lij        = abs( a(ll) )
 Lmax       = max( Lmax, Lij )
 !!!!! DEBUG
 ! if (Lij > Ltol) then
 ! write( *  ,*) ' Big Lij!!!', nrowu
 ! write(nout,*) ' Big Lij!!!', nrowu
 ! end if

 indc(ll)   = 0
 indr(ll)   = 0
 indc(ls)   = leni
 indr(ls)   = iqloc(i)
 iqloc(i)   = lsave - ls
end do

!===============================================================
! Do the Gaussian elimination.
! This involves adding a multiple of the pivot column
! to all other columns in the pivot row.
!
! Sometimes more than one call to lu1gau is needed to allow
! compression of the column file.
! lfirst  says which column the elimination should start with.
! minfre  is a bound on the storage needed for any one column.
! lu      points to off-diagonals of u.
! nfill   keeps track of pending fill-in in the row file.
!===============================================================
if (nelim == 0) go to 700
lfirst = lpivr1
minfre = mleft + nspare
lu     = 1
nfill  = 0

400    call lu1gau( m     , melim , ncold , nspare, small ,         &
           lpivc1, lpivc2, lfirst, lpivr2, lfree , minfre, &
           ilast , jlast , lrow  , lcol  , lu    , nfill , &
           a     , indc  , indr  ,                         &
           lenc  , lenr  , locc  , locr  ,                 &
           iqloc , a(ll1), indc(ll1),                      &
           a(lu1), indr(ll1), indr(lu1) )

if (lfirst > 0) then

 ! The elimination was interrupted.
 ! Compress the column file and try again.
 ! lfirst, lu and nfill have appropriate new values.

 call lu1rec( n, .true., luparm, lcol, lena, a, indc, lenc, locc )
 lfile  = lcol
 jlast  = indc(lcol + 1)
 lpivc  = locc(jbest)
 lpivc1 = lpivc + 1
 lpivc2 = lpivc + melim
 nfree  = lfree - lcol
 if (nfree < minfre) go to 970
 go to 400
end if

!===============================================================
! The column file has been fully updated.
! Deal with any pending fill-in in the row file.
!===============================================================
if (nfill > 0) then

 ! Compress the row file if necessary.
 ! lu1gau has set nfill to be the number of pending fill-ins
 ! plus the current length of any rows that need to be moved.

 minfre = nfill
 nfree  = lfree - lrow
 if (nfree < minfre) then
    call lu1rec( m, .false., luparm, lrow, lena, a, indr, lenr, locr )
    lfile  = lrow
    ilast  = indr(lrow + 1)
    lpivr  = locr(ibest)
    lpivr1 = lpivr + 1
    lpivr2 = lpivr + nelim
    nfree  = lfree - lrow
    if (nfree < minfre) go to 970
 end if

 ! Move rows that have pending fill-in to end of the row file.
 ! Then insert the fill-in.

 call lu1pen( m     , melim , ncold , nspare, ilast, &
              lpivc1, lpivc2, lpivr1, lpivr2, lrow , &
              lenc  , lenr  , locc  , locr  ,        &
              indc  , indr  , indr(ll1), indr(lu1) )
end if

!===============================================================
! Restore the saved values of  iqloc.
! Insert the correct indices for the col of L and the row of U.
!===============================================================
700    lenr(ibest) = 0
lenc(jbest) = 0

ll          = ll1 - 1
ls          = lsave

do lc  = lpivc1, lpivc2
 ll       = ll + 1
 ls       = ls + 1
 i        = indc(lc)
 iqloc(i) = indr(ls)
 indc(ll) = i
 indr(ll) = ibest
end do

lu          = lu1 - 1

do lr  = lpivr, lpivr2
 lu       = lu + 1
 indr(lu) = indr(lr)
end do

!===============================================================
! Free the space occupied by the pivot row
! and update the column permutation.
! Then free the space occupied by the pivot column
! and update the row permutation.
!
! nzchng is found in both calls to lu1pq2, but we use it only
! after the second.
!===============================================================
call lu1pq2( ncold, nzchng, &
           indr(lpivr), indc( lu1 ), lenc, iqloc, q, iqinv )

call lu1pq2( nrowd, nzchng, &
           indc(lpivc), indc(lsave), lenr, iploc, p, ipinv )

nzleft = nzleft + nzchng

!===============================================================
! lu1mxr resets Amaxr(i) in each modified row i.
! lu1mxc moves the largest aij to the top of each modified col j.
! 28 Jun 2002: Note that cols of L have an implicit diag of 1.0,
!              so lu1mxr is called with ll1, not ll1+1, whereas
!              lu1mxc is called with             lu1+1.
!===============================================================
if (Utri .and. TPP) then
 ! Relax -- we're not keeping big elements at the top yet.

else
 if (TRP  .and.  melim > 0) then
    ! Beware: The parts of p that we need are in indc(ll1:ll)
    mark = mark + 1
    call lu1mxr( mark, ll1, ll, m, n, lena,             &
                 a, indc, lenc, locc, indr, lenr, locr, &
                 indc, markc, markr, Amaxr )
               ! ^^^^  Here are the p(k1:k2) needed by lu1mxr.
 end if

 if (nelim > 0) then
    call lu1mxc( lu1+1, lu, indr, a, indc, lenc, locc )

    if (TCP) then ! Update modified columns in heap
       do kk = lu1+1, lu
          j    = indr(kk)
          k    = Hk(j)
          v    = abs( a(locc(j)) ) ! Biggest aij in column j
          call Hchange( Ha, Hj, Hk, Hlen, n, k, v, j, h )
          hops = hops + h
       end do
    end if
 end if
end if

!===============================================================
! Negate lengths of pivot row and column so they will be
! eliminated during compressions.
!===============================================================
lenr(ibest) = - ncold
lenc(jbest) = - nrowd

! Test for fatal bug: row or column lists overwriting L and U.

if (lrow > lsave) go to 980
if (lcol > lsave) go to 980

! Reset the file lengths if pivot row or col was at the end.

if (ibest == ilast) then
 lrow = locr(ibest)
end if

if (jbest == jlast) then
 lcol = locc(jbest)
end if
800 end do

!------------------------------------------------------------------
! End of main loop.
!------------------------------------------------------------------

!------------------------------------------------------------------
! Normal exit.
! Move empty rows and cols to the end of p, q.
! Then finish with a dense LU if necessary.
!------------------------------------------------------------------
900 inform = 0
call lu1pq3( m, lenr, p, ipinv, mrank )
call lu1pq3( n, lenc, q, iqinv, nrank )
nrank  = min( mrank, nrank )

if ( densLU ) then
call lu1ful( m     , n    , lena , lenD , lu1 , TPP, &
           mleft , nleft, nrank, nrowu,            &
           lenL  , lenU , nsing,                   &
           keepLU, small,                          &
           a     , a(lD), indc , indr , p   , q,   &
           lenc  , lenr , locc , ipinv, locr )
!***     21 Dec 1994: Bug in next line.
!***     nrank  = nrank - nsing.  Changed to next line:
!***     nrank  = minmn - nsing

!***     26 Mar 2006: Previous line caused bug with m<n and nsing>0.
! Don't mess with nrank any more.  Let end of lu1fac handle it.
end if

minlen = lenL  +  lenU  +  2*(m + n)
go to 990

! Not enough space free after a compress.
! Set  minlen  to an estimate of the necessary value of  lena.

970 inform = 7
minlen = lena  +  lfile  +  2*(m + n)
go to 990

! Fatal error.  This will never happen!
! (Famous last words.)

980 inform = 8
go to 990

! Fatal error with TSP.  Diagonal pivot not found.

985 inform = 9

! Exit.

990 return

1100 format(/ 1x, a)
1200 format(' nrowu', i7,     '   i,jbest', 2i7, '   nrowd,ncold', 2i6, &
   '   i,jmax', 2i7, '   aijmax', es10.2)*/

} // lu1fad
    
    private void lu6chk(int mode, int m, int n, double w[], int lena, int luparm[], double parmlu[],
            double a[], int indc[], int indr[], int p[], int q[], int lenc[], int lenr[], int locc[], int locr[], int inform[]) {

/*integer(ip),   intent(in)    :: mode, m, n, lena
integer(ip),   intent(inout) :: inform
integer(ip),   intent(inout) :: luparm(30)
integer(ip),   intent(in)    :: indc(lena), indr(lena), p(m), q(n), &
                           lenc(n), lenr(m), locc(n), locr(m)
real(rp),      intent(in)    :: a(lena)
real(rp),      intent(inout) :: parmlu(30)
real(rp),      intent(out)   :: w(n)

!-----------------------------------------------------------------
! lu6chk  looks at the LU factorization  A = L*U.
!
! If mode = 1, lu6chk is being called by lu1fac.
! (Other modes not yet implemented.)
! The important input parameters are
!
! lprint = luparm(2)
! luparm(6) = 1 if TRP
! keepLU = luparm(8)
! Utol1  = parmlu(4)
! Utol2  = parmlu(5)
!
! and the significant output parameters are
!
! inform = luparm(10)
! nsing  = luparm(11)
! jsing  = luparm(12)
! jumin  = luparm(19)
! Lmax   = parmlu(11)
! Umax   = parmlu(12)
! DUmax  = parmlu(13)
! DUmin  = parmlu(14)
! and      w(*).
!
! Lmax  and Umax  return the largest elements in L and U.
! DUmax and DUmin return the largest and smallest diagonals of U
! (excluding diagonals that are exactly zero).
!
! In general, w(j) is set to the maximum absolute element in
! the j-th column of U.  However, if the corresponding diagonal
! of U is small in absolute terms or relative to w(j)
! (as judged by the parameters Utol1, Utol2 respectively),
! then w(j) is changed to - w(j).
!
! Thus, if w(j) is not positive, the j-th column of A
! appears to be dependent on the other columns of A.
! The number of such columns, and the position of the last one,
! are returned as nsing and jsing.
!
! Note that nrank is assumed to be set already, and is not altered.
! Typically, nsing will satisfy      nrank + nsing = n,  but if
! Utol1 and Utol2 are rather large,  nsing > n - nrank   may occur.
!
! If keepLU = 0,
!              Lmax  and Umax  are already set by lu1fac.
!              The diagonals of U are in the top of A.
!              Only Utol1 is used in the singularity test to set w(*).
!
! inform = 0  if A appears to have full column rank (nsing = 0).
! inform = 1  otherwise (nsing > 0).
!
! 00 Jul 1987: Early version.
! 09 May 1988: f77 version.
! 11 Mar 2001: Allow for keepLU = 0.
! 17 Nov 2001: Briefer output for singular factors.
! 05 May 2002: Comma needed in format 1100 (via Kenneth Holmstrom).
! 06 May 2002: With keepLU = 0, diags of U are in natural order.
!              They were not being extracted correctly.
! 23 Apr 2004: TRP can judge singularity better by comparing
!              all diagonals to DUmax.
! 27 Jun 2004: (PEG) Allow write only if nout > 0 .
! 13 Dec 2011: First f90 version.
!------------------------------------------------------------------

character(1)        :: mnkey
logical             :: keepLU, TRP
integer(ip)         :: i, j, jsing, jumin, k, l, l1, l2, ldiagU, lenL, &
                  lprint, ndefic, nout, nrank, nsing
real(rp)            :: aij, diag, DUmax, DUmin, Lmax, Umax, Utol1, Utol2
real(rp), parameter :: zero = 0.0


nout   = luparm(1)
lprint = luparm(2)
TRP    = luparm(6) == 1  ! Threshold Rook Pivoting
keepLU = luparm(8) /= 0
nrank  = luparm(16)
lenL   = luparm(23)
Utol1  = parmlu(4)
Utol2  = parmlu(5)

inform = 0
Lmax   = zero
Umax   = zero
nsing  = 0
jsing  = 0
jumin  = 0
DUmax  = zero
DUmin  = 1.0d+30

w(1:n) = zero


if (keepLU) then
!--------------------------------------------------------------
! Find  Lmax.
!--------------------------------------------------------------
do l = lena + 1 - lenL, lena
 Lmax  = max( Lmax, abs(a(l)) )
end do

!--------------------------------------------------------------
! Find Umax and set w(j) = maximum element in j-th column of U.
!--------------------------------------------------------------
do k = 1, nrank
 i     = p(k)
 l1    = locr(i)
 l2    = l1 + lenr(i) - 1

 do l = l1, l2
    j     = indr(l)
    aij   = abs( a(l) )
    w(j)  = max( w(j), aij )
    Umax  = max( Umax, aij )
 end do
end do

parmlu(11) = Lmax
parmlu(12) = Umax

!--------------------------------------------------------------
! Find DUmax and DUmin, the extreme diagonals of U.
!--------------------------------------------------------------
do k = 1, nrank
 j      = q(k)
 i      = p(k)
 l1     = locr(i)
 diag   = abs( a(l1) )
 DUmax  = max( DUmax, diag )
 if (DUmin > diag) then
    DUmin  =   diag
    jumin  =   j
 end if
end do

else
!--------------------------------------------------------------
! keepLU = 0.
! Only diag(U) is stored.  Set w(*) accordingly.
! Find DUmax and DUmin, the extreme diagonals of U.
!--------------------------------------------------------------
ldiagU = lena - n

do k = 1, nrank
 j      = q(k)
! diag   = abs( a(ldiagU + k) ) ! 06 May 2002: Diags
 diag   = abs( a(ldiagU + j) ) ! are in natural order
 w(j)   = diag
 DUmax  = max( DUmax, diag )
 if (DUmin > diag) then
    DUmin  =   diag
    jumin  =   j
 end if
end do
end if


!--------------------------------------------------------------
! Negate w(j) if the corresponding diagonal of U is
! too small in absolute terms or relative to the other elements
! in the same column of  U.
!
! 23 Apr 2004: TRP ensures that diags are NOT small relative to
!              other elements in their own column.
!              Much better, we can compare all diags to DUmax.
!--------------------------------------------------------------
if (mode == 1  .and.  TRP) then
Utol1 = max( Utol1, Utol2*DUmax )
end if

if (keepLU) then
do k = 1, n
 j     = q(k)
 if (k > nrank) then
    diag   = zero
 else
    i      = p(k)
    l1     = locr(i)
    diag   = abs( a(l1) )
 end if

 if (diag <= Utol1  .or.  diag <= Utol2*w(j)) then
    nsing  =   nsing + 1
    jsing  =   j
    w(j)   = - w(j)
 end if
end do

else ! keepLU = 0

do k = 1, n
 j      = q(k)
 diag   = w(j)

 if (diag <= Utol1) then
    nsing  =   nsing + 1
    jsing  =   j
    w(j)   = - w(j)
 end if
end do
end if


!-----------------------------------------------------------------
! Set output parameters.
!-----------------------------------------------------------------
if (jumin == 0) DUmin = zero
luparm(11) = nsing
luparm(12) = jsing
luparm(19) = jumin
parmlu(13) = DUmax
parmlu(14) = DUmin

if (nsing > 0) then  ! The matrix has been judged singular.
inform = 1
ndefic = n - nrank
if (nout > 0  .and.  lprint >= 0) then
 if (m > n) then
    mnkey  = '>'
 else if (m == n) then
    mnkey  = '='
 else
    mnkey  = '<'
 end if
 write(nout, 1100) mnkey, nrank, ndefic, nsing
end if
end if

! Exit.

luparm(10) = inform
return

1100 format(' Singular(m', a, 'n)', '  rank', i9, '  n-rank', i8, '  nsing', i9)*/

} // lu6chk


}