// code by jph
package ch.ethz.idsc.gokart.core.pursuit;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import ch.ethz.idsc.gokart.lcm.ArrayFloatBlob;
import ch.ethz.idsc.owl.math.state.StateTime;
import ch.ethz.idsc.owl.math.state.TrajectorySample;
import ch.ethz.idsc.tensor.Tensor;

public enum TrajectoryEvents {
  ;
  /** @param byteBuffer
   * @return */
  public static List<TrajectorySample> trajectory(ByteBuffer byteBuffer) {
    Tensor tensor = ArrayFloatBlob.decode(byteBuffer);
    return TrajectoryEvents.trajectory(tensor);
  }

  /** reconstruct trajectory from tensor
   * 
   * @param tensor
   * @return */
  private static List<TrajectorySample> trajectory(Tensor tensor) {
    List<TrajectorySample> trajectory = new ArrayList<>();
    for (Tensor sample : tensor) {
      int last = sample.length() - 1;
      StateTime stateTime = new StateTime(sample.extract(0, last), sample.Get(last));
      TrajectorySample trajectorySample = new TrajectorySample(stateTime, null);
      trajectory.add(trajectorySample);
    }
    return trajectory;
  }
}
