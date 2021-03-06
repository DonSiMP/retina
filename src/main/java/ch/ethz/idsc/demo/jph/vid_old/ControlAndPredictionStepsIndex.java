// code by jph
package ch.ethz.idsc.demo.jph.vid_old;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.TreeMap;

import ch.ethz.idsc.gokart.core.mpc.ControlAndPredictionSteps;
import ch.ethz.idsc.gokart.core.mpc.ControlAndPredictionStepsMessage;
import ch.ethz.idsc.gokart.gui.GokartLcmChannel;
import ch.ethz.idsc.gokart.lcm.OfflineLogListener;
import ch.ethz.idsc.gokart.lcm.OfflineLogPlayer;
import ch.ethz.idsc.retina.util.Refactor;
import ch.ethz.idsc.tensor.Scalar;

// class is likely to be obsolete in the future
@Refactor
/* package */ class ControlAndPredictionStepsIndex implements OfflineLogListener {
  /** @param file
   * @return navigable map with quantity with unit [s] as key type
   * @throws IOException */
  public static NavigableMap<Scalar, ControlAndPredictionSteps> build(File file) throws IOException {
    ControlAndPredictionStepsIndex controlAndPredictionIndex = new ControlAndPredictionStepsIndex();
    OfflineLogPlayer.process(file, controlAndPredictionIndex);
    return Collections.unmodifiableNavigableMap(controlAndPredictionIndex.navigableMap);
  }

  // ---
  private final NavigableMap<Scalar, ControlAndPredictionSteps> navigableMap = new TreeMap<>();

  @Override // from OfflineLogListener
  public void event(Scalar time, String channel, ByteBuffer byteBuffer) {
    if (channel.equals(GokartLcmChannel.MPC_FORCES_CNS)) {
      ControlAndPredictionStepsMessage controlAndPredictionStepsMessage = //
          new ControlAndPredictionStepsMessage(byteBuffer);
      navigableMap.put(time, controlAndPredictionStepsMessage.getPayload());
    }
  }
}
