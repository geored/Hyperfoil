package io.hyperfoil.clustering.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import io.hyperfoil.api.config.Benchmark;
import io.hyperfoil.api.config.BenchmarkData;
import io.hyperfoil.core.parser.BenchmarkParser;
import io.hyperfoil.core.parser.ParserException;
import io.hyperfoil.util.Util;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class PersistenceUtil {
    private static final Logger log = LoggerFactory.getLogger(PersistenceUtil.class);

    public static void store(Benchmark benchmark, Path dir) {
        try {
           byte[] bytes = Util.serialize(benchmark);
           if (bytes != null) {
              Path path = dir.resolve(benchmark.name() + ".serialized");
              try {
                 Files.write(path, bytes);
                 log.info("Stored benchmark '{}' in {}", benchmark.name(), path);
              } catch (IOException e) {
                 log.error(e, "Failed to persist benchmark {} to {}", benchmark.name(), path);
              }
           }
        } catch (IOException e) {
           log.error("Failed to serialize", e);
        }
        if (benchmark.source() != null) {
            Path path = dir.resolve(benchmark.name() + ".yaml");
            try {
                Files.write(path, benchmark.source().getBytes(StandardCharsets.UTF_8));
                log.info("Stored benchmark '{}' in {}", benchmark.name(), path);
            } catch (IOException e) {
                log.error("Failed to persist benchmark {} to {}", e, benchmark.name(), path);
            }
           Path dataDirPath = dir.resolve(benchmark.name() + ".data");
            if (!benchmark.files().isEmpty()) {
               File dataDir = dataDirPath.toFile();
               if (dataDir.exists()) {
                  // Make sure the directory is empty
                  for (String file : dataDir.list()) {
                     if (!dataDirPath.resolve(file).toFile().delete()) {
                        log.warn("Could not delete old file {}/{}", dataDirPath, file);
                     }
                  }
               } else {
                  if (!dataDir.mkdir()) {
                     log.error("Couldn't create data dir {}", dataDir);
                     return;
                  }
               }
            }
            for (Map.Entry<String, byte[]> entry : benchmark.files().entrySet()) {
               try {
                  String sanitizedName = PersistedBenchmarkData.sanitize(entry.getKey());
                  Files.write(dataDirPath.resolve(sanitizedName), entry.getValue());
               } catch (IOException e) {
                  log.error("Couldn't persiste data file {}", e, entry.getKey());
               }
            }
        }
   }

   public static Benchmark load(Path file) {
       String filename = file.getFileName().toString();
       if (filename.endsWith(".yaml")) {
           BenchmarkData data = BenchmarkData.EMPTY;
           String dataDirName = filename.substring(0, filename.length() - 5) + ".data";
           Path dataDirPath = file.getParent().resolve(dataDirName);
           File dataDir = dataDirPath.toFile();
           if (dataDir.exists()) {
              if (dataDir.isDirectory()) {
                 data = new PersistedBenchmarkData(dataDirPath);
              } else {
                 log.error("Expected data dir {} to be a directory!", dataDirName);
              }
           }
           try {
               String source = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
               Benchmark benchmark = BenchmarkParser.instance().buildBenchmark(source, data);
               log.info("Loaded benchmark '{}' from {}", benchmark.name(), file);
               return benchmark;
           } catch (IOException e) {
               log.error(e, "Cannot read file {}", file);
           } catch (ParserException e) {
               log.error(e, "Cannot parser file {}", file);
           }
       } else if (filename.endsWith(".serialized")) {
           try {
               Benchmark benchmark = Util.deserialize(Files.readAllBytes(file));
               if (benchmark != null) {
                   log.info("Loaded benchmark '{}' from {}", benchmark.name(), file);
                   return benchmark;
               }
           } catch (Exception e) {
               log.error(e, "Cannot read file {}", file);
               return null;
           }
       } else if (file.toFile().isDirectory() && filename.endsWith(".data")) {
           log.debug("Ignoring directory {}", filename);
       } else {
           log.warn("Unknown benchmark file format: {}", file);
       }
       return null;
   }
}
