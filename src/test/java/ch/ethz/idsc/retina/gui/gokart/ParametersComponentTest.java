// code by jph
package ch.ethz.idsc.retina.gui.gokart;

import ch.ethz.idsc.retina.dev.steer.SteerConfig;
import junit.framework.TestCase;

public class ParametersComponentTest extends TestCase {
  public void testSimple() {
    new ParametersComponent(SteerConfig.GLOBAL);
  }
}