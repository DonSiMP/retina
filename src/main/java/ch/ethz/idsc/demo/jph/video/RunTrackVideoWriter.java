// code by jph
package ch.ethz.idsc.demo.jph.video;

import java.io.File;
import java.io.IOException;

import ch.ethz.idsc.gokart.lcm.OfflineLogPlayer;
import ch.ethz.idsc.gokart.offline.video.TrackVideoConfig;
import ch.ethz.idsc.gokart.offline.video.TrackVideoWriter;
import ch.ethz.idsc.tensor.io.HomeDirectory;

/* package */ enum RunTrackVideoWriter {
  ;
  private static void run(File file, File dest) throws IOException, Exception {
    try (TrackVideoWriter trackVideoWriter = new TrackVideoWriter( //
        RunVideoBackground.get20190414(), new TrackVideoConfig(), dest)) {
      OfflineLogPlayer.process(file, trackVideoWriter);
    }
  }

  public static void main(String[] args) throws Exception {
    run(new File("/home/datahaki/ensemblelaps/dynamic/m13.lcm"), HomeDirectory.file("dynamic13b.mp4"));
    // run(new File("/home/datahaki/ensemblelaps/kinematic/m00.lcm"), HomeDirectory.file("kinematic00a.mp4"));
    // run(new File("/home/datahaki/ensemblelaps/human/m03.lcm"), HomeDirectory.file("human03.mp4"));
    System.out.println("[done.]");
  }
}
