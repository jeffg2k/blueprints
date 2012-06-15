package com.tinkerpop.blueprints.util.io.dot;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.io.BlueprintsTokens;

import java.awt.*;
import java.io.IOException;
import java.io.StreamTokenizer;

public class DOTParser {

    private final String defaultEdgeLabel;

    private boolean directed = false;

    private int edgeCount = 0;

    private final Graph graph;

    private final String vertexIdKey;

    private final String edgeIdKey;

    private final String edgeLabelKey;


    public DOTParser(final Graph graph, final String defaultEdgeLabel, final String vertexIdKey, final String edgeIdKey,
                     final String edgeLabelKey) {
        this.graph = graph;
        this.vertexIdKey = vertexIdKey;
        this.edgeIdKey = edgeIdKey;
        this.edgeLabelKey = edgeLabelKey;
        this.defaultEdgeLabel = defaultEdgeLabel;
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

    public void parse(StreamTokenizer st) throws IOException {
        while (st.nextToken() != StreamTokenizer.TT_EOF) {
            if (st.ttype == StreamTokenizer.TT_WORD) {
                if (st.sval.equalsIgnoreCase(DOTTokens.DIRECTED) || st.sval.equalsIgnoreCase(DOTTokens.GRAPH)) {
                    directed = st.sval.equalsIgnoreCase(DOTTokens.DIRECTED);

                    st.nextToken();
                    if (st.ttype == StreamTokenizer.TT_WORD) {
                        //graphName = st.sval;
                        st.nextToken();
                    }

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
    }

    private void parseGraph(StreamTokenizer st) throws IOException {
        do {
            st.nextToken();
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
        } while (st.ttype != StreamTokenizer.TT_EOF);
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
                String attributeName = st.sval;
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
                    if (st.ttype == StreamTokenizer.TT_WORD || st.ttype == '"') ;
                    else {
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
