// code by jph
package ch.ethz.idsc.gokart.gui.top;

import ch.ethz.idsc.gokart.calib.steer.RimoTireConfiguration;
import ch.ethz.idsc.gokart.dev.rimo.RimoGetEvent;
import ch.ethz.idsc.gokart.dev.rimo.RimoGetEvents;
import ch.ethz.idsc.owl.car.math.DifferentialSpeed;
import ch.ethz.idsc.retina.util.math.Magnitude;
import ch.ethz.idsc.retina.util.math.SI;
import ch.ethz.idsc.tensor.DoubleScalar;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.qty.Quantity;
import ch.ethz.idsc.tensor.qty.QuantityMagnitude;
import ch.ethz.idsc.tensor.qty.QuantityUnit;
import ch.ethz.idsc.tensor.qty.Unit;
import ch.ethz.idsc.tensor.sca.Chop;
import ch.ethz.idsc.tensor.sca.Clips;
import ch.ethz.idsc.tensor.sca.ScalarUnaryOperator;
import junit.framework.TestCase;

public class ChassisGeometryTest extends TestCase {
  private static final ScalarUnaryOperator IN_CM = QuantityMagnitude.SI().in("cm");

  public void testSimple() {
    DifferentialSpeed differentialSpeed = //
        ChassisGeometry.GLOBAL.getDifferentialSpeed();
    differentialSpeed.pair(RealScalar.ONE, RealScalar.of(.3));
  }

  public void testSingleton() {
    Scalar xAxleDistance = ChassisGeometry.GLOBAL.xAxleDistanceMeter();
    Clips.interval(1.1, 1.25).requireInside(xAxleDistance);
    ChassisGeometry.GLOBAL.yTireFrontMeter();
  }

  public void testxTipMeter() {
    Scalar xTipMeter = ChassisGeometry.GLOBAL.xTipMeter();
    assertEquals(xTipMeter, DoubleScalar.of(1.75));
  }

  public void testyHalfWidthMeter() {
    Scalar scalar = ChassisGeometry.GLOBAL.yHalfWidthMeter();
    assertTrue(scalar instanceof RealScalar);
  }

  public void testSteerAngleTowardsLeft() {
    Scalar angle = ChassisGeometry.GLOBAL.steerAngleForTurningRatio(Quantity.of(0.3, SI.PER_METER));
    assertTrue(Chop._13.close(RealScalar.of(0.34289723785565446), angle));
  }

  public void testSteerAngleTowardsRight() {
    Scalar angle = ChassisGeometry.GLOBAL.steerAngleForTurningRatio(Quantity.of(-.2, SI.PER_METER));
    assertTrue(Chop._13.close(RealScalar.of(-0.2336530501796457), angle));
  }

  public void testSteerAngleStraight() {
    Scalar angle = ChassisGeometry.GLOBAL.steerAngleForTurningRatio(Quantity.of(0, SI.PER_METER));
    assertTrue(Chop._13.close(RealScalar.of(0), angle));
  }

  public void testTireWidthFront() {
    Scalar width = RimoTireConfiguration.FRONT.halfWidth().multiply(RealScalar.of(2));
    assertEquals(IN_CM.apply(width), RealScalar.of(13)); // cm
  }

  public void testTireWidthRear() {
    Scalar width = RimoTireConfiguration._REAR.halfWidth().multiply(RealScalar.of(2));
    assertEquals(IN_CM.apply(width), RealScalar.of(19.5)); // cm
  }

  public void testTireWidthContactFront() {
    Scalar width = ChassisGeometry.GLOBAL.tireHalfWidthContactFront.multiply(RealScalar.of(2));
    assertEquals(IN_CM.apply(width), RealScalar.of(9)); // cm
  }

  public void testTireWidthContactRear() {
    Scalar width = ChassisGeometry.GLOBAL.tireHalfWidthContactRear.multiply(RealScalar.of(2));
    assertEquals(IN_CM.apply(width), RealScalar.of(13.5)); // cm
  }

  public void testOdometry() {
    RimoGetEvent rimoGetEvent = RimoGetEvents.create(100, 200);
    Scalar speed = ChassisGeometry.GLOBAL.odometryTangentSpeed(rimoGetEvent);
    assertEquals(QuantityUnit.of(speed), Unit.of("s^-1*m"));
    Scalar vel = Magnitude.VELOCITY.apply(speed);
    assertTrue(Chop._04.close(vel, RealScalar.of(0.3)));
    Scalar rate = ChassisGeometry.GLOBAL.odometryTurningRate(rimoGetEvent);
    assertEquals(QuantityUnit.of(rate), Unit.of("s^-1"));
    Scalar ome = Magnitude.PER_SECOND.apply(rate);
    assertTrue(Chop._04.close(ome, RealScalar.of(0.18518518518518517)));
  }

  public void testAckermann() {
    Tensor pair = ChassisGeometry.GLOBAL.getAckermannSteering().pair(RealScalar.of(0.3));
    assertTrue(Chop._10.close(pair, Tensors.vector(0.3397325320025735, 0.2683854870479421)));
  }
}
