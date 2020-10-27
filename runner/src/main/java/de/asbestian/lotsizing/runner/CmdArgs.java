package de.asbestian.lotsizing.runner;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/** @author Sebastian Schenker */
@Command(
    name = "graph-opt",
    mixinStandardHelpOptions = true,
    version = "1.0",
    description = "Lot sizing optimisation via graph algorithms.")
class CmdArgs {

  @Option(
      names = {"-t", "--timeLimit"},
      description = "Time limit (in seconds) of computation. Default value is ${DEFAULT-VALUE}.",
      defaultValue = "600")
  double timeLimit;

  @Option(
      names = {"-e", "--enumerate"},
      description =
          "Attempt full enumeration of the search space. (Note that given time limit applies.)",
      defaultValue = "false")
  boolean enumerate;

  @Option(
      names = {"-n", "--neighbourhood"},
      description =
          "Size of initial demand vertex neighbourhood used in local search procedure. Default value is ${DEFAULT-VALUE}.",
      defaultValue = "4")
  int neighbourhoodSize;

  @Option(
      names = {"-r", "--random"},
      description =
          "Use random schedule as initial schedule. Default is to use the optimal inventory cost schedule is used as initial schedule.",
      defaultValue = "false")
  boolean randomSchedule;

  @Option(
      names = {"-g", "--greatestDescent"},
      description = "Use greatest descent improvement. Default is to first descent improvement.",
      defaultValue = "false")
  boolean greatestDescent;

  @Parameters(paramLabel = "file", description = "The file containing the problem instance.")
  String file;
}
