// code by ynager
package ch.ethz.idsc.demo.yn;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import javax.imageio.ImageIO;

import ch.ethz.idsc.gokart.core.pos.GokartPoseEvent;
import ch.ethz.idsc.gokart.core.pos.GokartPoseLcmServer;
import ch.ethz.idsc.gokart.core.pos.GokartPoseOdometry;
import ch.ethz.idsc.gokart.core.pos.LocalizationConfig;
import ch.ethz.idsc.gokart.core.pos.MappedPoseInterface;
import ch.ethz.idsc.gokart.core.slam.PredefinedMap;
import ch.ethz.idsc.gokart.gui.GokartLcmChannel;
import ch.ethz.idsc.gokart.gui.top.GokartRender;
import ch.ethz.idsc.gokart.gui.top.TrajectoryRender;
import ch.ethz.idsc.gokart.offline.api.GokartLogAdapter;
import ch.ethz.idsc.gokart.offline.api.GokartLogInterface;
import ch.ethz.idsc.gokart.offline.slam.ScatterImage;
import ch.ethz.idsc.gokart.offline.slam.WallScatterImage;
import ch.ethz.idsc.owl.car.core.VehicleModel;
import ch.ethz.idsc.owl.car.shop.RimoSinusIonModel;
import ch.ethz.idsc.owl.gui.RenderInterface;
import ch.ethz.idsc.owl.gui.ren.Se2WaypointRender;
import ch.ethz.idsc.owl.gui.win.GeometricLayer;
import ch.ethz.idsc.owl.math.state.StateTime;
import ch.ethz.idsc.owl.math.state.TrajectorySample;
import ch.ethz.idsc.retina.lcm.ArrayFloatBlob;
import ch.ethz.idsc.retina.lcm.OfflineLogListener;
import ch.ethz.idsc.retina.lcm.OfflineLogPlayer;
import ch.ethz.idsc.retina.util.math.Magnitude;
import ch.ethz.idsc.retina.util.math.SI;
import ch.ethz.idsc.subare.util.UserHome;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.io.ResourceData;
import ch.ethz.idsc.tensor.qty.Quantity;
import ch.ethz.idsc.tensor.sca.Round;

class PlannerAnalysis extends TrajectoryRender {
  private static final VehicleModel VEHICLE_MODEL = RimoSinusIonModel.standard();
  private static final Tensor ARROWHEAD = Tensors.matrixDouble( //
      new double[][] { { .3, 0 }, { -.1, -.1 }, { -.1, +.1 } }).multiply(RealScalar.of(3));
  private static final Tensor waypoints = ResourceData.of("/demo/dubendorf/hangar/20180425waypoints.csv");
  private static RenderInterface wr = new Se2WaypointRender(waypoints, ARROWHEAD, new Color(64, 192, 64, 255));
  private static TrajectoryRender tr = new TrajectoryRender();
  private static GokartPoseEvent gpe;
  private static ScatterImage scatterImage;
  private static GokartPoseOdometry gokartPoseOdometry = GokartPoseLcmServer.INSTANCE.getGokartPoseOdometry();
  private static MappedPoseInterface gokartPoseInterface = gokartPoseOdometry;
  private static Scalar time_next = Quantity.of(0, SI.SECOND);
  private static Scalar delta = Quantity.of(0.1, SI.SECOND);
  // ---
  private static OfflineLogListener oll = new OfflineLogListener() {
    @Override
    public void event(Scalar time, String channel, ByteBuffer byteBuffer) {
      if (channel.equals(GokartLcmChannel.POSE_LIDAR)) {
        gpe = new GokartPoseEvent(byteBuffer);
      } else if (channel.equals(GokartLcmChannel.TRAJECTORY_STATETIME)) {
        Tensor trajTensor = ArrayFloatBlob.decode(byteBuffer);
        tr.TRAJECTORY = getTrajectory(trajTensor);
      }
      if (Scalars.lessThan(time_next, time) && Objects.nonNull(gpe) && Objects.nonNull(tr)) {
        time_next = time.add(delta);
        System.out.print("Extracting log at " + time.map(Round._2) + "\n");
        PredefinedMap predefinedMap = LocalizationConfig.getPredefinedMap();
        scatterImage = new WallScatterImage(predefinedMap);
        // ---
        GeometricLayer gl = new GeometricLayer(predefinedMap.getModel2Pixel(), Tensors.vector(0, 0, 0));
        BufferedImage image = scatterImage.getImage();
        Graphics2D graphics = (Graphics2D) image.getGraphics();
        gokartPoseInterface.setPose(gpe.getPose(), gpe.getQuality());
        GokartRender gr = new GokartRender(gokartPoseInterface, VEHICLE_MODEL);
        tr.render(gl, graphics);
        wr.render(gl, graphics);
        gr.render(gl, graphics);
        // ---
        try {
          ImageIO.write(image, "png", UserHome.Pictures("/log/" + Magnitude.SECOND.apply(time).toString() + ".png"));
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  };

  public static void main(String[] args) throws FileNotFoundException, IOException {
    File file = UserHome.file("gokart/logs");
    System.out.println(file.getName());
    GokartLogInterface olr = GokartLogAdapter.of(file);
    OfflineLogPlayer.process(olr.file(), oll);
    System.out.print("Done.");
  }

  private static List<TrajectorySample> getTrajectory(Tensor tensor) {
    List<TrajectorySample> trajectory = new LinkedList<>();
    for (Tensor sample : tensor) {
      StateTime stateTime = new StateTime(sample.extract(0, 3), sample.Get(3));
      TrajectorySample trajectorySample = new TrajectorySample(stateTime, null);
      trajectory.add(trajectorySample);
    }
    return trajectory;
  }
}