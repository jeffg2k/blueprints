package com.tinkerpop.blueprints.pgm.util.io.tlp;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.TransactionalGraph;
import com.tinkerpop.blueprints.pgm.Vertex;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;

/**
 * A reader for the Tulip Graph Format (tlp).
 * <p/>
 * TLP definition taken from
 * (http://tulip.labri.fr/TulipDrupal/?q=tlp-file-format)
 * <p/>
 *
 * @author Jeff Gentes
 * @author Sebastien Heymann - Templated Gephi TLPImporter
 */
public class TLPReader {
    public static final String DEFAULT_LABEL = "undefined";

    private static final int DEFAULT_BUFFER_SIZE = 1000;

    private final Graph graph;

    private final String defaultEdgeLabel;

    /**
     * Create a new TLP reader
     * <p/>
     * (Uses default edge label DEFAULT_LABEL)
     *
     * @param graph the graph to load data into
     */
    public TLPReader(Graph graph) {
        this(graph, DEFAULT_LABEL);
    }

    /**
     * Create a new TLP reader
     *
     * @param graph            the graph to load data into
     * @param defaultEdgeLabel the default edge label to be used if the TLP edge does not define a label
     */
    public TLPReader(Graph graph, String defaultEdgeLabel) {
        this.graph = graph;
        this.defaultEdgeLabel = defaultEdgeLabel;
    }

    /**
     * Read the TLP from from the stream.
     * <p/>
     * If the file is malformed incomplete data can be loaded.
     *
     * @param inputStream  TLP file
     * @throws java.io.IOException thrown if the data is not valid
     */
    public void inputGraph(InputStream inputStream) throws IOException {
        inputGraph(inputStream, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Load the TLP file into the Graph.
     *
     * @param graph       to receive the data
     * @param inputStream TLP file
     * @throws IOException thrown if the data is not valid
     */
    public static void inputGraph(Graph graph, InputStream inputStream) throws IOException {
        inputGraph(graph, inputStream, DEFAULT_LABEL);
    }

    /**
     * Load the TLP file into the Graph.
     *
     * @param graph            to receive the data
     * @param inputStream      TLP file
     * @param defaultEdgeLabel default edge label to be used if not defined in the data
     * @throws IOException thrown if the data is not valid
     */
    public static void inputGraph(Graph graph, InputStream inputStream, String defaultEdgeLabel) throws IOException {
        new TLPReader(graph, defaultEdgeLabel).inputGraph(inputStream, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Read the TLP from from the stream.
     * <p/>
     * If the file is malformed incomplete data can be loaded.
     *
     * @param inputStream      TLP file
     * @throws IOException thrown if the data is not valid
     */
    public void inputGraph(InputStream inputStream, int bufferSize) throws IOException {
        int previousMaxBufferSize = 0;
        if (graph instanceof TransactionalGraph) {
            previousMaxBufferSize = ((TransactionalGraph) graph).getMaxBufferSize();
            ((TransactionalGraph) graph).setMaxBufferSize(bufferSize);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("ISO-8859-1")));

        try {
            int state = 0; // 0=topology, 1=properties
            String property = null;
            String nodedefault = "";
            String edgedefault = "";
            boolean cancel = false;
            while (reader.ready() && !cancel) {
                String line = reader.readLine();
                if (!isComment(line)) {
                    String[] tokens = customSplit(line);
                    if (tokens.length > 0) {
                        if (state == 0) {
                            // topology
                            if (tokens[0].equals(TLPTokens.NODE)) {
                                //Nodes
                                parseNodes(tokens);
                            } else if (tokens[0].equals(TLPTokens.EDGE)) {
                                //Edges
                                parseEdge(tokens);
                            } else if (tokens[0].equals(TLPTokens.PROPERTY)) {
                                //switch to properties grabbing
                                state = 1;
                            }
                        }
                        if (state == 1) {
                            // properties
                            if (tokens[0].equals(TLPTokens.PROPERTY) && tokens.length > 3) {
                                property = tokens[3].replaceAll("\"", "");
                            }
                            if (property != null) {
                                if (tokens[0].equals(TLPTokens.DEFAULT) && tokens.length > 2) {
                                    nodedefault = tokens[1];
                                    edgedefault = tokens[2];
                                } else if (tokens[0].equals(TLPTokens.NODEPROPERTY) && tokens.length > 1) {
                                    //Node
                                    Vertex node = graph.getVertex(tokens[1]);
                                    String value;
                                    if (tokens.length < 3 || tokens[2] == null || tokens[2].length() == 0) {
                                        value = nodedefault;
                                    } else {
                                        value = tokens[2];
                                    }
                                    node.setProperty(property, value);
                                } else if (tokens[0].equals(TLPTokens.EDGE) && tokens.length > 1) {
                                    //Edge
                                    Edge edge = graph.getEdge(tokens[1]);
                                    String value;
                                    if (tokens.length < 3 || tokens[2] == null || tokens[2].length() == 0) {
                                        value = edgedefault;
                                    } else {
                                        value = tokens[2];
                                    }
                                    edge.setProperty(property, value);
                                }
                            }
                        }
                    }
                }
            }

            if (graph instanceof TransactionalGraph) {
                ((TransactionalGraph) graph).setMaxBufferSize(previousMaxBufferSize);
            }
        } catch (IOException e) {
            throw new IOException(e);
        }

    }

    private boolean isComment(String s) {
        return s.startsWith(TLPTokens.COMMENT_CHAR);
    }

    private void parseNodes(String[] tokens) {
        for (int i = 1; i < tokens.length; i++) {
            String id = tokens[i];
            graph.addVertex(id);
        }
    }

    private void parseEdge(String[] tokens) {
        if (tokens.length > 3) {
            String id = tokens[1];
            Vertex source = graph.getVertex(tokens[2]);
            Vertex target = graph.getVertex(tokens[3]);
            graph.addEdge(id, source, target, defaultEdgeLabel);
        }
    }

    private static String[] customSplit(final String input) {
        ArrayList<String> elements = new ArrayList<String>();
        StringBuilder elementBuilder = new StringBuilder();

        boolean isQuoted = false;
        for (char c : input.toCharArray()) {
            if (c == '\"') {
                isQuoted = !isQuoted;
                // continue;        // changed according to the OP comment - \" shall not be skipped
            }
            if ((c == ' ' && !isQuoted) || c == ')' && !isQuoted) {
                elements.add(elementBuilder.toString().trim());
                elementBuilder = new StringBuilder();
                continue;
            }
            elementBuilder.append(c);
        }
        elements.add(elementBuilder.toString().trim());
        return elements.toArray(new String[elements.size()]);
    }


}
