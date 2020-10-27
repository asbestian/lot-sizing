module graph {
  exports de.asbestian.lotsizing.graph.vertex;
  exports de.asbestian.lotsizing.graph;

  opens de.asbestian.lotsizing.graph;

  requires org.slf4j;
  requires org.jgrapht.core;
  requires input;
  requires it.unimi.dsi.fastutil;
  requires dagger;
  requires javax.inject;
}
