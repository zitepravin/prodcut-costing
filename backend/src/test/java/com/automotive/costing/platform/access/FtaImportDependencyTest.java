package com.automotive.costing.platform.access;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
class FtaImportDependencyTest {
  @Test void central_access_contains_dependency() throws Exception {
    Path p = Path.of("src/main/java/com/automotive/costing/platform/access/service/CentralAccessService.java");
    assertTrue(Files.exists(p), "CentralAccessService source missing at " + p.toAbsolutePath());
    String src = Files.readString(p);
    assertTrue(src.contains("FTA_TRADE_AGREEMENT"));
    assertTrue(src.contains("assertFeatureEnabled(customerId, \"IMPORT_COSTING\")"));
  }
}
