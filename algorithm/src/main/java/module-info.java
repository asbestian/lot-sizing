module algorithm {
  exports de.asbestian.lotsizing.algorithm;
  exports de.asbestian.lotsizing.algorithm.scc;
  exports de.asbestian.lotsizing.algorithm.cycle;

  opens de.asbestian.lotsizing.algorithm.scc;
  opens de.asbestian.lotsizing.algorithm.cycle;

  requires input;
  requires graph;
  requires org.jgrapht.core;
  requires org.slf4j;
}
