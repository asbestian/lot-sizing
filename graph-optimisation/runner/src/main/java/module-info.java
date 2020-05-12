module runner {
  opens de.asbestian.lotsizing.runner;

  requires algorithm;
  requires graph;
  requires input;
  requires visualisation;
  requires org.slf4j;
  requires info.picocli;
  requires org.jgrapht.core;
}
