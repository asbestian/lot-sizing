package de.asbestian.lotsizing.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** @author Sebastian Schenker */
class FileInputTest {

  private FileInput input;

  @BeforeEach
  void setUp() {
    final String path = "src/test/resources/Instance-5timeslots_2types.txt";
    assert Files.exists(Paths.get(path));
    input = new FileInput(path);
  }

  @Test
  void getNumberOfTimeSlots() {
    assertEquals(5, input.getNumTimeSlots());
  }

  @Test
  void getNumberOfTypes() {
    assertEquals(2, input.getNumTypes());
  }

  @Test
  void getInventoryCost() {
    assertEquals(2, input.getInventoryCost());
  }

  @Test
  void getNumberOfProducedItems() {
    assertEquals(4, input.getNumProducedItems());
  }

  @Test
  void getChangeOverCost() {
    assertEquals(0, input.getChangeOverCost(0, 0));
    assertEquals(5, input.getChangeOverCost(0, 1));
    assertEquals(3, input.getChangeOverCost(1, 0));
    assertEquals(0, input.getChangeOverCost(1, 1));
  }

  @Test
  void getDemand() {
    final List<Integer> expectedDemandType0 = Arrays.asList(0, 1, 0, 0, 1);
    final List<Integer> expectedDemandType1 = Arrays.asList(1, 0, 0, 0, 1);

    assertIterableEquals(expectedDemandType0, input.getDemand(0));
    assertIterableEquals(expectedDemandType1, input.getDemand(1));
  }
}
