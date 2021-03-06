package com.tinkerpop.blueprints.util.io.net;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.util.wrappers.batch.BatchGraph;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * A reader for the Pajek Graph Format (net).
 * <p/>
 * NET definition taken from
 * (https://gephi.org/users/supported-graph-formats/pajek-net-format/)
 * <p/>
 *
 * @author Jeff Gentes
 * @author Mathieu Bastian - Templated Gephi NETImporter
 */
public class NETReader {
    public static final String DEFAULT_LABEL = "undefined";

    private static final int DEFAULT_BUFFER_SIZE = 1000;

    private final Graph graph;

    private final String defaultEdgeLabel;

    private int edgeCount = 0;

    private String vertexIdKey;

    private String edgeIdKey;

    private String edgeLabelKey = NETTokens.LABEL;

    /**
     * Create a new NET reader
     * <p/>
     * (Uses default edge label DEFAULT_LABEL)
     *
     * @param graph the graph to load data into
     */
    public NETReader(Graph graph) {
        this(graph, DEFAULT_LABEL);
    }

    /**
     * Create a new NET reader
     *
     * @param graph            the graph to load data into
     * @param defaultEdgeLabel the default edge label to be used if the NET edge does not define a label
     */
    public NETReader(Graph graph, String defaultEdgeLabel) {
        this.graph = graph;
        this.defaultEdgeLabel = defaultEdgeLabel;
    }

    /**
     * @param vertexIdKey NET property to use as id for verticies
     */
    public void setVertexIdKey(String vertexIdKey) {
        this.vertexIdKey = vertexIdKey;
    }

    /**
     * @param edgeIdKey NET property to use as id for edges
     */
    public void setEdgeIdKey(String edgeIdKey) {
        this.edgeIdKey = edgeIdKey;
    }

    /**
     * @param edgeLabelKey NET property to assign edge Labels to
     */
    public void setEdgeLabelKey(String edgeLabelKey) {
        this.edgeLabelKey = edgeLabelKey;
    }
    
    /**
     * Read the NET from from the stream.
     * <p/>
     * If the file is malformed incomplete data can be loaded.
     *
     * @param inputStream
     * @throws IOException
     */
    public void inputGraph(InputStream inputStream) throws IOException {
        inputGraph(this.graph, inputStream, DEFAULT_BUFFER_SIZE, this.defaultEdgeLabel,
                this.vertexIdKey, this.edgeIdKey, this.edgeLabelKey);
    }

    /**
     * Read the NET from from the stream.
     * <p/>
     * If the file is malformed incomplete data can be loaded.
     *
     * @param inputStream
     * @throws IOException
     */
    public void inputGraph(InputStream inputStream, int bufferSize) throws IOException {
        inputGraph(this.graph, inputStream, bufferSize, this.defaultEdgeLabel,
                this.vertexIdKey, this.edgeIdKey, this.edgeLabelKey);
    }

    /**
     * Load the NET file into the Graph.
     *
     * @param graph to receive the data
     * @param inputStream NET file
     * @throws IOException thrown if the data is not valid
     */
    public static void inputGraph(Graph graph, InputStream inputStream) throws IOException {
        inputGraph(graph, inputStream, DEFAULT_BUFFER_SIZE, DEFAULT_LABEL, null, null, null);
    }

    /**
     * Load the NET file into the Graph.
     *
     * @param inputGraph to receive the data
     * @param inputStream NET file
     * @param defaultEdgeLabel default edge label to be used if not defined in the data
     * @param vertexIdKey if the id of a vertex is a &lt;data/&gt; property, fetch it from the data property.
     * @param edgeIdKey if the id of an edge is a &lt;data/&gt; property, fetch it from the data property.
     * @param edgeLabelKey if the label of an edge is a &lt;data/&gt; property, fetch it from the data property.
     * @throws IOException thrown if the data is not valid
     */
    public static void inputGraph(final Graph inputGraph, final InputStream inputStream, final int bufferSize,
                                  final String defaultEdgeLabel, final String vertexIdKey, final String edgeIdKey,
                                  final String edgeLabelKey) throws IOException {
        final BatchGraph graph = BatchGraph.wrap(inputGraph, bufferSize);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("ISO-8859-1")));
        try {
            new NETParser(graph, defaultEdgeLabel, vertexIdKey, edgeIdKey, edgeLabelKey).parse(reader);
            graph.stopTransaction(TransactionalGraph.Conclusion.SUCCESS);
        } catch (IOException e) {
            throw new IOException("NET malformed", e);
        }

    }

}
