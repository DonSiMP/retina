// code by jph
package ch.ethz.idsc.demo.mp.playground;

import ch.ethz.idsc.owl.bot.r2.R2ImageRegionWrap;
import ch.ethz.idsc.owl.bot.r2.R2ImageRegions;
import ch.ethz.idsc.owl.bot.se2.glc.CarPolicyEntity;
import ch.ethz.idsc.owl.bot.util.DemoInterface;
import ch.ethz.idsc.owl.bot.util.RegionRenders;
import ch.ethz.idsc.owl.glc.adapter.CatchyTrajectoryRegionQuery;
import ch.ethz.idsc.owl.gui.win.BaseFrame;
import ch.ethz.idsc.owl.gui.win.OwlyAnimationFrame;
import ch.ethz.idsc.owl.math.region.ImageRegion;
import ch.ethz.idsc.owl.math.state.TrajectoryRegionQuery;
import ch.ethz.idsc.subare.core.td.SarsaType;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;

/* package */ class RLDemo implements DemoInterface {
  @Override
  public BaseFrame start() {
    OwlyAnimationFrame owlyAnimationFrame = new OwlyAnimationFrame();
    R2ImageRegionWrap r2ImageRegionWrap = R2ImageRegions._GTOB;
    ImageRegion imageRegion = r2ImageRegionWrap.imageRegion();
    TrajectoryRegionQuery trajectoryRegionQuery = CatchyTrajectoryRegionQuery.timeInvariant(imageRegion);
    owlyAnimationFrame.addBackground(RegionRenders.create(imageRegion));
    owlyAnimationFrame.addBackground(RegionRenders.create(trajectoryRegionQuery));
    Tensor startPos = Tensors.vector(3.000, 6.983, 0.000);
    CarPolicyEntity carPolicyEntity = //
        new CarPolicyEntity(startPos, SarsaType.ORIGINAL, trajectoryRegionQuery);
    owlyAnimationFrame.add(carPolicyEntity);
    CarPolicyEntity carPolicyEntity2 = //
        new CarPolicyEntity(startPos, SarsaType.EXPECTED, trajectoryRegionQuery);
    owlyAnimationFrame.add(carPolicyEntity2);
    CarPolicyEntity carPolicyEntity3 = //
        new CarPolicyEntity(startPos, SarsaType.QLEARNING, trajectoryRegionQuery);
    owlyAnimationFrame.add(carPolicyEntity3);
    owlyAnimationFrame.configCoordinateOffset(50, 700);
    owlyAnimationFrame.jFrame.setBounds(100, 50, 1200, 800);
    return owlyAnimationFrame;
  }

  public static void main(String[] args) {
    new RLDemo().start().jFrame.setVisible(true);
  }
}
