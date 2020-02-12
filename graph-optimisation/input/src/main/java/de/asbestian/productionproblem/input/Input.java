package de.asbestian.productionproblem.input;

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

  private int numTimeSlots;
  private int numTypes;
  private int inventoryCost;
  private final ArrayList<ArrayList<Integer>> demand;
  private final ArrayList<ArrayList<Integer>> changeover_cost;
  private ArrayList<Integer> numProducedTypeItems;

  public Input() {
    demand = new ArrayList<>();
    changeover_cost = new ArrayList<>();
  }

  public void read(final String file) {
    try (BufferedReader bf = new BufferedReader(new FileReader(file))) {
      numTimeSlots = readSingleValueLine(bf.readLine());
      numTypes = readSingleValueLine(bf.readLine());
      for (int type = 0; type < numTypes; ++type) {
        ArrayList<Integer> type_demand = readMultipleValueLine(bf.readLine());
        assert type_demand.size() == numTimeSlots;
        demand.add(type_demand);
      }
      inventoryCost = readSingleValueLine(bf.readLine());
      for (int type = 0; type < numTypes; ++type) {
        ArrayList<Integer> cost = readMultipleValueLine(bf.readLine());
        assert cost.size() == numTypes;
        changeover_cost.add(cost);
      }
      numProducedTypeItems = computeNumProducedTypeItems();
    } catch (Exception e) {
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

  private static ArrayList<Integer> readMultipleValueLine(String line) {
    String[] values = line.split(" ");
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

  public int getNumProducedTypeItems(final int type) {
    return numProducedTypeItems.get(type);
  }

  public int getNumProducedItems() {
    return numProducedTypeItems.stream().mapToInt(i -> i).sum();
  }

  public ArrayList<Integer> getDemand(final int type) {
    return demand.get(type);
  }

  /**
   * Returns the change over cost from predType to succType;
   */
  public int getChangeOverCost(int predType, int succType) {
    return changeover_cost.get(predType).get(succType);
  }
}
