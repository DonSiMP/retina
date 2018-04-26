// code by mg
package ch.ethz.idsc.demo.mg.pipeline;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import ch.ethz.idsc.demo.mg.HandLabelFileLocations;
import ch.ethz.idsc.demo.mg.gui.AccumulatedEventFrame;
import ch.ethz.idsc.demo.mg.gui.PhysicalBlobFrame;
import ch.ethz.idsc.demo.mg.gui.PipelineVisualization;
import ch.ethz.idsc.retina.dev.davis.DavisDvsListener;
import ch.ethz.idsc.retina.dev.davis._240c.DavisDvsEvent;
import ch.ethz.idsc.retina.dev.davis.data.DavisDvsDatagramDecoder;
import ch.ethz.idsc.retina.lcm.OfflineLogListener;
import ch.ethz.idsc.tensor.Scalar;

// this module distributes the event stream to the visualization and control pipeline
public class InputSubModule implements OfflineLogListener, DavisDvsListener {
  // event filtering
  private final DavisDvsDatagramDecoder davisDvsDatagramDecoder = new DavisDvsDatagramDecoder(); // listener
  private final EventFiltering eventFiltering = new EventFiltering(); // for filtering of event stream
  // blob tracking
  private final BlobTracking tracking = new BlobTracking(); // tracks blobs
  // feature filtering
  private final ImageBlobSelector blobSelector = new ImageBlobSelector(); // selects blobs that match certain criteria
  // blob transformation
  private final ImageToWorldTransform transformer = new ImageToWorldTransform(); // transform to world coordinates
  // visualization
  private final PipelineVisualization viz = new PipelineVisualization(); // visualization GUI
  private final AccumulatedEventFrame[] eventFrames = new AccumulatedEventFrame[3]; // perception module visualization frames
  private final PhysicalBlobFrame[] physicalFrames = new PhysicalBlobFrame[3]; // control module visualization frames
  // performance evaluation
  private final File pathToHandlabelsFile = HandLabelFileLocations.labels("labeledFeatures.dat"); // ground truth file for tracking evaluator
  // private final TrackingEvaluator evaluator = new TrackingEvaluator(pathToHandlabelsFile, track);
  // pipeline configuration
  private final int maxDuration = 10000; // [ms]
  private final int backgroundActivityFilterTime = 200; // [us] the shorter the more is filtered
  private final int imageInterval = 200; // [ms] visualization interval
  private final int savingInterval = 1000; // [ms] image saving interval
  private final boolean useFilter = true;
  private boolean saveImages = false;
  private String imagePrefix = "Test"; // image name structure: "%s_%04d_%d.png", imagePrefix, imageCount, timeStamp
  private File pathToImages = HandLabelFileLocations.images(); // path where images are saved
  // fields for testing
  private float eventCount = 0;
  private float filteredEventCount;
  private int lastImagingTimestamp;
  private int lastSavingTimestamp;
  private int begin, end;
  private long startTime, endTime;

  public InputSubModule() {
    davisDvsDatagramDecoder.addDvsListener(this);
    for (int i = 0; i < eventFrames.length; i++) {
      eventFrames[i] = new AccumulatedEventFrame();
    }
    for (int i = 0; i < physicalFrames.length; i++) {
      physicalFrames[i] = new PhysicalBlobFrame();
    }
  }

  @Override
  public void event(Scalar time, String channel, ByteBuffer byteBuffer) {
    if (channel.equals("davis240c.overview.dvs")) {
      davisDvsDatagramDecoder.decode(byteBuffer);
    }
  }

  @Override
  public void davisDvs(DavisDvsEvent davisDvsEvent) {
    // initialize timers
    if (eventCount == 0) {
      begin = davisDvsEvent.time;
      lastImagingTimestamp = davisDvsEvent.time;
      lastSavingTimestamp = davisDvsEvent.time;
      startTime = System.currentTimeMillis();
    }
    ++eventCount;
    // visualization of raw events
    eventFrames[0].receiveEvent(davisDvsEvent);
    // filtering returns a boolean
    if (eventFiltering.filterPipeline(davisDvsEvent, backgroundActivityFilterTime) && useFilter) {
      // control pipeline
      tracking.receiveEvent(davisDvsEvent);
      blobSelector.receiveActiveBlobs(tracking.getActiveBlobs());
      transformer.transformSelectedBlobs(blobSelector.getSelectedBlobs());
      // evaluation tool
      // evaluator.receiveEvent(davisDvsEvent);
      // visualization
      eventFrames[1].receiveEvent(davisDvsEvent);
      eventFrames[2].receiveEvent(davisDvsEvent);
      ++filteredEventCount;
      // the events are accumulated for the interval time and then displayed in a single frame
      if ((davisDvsEvent.time - lastImagingTimestamp) > imageInterval * 1000) {
        // visualization repaint
        viz.setFrames(constructFrames());
        System.out.println("ActiveBlobs: " + tracking.getActiveBlobs().size());
        System.out.println("SelectedBlobs: " + blobSelector.getSelectedBlobs().size());
        System.out.println("TransformedBlobs: " + transformer.getPhysicalBlobs().size());
        if (saveImages && (davisDvsEvent.time - lastSavingTimestamp) > savingInterval * 1000) {
          try {
            viz.saveImage(pathToImages, imagePrefix, davisDvsEvent.time);
          } catch (IOException e) {
            e.printStackTrace();
          }
          lastSavingTimestamp = davisDvsEvent.time;
        }
        clearAllFrames();
        lastImagingTimestamp = davisDvsEvent.time;
      }
    }
    if (davisDvsEvent.time - begin > maxDuration * 1000) {
      end = davisDvsEvent.time;
      endTime = System.currentTimeMillis();
      int diff = end - begin;
      System.out.println("Percentage hit by active blobs: " + tracking.hitthreshold / eventCount * 100);
      System.out.println("Elapsed time in the eventstream [ms]: " + diff / 1000 + " with " + eventCount + " events");
      long elapsedTime = endTime - startTime;
      System.out.println("Computation time: " + elapsedTime + "[ms]");
      System.out.format("%.2f%% of the events were processed after filtering.", getFilteredPercentage());
      System.exit(0);
    }
  }

  // for visualization
  private BufferedImage[] constructFrames() {
    BufferedImage[] combinedFrames = new BufferedImage[6];
    combinedFrames[0] = eventFrames[0].getAccumulatedEvents();
    combinedFrames[1] = eventFrames[1].overlayActiveBlobs(blobSelector.getProcessedBlobs());
    combinedFrames[2] = eventFrames[2].overlayHiddenBlobs((tracking.getHiddenBlobs()));
    combinedFrames[3] = physicalFrames[0].overlayPhysicalBlobs((transformer.getPhysicalBlobs()));
    combinedFrames[4] = physicalFrames[1].getFrame();
    combinedFrames[5] = physicalFrames[2].getFrame();
    return combinedFrames;
  }

  // for visualization
  private void clearAllFrames() {
    for (int i = 0; i < eventFrames.length; i++) {
      eventFrames[i].clearImage();
    }
    for (int i = 0; i < physicalFrames.length; i++) {
      physicalFrames[i].clearImage();
    }
  }

  // for testing
  public float getFilteredPercentage() {
    return 100 * filteredEventCount / eventCount;
  }
}
