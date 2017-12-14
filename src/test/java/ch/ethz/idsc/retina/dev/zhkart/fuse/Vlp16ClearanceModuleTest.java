// code by jph
package ch.ethz.idsc.retina.dev.zhkart.fuse;

import ch.ethz.idsc.retina.dev.lidar.LidarSpacialEvent;
import ch.ethz.idsc.retina.dev.rimo.RimoPutEvent;
import ch.ethz.idsc.retina.gui.gokart.GokartStatusEvent;
import junit.framework.TestCase;

public class Vlp16ClearanceModuleTest extends TestCase {
  public void testSimple() {
    Vlp16ClearanceModule vcm = new Vlp16ClearanceModule();
    assertTrue(vcm.putEvent().isPresent());
    assertEquals(vcm.putEvent().get(), RimoPutEvent.PASSIVE);
  }

  public void testEvents() throws Exception {
    Vlp16ClearanceModule vcm = new Vlp16ClearanceModule();
    GokartStatusEvent gokartStatusEvent = new GokartStatusEvent(0.1f);
    vcm.getEvent(gokartStatusEvent);
    assertFalse(vcm.putEvent().isPresent());
    float[] coords = new float[3];
    // ---
    coords[0] = 0;
    coords[1] = 0;
    coords[2] = 0;
    vcm.lidarSpacial(new LidarSpacialEvent(123, coords, 12));
    assertFalse(vcm.putEvent().isPresent());
    // ---
    coords[0] = 1;
    coords[1] = 0;
    coords[2] = 0;
    vcm.lidarSpacial(new LidarSpacialEvent(123, coords, 12));
    assertFalse(vcm.putEvent().isPresent());
    // ---
    coords[0] = 0;
    coords[1] = 1;
    coords[2] = 0;
    vcm.lidarSpacial(new LidarSpacialEvent(123, coords, 12));
    assertTrue(vcm.putEvent().isPresent());
    Thread.sleep(510);
    assertFalse(vcm.putEvent().isPresent());
  }
}
