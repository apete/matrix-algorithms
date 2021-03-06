package com.github.waikatodatamining.matrix.algorithms.pls.kernel;

import com.github.waikatodatamining.matrix.core.matrix.Matrix;

/**
 * Linear Kernel.
 * <p>
 * K(x,y)=(gamma*x^T*y + coef_0)^d
 *
 * @author Steven Lang
 */
public class PolyKernel extends AbstractKernel {

  private static final long serialVersionUID = 841527107134287683L;

  protected int m_Degree = 3;

  protected double m_Coef0 = 0.0;

  protected double m_Gamma = Double.NaN;


  /**
   * Get the gamma parameter.
   * Defaults: 1/n_features
   */
  public double getGamma() {
    return m_Gamma;
  }

  /**
   * Set the gamma parameter.
   *
   * @param gamma Gamma parameter
   */
  public void setGamma(double gamma) {
    this.m_Gamma = gamma;
  }

  /**
   * Get the independent coefficient parameter.
   *
   * @return Independent coefficient
   */
  public double getCoef0() {
    return m_Coef0;
  }


  /**
   * Set the independent coefficient parameter.
   *
   * @param coef0 Independent coefficient
   */
  public void setCoef0(double coef0) {
    this.m_Coef0 = coef0;
  }

  /**
   * Get the degree parameter.
   *
   * @return Degree parameter
   */
  public int getDegree() {
    return m_Degree;
  }

  /**
   * Set the degree parameter.
   *
   * @param degree Degree parameter
   */
  public void setDegree(int degree) {
    if (degree > 0) {
      m_Degree = degree;
    }
  }

  @Override
  public Matrix applyMatrix(Matrix X, Matrix Y) {
    if (Double.isNaN(m_Gamma)) {
      m_Gamma = 1.0 / X.numColumns();
    }
    Matrix result = X.mul(Y.transpose());
    result = result.mul(m_Gamma);
    result = result.add(m_Coef0);
    result = result.powElementwise(m_Degree);
    return result;
  }

  @Override
  public Matrix applyMatrix(Matrix X) {
    return this.applyMatrix(X, X);
  }

  @Override
  public double applyVector(Matrix x, Matrix y) {
    double linearTerm = x.vectorDot(y);
    if (Double.isNaN(m_Gamma)) {
      m_Gamma = 1.0 / x.numColumns();
    }
    return StrictMath.pow(m_Gamma * linearTerm + m_Coef0, m_Degree);
  }

  @Override
  public String toString() {
    return String.format("Polynomial Kernel: K(x,y)=(gamma*x^T*y + coef_0)^d, gamma=%f, d=%d, coef_0=%f", m_Gamma, m_Degree, m_Coef0);
  }
}
