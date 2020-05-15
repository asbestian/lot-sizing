package de.asbestian.lotsizing.input;

import java.io.IOException;
import java.io.Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * This class handles the file input. An example input looks like:
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
public class Input {

  private final Logger LOGGER = LoggerFactory.getLogger(Input.class);

  private int numTimeSlots;
  private int numTypes;
  private int inventoryCost;
  private final ArrayList<ArrayList<Integer>> demand;
  private final ArrayList<ArrayList<Integer>> changeover_cost;
  private ArrayList<Integer> overallDemandPerType;

  public Input() {
    demand = new ArrayList<>();
    changeover_cost = new ArrayList<>();
  }

  public void read(final String file) {
    try (final BufferedReader bf = new BufferedReader(new FileReader(file))) {
      numTimeSlots = readSingleValueLine(bf.readLine());
      LOGGER.info("Number of time slots: {}", numTimeSlots);
      numTypes = readSingleValueLine(bf.readLine());
      LOGGER.info("Number of types: {}", numTypes);
      for (int type = 0; type < numTypes; ++type) {
        final ArrayList<Integer> type_demand = readMultipleValueLine(bf.readLine());
        assert type_demand.size() == numTimeSlots;
        demand.add(type_demand);
      }
      inventoryCost = readSingleValueLine(bf.readLine());
      LOGGER.info("Inventory cost: {}", inventoryCost);
      for (int type = 0; type < numTypes; ++type) {
        final ArrayList<Integer> cost = readMultipleValueLine(bf.readLine());
        assert cost.size() == numTypes;
        changeover_cost.add(cost);
      }
      overallDemandPerType = computeNumProducedTypeItems();
      LOGGER.info("Overall demand per type: {}", overallDemandPerType);
    } catch (final Exception e) {
      e.printStackTrace();
    }
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

  public int getNumTimeSlots() {
    return numTimeSlots;
  }

  public int getNumTypes() {
    return numTypes;
  }

  public int getInventoryCost() {
    return inventoryCost;
  }

  public int getOverallDemandPerType(final int type) {
    return overallDemandPerType.get(type);
  }

  public int getNumProducedItems() {
    return overallDemandPerType.stream().mapToInt(i -> i).sum();
  }

  public ArrayList<Integer> getDemand(final int type) {
    return demand.get(type);
  }

  /** Returns the change over cost from predType to succType; */
  public int getChangeOverCost(final int predType, final int succType) {
    return changeover_cost.get(predType).get(succType);
  }
}
