package com.tinkerpop.blueprints.pgm.util.io;

/**
 * Generic Tokens for Translating between different
 * file formats when reading and writing graph files.
 *
 * @author Jeff Gentes
 */
public class BlueprintsTokens {
    public static final String ID = "id";
    public static final String LABEL = "label";
    public static final String NODE = "node";
    public static final String EDGE = "edge";
    public static final String DIRECTED = "directed";
    public static final String COLOR = "color";     //Store as an RGB INT - Color.getRGB()
    public static final String COLOR2 = "color2";   //Store as an RGB INT - Color.getRGB()
    public static final String X = "x";
    public static final String Y = "y";
    public static final String Z = "z";
    public static final String TYPE = "type";
    public static final String SIZE = "size";
    public static final String WEIGHT = "weight";
    public static final String STYLE = "style";
}
