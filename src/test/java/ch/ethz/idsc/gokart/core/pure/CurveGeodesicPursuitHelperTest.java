// code by jph, gjoel
package ch.ethz.idsc.gokart.core.pure;

import java.util.Optional;

import ch.ethz.idsc.gokart.gui.top.ChassisGeometry;
import ch.ethz.idsc.retina.util.math.SI;
import ch.ethz.idsc.sophus.group.Se2GroupElement;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.opt.TensorUnaryOperator;
import ch.ethz.idsc.tensor.qty.Quantity;
import ch.ethz.idsc.tensor.sca.Chop;
import ch.ethz.idsc.tensor.sca.Clips;
import junit.framework.TestCase;

public class CurveGeodesicPursuitHelperTest extends TestCase {
  // TODO add more tests
  public void testSpecific1() throws Exception {
    Tensor pose = Tensors.fromString("{35.1[m], 44.9[m], 1}");
    Scalar speed = Quantity.of(1, SI.VELOCITY);
    Optional<Scalar> optional = CurveGeodesicPursuitHelper.getRatio(pose, speed, DubendorfCurve.TRACK_OVAL_SE2, true, //
        PursuitConfig.GLOBAL.geodesicInterface, PursuitConfig.GLOBAL.trajectoryEntryFinder, PursuitConfig.ratioLimits());
    Scalar ratio = optional.get();
    Scalar angle = ChassisGeometry.GLOBAL.steerAngleForTurningRatio(ratio);
    assertTrue(Clips.interval( //
        Quantity.of(-0.38, ""), //
        Quantity.of(-0.37, "")).isInside(angle));
  }

  public void testSpecific2() throws Exception {
    Tensor pose = Tensors.fromString("{35.1[m], 44.9[m], 0.9}");
    Scalar speed = Quantity.of(1, SI.VELOCITY);
    Optional<Scalar> optional = CurveGeodesicPursuitHelper.getRatio(pose, speed, DubendorfCurve.TRACK_OVAL_SE2, true, //
        PursuitConfig.GLOBAL.geodesicInterface, PursuitConfig.GLOBAL.trajectoryEntryFinder, PursuitConfig.ratioLimits());
    Scalar ratio = optional.get();
    Scalar angle = ChassisGeometry.GLOBAL.steerAngleForTurningRatio(ratio);
    assertTrue(Clips.interval( //
        Quantity.of(-0.37, ""), //
        Quantity.of(-0.36, "")).isInside(angle));
  }

  public void testTransform() {
    Se2GroupElement se2GroupElement = new Se2GroupElement(Tensors.fromString("{2[m],3[m],1}"));
    TensorUnaryOperator tensorUnaryOperator = se2GroupElement.inverse()::combine;
    Tensor curve = Tensors.fromString("{{2[m],3[m],1},{3[m],4[m],2}}");
    Tensor local = Tensor.of(curve.stream().map(tensorUnaryOperator));
    assertTrue(Chop.NONE.allZero(local.get(0)));
  }
}
