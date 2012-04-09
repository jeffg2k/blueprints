package com.tinkerpop.blueprints.pgm.util.io.dot;

/**
 * A collection of tokens used for DOT related data.
 * <p/>
 * DOT definition taken from
 * (http://www.graphviz.org/pub/scm/graphviz2/doc/info/lang.html)
 * <p/>
 *
 * @author Jeff Gentes
 */

public class DOTTokens {
    public static final String DOT = "dot";
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String LABEL = "label";
    public static final String GRAPH = "graph";
    public static final String NODE = "node";
    public static final String EDGE = "edge";
    public static final String DIRECTED = "digraph"; // digraph (0) graph (1) default is undirected

    /**
     * Special token used to store Blueprint ids as they may not be integers
     */
    public static final String BLUEPRINTS_ID = "blueprintsId";
}
