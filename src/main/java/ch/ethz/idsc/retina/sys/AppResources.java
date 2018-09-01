// code by jph
package ch.ethz.idsc.retina.sys;

import java.io.File;

import ch.ethz.idsc.retina.util.data.TensorProperties;

/** system specific customization
 * for instance safety thresholds, and control parameters */
public enum AppResources {
  ;
  private static final File DIRECTORY = new File("resources", "properties");

  private static File file(Object object) {
    return new File(DIRECTORY, object.getClass().getSimpleName() + ".properties");
  }

  public static <T> T load(T object) {
    TensorProperties.wrap(object).tryLoad(file(object));
    return object;
  }

  public static void save(Object object) {
    try {
      TensorProperties.wrap(object).save(file(object));
    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }
}
