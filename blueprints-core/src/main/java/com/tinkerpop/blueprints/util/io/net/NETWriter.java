package com.tinkerpop.blueprints.util.io.net;

import com.tinkerpop.blueprints.*;
import com.tinkerpop.blueprints.util.io.LexicographicalElementComparator;
import com.tinkerpop.blueprints.util.io.gml.GMLTokens;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.Map.Entry;

/**
 * NETWriter writes a Graph to a NET (Pajek) OutputStream.
 * <p/>
 * NET definition taken from
 * (https://gephi.org/users/supported-graph-formats/pajek-net-format/)
 *
 * @author Jeff Gentes
 */
public class NETWriter {

    private static final String DELIMITER = " ";
    private static final String TAB = "\t";
    private static final String NEW_LINE = "\r\n";
    private static final String OPEN_LIST = " [" + NEW_LINE;
    private static final String CLOSE_LIST = "]" + NEW_LINE;
    private final Graph graph;
    private boolean normalize = false;
    private boolean useId = false;
    private String vertexIdKey = GMLTokens.BLUEPRINTS_ID;
    private String edgeIdKey = GMLTokens.BLUEPRINTS_ID;

    /**
     * @param graph the Graph to pull the data from
     */
    public NETWriter(final Graph graph) {
        this.graph = graph;
    }

    /**
     * @param normalize whether to normalize the output. Normalized output is deterministic with respect to the order of
     *                  elements and properties in the resulting XML document, and is compatible with line diff-based tools
     *                  such as Git. Note: normalized output is memory-intensive and is not appropriate for very large graphs.
     */
    public void setNormalize(final boolean normalize) {
        this.normalize = normalize;
    }

    /**
     * @param useId whether to use the blueprints id directly or substitute with a generated integer. To use this option
     *              the blueprints ids must all be Integers of String representations of integers
     */
    public void setUseId(final boolean useId) {
        this.useId = useId;
    }

    /**
     * @param vertexIdKey net property to use for the blueprints vertex id, defaults to {@link com.tinkerpop.blueprints.util.io.net.NETTokens#BLUEPRINTS_ID}
     */
    public void setVertexIdKey(String vertexIdKey) {
        this.vertexIdKey = vertexIdKey;
    }

    /**
     * @param edgeIdKey net property to use for the blueprints edges id, defaults to {@link com.tinkerpop.blueprints.util.io.net.NETTokens#BLUEPRINTS_ID}
     */
    public void setEdgeIdKey(String edgeIdKey) {
        this.edgeIdKey = edgeIdKey;
    }

    /**
     * Write the data in a Graph to a GML OutputStream.
     *
     * @param gMLOutputStream the GML OutputStream to write the Graph data to
     * @throws java.io.IOException thrown if there is an error generating the GML data
     */
    public void outputGraph(final OutputStream gMLOutputStream) throws IOException {

        // ISO 8859-1 as specified in the GML documentation
        Writer writer = new BufferedWriter(new OutputStreamWriter(gMLOutputStream, Charset.forName("ISO-8859-1")));

        List<Vertex> verticies = new ArrayList<Vertex>();
        List<Edge> edges = new ArrayList<Edge>();

        populateLists(verticies, edges);

        if (normalize) {
            LexicographicalElementComparator comparator = new LexicographicalElementComparator();
            Collections.sort(verticies, comparator);
            Collections.sort(edges, comparator);
        }

        writeGraph(writer, verticies, edges);

        writer.flush();
        writer.close();
    }

    private void writeGraph(Writer writer, List<Vertex> verticies, List<Edge> edges) throws IOException {
        Map<Vertex, Integer> ids = new HashMap<Vertex, Integer>();


        writer.write(GMLTokens.GRAPH);
        writer.write(OPEN_LIST);
        writeVerticies(writer, verticies, ids);
        writeEdges(writer, edges, ids);
        writer.write(CLOSE_LIST);

    }

    private void writeVerticies(Writer writer, List<Vertex> verticies, Map<Vertex, Integer> ids) throws IOException {
        int count = 1;
        for (Vertex v : verticies) {
            if (useId) {
                Integer id = Integer.valueOf(v.getId().toString());
                writeVertex(writer, v, id);
                ids.put(v, id);
            } else {
                writeVertex(writer, v, count);
                ids.put(v, count++);
            }

        }
    }

    private void writeVertex(Writer writer, Vertex v, int id) throws IOException {
        writer.write(TAB);
        writer.write(GMLTokens.NODE);
        writer.write(OPEN_LIST);
        writeKey(writer, GMLTokens.ID);
        writeNumberProperty(writer, id);
        writeVertexProperties(writer, v);
        writer.write(TAB);
        writer.write(CLOSE_LIST);
    }

    private void writeEdges(Writer writer, List<Edge> edges, Map<Vertex, Integer> ids) throws IOException {
        for (Edge e : edges) {
            writeEdgeProperties(writer, e, ids.get(e.getVertex(Direction.OUT)), ids.get(e.getVertex(Direction.IN)));
        }
    }

    private void writeEdgeProperties(Writer writer, Edge e, Integer source, Integer target) throws IOException {
        writer.write(TAB);
        writer.write(GMLTokens.EDGE);
        writer.write(OPEN_LIST);
        writeKey(writer, GMLTokens.SOURCE);
        writeNumberProperty(writer, source);
        writeKey(writer, GMLTokens.TARGET);
        writeNumberProperty(writer, target);
        writeKey(writer, GMLTokens.LABEL);
        writeStringProperty(writer, e.getLabel());
        writeEdgeProperties(writer, e);
        writer.write(TAB);
        writer.write(CLOSE_LIST);
    }

    private void writeVertexProperties(Writer writer, Vertex e) throws IOException {
        Object blueprintsId = e.getId();
        if (!useId) {
            writeKey(writer, vertexIdKey);
            if (blueprintsId instanceof Number) {
                writeNumberProperty(writer, (Number) blueprintsId);
            } else {
                writeStringProperty(writer, blueprintsId);
            }
        }
        writeProperties(writer, e);
    }

    private void writeEdgeProperties(Writer writer, Edge e) throws IOException {
        Object blueprintsId = e.getId();
        if (!useId) {
            writeKey(writer, edgeIdKey);
            if (blueprintsId instanceof Number) {
                writeNumberProperty(writer, (Number) blueprintsId);
            } else {
                writeStringProperty(writer, blueprintsId);
            }
        }
        writeProperties(writer, e);
    }

    private void writeProperties(Writer writer, Element e) throws IOException {
        for (String key : e.getPropertyKeys()) {
            Object property = e.getProperty(key);
            writeKey(writer, key);
            writeProperty(writer, property, 0);
        }
    }

    private void writeProperty(Writer writer, Object property, int tab) throws IOException {
        if (property instanceof Number) {
            writeNumberProperty(writer, (Number) property);
        } else if (property instanceof Map) {
            writeMapProperty(writer, (Map<?, ?>) property, tab);
        } else {
            writeStringProperty(writer, property.toString());
        }
    }

    private void writeMapProperty(Writer writer, Map<?, ?> map, int tabs) throws IOException {
        writer.write(OPEN_LIST);
        tabs++;
        for (Entry<?, ?> entry : map.entrySet()) {
            writeTabs(writer, tabs);
            writeKey(writer, entry.getKey().toString());
            writeProperty(writer, entry.getValue(), tabs);
        }
        writeTabs(writer, tabs - 1);
        writer.write(CLOSE_LIST);

    }

    private void writeTabs(Writer writer, int tabs) throws IOException {
        for (int i = 0; i <= tabs; i++) {
            writer.write(TAB);
        }
    }

    private void writeNumberProperty(Writer writer, Number integer) throws IOException {
        writer.write(integer.toString());
        writer.write(NEW_LINE);
    }

    private void writeStringProperty(Writer writer, Object string) throws IOException {
        writer.write("\"");
        writer.write(string.toString());
        writer.write("\"");
        writer.write(NEW_LINE);
    }

    private void writeKey(Writer writer, String command) throws IOException {
        writer.write(TAB);
        writer.write(TAB);
        writer.write(command);
        writer.write(DELIMITER);
    }

    private void populateLists(List<Vertex> verticies, List<Edge> edges) {
        for (Vertex v : graph.getVertices()) {
            verticies.add(v);
        }
        for (Edge e : graph.getEdges()) {
            edges.add(e);
        }
    }

    /**
     * Write the data in a Graph to a GML OutputStream.
     *
     * @param graph               the Graph to pull the data from
     * @param graphMLOutputStream the GML OutputStream to write the Graph data to
     * @throws java.io.IOException thrown if there is an error generating the GML data
     */
    public static void outputGraph(final Graph graph, final OutputStream graphMLOutputStream) throws IOException {
        NETWriter writer = new NETWriter(graph);
        writer.outputGraph(graphMLOutputStream);
    }
}
