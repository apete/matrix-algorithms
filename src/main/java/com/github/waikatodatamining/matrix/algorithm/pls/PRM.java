package com.github.waikatodatamining.matrix.algorithm.pls;

import com.github.waikatodatamining.matrix.core.Matrix;
import com.github.waikatodatamining.matrix.core.MatrixFactory;

/**
 * Partial robust M-regression as described in
 * <a href="https://www.sciencedirect.com/science/article/abs/pii/S0169743905000638">Partial robust M-regression</a>
 * <p>
 * <p>
 * Parameters:
 * - c: Tuning constant for Fair weight function. Higher values result in a
 * flatter function. c=Infinity is equal to SIMPLS result.
 * - tol: Iterative convergence tolerance
 * - maxIter: Maximum number of iterations
 * - numSimplsCoefficients: Number of SIMPLS coefficients
 *
 * @author Steven Lang
 */
public class PRM extends AbstractSingleResponsePLS {

  private static final long serialVersionUID = 4864232250283829109L;

  /** Loop improvement tolerance */
  protected double m_Tol;

  /** Loop maximum number of iterations */
  protected int m_MaxIter;

  /** C parameter */
  protected double m_C;

  /** Residual weights N x 1 */
  protected Matrix m_Wr;

  /** Leverage weights N x 1 */
  protected Matrix m_Wx;

  /** T score matrix from SIMPLS algorithm */
  protected Matrix m_T;

  /** Gamma regression coefficients from SIMPLS algorithm */
  protected Matrix m_Gamma;

  /** The number of SIMPLS coefficients in W to keep (0 keep all). */
  protected int m_NumSimplsCoefficients;

  /** Final regression coefficients */
  protected Matrix m_FinalRegressionCoefficients;

  /** SIMPLS algorithm */
  protected SIMPLS m_Simpls;

  /**
   * Sets the number of coefficients of W matrix to keep (rest gets zeroed).
   *
   * @param value the number of coefficients, 0 to keep all
   */
  public void setNumSimplsCoefficients(int value) {
    m_NumSimplsCoefficients = value;
    reset();
  }

  /**
   * returns the number of coefficients of W matrix to keep (rest gets zeroed).
   *
   * @return the maximum number of attributes, 0 to keep all
   */
  public int getNumSimplsCoefficients() {
    return m_NumSimplsCoefficients;
  }

  public int getMaxIter() {
    return m_MaxIter;
  }

  public void setMaxIter(int maxIter) {
    if (maxIter < 0) {
      m_Logger.warning("Maximum iterations parameter must be positive " +
	"but was " + maxIter + ".");
    }
    else {
      this.m_MaxIter = maxIter;
      reset();
    }
  }

  public double getTol() {
    return m_Tol;
  }

  public void setTol(double tol) {
    if (tol < 0) {
      m_Logger.warning("Tolerance parameter must be positive but " +
	"was " + tol + ".");
    }
    else {
      this.m_Tol = tol;
      reset();
    }
  }

  public double getC() {
    return m_C;
  }

  public void setC(double c) {
    if (Math.abs(c) < 1e-10) {
      m_Logger.warning("Parameter c must not be zero!");
    }
    else {
      this.m_C = c;
      reset();
    }
  }

  @Override
  protected void reset() {
    super.reset();
    m_Wr = null;
    m_Wx = null;
    m_FinalRegressionCoefficients = null;
    m_Gamma = null;
    m_T = null;
    m_Simpls = null;
  }

  @Override
  protected void initialize() {
    super.initialize();
    setC(4);
    setTol(1e-6);
    setMaxIter(500);
    setNumSimplsCoefficients(-1);
  }

  @Override
  public String[] getMatrixNames() {
    return new String[]{
      "B",
      "Wr",
      "Wx",
      "W"
    };
  }

  @Override
  public Matrix getMatrix(String name) {
    switch (name) {
      case "B":
	return m_FinalRegressionCoefficients;
      case "Wr":
	return m_Wr;
      case "Wx":
	return m_Wx;
      case "W":
	return m_Wr.mulElementwise(m_Wx);

    }
    return null;
  }

  @Override
  public boolean hasLoadings() {
    return false;
  }

  @Override
  public Matrix getLoadings() {
    return null;
  }

  @Override
  public String toString() {
    return "";
  }

  /**
   * Fair function implementation.
   * <p>
   * f(z,c) = 1/(1+|z/c|)^2
   *
   * @param z First parameter
   * @param c Second parameter
   * @return Fair function result
   */
  protected double fairFunction(double z, double c) {
    return 1.0 / StrictMath.pow(1.0 + StrictMath.abs(z / c), 2);
  }

  /**
   * Initialize the residual and leverage weights.
   *
   * @param X Predictor matrix
   * @param y Response matrix
   */
  protected void initWeights(Matrix X, Matrix y) {
    updateResidualWeights(X, y);
    updateLeverageWeights(X);
  }

  /**
   * Update the leverage weights based on the score matrix T
   *
   * @param T Score matrix
   */
  private void updateLeverageWeights(Matrix T) {
    int n = T.numRows();
    m_Wx = MatrixFactory.zeros(n, 1);

    Matrix rowL1Median = geometricMedian(T);
    Matrix distancesToMedian = cdist(T, rowL1Median);

    double medianOfDistsToMedian = distancesToMedian.median();

    // Calculate wxi by f(zi, c) with zi = (distance_i to median) / (median of distances to median)
    for (int i = 0; i < n; i++) {
      double distToMedian = distancesToMedian.get(i, 0);
      double wxi = fairFunction(distToMedian / medianOfDistsToMedian, m_C);
      m_Wx.set(i, 0, wxi);
    }
  }

  /**
   * Update the residual weights based on the new residuals.
   *
   * @param X Predictors
   * @param y Response
   */
  protected void updateResidualWeights(Matrix X, Matrix y) {
    int n = X.numRows();
    m_Wr = MatrixFactory.zeros(n, 1);

    Matrix residuals = MatrixFactory.zeros(n, 1);

    // Check if this is the first iteration
    boolean isFirstIteration = m_T == null && m_Gamma == null;

    double yiHat = y.median();
    for (int i = 0; i < n; i++) {
      // Use ti * gamma as estimation if iteration > 0
      if (!isFirstIteration) {
	yiHat = m_T.getRow(i).mul(m_Gamma).asDouble();
      }

      // Calculate residual
      double yi = y.get(i, 0);
      double ri = yi - yiHat;
      residuals.set(i, 0, ri);
    }


    // Get estimate of residual scale
    double sigma = medianAbsoluteDeviation(residuals);
    residuals.divi(sigma);

    // Calculate weights
    for (int i = 0; i < n; i++) {
      double ri = residuals.get(i, 0);
      double wri = fairFunction(ri, m_C);
      m_Wr.set(i, 0, wri);
    }
  }

  /**
   * Mean Absolute Deviation:
   * MAD(v) = median_i | v_i - median_j v_j |
   *
   * @param v Input vector
   * @return MAD result
   */
  public double medianAbsoluteDeviation(Matrix v) {
    return v.sub(v.median()).abs().median();
  }

  /**
   * Trains using the provided data.
   *
   * @param predictors the input data
   * @param response   the dependent variable(s)
   * @return null if successful, otherwise error message
   * @throws Exception if analysis fails
   */
  protected String doPerformInitialization(Matrix predictors, Matrix response) throws Exception {
    // 1) Compute robust starting values for residual and leverage weights
    initWeights(predictors, response);

    Matrix gammaOld;
    int numComponents = getNumComponents();
    m_Gamma = MatrixFactory.zeros(numComponents, 1);
    int iteration = 0;

    // Loop until convergence of gamma
    do {
      // 2) Perform PLS (SIMPLS) on reweighted data matrices
      Matrix Xp = getReweightedMatrix(predictors);
      Matrix yp = getReweightedMatrix(response);

      m_Simpls = new SIMPLS();
      m_Simpls.setNumCoefficients(m_NumSimplsCoefficients);
      m_Simpls.setNumComponents(numComponents);
      m_Simpls.initialize(Xp, yp);

      // Get scores and regression coefficients
      gammaOld = m_Gamma.copy();
      m_T = m_Simpls.transform(Xp);
      m_Gamma = m_Simpls.getMatrix("Q").t();

      // Rescale ti by 1/sqrt(wi)
      for (int i = 0; i < m_T.numRows(); i++) {
	double wiSqrt = Math.sqrt(getCombinedWeight(i));
	Matrix rowiScaled = m_T.getRow(i).div(wiSqrt);
	m_T.setRow(i, rowiScaled);
      }

      // Update weights
      updateResidualWeights(Xp, yp);
      updateLeverageWeights(m_T);
      iteration++;

      // Check for convergence
    }
    while (m_Gamma.sub(gammaOld).norm2squared() < m_Tol && iteration < m_MaxIter);

    // Get the final regression coefficients from the latest SIMPLS run
    m_FinalRegressionCoefficients = m_Simpls.getMatrix("B");
    return null;
  }

  @Override
  public boolean canPredict() {
    return true;
  }

  @Override
  protected Matrix doTransform(Matrix predictors) throws Exception {
    return predictors.mul(m_Simpls.getMatrix("W"));
  }

  @Override
  protected Matrix doPerformPredictions(Matrix predictors) throws Exception {
    return predictors.mul(m_FinalRegressionCoefficients);
  }

  protected Matrix getReweightedMatrix(Matrix A) {
    return A.copy().scaleByColumnVector(m_Wr.mulElementwise(m_Wx).sqrt());
  }

  protected double getCombinedWeight(int i) {
    double wxi = m_Wx.getRow(i).asDouble();
    double wri = m_Wr.getRow(i).asDouble();
    return wxi * wri;
  }

  /**
   * Geometric median according to
   * <a href="https://en.wikipedia.org/wiki/Geometric_median">Geometric Median</a>
   * <p>
   * Weiszfeld's algorithm.
   *
   * @param X Points
   * @return Geometric median of {@code X}
   */
  protected Matrix geometricMedian(Matrix X) {
    // Initial guess
    Matrix guess = X.mean(0);

    int iteration = 0;
    while (iteration < m_MaxIter) {
      Matrix dists = cdist(X, guess);

      dists = dists.modifyEach(value -> {
	if (Math.abs(value) < 1e-10) {
	  return 1.0 / 0.1; // Fix zero distances
	}
	else {
	  return 1.0 / value; // invert
	}
      });

      Matrix nom = X.scaleByColumnVector(dists).sum(0);
      double denom = dists.sum(0).asDouble();
      Matrix guessNext = nom.div(denom);

      double change = guessNext.sub(guess).norm2squared();
      guess = guessNext;
      if (change < m_Tol) {
	break;
      }

      iteration++;
    }

    return guess;
  }

  /**
   * Distance function between all rows of X and a given row vector.
   *
   * @param X      Input matrix with rows
   * @param vector Row vector to compare all rows of X to
   * @return Distances of each row r_i with the input vector
   */
  protected Matrix cdist(Matrix X, Matrix vector) {
    Matrix dist = MatrixFactory.zeros(X.numRows(), 1);
    for (int i = 0; i < X.numRows(); i++) {
      Matrix rowi = X.getRow(i);
      double d = rowi.sub(vector).norm2();
      dist.set(i, 0, d);
    }
    return dist;
  }
}