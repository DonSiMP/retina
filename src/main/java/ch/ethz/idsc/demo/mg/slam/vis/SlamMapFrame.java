// code by mg
package ch.ethz.idsc.demo.mg.slam.vis;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.List;

import org.bytedeco.javacpp.opencv_core.Mat;

import ch.ethz.idsc.demo.mg.slam.MapProvider;
import ch.ethz.idsc.demo.mg.slam.SlamConfig;
import ch.ethz.idsc.demo.mg.slam.WayPoint;
import ch.ethz.idsc.demo.mg.util.slam.SlamOpenCVUtil;
import ch.ethz.idsc.retina.util.img.ImageReflect;
import ch.ethz.idsc.tensor.Tensor;

/** gives an image of the maps generated by the SLAM algorithm */
public class SlamMapFrame {
  private static final byte CLEAR_BYTE = (byte) 255;
  private static final byte ORANGE = (byte) -52;
  private static final byte GREEN = (byte) 30;
  private static final byte BLUE = (byte) 5;
  private static final byte RED = (byte) -76;
  private static final byte[] LOOKUP = { ORANGE, GREEN, BLUE };
  // ---
  private final BufferedImage bufferedImage;
  private final Graphics2D graphics;
  private final byte[] bytes;
  private final double wayPointRadius;
  private final double cornerX;
  private final double cornerY;
  private final double cellDim;
  private final int kartLength;
  private final int frameWidth;
  private final int frameHeight;
  private final int numberOfCells;

  public SlamMapFrame(SlamConfig slamConfig) {
    frameWidth = slamConfig.dimX.divide(slamConfig.cellDim).number().intValue();
    frameHeight = slamConfig.dimY.divide(slamConfig.cellDim).number().intValue();
    numberOfCells = frameWidth * frameHeight;
    wayPointRadius = slamConfig.wayPointRadius.number().doubleValue();
    cornerX = slamConfig.corner.Get(0).number().doubleValue();
    cornerY = slamConfig.corner.Get(1).number().doubleValue();
    cellDim = slamConfig.cellDim.number().doubleValue();
    kartLength = (int) (slamConfig.kartSize.number().doubleValue() / cellDim);
    bufferedImage = new BufferedImage(frameWidth, frameHeight, BufferedImage.TYPE_BYTE_INDEXED);
    graphics = bufferedImage.createGraphics();
    DataBufferByte dataBufferByte = (DataBufferByte) bufferedImage.getRaster().getDataBuffer();
    bytes = dataBufferByte.getData();
  }

  public void setRawMap(MapProvider map) {
    if (map.getNumberOfCells() != numberOfCells)
      System.out.println("FATAL: something went wrong!");
    StaticHelper.paintRawMap(map.getMapArray(), bytes, map.getMaxValue());
  }

  public void addGokartPose(Tensor pose, Color color) {
    StaticHelper.addGokartPose(pose, color, graphics, cornerX, cornerY, cellDim, kartLength);
  }

  // return flipped image such that x axis points right and y axis upwards
  public BufferedImage getFrame() {
    return ImageReflect.flipHorizontal(bufferedImage);
  }

  public void setWayPoints(List<WayPoint> wayPoints) {
    StaticHelper.clearFrame(bytes);
    for (int i = 0; i < wayPoints.size(); i++)
      StaticHelper.drawWayPoint(graphics, wayPoints.get(i), wayPointRadius, cornerX, cornerY, cellDim);
  }

  public void setProcessedMat(Mat processedMat) {
    byte[] processedByteArray = SlamOpenCVUtil.matToByteArray(processedMat);
    for (int i = 0; i < bytes.length; i++)
      if (processedByteArray[i] == 0)
        bytes[i] = CLEAR_BYTE;
      else {
        int labelID = processedByteArray[i] % 3;
        bytes[i] = LOOKUP[labelID];
      }
  }
}