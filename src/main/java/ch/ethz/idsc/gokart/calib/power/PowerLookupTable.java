// code by mh
package ch.ethz.idsc.gokart.calib.power;

import java.io.File;

import ch.ethz.idsc.gokart.core.man.ManualConfig;
import ch.ethz.idsc.retina.util.math.NonSI;
import ch.ethz.idsc.retina.util.math.SI;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.io.Export;
import ch.ethz.idsc.tensor.io.Import;
import ch.ethz.idsc.tensor.opt.LinearInterpolation;
import ch.ethz.idsc.tensor.qty.Quantity;
import ch.ethz.idsc.tensor.sca.Chop;
import ch.ethz.idsc.tensor.sca.Clip;
import ch.ethz.idsc.tensor.sca.Clips;

/** to ensure that the maximum motor torque is actually applied */
public class PowerLookupTable {
  private static final File DIRECTORY = new File("resources/lookup");
  private static final File FILE_FORWARD = new File(DIRECTORY, "powerlookuptable_forward.object");
  private static final File FILE_INVERSE = new File(DIRECTORY, "powerlookuptable_inverse.object");
  // ---
  private static final Scalar MAX_VEL = Quantity.of(+10, SI.VELOCITY);
  private static final Scalar MAX_ACC = Quantity.of(+2, SI.ACCELERATION);
  /** min and max values for lookup tables */
  public static final Clip CLIP_VEL = Clips.interval(MAX_VEL.negate(), MAX_VEL);
  public static final Clip CLIP_ACC = Clips.interval(MAX_ACC.negate(), MAX_ACC);
  private static final int RES = 1000;
  private static final PowerLookupTable INSTANCE = new PowerLookupTable();

  /** @return global instance of PowerLookupTable */
  public static PowerLookupTable getInstance() {
    return INSTANCE;
  }

  // ---
  /** maps from (current, speed)->(acceleration) */
  private final LookupTable2D lookupTable_forward;
  /** maps from (acceleration, speed)->(current) */
  private final LookupTable2D lookupTable_inverse;

  /** create or load power lookup table */
  private static LookupTable2D forward() {
    try {
      return Import.object(FILE_FORWARD); // load forward table
    } catch (Exception exception) {
      // ---
    }
    System.out.println("compute power lookup table forward...");
    // maps from (current, speed) -> (acceleration)
    LookupTable2D lookupTable2D = LookupTable2D.build( //
        MotorFunction::getAccelerationEstimation, //
        RES, RES, //
        ManualConfig.GLOBAL.torqueLimitClip(), //
        CLIP_VEL);
    try {
      Export.object(FILE_FORWARD, lookupTable2D);
    } catch (Exception exception) {
      // ---
    }
    return lookupTable2D;
  }

  /** create or load power lookup table */
  private static LookupTable2D inverse(LookupTable2D forward) {
    try {
      return Import.object(FILE_INVERSE); // load inverse table
    } catch (Exception exception) {
      // ---
    }
    System.out.println("compute power lookup table inverse...");
    // maps from (acceleration, speed)->(acceleration)
    LookupTable2D lookupTable2D = forward.getInverseLookupTableBinarySearch( //
        MotorFunction::getAccelerationEstimation, //
        0, //
        RES, RES, //
        CLIP_ACC, Chop._03);
    try {
      Export.object(FILE_INVERSE, lookupTable2D);
    } catch (Exception exception) {
      // ---
    }
    return lookupTable2D;
  }

  private PowerLookupTable() {
    DIRECTORY.mkdir();
    lookupTable_forward = forward();
    lookupTable_inverse = inverse(lookupTable_forward);
  }

  /** get min and max possible acceleration
   * Example: {-1.6261117143630983[m*s^-2], 1.8412589178085328[m*s^-2]}
   * 
   * @param velocity current velocity [m/s]
   * @return a tensor of the maximal and minimal acceleration [m/s^2] */
  public Tensor getMinMaxAcceleration(Scalar velocity) {
    // the min and max values are multiplied by 1.02
    // in order to ensure that the maximum value can be output
    return lookupTable_forward.getExtremalValues0(velocity).multiply(RealScalar.of(1.02));
  }

  /** get acceleration for a given current and velocity
   * 
   * @param current the applied motor current [ARMS]
   * @param velocity the velocity [m/s]
   * @return the resulting acceleration [m/s^2] */
  public Scalar getAcceleration(Scalar current, Scalar velocity) {
    return lookupTable_forward.lookup(current, velocity);
  }

  /** get the need current for a wanted acceleration
   * If the acceleration is not achievable
   * the motor current corresponding to the nearest possible acceleration value is returned
   * @param wantedAcceleration the wanted acceleration [m/s^2]
   * @param velocity the velocity [m/s]
   * @return the needed motor current [ARMS] */
  public Scalar getNeededCurrent(Scalar wantedAcceleration, Scalar velocity) {
    return lookupTable_inverse.lookup(wantedAcceleration, velocity);
  }

  /** get the acceleration characterized by the relative power value
   * @param power value scaled from [-1,1] characterizing the requested power value [ONE]
   * -1: minimal acceleration (full deceleration)
   * 0: no motor current
   * 1: maximal acceleration
   * @param velocity [m/s]
   * @return the resulting acceleration [m/s^2] */
  public Scalar getNormalizedAccelerationTorqueCentered(Scalar power, Scalar velocity) {
    Scalar torqueFreeAcc = getAcceleration(Quantity.of(0, NonSI.ARMS), velocity);
    return getNormalizedAcceleration(torqueFreeAcc, power, velocity);
  }

  /** get the acceleration characterized by the relative power value
   *
   * @param torqueFreeAcc with unit [m*s^-2]
   * @param power value scaled from [-1,1] characterizing the requested power value [ONE]
   * -1: minimal acceleration (full deceleration)
   * 0: no acceleration
   * 1: maximal acceleration
   * @param velocity [m/s]
   * @return the resulting acceleration [m/s^2] */
  /* package */ Scalar getNormalizedAcceleration(Scalar torqueFreeAcc, Scalar power, Scalar velocity) {
    Tensor minMaxAcc = getMinMaxAcceleration(velocity);
    Scalar clippedPower = Clips.absoluteOne().apply(power);
    Tensor keypoints = Tensors.of(minMaxAcc.Get(0), torqueFreeAcc, minMaxAcc.Get(1));
    return LinearInterpolation.of(keypoints).At(clippedPower.add(RealScalar.ONE));
  }
}
