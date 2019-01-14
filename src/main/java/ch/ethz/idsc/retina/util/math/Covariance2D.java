// code by mg
package ch.ethz.idsc.retina.util.math;

import ch.ethz.idsc.sophus.planar.ArcTan2D;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Transpose;
import ch.ethz.idsc.tensor.io.Primitives;
import ch.ethz.idsc.tensor.lie.RotationMatrix;
import ch.ethz.idsc.tensor.mat.DiagonalMatrix;
import ch.ethz.idsc.tensor.mat.Eigensystem;
import ch.ethz.idsc.tensor.sca.Sqrt;

/** immutable */
// TODO JPH refactor so that class does not use primitive double[][]
public class Covariance2D {
  public static Covariance2D of(double firstAxis, double secondAxis, double rotAngle) {
    Tensor rotMatrix = RotationMatrix.of(RealScalar.of(rotAngle));
    Tensor diagonal = DiagonalMatrix.of(firstAxis, secondAxis);
    Tensor rotated = rotMatrix.dot(diagonal).dot(Transpose.of(rotMatrix));
    return new Covariance2D(Primitives.toDoubleArray2D(rotated));
  }

  // ---
  private final double[][] covariance;
  private final Eigensystem eigensystem;

  /** @param covariance symmetric matrix as 2x2 array */
  public Covariance2D(double[][] covariance) {
    this.covariance = covariance;
    // EigenSystem.ofSymmetric(..) requires a symmetric matrix:
    covariance[1][0] = covariance[0][1];
    Tensor covarianceMatrix = Tensors.matrixDouble(covariance);
    eigensystem = Eigensystem.ofSymmetric(covarianceMatrix);
  }

  public double[][] getCovariance() {
    return covariance;
  }

  /** @return angle between the eigenvector belonging to the first eigenvalue and the x-axis */
  public Scalar rotAngle() {
    return ArcTan2D.of(eigensystem.vectors().get(0));
  }

  /** @return vector of length 2 consisting of eigenvalues */
  public Tensor stdDev() {
    return Sqrt.of(eigensystem.values());
  }
}
