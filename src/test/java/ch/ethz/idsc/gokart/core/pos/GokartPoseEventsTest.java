// code by jph
package ch.ethz.idsc.gokart.core.pos;

import ch.ethz.idsc.retina.util.math.SI;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.qty.Quantity;
import junit.framework.TestCase;

public class GokartPoseEventsTest extends TestCase {
  public void testSimple() {
    GokartPoseEvent gokartPoseEvent = GokartPoseEvents.create( //
        Tensors.fromString("{1[m], 2[m], 3}"), RealScalar.ONE);
    assertEquals(gokartPoseEvent.getVelocityXY(), Tensors.fromString("{0[m*s^-1],0[m*s^-1]}"));
    assertEquals(gokartPoseEvent.getGyroZ(), Quantity.of(0, SI.PER_SECOND));
    assertTrue(gokartPoseEvent.hasVelocity());
    assertEquals(gokartPoseEvent.asVector(), Tensors.vector(1, 2, 3, 1, 0, 0, 0));
  }

  public void testMotionless() {
    GokartPoseEvent gokartPoseEvent = GokartPoseEvents.motionlessUninitialized();
    assertTrue(gokartPoseEvent.hasVelocity());
    assertEquals(gokartPoseEvent.asVector(), Tensors.vector(0, 0, 0, 0, 0, 0, 0));
  }

  public void testExtended() {
    GokartPoseEvent gokartPoseEvent = GokartPoseEvents.create( //
        Tensors.fromString("{1[m], 2[m], 3}"), RealScalar.ONE, //
        Tensors.fromString("{4[m*s^-1], 5[m*s^-1]}"), //
        Quantity.of(6, SI.PER_SECOND));
    assertEquals(gokartPoseEvent.getVelocityXY(), Tensors.fromString("{4[m*s^-1],5[m*s^-1]}"));
    assertEquals(gokartPoseEvent.getGyroZ(), Quantity.of(6, SI.PER_SECOND));
    assertTrue(gokartPoseEvent.hasVelocity());
    assertEquals(gokartPoseEvent.asVector(), Tensors.vector(1, 2, 3, 1, 4, 5, 6));
  }

  public void testNullFail() {
    try {
      GokartPoseEvents.create(null, RealScalar.ONE);
      fail();
    } catch (Exception exception) {
      // ---
    }
  }
}
