package com.automotive.costing.importcosting.engine;
import com.automotive.costing.importcosting.domain.AllocationMethod;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
/** Allocate a shipment-level charge across lines. Avoids N+1 by pure in-memory math. */
public final class AllocationEngine {
  private static final int SCALE = 6;
  private AllocationEngine() {}

  public record LineDriver(String itemId, BigDecimal value, BigDecimal quantity, BigDecimal netWeight,
                           BigDecimal grossWeight, BigDecimal volume, BigDecimal chargeableWeight) {}
  public record AllocationLine(String itemId, BigDecimal allocatedAmount, BigDecimal allocationPct,
                               BigDecimal driverQuantity, String method) {}

  public static List<AllocationLine> allocate(BigDecimal charge, String method, List<LineDriver> lines) {
    List<AllocationLine> out = new ArrayList<>();
    if (lines == null || lines.isEmpty()) return out;
    String m = method == null ? AllocationMethod.VALUE.name() : method.trim().toUpperCase();
    BigDecimal totalDriver = BigDecimal.ZERO;
    List<BigDecimal> drivers = new ArrayList<>(lines.size());
    for (LineDriver ld : lines) {
      BigDecimal d = driverOf(ld, m);
      drivers.add(d);
      totalDriver = totalDriver.add(d);
    }
    if (totalDriver.compareTo(BigDecimal.ZERO) <= 0) {
      // equal split fallback with explicit note via zero driver flag
      BigDecimal each = nz(charge).divide(BigDecimal.valueOf(lines.size()), SCALE, RoundingMode.HALF_UP);
      BigDecimal pct = BigDecimal.ONE.divide(BigDecimal.valueOf(lines.size()), 8, RoundingMode.HALF_UP);
      for (LineDriver ld : lines) {
        out.add(new AllocationLine(ld.itemId(), each, pct, BigDecimal.ZERO, m + "_EQUAL_FALLBACK"));
      }
      return out;
    }
    for (int i = 0; i < lines.size(); i++) {
      LineDriver ld = lines.get(i);
      BigDecimal d = drivers.get(i);
      BigDecimal pct = d.divide(totalDriver, 8, RoundingMode.HALF_UP);
      BigDecimal amt = nz(charge).multiply(pct).setScale(SCALE, RoundingMode.HALF_UP);
      out.add(new AllocationLine(ld.itemId(), amt, pct, d, m));
    }
    return out;
  }

  private static BigDecimal driverOf(LineDriver ld, String m) {
    return switch (m) {
      case "QUANTITY" -> nz(ld.quantity());
      case "NET_WEIGHT" -> nz(ld.netWeight());
      case "GROSS_WEIGHT" -> nz(ld.grossWeight());
      case "VOLUME" -> nz(ld.volume());
      case "CHARGEABLE_WEIGHT" -> nz(ld.chargeableWeight());
      case "VALUE" -> nz(ld.value());
      default -> nz(ld.value());
    };
  }

  private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
