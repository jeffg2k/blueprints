package com.tinkerpop.blueprints.pgm.util.io.net;

/**
 * A reader for the Pajek Graph Format (net).
 * <p/>
 * NET definition taken from
 * (https://gephi.org/users/supported-graph-formats/pajek-net-format/)
 * <p/>
 *
 * @author Jeff Gentes
 */
public class NETTokens {
    public static final String NET = "net";
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String LABEL = "label";
    public static final String WEIGHT = "weight";
    public static final String NEXT = "*";
    public static final String EDGES = "*edges";
    public static final String NODES = "*vertices";
    public static final String ARCS = "*arcs";
    public static final String SIZE = "size";
    public static final String X = "x_fact";
    public static final String Y = "y_fact";
    public static final String ICCOLOR = "ic";
    public static final String BCCOLOR = "bc";
    public static final String ECOLOR = "c";

}
