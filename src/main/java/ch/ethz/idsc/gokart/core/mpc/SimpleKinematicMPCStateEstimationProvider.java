// code by mh
package ch.ethz.idsc.gokart.core.mpc;

import java.util.Objects;

import ch.ethz.idsc.gokart.core.pos.GokartPoseEvent;
import ch.ethz.idsc.gokart.core.pos.GokartPoseLcmClient;
import ch.ethz.idsc.gokart.core.pos.GokartPoseListener;
import ch.ethz.idsc.gokart.core.slam.LidarLocalizationModule;
import ch.ethz.idsc.gokart.dev.linmot.LinmotGetEvent;
import ch.ethz.idsc.gokart.dev.linmot.LinmotGetListener;
import ch.ethz.idsc.gokart.dev.linmot.LinmotSocket;
import ch.ethz.idsc.gokart.dev.rimo.RimoGetEvent;
import ch.ethz.idsc.gokart.dev.rimo.RimoGetListener;
import ch.ethz.idsc.gokart.dev.rimo.RimoSocket;
import ch.ethz.idsc.gokart.dev.steer.SteerColumnInterface;
import ch.ethz.idsc.gokart.dev.steer.SteerGetEvent;
import ch.ethz.idsc.gokart.dev.steer.SteerGetListener;
import ch.ethz.idsc.gokart.dev.steer.SteerPutEvent;
import ch.ethz.idsc.gokart.dev.steer.SteerSocket;
import ch.ethz.idsc.gokart.gui.top.ChassisGeometry;
import ch.ethz.idsc.retina.util.math.NonSI;
import ch.ethz.idsc.retina.util.math.SI;
import ch.ethz.idsc.retina.util.sys.ModuleAuto;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.io.Timing;
import ch.ethz.idsc.tensor.qty.Quantity;

/** only needed when no IMU/velocity estimation is available */
/* package */ class SimpleKinematicMPCStateEstimationProvider extends MPCStateEstimationProvider {
  private final LidarLocalizationModule lidarLocalizationModule = //
      ModuleAuto.INSTANCE.getInstance(LidarLocalizationModule.class);
  private Scalar Ux = Quantity.of(0, SI.VELOCITY);
  // assumed to be zero here (Kinematic controller cannot do anything with this information
  private Scalar Uy = Quantity.of(0, SI.VELOCITY);
  private Scalar orientation = RealScalar.of(0);
  private Scalar dotOrientation = Quantity.of(0, SI.PER_SECOND);
  private Scalar xPosition = Quantity.of(0, SI.METER);
  private Scalar yPosition = Quantity.of(0, SI.METER);
  private Scalar w2L = Quantity.of(0, SI.PER_SECOND);
  private Scalar w2R = Quantity.of(0, SI.PER_SECOND);
  private Scalar s = Quantity.of(0, SteerPutEvent.UNIT_ENCODER);
  private Scalar bTemp = Quantity.of(0, NonSI.DEGREE_CELSIUS);
  private Scalar lastUpdate = Quantity.of(0, SI.SECOND);
  private GokartState lastGokartState = null;
  private final LinmotGetListener linmotGetListener = new LinmotGetListener() {
    @Override
    public void getEvent(LinmotGetEvent getEvent) {
      bTemp = getEvent.getWindingTemperatureMax();
      lastUpdate = getTime();
    }
  };
  private final RimoGetListener rimoGetListener = new RimoGetListener() {
    @Override
    public void getEvent(RimoGetEvent getEvent) {
      // in this version we assume no slip (not applicable for dynamic control)
      Ux = ChassisGeometry.GLOBAL.odometryTangentSpeed(getEvent);
      w2L = getEvent.getTireL.getAngularRate_Y();
      w2R = getEvent.getTireR.getAngularRate_Y();
      // also get gyroZ (don't need to update at every step)
      dotOrientation = lidarLocalizationModule.getGyroZ();
      lastUpdate = getTime();
    }
  };
  private final SteerColumnInterface steerColumnInterface = SteerSocket.INSTANCE.getSteerColumnTracker();
  private final SteerGetListener steerGetListener = new SteerGetListener() {
    @Override
    public void getEvent(SteerGetEvent getEvent) {
      if (steerColumnInterface.isSteerColumnCalibrated()) {
        // TODO is this smart? Can we get the info directly from the getEvenet
        s = steerColumnInterface.getSteerColumnEncoderCentered();
        lastUpdate = getTime();
      }
    }
  };
  private final GokartPoseLcmClient gokartPoseLcmClient = new GokartPoseLcmClient();
  private final GokartPoseListener gokartPoseListener = new GokartPoseListener() {
    @Override
    public void getEvent(GokartPoseEvent getEvent) {
      Tensor pose = getEvent.getPose();
      xPosition = pose.Get(0);
      yPosition = pose.Get(1);
      orientation = pose.Get(2);
    }
  };

  protected SimpleKinematicMPCStateEstimationProvider(Timing timing) {
    super(timing);
  }

  @Override
  public GokartState getState() {
    // check if there was an update since the creation of the last gokart state
    if (Objects.isNull(lastGokartState) || !lastGokartState.getTime().equals(lastUpdate))
      lastGokartState = new GokartState( //
          getTime(), //
          Ux, //
          Uy, //
          dotOrientation, //
          xPosition, //
          yPosition, //
          orientation, //
          w2L, //
          w2R, //
          s, //
          bTemp);
    return lastGokartState;
  }

  @Override // from MPCStateEstimationProvider
  void first() {
    LinmotSocket.INSTANCE.addGetListener(linmotGetListener);
    RimoSocket.INSTANCE.addGetListener(rimoGetListener);
    SteerSocket.INSTANCE.addGetListener(steerGetListener);
    gokartPoseLcmClient.addListener(gokartPoseListener);
    gokartPoseLcmClient.startSubscriptions();
  }

  @Override // from MPCStateEstimationProvider
  void last() {
    LinmotSocket.INSTANCE.removeGetListener(linmotGetListener);
    RimoSocket.INSTANCE.removeGetListener(rimoGetListener);
    SteerSocket.INSTANCE.removeGetListener(steerGetListener);
    gokartPoseLcmClient.stopSubscriptions();
  }
}
