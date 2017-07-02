// code by jph
package ch.ethz.idsc.retina.app;

import java.io.File;

import ch.ethz.idsc.retina.core.DvsAccumulate;
import ch.ethz.idsc.retina.core.DvsEvent;
import ch.ethz.idsc.retina.digest.DvsEventBuffer;
import ch.ethz.idsc.retina.digest.DvsEventStatistics;
import ch.ethz.idsc.retina.supply.DvsEventSupplier;
import ch.ethz.idsc.tensor.io.GifSequenceWriter;

public enum AccumulateToGif {
  ;
  public static void of(DvsEventSupplier dvsEventSupplier, File gifFile, int window_us) throws Exception {
    of(dvsEventSupplier, gifFile, window_us, window_us);
  }

  /** @param dvsEventSupplier
   * @param gifFile
   * @param window_us
   * @param rate_us
   * @throws Exception */
  public static void of(DvsEventSupplier dvsEventSupplier, File gifFile, int window_us, int rate_us) throws Exception {
    DvsEventStatistics dvsEventStatistics = new DvsEventStatistics();
    GifSequenceWriter gsw = GifSequenceWriter.of(gifFile, rate_us / 1000);
    try {
      DvsEventBuffer dvsEventBuffer = new DvsEventBuffer(window_us);
      long next = rate_us;
      while (true) {
        DvsEvent dvsEvent = dvsEventSupplier.next();
        dvsEventStatistics.digest(dvsEvent);
        long time = dvsEvent.time_us;
        while (next <= time) {
          gsw.append(DvsAccumulate.of(dvsEventBuffer, dvsEventSupplier.dimension(), next));
          next += rate_us;
        }
        dvsEventBuffer.digest(dvsEvent);
      }
    } catch (Exception exception) {
      exception.printStackTrace();
    }
    gsw.close();
    dvsEventStatistics.printSummary();
  }
}
