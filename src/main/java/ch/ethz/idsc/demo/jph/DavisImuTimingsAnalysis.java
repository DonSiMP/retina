// code by jph
package ch.ethz.idsc.demo.jph;

import java.io.File;
import java.io.IOException;

import ch.ethz.idsc.gokart.lcm.OfflineLogPlayer;
import ch.ethz.idsc.gokart.offline.api.GokartLogAdapter;
import ch.ethz.idsc.gokart.offline.api.GokartLogInterface;
import ch.ethz.idsc.gokart.offline.api.OfflineIndex;
import ch.ethz.idsc.gokart.offline.api.OfflineTableSupplier;
import ch.ethz.idsc.gokart.offline.channel.DavisImuChannel;
import ch.ethz.idsc.gokart.offline.tab.SingleChannelTable;
import ch.ethz.idsc.tensor.io.CsvFormat;
import ch.ethz.idsc.tensor.io.Export;
import ch.ethz.idsc.tensor.io.HomeDirectory;

/** investigation of temporal regularity/sampling rate of davis240c imu measurements
 * as the samples enhance the lidar based localization algorithm */
/* package */ enum DavisImuTimingsAnalysis {
  ;
  public static void main(String[] args) throws IOException {
    for (File folder : OfflineIndex.folders(HomeDirectory.file("gokart/ImuTimings"))) {
      // System.out.println(folder);
      String parent = folder.getParentFile().getName();
      final String name = parent + "_" + folder.getName();
      System.out.println(name);
      GokartLogInterface gokartLogInterface = GokartLogAdapter.of(folder);
      // ---
      OfflineTableSupplier davisImuTable = SingleChannelTable.of(DavisImuChannel.INSTANCE);
      OfflineLogPlayer.process(gokartLogInterface.file(), davisImuTable);
      Export.of(HomeDirectory.file("csv/" + name + ".csv"), davisImuTable.getTable().map(CsvFormat.strict()));
    }
  }
}
