// code by am
package ch.ethz.idsc.gokart.core.adas;

import ch.ethz.idsc.retina.util.sys.AppResources;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.qty.Quantity;
import ch.ethz.idsc.tensor.ref.FieldSubdivide;

public class HapticSteerConfig {
  public static final HapticSteerConfig GLOBAL = AppResources.load(new HapticSteerConfig());
  /***************************************************/
  /** Value to amplify the Input in the PowerSteeringModule */
  public Scalar staticCompensation = Quantity.of(0.6, "SCT*SCE^-1");
  public Scalar dynamicCompensation = Quantity.of(90, "SCT");
  public Scalar dynamicCompensationBoundary = Quantity.of(0.1, "SCT");
  /** Constant Torque for Experiment */
  @FieldSubdivide(start = "-7/10[SCT]", end = "7/10[SCT]", intervals = 14)
  public Scalar constantTorque = Quantity.of(0, "SCT");
  /** Values for Vibration Mode */
  public Scalar vibrationAmplitude = RealScalar.of(0.4);
  public Scalar vibrationFrequency = RealScalar.of(12);
  public double criticalSlip = 0.2;
  public double criticalAngle = 12; // die Einheit davon ist Grad, ?
  public Scalar absFrequency = RealScalar.of(5);
  public Scalar absAmplitude = RealScalar.of(0.2);
  public double absDuration = 1;
}
