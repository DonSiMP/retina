// code by jph, gjoel
package ch.ethz.idsc.retina.lidar;

/** the spacial provider listens to ray data and transforms information to events
 * with time and 3d, or 2d coordinates */
public interface LidarSpacialProvider <T extends LidarSpacialEvent> extends LidarRayDataListener {
  void addListener(LidarSpacialListener<T> lidarSpacialListener);
}
