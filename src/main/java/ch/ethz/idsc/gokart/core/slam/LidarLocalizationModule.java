// code by jph
package ch.ethz.idsc.gokart.core.slam;

import java.nio.FloatBuffer;
import java.util.Objects;
import java.util.Optional;

import ch.ethz.idsc.gokart.core.pos.GokartPoseEvent;
import ch.ethz.idsc.gokart.core.pos.GokartPoseEvents;
import ch.ethz.idsc.gokart.core.pos.LocalizationConfig;
import ch.ethz.idsc.gokart.core.pos.PoseVelocityInterface;
import ch.ethz.idsc.gokart.gui.top.SensorsConfig;
import ch.ethz.idsc.gokart.lcm.imu.Vmu931ImuLcmClient;
import ch.ethz.idsc.gokart.lcm.lidar.Vlp16LcmHandler;
import ch.ethz.idsc.retina.imu.vmu931.Vmu931ImuFrame;
import ch.ethz.idsc.retina.imu.vmu931.Vmu931ImuFrameListener;
import ch.ethz.idsc.retina.lidar.LidarAngularFiringCollector;
import ch.ethz.idsc.retina.lidar.LidarRayBlockEvent;
import ch.ethz.idsc.retina.lidar.LidarRayBlockListener;
import ch.ethz.idsc.retina.lidar.LidarRotationProvider;
import ch.ethz.idsc.retina.lidar.LidarSpacialProvider;
import ch.ethz.idsc.retina.util.math.SI;
import ch.ethz.idsc.retina.util.sys.AbstractModule;
import ch.ethz.idsc.sophus.filter.GeodesicIIR1Filter;
import ch.ethz.idsc.sophus.group.LieDifferences;
import ch.ethz.idsc.sophus.group.RnGeodesic;
import ch.ethz.idsc.sophus.group.Se2CoveringExponential;
import ch.ethz.idsc.sophus.group.Se2Group;
import ch.ethz.idsc.tensor.DoubleScalar;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.qty.Quantity;
import ch.ethz.idsc.tensor.sca.Clips;

/** match the most recent lidar scan to static geometry of a pre-recorded map
 * the module runs a separate thread. on a standard pc the matching takes 0.017[s] on average */
// TODO JPH split class in two classes
public class LidarLocalizationModule extends AbstractModule implements //
    LidarRayBlockListener, Vmu931ImuFrameListener, Runnable, PoseVelocityInterface {
  private static final LieDifferences LIE_DIFFERENCES = //
      new LieDifferences(Se2Group.INSTANCE, Se2CoveringExponential.INSTANCE);
  private static final LidarGyroLocalization LIDAR_GYRO_LOCALIZATION = //
      LidarGyroLocalization.of(LocalizationConfig.getPredefinedMap());
  // ---
  private final Vmu931ImuLcmClient vmu931ImuLcmClient = new Vmu931ImuLcmClient();
  private final Vmu931Odometry vmu931Odometry = new Vmu931Odometry(SensorsConfig.getPlanarVmu931Imu());
  private final Vlp16LcmHandler vlp16LcmHandler = SensorsConfig.GLOBAL.vlp16LcmHandler();
  private final Thread thread = new Thread(this);
  // ---
  private boolean tracking = false;
  private boolean flagSnap = false;
  /** tear down flag to stop thread */
  private boolean isLaunched = true;
  /** points_ferry is null or a matrix with dimension Nx2
   * containing the cross-section of the static geometry
   * with the horizontal plane at height of the lidar */
  private Tensor points2d_ferry = null;
  private Scalar quality = RealScalar.ZERO;

  /** @return */
  public boolean isTracking() {
    return tracking;
  }

  /** @param tracking */
  public void setTracking(boolean tracking) {
    this.tracking = tracking;
  }

  /** flag snap */
  public void flagSnap() {
    flagSnap = true;
  }

  @Override // from AbstractModule
  protected void first() {
    vmu931ImuLcmClient.addListener(this);
    vmu931ImuLcmClient.startSubscriptions();
    // ---
    LidarAngularFiringCollector lidarAngularFiringCollector = new LidarAngularFiringCollector(2304, 2);
    LidarSpacialProvider lidarSpacialProvider = LocalizationConfig.GLOBAL.planarEmulatorVlp16();
    lidarSpacialProvider.addListener(lidarAngularFiringCollector);
    LidarRotationProvider lidarRotationProvider = new LidarRotationProvider();
    lidarRotationProvider.addListener(lidarAngularFiringCollector);
    lidarAngularFiringCollector.addListener(this);
    vlp16LcmHandler.velodyneDecoder.addRayListener(lidarSpacialProvider);
    vlp16LcmHandler.velodyneDecoder.addRayListener(lidarRotationProvider);
    // ---
    vlp16LcmHandler.startSubscriptions();
    thread.start();
  }

  @Override // from AbstractModule
  protected void last() {
    isLaunched = false;
    thread.interrupt();
    vlp16LcmHandler.stopSubscriptions();
    vmu931ImuLcmClient.stopSubscriptions();
  }

  @Override // from LidarRayBlockListener
  public synchronized void lidarRayBlock(LidarRayBlockEvent lidarRayBlockEvent) { // receive 2D block event
    if (flagSnap || tracking) {
      FloatBuffer floatBuffer = lidarRayBlockEvent.floatBuffer;
      points2d_ferry = Tensors.vector(i -> Tensors.of( //
          DoubleScalar.of(floatBuffer.get()), //
          DoubleScalar.of(floatBuffer.get())), lidarRayBlockEvent.size());
      thread.interrupt();
    }
    if (!tracking)
      vmu931Odometry.inertialOdometry.resetVelocity();
  }

  /***************************************************/
  /** the constant 0.1 was established in post-processing
   * with mh and jph to filter out spikes in the gyroZ signal */
  private GeodesicIIR1Filter geodesicIIR1Filter = //
      new GeodesicIIR1Filter(RnGeodesic.INSTANCE, RealScalar.of(0.1));
  private Scalar gyroZ_filtered = Quantity.of(0.0, SI.PER_SECOND);

  // DelayedQueue<Vmu931ImuFrame> delayedQueue = new DelayedQueue<>(0);
  @Override // from Vmu931ImuFrameListener
  public void vmu931ImuFrame(Vmu931ImuFrame vmu931ImuFrame) {
    vmu931Odometry.vmu931ImuFrame(vmu931ImuFrame);
    Scalar vmu931_gyroZ = vmu931Odometry.inertialOdometry.getGyroZ();
    gyroZ_filtered = geodesicIIR1Filter.apply(vmu931_gyroZ).Get();
  }

  /***************************************************/
  @Override // from Runnable
  public void run() {
    while (isLaunched) {
      Tensor points = points2d_ferry;
      if (Objects.nonNull(points)) {
        points2d_ferry = null;
        fit(points);
      } else
        try {
          Thread.sleep(2000); // is interrupted once data arrives
        } catch (Exception exception) {
          // ---
        }
    }
  }

  private SlamResult prevResult = null;

  public void fit(Tensor points) {
    Optional<SlamResult> optional = LIDAR_GYRO_LOCALIZATION.handle(getPose(), getGyroZ(), points);
    if (optional.isPresent()) {
      SlamResult slamResult = optional.get();
      quality = slamResult.getMatchRatio();
      boolean matchOk = Scalars.lessThan(RealScalar.of(0.7), quality);
      // System.out.println("flagSnap=" + flagSnap);
      if (matchOk || flagSnap) {
        // blend pose
        Scalar blend = flagSnap //
            ? RealScalar.of(1)
            : RealScalar.of(0.1); // TODO JPH magic const
        vmu931Odometry.inertialOdometry.blendPose(slamResult.getTransform(), blend);
        if (Objects.nonNull(prevResult)) {
          // blend velocity
          Tensor velXY = LIE_DIFFERENCES.pair( //
              prevResult.getTransform(), //
              slamResult.getTransform()) //
              .extract(0, 2).multiply(SensorsConfig.GLOBAL.vlp16_rate);
          // System.out.println("---");
          // TODO JPH/MH magic const
          vmu931Odometry.inertialOdometry.blendVelocity(velXY, RealScalar.of(0.01));
        }
        prevResult = slamResult;
        flagSnap = false;
      } else
        prevResult = null;
    } else {
      quality = Clips.unit().apply(quality.subtract(RealScalar.of(0.05)));
      prevResult = null;
    }
  }

  /***************************************************/
  @Override // from GokartPoseInterface
  public Tensor getPose() {
    return vmu931Odometry.inertialOdometry.getPose();
  }

  @Override // from PoseVelocityInterface
  public Tensor getVelocityXY() {
    return vmu931Odometry.inertialOdometry.getVelocityXY();
  }

  @Override // from PoseVelocityInterface
  public Scalar getGyroZ() {
    return gyroZ_filtered;
  }

  public GokartPoseEvent createPoseEvent() {
    return GokartPoseEvents.getPoseEvent(getPose(), quality, getVelocityXY(), getGyroZ());
  }

  /** function called when operator initializes pose
   * 
   * @param pose */
  public void resetPose(Tensor pose) {
    // System.out.println("reset pose=" + pose.map(Round._5));
    vmu931Odometry.inertialOdometry.resetPose(pose);
    quality = RealScalar.ONE;
    prevResult = null;
  }
}
