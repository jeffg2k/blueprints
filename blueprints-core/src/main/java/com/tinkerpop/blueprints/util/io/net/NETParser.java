package com.tinkerpop.blueprints.util.io.net;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.io.BlueprintsTokens;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class NETParser {
    /**
     * <Mapped ID String, ID Object>
     */
    //private final Map<Object, Object> vertexIdMap = new HashMap<Object, Object>();

    private final Map<Object, Object> vertexMappedIdMap = new HashMap<Object, Object>();

    private final String defaultEdgeLabel;

    private final Graph graph;

    private final String vertexIdKey;

    private final String edgeIdKey;

    private final String edgeLabelKey;

    private boolean directed = false;

    private boolean cancel = false;

    private int edgeCount = 0;

    public NETParser(final Graph graph, final String defaultEdgeLabel, final String vertexIdKey, final String edgeIdKey,
                     final String edgeLabelKey) {
        this.graph = graph;
        this.vertexIdKey = vertexIdKey;
        this.edgeIdKey = edgeIdKey;
        this.edgeLabelKey = edgeLabelKey;
        this.defaultEdgeLabel = defaultEdgeLabel;
    }

    public void parse(final BufferedReader reader) throws IOException {

        try {
            // ignore everything until we see '*Vertices'
            String curLine = null;
            try {
                curLine = skip(reader, NETTokens.NODES);
            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            if (curLine == null) // no vertices in the graph; return empty graph
            {
                System.err.println("importerNET_error_dataformat1");
                return;
            }

            // create appropriate number of vertices
            StringTokenizer st = new StringTokenizer(curLine);
            st.nextToken(); // skip past "*vertices";
            int num_vertices = Integer.parseInt(st.nextToken());
            for (int i = 0; i < num_vertices; i++) {
                String label = "" + (i + 1);
                Vertex node = graph.addVertex(label);
                node.setProperty(BlueprintsTokens.LABEL, label);
            }

            curLine = null;
            while (reader.ready()) {
                if (cancel) {
                    reader.close();
                    return;
                }
                curLine = reader.readLine();
                if (curLine == null || curLine.startsWith("*")) {
                    break;
                }
                if (curLine.isEmpty()) { // skip blank lines
                    System.err.println("importerNET_error_dataformat2");
                    continue;
                }

                try {
                    readVertex(curLine, num_vertices);
                } catch (IllegalArgumentException iae) {
                    reader.close();
                    throw iae;
                }
            }

            //Get arcs
            curLine = readArcsOrEdges(curLine, reader);

            //Get edges
            readArcsOrEdges(curLine, reader);
            reader.close();
        } catch (Exception e) {
            throw new IOException("NET malformed", e);
        }
    }

    private void readVertex(String curLine, int num_vertices) throws Exception {
        String[] parts = null;
        int firstParts = -1;     // index of first coordinate in parts; -1 indicates no coordinates found
        String index;
        String label = null;
        // if there are quote marks on this line, split on them; label is surrounded by them
        if (curLine.indexOf('"') != -1) {
            String[] initial_split = curLine.trim().split("\"");
            // if there are any quote marks, there should be exactly 2
            if (initial_split.length < 1 || initial_split.length > 3) {
                System.err.println("importerNET_error_dataformat3");
            }
            index = initial_split[0].trim();
            if (initial_split.length > 1) {
                label = initial_split[1].trim();
            }

            if (initial_split.length == 3) {
                parts = initial_split[2].trim().split("\\s+", -1);
            }
            firstParts = 0;
        } else // no quote marks, but are there coordinates?
        {
            parts = curLine.trim().split("\\s+", -1);
            index = parts[0];
            switch (parts.length) {
                case 1:         // just the ID; nothing to do, continue
                    break;
                case 2:         // just the ID and a label
                    label = parts[1];
                    break;
                case 3:         // ID, no label, coordinates
                    firstParts = 1;
                    break;
                case 4:         // ID, label, (x,y) coordinates
                    firstParts = 2;
                    break;
            }
        }
        int v_id = Integer.parseInt(index) - 1; // go from 1-based to 0-based index
        if (v_id >= num_vertices || v_id < 0) {
            System.err.println("importerNET_error_dataformat4");
        }

        Vertex node = getOrCreateNode(v_id);

        // only attach the label if there's one to attach
        if (label != null && label.length() > 0) {
            node.setProperty(BlueprintsTokens.LABEL, label);
        }

        // parse the rest of the line
        if (firstParts != -1 && parts != null && parts.length >= firstParts + 2) {
            int i = firstParts;
            //Coordinates
            if (i < parts.length - 1) {
                try {
                    float x = Float.parseFloat(parts[i]);
                    float y = Float.parseFloat(parts[i + 1]);
                    node.setProperty(BlueprintsTokens.X, x);
                    node.setProperty(BlueprintsTokens.Y, y);
                    i++;
                } catch (Exception e) {
                    System.err.println("importerNET_error_dataformat5");
                }
            }

            //Size
            if (i < parts.length - 1) {
                try {
                    float size = Float.parseFloat(parts[i]);
                    node.setProperty(BlueprintsTokens.SIZE, size);
                    i++;
                } catch (Exception e) {
                    System.err.println("importerNET_error_dataformat6");
                }
            }

            // parse colors
            for (; i < parts.length - 1; i++) {
                // node's internal color
                if (NETTokens.ICCOLOR.equals(parts[i])) {
                    //String colorName = parts[i + 1].replaceAll(" ", ""); // remove spaces from color's name so we can look it up
                    node.setProperty(BlueprintsTokens.COLOR, parts[i + 1]);
                    break;
                }
                if (NETTokens.BCCOLOR.equals(parts[i])) {
                    //String colorName = parts[i + 1].replaceAll(" ", ""); // remove spaces from color's name so we can look it up
                    node.setProperty(BlueprintsTokens.COLOR2, parts[i + 1]);
                    break;
                }
            }
        }
    }

    private String readArcsOrEdges(String curLine, BufferedReader br) throws Exception {
        String nextLine = curLine;

        boolean reading_arcs = false;
        boolean reading_edges = false;

        if (nextLine.toLowerCase().startsWith(NETTokens.ARCS)) {
            reading_arcs = true;
        } else if (nextLine.toLowerCase().startsWith(NETTokens.EDGES)) {
            reading_edges = true;
        }

        if (!(reading_arcs || reading_edges)) {
            return nextLine;
        }

        boolean is_list = false;
        if (nextLine.toLowerCase().endsWith("list")) {
            is_list = true;
        }

        while (br.ready()) {
            if (cancel) {
                return nextLine;
            }
            nextLine = br.readLine();
            if (nextLine == null || nextLine.startsWith(NETTokens.NEXT)) {
                break;
            }
            if (nextLine.equals("")) { // skip blank lines
                System.err.println("importerNET_error_dataformat2");
                continue;
            }

            StringTokenizer st = new StringTokenizer(nextLine.trim());

            int vid1 = Integer.parseInt(st.nextToken()) - 1;
            Vertex nodeFrom = graph.getVertex(vid1);

            if (is_list) // one source, multiple destinations
            {
                do {
                    int vid2 = Integer.parseInt(st.nextToken()) - 1;
                    Vertex nodeTo = getOrCreateNode(vid2);

                    Edge edge = graph.addEdge(edgeCount, nodeFrom, nodeTo, defaultEdgeLabel);
                    edgeCount++;
                } while (st.hasMoreTokens());
            } else // one source, one destination, at most one weight
            {
                int vid2 = Integer.parseInt(st.nextToken()) - 1;
                Vertex nodeTo = getOrCreateNode(vid2);

                Edge edge = graph.addEdge(edgeCount, nodeFrom, nodeTo, defaultEdgeLabel);
                edgeCount++;
                // get the edge weight
                if (st.hasMoreTokens()) {
                    float edgeWeight = 1f;
                    try {
                        edgeWeight = new Float(st.nextToken());
                    } catch (Exception e) {
                        System.err.println("importerNET_error_dataformat7");
                    }

                    edge.setProperty(BlueprintsTokens.WEIGHT, edgeWeight);
                }
                if (st.hasMoreTokens()) {
                    String key = st.nextToken();
                    if (key.equalsIgnoreCase(NETTokens.ECOLOR)) {
                        String value = st.nextToken();
                        edge.setProperty(BlueprintsTokens.COLOR, value);
                    }
                }
            }
        }
        return nextLine;
    }

    protected Vertex getOrCreateNode(int id) {
        if (graph.getVertex(id) == null) {
            return graph.addVertex(id);
        }
        return graph.getVertex(id);
    }

    private String skip(BufferedReader br, String str) throws Exception {
        while (br.ready()) {
            String curLine = br.readLine();
            if (curLine == null) {
                break;
            }
            curLine = curLine.trim();
            if (curLine.toLowerCase().startsWith(str)) {
                return curLine;
            }
        }
        return null;
    }
}
