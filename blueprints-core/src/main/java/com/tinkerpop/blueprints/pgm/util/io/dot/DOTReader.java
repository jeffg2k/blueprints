package com.tinkerpop.blueprints.pgm.util.io.dot;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.TransactionalGraph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.blueprints.pgm.util.io.BlueprintsTokens;

import java.awt.*;
import java.io.*;
import java.nio.charset.Charset;

/**
 * A reader for the GraphViz Format (DOT).
 * <p/>
 * DOT definition taken from
 * (http://www.graphviz.org/pub/scm/graphviz2/doc/info/lang.html)
 * <p/>
 *
 * @author Jeff Gentes
 * @author Stuart Hendren (http://stuarthendren.net) Templated Blueprint GMLParser
 * @author Mathieu Bastian <mathieu.bastian@gephi.org> Templated Gephi DOTImporter
 */

public class DOTReader {
    public static final String DEFAULT_LABEL = "undefined";

    private static final int DEFAULT_BUFFER_SIZE = 1000;

    private final Graph graph;

    private final String defaultEdgeLabel;

    private boolean directed = false;

    private int edgeCount = 0;

    /**
     * Create a new DOT reader
     * <p/>
     * (Uses default edge label DEFAULT_LABEL)
     *
     * @param graph the graph to load data into
     */
    public DOTReader(Graph graph) {
        this(graph, DEFAULT_LABEL);
    }

    /**
     * Create a new DOT reader
     *
     * @param graph            the graph to load data into
     * @param defaultEdgeLabel the default edge label to be used if the DOT edge does not define a label
     */
    public DOTReader(Graph graph, String defaultEdgeLabel) {
        this.graph = graph;
        this.defaultEdgeLabel = defaultEdgeLabel;
    }

    /**
     * Read the DOT from from the stream.
     * <p/>
     * If the file is malformed incomplete data can be loaded.
     *
     * @param inputStream
     * @throws IOException
     */
    public void inputGraph(InputStream inputStream) throws IOException {
        inputGraph(inputStream, DEFAULT_BUFFER_SIZE);
    }


    /**
     * Load the DOT file into the Graph.
     *
     * @param graph       to receive the data
     * @param inputStream DOT file
     * @throws IOException thrown if the data is not valid
     */
    public static void inputGraph(Graph graph, InputStream inputStream) throws IOException {
        inputGraph(graph, inputStream, DEFAULT_LABEL);
    }

    /**
     * Load the DOT file into the Graph.
     *
     * @param graph            to receive the data
     * @param inputStream      DOT file
     * @param defaultEdgeLabel default edge label to be used if not defined in the data
     * @throws IOException thrown if the data is not valid
     */
    public static void inputGraph(Graph graph, InputStream inputStream, String defaultEdgeLabel) throws IOException {
        new DOTReader(graph, defaultEdgeLabel).inputGraph(inputStream, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Read the DOT from from the stream.
     * <p/>
     * If the file is malformed incomplete data can be loaded.
     *
     * @param inputStream
     * @throws IOException
     */
    public void inputGraph(InputStream inputStream, int bufferSize) throws IOException {
        int previousMaxBufferSize = 0;
        if (graph instanceof TransactionalGraph) {
            previousMaxBufferSize = ((TransactionalGraph) graph).getMaxBufferSize();
            ((TransactionalGraph) graph).setMaxBufferSize(bufferSize);
        }

        Reader r = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("ISO-8859-1")));
        StreamTokenizer st = new StreamTokenizer(r);

        try {
            st.resetSyntax();
            st.eolIsSignificant(false);
            st.slashStarComments(true);
            st.slashSlashComments(true);
            st.whitespaceChars(0, ' ');
            st.wordChars(' ' + 1, '\u00ff');
            st.ordinaryChar('[');
            st.ordinaryChar(']');
            st.ordinaryChar('{');
            st.ordinaryChar('}');
            st.ordinaryChar('-');
            st.ordinaryChar('>');
            st.ordinaryChar('/');
            st.ordinaryChar('*');
            st.ordinaryChar(',');
            st.quoteChar('"');
            st.whitespaceChars(';', ';');
            st.ordinaryChar('=');

            parse(st);

            if (graph instanceof TransactionalGraph) {
                ((TransactionalGraph) graph).setMaxBufferSize(previousMaxBufferSize);
            }
        } catch (IOException e) {
            throw new IOException(error(st), e);
        }

    }

    private boolean hasNext(StreamTokenizer st) throws IOException {
        return st.nextToken() != StreamTokenizer.TT_EOF;
    }

    private String error(StreamTokenizer st) {
        return "DOT malformed line number " + st.lineno() + ": ";
    }

    private boolean notLineBreak(int type) {
        return type != StreamTokenizer.TT_EOL;
    }

    private void parse(StreamTokenizer st) throws IOException {
        while (st.nextToken() != StreamTokenizer.TT_EOF) {
            if (st.ttype == StreamTokenizer.TT_WORD) {
                if (st.sval.equalsIgnoreCase(DOTTokens.DIRECTED) || st.sval.equalsIgnoreCase(DOTTokens.GRAPH)) {
                    directed = st.sval.equalsIgnoreCase(DOTTokens.DIRECTED);

                    st.nextToken();

                    while (st.ttype != '{') {
                        st.nextToken();
                        if (st.ttype == StreamTokenizer.TT_EOF) {
                            return;
                        }
                    }
                    parseGraph(st);
                }
            }
        }

        while (hasNext(st)) {
            int type = st.ttype;
            if (notLineBreak(type) && type == StreamTokenizer.TT_WORD) {
                String value = st.sval;
                if (DOTTokens.GRAPH.equalsIgnoreCase(value)) {
                    parseGraph(st);
                    if (!hasNext(st)) {
                        return;
                    }
                }
            }
        }
        throw new IOException("Graph not complete");
    }

    private void parseGraph(StreamTokenizer st) throws IOException {
        while (hasNext(st)) {
            String key = st.sval;
            if (key == null || key.equalsIgnoreCase(DOTTokens.GRAPH) || key.equalsIgnoreCase(DOTTokens.NODE)
                    || key.equalsIgnoreCase(DOTTokens.EDGE)) {
            } else {
                String nodeId = nodeID(st);
                st.nextToken();

                if (st.ttype == '-') {
                    Vertex node = getOrCreateNode(nodeId);
                    edgeStructure(st, node);
                } else if (st.ttype == '[') {
                    Vertex node = getOrCreateNode(nodeId);
                    nodeAttributes(st, node);
                }
            }
        }
        throw new IOException("Graph not complete");
    }

    protected void edgeStructure(StreamTokenizer st, final Vertex node) throws IOException {
        st.nextToken();

        Edge edge = null;
        if (st.ttype == '>' || st.ttype == '-') {
            st.nextToken();
            if (st.ttype == '{') {
                while (true) {
                    st.nextToken();
                    if (st.ttype == '}') {
                        break;
                    } else {
                        nodeID(st);
                        Object edgeId = edgeCount++;
                        edge = graph.addEdge(edgeId, node, getOrCreateNode("" + st.sval), defaultEdgeLabel);
                        if (directed) {
                            edge.setProperty(BlueprintsTokens.DIRECTED, directed);
                        }
                    }
                }
            } else {
                nodeID(st);
                Object edgeId = edgeCount++;
                edge = graph.addEdge(edgeId, node, getOrCreateNode("" + st.sval), defaultEdgeLabel);
                if (directed) {
                    edge.setProperty(BlueprintsTokens.DIRECTED, directed);
                }
            }
        } else {
            if (st.ttype == StreamTokenizer.TT_WORD) {
                st.pushBack();
            }
            return;
        }

        st.nextToken();

        if (st.ttype == '[' && edge != null) {
            edgeAttributes(st, edge);
        } else {
            st.pushBack();
        }
    }

    protected void nodeAttributes(StreamTokenizer st, final Vertex node) throws IOException {
        st.nextToken();

        if (st.ttype == ']' || st.ttype == StreamTokenizer.TT_EOF) {
            return;
        } else if (st.ttype == StreamTokenizer.TT_WORD) {
            // attributes
            if (st.sval.equalsIgnoreCase(DOTTokens.LABEL)) {
                st.nextToken();
                if (st.ttype == '=') {
                    st.nextToken();
                    if (st.ttype == StreamTokenizer.TT_WORD || st.ttype == '"') {
                        node.setProperty(BlueprintsTokens.LABEL, st.sval);
                    } else {
                        st.pushBack();
                    }
                } else {
                    st.pushBack();
                }
            } else if (st.sval.equalsIgnoreCase(DOTTokens.COLOR)) {
                st.nextToken();
                if (st.ttype == '=') {
                    st.nextToken();
                    if (st.ttype == StreamTokenizer.TT_WORD || st.ttype == '"') {
                        if (DOTTokens.containsColor(st.sval)) {
                            node.setProperty(BlueprintsTokens.COLOR, DOTTokens.getColorFromName(st.sval));
                        } else {
                            try {
                                String[] colors = st.sval.split(" ");
                                Color nodeColor = new Color(Float.parseFloat(colors[0]), Float.parseFloat(colors[1]), Float.parseFloat(colors[2]));
                                node.setProperty(BlueprintsTokens.COLOR, nodeColor.getRGB());
                            } catch (Exception e) {
                            }
                        }
                    } else {
                        st.pushBack();
                    }
                } else {
                    st.pushBack();
                }
            } else if (st.sval.equalsIgnoreCase(DOTTokens.POS)) {
                st.nextToken();
                if (st.ttype == '=') {
                    st.nextToken();
                    if (st.ttype == StreamTokenizer.TT_WORD || st.ttype == '"') {
                        try {
                            String[] positions = st.sval.split(",");
                            if (positions.length == 2) {
                                node.setProperty(BlueprintsTokens.X, Float.parseFloat(positions[0]));
                                node.setProperty(BlueprintsTokens.Y, Float.parseFloat(positions[1]));

                            } else if (positions.length == 3) {
                                node.setProperty(BlueprintsTokens.X, Float.parseFloat(positions[0]));
                                node.setProperty(BlueprintsTokens.Y, Float.parseFloat(positions[1]));
                                node.setProperty(BlueprintsTokens.Z, Float.parseFloat(positions[2]));
                            }
                        } catch (Exception e) {
                        }
                    } else {
                        st.pushBack();
                    }
                } else {
                    st.pushBack();
                }
            } else if (st.sval.equalsIgnoreCase(DOTTokens.STYLE)) {
                st.nextToken();
                if (st.ttype == '=') {
                    st.nextToken();
                    if (st.ttype == StreamTokenizer.TT_WORD || st.ttype == '"') {
                        node.setProperty(BlueprintsTokens.STYLE, st.sval);
                    } else {
                        st.pushBack();
                    }
                } else {
                    st.pushBack();
                }
            } else {
                // other attributes
                String attributeName =  st.sval;
                st.nextToken();
                if (st.ttype == '=') {
                    st.nextToken();
                    if (st.ttype == StreamTokenizer.TT_WORD || st.ttype == '"') {
                        node.setProperty(attributeName, st.sval);
                    } else {
                        st.pushBack();
                    }
                } else {
                    st.pushBack();
                }
            }
        }
        nodeAttributes(st, node);
    }


    protected void edgeAttributes(StreamTokenizer st, final Edge edge) throws IOException {
        st.nextToken();
        if (st.ttype == ']' || st.ttype == StreamTokenizer.TT_EOF) {
            return;
        } else if (st.ttype == StreamTokenizer.TT_WORD) {
            String attributeName = st.sval;
            st.nextToken();
            if (st.ttype == '=') {
                st.nextToken();
                if (st.ttype == StreamTokenizer.TT_WORD || st.ttype == '"') {
                    edge.setProperty(attributeName, st.sval);
                } else {
                    st.pushBack();
                }
            } else {
                st.pushBack();
            }


            if (st.sval.equalsIgnoreCase(DOTTokens.LABEL)) {
                st.nextToken();
                if (st.ttype == '=') {
                    st.nextToken();
                    if (st.ttype == StreamTokenizer.TT_WORD || st.ttype == '"') {
                        edge.setProperty(BlueprintsTokens.LABEL, st.sval);
                    } else {
                       st.pushBack();
                    }
                } else {
                    st.pushBack();
                }
            } else if (st.sval.equalsIgnoreCase(DOTTokens.COLOR)) {
                st.nextToken();
                if (st.ttype == '=') {
                    st.nextToken();
                    if (st.ttype == StreamTokenizer.TT_WORD || st.ttype == '"') {
                        if (DOTTokens.containsColor(st.sval)) {
                            edge.setProperty(BlueprintsTokens.COLOR, DOTTokens.getColorFromName(st.sval));
                        } else {
                            try {
                                String[] colors = st.sval.split(" ");
                                Color edgeColor = new Color(Float.parseFloat(colors[0]), Float.parseFloat(colors[1]), Float.parseFloat(colors[2]));
                                edge.setProperty(BlueprintsTokens.COLOR, edgeColor.getRGB());
                            } catch (Exception e) {
                            }
                        }
                    } else {
                        st.pushBack();
                    }
                } else {
                    st.pushBack();
                }
            } else if (st.sval.equalsIgnoreCase(DOTTokens.STYLE)) {
                st.nextToken();
                if (st.ttype == '=') {
                    st.nextToken();
                    if (st.ttype == StreamTokenizer.TT_WORD || st.ttype == '"'); else {
                        edge.setProperty(BlueprintsTokens.STYLE, st.sval);
                    }
                } else {
                    st.pushBack();
                }
            } else if (st.sval.equalsIgnoreCase(DOTTokens.WEIGHT)) {
                st.nextToken();
                if (st.ttype == '=') {
                    st.nextToken();
                    if (st.ttype == StreamTokenizer.TT_WORD || st.ttype == '"') {
                        try {
                            Float weight = Float.parseFloat(st.sval);
                            edge.setProperty(BlueprintsTokens.WEIGHT, weight);
                        } catch (Exception e) {
                        }
                    } else {
                        st.pushBack();
                    }
                } else {
                    st.pushBack();
                }
            }

        }
        edgeAttributes(st, edge);
    }

    protected String nodeID(StreamTokenizer st) {
        if (st.ttype == '"' || st.ttype == StreamTokenizer.TT_WORD || (st.ttype >= 'a' && st.ttype <= 'z')
                || (st.ttype >= 'A' && st.ttype <= 'Z')) {
            return st.sval;
        } else {
            return null;
        }
    }

    protected Vertex getOrCreateNode(String id) {
        if (graph.getVertex(id) == null) {
            return graph.addVertex(id);
        }
        return graph.getVertex(id);
    }

}
