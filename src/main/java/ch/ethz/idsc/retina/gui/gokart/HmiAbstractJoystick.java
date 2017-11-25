// code by jph
package ch.ethz.idsc.retina.gui.gokart;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import ch.ethz.idsc.retina.dev.joystick.GokartJoystickInterface;
import ch.ethz.idsc.retina.dev.joystick.JoystickEvent;
import ch.ethz.idsc.retina.dev.joystick.JoystickListener;
import ch.ethz.idsc.retina.dev.linmot.LinmotGetEvent;
import ch.ethz.idsc.retina.dev.linmot.LinmotGetListener;
import ch.ethz.idsc.retina.dev.linmot.LinmotPutEvent;
import ch.ethz.idsc.retina.dev.linmot.LinmotPutHelper;
import ch.ethz.idsc.retina.dev.linmot.LinmotPutListener;
import ch.ethz.idsc.retina.dev.linmot.LinmotPutProvider;
import ch.ethz.idsc.retina.dev.rimo.RimoGetTire;
import ch.ethz.idsc.retina.dev.rimo.RimoPutProvider;
import ch.ethz.idsc.retina.dev.rimo.RimoRateControllerWrap;
import ch.ethz.idsc.retina.dev.steer.SteerColumnTracker;
import ch.ethz.idsc.retina.dev.steer.SteerPositionControl;
import ch.ethz.idsc.retina.dev.steer.SteerPutEvent;
import ch.ethz.idsc.retina.dev.steer.SteerPutProvider;
import ch.ethz.idsc.retina.dev.steer.SteerSocket;
import ch.ethz.idsc.retina.dev.zhkart.ProviderRank;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.qty.Quantity;

public abstract class HmiAbstractJoystick implements JoystickListener {
  public static final List<Scalar> SPEEDS = Arrays.asList( //
      Quantity.of(0, RimoGetTire.UNIT_RATE), //
      Quantity.of(10, RimoGetTire.UNIT_RATE), //
      Quantity.of(50, RimoGetTire.UNIT_RATE), //
      Quantity.of(100, RimoGetTire.UNIT_RATE), //
      Quantity.of(200, RimoGetTire.UNIT_RATE) //
  );
  /** no joystick info older than watchdog period is used */
  private static final int WATCHDOG_MS = 250; // 250[ms]
  // ---
  public final RimoRateControllerWrap rimoRateControllerWrap = new RimoRateControllerWrap();
  private final SteerPositionControl positionController = new SteerPositionControl();
  private GokartJoystickInterface _joystick;
  private long tic_joystick;
  private LinmotGetEvent _linmotGetEvent;
  private LinmotPutEvent _linmotPutEvent;
  private Scalar speedLimit = SPEEDS.get(0);

  final Optional<GokartJoystickInterface> getJoystick() {
    return Optional.ofNullable(now_ms() < tic_joystick + WATCHDOG_MS ? _joystick : null);
  }

  @Override
  public final void joystick(JoystickEvent joystickEvent) {
    _joystick = (GokartJoystickInterface) joystickEvent;
    tic_joystick = now_ms();
  }

  /** steering */
  public final SteerPutProvider steerPutProvider = new SteerPutProvider() {
    @Override
    public Optional<SteerPutEvent> putEvent() {
      Optional<GokartJoystickInterface> optional = getJoystick();
      if (optional.isPresent()) {
        final SteerColumnTracker steerColumnTracker = SteerSocket.INSTANCE.getSteerColumnTracker();
        if (steerColumnTracker.isCalibrated()) {
          final Scalar currAngle = steerColumnTracker.getEncoderValueCentered();
          Scalar desPos = RealScalar.of(optional.get().getSteerLeft()).multiply(SteerColumnTracker.MAX_SCE);
          final Scalar torqueCmd = //
              positionController.iterate(Quantity.of(desPos.subtract(currAngle), SteerPutEvent.UNIT_ENCODER));
          return Optional.of(SteerPutEvent.createOn(torqueCmd));
        }
      }
      return Optional.empty();
    }

    @Override
    public ProviderRank getProviderRank() {
      return ProviderRank.MANUAL;
    }
  };
  public final LinmotGetListener linmotGetListener = new LinmotGetListener() {
    @Override
    public void getEvent(LinmotGetEvent linmotGetEvent) {
      _linmotGetEvent = linmotGetEvent;
    }
  };
  public final LinmotPutListener linmotPutListener = new LinmotPutListener() {
    @Override
    public void putEvent(LinmotPutEvent linmotPutEvent) {
      _linmotPutEvent = linmotPutEvent;
    }
  };
  /** breaking */
  public final LinmotPutProvider linmotPutProvider = new LinmotPutProvider() {
    @Override
    public Optional<LinmotPutEvent> putEvent() {
      Optional<GokartJoystickInterface> optional = getJoystick();
      if (optional.isPresent()) {
        // TODO see if this status check is mandatory
        boolean status = true;
        status &= Objects.nonNull(_linmotGetEvent) && _linmotGetEvent.isOperational();
        status &= Objects.nonNull(_linmotPutEvent) && _linmotPutEvent.isOperational();
        if (status)
          return Optional.of(LinmotPutHelper.operationToRelativePosition(breakStrength(optional.get())));
      }
      return Optional.empty();
    }

    @Override
    public ProviderRank getProviderRank() {
      return ProviderRank.MANUAL;
    }
  };

  public abstract RimoPutProvider getRimoPutProvider();

  /** @return value in the interval [0, 1]
   * 0 means no break, and 1 means all the way */
  protected abstract double breakStrength(GokartJoystickInterface gokartJoystickInterface);

  public final void setSpeedLimit(Scalar speedLimit) {
    this.speedLimit = speedLimit;
  }

  /** @return quantity with unit rad*s^-1 */
  public final Scalar getSpeedLimit() {
    return speedLimit;
  }

  private static long now_ms() {
    return System.currentTimeMillis();
  }
}
