package com.tinkerpop.blueprints.pgm.util.io.gexf;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.TransactionalGraph;
import com.tinkerpop.blueprints.pgm.Vertex;

import javax.xml.stream.*;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.nio.charset.Charset;

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
public class GEXFReader {
    public static final String DEFAULT_LABEL = "undefined";

    private static final int DEFAULT_BUFFER_SIZE = 1000;

    private final Graph graph;

    private final String defaultEdgeLabel;

    private boolean directed = false;

    private int edgeCount = 0;

    //Architecture
    private Reader reader;
    private boolean cancel;
    private XMLStreamReader xmlReader;

    /**
     * Create a new GEXF reader
     * <p/>
     * (Uses default edge label DEFAULT_LABEL)
     *
     * @param graph the graph to load data into
     */
    public GEXFReader(Graph graph) {
        this(graph, DEFAULT_LABEL);
    }

    /**
     * Create a new GEXF reader
     *
     * @param graph            the graph to load data into
     * @param defaultEdgeLabel the default edge label to be used if the GEXF edge does not define a label
     */
    public GEXFReader(Graph graph, String defaultEdgeLabel) {
        this.graph = graph;
        this.defaultEdgeLabel = defaultEdgeLabel;
    }

    /**
     * Read the GEXF from from the stream.
     * <p/>
     * If the file is malformed incomplete data can be loaded.
     *
     * @param inputStream
     * @throws java.io.IOException
     */
    public void inputGraph(InputStream inputStream) throws IOException {
        inputGraph(inputStream, DEFAULT_BUFFER_SIZE);
    }


    /**
     * Load the GEXF file into the Graph.
     *
     * @param graph       to receive the data
     * @param inputStream GEXF file
     * @throws IOException thrown if the data is not valid
     */
    public static void inputGraph(Graph graph, InputStream inputStream) throws IOException {
        inputGraph(graph, inputStream, DEFAULT_LABEL);
    }

    /**
     * Load the GEXF file into the Graph.
     *
     * @param graph            to receive the data
     * @param inputStream      GEXF file
     * @param defaultEdgeLabel default edge label to be used if not defined in the data
     * @throws IOException thrown if the data is not valid
     */
    public static void inputGraph(Graph graph, InputStream inputStream, String defaultEdgeLabel) throws IOException {
        new GEXFReader(graph, defaultEdgeLabel).inputGraph(inputStream, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Read the GEXF from from the stream.
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

        reader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("ISO-8859-1")));

        try {

            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            if (inputFactory.isPropertySupported("javax.xml.stream.isValidating")) {
                inputFactory.setProperty("javax.xml.stream.isValidating", Boolean.FALSE);
            }
            inputFactory.setXMLReporter(new XMLReporter() {

                @Override
                public void report(String message, String errorType, Object relatedInformation, Location location) throws XMLStreamException {
                    System.out.println("Error:" + errorType + ", message : " + message);
                }
            });
            xmlReader = inputFactory.createXMLStreamReader(reader);

            while (xmlReader.hasNext()) {

                Integer eventType = xmlReader.next();
                if (eventType.equals(XMLEvent.START_ELEMENT)) {
                    String name = xmlReader.getLocalName();
                    if (GEXFTokens.GEXF.equalsIgnoreCase(name)) {
                        readGexf(xmlReader);
                    } else if (GEXFTokens.GRAPH.equalsIgnoreCase(name)) {
                        readGraph(xmlReader);
                    } else if (GEXFTokens.NODE.equalsIgnoreCase(name)) {
                        readNode(xmlReader, null);
                    } else if (GEXFTokens.EDGE.equalsIgnoreCase(name)) {
                        readEdge(xmlReader);
                    } else if (GEXFTokens.ATTRIBUTES.equalsIgnoreCase(name)) {
                        //readAttributes(xmlReader);
                    }
                } else if (eventType.equals(XMLStreamReader.END_ELEMENT)) {
                    String name = xmlReader.getLocalName();
                    if (GEXFTokens.NODE.equalsIgnoreCase(name)) {
                    }
                }
            }
            xmlReader.close();
            if (graph instanceof TransactionalGraph) {
                ((TransactionalGraph) graph).setMaxBufferSize(previousMaxBufferSize);
            }

        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }

    private void readGexf(XMLStreamReader reader) throws Exception {
        String version = "";

        //Attributes
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String attName = reader.getAttributeName(i).getLocalPart();
            if (GEXFTokens.GEXF_VERSION.equalsIgnoreCase(attName)) {
                version = reader.getAttributeValue(i);
            }
        }
    }

    private void readGraph(XMLStreamReader reader) throws Exception {
        String mode = "";
        String defaultEdgeType = "";
        String start = "";
        String end = "";
        String timeFormat = "";

        //Attributes
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String attName = reader.getAttributeName(i).getLocalPart();
            if (GEXFTokens.GRAPH_DEFAULT_EDGETYPE.equalsIgnoreCase(attName)) {
                defaultEdgeType = reader.getAttributeValue(i);
                if (!defaultEdgeType.isEmpty()) {
                    directed = defaultEdgeType.equalsIgnoreCase(GEXFTokens.DIRECTED);
                }
            } else if (GEXFTokens.ATTRIBUTES_TYPE2.equalsIgnoreCase(attName)) {
                mode = reader.getAttributeValue(i);
            } else if (GEXFTokens.START.equalsIgnoreCase(attName)) {
                start = reader.getAttributeValue(i);
            } else if (GEXFTokens.END.equalsIgnoreCase(attName)) {
                end = reader.getAttributeValue(i);
            } else if (GEXFTokens.GRAPH_TIMEFORMAT.equalsIgnoreCase(attName) || GEXFTokens.GRAPH_TIMEFORMAT2.equalsIgnoreCase(attName)) {
                timeFormat = reader.getAttributeValue(i);
            }
        }
    }

    private void readNode(XMLStreamReader reader, Vertex parent) throws Exception {
        String id = "";

        //Attributes
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String attName = reader.getAttributeName(i).getLocalPart();
            if (GEXFTokens.NODE_ID.equalsIgnoreCase(attName)) {
                id = reader.getAttributeValue(i);
            }
        }

        if (id.isEmpty()) {
            System.err.println("importerGEXF_error_nodeid");
            return;
        }

        Vertex node = null;
        if (graph.getVertex(id) != null) {
            node = graph.getVertex(id);
        } else {
            node = graph.addVertex(id);
        }

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String attName = reader.getAttributeName(i).getLocalPart();
            if (!GEXFTokens.NODE_ID.equalsIgnoreCase(attName)) {
                String attValue = reader.getAttributeValue(i);
                node.setProperty(attName, attValue);
            }
        }

        boolean end = false;
        while (reader.hasNext() && !end) {
            int type = reader.next();

            switch (type) {
                case XMLStreamReader.START_ELEMENT:
                    readNode(reader, node);
                    break;

                case XMLStreamReader.END_ELEMENT:
                    if (GEXFTokens.NODE.equalsIgnoreCase(xmlReader.getLocalName())) {
                        end = true;
                    }
                    break;
            }
        }

    }

    private void readEdge(XMLStreamReader reader) throws Exception {
        String id = "";
        String label = "";
        String source = "";
        String target = "";

        //Attributes
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String attName = reader.getAttributeName(i).getLocalPart();
            if (GEXFTokens.EDGE_SOURCE.equalsIgnoreCase(attName)) {
                source = reader.getAttributeValue(i);
            } else if (GEXFTokens.EDGE_TARGET.equalsIgnoreCase(attName)) {
                target = reader.getAttributeValue(i);
            } else if (GEXFTokens.EDGE_ID.equalsIgnoreCase(attName)) {
                id = reader.getAttributeValue(i);
            } else if (GEXFTokens.EDGE_LABEL.equalsIgnoreCase(attName)) {
                label = reader.getAttributeValue(i);
            }
        }

        Vertex n1 = graph.getVertex(source);
        Vertex n2 = graph.getVertex(target);
        Edge edge = null;
        if (n1 != null && n2 != null) {
            edge = graph.addEdge(id,n1,n2,label);
        } else {
            return;
        }
        if (directed) {
            edge.setProperty(GEXFTokens.DIRECTED, directed);
        }
        //Attributes
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String attName = reader.getAttributeName(i).getLocalPart();
            if (!GEXFTokens.EDGE_SOURCE.equalsIgnoreCase(attName)
                    && !GEXFTokens.EDGE_TARGET.equalsIgnoreCase(attName)
                    && !GEXFTokens.EDGE_ID.equalsIgnoreCase(attName)
                    && !GEXFTokens.EDGE_LABEL.equalsIgnoreCase(attName)) {
                String value = reader.getAttributeValue(i);
                edge.setProperty(attName, value);
            }
        }
    }

}



