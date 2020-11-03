package gov.nih.mipav.model.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.locks.Lock;

import Jama.Matrix;
import gov.nih.mipav.model.structures.jama.GeneralizedEigenvalue;
import gov.nih.mipav.model.structures.jama.GeneralizedInverse2;
import gov.nih.mipav.model.structures.jama.LinearEquations;
import gov.nih.mipav.view.Preferences;

/**
 * This is a port of the C++ files in ceres-solver-1.14.0 under the BSD license:
 Ceres Solver - A fast non-linear least squares minimizer
Copyright 2015 Google Inc. All rights reserved.
http://ceres-solver.org/

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice,
  this list of conditions and the following disclaimer.
* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.
* Neither the name of Google Inc. nor the names of its contributors may be
  used to endorse or promote products derived from this software without
  specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

Ceres Solver is an open source C++ library for modeling and solving
large, complicated optimization problems. It is a feature rich, mature
and performant library which has been used in production at Google
since 2010. Ceres Solver can solve two kinds of problems.

1. Non-linear Least Squares problems with bounds constraints.
2. General unconstrained optimization problems.

Please see [ceres-solver.org](http://ceres-solver.org/) for more
information.



 * @author aailb
 *
 */

public abstract class CeresSolver {
	// It is a near impossibility that user code generates this exact
	// value in normal operation, thus we will use it to fill arrays
	// before passing them to user code. If on return an element of the
	// array still contains this value, we will assume that the user code
	// did not write to that memory location.
	private int MAX_LOG_LEVEL = 1;
	private final double kImpossibleValue = 1e302;
	private boolean testMode = false;
	double epsilon = 2.2204460e-16;
	double default_relstep = Math.pow(epsilon,1.0/3.0);
	/** integer scalar containing the number of data points. */
    protected int m; 
    private int num_residuals_;

    /** variables integer scalar containing the number of unknowns. */
    protected int N0; // x.length
	// For SizedCostFunction and AutoDiffCostFunction, DYNAMIC can be
	// specified for the number of residuals. If specified, then the
	// number of residuas for that cost function can vary at runtime.
	//enum DimensionType {
	  private int DYNAMIC = -1;
	//};
	  
	// Argument type used in interfaces that can optionally take ownership
	// of a passed in argument. If TAKE_OWNERSHIP is passed, the called
	// object takes ownership of the pointer argument, and will call
	// delete on it upon completion.
	enum Ownership {
	  DO_NOT_TAKE_OWNERSHIP,
	  TAKE_OWNERSHIP
	};
	
	enum MinimizerType {
		  LINE_SEARCH,
		  TRUST_REGION
		};
		
enum LineSearchDirectionType {
	  // Negative of the gradient.
	  STEEPEST_DESCENT,

	  // A generalization of the Conjugate Gradient method to non-linear
	  // functions. The generalization can be performed in a number of
	  // different ways, resulting in a variety of search directions. The
	  // precise choice of the non-linear conjugate gradient algorithm
	  // used is determined by NonlinerConjuateGradientType.
	  NONLINEAR_CONJUGATE_GRADIENT,

	  // BFGS, and it's limited memory approximation L-BFGS, are quasi-Newton
	  // algorithms that approximate the Hessian matrix by iteratively refining
	  // an initial estimate with rank-one updates using the gradient at each
	  // iteration. They are a generalisation of the Secant method and satisfy
	  // the Secant equation.  The Secant equation has an infinium of solutions
	  // in multiple dimensions, as there are N*(N+1)/2 degrees of freedom in a
	  // symmetric matrix but only N conditions are specified by the Secant
	  // equation. The requirement that the Hessian approximation be positive
	  // definite imposes another N additional constraints, but that still leaves
	  // remaining degrees-of-freedom.  (L)BFGS methods uniquely deteremine the
	  // approximate Hessian by imposing the additional constraints that the
	  // approximation at the next iteration must be the 'closest' to the current
	  // approximation (the nature of how this proximity is measured is actually
	  // the defining difference between a family of quasi-Newton methods including
	  // (L)BFGS & DFP). (L)BFGS is currently regarded as being the best known
	  // general quasi-Newton method.
	  //
	  // The principal difference between BFGS and L-BFGS is that whilst BFGS
	  // maintains a full, dense approximation to the (inverse) Hessian, L-BFGS
	  // maintains only a window of the last M observations of the parameters and
	  // gradients. Using this observation history, the calculation of the next
	  // search direction can be computed without requiring the construction of the
	  // full dense inverse Hessian approximation. This is particularly important
	  // for problems with a large number of parameters, where storage of an N-by-N
	  // matrix in memory would be prohibitive.
	  //
	  // For more details on BFGS see:
	  //
	  // Broyden, C.G., "The Convergence of a Class of Double-rank Minimization
	  // Algorithms,"; J. Inst. Maths. Applics., Vol. 6, pp 76�90, 1970.
	  //
	  // Fletcher, R., "A New Approach to Variable Metric Algorithms,"
	  // Computer Journal, Vol. 13, pp 317�322, 1970.
	  //
	  // Goldfarb, D., "A Family of Variable Metric Updates Derived by Variational
	  // Means," Mathematics of Computing, Vol. 24, pp 23�26, 1970.
	  //
	  // Shanno, D.F., "Conditioning of Quasi-Newton Methods for Function
	  // Minimization," Mathematics of Computing, Vol. 24, pp 647�656, 1970.
	  //
	  // For more details on L-BFGS see:
	  //
	  // Nocedal, J. (1980). "Updating Quasi-Newton Matrices with Limited
	  // Storage". Mathematics of Computation 35 (151): 773�782.
	  //
	  // Byrd, R. H.; Nocedal, J.; Schnabel, R. B. (1994).
	  // "Representations of Quasi-Newton Matrices and their use in
	  // Limited Memory Methods". Mathematical Programming 63 (4):
	  // 129�156.
	  //
	  // A general reference for both methods:
	  //
	  // Nocedal J., Wright S., Numerical Optimization, 2nd Ed. Springer, 1999.
	  LBFGS,
	  BFGS,
	};
	
	enum LineSearchType {
		  // Backtracking line search with polynomial interpolation or
		  // bisection.
		  ARMIJO,
		  WOLFE,
		};
		
		// Nonliner conjugate gradient methods are a generalization of the
		// method of Conjugate Gradients for linear systems. The
		// generalization can be carried out in a number of different ways
		// leading to number of different rules for computing the search
		// direction. Ceres provides a number of different variants. For more
		// details see Numerical Optimization by Nocedal & Wright.
		enum NonlinearConjugateGradientType {
		  FLETCHER_REEVES,
		  POLAK_RIBIERE,
		  HESTENES_STIEFEL,
		};
		
		// TODO(keir): Considerably expand the explanations of each solver type.
		enum LinearSolverType {
		  // These solvers are for general rectangular systems formed from the
		  // normal equations A'A x = A'b. They are direct solvers and do not
		  // assume any special problem structure.

		  // Solve the normal equations using a dense Cholesky solver; based
		  // on Eigen.
		  DENSE_NORMAL_CHOLESKY,

		  // Solve the normal equations using a dense QR solver; based on
		  // Eigen.
		  DENSE_QR,

		  // Solve the normal equations using a sparse cholesky solver; requires
		  // SuiteSparse or CXSparse.
		  SPARSE_NORMAL_CHOLESKY,

		  // Specialized solvers, specific to problems with a generalized
		  // bi-partitite structure.

		  // Solves the reduced linear system using a dense Cholesky solver;
		  // based on Eigen.
		  DENSE_SCHUR,

		  // Solves the reduced linear system using a sparse Cholesky solver;
		  // based on CHOLMOD.
		  SPARSE_SCHUR,

		  // Solves the reduced linear system using Conjugate Gradients, based
		  // on a new Ceres implementation.  Suitable for large scale
		  // problems.
		  ITERATIVE_SCHUR,

		  // Conjugate gradients on the normal equations.
		  CGNR
		};

		enum PreconditionerType {
		  // Trivial preconditioner - the identity matrix.
		  IDENTITY,

		  // Block diagonal of the Gauss-Newton Hessian.
		  JACOBI,

		  // Note: The following three preconditioners can only be used with
		  // the ITERATIVE_SCHUR solver. They are well suited for Structure
		  // from Motion problems.

		  // Block diagonal of the Schur complement. This preconditioner may
		  // only be used with the ITERATIVE_SCHUR solver.
		  SCHUR_JACOBI,

		  // Visibility clustering based preconditioners.
		  //
		  // The following two preconditioners use the visibility structure of
		  // the scene to determine the sparsity structure of the
		  // preconditioner. This is done using a clustering algorithm. The
		  // available visibility clustering algorithms are described below.
		  //
		  // Note: Requires SuiteSparse.
		  CLUSTER_JACOBI,
		  CLUSTER_TRIDIAGONAL
		};

		enum VisibilityClusteringType {
		  // Canonical views algorithm as described in
		  //
		  // "Scene Summarization for Online Image Collections", Ian Simon, Noah
		  // Snavely, Steven M. Seitz, ICCV 2007.
		  //
		  // This clustering algorithm can be quite slow, but gives high
		  // quality clusters. The original visibility based clustering paper
		  // used this algorithm.
		  CANONICAL_VIEWS,

		  // The classic single linkage algorithm. It is extremely fast as
		  // compared to CANONICAL_VIEWS, but can give slightly poorer
		  // results. For problems with large number of cameras though, this
		  // is generally a pretty good option.
		  //
		  // If you are using SCHUR_JACOBI preconditioner and have SuiteSparse
		  // available, CLUSTER_JACOBI and CLUSTER_TRIDIAGONAL in combination
		  // with the SINGLE_LINKAGE algorithm will generally give better
		  // results.
		  SINGLE_LINKAGE
		};

		enum SparseLinearAlgebraLibraryType {
		  // High performance sparse Cholesky factorization and approximate
		  // minimum degree ordering.
		  SUITE_SPARSE,

		  // A lightweight replacment for SuiteSparse, which does not require
		  // a LAPACK/BLAS implementation. Consequently, its performance is
		  // also a bit lower than SuiteSparse.
		  CX_SPARSE,

		  // Eigen's sparse linear algebra routines. In particular Ceres uses
		  // the Simplicial LDLT routines.
		  EIGEN_SPARSE,

		  // No sparse linear solver should be used.  This does not necessarily
		  // imply that Ceres was built without any sparse library, although that
		  // is the likely use case, merely that one should not be used.
		  NO_SPARSE
		};

		enum DenseLinearAlgebraLibraryType {
		  EIGEN,
		  LAPACK
		};

		// Logging options
		// The options get progressively noisier.
		enum LoggingType {
		  SILENT,
		  PER_MINIMIZER_ITERATION
		};
		
		// Ceres supports different strategies for computing the trust region
		// step.
		enum TrustRegionStrategyType {
		  // The default trust region strategy is to use the step computation
		  // used in the Levenberg-Marquardt algorithm. For more details see
		  // levenberg_marquardt_strategy.h
		  LEVENBERG_MARQUARDT,

		  // Powell's dogleg algorithm interpolates between the Cauchy point
		  // and the Gauss-Newton step. It is particularly useful if the
		  // LEVENBERG_MARQUARDT algorithm is making a large number of
		  // unsuccessful steps. For more details see dogleg_strategy.h.
		  //
		  // NOTES:
		  //
		  // 1. This strategy has not been experimented with or tested as
		  // extensively as LEVENBERG_MARQUARDT, and therefore it should be
		  // considered EXPERIMENTAL for now.
		  //
		  // 2. For now this strategy should only be used with exact
		  // factorization based linear solvers, i.e., SPARSE_SCHUR,
		  // DENSE_SCHUR, DENSE_QR and SPARSE_NORMAL_CHOLESKY.
		  DOGLEG
		};

		// Ceres supports two different dogleg strategies.
		// The "traditional" dogleg method by Powell and the
		// "subspace" method described in
		// R. H. Byrd, R. B. Schnabel, and G. A. Shultz,
		// "Approximate solution of the trust region problem by minimization
		//  over two-dimensional subspaces", Mathematical Programming,
		// 40 (1988), pp. 247--263
		enum DoglegType {
		  // The traditional approach constructs a dogleg path
		  // consisting of two line segments and finds the furthest
		  // point on that path that is still inside the trust region.
		  TRADITIONAL_DOGLEG,

		  // The subspace approach finds the exact minimum of the model
		  // constrained to the subspace spanned by the dogleg path.
		  SUBSPACE_DOGLEG
		};

		enum TerminationType {
		  // Minimizer terminated because one of the convergence criterion set
		  // by the user was satisfied.
		  //
		  // 1.  (new_cost - old_cost) < function_tolerance * old_cost;
		  // 2.  max_i |gradient_i| < gradient_tolerance
		  // 3.  |step|_2 <= parameter_tolerance * ( |x|_2 +  parameter_tolerance)
		  //
		  // The user's parameter blocks will be updated with the solution.
		  CONVERGENCE,

		  // The solver ran for maximum number of iterations or maximum amount
		  // of time specified by the user, but none of the convergence
		  // criterion specified by the user were met. The user's parameter
		  // blocks will be updated with the solution found so far.
		  NO_CONVERGENCE,

		  // The minimizer terminated because of an error.  The user's
		  // parameter blocks will not be updated.
		  FAILURE,

		  // Using an IterationCallback object, user code can control the
		  // minimizer. The following enums indicate that the user code was
		  // responsible for termination.
		  //
		  // Minimizer terminated successfully because a user
		  // IterationCallback returned SOLVER_TERMINATE_SUCCESSFULLY.
		  //
		  // The user's parameter blocks will be updated with the solution.
		  USER_SUCCESS,

		  // Minimizer terminated because because a user IterationCallback
		  // returned SOLVER_ABORT.
		  //
		  // The user's parameter blocks will not be updated.
		  USER_FAILURE
		};

		// Enums used by the IterationCallback instances to indicate to the
		// solver whether it should continue solving, the user detected an
		// error or the solution is good enough and the solver should
		// terminate.
		enum CallbackReturnType {
		  // Continue solving to next iteration.
		  SOLVER_CONTINUE,

		  // Terminate solver, and do not update the parameter blocks upon
		  // return. Unless the user has set
		  // Solver:Options:::update_state_every_iteration, in which case the
		  // state would have been updated every iteration
		  // anyways. Solver::Summary::termination_type is set to USER_ABORT.
		  SOLVER_ABORT,

		  // Terminate solver, update state and
		  // return. Solver::Summary::termination_type is set to USER_SUCCESS.
		  SOLVER_TERMINATE_SUCCESSFULLY
		};

		// The format in which linear least squares problems should be logged
		// when Solver::Options::lsqp_iterations_to_dump is non-empty.
		enum DumpFormatType {
		  // Print the linear least squares problem in a human readable format
		  // to stderr. The Jacobian is printed as a dense matrix. The vectors
		  // D, x and f are printed as dense vectors. This should only be used
		  // for small problems.
		  CONSOLE,

		  // Write out the linear least squares problem to the directory
		  // pointed to by Solver::Options::lsqp_dump_directory as text files
		  // which can be read into MATLAB/Octave. The Jacobian is dumped as a
		  // text file containing (i,j,s) triplets, the vectors D, x and f are
		  // dumped as text files containing a list of their values.
		  //
		  // A MATLAB/octave script called lm_iteration_???.m is also output,
		  // which can be used to parse and load the problem into memory.
		  TEXTFILE
		};
		
		// The differentiation method used to compute numerical derivatives in
		// NumericDiffCostFunction and DynamicNumericDiffCostFunction.
		enum NumericDiffMethodType {
		  // Compute central finite difference: f'(x) ~ (f(x+h) - f(x-h)) / 2h.
		  CENTRAL,

		  // Compute forward finite difference: f'(x) ~ (f(x+h) - f(x)) / h.
		  FORWARD,

		  // Adaptive numerical differentiation using Ridders' method. Provides more
		  // accurate and robust derivatives at the expense of additional cost
		  // function evaluations.
		  RIDDERS
		};

		enum LineSearchInterpolationType {
		  BISECTION,
		  QUADRATIC,
		  CUBIC
		};

		enum CovarianceAlgorithmType {
		  DENSE_SVD,
		  SPARSE_QR,
		};
		
		class CostFunctorExample {
			public CostFunctorExample() {
				
			}
			  public boolean operator(double x[], double residual[]) {
			    residual[0] = 10.0 - x[0];
			    return true;
			  }
			};
			
			// A CostFunction implementing analytically derivatives for the
			// function f(x) = 10 - x.
			class QuadraticCostFunction extends SizedCostFunction  {
			 public QuadraticCostFunction() {
				 // number of resdiuals
				 // size of first parameter
				 super(1, 1,0,0,0,0,0,0,0,0,0);
			 }

			  public boolean Evaluate(Vector<double[]> parameters,
			                        double residuals[],
			                        double jacobians[][]) {
			    double x[] = parameters.get(0);

			    // f(x) = 10 - x.
			    residuals[0] = 10 - x[0];

			    // f'(x) = -1. Since there's only 1 parameter and that parameter
			    // has 1 dimension, there is only 1 element to fill in the
			    // jacobians.
			    //
			    // Since the Evaluate function can be called with the jacobians
			    // pointer equal to NULL, the Evaluate function must check to see
			    // if jacobians need to be computed.
			    //
			    // For this simple problem it is overkill to check if jacobians[0]
			    // is NULL, but in general when writing more complex
			    // CostFunctions, it is possible that Ceres may only demand the
			    // derivatives w.r.t. a subset of the parameter blocks.
			    if (jacobians != null && jacobians[0] != null) {
			      jacobians[0][0] = -1;
			    }

			    return true;
			  }
			};



	  
	  public void runAutoDiffCostFunctionExample() {
		  

		  double x[] = new double[] {0.5};
		// auto-differentiation to obtain the derivative (jacobian).
		  CostFunctorExample cf = new CostFunctorExample();
		  CostFunction cost_function =
		      new AutoDiffCostFunction<CostFunctorExample>(cf, 1, 1,0,0,0,0,0,0,0,0,0);
		  ProblemImpl problem = new ProblemImpl();
		  problem.AddResidualBlock(cost_function, null, x);

		  // Run the solver!
		  Solver solver = new Solver();
		  solver.options.minimizer_progress_to_stdout = true;
		  //Solver::Summary summary;
		  Solve(solver.options, problem, solver.summary);

	  }
	  
	  public void runSizedCostFunctionExample() {
		  

			  double x[] = new double[] {0.5};
			  CostFunction cost_function = new QuadraticCostFunction();
			  ProblemImpl problem = new ProblemImpl();
			  problem.AddResidualBlock(cost_function, null, x);

			  // Run the solver!
			  Solver solver = new Solver();
			  solver.options.minimizer_progress_to_stdout = true;
			  //Solver::Summary summary;
			  Solve(solver.options, problem, solver.summary);

		  }
	  
	// A templated cost functor that implements the residual r = 10 -
	// x. The method operator() is templated so that we can then use an
	// automatic differentiation wrapper around it to generate its
	// derivatives.
	public abstract void fitToFunction(double[] x, double[] residuals);
	
    public abstract void fitToJacobian(double[] x, double[][] jacobian);
    
    public abstract void fitToHessian(double[] x, double[][][] hessian);
    
    public void fitToTestFunction(double[] x, double[] residuals) {
    	
    }
    
    private void fitToTestJacobian(double x[], double J[][]) {
    	
    }
    
    private void fitToTestHessian(double x[], double hessian[][][]) {
    	
    }
    
    private void fitToNumericalJacobian(double xinit[], double[][] jacobian) {
    	int i, j;
        double relstep = default_relstep;
        double absstep = relstep;
        double residualsplus[] = new double[num_residuals_];
        double residualsminus[] = new double[num_residuals_];
        double eps;
        double x[] = new double[xinit.length];
        for (i = 0; i < N0; i++) {
        	x[i] = xinit[i];
        }
        for (j = 0; j < N0; j++) {
    		eps = Math.max(relstep*Math.abs(x[j]), absstep);
    		x[j] = xinit[j] + eps;
    		if (testMode) {
    		    fitToTestFunction(x, residualsplus)	;
    		}
    		else {
    		    fitToFunction(x, residualsplus);	
    		}
    		x[j] = xinit[j] - eps;
    		if (testMode) {
    			fitToTestFunction(x, residualsminus);
    		}
    		else {
    			fitToFunction(x, residualsminus);
    		}
    		x[j] = xinit[j];
    		for (i = 0; i < num_residuals_; i++) {
    			jacobian[i][j] = (residualsplus[i] - residualsminus[i])/(2.0 * eps);
    		}
        }
        
    }
    
    private void fitToNumericalHessian(double xinit[], double hessian[][][]) {
    	int i, j,k;
        double relstep = default_relstep;
        double absstep = relstep;
        double residualsxpp[] = new double[num_residuals_];
        double residualsxpm[] = new double[num_residuals_];
        double residualsxmm[] = new double[num_residuals_];
        double residualsxmp[] = new double[num_residuals_];
        double residualsx[] = new double[num_residuals_];
        double epsi;
        double epsj;
        double x[] = new double[xinit.length];
        for (i = 0; i < N0; i++) {
        	x[i] = xinit[i];
        }
        for (i = 0; i < N0; i++) {
        	epsi = Math.max(relstep*Math.abs(x[i]), absstep);
    		x[i] = xinit[i] + epsi;
    		if (testMode) {
    		    fitToTestFunction(x, residualsxpp)	;
    		}
    		else {
    		    fitToFunction(x, residualsxpp);	
    		}
    		x[i] = xinit[i] - epsi;
    		if (testMode) {
    			fitToTestFunction(x, residualsxmm);
    		}
    		else {
    			fitToFunction(x, residualsxmm);
    		}
    		x[i] = xinit[i];
    		if (testMode) {
    			fitToTestFunction(x, residualsx);
    		}
    		else {
    			fitToFunction(x, residualsx);
    		}
    		for (k = 0; k < num_residuals_; k++) {
    			hessian[i][i][k] = (residualsxpp[k] - 2.0*residualsx[k] + residualsxmm[k])/(epsi*epsi);
    		}
    		for (j = i+1; j < N0; j++) {
    			epsj = Math.max(relstep*Math.abs(x[j]), absstep);
    			x[i] = xinit[i] + epsi;
    			x[j] = xinit[j] + epsj;
    			if (testMode) {
        		    fitToTestFunction(x, residualsxpp)	;
        		}
        		else {
        		    fitToFunction(x, residualsxpp);	
        		}
    			x[j] = xinit[j] - epsj;
    			if (testMode) {
        		    fitToTestFunction(x, residualsxpm);
        		}
        		else {
        		    fitToFunction(x, residualsxpm);	
        		}
    			x[i] = xinit[i] - epsi;
    			if (testMode) {
        		    fitToTestFunction(x, residualsxmm)	;
        		}
        		else {
        		    fitToFunction(x, residualsxmm);	
        		}
    			x[j] = xinit[j] + epsj;
    			if (testMode) {
        		    fitToTestFunction(x, residualsxmp)	;
        		}
        		else {
        		    fitToFunction(x, residualsxmp);	
        		}
    			x[i] = xinit[i];
    			x[j] = xinit[j];
    			for (k = 0; k < num_residuals_; k++) {
        			hessian[i][j][k] = (residualsxpp[k] - residualsxpm[k] + - residualsxmp[k] + residualsxmm[k])/(4.0*epsi*epsj);
        			hessian[j][i][k] = hessian[i][j][k];
        		}
    		}
        }
    }
    
    public void Solve(Solver.Options options, ProblemImpl problem, Solver.Summary summary) {
        if (problem == null) {
        	System.err.println("ProblemImpl problem is null in Solve");
        	return;
        }
        if (summary == null) {
        	System.err.println("Solver.Summary summary is null in Solve");
        	return;
        }
        double start_time = 1.0E-3*System.currentTimeMillis();
        //Summary();
        if (!options.IsValid(summary)) {
            System.err.println("Terminating: " + summary.message[0]);
            return;
        }

        ProblemImpl problem_impl = problem;
        Program program = problem_impl.mutable_program();
        PreSolveSummarize(options, problem_impl, summary);
        
        // The main thread also does work so we only need to launch num_threads - 1.
        // When compiled with C++11 threading support, resize the thread pool to have
        // at min(num_thread, num_hardware_threads) where num_hardware_threads is
        // defined by the hardware.  Otherwise this call is a no-op.
        //problem_impl.context().EnsureMinimumThreads(options.num_threads - 1);
        
        // Make sure that all the parameter blocks states are set to the
        // values provided by the user.
        // Set the parameter block pointers to the user pointers. Since this
        // runs parameter block set state internally, which may call local
        // parameterizations, this can fail. False is returned on failure.
        program.SetParameterBlockStatePtrsToUserStatePtrs();

        // If gradient_checking is enabled, wrap all cost functions in a
        // gradient checker and install a callback that terminates if any gradient
        // error is detected.
        //scoped_ptr<internal::ProblemImpl> gradient_checking_problem;
        ProblemImpl gradient_checking_problem = new ProblemImpl();
        GradientCheckingIterationCallback gradient_checking_callback = new GradientCheckingIterationCallback();
        Solver.Options modified_options = options;
        //options.check_gradients = true;
        if (options.check_gradients) {
          modified_options.callbacks.add(gradient_checking_callback);
          gradient_checking_problem = 
              CreateGradientCheckingProblemImpl(
                  problem_impl,
                  options.gradient_check_numeric_derivative_relative_step_size,
                  options.gradient_check_relative_precision,
                  gradient_checking_callback);
          problem_impl = gradient_checking_problem;
          program = problem_impl.mutable_program();
        }
        
        //scoped_ptr<Preprocessor> preprocessor(
        	      //Preprocessor::Create(modified_options.minimizer_type));
        Preprocessor preprocessor = CreatePreprocessor(modified_options.minimizer_type);
        PreprocessedProblem pp = new PreprocessedProblem();

        boolean status = preprocessor.Preprocess(modified_options, problem_impl, pp);
        
        // We check the linear_solver_options.type rather than
        // modified_options.linear_solver_type because, depending on the
        // lack of a Schur structure, the preprocessor may change the linear
        // solver type.
        if (IsSchurType(pp.linear_solver_options.type)) {
          System.out.println("Schur type");
          // TODO(sameeragarwal): We can likely eliminate the duplicate call
          // to DetectStructure here and inside the linear solver, by
          // calling this in the preprocessor.
          int row_block_size[] = new int[1];
          int e_block_size[] = new int[1];
          int f_block_size[] = new int[1];
          /*DetectStructure(*static_cast<internal::BlockSparseMatrix*>(
                              pp.minimizer_options.jacobian.get())
                          ->block_structure(),
                          pp.linear_solver_options.elimination_groups[0],
                          &row_block_size,
                          &e_block_size,
                          &f_block_size);
          summary.schur_structure_given =
              SchurStructureToString(row_block_size, e_block_size, f_block_size);
          internal::GetBestSchurTemplateSpecialization(&row_block_size,
                                                       &e_block_size,
                                                       &f_block_size);
          summary.schur_structure_used =
              SchurStructureToString(row_block_size, e_block_size, f_block_size);*/
        }
        
        summary.fixed_cost = pp.fixed_cost[0];
        summary.preprocessor_time_in_seconds = 1.0E-3*System.currentTimeMillis() - start_time;

        if (status) {
            double minimizer_start_time = 1.0E-3*System.currentTimeMillis();
            Minimize(pp, summary);
            summary.minimizer_time_in_seconds = 1.0E-3*System.currentTimeMillis() - minimizer_start_time;
          } else {
            summary.message = pp.error;
          }
        
        double postprocessor_start_time = 1.0E-3*System.currentTimeMillis();
        problem_impl = problem;
        program = problem_impl.mutable_program();
        // On exit, ensure that the parameter blocks again point at the user
        // provided values and the parameter blocks are numbered according
        // to their position in the original user provided program.
        program.SetParameterBlockStatePtrsToUserStatePtrs();
        program.SetParameterOffsetsAndIndex();
        PostSolveSummarize(pp, summary);
        summary.postprocessor_time_in_seconds = 1.0E-3*System.currentTimeMillis() - postprocessor_start_time;

        // If the gradient checker reported an error, we want to report FAILURE
        // instead of USER_FAILURE and provide the error log.
        if (gradient_checking_callback.gradient_error_detected()) {
          summary.termination_type = TerminationType.FAILURE;
          summary.message = gradient_checking_callback.error_log();
        }

        summary.total_time_in_seconds = 1.0E-3*System.currentTimeMillis() - start_time;


        //System.err.println("I finish");
    } // public void Solve
    
    public void PostSolveSummarize(PreprocessedProblem pp, Solver.Summary summary) {
			OrderingToGroupSizes(pp.options.linear_solver_ordering, summary.linear_solver_ordering_used);
			OrderingToGroupSizes(pp.options.inner_iteration_ordering, summary.inner_iteration_ordering_used);
			
			summary.inner_iterations_used          = pp.inner_iteration_minimizer != null;     // NOLINT
			summary.linear_solver_type_used        = pp.linear_solver_options.type;
			summary.num_linear_solver_threads_used = pp.options.num_threads;
			summary.num_threads_used               = pp.options.num_threads;
			summary.preconditioner_type_used       = pp.options.preconditioner_type;
			
			/*internal::SetSummaryFinalCost(summary);
			
			if (pp.reduced_program.get() != NULL) {
			SummarizeReducedProgram(*pp.reduced_program, summary);
			}
			
			using internal::CallStatistics;
			
			// It is possible that no evaluator was created. This would be the
			// case if the preprocessor failed, or if the reduced problem did
			// not contain any parameter blocks. Thus, only extract the
			// evaluator statistics if one exists.
			if (pp.evaluator.get() != NULL) {
			const map<string, CallStatistics>& evaluator_statistics =
			pp.evaluator->Statistics();
			{
			const CallStatistics& call_stats = FindWithDefault(
			evaluator_statistics, "Evaluator::Residual", CallStatistics());
			
			summary->residual_evaluation_time_in_seconds = call_stats.time;
			summary->num_residual_evaluations = call_stats.calls;
			}
			{
			const CallStatistics& call_stats = FindWithDefault(
			evaluator_statistics, "Evaluator::Jacobian", CallStatistics());
			
			summary->jacobian_evaluation_time_in_seconds = call_stats.time;
			summary->num_jacobian_evaluations = call_stats.calls;
			}
			}
			
			// Again, like the evaluator, there may or may not be a linear
			// solver from which we can extract run time statistics. In
			// particular the line search solver does not use a linear solver.
			if (pp.linear_solver.get() != NULL) {
			const map<string, CallStatistics>& linear_solver_statistics =
			pp.linear_solver->Statistics();
			const CallStatistics& call_stats = FindWithDefault(
			linear_solver_statistics, "LinearSolver::Solve", CallStatistics());
			summary->num_linear_solves = call_stats.calls;
			summary->linear_solver_time_in_seconds = call_stats.time;
			}*/
			}

    
   public void Minimize(PreprocessedProblem pp, Solver.Summary summary) {
			Program program = pp.reduced_program;
			if (pp.reduced_program.NumParameterBlocks() == 0) {
			  summary.message[0] = "Function tolerance reached. No non-constant parameter blocks found.";
			  summary.termination_type = TerminationType.CONVERGENCE;
			  if(pp.options.logging_type != LoggingType.SILENT) {
				  Preferences.debug(summary.message[0] + "\n", Preferences.DEBUG_ALGORITHM);
			  }
			  summary.initial_cost = summary.fixed_cost;
			  summary.final_cost = summary.fixed_cost;
			  return;
			}

			Vector original_reduced_parameters = pp.reduced_parameters;
			  /*scoped_ptr<Minimizer> minimizer(
			      Minimizer::Create(pp->options.minimizer_type));
			  minimizer->Minimize(pp->minimizer_options,
			                      pp->reduced_parameters.data(),
			                      summary);

			  program->StateVectorToParameterBlocks(
			      summary->IsSolutionUsable()
			      ? pp->reduced_parameters.data()
			      : original_reduced_parameters.data());
			  program->CopyParameterBlockStateToUserState();*/

    } // public void Minimize

    
    public boolean IsSchurType(LinearSolverType type) {
    	  return ((type == LinearSolverType.SPARSE_SCHUR) ||
    	          (type == LinearSolverType.DENSE_SCHUR)  ||
    	          (type == LinearSolverType.ITERATIVE_SCHUR));
    	}

    
    Preprocessor CreatePreprocessor(MinimizerType minimizer_type) {
		  if (minimizer_type == MinimizerType.TRUST_REGION) {
		    return new TrustRegionPreprocessor();
		  }

		  if (minimizer_type == MinimizerType.LINE_SEARCH) {
		    return new LineSearchPreprocessor();
		  }

		  System.err.println("Unknown minimizer_type: " + minimizer_type);
		  return null;
		}
    
    abstract class Preprocessor {
    	public Preprocessor() {
    		
    	}
    	
    	public abstract boolean Preprocess(Solver.Options options,
                 ProblemImpl problem,
                 PreprocessedProblem pp);
    	
    	


    }
    
    class TrustRegionPreprocessor extends Preprocessor {
    	public TrustRegionPreprocessor() {
    		super();
    	}
    	
    	public boolean Preprocess(Solver.Options options,
                ProblemImpl problem,
                PreprocessedProblem pp) {
    		if (pp == null) {
    		    System.err.println("PreprocessedProblem pp == null in TrustRegionPreProcessor Preprocess");
    		    return false;
    		}
    		pp.options = options;
    		//ChangeNumThreadsIfNeeded(pp.options);
    		
    		pp.problem = problem;
    		  Program program = problem.mutable_program();
    		  if (!IsProgramValid(program, pp.error)) {
    		    return false;
    		  }
    		  
    		  pp.reduced_program = 
    			      program.CreateReducedProgram(pp.removed_parameter_blocks,
    			                                    pp.fixed_cost,
    			                                    pp.error);
    		  
    		  if (pp.reduced_program == null) {
    			    return false;
    		  }

    		  if (pp.reduced_program.NumParameterBlocks() == 0) {
    			    // The reduced problem has no parameter or residual blocks. There
    			    // is nothing more to do.
    			    return true;
    		  }

    		  /*if (!SetupLinearSolver(pp) ||
    			  !SetupEvaluator(pp) ||
    			  !SetupInnerIterationMinimizer(pp)) {
    			  return false;
    		  }

    		  SetupMinimizerOptions(pp);*/
    		  return true;
    	}
    	
    	// Check if all the user supplied values in the parameter blocks are
    	// sane or not, and if the program is feasible or not.
    	boolean IsProgramValid(Program program, String error[]) {
    	  return (program.ParameterBlocksAreFinite(error) &&
    	          program.IsFeasible(error));
    	}
    	
    	// Configure and create a linear solver object. In doing so, if a
    	// sparse direct factorization based linear solver is being used, then
    	// find a fill reducing ordering and reorder the program as needed
    	// too.
    	public boolean SetupLinearSolver(PreprocessedProblem pp) {
    	  Solver.Options options = pp.options;
    	  if (options.linear_solver_ordering == null) {
    	    // If the user has not supplied a linear solver ordering, then we
    	    // assume that they are giving all the freedom to us in choosing
    	    // the best possible ordering. This intent can be indicated by
    	    // putting all the parameter blocks in the same elimination group.
    	    options.linear_solver_ordering =
    	        CreateDefaultLinearSolverOrdering(pp.reduced_program);
    	  } else {
    	    // If the user supplied an ordering, then check if the first
    	    // elimination group is still non-empty after the reduced problem
    	    // has been constructed.
    	    //
    	    // This is important for Schur type linear solvers, where the
    	    // first elimination group is special -- it needs to be an
    	    // independent set.
    	    //
    	    // If the first elimination group is empty, then we cannot use the
    	    // user's requested linear solver (and a preconditioner as the
    	    // case may be) so we must use a different one.
    	    OrderedGroups<double[]> ordering = options.linear_solver_ordering;
    	    int min_group_id = ordering.MinNonZeroGroup();
    	    ordering.Remove(pp.removed_parameter_blocks);
    	    if (IsSchurType(options.linear_solver_type) &&
    	        min_group_id != ordering.MinNonZeroGroup()) {
    	      AlternateLinearSolverAndPreconditionerForSchurTypeLinearSolver(
    	          options);
    	    }
    	  }

    	  // Reorder the program to reduce fill in and improve cache coherency
    	  // of the Jacobian.
    	  if (!ReorderProgram(pp)) {
    	    return false;
    	  }

    	  // Configure the linear solver.
    	  /*pp->linear_solver_options = LinearSolver::Options();
    	  pp->linear_solver_options.min_num_iterations =
    	      options.min_linear_solver_iterations;
    	  pp->linear_solver_options.max_num_iterations =
    	      options.max_linear_solver_iterations;
    	  pp->linear_solver_options.type = options.linear_solver_type;
    	  pp->linear_solver_options.preconditioner_type = options.preconditioner_type;
    	  pp->linear_solver_options.visibility_clustering_type =
    	      options.visibility_clustering_type;
    	  pp->linear_solver_options.sparse_linear_algebra_library_type =
    	      options.sparse_linear_algebra_library_type;
    	  pp->linear_solver_options.dense_linear_algebra_library_type =
    	      options.dense_linear_algebra_library_type;
    	  pp->linear_solver_options.use_explicit_schur_complement =
    	      options.use_explicit_schur_complement;
    	  pp->linear_solver_options.dynamic_sparsity = options.dynamic_sparsity;
    	  pp->linear_solver_options.num_threads = options.num_threads;
    	  pp->linear_solver_options.use_postordering = options.use_postordering;
    	  pp->linear_solver_options.context = pp->problem->context();

    	  if (IsSchurType(pp->linear_solver_options.type)) {
    	    OrderingToGroupSizes(options.linear_solver_ordering.get(),
    	                         &pp->linear_solver_options.elimination_groups);

    	    // Schur type solvers expect at least two elimination groups. If
    	    // there is only one elimination group, then it is guaranteed that
    	    // this group only contains e_blocks. Thus we add a dummy
    	    // elimination group with zero blocks in it.
    	    if (pp->linear_solver_options.elimination_groups.size() == 1) {
    	      pp->linear_solver_options.elimination_groups.push_back(0);
    	    }

    	    if (options.linear_solver_type == SPARSE_SCHUR) {
    	      // When using SPARSE_SCHUR, we ignore the user's postordering
    	      // preferences in certain cases.
    	      //
    	      // 1. SUITE_SPARSE is the sparse linear algebra library requested
    	      //    but cholmod_camd is not available.
    	      // 2. CX_SPARSE is the sparse linear algebra library requested.
    	      //
    	      // This ensures that the linear solver does not assume that a
    	      // fill-reducing pre-ordering has been done.
    	      //
    	      // TODO(sameeragarwal): Implement the reordering of parameter
    	      // blocks for CX_SPARSE.
    	      if ((options.sparse_linear_algebra_library_type == SUITE_SPARSE &&
    	           !SuiteSparse::
    	           IsConstrainedApproximateMinimumDegreeOrderingAvailable()) ||
    	          (options.sparse_linear_algebra_library_type == CX_SPARSE)) {
    	        pp->linear_solver_options.use_postordering = true;
    	      }
    	    }
    	  }

    	  pp->linear_solver.reset(LinearSolver::Create(pp->linear_solver_options));
    	  return (pp->linear_solver.get() != NULL);*/
    	  return true;
    	}
    	
    	// For Schur type and SPARSE_NORMAL_CHOLESKY linear solvers, reorder
    	// the program to reduce fill-in and increase cache coherency.
    	public boolean ReorderProgram(PreprocessedProblem pp) {
    	  Solver.Options options = pp.options;
    	  if (IsSchurType(options.linear_solver_type)) {
    	    return ReorderProgramForSchurTypeLinearSolver(
    	        options.linear_solver_type,
    	        options.sparse_linear_algebra_library_type,
    	        pp.problem.parameter_map(),
    	        options.linear_solver_ordering,
    	        pp.reduced_program,
    	        pp.error);
    	  }


    	  if (options.linear_solver_type == LinearSolverType.SPARSE_NORMAL_CHOLESKY &&
    	      !options.dynamic_sparsity) {
    	    /*return ReorderProgramForSparseNormalCholesky(
    	        options.sparse_linear_algebra_library_type,
    	        options.linear_solver_ordering,
    	        pp.reduced_program,
    	        pp.error);*/
    	  }

    	  return true;
    	}

    	
    	public void AlternateLinearSolverAndPreconditionerForSchurTypeLinearSolver(
    		    Solver.Options options) {
    		  if (!IsSchurType(options.linear_solver_type)) {
    		    return;
    		  }

    		  LinearSolverType linear_solver_type_given = options.linear_solver_type;
    		  PreconditionerType preconditioner_type_given = options.preconditioner_type;
    		  options.linear_solver_type = LinearSolverForZeroEBlocks(linear_solver_type_given);

    		  String message;
    		  if (linear_solver_type_given == LinearSolverType.ITERATIVE_SCHUR) {
    		    options.preconditioner_type = PreconditionerForZeroEBlocks(preconditioner_type_given);

    		    message =
    		            "No E blocks. Switching from " +
    		            LinearSolverTypeToString(linear_solver_type_given) + "(" +
    		            PreconditionerTypeToString(preconditioner_type_given) + ") to " +
    		            LinearSolverTypeToString(options.linear_solver_type) + "(" +
    		            PreconditionerTypeToString(options.preconditioner_type) + ").";
    		  } else {
    		    message =
    		            "No E blocks. Switching from " +
    		            LinearSolverTypeToString(linear_solver_type_given) + " to " +
    		            LinearSolverTypeToString(options.linear_solver_type) + ".";
    		  }

    		  if (options.logging_type != LoggingType.SILENT) {
    			  Preferences.debug(message + "\n", Preferences.DEBUG_ALGORITHM);
    		  }
    		}
    	
    	public OrderedGroups<double[]> CreateDefaultLinearSolverOrdering(Program program) {
    		  OrderedGroups<double[]> ordering = new OrderedGroups<>();
    		  Vector<ParameterBlock> parameter_blocks = program.parameter_blocks();
    		  for (int i = 0; i < parameter_blocks.size(); ++i) {
    		    ordering.AddElementToGroup(parameter_blocks.get(i).user_state(), 0);
    		  }
    		  return ordering;
    		}

    	
    }  // class TrustRegionProprocessor
    
    public <T> void swap(T el1, T el2) {
    	T temp = el1;
    	el1 = el2;
    	el2 = temp;
    }
    
    public boolean ReorderProgramForSchurTypeLinearSolver(
    	    LinearSolverType linear_solver_type,
    	    SparseLinearAlgebraLibraryType sparse_linear_algebra_library_type,
    	    HashMap<double[], ParameterBlock> parameter_map,
    	    OrderedGroups<double[]> parameter_block_ordering,
    	    Program program,
    	    String error[]) {
    	  if (parameter_block_ordering.NumElements() !=
    	      program.NumParameterBlocks()) {
    	    error[0] = 
    	        "The program has " + program.NumParameterBlocks() + " parameter blocks, but the parameter block " + "\n" +
    	        "ordering has" + parameter_block_ordering.NumElements() + " parameter blocks.";
    	    return false;
    	  }

    	  if (parameter_block_ordering.NumGroups() == 1) {
    	    // If the user supplied an parameter_block_ordering with just one
    	    // group, it is equivalent to the user supplying NULL as an
    	    // parameter_block_ordering. Ceres is completely free to choose the
    	    // parameter block ordering as it sees fit. For Schur type solvers,
    	    // this means that the user wishes for Ceres to identify the
    	    // e_blocks, which we do by computing a maximal independent set.
    	    Vector<ParameterBlock> schur_ordering = new Vector<ParameterBlock>();
    	    int size_of_first_elimination_group =
    	        ComputeStableSchurOrdering(program, schur_ordering);

    	    if (schur_ordering.size() != program.NumParameterBlocks()) {
    	        System.err.println("Congratulations, you found a Ceres bug! Please report this error ");
    	        System.err.println("to the developers.");
    	        System.err.println("schur_ordering.size() != program.NumParameterBlocks() in ReorderProgramForSchurTypeLinearSolver");
    	        return false;
    	    }

    	    // Update the parameter_block_ordering object.
    	    for (int i = 0; i < schur_ordering.size(); ++i) {
    	      double[] parameter_block = schur_ordering.get(i).mutable_user_state();
    	      int group_id = (i < size_of_first_elimination_group) ? 0 : 1;
    	      parameter_block_ordering.AddElementToGroup(parameter_block, group_id);
    	    }

    	    // We could call ApplyOrdering but this is cheaper and
    	    // simpler.
    	    swap(program.mutable_parameter_blocks(), schur_ordering);
    	  } else {
    	    // The user provided an ordering with more than one elimination
    	    // group.

    	    // Verify that the first elimination group is an independent set.
    		Collection<Set<double[]>>  col = parameter_block_ordering.group_to_elements().values();
    		Iterator <Set<double[]>>it = col.iterator();
    		Set<double[]> first_elimination_group = (Set<double[]>) it.next();
    	    if (!program.IsParameterBlockSetIndependent(first_elimination_group)) {
    	      error[0] =
    	          "The first elimination group in the parameter block \n" +
    	          "ordering of size " + first_elimination_group.size() + " is not an independent set";
    	      return false;
    	    }

    	    if (!ApplyOrdering(parameter_map,
    	                       parameter_block_ordering,
    	                       program,
    	                       error)) {
    	      return false;
    	    }
    	  }

    	  program.SetParameterOffsetsAndIndex();

    	  Collection<Set<double[]>> col = parameter_block_ordering.group_to_elements().values();
  		  Iterator <Set<double[]>>it = col.iterator();
  		  Set<double[]> first_elimination_group = (Set<double[]>) it.next();
    	  int size_of_first_elimination_group =
    	      first_elimination_group.size();

    	  /*if (linear_solver_type == LinearSolverType.SPARSE_SCHUR) {
    	    if (sparse_linear_algebra_library_type == SparseLinearAlgebraLibraryType.SUITE_SPARSE) {
    	      MaybeReorderSchurComplementColumnsUsingSuiteSparse(
    	          *parameter_block_ordering,
    	          program);
    	    } else if (sparse_linear_algebra_library_type == SparseLinearAlgebraLibraryType.EIGEN_SPARSE) {
    	      MaybeReorderSchurComplementColumnsUsingEigen(
    	          size_of_first_elimination_group,
    	          parameter_map,
    	          program);
    	    }
    	  }

    	  // Schur type solvers also require that their residual blocks be
    	  // lexicographically ordered.
    	  if (!LexicographicallyOrderResidualBlocks(size_of_first_elimination_group,
    	                                            program,
    	                                            error)) {
    	    return false;
    	  }*/

    	  return true;
    	}
    
    public boolean ApplyOrdering(HashMap<double[], ParameterBlock> parameter_map,
            OrderedGroups<double[]> ordering,
            Program program,
            String error[]) {
			int num_parameter_blocks =  program.NumParameterBlocks();
			if (ordering.NumElements() != num_parameter_blocks) {
			error[0] = "User specified ordering does not have the same \n" +
			                   "number of parameters as the problem. The problem\n" +
			                   "has " + num_parameter_blocks + " blocks while the ordering has " + ordering.NumElements() + " blocks.";
			return false;
			}
			
			Vector<ParameterBlock> parameter_blocks =
			program.mutable_parameter_blocks();
			parameter_blocks.clear();
			
			HashMap<Integer, Set<double[]> > groups = ordering.group_to_elements();
			Collection<Set<double[]>>  col = groups.values();
    		Iterator <Set<double[]>> group_it = col.iterator();
    		Set<Integer> groupNumSet = groups.keySet();
    		Iterator <Integer> num_iterator = groupNumSet.iterator();
    		while (group_it.hasNext()) {
    		    Set<double[]> group = group_it.next();	
    		    int groupNum = num_iterator.next();
    		    Iterator<double[]> parameter_block_ptr_it = group.iterator();
    		    while (parameter_block_ptr_it.hasNext()) {
    		    	ParameterBlock pm = parameter_map.get(parameter_block_ptr_it.next());
    		    	if (pm == null) {
    		    		error[0] = "User specified ordering contains a double[] \n" +
			                       "that does not have a parameter block in \n" +
			                       "the problem. The invalid double is in group: " + groupNum;
			            return false;	
    		    	}
    		    	parameter_blocks.add(pm);
    		    }
    		}
			return true;
			}

    
	 // A unweighted undirected graph templated over the vertex ids. Vertex
	 // should be hashable.
	 class Graph<Vertex> {
		 private HashSet<Vertex> vertices_;
		 private HashMap<Vertex, HashSet<Vertex> > edges_;
	   public Graph() {
		   vertices_ = new HashSet<Vertex>();
		   edges_ = new HashMap<Vertex, HashSet<Vertex>>();
	   }
	   
	   // Add a vertex.
	   public void AddVertex(Vertex vertex) {
	     if (vertices_.add(vertex)) {
	       edges_.put(vertex, new HashSet<Vertex>());
	     }
	   }
	   
	   // Add an edge between the vertex1 and vertex2. Calling AddEdge on a
	   // pair of vertices which do not exist in the graph yet will result
	   // in undefined behavior.
	   //
	   // It is legal to call this method repeatedly for the same set of
	   // vertices.
	   public void AddEdge(Vertex vertex1, Vertex vertex2) {
		 if (!vertices_.contains(vertex1)) {
			 System.err.println("!vertices_contains(vertex1) in Graph.AddEdge");
			 return;
		 }
		 if (!vertices_.contains(vertex2)) {
			 System.err.println("!vertices_contains(vertex2) in Graph.AddEdge");
			 return;
		 }

	     if (edges_.get(vertex1).add(vertex2)) {
	       edges_.get(vertex2).add(vertex1);
	     }
	   } // public void AddEdge
	   
	   public HashSet<Vertex> vertices() {
		    return vertices_;
	   }
	   
	   // Calling Neighbors on a vertex not in the graph will result in
	   // undefined behaviour.
	   public HashSet<Vertex> Neighbors(Vertex vertex) {
	     HashSet<Vertex> value = edges_.get(vertex);
	     if (value == null) {
	    	 System.err.println("In public HashSet<Vertex> Neighbors HashMap edges_ does not have key vertex");
	    	 return null;
	     }
	     return value;
	   }
	 } // class Graph<Vertex>
	 
	 class VertexDegreeLessThan<Vertex> implements Comparator<Vertex> {
	 private Graph<Vertex> graph_;
	  public VertexDegreeLessThan(Graph<Vertex> graph) {
	      graph_ = graph;
	  }

	// Compare two vertices of a graph by their degrees 
	  public int compare(Vertex lhs, Vertex rhs) {
	     return (graph_.Neighbors(lhs).size() - graph_.Neighbors(rhs).size());
	   }
	 } // class VertexDegreesLessThan
	 
	 public Graph<ParameterBlock> CreateHessianGraph(Program program) {
		  Graph<ParameterBlock> graph = new Graph<ParameterBlock>();
		  Vector<ParameterBlock> parameter_blocks = program.parameter_blocks();
		  for (int i = 0; i < parameter_blocks.size(); ++i) {
		    ParameterBlock parameter_block = parameter_blocks.get(i);
		    if (!parameter_block.IsConstant()) {
		      graph.AddVertex(parameter_block);
		    }
		  }

		  Vector<ResidualBlock> residual_blocks = program.residual_blocks();
		  for (int i = 0; i < residual_blocks.size(); ++i) {
		    ResidualBlock residual_block = residual_blocks.get(i);
		    int num_parameter_blocks = residual_block.NumParameterBlocks();
		    ParameterBlock parameter_blocks2[] =
		        residual_block.parameter_blocks();
		    for (int j = 0; j < num_parameter_blocks; ++j) {
		      if (parameter_blocks2[j].IsConstant()) {
		        continue;
		      }

		      for (int k = j + 1; k < num_parameter_blocks; ++k) {
		        if (parameter_blocks2[k].IsConstant()) {
		          continue;
		        }

		        graph.AddEdge(parameter_blocks2[j], parameter_blocks2[k]);
		      }
		    }
		  }

		  return graph;
		}

    
    public int ComputeStableSchurOrdering(Program program,
            Vector<ParameterBlock> ordering) {
    	    if (ordering == null) {
    	    	System.err.println("Vector<ParameterBlock> ordering == null in ComputeStableSchurOrdering");
    	    	return -1;
    	    }
			ordering.clear();
			EventLogger event_logger = new EventLogger("ComputeStableSchurOrdering");
			//scoped_ptr<Graph< ParameterBlock*> > graph(CreateHessianGraph(program));
			Graph<ParameterBlock> graph = CreateHessianGraph(program);
			event_logger.AddEvent("CreateHessianGraph");
			
			Vector<ParameterBlock> parameter_blocks = program.parameter_blocks();
			HashSet<ParameterBlock> vertices = graph.vertices();
			for (int i = 0; i < parameter_blocks.size(); ++i) {
			if (vertices.contains(parameter_blocks.get(i))) {
			    ordering.add(parameter_blocks.get(i));
			}
			}
			event_logger.AddEvent("Preordering");
			
			int independent_set_size = StableIndependentSetOrdering(graph, ordering);
			event_logger.AddEvent("StableIndependentSet");
			
			// Add the excluded blocks to back of the ordering vector.
			for (int i = 0; i < parameter_blocks.size(); ++i) {
			ParameterBlock parameter_block = parameter_blocks.get(i);
			if (parameter_block.IsConstant()) {
			ordering.add(parameter_block);
			}
			}
			event_logger.AddEvent("ConstantParameterBlocks");
			
			return independent_set_size;
			}
    
 // Same as above with one important difference. The ordering parameter
 // is an input/output parameter which carries an initial ordering of
 // the vertices of the graph. The greedy independent set algorithm
 // starts by sorting the vertices in increasing order of their
 // degree. The input ordering is used to stabilize this sort, i.e., if
 // two vertices have the same degree then they are ordered in the same
 // order in which they occur in "ordering".
 //
 // This is useful in eliminating non-determinism from the Schur
 // ordering algorithm over all.
 public <Vertex>int StableIndependentSetOrdering(Graph<Vertex> graph,
                                  Vector<Vertex> ordering) {
   if (ordering == null) {
	   System.err.println("Vector<Vertex> ordering == null in StableIndependentSetOrdering");
	   return -1;
   }
   HashSet<Vertex> vertices = graph.vertices();
   int num_vertices = vertices.size();
   if (vertices.size() != ordering.size()) {
	   System.err.println("vertices.size() != ordering.size() in StableIndependentSetOrdering");
	   return -1;
   }

   // Colors for labeling the graph during the BFS.
   final byte kWhite = 0;
   final byte kGrey = 1;
   final byte kBlack = 2;

   List<Vertex> vertex_queue= new ArrayList<Vertex>();
   for (int i = 0; i < ordering.size(); i++) {
	   vertex_queue.add(ordering.get(i));
   }
   
   VertexDegreeLessThan<Vertex> VD = new VertexDegreeLessThan<Vertex>(graph);
   Collections.sort(vertex_queue, VD);

   // Mark all vertices white.
   HashMap<Vertex, Byte> vertex_color = new HashMap<Vertex, Byte>();
   for (Vertex v: vertices) {
	   vertex_color.put(v, kWhite);
   }

   ordering.clear();
   ordering.ensureCapacity(num_vertices);
   // Iterate over vertex_queue. Pick the first white vertex, add it
   // to the independent set. Mark it black and its neighbors grey.
   for (Vertex vertex: vertex_queue) {
     if (vertex_color.get(vertex) != kWhite) {
       continue;
     }

     ordering.add(vertex);
     vertex_color.put(vertex,kBlack);
     HashSet<Vertex> neighbors = graph.Neighbors(vertex);
     for (Vertex it : neighbors) {
    	 vertex_color.put(it, kGrey);
     }
   }

   int independent_set_size = ordering.size();

   // Iterate over the vertices and add all the grey vertices to the
   // ordering. At this stage there should only be black or grey
   // vertices in the graph.
   for (Vertex vertex : vertex_queue) {
     if (vertex_color.get(vertex) == kWhite) {
    	 System.err.println("Unexpected kWhite found in vertex_color in StableIndependentSetOrdering");
    	 return -1;
     }
     if (vertex_color.get(vertex) != kBlack) {
       ordering.add(vertex);
     }
   }

   if (ordering.size() != num_vertices) {
       System.err.println("ordering.size() != num_vertices in StableIndependentSetOrdering");
       return -1;
   }
   return independent_set_size;
 }
    
    class EventLogger {
    	private double start_time_;
      	private double last_event_time_;
      	String events_;
    	  public EventLogger(String logger_name) {
    		  start_time_ = 1.0E-3*System.currentTimeMillis();
    	      last_event_time_ = start_time_;
    	      events_ = 
    	                "\n"+logger_name+"\n                                   Delta   Cumulative\n";
    	 
    	  }
 
    	  public void AddEvent(String event_name) {
    		  if (3 <= MAX_LOG_LEVEL) {
    			    return;
    			  }

    			  double current_time = 1.0E-3*System.currentTimeMillis();
    			  double relative_time_delta = current_time - last_event_time_;
    			  double absolute_time_delta = current_time - start_time_;
    			  last_event_time_ = current_time;

    			  events_ = events_ + 
    			                String.format("  %30s : %10.5f   %10.5f\n",
    			                event_name,
    			                relative_time_delta,
    			                absolute_time_delta);
  
    	  }
    	} // class EventLogger

    
    public String LinearSolverTypeToString(LinearSolverType type) {
    	  switch (type) {
    	  case DENSE_NORMAL_CHOLESKY:
    		  return "DENSE_NORMAL_CHOLESKY";
    	  case DENSE_QR:
    		  return "DENSE_QR";
    	  case SPARSE_NORMAL_CHOLESKY:
    		  return "NORMAL_CHOLESKY";
    	  case DENSE_SCHUR:
    		  return "DENSE_SCHUR";
    	  case SPARSE_SCHUR:
    		  return "SPARSE_SCHUR";
    	  case ITERATIVE_SCHUR:
    		  return "ITERATIVR_SCHUR";
    	  case CGNR:
    		  return "CGNR";
    	    default:
    	      return "UNKNOWN";
    	  }
    	}
    
    public String PreconditionerTypeToString(PreconditionerType type) {
    	  switch (type) {
    	  case IDENTITY:
    		  return "IDENTITY";
    	  case JACOBI:
    		  return "JACOBI";
    	  case SCHUR_JACOBI:
    		  return "SCHUR_JACOBI";
    	  case CLUSTER_JACOBI:
    		  return "CLUSTER_JACOBI";
    	  case CLUSTER_TRIDIAGONAL:
    		  return "CLUSTER_TRIDIAGONAL";
    	    default:
    	      return "UNKNOWN";
    	  }
    	}


    
    public PreconditionerType PreconditionerForZeroEBlocks(PreconditionerType preconditioner_type) {
		  if (preconditioner_type == PreconditionerType.SCHUR_JACOBI ||
		      preconditioner_type == PreconditionerType.CLUSTER_JACOBI ||
		      preconditioner_type == PreconditionerType.CLUSTER_TRIDIAGONAL) {
		    return PreconditionerType.JACOBI;
		  }
		  return preconditioner_type;
		}
    
    class LineSearchPreprocessor extends Preprocessor {
    	public LineSearchPreprocessor() {
    		super();
    	}
    	
    	public boolean Preprocess(Solver.Options options,
                ProblemImpl problem,
                PreprocessedProblem pp) {
    		  if (pp == null) {
    		    System.err.println("PreprocessedProblem pp == null in TrustRegionPreProcessor Preprocess");
    		    return false;
    		  }
    		  pp.options = options;
    		  //ChangeNumThreadsIfNeeded(pp.options);

    		  pp.problem = problem;
    		  Program program = problem.mutable_program();
    		  if (!IsProgramValid(program, pp.error)) {
    		    return false;
    		  }

    		  pp.reduced_program = 
    		      program.CreateReducedProgram(pp.removed_parameter_blocks,
    		                                    pp.fixed_cost,
    		                                    pp.error);

    		  if (pp.reduced_program == null) {
    		    return false;
    		  }

    		  if (pp.reduced_program.NumParameterBlocks() == 0) {
    		    return true;
    		  }

    		  if (!SetupEvaluator(pp)) {
    		    return false;
    		  }

    		  SetupCommonMinimizerOptions(pp);

    		return true;
    	}
    	
    	public boolean IsProgramValid(Program program, String error[]) {
    		  if (program.IsBoundsConstrained()) {
    		    error[0] = "LINE_SEARCH Minimizer does not support bounds.";
    		    return false;
    		  }
    		  return program.ParameterBlocksAreFinite(error);
    		}
    	
    	public boolean SetupEvaluator(PreprocessedProblem pp) {
    		  Evaluator ev = new Evaluator();
    		  pp.evaluator_options = ev.options;
    		  // This ensures that we get a Block Jacobian Evaluator without any
    		  // requirement on orderings.
    		  pp.evaluator_options.linear_solver_type = LinearSolverType.CGNR;
    		  pp.evaluator_options.num_eliminate_blocks = 0;
    		  pp.evaluator_options.num_threads = pp.options.num_threads;
    		  pp.evaluator_options.context = pp.problem.context();
    		  pp.evaluator_options.evaluation_callback = pp.options.evaluation_callback;
    		  pp.evaluator = ev.Create(pp.evaluator_options,
    		                                        pp.reduced_program,
    		                                        pp.error);
    		  return (pp.evaluator != null);
    		}


    } // class LineSearchPreProcessor
    
 // A PreprocessedProblem is the result of running the Preprocessor on
 // a Problem and Solver::Options object.
 class PreprocessedProblem {
	  String error[];
	  Solver.Options options;
	  LinearSolver.Options linear_solver_options;
	  Evaluator.Options evaluator_options;
	  Minimizer.Options minimizer_options;

	  ProblemImpl problem;
	  //scoped_ptr<ProblemImpl> gradient_checking_problem;
	  ProblemImpl gradient_checking_problem;
	  //scoped_ptr<Program> reduced_program;
	  Program reduced_program;
	  //scoped_ptr<LinearSolver> linear_solver;
	  //LinearSolver linear_solver;
	  //scoped_ptr<IterationCallback> logging_callback;
	  IterationCallback logging_callback;
	  //scoped_ptr<IterationCallback> state_updating_callback;
	  IterationCallback state_updating_callback;

	  //shared_ptr<Evaluator> evaluator;
	  Evaluator evaluator;
	  //shared_ptr<CoordinateDescentMinimizer> inner_iteration_minimizer;
	  CoordinateDescentMinimizer inner_iteration_minimizer;

	  Vector<double[]> removed_parameter_blocks;
	  Vector<double[]> reduced_parameters;
	 double fixed_cost[];
     public PreprocessedProblem() {
    	 fixed_cost = new double[] {0.0};
    	 error = new String[1];
    	 removed_parameter_blocks = new Vector<double[]>();
    	 reduced_parameters = new Vector<double[]>();
    	 LinearSolver ls = new LinearSolver();
    	 linear_solver_options = ls.options;
     }
     
     
 } // class Preprocessed
 
 public void SetupCommonMinimizerOptions(PreprocessedProblem pp) {
	  Solver.Options options = pp.options;
	  Program program = pp.reduced_program;

	  // Assuming that the parameter blocks in the program have been
	  // reordered as needed, extract them into a contiguous vector.
	  while (pp.reduced_parameters.size() > program.NumParameters()) {
		  pp.reduced_parameters.removeElementAt(pp.reduced_parameters.size()-1);
	  }
	  //pp.reduced_parameters.resize(program.NumParameters());
	  Vector<double[]> reduced_parameters = pp.reduced_parameters;
	  program.ParameterBlocksToStateVector(reduced_parameters);

	  Minimizer.Options minimizer_options = pp.minimizer_options;
	  Minimizer min = new Minimizer(options);
	  minimizer_options = min.options_;
	  minimizer_options.evaluator = pp.evaluator;

	  if (options.logging_type != LoggingType.SILENT) {
	    pp.logging_callback =
	        new LoggingCallback(options.minimizer_type,
	                            options.minimizer_progress_to_stdout);
	    minimizer_options.callbacks.add(0,pp.logging_callback);
	  }

	  if (options.update_state_every_iteration) {
	    pp.state_updating_callback = 
	      new StateUpdatingCallback(program, reduced_parameters);
	    // This must get pushed to the front of the callbacks so that it
	    // is run before any of the user callbacks.
	    minimizer_options.callbacks.add(0, pp.state_updating_callback);
	  }
	}
 
    class LinearSolver {
    	public Options options;
    	public LinearSolver() {
    		options = new Options();
    	}
    	
    	class Options {
    		public LinearSolverType type;
    	    public PreconditionerType preconditioner_type;
    	    public VisibilityClusteringType visibility_clustering_type;
    	    public DenseLinearAlgebraLibraryType dense_linear_algebra_library_type;
    	    public SparseLinearAlgebraLibraryType sparse_linear_algebra_library_type;

    	    // See solver.h for information about these flags.
    	    public boolean use_postordering;
    	    public boolean dynamic_sparsity;
    	    public boolean use_explicit_schur_complement;

    	    // Number of internal iterations that the solver uses. This
    	    // parameter only makes sense for iterative solvers like CG.
    	    public int min_num_iterations;
    	    public int max_num_iterations;

    	    // If possible, how many threads can the solver use.
    	    public int num_threads;

    	    // Hints about the order in which the parameter blocks should be
    	    // eliminated by the linear solver.
    	    //
    	    // For example if elimination_groups is a vector of size k, then
    	    // the linear solver is informed that it should eliminate the
    	    // parameter blocks 0 ... elimination_groups[0] - 1 first, and
    	    // then elimination_groups[0] ... elimination_groups[1] - 1 and so
    	    // on. Within each elimination group, the linear solver is free to
    	    // choose how the parameter blocks are ordered. Different linear
    	    // solvers have differing requirements on elimination_groups.
    	    //
    	    // The most common use is for Schur type solvers, where there
    	    // should be at least two elimination groups and the first
    	    // elimination group must form an independent set in the normal
    	    // equations. The first elimination group corresponds to the
    	    // num_eliminate_blocks in the Schur type solvers.
    	    public Vector<Integer> elimination_groups;

    	    // Iterative solvers, e.g. Preconditioned Conjugate Gradients
    	    // maintain a cheap estimate of the residual which may become
    	    // inaccurate over time. Thus for non-zero values of this
    	    // parameter, the solver can be told to recalculate the value of
    	    // the residual using a |b - Ax| evaluation.
    	    public int residual_reset_period;

    	    // If the block sizes in a BlockSparseMatrix are fixed, then in
    	    // some cases the Schur complement based solvers can detect and
    	    // specialize on them.
    	    //
    	    // It is expected that these parameters are set programmatically
    	    // rather than manually.
    	    //
    	    // Please see schur_complement_solver.h and schur_eliminator.h for
    	    // more details.
    	    public int row_block_size;
    	    public int e_block_size;
    	    public int f_block_size;

    	    public ContextImpl context;
    	    
    	    public Options() {
    	    	type = LinearSolverType.SPARSE_NORMAL_CHOLESKY;
    	        preconditioner_type = PreconditionerType.JACOBI;
    	        visibility_clustering_type = VisibilityClusteringType.CANONICAL_VIEWS;
    	        dense_linear_algebra_library_type = DenseLinearAlgebraLibraryType.EIGEN;
    	        sparse_linear_algebra_library_type = SparseLinearAlgebraLibraryType.SUITE_SPARSE;
    	        use_postordering = false;
    	        dynamic_sparsity = false;
    	        use_explicit_schur_complement = false;
    	        min_num_iterations = 1;
    	        max_num_iterations = 1;
    	        num_threads = 1;
    	        residual_reset_period = 10;
    	        //row_block_size(Eigen::Dynamic),
    	        //e_block_size(Eigen::Dynamic),
    	        //f_block_size(Eigen::Dynamic),
    	        context = null;
    	    }
    	} // class Options
    	
    } // class LinearSolver
    
    public LinearSolverType LinearSolverForZeroEBlocks(LinearSolverType linear_solver_type) {
		  if (!IsSchurType(linear_solver_type)) {
		    return linear_solver_type;
		  }

		  if (linear_solver_type == LinearSolverType.SPARSE_SCHUR) {
		    return LinearSolverType.SPARSE_NORMAL_CHOLESKY;
		  }

		  if (linear_solver_type == LinearSolverType.DENSE_SCHUR) {
		    // TODO(sameeragarwal): This is probably not a great choice.
		    // Ideally, we should have a DENSE_NORMAL_CHOLESKY, that can take
		    // a BlockSparseMatrix as input.
		    return LinearSolverType.DENSE_QR;
		  }

		  if (linear_solver_type == LinearSolverType.ITERATIVE_SCHUR) {
		    return LinearSolverType.CGNR;
		  }

		  return linear_solver_type;
		}


 
     class Evaluator {
    	 public Options options;
    	 public Evaluator() {
    	     options = new Options();	 
    	 }
    	 
    	 class Options {
    		public int num_threads;
 		    public int num_eliminate_blocks;
 		    public LinearSolverType linear_solver_type;
 		    public boolean dynamic_sparsity;
 		    public ContextImpl context;
 		    public EvaluationCallback evaluation_callback;
    		    public Options() {
    		        num_threads = 1;
    		        num_eliminate_blocks = -1;
    		        linear_solver_type = LinearSolverType.DENSE_QR;
    		        dynamic_sparsity = false;
    		        context = null;
    		        evaluation_callback = null;
    		    }

    		    
    		} // class Options
    	 
    	  // Options struct to control Evaluator::Evaluate;
    	  class EvaluateOptions {
    		// If false, the loss function correction is not applied to the
      	    // residual blocks.
      	    public boolean apply_loss_function;

      	    // If false, this evaluation point is the same as the last one.
      	    public boolean new_evaluation_point;
    	    public EvaluateOptions() {
    	        apply_loss_function = true;
    	        new_evaluation_point = true;
    	    }

    	    
    	  } // class EvaluateOptions
    	  
    	  public Evaluator Create(Evaluator.Options options,
                  Program program,
                  String error[]) {
    		if (options.context == null) {
    			System.err.println("options.context == null in public Evaluator Create");
    			return null;
    		}
			
			switch (options.linear_solver_type) {
			case DENSE_QR:
			case DENSE_NORMAL_CHOLESKY:
			DenseJacobianWriter dw = new DenseJacobianWriter(options, program);
			return new ProgramEvaluator<ScratchEvaluatePreparer,
			                       DenseJacobianWriter, NullJacobianFinalizer>(dw, options,
			                                            program);
			case DENSE_SCHUR:
			case SPARSE_SCHUR:
			case ITERATIVE_SCHUR:
			case CGNR:
			BlockJacobianWriter bw = new BlockJacobianWriter(options, program);
			return new ProgramEvaluator<BlockEvaluatePreparer,
			                       BlockJacobianWriter, NullJacobianFinalizer>(bw, options,
			                                            program);
			case SPARSE_NORMAL_CHOLESKY:
			if (options.dynamic_sparsity) {
			DynamicCompressedRowJacobianWriter dc = new DynamicCompressedRowJacobianWriter(options, program);
			return new ProgramEvaluator<ScratchEvaluatePreparer,
			                         DynamicCompressedRowJacobianWriter,
			                         DynamicCompressedRowJacobianFinalizer>(
			                             dc, options, program);
			} else {
			BlockJacobianWriter bw2 = new BlockJacobianWriter(options, program);
			return new ProgramEvaluator<BlockEvaluatePreparer,
			                         BlockJacobianWriter, NullJacobianFinalizer>(bw2, options,
			                                              program);
			}
			
			default:
			error[0] = "Invalid Linear Solver Type. Unable to create evaluator.";
			return null;
			}
			}

     } // class Evaluator
     
     class DynamicCompressedRowJacobianFinalizer {
    	  /*void operator()(SparseMatrix* base_jacobian, int num_parameters) {
    	    DynamicCompressedRowSparseMatrix* jacobian =
    	      down_cast<DynamicCompressedRowSparseMatrix*>(base_jacobian);
    	    jacobian->Finalize(num_parameters);
    	  }*/
    	};
     
     class NullJacobianFinalizer {
    	 public NullJacobianFinalizer() {
    		 
    	 }
    	  //void operator()(SparseMatrix* jacobian, int num_parameters) {}
     };
     
     class ScratchEvaluatePreparer {
    	// Scratch space for the jacobians; each jacobian is packed one after another.
    	// There is enough scratch to hold all the jacobians for the largest residual.
    	//scoped_array<double> jacobian_scratch_;
    	private double jacobian_scratch_[];
    	public ScratchEvaluatePreparer() {
    		
    	}
    	
    	public void Init(int max_derivatives_per_residual_block) {
    		  jacobian_scratch_ =  new double[max_derivatives_per_residual_block];
    	}


     }
     
     class BlockEvaluatePreparer {
    	 private int jacobian_layout_[];

   	     // For the case that the overall jacobian is not available, but the
   	     // individual jacobians are requested, use a pass-through scratch evaluate
   	     // preparer.
   	     private ScratchEvaluatePreparer scratch_evaluate_preparer_;
    	 public BlockEvaluatePreparer() {
    		 
    	 }
    	 
    	 public void Init(int jacobian_layout[], int max_derivatives_per_residual_block) {
           jacobian_layout_ = jacobian_layout;
           scratch_evaluate_preparer_.Init(max_derivatives_per_residual_block);
    	 }
    	  
     }
     
     class DenseJacobianWriter {
    	 private Program program_;
    	 public DenseJacobianWriter(Evaluator.Options options/* ignored */,
    		                      Program program) {
    		    program_ = program;
         }
    	 
    	  // Since the dense matrix has different layout than that assumed by the cost
    	  // functions, use scratch space to store the jacobians temporarily then copy
    	  // them over to the larger jacobian later.
    	  public ScratchEvaluatePreparer[] CreateEvaluatePreparers(int num_threads) {
    	    return Create(program_, num_threads);
    	  }

     } // class DenseJacobianWriter
     
     class DynamicCompressedRowJacobianWriter {
    	 private Program program_;
    	 public DynamicCompressedRowJacobianWriter(Evaluator.Options options/* ignored */,
    		                      Program program) {
    		    program_ = program;
         }
    	 
    	  // Since the dense matrix has different layout than that assumed by the cost
    	  // functions, use scratch space to store the jacobians temporarily then copy
    	  // them over to the larger jacobian later.
    	  public ScratchEvaluatePreparer[] CreateEvaluatePreparers(int num_threads) {
    	    return Create(program_, num_threads);
    	  }

     } // class DynamicCompressedRowJacobianWriter
     
     class BlockJacobianWriter {
    	 private Program program_;
    	 private Vector<int[]> jacobian_layout_;

    	  // The pointers in jacobian_layout_ point directly into this vector.
    	  private Vector<Integer> jacobian_layout_storage_;
    	  public BlockJacobianWriter(Evaluator.Options options, Program program) {
    		  program_ = program;
              if (options.num_eliminate_blocks < 0) {
                   System.err.println("num_eliminate_blocks must be >= 0 in public BlockJacobian.");
                   return;
              }
			  BuildJacobianLayout(program, options.num_eliminate_blocks, jacobian_layout_, jacobian_layout_storage_);
          }
    	  
    	  public void BuildJacobianLayout(Program program,
                  int num_eliminate_blocks,
                  Vector<int[]> jacobian_layout,
                  Vector<Integer> jacobian_layout_storage) {
			      Vector<ResidualBlock> residual_blocks = program.residual_blocks();
			
			// Iterate over all the active residual blocks and determine how many E blocks
			// are there. This will determine where the F blocks start in the jacobian
			// matrix. Also compute the number of jacobian blocks.
			int f_block_pos = 0;
			int num_jacobian_blocks = 0;
			for (int i = 0; i < residual_blocks.size(); ++i) {
			ResidualBlock residual_block = residual_blocks.get(i);
			int num_residuals = residual_block.NumResiduals();
			int num_parameter_blocks = residual_block.NumParameterBlocks();
			
			// Advance f_block_pos over each E block for this residual.
			for (int j = 0; j < num_parameter_blocks; ++j) {
			ParameterBlock parameter_block = residual_block.parameter_blocks()[j];
			if (!parameter_block.IsConstant()) {
			 // Only count blocks for active parameters.
			 num_jacobian_blocks++;
			 if (parameter_block.index() < num_eliminate_blocks) {
			   f_block_pos += num_residuals * parameter_block.LocalSize();
			 }
			}
			}
			}
			
			// We now know that the E blocks are laid out starting at zero, and the F
			// blocks are laid out starting at f_block_pos. Iterate over the residual
			// blocks again, and this time fill the jacobian_layout array with the
			// position information.
			
			while (jacobian_layout.size() > program.NumResidualBlocks()) {
				jacobian_layout.removeElementAt(jacobian_layout.size()-1);
			}
			//jacobian_layout->resize(program.NumResidualBlocks());
			while (jacobian_layout_storage.size() > num_jacobian_blocks) {
				jacobian_layout_storage.removeElementAt(jacobian_layout_storage.size()-1);
			}
			while (jacobian_layout_storage.size() < num_jacobian_blocks) {
				jacobian_layout_storage.add(0);
			}
			//jacobian_layout_storage->resize(num_jacobian_blocks);
			
			int e_block_pos = 0;
			for (int i = 0; i < residual_blocks.size(); ++i) {
			ResidualBlock residual_block = residual_blocks.get(i);
			int num_residuals = residual_block.NumResiduals();
			int num_parameter_blocks = residual_block.NumParameterBlocks();
			
			for (int j = 0, k = 0; j < num_parameter_blocks; ++j) {
			ParameterBlock parameter_block = residual_block.parameter_blocks()[j];
			int parameter_block_index = parameter_block.index();
			if (parameter_block.IsConstant()) {
			 continue;
			}
			int jacobian_block_size =
			   num_residuals * parameter_block.LocalSize();
			if (parameter_block_index < num_eliminate_blocks) {
			 jacobian_layout.get(i)[k] = e_block_pos;
			 e_block_pos += jacobian_block_size;
			} else {
			 jacobian_layout.get(i)[k] = f_block_pos;
			 f_block_pos += jacobian_block_size;
			}
			k++;
			}
			}
			}

    	  
    	// Create evaluate prepareres that point directly into the final jacobian. This
    	// makes the final Write() a nop.
    	public BlockEvaluatePreparer[] CreateEvaluatePreparers(int num_threads) {
    	  int max_derivatives_per_residual_block = program_.MaxDerivativesPerResidualBlock();

    	  BlockEvaluatePreparer preparers[] = new BlockEvaluatePreparer[num_threads];
    	  for (int i = 0; i < num_threads; i++) {
    		preparers[i] = new BlockEvaluatePreparer();
    	    preparers[i].Init(jacobian_layout_.get(0), max_derivatives_per_residual_block);
    	  }
    	  return preparers;
    	}


     }
     
     public ScratchEvaluatePreparer[] Create(Program program, int num_threads) {
    		  ScratchEvaluatePreparer preparers[] = new ScratchEvaluatePreparer[num_threads];
    		  int max_derivatives_per_residual_block =
    		      program.MaxDerivativesPerResidualBlock();
    		  for (int i = 0; i < num_threads; i++) {
    			preparers[i] = new ScratchEvaluatePreparer();
    		    preparers[i].Init(max_derivatives_per_residual_block);
    		  }
    		  return preparers;
    }
     
     class CallStatistics {
    	 public double time;
   	     public int calls;
    	  public CallStatistics() {
    		  time = 0;
    		  calls = 0;
    	  }
    	  
    	};

    	// Struct used by various objects to report statistics about their
    	// execution.
    	class ExecutionSummary {
    		private Lock mutex_;
    	    private HashMap<String, CallStatistics> statistics_;
    	  public void IncrementTimeBy(String name, double value) {
    	    mutex_.lock();
    	    CallStatistics call_stats = statistics_.get(name);
    	    call_stats.time += value;
    	    ++call_stats.calls;
    	    mutex_.unlock();
    	  }

    	  public Map<String, CallStatistics> statistics() {
    	    return statistics_;
    	  }

    	 
    	};

     
     /*template<typename EvaluatePreparer,
     typename JacobianWriter,
     typename JacobianFinalizer = NullJacobianFinalizer>*/
		class ProgramEvaluator<EvaluatePreparer, JacobianWriter, JacobianFinalizer> extends Evaluator {
			
			private Evaluator.Options options_;
			private Program program_;
			private JacobianWriter jacobian_writer_;
			//scoped_array<EvaluatePreparer> evaluate_preparers_;
			private EvaluatePreparer evaluate_preparers_[];
			//scoped_array<EvaluateScratch> evaluate_scratch_;
			private EvaluateScratch evaluate_scratch_[];
			private Vector<Integer> residual_layout_;
			private ExecutionSummary execution_summary_;
		    public ProgramEvaluator(JacobianWriter jw, Evaluator.Options options, Program program) {
		       super();
		       options_ = options;
		       program_ = program;
		       jacobian_writer_ = jw;
		       evaluate_scratch_ = CreateEvaluatorScratch(program, options.num_threads);
				/*#ifdef CERES_NO_THREADS
				if (options_.num_threads > 1) {
				  LOG(WARNING)
				      << "Neither OpenMP nor TBB support is compiled into this binary; "
				      << "only options.num_threads = 1 is supported. Switching "
				      << "to single threaded mode.";
				  options_.num_threads = 1;
				}
				#endif // CERES_NO_THREADS*/
		
		        residual_layout_ = new Vector<Integer>();
		        BuildResidualLayout(program, residual_layout_);
		        evaluate_scratch_ = CreateEvaluatorScratch(program, options.num_threads);
		}
		    
		    
		} // class ProgramEvaluator
		
		class EvaluateScratch {
			private double cost;
		    //scoped_array<double> residual_block_evaluate_scratch;
			private double residual_block_evaluate_scratch[];
		    // The gradient in the local parameterization.
		    //scoped_array<double> gradient;
			private double gradient[];
		    // Enough space to store the residual for the largest residual block.
		    //scoped_array<double> residual_block_residuals;
			private double residual_block_residuals[];
		    //scoped_array<double*> jacobian_block_ptrs;
			private double jacobian_block_ptrs[][];
			public EvaluateScratch() {
				
			}
		    public void Init(int max_parameters_per_residual_block,
		              int max_scratch_doubles_needed_for_evaluate,
		              int max_residuals_per_residual_block,
		              int num_parameters) {
		      residual_block_evaluate_scratch = new double[max_scratch_doubles_needed_for_evaluate];
		      gradient = new double[num_parameters];
		      residual_block_residuals = new double[max_residuals_per_residual_block];
		      jacobian_block_ptrs = new double[max_parameters_per_residual_block][];
		    }

		    
		  };
	    
	 // Create scratch space for each thread evaluating the program.
	       public EvaluateScratch[] CreateEvaluatorScratch(Program program, int num_threads) {
	int max_parameters_per_residual_block = program.MaxParametersPerResidualBlock();
	int max_scratch_doubles_needed_for_evaluate = program.MaxScratchDoublesNeededForEvaluate();
	int max_residuals_per_residual_block = program.MaxResidualsPerResidualBlock();
	int num_parameters = program.NumEffectiveParameters();

	EvaluateScratch evaluate_scratch[] = new EvaluateScratch[num_threads];
	for (int i = 0; i < num_threads; i++) {
	evaluate_scratch[i].Init(max_parameters_per_residual_block,
	max_scratch_doubles_needed_for_evaluate,
	max_residuals_per_residual_block,
	num_parameters);
	}
	return evaluate_scratch;
	}
	    
		
		public void BuildResidualLayout(Program program, Vector<Integer> residual_layout) {
			Vector<ResidualBlock> residual_blocks = program.residual_blocks();
			while (residual_layout.size() > program.NumResidualBlocks()) {
				residual_layout.removeElementAt(residual_layout.size()-1);
			}
			while (residual_layout.size() < program.NumResidualBlocks()) {
				residual_layout.add(0);
			}
			int residual_pos = 0;
			for (int i = 0; i < residual_blocks.size(); ++i) {
			    int num_residuals = residual_blocks.get(i).NumResiduals();
			    residual_layout.set(i,residual_pos);
			    residual_pos += num_residuals;
			}
       }
		
       
     
  // Callback for logging the state of the minimizer to STDERR or
  // STDOUT depending on the user's preferences and logging level.
  class LoggingCallback extends IterationCallback {
      private MinimizerType minimizer_type;
	  private boolean log_to_stdout_;
   public LoggingCallback(MinimizerType minimizer_type, boolean log_to_stdout) {
	   this.minimizer_type = minimizer_type;
	   log_to_stdout_ = log_to_stdout;   
   }
    //virtual CallbackReturnType operator()(const IterationSummary& summary);
  };
  
	//Callback for updating the externally visible state of parameter
	//blocks.
	class StateUpdatingCallback extends IterationCallback {
		private Program program_;
		private Vector<double[]> parameters_;
	    public StateUpdatingCallback(Program program, Vector<double[]> parameters) {
	        program_ = program;
	        parameters_ = parameters;
	    }
	
	    // virtual CallbackReturnType operator()(const IterationSummary& summary);
	};
	
	class CoordinateDescentMinimizer extends Minimizer {
		Vector<ParameterBlock> parameter_blocks_;
		Vector<Vector<ResidualBlock> > residual_blocks_;
		  // The optimization is performed in rounds. In each round all the
		  // parameter blocks that form one independent set are optimized in
		  // parallel. This array, marks the boundaries of the independent
		  // sets in parameter_blocks_.
		  Vector<Integer> independent_set_offsets_;

		  Evaluator.Options evaluator_options_;

		  ContextImpl context_;  
		  
		  public CoordinateDescentMinimizer(ContextImpl context) {
			super();
			if (context == null) {
				System.err.println("ContextImpl context == null in public CoordinateDescentMinimizer");
				return;
			}
		    context_ = context; 
		  }

	} // class CoordinateDescentMinimizer
     
     // Interface for non-linear least squares solvers.
     class Minimizer {
      public Options options_;
      public Minimizer() {
    	  options_ = new Options();
      }
      
      public Minimizer(Solver.Options options) {
    	  options_ = new Options(options);
      }
       // Options struct to control the behaviour of the Minimizer. Please
       // see solver.h for detailed information about the meaning and
       // default values of each of these parameters.
       class Options {
    	   public int max_num_iterations;
           public double max_solver_time_in_seconds;
           public int num_threads;

           // Number of times the linear solver should be retried in case of
           // numerical failure. The retries are done by exponentially scaling up
           // mu at each retry. This leads to stronger and stronger
           // regularization making the linear least squares problem better
           // conditioned at each retry.
           public int max_step_solver_retries;
           public double gradient_tolerance;
           public double parameter_tolerance;
           public double function_tolerance;
           public double min_relative_decrease;
           public double eta;
           public boolean jacobi_scaling;
           public boolean use_nonmonotonic_steps;
           public int max_consecutive_nonmonotonic_steps;
           public Vector<Integer> trust_region_minimizer_iterations_to_dump;
           public DumpFormatType trust_region_problem_dump_format_type;
           public String trust_region_problem_dump_directory;
           public int max_num_consecutive_invalid_steps;
           public double min_trust_region_radius;
           public LineSearchDirectionType line_search_direction_type;
           public LineSearchType line_search_type;
           public NonlinearConjugateGradientType nonlinear_conjugate_gradient_type;
           public int max_lbfgs_rank;
           public boolean use_approximate_eigenvalue_bfgs_scaling;
           public LineSearchInterpolationType line_search_interpolation_type;
           public double min_line_search_step_size;
           public double line_search_sufficient_function_decrease;
           public double max_line_search_step_contraction;
           public double min_line_search_step_contraction;
           public int max_num_line_search_step_size_iterations;
           public int max_num_line_search_direction_restarts;
           public double line_search_sufficient_curvature_decrease;
           public double max_line_search_step_expansion;
           public double inner_iteration_tolerance;

           // If true, then all logging is disabled.
           public boolean is_silent;

           // Use a bounds constrained optimization algorithm.
           public boolean is_constrained;

           // List of callbacks that are executed by the Minimizer at the end
           // of each iteration.
           //
           // The Options struct does not own these pointers.
           Vector<IterationCallback> callbacks;

           // Object responsible for evaluating the cost, residuals and
           // Jacobian matrix.
           //shared_ptr<Evaluator> evaluator;
           Evaluator evaluator;

           // Object responsible for actually computing the trust region
           // step, and sizing the trust region radius.
           //shared_ptr<TrustRegionStrategy> trust_region_strategy;
           //TrustRegionStrategy trust_region_strategy;

           // Object holding the Jacobian matrix. It is assumed that the
           // sparsity structure of the matrix has already been initialized
           // and will remain constant for the life time of the
           // optimization.
           //shared_ptr<SparseMatrix> jacobian;
           //SparseMatrix jacobian;

           //shared_ptr<CoordinateDescentMinimizer> inner_iteration_minimizer;
           CoordinateDescentMinimizer inner_iteration_minimizer;
         public Options() {
           Solver solver = new Solver();
           Init(solver.options);
         }

         public Options(Solver.Options options) {
           Init(options);
         }

         public void Init(Solver.Options options) {
           num_threads = options.num_threads;
           max_num_iterations = options.max_num_iterations;
           max_solver_time_in_seconds = options.max_solver_time_in_seconds;
           max_step_solver_retries = 5;
           gradient_tolerance = options.gradient_tolerance;
           parameter_tolerance = options.parameter_tolerance;
           function_tolerance = options.function_tolerance;
           min_relative_decrease = options.min_relative_decrease;
           eta = options.eta;
           jacobi_scaling = options.jacobi_scaling;
           use_nonmonotonic_steps = options.use_nonmonotonic_steps;
           max_consecutive_nonmonotonic_steps =
               options.max_consecutive_nonmonotonic_steps;
           trust_region_problem_dump_directory =
               options.trust_region_problem_dump_directory;
           trust_region_minimizer_iterations_to_dump =
               options.trust_region_minimizer_iterations_to_dump;
           trust_region_problem_dump_format_type =
               options.trust_region_problem_dump_format_type;
           max_num_consecutive_invalid_steps =
               options.max_num_consecutive_invalid_steps;
           min_trust_region_radius = options.min_trust_region_radius;
           line_search_direction_type = options.line_search_direction_type;
           line_search_type = options.line_search_type;
           nonlinear_conjugate_gradient_type =
               options.nonlinear_conjugate_gradient_type;
           max_lbfgs_rank = options.max_lbfgs_rank;
           use_approximate_eigenvalue_bfgs_scaling =
               options.use_approximate_eigenvalue_bfgs_scaling;
           line_search_interpolation_type =
               options.line_search_interpolation_type;
           min_line_search_step_size = options.min_line_search_step_size;
           line_search_sufficient_function_decrease =
               options.line_search_sufficient_function_decrease;
           max_line_search_step_contraction =
               options.max_line_search_step_contraction;
           min_line_search_step_contraction =
               options.min_line_search_step_contraction;
           max_num_line_search_step_size_iterations =
               options.max_num_line_search_step_size_iterations;
           max_num_line_search_direction_restarts =
               options.max_num_line_search_direction_restarts;
           line_search_sufficient_curvature_decrease =
               options.line_search_sufficient_curvature_decrease;
           max_line_search_step_expansion =
               options.max_line_search_step_expansion;
           inner_iteration_tolerance = options.inner_iteration_tolerance;
           is_silent = (options.logging_type == LoggingType.SILENT);
           is_constrained = false;
           callbacks = options.callbacks;
         }

         
       };

      
     };
    
    private ProblemImpl CreateGradientCheckingProblemImpl(
    	    ProblemImpl problem_impl,
    	    double relative_step_size,
    	    double relative_precision,
    	    GradientCheckingIterationCallback callback) {
    	if (callback == null) {
    		System.err.println("in CreateGradientCheckingProblemImpl callback is null");
    		return null;
    	}
    	// We create new CostFunctions by wrapping the original CostFunction
    	// in a gradient checking CostFunction. So its okay for the
    	// ProblemImpl to take ownership of it and destroy it. The
    	// LossFunctions and LocalParameterizations are reused and since
    	// they are owned by problem_impl, gradient_checking_problem_impl
    	// should not take ownership of it.
    	ProblemImpl prob = new ProblemImpl();
    	ProblemImpl.Options gradient_checking_problem_options = prob.options_;
    	gradient_checking_problem_options.cost_function_ownership = Ownership.TAKE_OWNERSHIP;
    	gradient_checking_problem_options.loss_function_ownership =
    	      Ownership.DO_NOT_TAKE_OWNERSHIP;
    	gradient_checking_problem_options.local_parameterization_ownership =
    	      Ownership.DO_NOT_TAKE_OWNERSHIP;
    	gradient_checking_problem_options.context = problem_impl.context();
    	
    	NumericDiffOptions numeric_diff_options = new NumericDiffOptions();
    	numeric_diff_options.relative_step_size = relative_step_size;
    	
    	ProblemImpl gradient_checking_problem_impl = new ProblemImpl(gradient_checking_problem_options);

        Program program = problem_impl.mutable_program();

        // For every ParameterBlock in problem_impl, create a new parameter
        // block with the same local parameterization and constancy.
    	Vector<ParameterBlock> parameter_blocks = program.parameter_blocks();
		  for (int i = 0; i < parameter_blocks.size(); ++i) {
		    ParameterBlock parameter_block = parameter_blocks.get(i);
		    gradient_checking_problem_impl.AddParameterBlock(
		        parameter_block.mutable_user_state(),
		        parameter_block.Size(),
		        parameter_block.mutable_local_parameterization());

		    if (parameter_block.IsConstant()) {
		      gradient_checking_problem_impl.SetParameterBlockConstant(
		          parameter_block.mutable_user_state());
		    }
		  }
		  // For every ResidualBlock in problem_impl, create a new
		  // ResidualBlock by wrapping its CostFunction inside a
		  // GradientCheckingCostFunction.
		  Vector<ResidualBlock> residual_blocks = program.residual_blocks();
		  for (int i = 0; i < residual_blocks.size(); ++i) {
		    ResidualBlock residual_block = residual_blocks.get(i);

		    // Build a human readable string which identifies the
		    // ResidualBlock. This is used by the GradientCheckingCostFunction
		    // when logging debugging information.
		    String extra_info = "Residual block id " + i ; // + "; depends on parameters [";
		    Vector<double[]> parameter_blocks2 = new Vector<double[]>();
		    Vector<LocalParameterization> local_parameterizations = new Vector<LocalParameterization>();
		    parameter_blocks2.ensureCapacity(residual_block.NumParameterBlocks());
		    local_parameterizations.ensureCapacity(residual_block.NumParameterBlocks());
		    for (int j = 0; j < residual_block.NumParameterBlocks(); ++j) {
		      ParameterBlock parameter_block = residual_block.parameter_blocks()[j];
		      parameter_blocks2.add(parameter_block.mutable_user_state());
		      //StringAppendF(&extra_info, "%p", parameter_block.mutable_user_state());
		      //extra_info += (j < residual_block->NumParameterBlocks() - 1) ? ", " : "]";
		      local_parameterizations.add(problem_impl.GetParameterization(
		          parameter_block.mutable_user_state()));
		    }

		    // Wrap the original CostFunction in a GradientCheckingCostFunction.
		    CostFunction gradient_checking_cost_function =
		        new GradientCheckingCostFunction(residual_block.cost_function(),
		                                         local_parameterizations,
		                                         numeric_diff_options,
		                                         relative_precision,
		                                         extra_info,
		                                         callback);

		    // The const_cast is necessary because
		    // ProblemImpl::AddResidualBlock can potentially take ownership of
		    // the LossFunction, but in this case we are guaranteed that this
		    // will not be the case, so this const_cast is harmless.
		    gradient_checking_problem_impl.AddResidualBlock(
		        gradient_checking_cost_function,
		        //const_cast<LossFunction*>(residual_block->loss_function()),
		        residual_block.loss_function(),
		        parameter_blocks2);		  }
		  // Normally, when a problem is given to the solver, we guarantee
		  // that the state pointers for each parameter block point to the
		  // user provided data. Since we are creating this new problem from a
		  // problem given to us at an arbitrary stage of the solve, we cannot
		  // depend on this being the case, so we explicitly call
		  // SetParameterBlockStatePtrsToUserStatePtrs to ensure that this is
		  // the case.
		  gradient_checking_problem_impl
		      .mutable_program()
		      .SetParameterBlockStatePtrsToUserStatePtrs();

		  return gradient_checking_problem_impl;
    }

    
    private void PreSolveSummarize(Solver.Options options, ProblemImpl problem, Solver.Summary summary) {
    	SummarizeGivenProgram(problem.program(), summary);
    	OrderingToGroupSizes(options.linear_solver_ordering,
                summary.linear_solver_ordering_given);
    	OrderingToGroupSizes(options.inner_iteration_ordering,
                summary.inner_iteration_ordering_given);
    	summary.dense_linear_algebra_library_type  = options.dense_linear_algebra_library_type; 
    	summary.dogleg_type                        = options.dogleg_type;
        summary.inner_iteration_time_in_seconds    = 0.0;
    	summary.num_line_search_steps              = 0;
    	summary.line_search_cost_evaluation_time_in_seconds = 0.0;
        summary.line_search_gradient_evaluation_time_in_seconds = 0.0;
    	summary.line_search_polynomial_minimization_time_in_seconds = 0.0;
    	summary.line_search_total_time_in_seconds  = 0.0;
    	summary.inner_iterations_given             = options.use_inner_iterations;
    	summary.line_search_direction_type         = options.line_search_direction_type;  
    	summary.line_search_interpolation_type     = options.line_search_interpolation_type; 
    	summary.line_search_type                   = options.line_search_type;
    	summary.linear_solver_type_given           = options.linear_solver_type;
    	summary.max_lbfgs_rank                     = options.max_lbfgs_rank;
    	summary.minimizer_type                     = options.minimizer_type;
    	summary.nonlinear_conjugate_gradient_type  = options.nonlinear_conjugate_gradient_type;
    	summary.num_linear_solver_threads_given    = options.num_threads;
    	summary.num_threads_given                  = options.num_threads;
    	summary.preconditioner_type_given          = options.preconditioner_type;
    	summary.sparse_linear_algebra_library_type = options.sparse_linear_algebra_library_type; 
    	summary.trust_region_strategy_type         = options.trust_region_strategy_type;   
    	summary.visibility_clustering_type         = options.visibility_clustering_type; 
    }
    
    private void OrderingToGroupSizes(OrderedGroups<double []> ordering, Vector<Integer> group_sizes) {
    	if (group_sizes == null) {
    		System.err.println("Vector<Integer> group_sizes is null in OrderingToGroupSizes");
    		return;
    	}
    	group_sizes.clear();
		if (ordering == null) {
		    return;
		}
		
		HashMap<Integer, Set<double[]> > group_to_elements = ordering.group_to_elements();
		Set<Integer> keySet = group_to_elements.keySet();
		Iterator<Integer> iter = keySet.iterator();
		while (iter.hasNext()) {
			Set<double[]>values = (Set<double[]>)group_to_elements.get(iter.next());
			if (values == null) {
				group_sizes.add(0);
			}
			else {
				group_sizes.add(values.size());
			}
		}
    }

    
    private void SummarizeGivenProgram(Program program, Solver.Summary summary) {
			summary.num_parameter_blocks     = program.NumParameterBlocks();
			summary.num_parameters           = program.NumParameters();
			summary.num_effective_parameters = program.NumEffectiveParameters();
			summary.num_residual_blocks      = program.NumResidualBlocks();
			summary.num_residuals            = program.NumResiduals();
    }



	class CostFunction {
		//private int num_residuals_;
		private Vector<Integer>parameter_block_sizes_;
		public CostFunction() {
		    num_residuals_ = 0;	
		    parameter_block_sizes_ = new Vector<Integer>();
		}
		
		public Vector<Integer> parameter_block_sizes() {
		    return parameter_block_sizes_;
		}
		
		public int num_residuals() {
		    return num_residuals_;
		}
		
		protected Vector<Integer> mutable_parameter_block_sizes() {
		    return parameter_block_sizes_;
		}
		
		protected void set_num_residuals(int num_residuals) {
		    num_residuals_ = num_residuals;
		}
		
		protected void set_parameter_block_sizes(Vector<Integer>parameter_block_sizes) {
			parameter_block_sizes_ = parameter_block_sizes;
		}
		
		protected void AddParameterBlock(int size) {
		   parameter_block_sizes_.add(size);
		}

		protected void SetNumResiduals(int num_residuals) {
			num_residuals_ = num_residuals;
		}
		
		public boolean Evaluate(Vector<double[]> parameters,
                double residuals[],
                double jacobians[][]) {
		    return true;	
		}

	}
	
	class SizedCostFunction extends CostFunction {
		
	    public SizedCostFunction(int kNumResiduals, int N0, int N1, int N2, int N3, int N4, int N5, int N6, int N7, int N8, int N9,
	    		int num_residuals) {
	    	    super();
			    if (kNumResiduals != DYNAMIC) {
			       System.err.println("Cost functions must have at least one residual block.");
			       return;
			    }

			    if(!(((N1 == 0) && (N2 == 0) && (N3 == 0) && (N4 == 0) && (N5 == 0) && (N6 == 0) && (N7 == 0) && (N8 == 0) && (N9 == 0)) ||
			          ((N1 > 0) && (N2 == 0) && (N3 == 0) && (N4 == 0) && (N5 == 0) && (N6 == 0) && (N7 == 0) && (N8 == 0) && (N9 == 0)) ||
			          ((N1 > 0) && (N2 > 0) && (N3 == 0) && (N4 == 0) && (N5 == 0) && (N6 == 0) && (N7 == 0) && (N8 == 0) && (N9== 0)) ||         
			          ((N1 > 0) && (N2 > 0) && (N3 > 0) && (N4 == 0) && (N5 == 0) && (N6 == 0) && (N7 == 0) && (N8 == 0) && (N9 == 0)) ||                  
			          ((N1 > 0) && (N2 > 0) && (N3 > 0) && (N4 > 0) && (N5 == 0) && (N6 == 0) && (N7 == 0) && (N8 == 0) && (N9 == 0)) ||         
			          ((N1 > 0) && (N2 > 0) && (N3 > 0) && (N4 > 0) && (N5 > 0) && (N6 == 0) && (N7 == 0) && (N8 == 0) && (N9 == 0)) ||          
			          ((N1 > 0) && (N2 > 0) && (N3 > 0) && (N4 > 0) && (N5 > 0) && (N6 > 0) && (N7 == 0)&& (N8 == 0) && (N9 == 0)) ||      
			          ((N1 > 0) && (N2 > 0) && (N3 > 0) && (N4 > 0) && (N5 > 0) && (N6 > 0) && (N7 > 0) && (N8 == 0) && (N9 == 0)) ||        
			          ((N1 > 0) && (N2 > 0) && (N3 > 0) && (N4 > 0) && (N5 > 0) && (N6 > 0) && (N7 > 0) && (N8 > 0) && (N9 == 0)) ||   
			          ((N1 > 0) && (N2 > 0) && (N3 > 0) && (N4 > 0) && (N5 > 0) && (N6 > 0) && (N7 > 0) && (N8 > 0) && (N9 > 0)))) { 
			        System.err.println("Zero block cannot precede a non-zero block. Block sizes are ");
			        System.err.println ("(ignore trailing 0s): " + N0 + ", " + N1 + ", " + N2 + ", ");
			        System.err.println(N3 + ", " + N4 + ", " + N5 + ", " + N6 + ", " + N7 + ", ");
			        System.err.println(N8 + ", " + N9);
			        return;
			    }

			    set_num_residuals(num_residuals);
			    
			    if (N0 > 0) mutable_parameter_block_sizes().add(N0);
			    if (N1 > 0) mutable_parameter_block_sizes().add(N1);
			    if (N2 > 0) mutable_parameter_block_sizes().add(N2);
			    if (N3 > 0) mutable_parameter_block_sizes().add(N3);
			    if (N4 > 0) mutable_parameter_block_sizes().add(N4);
			    if (N5 > 0) mutable_parameter_block_sizes().add(N5);
			    if (N6 > 0) mutable_parameter_block_sizes().add(N6);
			    if (N7 > 0) mutable_parameter_block_sizes().add(N7);
			    if (N8 > 0) mutable_parameter_block_sizes().add(N8);
			    if (N9 > 0) mutable_parameter_block_sizes().add(N9);
	}
			    
			    public SizedCostFunction(int kNumResiduals, int N0, int N1, int N2, int N3, int N4, int N5, int N6, int N7, int N8, int N9) {
			    	    super();
					    if (kNumResiduals <= 0) {
					       System.err.println("Cost functions must have at least one residual block.");
					       return;
					    }

					    if(!(((N1 == 0) && (N2 == 0) && (N3 == 0) && (N4 == 0) && (N5 == 0) && (N6 == 0) && (N7 == 0) && (N8 == 0) && (N9 == 0)) ||
					          ((N1 > 0) && (N2 == 0) && (N3 == 0) && (N4 == 0) && (N5 == 0) && (N6 == 0) && (N7 == 0) && (N8 == 0) && (N9 == 0)) ||
					          ((N1 > 0) && (N2 > 0) && (N3 == 0) && (N4 == 0) && (N5 == 0) && (N6 == 0) && (N7 == 0) && (N8 == 0) && (N9== 0)) ||         
					          ((N1 > 0) && (N2 > 0) && (N3 > 0) && (N4 == 0) && (N5 == 0) && (N6 == 0) && (N7 == 0) && (N8 == 0) && (N9 == 0)) ||                  
					          ((N1 > 0) && (N2 > 0) && (N3 > 0) && (N4 > 0) && (N5 == 0) && (N6 == 0) && (N7 == 0) && (N8 == 0) && (N9 == 0)) ||         
					          ((N1 > 0) && (N2 > 0) && (N3 > 0) && (N4 > 0) && (N5 > 0) && (N6 == 0) && (N7 == 0) && (N8 == 0) && (N9 == 0)) ||          
					          ((N1 > 0) && (N2 > 0) && (N3 > 0) && (N4 > 0) && (N5 > 0) && (N6 > 0) && (N7 == 0)&& (N8 == 0) && (N9 == 0)) ||      
					          ((N1 > 0) && (N2 > 0) && (N3 > 0) && (N4 > 0) && (N5 > 0) && (N6 > 0) && (N7 > 0) && (N8 == 0) && (N9 == 0)) ||        
					          ((N1 > 0) && (N2 > 0) && (N3 > 0) && (N4 > 0) && (N5 > 0) && (N6 > 0) && (N7 > 0) && (N8 > 0) && (N9 == 0)) ||   
					          ((N1 > 0) && (N2 > 0) && (N3 > 0) && (N4 > 0) && (N5 > 0) && (N6 > 0) && (N7 > 0) && (N8 > 0) && (N9 > 0)))) { 
					        System.err.println("Zero block cannot precede a non-zero block. Block sizes are ");
					        System.err.println ("(ignore trailing 0s): " + N0 + ", " + N1 + ", " + N2 + ", ");
					        System.err.println(N3 + ", " + N4 + ", " + N5 + ", " + N6 + ", " + N7 + ", ");
					        System.err.println(N8 + ", " + N9);
					    }

					    set_num_residuals(kNumResiduals);
					    
					    if (N0 > 0) mutable_parameter_block_sizes().add(N0);
					    if (N1 > 0) mutable_parameter_block_sizes().add(N1);
					    if (N2 > 0) mutable_parameter_block_sizes().add(N2);
					    if (N3 > 0) mutable_parameter_block_sizes().add(N3);
					    if (N4 > 0) mutable_parameter_block_sizes().add(N4);
					    if (N5 > 0) mutable_parameter_block_sizes().add(N5);
					    if (N6 > 0) mutable_parameter_block_sizes().add(N6);
					    if (N7 > 0) mutable_parameter_block_sizes().add(N7);
					    if (N8 > 0) mutable_parameter_block_sizes().add(N8);
					    if (N9 > 0) mutable_parameter_block_sizes().add(N9);
			}
	}
	
	class AutoDiffCostFunction<CostFunctor> extends SizedCostFunction {
		private CostFunctor functor_;
		private int kNumResiduals;
		private int num_residuals;
		
		// Takes ownership of functor. Uses the template-provided value for the
		// number of residuals ("kNumResiduals").
		public AutoDiffCostFunction(CostFunctor functor, int kNumResiduals, int N0, int N1, int N2, int N3, int N4,
				int N5, int N6, int N7, int N8, int N9) {
			super(kNumResiduals, N0, N1, N2, N3,
		            N4, N5, N6, N7, N8, N9);
			this.functor_ = functor;
			this.kNumResiduals = kNumResiduals;
			if (kNumResiduals == DYNAMIC) {
				System.err.println("Can't run the fixed-size constructor if the number of residuals is set to DYNAMIC.");
			}
		}
		
		  // Takes ownership of functor. Ignores the template-provided
		  // kNumResiduals in favor of the "num_residuals" argument provided.
		  //
		  // This allows for having autodiff cost functions which return varying
		  // numbers of residuals at runtime.
		  public AutoDiffCostFunction(CostFunctor functor, int kNumResiduals, int N0, int N1, int N2, int N3, int N4,
					int N5, int N6, int N7, int N8, int N9, int num_residuals) {
			  super(kNumResiduals, N0, N1, N2, N3,
			            N4, N5, N6, N7, N8, N9, num_residuals);
			this.functor_ = functor;
			this.kNumResiduals = kNumResiduals;
			this.num_residuals = num_residuals;
		    if (kNumResiduals != DYNAMIC) {
		        System.err.println("Can't run the dynamic-size constructor if the number of residuals is not DYNAMIC.");
		        return;
		    }
		    
		  }
		  
		  public boolean Evaluate(Vector<double[]> parameters,
                  double residuals[],
                  double jacobians[][]) {
			if (jacobians == null) {
				if ((kNumResiduals == 1) || ((kNumResiduals == DYNAMIC) && (num_residuals == 1))) {
					double x[] = parameters.get(0);
					return ((CostFunctorExample) functor_).operator(x, residuals);
				}
				else {
					return true;
				}
			}
			else {
				return true;
			}
		  }
			/*return internal::VariadicEvaluate<
			    CostFunctor, double, N0, N1, N2, N3, N4, N5, N6, N7, N8, N9>
			    ::Call(*functor_, parameters, residuals);
			}
			return internal::AutoDiff<CostFunctor, double,
			     N0, N1, N2, N3, N4, N5, N6, N7, N8, N9>::Differentiate(
			         *functor_,
			         parameters,
			         SizedCostFunction<kNumResiduals,
			                           N0, N1, N2, N3, N4,
			                           N5, N6, N7, N8, N9>::num_residuals(),
			         residuals,
			         jacobians);
			}*/

	}
	
	abstract class LossFunction {
		public LossFunction() {
			
		}
		
		// For a residual vector with squared 2-norm 'sq_norm', this method
		  // is required to fill in the value and derivatives of the loss
		  // function (rho in this example):
		  //
		  //   out[0] = rho(sq_norm),
		  //   out[1] = rho'(sq_norm),
		  //   out[2] = rho''(sq_norm),
		  //
		  // Here the convention is that the contribution of a term to the
		  // cost function is given by 1/2 rho(s),  where
		  //
		  //   s = ||residuals||^2.
		  //
		  // Calling the method with a negative value of 's' is an error and
		  // the implementations are not required to handle that case.
		  //
		  // Most sane choices of rho() satisfy:
		  //
		  //   rho(0) = 0,
		  //   rho'(0) = 1,
		  //   rho'(s) < 1 in outlier region,
		  //   rho''(s) < 0 in outlier region,
		  //
		  // so that they mimic the least squares cost for small residuals.
		  public abstract void Evaluate(double sq_norm, double out[]);
	} // abstract class LossFunction
	
	class Program {
		// The Program does not own the ParameterBlock or ResidualBlock objects.
		  private Vector<ParameterBlock> parameter_blocks_;
		  private Vector<ResidualBlock> residual_blocks_;	
		  public Program() {
			  parameter_blocks_ = new Vector<ParameterBlock>();
			  residual_blocks_ = new Vector<ResidualBlock>();
		  }
		  
		  public int NumResidualBlocks()  {
			  if (residual_blocks_ == null) {
				  return 0;
			  }
			  return residual_blocks_.size();
			}

			public int NumParameterBlocks() {
			  if (parameter_blocks_ == null) {
				  return 0;
			  }
			  return parameter_blocks_.size();
			}

			public int NumResiduals() {
			  int num_residuals = 0;
			  if (residual_blocks_ == null) {
				  return 0;
			  }
			  for (int i = 0; i < residual_blocks_.size(); ++i) {
			    num_residuals += residual_blocks_.get(i).NumResiduals();
			  }
			  return num_residuals;
			}

			public int NumParameters() {
			  if (parameter_blocks_ == null) {
				  return 0;
			  }
			  int num_parameters = 0;
			  for (int i = 0; i < parameter_blocks_.size(); ++i) {
			    num_parameters += parameter_blocks_.get(i).Size();
			  }
			  return num_parameters;
			}

			public int NumEffectiveParameters() {
			  if (parameter_blocks_ == null) {
				  return 0;
			  }
			  int num_parameters = 0;
			  for (int i = 0; i < parameter_blocks_.size(); ++i) {
			    num_parameters += parameter_blocks_.get(i).LocalSize();
			  }
			  return num_parameters;
			}
			
			public boolean SetParameterBlockStatePtrsToUserStatePtrs() {
				  for (int i = 0; i < parameter_blocks_.size(); ++i) {
				    if (!parameter_blocks_.get(i).IsConstant() &&
				        !parameter_blocks_.get(i).SetState(parameter_blocks_.get(i).user_state())) {
				      return false;
				    }
				  }
				  return true;
			}
			
			public Vector<ParameterBlock> parameter_blocks() {
				  return parameter_blocks_;
			}
			
			public Vector<ParameterBlock> mutable_parameter_blocks() {
				  return parameter_blocks_;
		    }

			
			public Vector<ResidualBlock> residual_blocks() {
				  return residual_blocks_;
			}
			
			public boolean ParameterBlocksAreFinite(String message[]) {
				  if (message == null) {
					  System.err.println("String message[] is null in Program.ParameterBlocksAreFinite");
					  return false;
				  }
				  for (int i = 0; i < parameter_blocks_.size(); ++i) {
				    ParameterBlock parameter_block = parameter_blocks_.get(i);
				    double[] array = parameter_block.user_state();
				    int size = parameter_block.Size();
				    int invalid_index = FindInvalidValue(size, array);
				    if (invalid_index != size) {
				      message[0] = "ParameterBlock with size " + size + " has at least one invalid value.\n"
				          + "First invalid value = " + array[invalid_index] + " is at index " + invalid_index +
				          "\nParameter block values: ";
			              for (int k = 0; k < size; k++) {
			            	  message[0] += "\n" + array[k];
			              }
				      return false;
				    }
				  }
				  return true;
				}

			public boolean IsFeasible(String message[]) {
				if (message == null) {
					  System.err.println("String message[] is null in Program.IsFeasible");
					  return false;
				  }
				  for (int i = 0; i < parameter_blocks_.size(); ++i) {
				    ParameterBlock parameter_block = parameter_blocks_.get(i);
				    double[] parameters = parameter_block.user_state();
				    int size = parameter_block.Size();
				    if (parameter_block.IsConstant()) {
				      // Constant parameter blocks must start in the feasible region
				      // to ultimately produce a feasible solution, since Ceres cannot
				      // change them.
				      for (int j = 0; j < size; ++j) {
				        double lower_bound = parameter_block.LowerBoundForParameter(j);
				        double upper_bound = parameter_block.UpperBoundForParameter(j);
				        if (parameters[j] < lower_bound || parameters[j] > upper_bound) {
				          message[0] = "ParameterBlock with size " + size + " has at least one infeasible value." +
				              "\nFirst infeasible value is at index: " + j + "." +
				              "\nLower bound: " + lower_bound + " , value: " + parameters[j] + " , upper bound: " + upper_bound + 
				              "\nParameter block values: ";
				              for (int k = 0; k < size; k++) {
				            	  message[0] += "\n" + parameters[k];
				              }

				          return false;
				        }
				      }
				    } else {
				      // Variable parameter blocks must have non-empty feasible
				      // regions, otherwise there is no way to produce a feasible
				      // solution.
				      for (int j = 0; j < size; ++j) {
				        double lower_bound = parameter_block.LowerBoundForParameter(j);
				        double upper_bound = parameter_block.UpperBoundForParameter(j);
				        if (lower_bound >= upper_bound) {
				        	 message[0] = "ParameterBlock with size " + size + " has lower_bound >= upper_bound for parameter " + j + "." +
						     "\nLower bound: " + lower_bound + " , value: " + parameters[j] + " , upper bound: " + upper_bound + 
						     "\nParameter block values: ";
				              for (int k = 0; k < size; k++) {
				            	  message[0] += "\n" + parameters[k];
				              }
				          return false;
				        }
				      }
				    }
				  }

				  return true;
				}
			
			public boolean IsBoundsConstrained() {
				  for (int i = 0; i < parameter_blocks_.size(); ++i) {
				    ParameterBlock parameter_block = parameter_blocks_.get(i);
				    if (parameter_block.IsConstant()) {
				      continue;
				    }
				    int size = parameter_block.Size();
				    for (int j = 0; j < size; ++j) {
				      double lower_bound = parameter_block.LowerBoundForParameter(j);
				      double upper_bound = parameter_block.UpperBoundForParameter(j);
				      if (lower_bound > -Double.MAX_VALUE ||
				          upper_bound < Double.MAX_VALUE) {
				        return true;
				      }
				    }
				  }
				  return false;
				}

			
			public Program CreateReducedProgram(
				    Vector<double[]> removed_parameter_blocks,
				    double fixed_cost[],
				    String error[]) {
				 if (removed_parameter_blocks == null) {
					 System.err.println("In Program.CreateReducedProgram removed_parameter_blocks == null");
					 return null;
				 }
				 if (fixed_cost == null) {
					 System.err.println("In Program.CreateReducedProgram fixed_cost == null");
					 return null;
				 }
				 if (error == null) {
					 System.err.println("In Program.CreateReducedProgram error == null");
					 return null;	 
				 }

				  //scoped_ptr<Program> reduced_program(new Program(*this));
				  Program reduced_program = new Program();
				  if (!reduced_program.RemoveFixedBlocks(removed_parameter_blocks,
				                                          fixed_cost,
				                                          error)) {
				    return null;
				  }

				  reduced_program.SetParameterOffsetsAndIndex();
				  return reduced_program;
				}
			
			public boolean RemoveFixedBlocks(Vector<double[]> removed_parameter_blocks,
                    double fixed_cost[],
                    String error[]) {
				if (removed_parameter_blocks == null) {
					 System.err.println("In Program.RemoveFixedBlocks removed_parameter_blocks == null");
					 return false;
				 }
				if (fixed_cost == null) {
					System.err.println("In Program.RemoveFixedBlocks fixed_cost == null");
					return false;
				}
				 if (error == null) {
					 System.err.println("In Program.RemoveFixedBlocks error == null");
					 return false;	 
				 }
		
		//scoped_array<double> residual_block_evaluate_scratch;
		double residual_block_evaluate_scratch[];
		residual_block_evaluate_scratch = 
		new double[MaxScratchDoublesNeededForEvaluate()];
		fixed_cost[0] = 0.0;
		
		// Mark all the parameters as unused. Abuse the index member of the
		// parameter blocks for the marking.
		for (int i = 0; i < parameter_blocks_.size(); ++i) {
		parameter_blocks_.get(i).set_index(-1);
		}
		
		// Filter out residual that have all-constant parameters, and mark
		// all the parameter blocks that appear in residuals.
		int num_active_residual_blocks = 0;
		for (int i = 0; i < residual_blocks_.size(); ++i) {
		ResidualBlock residual_block = residual_blocks_.get(i);
		int num_parameter_blocks = residual_block.NumParameterBlocks();
		
		// Determine if the residual block is fixed, and also mark varying
		// parameters that appear in the residual block.
		boolean all_constant = true;
		for (int k = 0; k < num_parameter_blocks; k++) {
		ParameterBlock parameter_block = residual_block.parameter_blocks()[k];
		if (!parameter_block.IsConstant()) {
		all_constant = false;
		parameter_block.set_index(1);
		}
		}
		
		if (!all_constant) {
		residual_blocks_.set(num_active_residual_blocks++,residual_block);
		continue;
		}
		
		// The residual is constant and will be removed, so its cost is
		// added to the variable fixed_cost.
		double cost[] = new double[] {0.0};
		if (!residual_block.Evaluate(true,
		                      cost,
		                      null,
		                      null,
		                      residual_block_evaluate_scratch)) {
		error[0] = "Evaluation of the residual " + i + " failed during removal of fixed residual blocks.";
		return false;
		}
		fixed_cost[0] += cost[0];
		}
		//residual_blocks_.resize(num_active_residual_blocks);
		int oldSize = residual_blocks_.size();
		if (oldSize > num_active_residual_blocks) {
		    while (residual_blocks_.size() > num_active_residual_blocks) {
		    	residual_blocks_.removeElementAt(residual_blocks_.size()-1);
		    }
		}
		
		// Filter out unused or fixed parameter blocks.
		int num_active_parameter_blocks = 0;
		removed_parameter_blocks.clear();
		for (int i = 0; i < parameter_blocks_.size(); ++i) {
		ParameterBlock parameter_block = parameter_blocks_.get(i);
		if (parameter_block.index() == -1) {
		removed_parameter_blocks.add(
		parameter_block.mutable_user_state());
		} else {
		parameter_blocks_.set(num_active_parameter_blocks++,parameter_block);
		}
		}
		//parameter_blocks_.resize(num_active_parameter_blocks);
		oldSize = parameter_blocks_.size();
		if (oldSize > num_active_parameter_blocks) {
		    while (parameter_blocks_.size() > num_active_parameter_blocks) {
		    	parameter_blocks_.removeElementAt(parameter_blocks_.size()-1);
		    }
		}
		
		if (!(((NumResidualBlocks() == 0) &&
		(NumParameterBlocks() == 0)) ||
		((NumResidualBlocks() != 0) &&
		(NumParameterBlocks() != 0)))) {
		error[0] =  "Congratulations, you found a bug in Ceres. Please report it.";
		return false;
		}
		
		return true;
       }


	   public int MaxScratchDoublesNeededForEvaluate() {
				  // Compute the scratch space needed for evaluate.
				  int max_scratch_bytes_for_evaluate = 0;
				  for (int i = 0; i < residual_blocks_.size(); ++i) {
				    max_scratch_bytes_for_evaluate =
				        Math.max(max_scratch_bytes_for_evaluate,
				            residual_blocks_.get(i).NumScratchDoublesForEvaluate());
				  }
				  return max_scratch_bytes_for_evaluate;
	   }
	   
	   public void SetParameterOffsetsAndIndex() {
		   // Set positions for all parameters appearing as arguments to residuals to one
		   // past the end of the parameter block array.
		   for (int i = 0; i < residual_blocks_.size(); ++i) {
		     ResidualBlock residual_block = residual_blocks_.get(i);
		     for (int j = 0; j < residual_block.NumParameterBlocks(); ++j) {
		       residual_block.parameter_blocks()[j].set_index(-1);
		     }
		   }
		   // For parameters that appear in the program, set their position and offset.
		   int state_offset = 0;
		   int delta_offset = 0;
		   for (int i = 0; i < parameter_blocks_.size(); ++i) {
		     parameter_blocks_.get(i).set_index(i);
		     parameter_blocks_.get(i).set_state_offset(state_offset);
		     parameter_blocks_.get(i).set_delta_offset(delta_offset);
		     state_offset += parameter_blocks_.get(i).Size();
		     delta_offset += parameter_blocks_.get(i).LocalSize();
		   }
		 }

	   public void ParameterBlocksToStateVector(Vector<double[]> state) {
		   for (int i = 0; i < parameter_blocks_.size(); ++i) {
		     parameter_blocks_.get(i).GetState(state.get(i));
		   }
		 }
	   
	   public int MaxParametersPerResidualBlock() {
		   int max_parameters = 0;
		   for (int i = 0; i < residual_blocks_.size(); ++i) {
		     max_parameters = Math.max(max_parameters,
		                          residual_blocks_.get(i).NumParameterBlocks());
		   }
		   return max_parameters;
		 }

		 public int MaxResidualsPerResidualBlock() {
		   int max_residuals = 0;
		   for (int i = 0; i < residual_blocks_.size(); ++i) {
		     max_residuals = Math.max(max_residuals, residual_blocks_.get(i).NumResiduals());
		   }
		   return max_residuals;
		 }

		 public int MaxDerivativesPerResidualBlock() {
			  int max_derivatives = 0;
			  for (int i = 0; i < residual_blocks_.size(); ++i) {
			    int derivatives = 0;
			    ResidualBlock residual_block = residual_blocks_.get(i);
			    int num_parameters = residual_block.NumParameterBlocks();
			    for (int j = 0; j < num_parameters; ++j) {
			      derivatives += residual_block.NumResiduals() *
			                     residual_block.parameter_blocks()[j].LocalSize();
			    }
			    max_derivatives = Math.max(max_derivatives, derivatives);
			  }
			  return max_derivatives;
			}
		 
		 public boolean IsParameterBlockSetIndependent(Set<double[]> independent_set) {
				  // Loop over each residual block and ensure that no two parameter
				  // blocks in the same residual block are part of
				  // parameter_block_ptrs as that would violate the assumption that it
				  // is an independent set in the Hessian matrix.
			      for (ResidualBlock rb : residual_blocks_) {
			    	  ParameterBlock parameter_blocks[] = rb.parameter_blocks();
			    	  int num_parameter_blocks = rb.NumParameterBlocks();
			    	  int count = 0;
					    for (int i = 0; i < num_parameter_blocks; ++i) {
					      if (independent_set.contains(parameter_blocks[i].mutable_user_state())) {
					    	  count++;
					      }
					    }
					    if (count > 1) {
					      return false;
					    }
			      }
				  return true;
				}



	} // class Program
	
	class ProblemImpl {
		private Vector<double[]>residual_parameters_;
		private Program program_;
		private HashMap<double[], ParameterBlock> parameter_block_map_;
		protected Options options_;
		// Iff enable_fast_removal is enabled, contains the current residual blocks.
		private HashSet<ResidualBlock> residual_block_set_;
		private HashMap<CostFunction, Integer> cost_function_ref_count_;
		private HashMap<LossFunction, Integer> loss_function_ref_count_;
		private boolean context_impl_owned_;
		private ContextImpl context_impl_;
		private int count;
		
		class Options {
			// These flags control whether the Problem object owns the cost
		    // functions, loss functions, and parameterizations passed into
		    // the Problem. If set to TAKE_OWNERSHIP, then the problem object
		    // will delete the corresponding cost or loss functions on
		    // destruction. The destructor is careful to delete the pointers
		    // only once, since sharing cost/loss/parameterizations is
		    // allowed.
		    public Ownership cost_function_ownership;
		    public Ownership loss_function_ownership;
		    public Ownership local_parameterization_ownership;
		    
		    // The increase in memory usage is twofold: an additonal hash set per
		    // parameter block containing all the residuals that depend on the parameter
		    // block; and a hash set in the problem containing all residuals.
		    public boolean enable_fast_removal;

		    // By default, Ceres performs a variety of safety checks when constructing
		    // the problem. There is a small but measurable performance penalty to
		    // these checks, typically around 5% of construction time. If you are sure
		    // your problem construction is correct, and 5% of the problem construction
		    // time is truly an overhead you want to avoid, then you can set
		    // disable_all_safety_checks to true.
		    //
		    // WARNING: Do not set this to true, unless you are absolutely sure of what
		    // you are doing.
		    public boolean disable_all_safety_checks;

		    // A Ceres global context to use for solving this problem. This may help to
		    // reduce computation time as Ceres can reuse expensive objects to create.
		    // The context object can be NULL, in which case Ceres may create one.
		    //
		    // Ceres does NOT take ownership of the pointer.
		    public Context context;
		 
			public Options() {
				cost_function_ownership = Ownership.TAKE_OWNERSHIP;
		        loss_function_ownership = Ownership.TAKE_OWNERSHIP;
		        local_parameterization_ownership = Ownership.TAKE_OWNERSHIP;
		        enable_fast_removal = false;
		        disable_all_safety_checks = false;
		        context = null;	
			}
		}
		public ProblemImpl() {
		    residual_parameters_ = new Vector<double[]>(10);
		    options_ = new Options();
		    program_ = new Program();
		    parameter_block_map_ = new HashMap<double[], ParameterBlock>();
		    residual_block_set_ = new HashSet<ResidualBlock>();
		    cost_function_ref_count_ = new HashMap<CostFunction, Integer>();
		    loss_function_ref_count_ = new HashMap<LossFunction, Integer>();
		    InitializeContext(options_.context, context_impl_, context_impl_owned_);
		}
		
		public ProblemImpl(Options options) {
		    residual_parameters_ = new Vector<double[]>(10);
		    options_ = options;
		    program_ = new Program();
		    parameter_block_map_ = new HashMap<double[], ParameterBlock>();
		    residual_block_set_ = new HashSet<ResidualBlock>();
		    cost_function_ref_count_ = new HashMap<CostFunction, Integer>();
		    loss_function_ref_count_ = new HashMap<LossFunction, Integer>();
		    InitializeContext(options_.context, context_impl_, context_impl_owned_);
		}
		
		void InitializeContext(Context context,
                ContextImpl context_impl,
                boolean context_impl_owned) {
				if (context == null) {
				    context_impl_owned = true;
				    context_impl = new ContextImpl();
				} else {
				    context_impl_owned = false;
				    context_impl = (ContextImpl)context;
				}
		}
		
		public void AddParameterBlock(
			    double[] values,
			    int size,
			    LocalParameterization local_parameterization) {
			  ParameterBlock parameter_block =
			      InternalAddParameterBlock(values, size);
			  if (local_parameterization != null) {
			    parameter_block.SetParameterization(local_parameterization);
			  }
		}

		public void SetParameterBlockConstant(double[] values) {
			  ParameterBlock parameter_block =
			      FindWithDefault(parameter_block_map_, values, null);
			  if (parameter_block == null) {
			    System.err.println("In SetParameterBlockConstant Parameter block not found for supplied double[] values.");
			    System.err.println("You must add the parameter block to the problem before it can be set constant.");
			    return;
			  }

			  parameter_block.SetConstant();
		}
		
		public LocalParameterization GetParameterization(double[] values) {
			  ParameterBlock parameter_block =
			      FindWithDefault(parameter_block_map_, values, null);
			  if (parameter_block == null) {
			    System.err.println(" In GetParameterization Parameter block not found for supplied double[] values.");
			    System.err.println("You must add the parameter block to the problem before you can get its local parameterization.");
			    return null;
			  }

			  return parameter_block.local_parameterization();
			}


		
		public Program mutable_program() {
			return program_;
		}

		public Program program() {
			return program_;
		}
		
		public ContextImpl context() { return context_impl_; }
		
		public ResidualBlock AddResidualBlock(CostFunction cost_function, LossFunction loss_function, double[] x0) {
			  residual_parameters_.clear();
			  residual_parameters_.add(x0);
			  return AddResidualBlock(cost_function, loss_function, residual_parameters_);
		}
		
		public ResidualBlock AddResidualBlock(CostFunction cost_function, LossFunction loss_function, Vector<double[]> parameter_blocks) {
			int i,j;
			if (cost_function == null) {
				System.err.println("cost_function is null in AddResidualBlock");
				return null;
			}
			if (parameter_blocks.size() != cost_function.parameter_block_sizes().size()) {
				System.err.println("parameter_blocks.size() != cost_function.parameter_block_sizes().size() in AddResidualBlock");
				System.err.println("parameter_blocks_size() = " + parameter_blocks.size());
				System.err.println("cost_function.parameter_block_sizes().size() = " + cost_function.parameter_block_sizes().size());
				
				return null;
			}
			
			// Check the sizes match.
			Vector<Integer> parameter_block_sizes =
			      cost_function.parameter_block_sizes();
			
			if (!options_.disable_all_safety_checks) {
			    if (parameter_block_sizes.size() != parameter_blocks.size()) {
			        System.err.println("Number of blocks input is different than the number of blocks ");
			        System.err.println("that the cost function expects in AddResidualBlock");
			        return null;
			    }
			}

			    // Check for duplicate parameter blocks.
			    // That is, parameter blocks pointing to the same memory location
			    for (i = 0; i < parameter_blocks.size(); i++) {
			    	for (j = i+1; j < parameter_blocks.size(); j++) {
			    		if (parameter_blocks.get(i) == parameter_blocks.get(j)) {
			    			System.err.println("Duplicate parameter blocks in a residual parameter are not allowed.");
            		    	System.err.println("Parameter blocks " + i + " and " + j + " are duplicates.");	
			    		}
			    	}
			    }
			
			// Add parameter blocks and convert the double*'s to parameter blocks.
			Vector<ParameterBlock> parameter_block_ptrs = new Vector<ParameterBlock>(parameter_blocks.size());
			  for (i = 0; i < parameter_blocks.size(); ++i) {
			    parameter_block_ptrs.add(i,
			        InternalAddParameterBlock(parameter_blocks.get(i),
			                                  parameter_block_sizes.get(i)));
			  }
			  
			  if (!options_.disable_all_safety_checks) {
				    // Check that the block sizes match the block sizes expected by the
				    // cost_function.
				    for (i = 0; i < parameter_block_ptrs.size(); ++i) {
				      if (cost_function.parameter_block_sizes().get(i) !=
				               parameter_block_ptrs.get(i).Size()) {
				          System.err.println("The cost function expects parameter block " + i);
				          System.err.println(" of size " + cost_function.parameter_block_sizes().get(i));
				          System.err.println(" but was given a block of size " + parameter_block_ptrs.get(i).Size());
				      }
				    }
				  }
			  
			  ResidualBlock new_residual_block =
				      new ResidualBlock(cost_function,
				                        loss_function,
				                        parameter_block_ptrs,
				                        program_.residual_blocks_.size());

			// Add dependencies on the residual to the parameter blocks.
			  if (options_.enable_fast_removal) {
			    for (i = 0; i < parameter_blocks.size(); ++i) {
			      parameter_block_ptrs.get(i).AddResidualBlock(new_residual_block);
			    }
			  }

			  program_.residual_blocks_.add(new_residual_block);
			  
			  if (options_.enable_fast_removal) {
				    residual_block_set_.add(new_residual_block);
			  }
			  
			  if (options_.cost_function_ownership == Ownership.TAKE_OWNERSHIP) {
				    // Increment the reference count, creating an entry in the table if
				    // needed. Note: C++ maps guarantee that new entries have default
				    // constructed values; this implies integers are zero initialized.
				    if (cost_function_ref_count_.get(cost_function) == null) {
				    	cost_function_ref_count_.put(cost_function,1);
				    }
				    else {
					    count = cost_function_ref_count_.get(cost_function);
					    cost_function_ref_count_.put(cost_function, count + 1);
				    }
				  }

			  if (options_.loss_function_ownership == Ownership.TAKE_OWNERSHIP &&
				      loss_function != null) {
				  if (loss_function_ref_count_.get(loss_function) == null) {
					  loss_function_ref_count_.put(loss_function,1);
				  }
				  else {
					  count = loss_function_ref_count_.get(loss_function);
					  loss_function_ref_count_.put(loss_function, count + 1);
				  }
		      }

				  return new_residual_block;

	
		}
		
		ParameterBlock InternalAddParameterBlock(double[] values, int size) {

			  if (values == null) {
				  System.err.println("Null pointer passed to AddParameterBlock for a parameter with size " + size);
				  return null;
			  }

			  // Ignore the request if there is a block for the given pointer already.
	          if (parameter_block_map_.containsKey(values)) {
	        	  if (!options_.disable_all_safety_checks) {
	        	      int existing_size = parameter_block_map_.get(values).Size();
	        	      if (size != existing_size) {
	        	          System.err.println("Tried adding a parameter block with the same double[], ");
	        	          System.err.println(" twice, but with different block sizes. Original ");
	        	          System.err.println("size was " + existing_size + " but new size is " + size);
	        	          return null;
	        	      }
	        	    }
	        	    return parameter_block_map_.get(values);
	          }
	          
	          
	        	    
	          // Pass the index of the new parameter block as well to keep the index in
	          // sync with the position of the parameter in the program's parameter vector.
	          ParameterBlock new_parameter_block =
	              new ParameterBlock(values, size, program_.parameter_blocks_.size());

	          // For dynamic problems, add the list of dependent residual blocks, which is
	          // empty to start.
	          if (options_.enable_fast_removal) {
	            new_parameter_block.EnableResidualBlockDependencies();
	          }
	          parameter_block_map_.put(values,new_parameter_block);
	          program_.parameter_blocks_.add(new_parameter_block);
	          return new_parameter_block;


		}
		
		public HashMap<double[], ParameterBlock> parameter_map() { return parameter_block_map_;}
		
		
	} // class ProblemImpl
	
	class OrderedGroups <T>{
		private HashMap<Integer, Set<T> > group_to_elements_;
		private HashMap <T, Integer> element_to_group_;
		public OrderedGroups() {
		    group_to_elements_ = new HashMap<Integer, Set<T>>();
		    element_to_group_ = new HashMap<T, Integer>();
		}
		
		public HashMap<Integer, Set<T> > group_to_elements() {
		    return group_to_elements_;
		}
		
			  // Add an element to a group. If a group with this id does not
			  // exist, one is created. This method can be called any number of
			  // times for the same element. Group ids should be non-negative
			  // numbers.
			  //
			  // Return value indicates if adding the element was a success.
			  public boolean AddElementToGroup( T element, int group) {
			    if (group < 0) {
			      return false;
			    }
			    
			    Integer value_element_to_group = element_to_group_.get(element);
			    if (value_element_to_group != null) {
				    if (value_element_to_group.intValue() == group) {
				    	// Element is already in the right group, nothing to do.
				    	return true;
				    }
				    
				    group_to_elements_.get(group).remove(element);
				    if ( group_to_elements_.get(group).size() == 0) {
				        group_to_elements_.remove(group);	
				    }
			    }
			    element_to_group_.put(element, group);
			    group_to_elements_.get(group).add(element);
			    return true;
			  }
			  
			// Number of groups with one or more elements.
			public int NumGroups() {
			    return group_to_elements_.size();
			  }

			  // The first group with one or more elements. Calling this when
			  // there are no groups with non-zero elements will result in a
			  // crash.
			  public int MinNonZeroGroup() {
			    if (NumGroups() == 0) {
			    	System.err.println("In OrderedGroups.MinNonZeroGroup() == 0");
			    	return -1;
			    }
			    return ((Integer)group_to_elements_.keySet().toArray()[0]).intValue();
			  }
			  
			// Return the group id for the element. If the element is not a
			  // member of any group, return -1.
			  public int GroupId(T element) {
			    Integer value = element_to_group_.get(element);
			    if (value == null) {
			      return -1;
			    }
			    return value.intValue();
			  }
			  
			// Remove the element, no matter what group it is in. Return value
			  // indicates if the element was actually removed.
			  public boolean Remove(T element) {
			    int current_group = GroupId(element);
			    if (current_group < 0) {
			      return false;
			    }

			    group_to_elements_.get(current_group).remove(element);

			    if (group_to_elements_.get(current_group).size() == 0) {
			      // If the group is empty, then get rid of it.
			      group_to_elements_.remove(current_group);
			    }

			    element_to_group_.remove(element);
			    return true;
			  }
			  
			  public int NumElements() {
			      return element_to_group_.size();
			  }

			  // Bulk remove elements. The return value indicates the number of
			  // elements successfully removed.
			  public int Remove(Vector<T> elements) {
			    if (NumElements() == 0 || elements.size() == 0) {
			      return 0;
			    }

			    int num_removed = 0;
			    for (int i = 0; i < elements.size(); ++i) {
			      if (Remove(elements.get(i))) {
			    	  num_removed++;
			      }
			    }
			    return num_removed;
			  }

	}
	
	class Context {
		public Context() {
			
		}
	}
		
		class ContextImpl extends Context{
			public ContextImpl() {
				
			}
		}
		
		class ResidualBlock {
		    private CostFunction cost_function_;
		    private LossFunction loss_function_;
		    private ParameterBlock parameter_blocks_[];

			// The index of the residual, typically in a Program. This is only to permit
			// switching from a ResidualBlock* to an index in the Program's array, needed
			// to do efficient removals.
			private int index_;
			public ResidualBlock(CostFunction cost_function,
				    LossFunction loss_function,
				    Vector<ParameterBlock> parameter_blocks,
				    int index) {
			    if (cost_function == null) {
			    	System.err.println("cost_function == null in ResidualBlock constructor");
			    }
			    cost_function_ = cost_function;
			    loss_function_ = loss_function;
			    parameter_blocks_ = new ParameterBlock[cost_function.parameter_block_sizes().size()];
			    index_ = index;
			    for (int i = 0; i < parameter_blocks.size(); i++) {
			    	parameter_blocks_[i] = parameter_blocks.get(i);
			    }
			}
			
			  // The size of the residual vector returned by this residual function.
			  public int NumResiduals() { return cost_function_.num_residuals(); }
			  
			  // Number of variable blocks that this residual term depends on.
		      public int NumParameterBlocks() {
				  return cost_function_.parameter_block_sizes().size();
			  }
		      
		      // Access the parameter blocks for this residual. The array has size
		      // NumParameterBlocks().
		      public ParameterBlock[] parameter_blocks() {
		        return parameter_blocks_;
		      }
		      
		      public CostFunction cost_function() { return cost_function_; }
		      
		      public LossFunction loss_function() { return loss_function_; }
		      
		      public int NumScratchDoublesForEvaluate() {
		    	  // Compute the amount of scratch space needed to store the full-sized
		    	  // jacobians. For parameters that have no local parameterization  no storage
		    	  // is needed and the passed-in jacobian array is used directly. Also include
		    	  // space to store the residuals, which is needed for cost-only evaluations.
		    	  // This is slightly pessimistic, since both won't be needed all the time, but
		    	  // the amount of excess should not cause problems for the caller.
		    	  int num_parameters = NumParameterBlocks();
		    	  int scratch_doubles = 1;
		    	  for (int i = 0; i < num_parameters; ++i) {
		    	    ParameterBlock parameter_block = parameter_blocks_[i];
		    	    if (!parameter_block.IsConstant() &&
		    	        parameter_block.LocalParameterizationJacobian() != null) {
		    	      scratch_doubles += parameter_block.Size();
		    	    }
		    	  }
		    	  scratch_doubles *= NumResiduals();
		    	  return scratch_doubles;
		    	}
		      
		      public boolean Evaluate(boolean apply_loss_function,
                      double cost[],
                      double residuals[],
                      double jacobians[][],
                      double scratch[]) {
		              int num_parameter_blocks = NumParameterBlocks();
		              int num_residuals = cost_function_.num_residuals();
		
		// Collect the parameters from their blocks. This will rarely allocate, since
		// residuals taking more than 8 parameter block arguments are rare.
		Vector<double[]> parameters = new Vector<double[]>();
		for (int i = 0; i < num_parameter_blocks; ++i) {
		parameters.add(parameter_blocks_[i].state());
		}
		
		// Put pointers into the scratch space into global_jacobians as appropriate.
		double[][] global_jacobians = new double[num_parameter_blocks][];
		int scratchNum = 0;
		Vector<Integer> scratchSize = new Vector<Integer>();
		if (jacobians != null) {
			for (int i = 0; i < num_parameter_blocks; ++i) {
			     ParameterBlock parameter_block = parameter_blocks_[i];
			if (jacobians[i] != null &&
			   parameter_block.LocalParameterizationJacobian() != null) {
			 scratchNum++;
			 scratchSize.add(num_residuals * parameter_block.Size());
			} 
			}	
		double scratch2D[][] = new double[scratchNum][];
		for (int i = 0; i < scratchNum; i++) {
		    scratch2D[i] = new double[scratchSize.get(i)];
		}
		for (int i = 0, j = 0; i < num_parameter_blocks; ++i) {
		     ParameterBlock parameter_block = parameter_blocks_[i];
		if (jacobians[i] != null &&
		   parameter_block.LocalParameterizationJacobian() != null) {
		 //global_jacobians[i] = scratch;
		 //scratch += num_residuals * parameter_block.Size();
		  global_jacobians[i] = scratch2D[j++];
		} else {
		 global_jacobians[i] = jacobians[i];
		}
		}
		}
		
		// If the caller didn't request residuals, use the scratch space for them.
		boolean outputting_residuals = (residuals != null);
		if (!outputting_residuals) {
		residuals = scratch;
		}
		
		// Invalidate the evaluation buffers so that we can check them after
		// the CostFunction::Evaluate call, to see if all the return values
		// that were required were written to and that they are finite.
		double[][] eval_jacobians = (jacobians != null) ? global_jacobians : null;
		
		InvalidateEvaluation(cost, residuals, eval_jacobians);
		
		if (!cost_function_.Evaluate(parameters, residuals, eval_jacobians)) {
		return false;
		}
		
		if (!IsEvaluationValid(parameters,
		                  cost,
		                  residuals,
		                  eval_jacobians)) {
	     String message =
		 "\n\n" +
		 "Error in evaluating the ResidualBlock.\n\n" +
		 "There are two possible reasons. Either the CostFunction did not evaluate and fill all    \n"  +
		 "residual and jacobians that were requested or there was a non-finite value (nan/infinite)\n"  +
		 "generated during the or jacobian computation. \n\n" +
		 EvaluationToString(parameters,
		                    cost,
		                    residuals,
		                    eval_jacobians);
		System.err.println(message);
		return false;
		}
		
		double squared_norm = 0.0;
		for (int i = 0; i < num_residuals; i++) {
			squared_norm += (residuals[i]*residuals[i]);
		}
		
		// Update the jacobians with the local parameterizations.
		if (jacobians != null) {
		for (int i = 0; i < num_parameter_blocks; ++i) {
		if (jacobians[i] != null) {
		 ParameterBlock parameter_block = parameter_blocks_[i];
		
		 // Apply local reparameterization to the jacobians.
		 if (parameter_block.LocalParameterizationJacobian() != null) {
		   // jacobians[i] = global_jacobians[i] * global_to_local_jacobian.
		   MatrixMatrixMultiply(0,
		       global_jacobians[i],
		       num_residuals,
		       parameter_block.Size(),
		       parameter_block.LocalParameterizationJacobian(),
		       parameter_block.Size(),
		       parameter_block.LocalSize(),
		       jacobians[i], 0, 0,  num_residuals, parameter_block.LocalSize());
		 }
		}
		}
		}
		
		if (loss_function_ == null || !apply_loss_function) {
		    cost[0] = 0.5 * squared_norm;
		    return true;
		}
		
		double rho[] = new double[3];
		loss_function_.Evaluate(squared_norm, rho);
		cost[0] = 0.5 * rho[0];
		
		// No jacobians and not outputting residuals? All done. Doing an early exit
		// here avoids constructing the "Corrector" object below in a common case.
		if (jacobians == null && !outputting_residuals) {
		return true;
		}
		
		// Correct for the effects of the loss function. The jacobians need to be
		// corrected before the residuals, since they use the uncorrected residuals.
		Corrector correct = new Corrector(squared_norm, rho);
		if (jacobians != null) {
		for (int i = 0; i < num_parameter_blocks; ++i) {
		if (jacobians[i] != null) {
		 ParameterBlock parameter_block = parameter_blocks_[i];
		
		 // Correct the jacobians for the loss function.
		 correct.CorrectJacobian(num_residuals,
		                         parameter_block.LocalSize(),
		                         residuals,
		                         jacobians[i]);
		}
		}
		}
		
		// Correct the residuals with the loss function.
		if (outputting_residuals) {
		correct.CorrectResiduals(num_residuals, residuals);
		}
		return true;
		}

		public void InvalidateEvaluation(
                      double cost[],
                      double residuals[],
                      double[][] jacobians) {
			int num_parameter_blocks = NumParameterBlocks();
			int num_residuals = NumResiduals();
			
			InvalidateArray(1, cost);
			InvalidateArray(num_residuals, residuals);
			if (jacobians != null) {
			for (int i = 0; i < num_parameter_blocks; ++i) {
			  int parameter_block_size = parameter_blocks()[i].Size();
			  InvalidateArray(num_residuals * parameter_block_size, jacobians[i]);
			}
			}
		}
		
		public boolean IsEvaluationValid(Vector<double[]> parameters,
                double cost[],
                double residuals[],
                double jacobians[][]) {
				int num_parameter_blocks = NumParameterBlocks();
				int num_residuals = NumResiduals();
				
				if (!IsArrayValid(num_residuals, residuals)) {
				return false;
				}
				
				if (jacobians != null) {
				for (int i = 0; i < num_parameter_blocks; ++i) {
				    int parameter_block_size = parameter_blocks()[i].Size();
				if (!IsArrayValid(num_residuals * parameter_block_size, jacobians[i])) {
				 return false;
				}
				}
				}

				return true;
		}
		
		public String EvaluationToString(Vector<double[]> parameters,
                double cost[],
                double residuals[],
                double jacobians[][]) {
			if (cost == null) {
				return null;
			}
			if (residuals == null) {
				return null;
			}
			
			int num_parameter_blocks = NumParameterBlocks();
			int num_residuals = NumResiduals();
			String result[] = new String[1];
			
			result[0] = 
			      "Residual Block size: " + num_parameter_blocks + " parameter blocks x " + num_residuals + " residuals\n\n" +
			"For each parameter block, the value of the parameters are printed in the first column   \n"  +
			"and the value of the jacobian under the corresponding residual. If a ParameterBlock was \n"  +
			"held constant then the corresponding jacobian is printed as 'Not Computed'. If an entry \n"  +
			"of the Jacobian/residual array was requested but was not written to by user code, it is \n"  +
			"indicated by 'Uninitialized'. This is an error. Residuals or Jacobian values evaluating \n"  +
			"to Inf or NaN is also an error.  \n\n"; 
			
			String space = "Residuals:     ";
			result[0] += space;
			for (int i = 0; i < num_residuals; i++) {
				result[0] = result[0] + "\n" + residuals[i];
			}
			result[0] += "\n\n";
			
			for (int i = 0; i < num_parameter_blocks; ++i) {
			   int parameter_block_size = parameter_blocks()[i].Size();
			result[0] = result[0] + "Parameter Block " + i +", size: " + parameter_block_size + "\n";
			result[0] = result[0] + "\n";
			for (int j = 0; j < parameter_block_size; ++j) {
				double x[] = new double[] {parameters.get(i)[j]};
				AppendArrayToString(1, x, result);
			    result[0] = result[0] + "| ";
			for (int k = 0; k < num_residuals; ++k) {
			if (jacobians != null && jacobians[i] != null) {
				double y[] = new double[] {jacobians[i][k * parameter_block_size + j]};
				AppendArrayToString(1,y ,result);
			}
			}
			result[0] = result[0] + "\n";
			}
			result[0] = result[0] + "\n";
			}
			result[0] = result[0] + "\n";
			return result[0];
			}


		      
		} // class ResidualBlock
		
		class Corrector {
			private double sqrt_rho1_;
			private double residual_scaling_;
			private double alpha_sq_norm_;
			
			public Corrector(double sq_norm, double rho[]) {
				  if (sq_norm < 0.0) {
					  System.err.println("sq_norm < 0.0 in public Corrector(double sq_norm, double rho[])");
					  return;
				  }
				  sqrt_rho1_ = Math.sqrt(rho[1]);

				  // If sq_norm = 0.0, the correction becomes trivial, the residual
				  // and the jacobian are scaled by the squareroot of the derivative
				  // of rho. Handling this case explicitly avoids the divide by zero
				  // error that would occur below.
				  //
				  // The case where rho'' < 0 also gets special handling. Technically
				  // it shouldn't, and the computation of the scaling should proceed
				  // as below, however we found in experiments that applying the
				  // curvature correction when rho'' < 0, which is the case when we
				  // are in the outlier region slows down the convergence of the
				  // algorithm significantly.
				  //
				  // Thus, we have divided the action of the robustifier into two
				  // parts. In the inliner region, we do the full second order
				  // correction which re-wights the gradient of the function by the
				  // square root of the derivative of rho, and the Gauss-Newton
				  // Hessian gets both the scaling and the rank-1 curvature
				  // correction. Normaly, alpha is upper bounded by one, but with this
				  // change, alpha is bounded above by zero.
				  //
				  // Empirically we have observed that the full Triggs correction and
				  // the clamped correction both start out as very good approximations
				  // to the loss function when we are in the convex part of the
				  // function, but as the function starts transitioning from convex to
				  // concave, the Triggs approximation diverges more and more and
				  // ultimately becomes linear. The clamped Triggs model however
				  // remains quadratic.
				  //
				  // The reason why the Triggs approximation becomes so poor is
				  // because the curvature correction that it applies to the gauss
				  // newton hessian goes from being a full rank correction to a rank
				  // deficient correction making the inversion of the Hessian fraught
				  // with all sorts of misery and suffering.
				  //
				  // The clamped correction retains its quadratic nature and inverting it
				  // is always well formed.
				  if ((sq_norm == 0.0) || (rho[2] <= 0.0)) {
				    residual_scaling_ = sqrt_rho1_;
				    alpha_sq_norm_ = 0.0;
				    return;
				  }

				  // We now require that the first derivative of the loss function be
				  // positive only if the second derivative is positive. This is
				  // because when the second derivative is non-positive, we do not use
				  // the second order correction suggested by BANS and instead use a
				  // simpler first order strategy which does not use a division by the
				  // gradient of the loss function.
				  if (rho[1] <= 0.0) {
					  System.err.println("rho[1] <= 0.0 in public Corrector(double sq_norm, double rho[])");
					  return;
				  }

				  // Calculate the smaller of the two solutions to the equation
				  //
				  // 0.5 *  alpha^2 - alpha - rho'' / rho' *  z'z = 0.
				  //
				  // Start by calculating the discriminant D.
				  double D = 1.0 + 2.0 * sq_norm * rho[2] / rho[1];

				  // Since both rho[1] and rho[2] are guaranteed to be positive at
				  // this point, we know that D > 1.0.

				  double alpha = 1.0 - Math.sqrt(D);

				  // Calculate the constants needed by the correction routines.
				  residual_scaling_ = sqrt_rho1_ / (1 - alpha);
				  alpha_sq_norm_ = alpha / sq_norm;
				}
			
			public void CorrectResiduals(int num_rows, double residuals[]) {
				  int i;
				  if (residuals == null) {
					  System.err.println("residuals == null in Corrector.CorrectResiduals");
					  return;
				  }
				  // Equation 11 in BANS.
				  for (i = 0; i < num_rows; i++) {
					  residuals[i] *= residual_scaling_;
				  }
				}

			
			public void CorrectJacobian(int num_rows,
                    int num_cols,
                    double residuals[],
                    double jacobian[]) {
				    int i;
				if (residuals == null) {
					System.err.println("residuals == null in Corrector.CorrectJacobian");
					return;
				}
			    if (jacobian == null) {
			    	System.err.println("jacobian == null in Corrector.CorrectJacobian");
			    	return;
			    }
			
			// The common case (rho[2] <= 0).
			if (alpha_sq_norm_ == 0.0) {
			for (i = 0; i < num_rows*num_cols; i++) {
				jacobian[i] *= sqrt_rho1_;
			}
			return;
			}
			
			// Equation 11 in BANS.
			//
			//  J = sqrt(rho) * (J - alpha^2 r * r' J)
			//
			// In days gone by this loop used to be a single Eigen expression of
			// the form
			//
			//  J = sqrt_rho1_ * (J - alpha_sq_norm_ * r* (r.transpose() * J));
			//
			// Which turns out to about 17x slower on bal problems. The reason
			// is that Eigen is unable to figure out that this expression can be
			// evaluated columnwise and ends up creating a temporary.
			for (int c = 0; c < num_cols; ++c) {
			double r_transpose_j = 0.0;
			for (int r = 0; r < num_rows; ++r) {
			r_transpose_j += jacobian[r * num_cols + c] * residuals[r];
			}
			
			for (int r = 0; r < num_rows; ++r) {
			jacobian[r * num_cols + c] = sqrt_rho1_ *
			(jacobian[r * num_cols + c] -
			alpha_sq_norm_ * residuals[r] * r_transpose_j);
			}
			}
			}


		} // class Corrector
		
		class ParameterBlock {
			  int i, j;
			  private double[] user_state_;
			  private int size_;
			  boolean is_constant_;
			  LocalParameterization local_parameterization_;
			  // If non-null, contains the residual blocks this parameter block is in.
			  HashSet<ResidualBlock> residual_blocks_;

			  // The "state" of the parameter. These fields are only needed while the
			  // solver is running. While at first glance using mutable is a bad idea, this
			  // ends up simplifying the internals of Ceres enough to justify the potential
			  // pitfalls of using "mutable."
			  double[] state_;
			  double local_parameterization_jacobian_[];
			  //mutable scoped_array<double> local_parameterization_jacobian_;

			  // The index of the parameter. This is used by various other parts of Ceres to
			  // permit switching from a ParameterBlock* to an index in another array.
			  int index_;

			  // The offset of this parameter block inside a larger state vector.
			  int state_offset_;

			  // The offset of this parameter block inside a larger delta vector.
			  int delta_offset_;
			  
			  // Upper and lower bounds for the parameter block.  SetUpperBound
			  // and SetLowerBound lazily initialize the upper_bounds_ and
			  // lower_bounds_ arrays. If they are never called, then memory for
			  // these arrays is never allocated. Thus for problems where there
			  // are no bounds, or only one sided bounds we do not pay the cost of
			  // allocating memory for the inactive bounds constraints.
			  //
			  // Upon initialization these arrays are initialized to
			  // std::numeric_limits<double>::max() and
			  // -std::numeric_limits<double>::max() respectively which correspond
			  // to the parameter block being unconstrained.
			  //scoped_array<double> upper_bounds_;
			  //scoped_array<double> lower_bounds_;
			  double upper_bounds_[];
			  double lower_bounds_[];
			
			  // Create a parameter block with the user state, size, and index specified.
			  // The size is the size of the parameter block and the index is the position
			  // of the parameter block inside a Program (if any).
			  public ParameterBlock(double[] user_state, int size, int index) {
			    Init(user_state, size, index, null);
			  }

			  public ParameterBlock(double[] user_state,
			                 int size,
			                 int index,
			                 LocalParameterization local_parameterization) {
			    Init(user_state, size, index, local_parameterization);
			  }
			
			// The size of the parameter block.
			public int Size() { return size_; }
			
			public int LocalSize() {
			    return (local_parameterization_ == null)
			        ? size_
			        : local_parameterization_.LocalSize();
			  }
			
			public void SetConstant() {
				is_constant_ = true;
			}
			
			 // Set the parameterization. The parameterization can be set exactly once;
			  // multiple calls to set the parameterization to different values will crash.
			  // It is an error to pass NULL for the parameterization. The parameter block
			  // does not take ownership of the parameterization.
			  public void SetParameterization(LocalParameterization new_parameterization) {
			    if (new_parameterization == null) {
			    	System.err.println("null parameterization invalid.");
			    	return;
			    }
			    // Nothing to do if the new parameterization is the same as the
			    // old parameterization.
			    if (new_parameterization == local_parameterization_) {
			      return;
			    }

			    if (local_parameterization_ != null) {
			        System.err.println("Can't re-set the local parameterization; it leads to ");
			        System.err.println("ambiguous ownership. Current local parameterization is: " + local_parameterization_);
			        return;
			    }

			    if (new_parameterization.GlobalSize() != size_) {
			        System.err.println("Invalid parameterization for parameter block. The parameter block ");
			        System.err.println("has size " + size_ + " while the parameterization has a global ");
			        System.err.println("size of " + new_parameterization.GlobalSize() + ". Did you ");
			        System.err.println("accidentally use the wrong parameter block or parameterization?");
			        return;
			    }

			    if(new_parameterization.LocalSize() <= 0) {
			        System.err.println("Invalid parameterization. Parameterizations must have a positive ");
			        System.err.println("dimensional tangent space.");
			        return;
			    }

			    local_parameterization_ = new_parameterization;
			    //local_parameterization_jacobian_.reset(
			    local_parameterization_jacobian_ =
			        new double[local_parameterization_.GlobalSize() *
			                   local_parameterization_.LocalSize()];
			    if(!UpdateLocalParameterizationJacobian()) {
			        System.err.println("Local parameterization Jacobian computation failed for x: ");
			        for (i = 0; i < size_-1; i++) {
			        System.err.print(state_[i] + " ");	
			        }
			        System.err.println(state_[size_-1]);
			    }
			  }

			
			void Init(double[] user_state,
		            int size,
		            int index,
		            LocalParameterization local_parameterization) {
		    user_state_ = user_state;
		    size_ = size;
		    index_ = index;
		    is_constant_ = false;
		    state_ = user_state_;

		    local_parameterization_ = null;
		    if (local_parameterization != null) {
		      SetParameterization(local_parameterization);
		    }

		    state_offset_ = -1;
		    delta_offset_ = -1;
		  }
			
			boolean UpdateLocalParameterizationJacobian() {
			    if (local_parameterization_ == null) {
			      return true;
			    }

			    // Update the local to global Jacobian. In some cases this is
			    // wasted effort; if this is a bottleneck, we will find a solution
			    // at that time.

			    int jacobian_size = Size() * LocalSize();
			    // jacobian is a row-major GlobalSize() x LocalSize() matrix.
			    InvalidateArray(jacobian_size,
			                    local_parameterization_jacobian_);
			    if (!local_parameterization_.ComputeJacobian(
			            state_,
			            local_parameterization_jacobian_)) {
			      System.err.println("Local parameterization Jacobian computation failed for x:");
			      for (i = 0; i < size_-1; i++) {
				        System.err.print(state_[i] + " ");	
				  }
				  System.err.println(state_[size_-1]);
			      return false;
			    }

			    if (!IsArrayValid(jacobian_size, local_parameterization_jacobian_)) {
			      System.err.println("Local parameterization Jacobian computation returned");
			      System.err.println("an invalid matrix for x: ");
			      for (i = 0; i < size_-1; i++) {
				        System.err.print(state_[i] + " ");	
				  }
				  System.err.println(state_[size_-1]);
			      System.err.println("\n Jacobian matrix : ");
			      for (i = 0; i < size_; i++) {
			          for (j = 0; j < LocalSize()-1; j++) {
			        	  System.err.print(local_parameterization_jacobian_[i * size_ + j] + " ");
			          }
			          System.err.println(local_parameterization_jacobian_[i * size_ + LocalSize()-1]);
			      }
			      return false;
			    }
			    return true;
			  }
			
			void ToString() {
				System.out.println("size_ = " + size_);
				System.out.println("is_constant_ = " + is_constant_);
				System.out.println("index_ = " + index_);
				System.out.println("state_offset_ " + state_offset_);
				System.out.println("delta_offset_ " + delta_offset_);
			}
			
			void EnableResidualBlockDependencies() {
			    if (residual_blocks_ != null) {
			        System.err.println("Ceres bug: There is already a residual block collection ");
			        System.err.println("for parameter block: ");
			        ToString();
			        return;
			    }
			    residual_blocks_ = new HashSet<ResidualBlock>();
			  }
			
			void AddResidualBlock(ResidualBlock residual_block) {
			    if (residual_blocks_ == null) {
			        System.err.println("Ceres bug: The residual block collection is null for parameter block: ");
			        ToString();
			        return;
			    }
			    residual_blocks_.add(residual_block);
		    }
			
			public boolean IsConstant() { return is_constant_; }
			
			public double[] user_state() { return user_state_; }
			
			// Manipulate the parameter state.
			public boolean SetState(double[] x) {
				if (x == null) {
					System.err.println("In ParameterBlock.SetState x is null");
					return false;
				}
			    if (is_constant_) {
			    	System.err.println("In ParameterBlock.SetState is_constant_ is true");
			    	return false;
			    }

			    state_ = x;
			    return UpdateLocalParameterizationJacobian();
			  }
			
			public double[] mutable_user_state() { return user_state_; }
			
			public LocalParameterization mutable_local_parameterization() {
			    return local_parameterization_;
			}
			
			public LocalParameterization local_parameterization() {
			    return local_parameterization_;
			}
			
			public double LowerBoundForParameter(int index) {
			    if (lower_bounds_ == null) {
			      return -Double.MAX_VALUE;
			    } else {
			      return lower_bounds_[index];
			    }
			  }

			  public double UpperBoundForParameter(int index) {
			    if (upper_bounds_ == null) {
			      return Double.MAX_VALUE;
			    } else {
			      return upper_bounds_[index];
			    }
			  }
			  
			  public void SetUpperBound(int index, double upper_bound) {
				    if (index >= size_) {
				    	System.err.println("In ParameterBlock.SetUpperBound index >= size_");
				    	return;
				    }

				    if (upper_bounds_ == null) {
				      upper_bounds_ = new double[size_];
				      for (int i = 0; i < size_; i++) {
				    	  upper_bounds_[i] = Double.MAX_VALUE;
				      }
				    }

				    upper_bounds_[index] = upper_bound;
				  }

				  void SetLowerBound(int index, double lower_bound) {
					  if (index >= size_) {
					    	System.err.println("In ParameterBlock.SetLowerBound index >= size_");
					    	return;
					  }

				      if (lower_bounds_ == null) {
				          lower_bounds_ = new double[size_];
				          for (int i = 0; i < size_; i++) {
					    	  lower_bounds_[i] = -Double.MAX_VALUE;
					      }
				    }

				    lower_bounds_[index] = lower_bound;
				  }
				  
				  // The local to global jacobian. Returns NULL if there is no local
				  // parameterization for this parameter block. The returned matrix is row-major
				  // and has Size() rows and  LocalSize() columns.
				  public double[] LocalParameterizationJacobian() {
				    return local_parameterization_jacobian_;
				  }
				  
				  // This parameter block's index in an array.
				  public int index() { return index_; }
				  public void set_index(int index) { index_ = index; }
				  
				  // This parameter offset inside a larger state vector.
				  public int state_offset() { return state_offset_; }
				  public void set_state_offset(int state_offset) { state_offset_ = state_offset; }

				  // This parameter offset inside a larger delta vector.
				  public int delta_offset() { return delta_offset_; }
				  public void set_delta_offset(int delta_offset) { delta_offset_ = delta_offset; }
				  
				  public  double[] state() { return state_; }
				  
				  // Copy the current parameter state out to x. This is "GetState()" rather than
				  // simply "state()" since it is actively copying the data into the passed
				  // pointer.
				  public void GetState(double[] x) {
				    if (x != state_) {
				      for (int i = 0; i < state_.length; i++) {
				    	  x[i] = state_[i];
				      }
				    }
				  }
		} // class ParameterBlock
		
		// The class LocalParameterization defines the function Plus and its
		// Jacobian which is needed to compute the Jacobian of f w.r.t delta.
		abstract class LocalParameterization {
			  // jacobian is a row-major GlobalSize() x LocalSize() matrix.
			  public abstract boolean ComputeJacobian(double x[], double jacobian[]);
			  
			  // Size of x.
			  public abstract int GlobalSize();

			  // Size of delta.
			  public abstract int LocalSize();
		}
		
		class Solver {
			public Options options;
			public Summary summary;
			public Solver() {
				options = new Options();
				summary = new Summary();
			}
			
			class Options {
				public MinimizerType minimizer_type;
				public LineSearchDirectionType line_search_direction_type;
				public LineSearchType line_search_type;
				public NonlinearConjugateGradientType nonlinear_conjugate_gradient_type;
				public int max_lbfgs_rank;
				public boolean use_approximate_eigenvalue_bfgs_scaling;
				public LineSearchInterpolationType line_search_interpolation_type;
				// If during the line search, the step_size falls below this
			    // value, it is truncated to zero.
			    public double min_line_search_step_size;
			    public double line_search_sufficient_function_decrease;
			    public double max_line_search_step_contraction;
			    public double min_line_search_step_contraction;
			    public int max_num_line_search_step_size_iterations;
			    public int max_num_line_search_direction_restarts;
			    public double line_search_sufficient_curvature_decrease;
			    public double max_line_search_step_expansion;
			    public TrustRegionStrategyType trust_region_strategy_type;
			    public DoglegType dogleg_type;
			    public boolean use_nonmonotonic_steps;
			    public int max_consecutive_nonmonotonic_steps;
			    // Maximum number of iterations for the minimizer to run for.
			    public int max_num_iterations;
			    // Maximum time for which the minimizer should run for.
			    public double max_solver_time_in_seconds;
			    // Number of threads used by Ceres for evaluating the cost and jacobians.
			    public int num_threads;
			    // Trust region minimizer settings.
			    public double initial_trust_region_radius;
			    public double max_trust_region_radius;
			    // Minimizer terminates when the trust region radius becomes
			    // smaller than this value.
			    public double min_trust_region_radius;
			    // Lower bound for the relative decrease before a step is accepted.
			    public double min_relative_decrease;
			    public double min_lm_diagonal;
			    public double max_lm_diagonal;
			    public int max_num_consecutive_invalid_steps;
			    public double function_tolerance;
                public double gradient_tolerance;
                public double parameter_tolerance;
                public LinearSolverType linear_solver_type;
                public PreconditionerType preconditioner_type;
                public VisibilityClusteringType visibility_clustering_type;
                public DenseLinearAlgebraLibraryType dense_linear_algebra_library_type;
                public SparseLinearAlgebraLibraryType sparse_linear_algebra_library_type;
                // This setting is scheduled to be removed in 1.15.0.
                public int num_linear_solver_threads;
                // NOTE: This option can only be used with the SCHUR_JACOBI preconditioner.
                public boolean use_explicit_schur_complement;
                public boolean use_postordering;
                // This settings affects the SPARSE_NORMAL_CHOLESKY solver.
                public boolean dynamic_sparsity;
                public boolean use_inner_iterations;
                public double inner_iteration_tolerance;
                // Minimum number of iterations for which the linear solver should
                // run, even if the convergence criterion is satisfied.
                public int min_linear_solver_iterations;
                public  int max_linear_solver_iterations;
                public double eta;
                // Normalize the jacobian using Jacobi scaling before calling
                // the linear least squares solver.
                public boolean jacobi_scaling;
                public LoggingType logging_type;
                public boolean minimizer_progress_to_stdout;
                public String trust_region_problem_dump_directory;
                public DumpFormatType trust_region_problem_dump_format_type;
                public boolean check_gradients;
                public double gradient_check_relative_precision;
                public double gradient_check_numeric_derivative_relative_step_size;
                public boolean update_state_every_iteration;
                // List of iterations at which the minimizer should dump the trust
                // region problem. Useful for testing and benchmarking. If empty
                // (default), no problems are dumped.
                public Vector<Integer> trust_region_minimizer_iterations_to_dump;
                // The solver does NOT take ownership of the pointer.
                EvaluationCallback evaluation_callback;
                // A particular case of interest is bundle adjustment, where the user
    		    // has two options. The default is to not specify an ordering at all,
    		    // the solver will see that the user wants to use a Schur type solver
    		    // and figure out the right elimination ordering.
    		    //
    		    // But if the user already knows what parameter blocks are points and
    		    // what are cameras, they can save preprocessing time by partitioning
    		    // the parameter blocks into two groups, one for the points and one
    		    // for the cameras, where the group containing the points has an id
    		    // smaller than the group containing cameras.
    		    //shared_ptr<ParameterBlockOrdering> linear_solver_ordering;
    		    // Typedef for the most commonly used version of OrderedGroups.
    		    // typedef OrderedGroups<double*> ParameterBlockOrdering;
    		    public OrderedGroups<double[]> linear_solver_ordering;
    		    // If inner_iterations is true, then the user has two choices.
    		    //
    		    // 1. Let the solver heuristically decide which parameter blocks
    		    //    to optimize in each inner iteration. To do this leave
    		    //    Solver::Options::inner_iteration_ordering untouched.
    		    //
    		    // 2. Specify a collection of of ordered independent sets. Where
    		    //    the lower numbered groups are optimized before the higher
    		    //    number groups. Each group must be an independent set. Not
    		    //    all parameter blocks need to be present in the ordering.
    		    //shared_ptr<ParameterBlockOrdering> inner_iteration_ordering;
    		    public OrderedGroups<double[]> inner_iteration_ordering;
    		    // Callbacks that are executed at the end of each iteration of the
    		    // Minimizer. An iteration may terminate midway, either due to
    		    // numerical failures or because one of the convergence tests has
    		    // been satisfied. In this case none of the callbacks are
    		    // executed.

    		    // Callbacks are executed in the order that they are specified in
    		    // this vector. By default, parameter blocks are updated only at the
    		    // end of the optimization, i.e when the Minimizer terminates. This
    		    // behaviour is controlled by update_state_every_iteration. If the
    		    // user wishes to have access to the updated parameter blocks when
    		    // his/her callbacks are executed, then set
    		    // update_state_every_iteration to true.
    		    //
    		    // The solver does NOT take ownership of these pointers.
    		    Vector<IterationCallback> callbacks;
				public Options() {
					// Default constructor that sets up a generic sparse problem.
				      minimizer_type = MinimizerType.TRUST_REGION;
				      line_search_direction_type = LineSearchDirectionType.LBFGS;
				      line_search_type = LineSearchType.WOLFE;
				      nonlinear_conjugate_gradient_type = NonlinearConjugateGradientType.FLETCHER_REEVES;
				      max_lbfgs_rank = 20;
				      use_approximate_eigenvalue_bfgs_scaling = false;
				      line_search_interpolation_type = LineSearchInterpolationType.CUBIC;
				      min_line_search_step_size = 1e-9;
				      line_search_sufficient_function_decrease = 1e-4;
				      max_line_search_step_contraction = 1e-3;
				      min_line_search_step_contraction = 0.6;
				      max_num_line_search_step_size_iterations = 20;
				      max_num_line_search_direction_restarts = 5;
				      line_search_sufficient_curvature_decrease = 0.9;
				      max_line_search_step_expansion = 10.0;
				      trust_region_strategy_type = TrustRegionStrategyType.LEVENBERG_MARQUARDT;
				      dogleg_type = DoglegType.TRADITIONAL_DOGLEG;
				      use_nonmonotonic_steps = false;
				      max_consecutive_nonmonotonic_steps = 5;
				      max_num_iterations = 50;
				      max_solver_time_in_seconds = 1e9;
				      num_threads = 1;
				      initial_trust_region_radius = 1e4;
				      max_trust_region_radius = 1e16;
				      min_trust_region_radius = 1e-32;
				      min_relative_decrease = 1e-3;
				      min_lm_diagonal = 1e-6;
				      max_lm_diagonal = 1e32;
				      max_num_consecutive_invalid_steps = 5;
				      function_tolerance = 1e-6;
				      gradient_tolerance = 1e-10;
				      parameter_tolerance = 1e-8;

				//#if defined(CERES_NO_SUITESPARSE) && defined(CERES_NO_CXSPARSE) && !defined(CERES_ENABLE_LGPL_CODE)  // NOLINT
				      linear_solver_type = LinearSolverType.DENSE_QR;
				//#else
				      //linear_solver_type = SPARSE_NORMAL_CHOLESKY;
				//#endif

				      preconditioner_type = PreconditionerType.JACOBI;
				      visibility_clustering_type = VisibilityClusteringType.CANONICAL_VIEWS;
				      //dense_linear_algebra_library_type = EIGEN;
				      dense_linear_algebra_library_type = DenseLinearAlgebraLibraryType.LAPACK;

				      // Choose a default sparse linear algebra library in the order:
				      //
				      //   SUITE_SPARSE > CX_SPARSE > EIGEN_SPARSE > NO_SPARSE
				      sparse_linear_algebra_library_type = SparseLinearAlgebraLibraryType.NO_SPARSE;
				/*#if !defined(CERES_NO_SUITESPARSE)
				      sparse_linear_algebra_library_type = SUITE_SPARSE;
				#else
				  #if !defined(CERES_NO_CXSPARSE)
				      sparse_linear_algebra_library_type = CX_SPARSE;
				  #else
				    #if defined(CERES_USE_EIGEN_SPARSE)
				      sparse_linear_algebra_library_type = EIGEN_SPARSE;
				    #endif
				  #endif
				#endif*/

				      num_linear_solver_threads = -1;
				      use_explicit_schur_complement = false;
				      use_postordering = false;
				      dynamic_sparsity = false;
				      min_linear_solver_iterations = 0;
				      max_linear_solver_iterations = 500;
				      eta = 1e-1;
				      jacobi_scaling = true;
				      use_inner_iterations = false;
				      inner_iteration_tolerance = 1e-3;
				      logging_type = LoggingType.PER_MINIMIZER_ITERATION;
				      minimizer_progress_to_stdout = false;
				      trust_region_problem_dump_directory = "/tmp";
				      trust_region_problem_dump_format_type = DumpFormatType.TEXTFILE;
				      check_gradients = false;
				      gradient_check_relative_precision = 1e-8;
				      gradient_check_numeric_derivative_relative_step_size = 1e-6;
				      update_state_every_iteration = false;
				      // Callbacks that are executed at the end of each iteration of the
				      // Minimizer. An iteration may terminate midway, either due to
				      // numerical failures or because one of the convergence tests has
				      // been satisfied. In this case none of the callbacks are
				      // executed.

				      // Callbacks are executed in the order that they are specified in
				      // this vector. By default, parameter blocks are updated only at the
				      // end of the optimization, i.e when the Minimizer terminates. This
				      // behaviour is controlled by update_state_every_iteration. If the
				      // user wishes to have access to the updated parameter blocks when
				      // his/her callbacks are executed, then set
				      // update_state_every_iteration to true.
				      //
				      // The solver does NOT take ownership of these pointers.
				      callbacks = new Vector<IterationCallback>();
				      
				      // The solver does NOT take ownership of these pointers.
				      evaluation_callback = null;
				    }
				
				public boolean IsValid(Solver.Summary summary) {
					  if (!CommonOptionsAreValid(summary)) {
					    return false;
					  }

					  if (minimizer_type == MinimizerType.TRUST_REGION &&
					      !TrustRegionOptionsAreValid(summary)) {
					    return false;
					  }

					  // We do not know if the problem is bounds constrained or not, if it
					  // is then the trust region solver will also use the line search
					  // solver to do a projection onto the box constraints, so make sure
					  // that the line search options are checked independent of what
					  // minimizer algorithm is being used.
					  return LineSearchOptionsAreValid(summary);
					}
				
				private boolean CommonOptionsAreValid(Solver.Summary summary) {
					  if (max_num_iterations < 0) {
						  summary.message[0] = "max_num_iterations incorrectly < 0";
						  return false;
					  }
				      if (max_solver_time_in_seconds < 0.0) {
				    	  summary.message[0] = "max_solver_time_in_seconds incorrectly < 0.0";
				    	  return false;
				      }
					  if (function_tolerance < 0.0) {
						  summary.message[0] = "function_tolerance incorrectly < 0.0";
						  return false;
					  }
					  if (gradient_tolerance < 0.0) {
						  summary.message[0] = "gradient_tolerance incorrectly < 0.0";
						  return false;
					  }
					  if (parameter_tolerance < 0.0) {
						  summary.message[0] = "parameter_tolerance inocrrectly < 0.0";
						  return false;
					  }
					  if (num_threads <= 0) {
						  summary.message[0] = "num_threads incorrectly <= 0";
						  return false;
					  }
					  if (check_gradients) {
						if (gradient_check_relative_precision <= 0.0) {
							summary.message[0] = "gradient_check_relative_precision incorrectly < = 0.0";
							return false;
						}
					    if (gradient_check_numeric_derivative_relative_step_size <= 0.0) {
					    	summary.message[0] = "gradient_check_numeric_derivative_relative_step_size incorrectly <= 0.0";
					    	return false;
					    }
					  }
					  return true;
					}
				
				public boolean TrustRegionOptionsAreValid(Solver.Summary summary) {
					  if (initial_trust_region_radius <= 0.0) {
						  summary.message[0] = "initial_trust_region_radius incorrectly <= 0.0";
						  return false;
					  }
					  if (min_trust_region_radius <=  0.0) {
						  summary.message[0] = "min_trust_region_radius incorrectly <= 0.0";
						  return false;
					  }
					  if (max_trust_region_radius <= 0.0) {
						  summary.message[0] = "max_trust_region_radius incorrectly <= 0.0";
						  return false;
					  }
					  if (min_trust_region_radius > max_trust_region_radius) {
						  summary.message[0] = "min_trust_region_radius incorrectly > max_trust_region_radius";
						  return false;
					  }
					  if (min_trust_region_radius > initial_trust_region_radius) {
				          summary.message[0] = "min_trust_region_radius incorrectly > initial_trust_region_radius";
				          return false;
					  }
					  if (initial_trust_region_radius > max_trust_region_radius) {
						  summary.message[0] = "initial_trust_region_radius incorrectly > max_trust_region_radius";
						  return false;
					  }
					  if (min_relative_decrease < 0.0) {
						  summary.message[0] = "min_relative_decrease incorrectly < 0.0";
						  return false;
					  }
					  if (min_lm_diagonal < 0.0) {
						  summary.message[0] = "min_lm_diagonal incorrectly < 0.0";
						  return false;
					  }
					  if (max_lm_diagonal < 0.0) {
						  summary.message[0] = "max_lm_diagonal incorrectly < 0.0";
						  return false;
					  }
					  if (min_lm_diagonal > max_lm_diagonal) {
						  summary.message[0] = "min_lm_diagonal incorrectly > max_lm_diagonal";
						  return false;
					  }
					  if (max_num_consecutive_invalid_steps < 0) {
						  summary.message[0] = "max_num_consecutive_invalid_steps incorrectly < 0";
						  return false;
					  }
					  if (eta <= 0.0) {
						  summary.message[0] = "eta incorrectly <= 0.0";
						  return false;
					  }
					  if (min_linear_solver_iterations < 0) {
						  summary.message[0] = "min_linear_solver_iterations incorrectly < 0";
						  return false;
					  }
					  if (max_linear_solver_iterations < 1) {
						  summary.message[0] = "max_linear_solver_iterations incorrectly < 1";
						  return false;
					  }
					  if (min_linear_solver_iterations > max_linear_solver_iterations) {
						  summary.message[0] = "min_linear_solver_iterations incorrectly > max_linear_solver_iterations";
						  return false;
					  }

					  if (use_inner_iterations) {
						  if (inner_iteration_tolerance < 0.0) {
							  summary.message[0] = "inner_iteration_tolerance incorrectly < 0.0";
							  return false;
						  }
					  }

					  /*if (options.use_inner_iterations &&
					      options.evaluation_callback != NULL) {
					    *error =  "Inner iterations (use_inner_iterations = true) can't be "
					        "combined with an evaluation callback "
					        "(options.evaluation_callback != NULL).";
					    return false;
					  }*/

					  if (use_nonmonotonic_steps) {
						if (max_consecutive_nonmonotonic_steps <= 0) {
						    summary.message[0] = "max_consecutive_nonmonotonic_steps incorrectly <= 0";
						    return false;
						}
					  }

					  if (linear_solver_type == LinearSolverType.ITERATIVE_SCHUR &&
					      use_explicit_schur_complement &&
					      preconditioner_type != PreconditionerType.SCHUR_JACOBI) {
					    summary.message[0] =  "use_explicit_schur_complement only supports SCHUR_JACOBI as the preconditioner.";
					    return false;
					  }

					  if (preconditioner_type == PreconditionerType.CLUSTER_JACOBI &&
					      sparse_linear_algebra_library_type != SparseLinearAlgebraLibraryType.SUITE_SPARSE) {
					    summary.message[0] =  "CLUSTER_JACOBI requires Solver::Options::sparse_linear_algebra_library_type to be SUITE_SPARSE.";
					    return false;
					  }

					  if (preconditioner_type == PreconditionerType.CLUSTER_TRIDIAGONAL &&
					      sparse_linear_algebra_library_type != SparseLinearAlgebraLibraryType.SUITE_SPARSE) {
					    summary.message[0] =  "CLUSTER_TRIDIAGONAL requires Solver::Options::sparse_linear_algebra_library_type to be SUITE_SPARSE.";
					    return false;
					  }

					/*#ifdef CERES_NO_LAPACK
					  if (options.dense_linear_algebra_library_type == LAPACK) {
					    if (options.linear_solver_type == DENSE_NORMAL_CHOLESKY) {
					      *error = "Can't use DENSE_NORMAL_CHOLESKY with LAPACK because "
					          "LAPACK was not enabled when Ceres was built.";
					      return false;
					    } else if (options.linear_solver_type == DENSE_QR) {
					      *error = "Can't use DENSE_QR with LAPACK because "
					          "LAPACK was not enabled when Ceres was built.";
					      return false;
					    } else if (options.linear_solver_type == DENSE_SCHUR) {
					      *error = "Can't use DENSE_SCHUR with LAPACK because "
					          "LAPACK was not enabled when Ceres was built.";
					      return false;
					    }
					  }
					#endif

					#ifdef CERES_NO_SUITESPARSE
					  if (options.sparse_linear_algebra_library_type == SUITE_SPARSE) {
					    if (options.linear_solver_type == SPARSE_NORMAL_CHOLESKY) {
					      *error = "Can't use SPARSE_NORMAL_CHOLESKY with SUITESPARSE because "
					             "SuiteSparse was not enabled when Ceres was built.";
					      return false;
					    } else if (options.linear_solver_type == SPARSE_SCHUR) {
					      *error = "Can't use SPARSE_SCHUR with SUITESPARSE because "
					          "SuiteSparse was not enabled when Ceres was built.";
					      return false;
					    } else if (options.preconditioner_type == CLUSTER_JACOBI) {
					      *error =  "CLUSTER_JACOBI preconditioner not supported. "
					          "SuiteSparse was not enabled when Ceres was built.";
					      return false;
					    } else if (options.preconditioner_type == CLUSTER_TRIDIAGONAL) {
					      *error =  "CLUSTER_TRIDIAGONAL preconditioner not supported. "
					          "SuiteSparse was not enabled when Ceres was built.";
					    return false;
					    }
					  }
					#endif

					#ifdef CERES_NO_CXSPARSE
					  if (options.sparse_linear_algebra_library_type == CX_SPARSE) {
					    if (options.linear_solver_type == SPARSE_NORMAL_CHOLESKY) {
					      *error = "Can't use SPARSE_NORMAL_CHOLESKY with CX_SPARSE because "
					             "CXSparse was not enabled when Ceres was built.";
					      return false;
					    } else if (options.linear_solver_type == SPARSE_SCHUR) {
					      *error = "Can't use SPARSE_SCHUR with CX_SPARSE because "
					          "CXSparse was not enabled when Ceres was built.";
					      return false;
					    }
					  }
					#endif

					#ifndef CERES_USE_EIGEN_SPARSE
					  if (options.sparse_linear_algebra_library_type == EIGEN_SPARSE) {
					    if (options.linear_solver_type == SPARSE_NORMAL_CHOLESKY) {
					      *error = "Can't use SPARSE_NORMAL_CHOLESKY with EIGEN_SPARSE because "
					          "Eigen's sparse linear algebra was not enabled when Ceres was "
					          "built.";
					      return false;
					    } else if (options.linear_solver_type == SPARSE_SCHUR) {
					      *error = "Can't use SPARSE_SCHUR with EIGEN_SPARSE because "
					          "Eigen's sparse linear algebra was not enabled when Ceres was "
					          "built.";
					      return false;
					    }
					  }
					#endif*/

					  if (sparse_linear_algebra_library_type == SparseLinearAlgebraLibraryType.NO_SPARSE) {
					    if (linear_solver_type == LinearSolverType.SPARSE_NORMAL_CHOLESKY) {
					      summary.message[0] = "Can't use SPARSE_NORMAL_CHOLESKY as sparse_linear_algebra_library_type is NO_SPARSE.";
					      return false;
					    } else if (linear_solver_type == LinearSolverType.SPARSE_SCHUR) {
					      summary.message[0] = "Can't use SPARSE_SCHUR as sparse_linear_algebra_library_type is NO_SPARSE.";
					      return false;
					    }
					  }

					  if (trust_region_strategy_type == TrustRegionStrategyType.DOGLEG) {
					    if (linear_solver_type == LinearSolverType.ITERATIVE_SCHUR ||
					        linear_solver_type == LinearSolverType.CGNR) {
					      summary.message[0] = "DOGLEG only supports exact factorization based linear solvers.\n";
					      summary.message[0] += "If you want to use an iterative solver please \n";
					      summary.message[0] += "use LEVENBERG_MARQUARDT as the trust_region_strategy_type";
					      return false;
					    }
					  }

					  if (trust_region_minimizer_iterations_to_dump != null &&
						  trust_region_minimizer_iterations_to_dump.size() > 0 &&
					      trust_region_problem_dump_format_type != DumpFormatType.CONSOLE &&
					      ((trust_region_problem_dump_directory == null) ||
					       (trust_region_problem_dump_directory.length() == 0))) {
					    summary.message[0] = "Solver::Options::trust_region_problem_dump_directory is empty.";
					    return false;
					  }

					  if (dynamic_sparsity &&
					      linear_solver_type != LinearSolverType.SPARSE_NORMAL_CHOLESKY) {
					    summary.message[0] = "Dynamic sparsity is only supported with SPARSE_NORMAL_CHOLESKY.";
					    return false;
					  }

					  return true;
					}

				public boolean LineSearchOptionsAreValid(Solver.Summary summary) {
					  if (max_lbfgs_rank <= 0) {
						  summary.message[0] = "max_lbfgs_rank incorrectly <= 0";
						  return false;
					  }
					  if (min_line_search_step_size <= 0.0) {
						  summary.message[0] = "min_line_search_step_size incorrectly <= 0.0";
						  return false; 
					  }
					  if (max_line_search_step_contraction <= 0.0) {
						  summary.message[0] = "max_line_search_step_contraction incorrectly <= 0.0";
						  return false;
					  }
					  if (max_line_search_step_contraction >= 1.0) {
						  summary.message[0] = "max_line_search_step_contraction incorrectly >= 1.0";
						  return false;
					  }
					  if (max_line_search_step_contraction >= min_line_search_step_contraction) {
						  summary.message[0] = "max_line_search_step_contraction incorrectly >= min_line_search_step_contraction";
						  return false;
					  }
					  if (min_line_search_step_contraction > 1.0) {
						  summary.message[0] = "min_line_search_step_contraction incorrectly > 1.0";
						  return false;
					  }
					  if (max_num_line_search_step_size_iterations <= 0) {
						  summary.message[0] = "max_num_line_search_step_size_iterations incorrectly <= 0";
						  return false;
					  }
					  if (line_search_sufficient_function_decrease <= 0.0) {
						  summary.message[0] = "line_search_sufficient_function_decrease incorrectly <= 0.0";
						  return false;
					  }
					  if (line_search_sufficient_function_decrease >= line_search_sufficient_curvature_decrease) {
						  summary.message[0] = "line_search_sufficient_function_decrease incorrectly >= line_search_sufficient_curvature_decrease";
						  return false;
					  }
					  if (line_search_sufficient_curvature_decrease >= 1.0) {
						  summary.message[0] = "line_search_sufficient_curvature_decrease incorrectly >= 1.0";
						  return false;
					  }
					  if (max_line_search_step_expansion <= 1.0) {
						  summary.message[0] = "max_line_search_step_expansion incorrectly <= 1.0";
						  return false;
					  }

					  if ((line_search_direction_type == LineSearchDirectionType.BFGS ||
					       line_search_direction_type == LineSearchDirectionType.LBFGS) &&
					      line_search_type != LineSearchType.WOLFE) {
					    summary.message[0] = "Invalid configuration: Solver::Options::line_search_type = " + LineSearchTypeToString(line_search_type) + "\n";
					    summary.message[0] += "When using (L)BFGS, Solver::Options::line_search_type must be set to WOLFE.";
					    return false;
					  }

					  // Warn user if they have requested BISECTION interpolation, but constraints
					  // on max/min step size change during line search prevent bisection scaling
					  // from occurring. Warn only, as this is likely a user mistake, but one which
					  // does not prevent us from continuing.
					  // LOG_IF(WARNING,
					        if (line_search_interpolation_type == LineSearchInterpolationType.BISECTION &&
					          (max_line_search_step_contraction > 0.5 ||
					           options.min_line_search_step_contraction < 0.5)) {
					      summary.message[0] = "Warning, likely a user mistake but can continue\n";
					      summary.message[0] += "Line search interpolation type is BISECTION, but specified\n";
					      summary.message[0] += "max_line_search_step_contraction: " + max_line_search_step_contraction + ", and\n";
					      summary.message[0] += "min_line_search_step_contraction: " + min_line_search_step_contraction + ",\n";
					      summary.message[0] += "prevent bisection (0.5) scaling, continuing with solve regardless.";
					        }

					  return true;
					}

	
				}
			
			

			
			class Summary {
				public MinimizerType minimizer_type;
				public TerminationType termination_type;
				// Reason why the solver terminated.
			    public String message[];
			    // Cost of the problem (value of the objective function) before
			    // the optimization.
			    public double initial_cost;
			    // Cost of the problem (value of the objective function) after the
			    // optimization.
			    public double final_cost;
			    public double fixed_cost;
			    // IterationSummary for each minimizer iteration in order.
			    // public Vector<IterationSummary> iterations;
			    public int num_successful_steps;
			    public int num_unsuccessful_steps;
			    // Number of times inner iterations were performed.
			    public int num_inner_iteration_steps;
			    public int num_line_search_steps;
			    public double preprocessor_time_in_seconds;
			    // Time spent in the TrustRegionMinimizer.
			    public double minimizer_time_in_seconds;
			    public double postprocessor_time_in_seconds;
			    // Some total of all time spent inside Ceres when Solve is called.
			    public double total_time_in_seconds;
			    // Time (in seconds) spent in the linear solver computing the
			    // trust region step.
			    public double linear_solver_time_in_seconds;
			    public int num_linear_solves;
			    // Time (in seconds) spent evaluating the residual vector.
			    public double residual_evaluation_time_in_seconds;
			    // Number of residual only evaluations.
			    public int num_residual_evaluations;
			    // Time (in seconds) spent evaluating the jacobian matrix.
			    public double jacobian_evaluation_time_in_seconds;
			    // Number of Jacobian (and residual) evaluations.
			    int num_jacobian_evaluations;
			    // Time (in seconds) spent doing inner iterations.
			    public double inner_iteration_time_in_seconds;
			    // Time (in seconds) spent evaluating the univariate cost function as part
			    // of a line search.
			    public double line_search_cost_evaluation_time_in_seconds;
			    // Time (in seconds) spent evaluating the gradient of the univariate cost
			    // function as part of a line search.
			    public double line_search_gradient_evaluation_time_in_seconds;
			    // Time (in seconds) spent minimizing the interpolating polynomial
			    // to compute the next candidate step size as part of a line search.
			    public double line_search_polynomial_minimization_time_in_seconds;
			    // Total time (in seconds) spent performing line searches.
			    public double line_search_total_time_in_seconds;
			    // Number of parameter blocks in the problem.
			    public int num_parameter_blocks;
			    // Number of parameters in the probem.
			    public int num_parameters;
			    public int num_effective_parameters;
			    // Number of residual blocks in the problem.
			    public int num_residual_blocks;
			    // Number of residuals in the problem.
			    public int num_residuals;
			    public int num_parameter_blocks_reduced;
			    // Number of parameters in the reduced problem.
			    public int num_parameters_reduced;
			    public int num_effective_parameters_reduced;
			    // Number of residual blocks in the reduced problem.
			    public int num_residual_blocks_reduced;
			    //  Number of residuals in the reduced problem.
			    public int num_residuals_reduced;
			    // Is the reduced problem bounds constrained.
			    public boolean is_constrained;
			    //  Number of threads specified by the user for Jacobian and
			    //  residual evaluation.
			    public int num_threads_given;
			    public int num_threads_used;
			    // NOTE: This field is deprecated,
			    // Solver::Summary::num_threads_given should be used instead.
			    //
			    // This field is scheduled to be removed in 1.15.0. In the interim
			    // the value of this field will always be equal to
			    // num_threads_given.
			    //
			    // Number of threads specified by the user for solving the trust
			    // region problem.
			    public int num_linear_solver_threads_given;
			    // NOTE: This field is deprecated,
			    // Solver::Summary::num_threads_used should be used instead.
			    //
			    // This field is scheduled to be removed in 1.15.0. In the interim
			    // the value of this field will always be equal to
			    // num_threads_used.
			    //
			    // Number of threads actually used by the solver for solving the
			    // trust region problem. This number is not equal to
			    // num_threads_given if OpenMP is not available.
			    public int num_linear_solver_threads_used;
			    // Type of the linear solver requested by the user.
			    public LinearSolverType linear_solver_type_given;
			    // Type of the linear solver actually used. This may be different
			    // from linear_solver_type_given if Ceres determines that the
			    // problem structure is not compatible with the linear solver
			    // requested or if the linear solver requested by the user is not
			    // available, e.g. The user requested SPARSE_NORMAL_CHOLESKY but
			    // no sparse linear algebra library was available.
			    public LinearSolverType linear_solver_type_used;
			    // Size of the elimination groups given by the user as hints to
			    // the linear solver.
			    public Vector<Integer> linear_solver_ordering_given;
			    public Vector<Integer> linear_solver_ordering_used;
			    public String schur_structure_given;
                public String schur_structure_used;
                // True if the user asked for inner iterations to be used as part
                // of the optimization.
                public boolean inner_iterations_given;
                public boolean inner_iterations_used;
                // Size of the parameter groups given by the user for performing
                // inner iterations.
                public Vector<Integer> inner_iteration_ordering_given;
                public Vector<Integer> inner_iteration_ordering_used;
                // Type of the preconditioner requested by the user.
                public PreconditionerType preconditioner_type_given;
                public PreconditionerType preconditioner_type_used;
                public VisibilityClusteringType visibility_clustering_type;
                //  Type of trust region strategy.
                public TrustRegionStrategyType trust_region_strategy_type;
                //  Type of dogleg strategy used for solving the trust region
                //  problem.
                public DoglegType dogleg_type;
                //  Type of the dense linear algebra library used.
                public DenseLinearAlgebraLibraryType dense_linear_algebra_library_type;
                // Type of the sparse linear algebra library used.
                public SparseLinearAlgebraLibraryType sparse_linear_algebra_library_type;
                // Type of line search direction used.
                public LineSearchDirectionType line_search_direction_type;
                // Type of the line search algorithm used.
                public LineSearchType line_search_type;
                //  When performing line search, the degree of the polynomial used
                //  to approximate the objective function.
                public LineSearchInterpolationType line_search_interpolation_type;
                public NonlinearConjugateGradientType nonlinear_conjugate_gradient_type;
                // If the type of the line search direction is LBFGS, then this
                // indicates the rank of the Hessian approximation.
                public int max_lbfgs_rank;
				public Summary() {
					// Invalid values for most fields, to ensure that we are not
				    // accidentally reporting default values.
				    minimizer_type = MinimizerType.TRUST_REGION;
				    termination_type = TerminationType.FAILURE;
				    message = new String[] {"ceres::Solve was not called."};
				    initial_cost = -1.0;
				    final_cost = -1.0;
				    fixed_cost = -1.0;
				    num_successful_steps = -1;
				    num_unsuccessful_steps = -1;
				    num_inner_iteration_steps = -1;
				    num_line_search_steps = -1;
				    preprocessor_time_in_seconds = -1.0;
				    minimizer_time_in_seconds = -1.0;
				    postprocessor_time_in_seconds = -1.0;
				    total_time_in_seconds = -1.0;
				    linear_solver_time_in_seconds = -1.0;
				    num_linear_solves = -1;
				    residual_evaluation_time_in_seconds = -1.0;
				    num_residual_evaluations = -1;
				    jacobian_evaluation_time_in_seconds = -1.0;
				    num_jacobian_evaluations = -1;
				    inner_iteration_time_in_seconds = -1.0;
				    line_search_cost_evaluation_time_in_seconds = -1.0;
				    line_search_gradient_evaluation_time_in_seconds = -1.0;
				    line_search_polynomial_minimization_time_in_seconds = -1.0;
				    line_search_total_time_in_seconds = -1.0;
				    num_parameter_blocks = -1;
				    num_parameters = -1;
				    num_effective_parameters = -1;
				    num_residual_blocks = -1;
				    num_residuals = -1;
				    num_parameter_blocks_reduced = -1;
				    num_parameters_reduced = -1;
				    num_effective_parameters_reduced = -1;
				    num_residual_blocks_reduced = -1;
				    num_residuals_reduced = -1;
				    is_constrained = false;
				    num_threads_given = -1;
				    num_threads_used = -1;
				    num_linear_solver_threads_given = -1;
				    num_linear_solver_threads_used = -1;
				    linear_solver_type_given = LinearSolverType.SPARSE_NORMAL_CHOLESKY;
				    linear_solver_type_used = LinearSolverType.SPARSE_NORMAL_CHOLESKY;
				    inner_iterations_given = false;
				    inner_iterations_used = false;
				    preconditioner_type_given = PreconditionerType.IDENTITY;
				    preconditioner_type_used = PreconditionerType.IDENTITY;
				    visibility_clustering_type = VisibilityClusteringType.CANONICAL_VIEWS;
				    trust_region_strategy_type = TrustRegionStrategyType.LEVENBERG_MARQUARDT;
				    dense_linear_algebra_library_type = DenseLinearAlgebraLibraryType.EIGEN;
				    sparse_linear_algebra_library_type = SparseLinearAlgebraLibraryType.SUITE_SPARSE;
				    line_search_direction_type = LineSearchDirectionType.LBFGS;
				    line_search_type = LineSearchType.ARMIJO;
				    line_search_interpolation_type = LineSearchInterpolationType.BISECTION;
				    nonlinear_conjugate_gradient_type = NonlinearConjugateGradientType.FLETCHER_REEVES;
				    max_lbfgs_rank = -1;
				    linear_solver_ordering_given = new Vector<Integer>();
				    inner_iteration_ordering_given = new Vector<Integer>();
				}
			}
	   }
		
		class GradientCheckingIterationCallback extends IterationCallback {
			private boolean gradient_error_detected_;
			private String[] error_log_;
			 public GradientCheckingIterationCallback() {	 
			     super();
			     gradient_error_detected_ = false;
			 }
			 
			// Retrieve error status (not thread safe).
			  public boolean gradient_error_detected() { return gradient_error_detected_; }
			  public String[] error_log() { return error_log_; }
		}
		
		class EvaluationCallback {
			public EvaluationCallback() {
				
			}
		}
		
		class IterationCallback {
			public IterationCallback() {
				
			}
		}
		
		// Options pertaining to numeric differentiation (e.g., convergence criteria,
		// step sizes).
		class NumericDiffOptions {
		  public NumericDiffOptions() {
		    relative_step_size = 1e-6;
		    ridders_relative_initial_step_size = 1e-2;
		    max_num_ridders_extrapolations = 10;
		    ridders_epsilon = 1e-12;
		    ridders_step_shrink_factor = 2.0;
		  }

		  // Numeric differentiation step size (multiplied by parameter block's
		  // order of magnitude). If parameters are close to zero, the step size
		  // is set to sqrt(machine_epsilon).
		  public double relative_step_size;

		  // Initial step size for Ridders adaptive numeric differentiation (multiplied
		  // by parameter block's order of magnitude).
		  // If parameters are close to zero, Ridders' method sets the step size
		  // directly to this value. This parameter is separate from
		  // "relative_step_size" in order to set a different default value.
		  //
		  // Note: For Ridders' method to converge, the step size should be initialized
		  // to a value that is large enough to produce a significant change in the
		  // function. As the derivative is estimated, the step size decreases.
		  public double ridders_relative_initial_step_size;

		  // Maximal number of adaptive extrapolations (sampling) in Ridders' method.
		  public int max_num_ridders_extrapolations;

		  // Convergence criterion on extrapolation error for Ridders adaptive
		  // differentiation. The available error estimation methods are defined in
		  // NumericDiffErrorType and set in the "ridders_error_method" field.
		  public double ridders_epsilon;

		  // The factor in which to shrink the step size with each extrapolation in
		  // Ridders' method.
		  public double ridders_step_shrink_factor;
		}
		
		class GradientCheckingCostFunction extends CostFunction {
			  private CostFunction function_;
			  private GradientChecker gradient_checker_;
			  private double relative_precision_;
			  private String extra_info_;
			  private GradientCheckingIterationCallback callback_;

			public GradientCheckingCostFunction(
				      CostFunction function,
				      Vector<LocalParameterization> local_parameterizations,
				      NumericDiffOptions options,
				      double relative_precision,
				      String extra_info,
				      GradientCheckingIterationCallback callback) {
				          function_ = function;
				          gradient_checker_ = new GradientChecker(function, local_parameterizations, options);
				          relative_precision_ = relative_precision;
				          extra_info_ = extra_info; 
				          callback_ = callback;
				          if (callback == null) {
				        	  System.err.println("callback == null in GradientCheckingCostFunction constructor");
				        	  return;
				          }
				          Vector<Integer>parameter_block_sizes = function.parameter_block_sizes();
				          set_parameter_block_sizes(parameter_block_sizes);
				          set_num_residuals(function.num_residuals());

			}
	
		}
		
		class GradientChecker {
			  private Vector<LocalParameterization> local_parameterizations_;
			  private CostFunction function_;
			  //internal::scoped_ptr<CostFunction> finite_diff_cost_function_
			  private CostFunction finite_diff_cost_function_;
			  public GradientChecker(CostFunction function, Vector<LocalParameterization> local_parameterizations,
                                     NumericDiffOptions options) {
				   function_ = function;
				   if (function == null) {
					   System.err.println("CostFunction function = null in GradientChecker constructor");
					   return;
				   }
				   if (local_parameterizations != null) {
					    local_parameterizations_ = local_parameterizations;
					  } else {  
						// local_parameterizations == null so local_parameterizations has size == 0
					    //local_parameterizations_.resize(function.parameter_block_sizes().size(), null);
					    int newSize = function.parameter_block_sizes().size();
					    int oldSize = 0; // local_parameterizations.size();
					    if (newSize > oldSize) {
					    	int numAdd = newSize - oldSize;
					    	for (int i = 0; i < numAdd; i++) {
					    		local_parameterizations_.add(null);
					    	}
					    }
					  }
				   DynamicNumericDiffCostFunction<CostFunction>
				      finite_diff_cost_function =
				      new DynamicNumericDiffCostFunction<CostFunction>(
				          function, Ownership.DO_NOT_TAKE_OWNERSHIP, options, NumericDiffMethodType.CENTRAL);
				  finite_diff_cost_function_ = finite_diff_cost_function;

				  Vector<Integer> parameter_block_sizes =
					      function.parameter_block_sizes();
					  int num_parameter_blocks = parameter_block_sizes.size();
					  for (int i = 0; i < num_parameter_blocks; ++i) {
					    finite_diff_cost_function.AddParameterBlock(parameter_block_sizes.get(i));
					  }
					  finite_diff_cost_function.SetNumResiduals(function.num_residuals());

			  }
		}
		
		class DynamicNumericDiffCostFunction<T>  extends DynamicCostFunction {
			//internal::scoped_ptr<const CostFunctor> functor_;
			 T functor_;
			 Ownership ownership_;
			 NumericDiffOptions options_;
			 NumericDiffMethodType method;
			 public DynamicNumericDiffCostFunction(
				  T functor,
			      Ownership ownership,
			      NumericDiffOptions options,
			      NumericDiffMethodType method) {
				  super();
			      functor_ = functor;
			      ownership_ = ownership;
			      options_ = options;
			      this.method = method;
			  }
		}
		
		// A common base class for DynamicAutoDiffCostFunction and
		// DynamicNumericDiffCostFunction which depend on methods that can add
		// parameter blocks and set the number of residuals at run time.
		class DynamicCostFunction extends CostFunction {
		    public DynamicCostFunction() {
		    	super();
		    }
		    
		}
		
		void InvalidateArray(int size, double x[]) {
			  if (x != null) {
			    for (int i = 0; i < size; ++i) {
			      x[i] = kImpossibleValue;
			    }
			  }
		}
		
		boolean IsArrayValid(int size, double x[]) {
			  if (x != null) {
			    for (int i = 0; i < size; ++i) {
			      if (!Double.isFinite(x[i]) || (x[i] == kImpossibleValue))  {
			        return false;
			      }
			    }
			  }
			  return true;
		}
		
	    int FindInvalidValue(int size, double[] x) {
			  if (x == null) {
				  return size;
			  }
		    for (int i = 0; i < size; ++i) {
		      if (!Double.isFinite(x[i]) || (x[i] == kImpossibleValue))  {
		        return i;
		      }
		    }
		    return size;
		}
	    
	    public void AppendArrayToString(int size, double x[], String result[]) {
	    	  for (int i = 0; i < size; ++i) {
	    	    if (x == null) {
	    	      result[0] = result[0] + "Not Computed  ";
	    	      return;
	    	    } else {
	    	      if (x[i] == kImpossibleValue) {
	    	        result[0] = result[0] + "Uninitialized ";
	    	      } else {
	    	        result[0] = result[0] + x[i] + " ";
	    	      }
	    	    }
	    	  }
	    	}
	    
	    public void MatrixMatrixMultiply(int kOperation, double A[], int NUM_ROW_A, int NUM_COL_A,
	    		double B[], int NUM_ROW_B, int NUM_COL_B,
	    		double C[], int start_row_c, int start_col_c, int row_stride_c, int col_stride_c) {
	    	  if (NUM_COL_A != NUM_ROW_B) {
	    		  System.err.println("In MatrixMatrixMultiply NUM_COL_A != NUM_ROW_B");
	    		  return;
	    	  }

	    	  int NUM_ROW_C = NUM_ROW_A;
	    	  int NUM_COL_C = NUM_COL_B;
	    	  if (start_row_c + NUM_ROW_C > row_stride_c) {
	    		  System.err.println("In MatrixMatrixMultiply start_row_C + NUM_ROW_C > row_stride_c");
	    		  return;
	    	  }
	    	  if (start_col_c + NUM_COL_C > col_stride_c) {
	    		  System.err.println("In MatrixMatrixMultiply start_col_C + NUM_COL_C > col_stride_c");
	    		  return;
	    	  }

	    	  for (int row = 0; row < NUM_ROW_C; ++row) {
	    	    for (int col = 0; col < NUM_COL_C; ++col) {
	    	      double tmp = 0.0;
	    	      for (int k = 0; k < NUM_COL_A; ++k) {
	    	        tmp += A[row * NUM_COL_A + k] * B[k * NUM_COL_B + col];
	    	      }

	    	      int index = (row + start_row_c) * col_stride_c + start_col_c + col;
	    	      if (kOperation > 0) {
	    	        C[index] += tmp;
	    	      } else if (kOperation < 0) {
	    	        C[index] -= tmp;
	    	      } else {
	    	        C[index] = tmp;
	    	      }
	    	    }
	    	  }
	    	}


		
		String LineSearchTypeToString(LineSearchType type) {
			  switch (type) {
			  case ARMIJO:
				  return "ARMIJO";
			  case WOLFE:
				  return "WOLFE";
			    default:
			      return "UNKNOWN";
			  }
			}

		ParameterBlock FindWithDefault(HashMap<double[], ParameterBlock> collection,
                double[] key,
                ParameterBlock value) {
			if (!collection.containsKey(key)) {
				return value;
			}
			else {
				return collection.get(key);
			}
			
         }
		
		
	} // public abstract class CeresSolver
	
	class indexValueComparator implements Comparator<indexValue> {

        /**
         * DOCUMENT ME!
         * 
         * @param o1 DOCUMENT ME!
         * @param o2 DOCUMENT ME!
         * 
         * @return DOCUMENT ME!
         */
        public int compare(indexValue o1, indexValue o2) {
        	double a = o1.getValue();
            double b = o2.getValue();
            if (a < b) {
            	return -1;
            }
            else if (a > b) {
            	return 1;
            }
            else {
            	return 0;
            }
        }
	}
        
    class indexValue {
		private int index;
		private double value;
		
		public indexValue(int index, double value) {
			this.index = index;
			this.value = value;
		}
		
		public int getIndex() {
			return index;
		}
		
		public double getValue() {
			return value;
		}

		
		
	}
	



