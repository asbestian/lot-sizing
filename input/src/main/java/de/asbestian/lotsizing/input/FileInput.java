package de.asbestian.lotsizing.input;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles the file input. An example file looks like:
 *
 * <pre>
 *   5  // number of time slots
 *   2  // number of machine types
 *   0 1 0 0 1  // demand of first type
 *   1 0 0 0 1  // demand of second type
 *   2  // inventory cost
 *   0 5  // changeover cost from first type to second type
 *   3 0  // changeover cost from second type to first type
 * </pre>
 *
 * @author Sebastian Schenker
 */
public class FileInput implements Input {
  private static final Logger LOGGER = LoggerFactory.getLogger(Input.class);

  private final int numTimeSlots;
  private final int numTypes;
  private final int inventoryCost;
  private final ArrayList<ArrayList<Integer>> demand;
  private final ArrayList<ArrayList<Integer>> changeover_cost;
  private final ArrayList<Integer> overallDemandPerType;

  /**
   * Constructor
   *
   * @param file the path to the input file
   */
  @Inject
  public FileInput(final String file) {
    demand = new ArrayList<>();
    changeover_cost = new ArrayList<>();
    final ArrayDeque<String> input = new ArrayDeque<>();
    try (final BufferedReader bf = new BufferedReader(new FileReader(file))) {
      bf.lines().filter(line -> !line.isBlank()).forEachOrdered(input::addLast);
    } catch (final Exception e) {
      e.printStackTrace();
    }
    numTimeSlots = readSingleValueLine(input.removeFirst());
    LOGGER.info("Number of time slots: {}", numTimeSlots);
    numTypes = readSingleValueLine(input.removeFirst());
    LOGGER.info("Number of types: {}", numTypes);
    for (int type = 0; type < numTypes; ++type) {
      final ArrayList<Integer> type_demand = readMultipleValueLine(input.removeFirst());
      assert type_demand.size() == numTimeSlots;
      demand.add(type_demand);
    }
    inventoryCost = readSingleValueLine(input.removeFirst());
    LOGGER.info("Inventory cost: {}", inventoryCost);
    for (int type = 0; type < numTypes; ++type) {
      final ArrayList<Integer> cost = readMultipleValueLine(input.removeFirst());
      assert cost.size() == numTypes;
      changeover_cost.add(cost);
    }
    overallDemandPerType = computeNumProducedTypeItems();
    LOGGER.info("Overall demand per type: {}", overallDemandPerType);
  }

  private ArrayList<Integer> computeNumProducedTypeItems() {
    return demand.stream()
        .mapToInt(a -> a.stream().mapToInt(i -> i).sum())
        .boxed()
        .collect(Collectors.toCollection(ArrayList::new));
  }

  private static int readSingleValueLine(final String line) {
    return Integer.parseInt(line);
  }

  private static ArrayList<Integer> readMultipleValueLine(final String line) {
    final String[] values = line.split(" ");
    return Arrays.stream(values)
        .map(Integer::valueOf)
        .collect(Collectors.toCollection(ArrayList::new));
  }

  @Override
  public int getNumTimeSlots() {
    return numTimeSlots;
  }

  @Override
  public int getNumTypes() {
    return numTypes;
  }

  @Override
  public int getInventoryCost() {
    return inventoryCost;
  }

  @Override
  public int getOverallDemandPerType(final int type) {
    return overallDemandPerType.get(type);
  }

  @Override
  public int getNumProducedItems() {
    return overallDemandPerType.stream().mapToInt(i -> i).sum();
  }

  @Override
  public List<Integer> getDemand(final int type) {
    return Collections.unmodifiableList(demand.get(type));
  }

  @Override
  public int getChangeOverCost(final int predType, final int succType) {
    return changeover_cost.get(predType).get(succType);
  }
}
