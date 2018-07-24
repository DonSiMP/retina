// code by mg
package ch.ethz.idsc.demo.mg.slam.vis;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.stream.IntStream;

import ch.ethz.idsc.demo.mg.slam.SlamParticle;
import ch.ethz.idsc.demo.mg.slam.WayPoint;
import ch.ethz.idsc.demo.mg.slam.algo.SlamProvider;
import ch.ethz.idsc.demo.mg.util.slam.SlamParticleUtil;
import ch.ethz.idsc.gokart.core.pos.GokartPoseInterface;
import ch.ethz.idsc.tensor.Tensor;

/** provides slam visualization static methods
 * 
 * if a function from this enum is needed in the public scope,
 * that function can be extracted to another public class */
/* package */ enum StaticHelper {
  ;
  private static final byte CLEAR_BYTE = -1;

  /** paints a MapProvider map object
   * 
   * @param mapArray representing MapProvider object
   * @param bytes representing frame content
   * @param maxValue [-] max value of the mapArray */
  public static void paintRawMap(double[] mapArray, byte[] bytes, double maxValue) {
    if (maxValue == 0)
      clearFrame(bytes);
    else
      for (int i = 0; i < bytes.length; i++)
        bytes[i] = (byte) (216 + 39 * (1 - mapArray[i] / maxValue));
  }

  /** sets bytes back to CLEAR_BYTE value
   * 
   * @param bytes representing frame content */
  public static void clearFrame(byte[] bytes) {
    IntStream.range(0, bytes.length).forEach(i -> bytes[i] = CLEAR_BYTE);
  }

  /** draws a waypoint object onto the graphics object
   * 
   * @param graphics
   * @param wayPoint
   * @param radius [pixel]
   * @param cornerX [m]
   * @param cornerY [m]
   * @param cellDim [m] */
  public static void drawWayPoint(Graphics2D graphics, WayPoint wayPoint, double radius, double cornerX, double cornerY, double cellDim) {
    double[] framePos = worldToFrame(wayPoint.getWorldPosition(), cornerX, cornerY, cellDim);
    Ellipse2D ellipse = new Ellipse2D.Double( //
        framePos[0] - radius, //
        framePos[1] - radius, //
        2 * radius, 2 * radius);
    graphics.setColor(wayPoint.getVisibility() ? Color.GREEN : Color.ORANGE);
    graphics.fill(ellipse);
  }

  /** transforms world frame coordinates to frame coordinates
   * 
   * @param worldPos [m]
   * @param cornerX [m]
   * @param cornerY [m]
   * @param cellDim [m]
   * @return framePos [pixel] */
  private static double[] worldToFrame(double[] worldPos, double cornerX, double cornerY, double cellDim) {
    double[] framePos = new double[2];
    framePos[0] = (worldPos[0] - cornerX) / cellDim;
    framePos[1] = (worldPos[1] - cornerY) / cellDim;
    return framePos;
  }

  /** draws ellipse representing the gokart onto the graphics object
   * 
   * @param pose
   * @param color
   * @param graphics
   * @param cornerX [m]
   * @param cornerY [m]
   * @param cellDim [m]
   * @param kartLength [m] */
  public static void addGokartPose(Tensor pose, Color color, Graphics2D graphics, double cornerX, double cornerY, double cellDim, double kartLength) {
    double[] worldPos = { pose.Get(0).number().doubleValue(), pose.Get(1).number().doubleValue() };
    double rotAngle = pose.Get(2).number().doubleValue();
    double[] framePos = worldToFrame(worldPos, cornerX, cornerY, cellDim);
    Ellipse2D ellipse = new Ellipse2D.Double(framePos[0] - kartLength / 2, framePos[1] - kartLength / 4, kartLength, kartLength / 2);
    AffineTransform old = graphics.getTransform();
    graphics.rotate(rotAngle, framePos[0], framePos[1]);
    graphics.setColor(color);
    graphics.draw(ellipse);
    graphics.fill(ellipse);
    graphics.setTransform(old);
  }

  /** overlays e.g. go kart pose onto the maps generated by SLAM algorithm
   * 
   * @param slamMapFrames contain the SLAM maps
   * @param slamProvider
   * @param gokartLidarPose
   * @param lidarMappingMode SLAM algorithm parameter
   * @return array of frames */
  public static BufferedImage[] constructFrames(SlamMapFrame[] slamMapFrames, SlamProvider slamProvider, GokartPoseInterface gokartLidarPose,
      boolean lidarMappingMode) {
    slamMapFrames[0].setRawMap(slamProvider.getMap(0));
    slamMapFrames[0].addGokartPose(gokartLidarPose.getPose(), Color.BLACK);
    if (!lidarMappingMode)
      drawParticlePoses(slamMapFrames, slamProvider);
    slamMapFrames[0].addGokartPose(slamProvider.getPoseInterface().getPose(), Color.BLUE);
    // slamMapFrames[1].setProcessedMat(slamProvider.getProcessedMat());
    slamMapFrames[1].setWayPoints(slamProvider.getWayPoints());
    slamMapFrames[1].addGokartPose(slamProvider.getPoseInterface().getPose(), Color.BLUE);
    BufferedImage[] combinedFrames = new BufferedImage[3];
    for (int i = 0; i < 3; i++)
      combinedFrames[i] = slamMapFrames[i].getFrame();
    return combinedFrames;
  }

  /** overlays poses of particles with highest likelihood onto slamMapFrame
   * 
   * @param slamMapFrames
   * @param slamProvider */
  private static void drawParticlePoses(SlamMapFrame[] slamMapFrames, SlamProvider slamProvider) {
    SlamParticle[] slamParticles = slamProvider.getParticles();
    int partNumber = slamParticles.length / 3;
    Arrays.sort(slamParticles, 0, partNumber, SlamParticleUtil.SlamCompare);
    for (int i = 0; i < partNumber; i++)
      slamMapFrames[0].addGokartPose(slamParticles[i].getPose(), Color.RED);
  }
}
