// code by mg
package ch.ethz.idsc.demo.mg.slam.offline;

import java.nio.ByteBuffer;

import ch.ethz.idsc.demo.mg.slam.AbstractSlamWrap;
import ch.ethz.idsc.demo.mg.slam.config.SlamDvsConfig;
import ch.ethz.idsc.gokart.dev.rimo.RimoGetEvent;
import ch.ethz.idsc.gokart.lcm.OfflineLogListener;
import ch.ethz.idsc.gokart.lcm.autobox.RimoLcmServer;
import ch.ethz.idsc.tensor.Scalar;

/** wrapper to run SLAM algorithm with offline log files */
/* package */ class OfflineSlamWrap extends AbstractSlamWrap implements OfflineLogListener {
  private static final String CHANNEL_DVS = SlamDvsConfig.eventCamera.slamCoreConfig.dvsConfig.channel_DVS;

  public OfflineSlamWrap() {
    start();
  }

  @Override // from OfflineLogListener
  public void event(Scalar time, String channel, ByteBuffer byteBuffer) {
    if (channel.equals(CHANNEL_DVS))
      dvsLcmClient.messageReceived(byteBuffer);
    else //
    if (channel.equals(RimoLcmServer.CHANNEL_GET))
      gokartPoseOdometryDemo.getEvent(new RimoGetEvent(byteBuffer));
  }

  @Override // from AbstractSlamWrap
  protected void protected_start() {
    // ---
  }

  @Override // from AbstractSlamWrap
  protected void protected_stop() {
    slamCoreContainer.stop();
  }
}
