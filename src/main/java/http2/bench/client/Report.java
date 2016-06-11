package http2.bench.client;

import org.HdrHistogram.Histogram;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
class Report {
  final long expectedRequests;
  final long elapsed;
  final Histogram histogram;
  final int responseCount;
  final double ratio;
  final int connectFailureCount;
  final int resetCount;
  final int missedRequests;
  final int[] statuses;
  final long bytesRead;
  final long byteWritten;

  public Report(long expectedRequests, long elapsed, Histogram histogram, int responseCount, double ratio,
                int connectFailureCount, int resetCount, int missedRequests, int[] statuses,
                long bytesRead, long bytesWritten) {
    this.expectedRequests = expectedRequests;
    this.elapsed = elapsed;
    this.histogram = histogram;
    this.responseCount = responseCount;
    this.ratio = ratio;
    this.connectFailureCount = connectFailureCount;
    this.resetCount = resetCount;
    this.missedRequests = missedRequests;
    this.statuses = statuses;
    this.bytesRead = bytesRead;
    this.byteWritten = bytesWritten;
  }

  void prettyPrint() {
    double elapsedSeconds = TimeUnit.NANOSECONDS.toSeconds(elapsed);
    System.out.format("finished in %.2fs, %.2fs req/s, %.2fs ratio%n", elapsedSeconds, responseCount / elapsedSeconds, ratio);
    System.out.format("requests: %d total, %d errored, %d expected%n", responseCount, connectFailureCount + resetCount, expectedRequests);
    System.out.format("status codes: %d 2xx, %d 3xx, %d 4xx, %d 5xx, %d others%n", statuses[0], statuses[1], statuses[2], statuses[3], statuses[4]);
    System.out.format("bytes read: %d%n", bytesRead);
    System.out.format("bytes written: %d%n", byteWritten);
    System.out.format("missed requests: %d%n", missedRequests);
    System.out.println("min    = " + TimeUnit.NANOSECONDS.toMillis(histogram.getMinValue()));
    System.out.println("max    = " + TimeUnit.NANOSECONDS.toMillis(histogram.getMaxValue()));
    System.out.println("50%    = " + TimeUnit.NANOSECONDS.toMillis(histogram.getValueAtPercentile(50)));
    System.out.println("90%    = " + TimeUnit.NANOSECONDS.toMillis(histogram.getValueAtPercentile(90)));
    System.out.println("99%    = " + TimeUnit.NANOSECONDS.toMillis(histogram.getValueAtPercentile(99)));
    System.out.println("99.9%  = " + TimeUnit.NANOSECONDS.toMillis(histogram.getValueAtPercentile(99.9)));
    System.out.println("99.99% = " + TimeUnit.NANOSECONDS.toMillis(histogram.getValueAtPercentile(99.99)));
  }

  void save(String baseName) {
    try (PrintStream ps = new PrintStream(baseName + ".hdr")) {
      histogram.outputPercentileDistribution(ps, 1000000.0);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
