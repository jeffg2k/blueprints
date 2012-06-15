package com.tinkerpop.blueprints.util.io.gexf;

/**
 * A reader for the Gephi Format (GEXF).
 * <p/>
 * GEXF definition taken from
 * (http://gexf.net/format/)
 * <p/>
 *
 * @author Jeff Gentes
 * @author Mathieu Bastian <mathieu.bastian@gephi.org> Templated Gephi GEXFImporter
 */
public class GEXFTokens {

    //GEXF
    public static final String GEXF = "gexf";
    public static final String GEXF_VERSION = "version";
    public static final String GRAPH = "graph";
    public static final String GRAPH_DEFAULT_EDGETYPE = "defaultedgetype";
    public static final String GRAPH_TIMEFORMAT = "timeformat";
    public static final String GRAPH_TIMEFORMAT2 = "timetype"; // GEXF 1.1
    public static final String POS = "position";
    public static final String SIZE = "value";
    public static final String START = "start";
    public static final String END = "end";
    public static final String NODE = "node";
    public static final String NODE_ID = "id";
    public static final String EDGE = "edge";
    public static final String EDGE_ID = "id";
    public static final String EDGE_SOURCE = "source";
    public static final String EDGE_TARGET = "target";
    public static final String EDGE_WEIGHT = "weight";
    public static final String EDGE_TYPE = "type";
    public static final String LABEL = "label";
    public static final String COLOR = "color";
    public static final String ATTRIBUTES = "attributes";
    public static final String ATTRIBUTES_TYPE2 = "mode";
    public static final String DIRECTED = "directed"; // directed (0) undirected (1) default is undirected


}
