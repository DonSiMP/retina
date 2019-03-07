// code by jph
package ch.ethz.idsc.demo.jph.log;

import java.awt.BasicStroke;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jfree.chart.ChartUtils;

import ch.ethz.idsc.gokart.offline.channel.DavisDvsChannel;
import ch.ethz.idsc.gokart.offline.channel.GokartPoseChannel;
import ch.ethz.idsc.gokart.offline.channel.GokartStatusChannel;
import ch.ethz.idsc.gokart.offline.channel.LabjackAdcChannel;
import ch.ethz.idsc.gokart.offline.channel.LinmotGetVehicleChannel;
import ch.ethz.idsc.gokart.offline.channel.LinmotPutVehicleChannel;
import ch.ethz.idsc.gokart.offline.channel.RimoGetChannel;
import ch.ethz.idsc.gokart.offline.channel.RimoPutChannel;
import ch.ethz.idsc.gokart.offline.channel.SingleChannelInterface;
import ch.ethz.idsc.gokart.offline.channel.SteerGetChannel;
import ch.ethz.idsc.gokart.offline.channel.SteerPutChannel;
import ch.ethz.idsc.gokart.offline.channel.Vlp16RayChannel;
import ch.ethz.idsc.gokart.offline.channel.Vmu931ImuVehicleChannel;
import ch.ethz.idsc.gokart.offline.pose.GokartPosePostChannel;
import ch.ethz.idsc.retina.lidar.VelodyneStatics;
import ch.ethz.idsc.sophus.group.LieDifferences;
import ch.ethz.idsc.sophus.group.Se2CoveringExponential;
import ch.ethz.idsc.sophus.group.Se2Group;
import ch.ethz.idsc.sophus.math.WindowCenterSampler;
import ch.ethz.idsc.subare.util.HtmlUtf8;
import ch.ethz.idsc.subare.util.plot.ListPlot;
import ch.ethz.idsc.subare.util.plot.VisualRow;
import ch.ethz.idsc.subare.util.plot.VisualSet;
import ch.ethz.idsc.tensor.RationalScalar;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Accumulate;
import ch.ethz.idsc.tensor.alg.Differences;
import ch.ethz.idsc.tensor.alg.ListConvolve;
import ch.ethz.idsc.tensor.img.ColorDataLists;
import ch.ethz.idsc.tensor.io.Get;
import ch.ethz.idsc.tensor.io.Import;
import ch.ethz.idsc.tensor.sca.win.GaussianWindow;

/* package */ class HtmlLogReport {
  private static final int WIDTH = 854;
  private static final int HEIGHT = 360; // 480;

  // ---
  public static void generate(File directory) throws IOException {
    new HtmlLogReport(directory);
  }

  // ---
  private final File plot;
  private final Map<SingleChannelInterface, Tensor> map;

  private HtmlLogReport(File directory) throws IOException {
    plot = new File(directory, "plot");
    plot.mkdir();
    map = StaticHelper.SINGLE_CHANNEL_INTERFACES.stream() //
        .collect(Collectors.toMap(Function.identity(), singleChannelInterface -> {
          try {
            return Import.of(new File(directory, singleChannelInterface.exportName() + StaticHelper.EXTENSION));
          } catch (Exception exception) {
            throw new RuntimeException();
          }
        }));
    try (HtmlUtf8 htmlUtf8 = HtmlUtf8.page(new File(directory, "index.html"))) {
      htmlUtf8.appendln("<h1>" + directory.getName() + "</h1>");
      Tensor tensor = Get.of(new File(directory, StaticHelper.LOG_START_TIME));
      htmlUtf8.appendln("<p>Absolute time of start of log recording: " + tensor + " [us] <small>since 1970-01-01</small></p>");
      htmlUtf8.appendln("<p><small>report generated: " + new Date() + "</small>");
      htmlUtf8.appendln("<h2>Steering</h2>");
      htmlUtf8.appendln("<img src='plot/status.png'/><br/><br/>");
      htmlUtf8.appendln("<img src='plot/steerget.png'/><br/><br/>");
      htmlUtf8.appendln("<h2>Rear Wheel Motors</h2>");
      htmlUtf8.appendln("<img src='plot/rimoput.png'/><br/><br/>");
      htmlUtf8.appendln("<img src='plot/rimoget.png'/><br/><br/>");
      htmlUtf8.appendln("<h2>Brake</h2>");
      htmlUtf8.appendln("<img src='plot/linmotPosition.png'/><br/><br/>");
      htmlUtf8.appendln("<img src='plot/linmotTemperature.png'/><br/><br/>");
      htmlUtf8.appendln("<h2>VMU931 IMU</h2>");
      htmlUtf8.appendln("<img src='plot/vmu931acc.png'/><br/><br/>");
      htmlUtf8.appendln("<img src='plot/vmu931gyro.png'/><br/><br/>");
      htmlUtf8.appendln("<h2>Localization</h2>");
      htmlUtf8.appendln("<img src='plot/pose_raw.png'/><br/><br/>");
      htmlUtf8.appendln("<img src='plot/pose_smooth.png'/><br/><br/>");
      htmlUtf8.appendln("<img src='plot/speeds.png'/><br/><br/>");
      htmlUtf8.appendln("<img src='plot/vmu931accSmooth.png' /><br/><br/>");
      htmlUtf8.appendln("<img src='plot/poseQuality.png'/><br/><br/>");
      htmlUtf8.appendln("<h2>VLP16</h2>");
      htmlUtf8.appendln("<img src='plot/vlp16timing.png'/><br/><br/>");
      htmlUtf8.appendln("<img src='plot/vlp16rotation.png'/><br/><br/>");
      htmlUtf8.appendln("<img src='plot/vlp16rate.png'/><br/><br/>");
      htmlUtf8.appendln("<h2>Davis240C</h2>");
      htmlUtf8.appendln("<img src='plot/davisPolarity.png'/><br/><br/>");
      htmlUtf8.appendln("<h2>Labjack</h2>");
      htmlUtf8.appendln("<img src='plot/labjackAdc.png'/><br/><br/>");
    }
    exportStatus();
    exportSteerGet();
    exportRimoPut();
    exportRimoGet();
    exportLinmotPosition();
    exportLinmotTemperature();
    exportVmu931acc();
    exportVmu931accSmooth();
    exportPose();
    exportPoseSmooth();
    exportPoseDerivative();
    exportLabjackAdc();
    exportPoseQuality();
    exportDavisPolarityIntegral();
    exportVlp16Timing();
    exportVlp16Rotation();
    exportVlp16Rate();
  }

  private void exportListPlot(String filename, VisualSet visualSet) throws IOException {
    exportListPlot(filename, visualSet, HEIGHT);
  }

  private void exportListPlot(String filename, VisualSet visualSet, int height) throws IOException {
    ChartUtils.saveChartAsPNG(new File(plot, filename), ListPlot.of(visualSet), WIDTH, height);
  }

  public void exportStatus() throws IOException {
    VisualSet visualSet = new VisualSet(ColorDataLists._097.cyclic().deriveWithAlpha(128));
    visualSet.setPlotLabel("power steering position");
    visualSet.setAxesLabelX("time [s]");
    visualSet.setAxesLabelY("power steering position [n.a.]");
    {
      Tensor tensor = map.get(SteerGetChannel.INSTANCE);
      Tensor domain = tensor.get(Tensor.ALL, 0);
      visualSet.add(domain, tensor.get(Tensor.ALL, 8)).setLabel("raw");
    }
    {
      Tensor tensor = map.get(GokartStatusChannel.INSTANCE);
      Tensor domain = tensor.get(Tensor.ALL, 0);
      visualSet.add(domain, tensor.get(Tensor.ALL, 1)).setLabel("calibrated (0 = straight)");
    }
    exportListPlot("status.png", visualSet);
  }

  public void exportSteerGet() throws IOException {
    VisualSet visualSet = new VisualSet(ColorDataLists._097.cyclic().deriveWithAlpha(128));
    visualSet.setPlotLabel("power steering torque");
    visualSet.setAxesLabelX("time [s]");
    visualSet.setAxesLabelY("torque [n.a.]");
    {
      Tensor tensor = map.get(SteerPutChannel.INSTANCE);
      Tensor domain = tensor.get(Tensor.ALL, 0);
      visualSet.add(domain, tensor.get(Tensor.ALL, 2)).setLabel("commanded");
    }
    {
      Tensor tensor = map.get(SteerGetChannel.INSTANCE);
      Tensor domain = tensor.get(Tensor.ALL, 0);
      visualSet.add(domain, tensor.get(Tensor.ALL, 5)).setLabel("effective");
    }
    exportListPlot("steerget.png", visualSet);
  }

  public void exportRimoPut() throws IOException {
    VisualSet visualSet = new VisualSet(ColorDataLists._097.cyclic().deriveWithAlpha(128));
    visualSet.setPlotLabel("rear wheel motors torque command");
    visualSet.setAxesLabelX("time [s]");
    visualSet.setAxesLabelY("torque command [ARMS]");
    Tensor tensor = map.get(RimoPutChannel.INSTANCE);
    Tensor domain = tensor.get(Tensor.ALL, 0);
    Tensor tL = tensor.get(Tensor.ALL, 1);
    Tensor tR = tensor.get(Tensor.ALL, 2);
    visualSet.add(domain, tL).setLabel("left");
    visualSet.add(domain, tR).setLabel("right");
    visualSet.add(domain, tL.add(tR).multiply(RationalScalar.HALF)).setLabel("power");
    exportListPlot("rimoput.png", visualSet, 480);
  }

  public void exportRimoGet() throws IOException {
    VisualSet visualSet = new VisualSet();
    visualSet.setPlotLabel("rear wheel motors rotational rate");
    visualSet.setAxesLabelX("time [s]");
    visualSet.setAxesLabelY("wheel rotational rate [rad*s^-1]");
    Tensor tensor = map.get(RimoGetChannel.INSTANCE);
    Tensor domain = tensor.get(Tensor.ALL, 0);
    visualSet.add(domain, tensor.get(Tensor.ALL, 2)).setLabel("left");
    visualSet.add(domain, tensor.get(Tensor.ALL, 2 + 7)).setLabel("right");
    exportListPlot("rimoget.png", visualSet);
  }

  public void exportLinmotPosition() throws IOException {
    VisualSet visualSet = new VisualSet(ColorDataLists._097.cyclic().deriveWithAlpha(192));
    visualSet.setPlotLabel("Brake position");
    visualSet.setAxesLabelX("time [s]");
    visualSet.setAxesLabelY("brake position [m]");
    {
      Tensor tensor = map.get(LinmotPutVehicleChannel.INSTANCE);
      Tensor domain = tensor.get(Tensor.ALL, 0);
      visualSet.add(domain, tensor.get(Tensor.ALL, 1)).setLabel("command");
    }
    {
      Tensor tensor = map.get(LinmotGetVehicleChannel.INSTANCE);
      Tensor domain = tensor.get(Tensor.ALL, 0);
      visualSet.add(domain, tensor.get(Tensor.ALL, 1)).setLabel("effective");
    }
    exportListPlot("linmotPosition.png", visualSet);
  }

  public void exportLinmotTemperature() throws IOException {
    VisualSet visualSet = new VisualSet(ColorDataLists._097.cyclic().deriveWithAlpha(192));
    visualSet.setPlotLabel("Brake temperature");
    visualSet.setAxesLabelX("time [s]");
    visualSet.setAxesLabelY("temperature [degC]");
    {
      Tensor tensor = map.get(LinmotGetVehicleChannel.INSTANCE);
      Tensor domain = tensor.get(Tensor.ALL, 0);
      visualSet.add(domain, tensor.get(Tensor.ALL, 2)).setLabel("winding 1");
      visualSet.add(domain, tensor.get(Tensor.ALL, 3)).setLabel("winding 2");
    }
    exportListPlot("linmotTemperature.png", visualSet);
  }

  public void exportVmu931acc() throws IOException {
    VisualSet visualSet = new VisualSet(ColorDataLists._097.cyclic().deriveWithAlpha(64));
    visualSet.setPlotLabel("VMU931 acceleration");
    visualSet.setAxesLabelX("time [s]");
    visualSet.setAxesLabelY("acceleration [m*s^-2]");
    Tensor tensor = map.get(Vmu931ImuVehicleChannel.INSTANCE);
    Tensor domain = tensor.get(Tensor.ALL, 0);
    visualSet.add(domain, tensor.get(Tensor.ALL, 2)).setLabel("x (forward)");
    visualSet.add(domain, tensor.get(Tensor.ALL, 3)).setLabel("y (left)");
    exportListPlot("vmu931acc.png", visualSet);
  }

  public void exportVmu931accSmooth() throws IOException {
    VisualSet visualSet = new VisualSet(ColorDataLists._097.cyclic().deriveWithAlpha(192));
    visualSet.setPlotLabel("Smoothed Acceleration from VMU931");
    visualSet.setAxesLabelX("time [s]");
    visualSet.setAxesLabelY("acceleration [m*s^-2]");
    {
      Tensor tensor = map.get(Vmu931ImuVehicleChannel.INSTANCE);
      Tensor domain = tensor.get(Tensor.ALL, 0);
      Tensor mask = new WindowCenterSampler(GaussianWindow.FUNCTION).apply(100);
      Tensor smoothX = ListConvolve.of(mask, tensor.get(Tensor.ALL, 2));
      Tensor smoothY = ListConvolve.of(mask, tensor.get(Tensor.ALL, 3));
      visualSet.add(domain.extract(0, smoothX.length()), smoothX).setLabel("x (forward)");
      visualSet.add(domain.extract(0, smoothY.length()), smoothY).setLabel("y (left)");
    }
    exportListPlot("vmu931accSmooth.png", visualSet);
  }

  public void exportPose() throws IOException {
    VisualSet visualSet = new VisualSet();
    visualSet.setPlotLabel("Pose");
    visualSet.setAxesLabelX("time [s]");
    Tensor tensor = map.get(GokartPoseChannel.INSTANCE);
    Tensor domain = tensor.get(Tensor.ALL, 0);
    visualSet.add(domain, tensor.get(Tensor.ALL, 1)).setLabel("global x position [m]");
    visualSet.add(domain, tensor.get(Tensor.ALL, 2)).setLabel("global y position [m]");
    visualSet.add(domain, tensor.get(Tensor.ALL, 3)).setLabel("global heading [rad]");
    exportListPlot("pose_raw.png", visualSet);
  }

  public void exportPoseSmooth() throws IOException {
    VisualSet visualSet = new VisualSet();
    visualSet.setPlotLabel("Smoothed Pose");
    visualSet.setAxesLabelX("time [s]");
    Tensor tensor = Import.of(new File(plot.getParentFile(), StaticHelper.GOKART_POSE_SMOOTH + ".csv.gz"));
    Tensor domain = tensor.get(Tensor.ALL, 0);
    visualSet.add(domain, tensor.get(Tensor.ALL, 1)).setLabel("global x position [m]");
    visualSet.add(domain, tensor.get(Tensor.ALL, 2)).setLabel("global y position [m]");
    visualSet.add(domain, tensor.get(Tensor.ALL, 3)).setLabel("global heading [rad]");
    exportListPlot("pose_smooth.png", visualSet);
  }

  public void exportPoseDerivative() throws IOException {
    final Scalar hertz = RealScalar.of(20.0);
    Tensor tensor = Import.of(new File(plot.getParentFile(), StaticHelper.GOKART_POSE_SMOOTH + ".csv.gz"));
    LieDifferences lieDifferences = new LieDifferences(Se2Group.INSTANCE, Se2CoveringExponential.INSTANCE);
    Tensor refined = Tensor.of(tensor.stream().map(row -> row.extract(1, 4)));
    Tensor speeds = lieDifferences.apply(refined);
    Tensor times = tensor.get(Tensor.ALL, 0);
    Tensor domain = times.extract(0, tensor.length() - 1);
    {
      VisualSet visualSet = new VisualSet();
      visualSet.setPlotLabel("Derivatives from Smoothed Pose");
      visualSet.setAxesLabelX("time [s]");
      visualSet.add(domain, speeds.get(Tensor.ALL, 0).multiply(hertz)).setLabel("tangent velocity [m/s]");
      visualSet.add(domain, speeds.get(Tensor.ALL, 1).multiply(hertz)).setLabel("side slip [m/s]");
      exportListPlot("speeds.png", visualSet);
    }
    {
      VisualSet visualSet = new VisualSet(ColorDataLists._097.cyclic().deriveWithAlpha(192));
      visualSet.setPlotLabel("Rotational Rate");
      visualSet.setAxesLabelX("time [s]");
      visualSet.setAxesLabelY("gyro [rad*s^-1]");
      {
        VisualRow visualRow = visualSet.add(domain, speeds.get(Tensor.ALL, 2).multiply(hertz));
        visualRow.setLabel("from smoothed pose [rad/s]");
        visualRow.setStroke(new BasicStroke(2f));
      }
      {
        Tensor vmu931 = map.get(Vmu931ImuVehicleChannel.INSTANCE);
        visualSet.add(vmu931.get(Tensor.ALL, 0), vmu931.get(Tensor.ALL, 4)).setLabel("from VMU931");
      }
      exportListPlot("vmu931gyro.png", visualSet);
    }
  }

  public void exportPoseQuality() throws IOException {
    VisualSet visualSet = new VisualSet();
    visualSet.setPlotLabel("Pose Estimation Quality");
    visualSet.setAxesLabelX("time [s]");
    visualSet.setAxesLabelY("quality in [0, 1]");
    {
      Tensor tensor = map.get(GokartPoseChannel.INSTANCE);
      Tensor domain = tensor.get(Tensor.ALL, 0);
      visualSet.add(domain, tensor.get(Tensor.ALL, 4)).setLabel("live");
    }
    {
      Tensor tensor = map.get(GokartPosePostChannel.INSTANCE);
      Tensor domain = tensor.get(Tensor.ALL, 0);
      visualSet.add(domain, tensor.get(Tensor.ALL, 4)).setLabel("post-processing");
    }
    exportListPlot("poseQuality.png", visualSet);
  }

  public void exportVlp16Timing() throws IOException {
    VisualSet visualSet = new VisualSet();
    visualSet.setPlotLabel("VLP16 Timing");
    visualSet.setAxesLabelX("time [s]");
    visualSet.setAxesLabelY("[s/1000000]");
    {
      Tensor tensor = map.get(Vlp16RayChannel.INSTANCE);
      Tensor domain = tensor.get(Tensor.ALL, 0);
      Tensor diffs = Differences.of(tensor.get(Tensor.ALL, 1));
      visualSet.add(domain.extract(0, diffs.length()), diffs);
    }
    exportListPlot("vlp16timing.png", visualSet);
  }

  public void exportVlp16Rotation() throws IOException {
    VisualSet visualSet = new VisualSet();
    visualSet.setPlotLabel("VLP16 Rotation Delta");
    visualSet.setAxesLabelX("time [s]");
    visualSet.setAxesLabelY("[deg/100]");
    {
      Tensor tensor = map.get(Vlp16RayChannel.INSTANCE);
      Tensor domain = tensor.get(Tensor.ALL, 0);
      Tensor diffs = Differences.of(tensor.get(Tensor.ALL, 2));
      Tensor values = Tensor.of(diffs.stream() //
          .map(Scalar.class::cast) //
          .map(Scalar::number) //
          .mapToInt(Number::intValue) //
          .map(VelodyneStatics::lookupAzimuth) //
          .mapToObj(RealScalar::of));
      // Mod.function(36000)
      visualSet.add(domain.extract(0, values.length()), values);
    }
    exportListPlot("vlp16rotation.png", visualSet);
  }

  public void exportVlp16Rate() throws IOException {
    VisualSet visualSet = new VisualSet();
    visualSet.setPlotLabel("VLP16 Rate");
    visualSet.setAxesLabelX("time [s]");
    visualSet.setAxesLabelY("[Hz]");
    {
      Tensor tensor = map.get(Vlp16RayChannel.INSTANCE);
      Tensor domain = tensor.get(Tensor.ALL, 0);
      Tensor drotate = Differences.of(tensor.get(Tensor.ALL, 2));
      Tensor dtiming = Differences.of(tensor.get(Tensor.ALL, 1));
      Tensor values = Tensor.of(drotate.stream() //
          .map(Scalar.class::cast) //
          .map(Scalar::number) //
          .mapToInt(Number::intValue) //
          .map(VelodyneStatics::lookupAzimuth) //
          .mapToObj(RealScalar::of));
      Tensor rate = values.multiply(RationalScalar.of(1_000_000, 36_000)).pmul(dtiming.map(Scalar::reciprocal));
      visualSet.add(domain.extract(0, rate.length()), rate);
    }
    exportListPlot("vlp16rate.png", visualSet);
  }

  public void exportDavisPolarityIntegral() throws IOException {
    VisualSet visualSet = new VisualSet();
    visualSet.setPlotLabel("Davis 240C Event Count by Polarity");
    visualSet.setAxesLabelX("time [s]");
    Tensor tensor = map.get(DavisDvsChannel.INSTANCE);
    Tensor domain = tensor.get(Tensor.ALL, 0);
    visualSet.add(domain, Accumulate.of(tensor.get(Tensor.ALL, 1))).setLabel("bright to dark");
    visualSet.add(domain, Accumulate.of(tensor.get(Tensor.ALL, 2))).setLabel("dark to bright");
    exportListPlot("davisPolarity.png", visualSet);
  }

  public void exportLabjackAdc() throws IOException {
    Tensor tensor = map.get(LabjackAdcChannel.INSTANCE);
    VisualSet visualSet = new VisualSet(ColorDataLists._097.cyclic().deriveWithAlpha(192));
    visualSet.setPlotLabel("Labjack ADC readout");
    visualSet.setAxesLabelX("time [s]");
    visualSet.setAxesLabelY("voltage [V]");
    Tensor domain = tensor.get(Tensor.ALL, 0);
    visualSet.add(domain, tensor.get(Tensor.ALL, 1)).setLabel("boost");
    visualSet.add(domain, tensor.get(Tensor.ALL, 2)).setLabel("reverse");
    visualSet.add(domain, tensor.get(Tensor.ALL, 3)).setLabel("throttle");
    visualSet.add(domain, tensor.get(Tensor.ALL, 4)).setLabel("autonomous button");
    visualSet.add(domain, tensor.get(Tensor.ALL, 5)).setLabel("ADC5 (not used)");
    exportListPlot("labjackAdc.png", visualSet);
  }

  public static void main(String[] args) throws IOException {
    // HtmlLogReport.generate(new File(StaticHelper.DEST, "20190208/20190208T145312_04"));
    HtmlLogReport.generate(new File(StaticHelper.DEST, "20190211/20190211T100755_00"));
  }
}
