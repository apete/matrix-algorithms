package com.github.waikatodatamining.matrix.algorithms.pls.kernel;


import com.github.waikatodatamining.matrix.core.matrix.Matrix;
import com.github.waikatodatamining.matrix.core.matrix.MatrixFactory;

import java.io.Serializable;

/**
 * Abstract kernel class. Implementations represent kernels that compute a dot product of two given
 * vectors in the kernel space (see {@link AbstractKernel#applyVector(Matrix, Matrix)}).
 * That is: K(x,y) = phi(x)*phi(y)
 *
 * @author Steven Lang
 */
public abstract class AbstractKernel implements Serializable {
    private static final long serialVersionUID = 8820493548875411535L;

    /**
     * Compute the dot product of the mapped x and y vectors in the kernel space, that is:
     * K(x,y) = phi(x)*phi(y)
     *
     * @param x First vector
     * @param y Second vector
     * @return Dot product of the given vectors in the kernel space
     */
    public abstract double applyVector(Matrix x, Matrix y);

    /**
     * Create a matrix K that consists of entries K_i,j = K(x_i,y_j) = phi(x_i)*phi(y_j)
     *
     * @param X First matrix
     * @param Y Second matrix
     * @return Matrix K with K_i,j = K(x_i,y_j) = phi(x_i)*phi(y_j)
     */
    public Matrix applyMatrix(Matrix X, Matrix Y) {
        Matrix result = MatrixFactory.zeros(X.numRows(), Y.numRows());
        for (int i = 0; i < X.numRows(); i++) {
            for (int j = 0; j < Y.numRows(); j++) {
                Matrix rowI = X.getRow(i);
                Matrix rowJ = Y.getRow(j);
                double value = applyVector(rowI, rowJ);
                result.set(i, j, value);
            }
        }
        return result;
    }
    /**
     * Create a matrix K that consists of entries K_i,j = K(x_i,x_j) = phi(x_i)*phi(x_j)
     *
     * @param X First matrix
     * @return Matrix K with K_i,j = K(x_i,x_j) = phi(x_i)*phi(x_j)
     */
    public Matrix applyMatrix(Matrix X) {
        int n = X.numRows();
        Matrix result = MatrixFactory.zeros(n, n);
        for (int i = 0; i < X.numRows(); i++) {
            for (int j = i; j < n; j++) {
                Matrix rowI = X.getRow(i);
                Matrix rowJ = X.getRow(j);
                double value = applyVector(rowI, rowJ);
                result.set(i, j, value);
                result.set(j, i, value);
            }
        }
        return result;
    }
}
