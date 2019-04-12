// code by jph
package ch.ethz.idsc.gokart.core.pursuit;

import java.util.Optional;

import ch.ethz.idsc.gokart.core.PutProvider;
import ch.ethz.idsc.gokart.dev.steer.SteerColumnInterface;
import ch.ethz.idsc.gokart.dev.steer.SteerSocket;
import ch.ethz.idsc.owl.ani.api.ProviderRank;
import ch.ethz.idsc.retina.util.StartAndStoppable;

/** base class for pursuit trajectory following motor and steering control */
abstract class PursuitBase<PE> implements StartAndStoppable, PutProvider<PE> {
  private final SteerColumnInterface steerColumnInterface = SteerSocket.INSTANCE.getSteerColumnTracker();
  /** status default false */
  private boolean status = false;

  /** @param status */
  /* package */ final void setOperational(boolean status) {
    this.status = status;
  }

  /** function used in tests
   * 
   * @return status */
  public final boolean private_isOperational() {
    return status;
  }

  @Override // from PutProvider
  public final ProviderRank getProviderRank() {
    return ProviderRank.AUTONOMOUS;
  }

  @Override // from PutProvider
  public final Optional<PE> putEvent() {
    return private_putEvent(steerColumnInterface);
  }

  // function non-private for testing only
  public final Optional<PE> private_putEvent(SteerColumnInterface steerColumnInterface) {
    return private_isOperational() && steerColumnInterface.isSteerColumnCalibrated() //
        // rimo requires RimoGetEvent for velocity control to return non-empty
        // steer always returns non-empty at this point
        ? control(steerColumnInterface)
        : fallback();
  }

  /** @param steerColumnInterface guaranteed to be calibrated
   * @return */
  abstract Optional<PE> control(SteerColumnInterface steerColumnInterface);

  /** @return */
  abstract Optional<PE> fallback();
}
