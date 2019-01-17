// code by mh
package ch.ethz.idsc.gokart.core.mpc;

import java.nio.ByteBuffer;

import ch.ethz.idsc.retina.util.math.SI;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Transpose;
import ch.ethz.idsc.tensor.io.UserName;
import ch.ethz.idsc.tensor.qty.Quantity;
import ch.ethz.idsc.tensor.qty.QuantityTensor;
import junit.framework.TestCase;

public class MPCBSplineTrackTest extends TestCase {
  public void testSaving() {
    Tensor ctrX = QuantityTensor.of(Tensors.vector(0, 1, 2), SI.METER);
    Tensor ctrY = QuantityTensor.of(Tensors.vector(3, 4, 5), SI.METER);
    Tensor ctrR = QuantityTensor.of(Tensors.vector(6, 7, 8), SI.METER);
    Scalar prog = RealScalar.ZERO;
    MPCPathParameter mpcPathParameter = new MPCPathParameter(prog, Transpose.of(Tensors.of(ctrX, ctrY, ctrR)));
    byte[] bytes = new byte[1000];
    ByteBuffer buffer = ByteBuffer.wrap(bytes);
    mpcPathParameter.insert(buffer);
    buffer.rewind();
    MPCPathParameter mpcPathParameter2 = new MPCPathParameter(buffer);
    Tensor ctrX2 = mpcPathParameter2.getControlPointsX();
    Tensor ctrY2 = mpcPathParameter2.getControlPointsY();
    Tensor ctrR2 = mpcPathParameter2.getControlPointsR();
    assertEquals(ctrX, ctrX2);
    assertEquals(ctrY, ctrY2);
    assertEquals(ctrR, ctrR2);
    assertTrue(mpcPathParameter.getProgressOnPath().equals(mpcPathParameter2.getProgressOnPath()));
  }

  public void testQuery1() {
    Tensor ctrX = QuantityTensor.of(Tensors.vector(0, 1, 2, 6, 2, 10), SI.METER);
    Tensor ctrY = QuantityTensor.of(Tensors.vector(3, 4, 5, 7, 8, 9), SI.METER);
    Tensor ctrR = QuantityTensor.of(Tensors.vector(6, 7, 8, 1, 2, 3), SI.METER);
    MPCBSplineTrack mpcbSplineTrack = new MPCBSplineTrack(Transpose.of(Tensors.of(ctrX, ctrY, ctrR)), true);
    MPCPathParameter mpcPathParameter = mpcbSplineTrack.getPathParameterPreview(6, Tensors.vector(1.1, 4.1).multiply(Quantity.of(1, SI.METER)),
        Quantity.of(0, SI.METER));
    // mpcPathParameter
    assertEquals(mpcPathParameter.getControlPointsX(), ctrX);
    assertEquals(mpcPathParameter.getControlPointsY(), ctrY);
    assertEquals(mpcPathParameter.getControlPointsR(), ctrR);
  }

  public void testQuery2() {
    Tensor ctrX = QuantityTensor.of(Tensors.vector(0, 1, 2), SI.METER);
    Tensor ctrY = QuantityTensor.of(Tensors.vector(3, 4, 5), SI.METER);
    Tensor ctrR = QuantityTensor.of(Tensors.vector(6, 7, 8), SI.METER);
    MPCBSplineTrack mpcbSplineTrack = new MPCBSplineTrack(Transpose.of(Tensors.of(ctrX, ctrY, ctrR)), true);
    long startTime = System.nanoTime();
    MPCPathParameter mpcPathParameter = //
        mpcbSplineTrack.getPathParameterPreview(5, Tensors.vector(0, 3).multiply(Quantity.of(1, SI.METER)), Quantity.of(0, SI.METER));
    long endTime = System.nanoTime();
    assertTrue(endTime - startTime < 300_000);
    // System.out.println(" path progress timing: " + (endTime - startTime) + "[ns]");
    assertEquals(mpcPathParameter.getControlPointsX(), QuantityTensor.of(Tensors.vector(2, 0, 1, 2, 0), SI.METER));
    assertEquals(mpcPathParameter.getControlPointsY(), QuantityTensor.of(Tensors.vector(5, 3, 4, 5, 3), SI.METER));
    assertEquals(mpcPathParameter.getControlPointsR(), QuantityTensor.of(Tensors.vector(8, 6, 7, 8, 6), SI.METER));
  }

  public void testQuery3() {
    Tensor ctrX = QuantityTensor.of(Tensors.vector(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10), SI.METER);
    Tensor ctrY = QuantityTensor.of(Tensors.vector(3, 4, 5, 3, 4, 5, 6, 7, 8, 9, 10), SI.METER);
    Tensor ctrR = QuantityTensor.of(Tensors.vector(6, 7, 8, 3, 4, 5, 6, 7, 8, 9, 10), SI.METER);
    MPCBSplineTrack mpcbSplineTrack = new MPCBSplineTrack(Transpose.of(Tensors.of(ctrX, ctrY, ctrR)), true);
    long startTime = System.nanoTime();
    // MPCPathParameter mpcPathParameter =
    mpcbSplineTrack.getPathParameterPreview(5, Tensors.vector(0, 3).multiply(Quantity.of(1, SI.METER)), Quantity.of(0, SI.METER));
    long endTime = System.nanoTime();
    // System.out.println(endTime - startTime);
    long limit = UserName.is("travis") ? 1_500_000 : 400_000;
    System.out.println(endTime - startTime);
    assertTrue(endTime - startTime < limit);
  }
}
