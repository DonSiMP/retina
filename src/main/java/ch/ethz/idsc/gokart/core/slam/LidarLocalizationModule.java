// code by jph
package ch.ethz.idsc.gokart.core.slam;

import ch.ethz.idsc.gokart.core.pos.GokartPoseEvent;
import ch.ethz.idsc.gokart.core.pos.PoseVelocityInterface;
import ch.ethz.idsc.gokart.gui.top.SensorsConfig;
import ch.ethz.idsc.gokart.lcm.imu.Vmu931ImuLcmClient;
import ch.ethz.idsc.gokart.lcm.lidar.Vlp16LcmClient;
import ch.ethz.idsc.retina.util.sys.AbstractModule;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;

/** match the most recent lidar scan to static geometry of a pre-recorded map
 * the module runs a separate thread. on a standard pc the matching takes 0.017[s] on average */
public class LidarLocalizationModule extends AbstractModule implements PoseVelocityInterface {
  private final Vmu931ImuLcmClient vmu931ImuLcmClient = new Vmu931ImuLcmClient();
  private final LidarLocalizationCore lidarLocalizationCore = new LidarLocalizationCore();
  private final Vlp16LcmClient vlp16LcmClient = //
      SensorsConfig.GLOBAL.vlp16LcmClient(lidarLocalizationCore.velodyneDecoder);

  @Override // from AbstractModule
  protected void first() {
    vmu931ImuLcmClient.addListener(lidarLocalizationCore);
    vmu931ImuLcmClient.startSubscriptions();
    // ---
    vlp16LcmClient.startSubscriptions();
    lidarLocalizationCore.isLaunched = true;
    lidarLocalizationCore.thread.start();
  }

  @Override // from AbstractModule
  protected void last() {
    lidarLocalizationCore.isLaunched = false;
    lidarLocalizationCore.thread.interrupt();
    vlp16LcmClient.stopSubscriptions();
    vmu931ImuLcmClient.stopSubscriptions();
  }

  /** @return */
  public boolean isTracking() {
    return lidarLocalizationCore.isTracking();
  }

  /** @param tracking */
  public void setTracking(boolean tracking) {
    lidarLocalizationCore.setTracking(tracking);
  }

  /** flag snap */
  public void flagSnap() {
    lidarLocalizationCore.flagSnap();
  }

  /***************************************************/
  @Override // from GokartPoseInterface
  public Tensor getPose() {
    return lidarLocalizationCore.getPose();
  }

  @Override // from PoseVelocityInterface
  public Tensor getVelocity() {
    return lidarLocalizationCore.getVelocity();
  }

  @Override // from PoseVelocityInterface
  public Scalar getGyroZ() {
    return lidarLocalizationCore.getGyroZ();
  }

  public GokartPoseEvent createPoseEvent() {
    return lidarLocalizationCore.createPoseEvent();
  }

  /** function called when operator initializes pose
   * 
   * @param pose */
  public void resetPose(Tensor pose) {
    lidarLocalizationCore.resetPose(pose);
  }
}
