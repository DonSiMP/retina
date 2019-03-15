// code by jph, gjoel
package ch.ethz.idsc.retina.lidar;

@FunctionalInterface
public interface LidarSpacialListener <T extends LidarSpacialEvent> {
  void lidarSpacial(T lidarSpacialEvent);
}
