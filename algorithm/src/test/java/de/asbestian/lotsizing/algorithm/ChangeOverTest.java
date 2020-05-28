package de.asbestian.lotsizing.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.asbestian.lotsizing.graph.Schedule;
import de.asbestian.lotsizing.input.Input;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** @author Sebastian Schenker */
class ChangeOverTest {

  private Input input;
  private Schedule schedule;

  @BeforeEach
  void setUp() {
    input = mock(Input.class);
    /* change over cost matrix:
         0 3 5
         2 0 4
         6 1 0
    */
    when(input.getChangeOverCost(0, 0)).thenReturn(0);
    when(input.getChangeOverCost(0, 1)).thenReturn(3);
    when(input.getChangeOverCost(0, 2)).thenReturn(5);
    when(input.getChangeOverCost(1, 0)).thenReturn(2);
    when(input.getChangeOverCost(1, 1)).thenReturn(0);
    when(input.getChangeOverCost(1, 2)).thenReturn(4);
    when(input.getChangeOverCost(2, 0)).thenReturn(6);
    when(input.getChangeOverCost(2, 1)).thenReturn(1);
    when(input.getChangeOverCost(2, 2)).thenReturn(0);
    schedule = mock(Schedule.class);
  }

  @Test
  void getPartialChangeOverCost_originalSubSchedule() {
    final IntList nonIdleSchedule = new IntArrayList(List.of(0, 2, 1, 1, 0, 2));
    when(schedule.getScheduleWithoutIdleSlots()).thenReturn(nonIdleSchedule);
    final ChangeOver changeOver = new ChangeOver(input, schedule);

    assertEquals(5 + 1 + 2, changeOver.getPartialChangeOverCost(0, 3, List.of(0, 2, 1, 1)));
    assertEquals(1 + 2 + 5, changeOver.getPartialChangeOverCost(2, 5, List.of(1, 1, 0, 2)));
    assertEquals(5 + 1 + 2 + 5, changeOver.getPartialChangeOverCost(1, 4, List.of(2, 1, 1, 0)));
  }

  @Test
  void getPartialChangeOverCost_permutatedSubSchedule() {
    final IntList nonIdleSchedule = new IntArrayList(List.of(0, 2, 1, 1, 0, 2));
    when(schedule.getScheduleWithoutIdleSlots()).thenReturn(nonIdleSchedule);
    final ChangeOver changeOver = new ChangeOver(input, schedule);

    assertEquals(6 + 3, changeOver.getPartialChangeOverCost(0, 2, List.of(2, 0, 1)));
    assertEquals(4 + 1 + 2, changeOver.getPartialChangeOverCost(3, 5, List.of(2, 1, 0)));
    assertEquals(3 + 4 + 1 + 2, changeOver.getPartialChangeOverCost(1, 3, List.of(1, 2, 1)));
  }

  @Test
  void computeCandidates_maxCardinalityTwo() {
    final IntList nonIdleSchedule = new IntArrayList(List.of(0, 2, 1, 1, 0, 2));
    when(schedule.getScheduleWithoutIdleSlots()).thenReturn(nonIdleSchedule);
    final ChangeOver changeOver = new ChangeOver(input, schedule);

    final List<IntList> candidates = changeOver.computeCandidates(2);

    assertTrue(candidates.isEmpty());
  }

  @Test
  void computeCandidates_maxCardinalityFour() {
    final IntList nonIdleSchedule = new IntArrayList(List.of(0, 0, 0, 1));
    when(schedule.getScheduleWithoutIdleSlots()).thenReturn(nonIdleSchedule);
    final ChangeOver changeOver = new ChangeOver(input, schedule);

    final List<IntList> candidates = changeOver.computeCandidates(4);
    final IntList expected = new IntArrayList(List.of(1, 0, 0, 0));

    assertEquals(1, candidates.size());
    assertEquals(expected, candidates.get(0));
  }
}
