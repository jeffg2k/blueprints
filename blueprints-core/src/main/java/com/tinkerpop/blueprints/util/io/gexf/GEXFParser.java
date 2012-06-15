package com.tinkerpop.blueprints.util.io.gexf;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.io.BlueprintsTokens;

import javax.xml.stream.*;
import javax.xml.stream.events.XMLEvent;
import java.awt.*;
import java.io.BufferedReader;

public class GEXFParser {

    private final String defaultEdgeLabel;

    private final Graph graph;

    private final String vertexIdKey;

    private final String edgeIdKey;

    private final String edgeLabelKey;

    private boolean directed = false;
    private boolean cancel;
    private XMLStreamReader xmlReader;


    public GEXFParser(final Graph graph, final String defaultEdgeLabel, final String vertexIdKey, final String edgeIdKey,
                     final String edgeLabelKey) {
        this.graph = graph;
        this.vertexIdKey = vertexIdKey;
        this.edgeIdKey = edgeIdKey;
        this.edgeLabelKey = edgeLabelKey;
        this.defaultEdgeLabel = defaultEdgeLabel;
    }

    public void parse(BufferedReader reader) {
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
            if (!attName.equalsIgnoreCase(GEXFTokens.NODE_ID)) {
                String attValue = reader.getAttributeValue(i);
                if (attName.equalsIgnoreCase(GEXFTokens.LABEL)) {
                    node.setProperty(BlueprintsTokens.LABEL, attValue);
                } else if (attName.equalsIgnoreCase(GEXFTokens.COLOR)) {
                    Color color = readColor(reader);
                    node.setProperty(BlueprintsTokens.COLOR, color.getRGB());
                } else if (attName.equalsIgnoreCase(GEXFTokens.POS)) {
                    readNodePosition(reader, node);
                } else {
                    node.setProperty(attName, attValue);
                }
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
            } else if (GEXFTokens.LABEL.equalsIgnoreCase(attName)) {
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
            edge.setProperty(BlueprintsTokens.DIRECTED, directed);
        }
        //Attributes
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String attName = reader.getAttributeName(i).getLocalPart();
            if (!GEXFTokens.EDGE_SOURCE.equalsIgnoreCase(attName)
                    && !GEXFTokens.EDGE_TARGET.equalsIgnoreCase(attName)
                    && !GEXFTokens.EDGE_ID.equalsIgnoreCase(attName)
                    && !GEXFTokens.LABEL.equalsIgnoreCase(attName)) {
                String value = reader.getAttributeValue(i);
                if (GEXFTokens.COLOR.equalsIgnoreCase(attName)) {
                    Color color = readColor(reader);
                    edge.setProperty(BlueprintsTokens.COLOR, color.getRGB());
                } else if (GEXFTokens.EDGE_WEIGHT.equalsIgnoreCase(attName)) {
                    edge.setProperty(BlueprintsTokens.WEIGHT, value);
                } else if (GEXFTokens.EDGE_TYPE.equalsIgnoreCase(attName)) {
                    edge.setProperty(BlueprintsTokens.TYPE, value);
                } else {
                    edge.setProperty(attName, value);
                }
            }
        }
    }

    public static Color readColor(XMLStreamReader reader) {
        String rStr = "";
        String gStr = "";
        String bStr = "";
        String aStr = "";

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String attName = reader.getAttributeName(i).getLocalPart();
            if ("r".equalsIgnoreCase(attName)) {
                rStr = reader.getAttributeValue(i);
            } else if ("g".equalsIgnoreCase(attName)) {
                gStr = reader.getAttributeValue(i);
            } else if ("b".equalsIgnoreCase(attName)) {
                bStr = reader.getAttributeValue(i);
            } else if ("a".equalsIgnoreCase(attName)) {
                aStr = reader.getAttributeValue(i);
            }
        }

        int r = (rStr.isEmpty()) ? 0 : Integer.parseInt(rStr);
        int g = (gStr.isEmpty()) ? 0 : Integer.parseInt(gStr);
        int b = (bStr.isEmpty()) ? 0 : Integer.parseInt(bStr);
        float a = (aStr.isEmpty()) ? 0 : Float.parseFloat(aStr); //not used
        if(r < 0 || r > 255) {
            r=128;
        }
        if(g < 0 || g > 255) {
            g=128;
        }
        if(b < 0 || b > 255) {
            r=128;
        }
        if(a < 0f || a > 1f) {
            r=255;
        }

        return new Color(r, g, b);
    }

    private void readNodePosition(XMLStreamReader reader, Vertex node) {
        String xStr = "";
        String yStr = "";
        String zStr = "";

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String attName = reader.getAttributeName(i).getLocalPart();
            if ("x".equalsIgnoreCase(attName)) {
                xStr = reader.getAttributeValue(i);
            } else if ("y".equalsIgnoreCase(attName)) {
                yStr = reader.getAttributeValue(i);
            } else if ("z".equalsIgnoreCase(attName)) {
                zStr = reader.getAttributeValue(i);
            }
        }

        if (!xStr.isEmpty()) {
            float x = Float.parseFloat(xStr);
            node.setProperty(BlueprintsTokens.X, x);
        }
        if (!yStr.isEmpty()) {
            float y = Float.parseFloat(yStr);
            node.setProperty(BlueprintsTokens.Y, y);
        }
        if (!zStr.isEmpty()) {
            float z = Float.parseFloat(zStr);
            node.setProperty(BlueprintsTokens.Z, z);
        }
    }

    private void readNodeSize(XMLStreamReader reader, Vertex node) {
        String attName = reader.getAttributeName(0).getLocalPart();
        if (GEXFTokens.SIZE.equalsIgnoreCase(attName)) {
            String sizeStr = reader.getAttributeValue(0);
            if (!sizeStr.isEmpty()) {
                float size = Float.parseFloat(sizeStr);
                node.setProperty(BlueprintsTokens.SIZE, size);
            }
        }
    }
}
