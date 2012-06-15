package com.tinkerpop.blueprints.util.io.tlp;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.io.BlueprintsTokens;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

public class TLPParser {
    private final String defaultEdgeLabel;

    private final Graph graph;

    private final String vertexIdKey;

    private final String edgeIdKey;

    private final String edgeLabelKey;


    public TLPParser(final Graph graph, final String defaultEdgeLabel, final String vertexIdKey, final String edgeIdKey,
                     final String edgeLabelKey) {
        this.graph = graph;
        this.vertexIdKey = vertexIdKey;
        this.edgeIdKey = edgeIdKey;
        this.edgeLabelKey = edgeLabelKey;
        this.defaultEdgeLabel = defaultEdgeLabel;
    }

    public void parse(final BufferedReader reader) throws IOException {
        int state = 0; // 0=topology, 1=properties
        String property = null;
        String nodeDefault = "";
        String edgeDefault = "";
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
                                nodeDefault = tokens[1];
                                edgeDefault = tokens[2];
                            } else if (tokens[0].equals(TLPTokens.NODEPROPERTY) && tokens.length > 1) {
                                //Node
                                Vertex node = graph.getVertex(tokens[1]);
                                String value;
                                if (tokens.length < 3 || tokens[2] == null || tokens[2].length() == 0) {
                                    value = nodeDefault;
                                } else {
                                    value = tokens[2].replaceAll("\"", "");
                                }
                                if (property.equalsIgnoreCase(TLPTokens.LABEL)) {
                                    node.setProperty(BlueprintsTokens.LABEL, value);
                                } else if (property.equalsIgnoreCase(TLPTokens.COLOR)) {
                                    String[] parseVal = value.replace("(", "").replace(")","").split(",");
                                    Color color = new Color(128,128,128);
                                    if (parseVal.length > 3) {
                                        color = new Color(Integer.parseInt(parseVal[0]), Integer.parseInt(parseVal[1]), Integer.parseInt(parseVal[2]), Integer.parseInt(parseVal[3]));
                                        node.setProperty(BlueprintsTokens.COLOR, color.getRGB());
                                    } else if (parseVal.length > 2) {
                                        color = new Color(Integer.parseInt(parseVal[0]), Integer.parseInt(parseVal[1]), Integer.parseInt(parseVal[2]));
                                        node.setProperty(BlueprintsTokens.COLOR, color.getRGB());
                                    }
                                } else {
                                    node.setProperty(property, value);
                                }
                            } else if (tokens[0].equals(TLPTokens.EDGE) && tokens.length > 1) {
                                //Edge
                                Edge edge = graph.getEdge(tokens[1]);
                                String value;
                                if (tokens.length < 3 || tokens[2] == null || tokens[2].length() == 0) {
                                    value = edgeDefault;
                                } else {
                                    value = tokens[2].replaceAll("\"", "");
                                }
                                if (property.equalsIgnoreCase(TLPTokens.LABEL)) {
                                    edge.setProperty(BlueprintsTokens.LABEL, value);
                                }  else if (property.equalsIgnoreCase(TLPTokens.COLOR)) {
                                    String[] parseVal = value.replace("(", "").replace(")","").split(",");
                                    Color color = new Color(128,128,128);
                                    if (parseVal.length > 3) {
                                        color = new Color(Integer.parseInt(parseVal[0]), Integer.parseInt(parseVal[1]), Integer.parseInt(parseVal[2]), Integer.parseInt(parseVal[3]));
                                        edge.setProperty(BlueprintsTokens.COLOR, color.getRGB());
                                    } else if (parseVal.length > 2) {
                                        color = new Color(Integer.parseInt(parseVal[0]), Integer.parseInt(parseVal[1]), Integer.parseInt(parseVal[2]));
                                        edge.setProperty(BlueprintsTokens.COLOR, color.getRGB());
                                    }
                                } else {
                                    edge.setProperty(property, value);
                                }
                            }
                        }
                    }
                }
            }
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
