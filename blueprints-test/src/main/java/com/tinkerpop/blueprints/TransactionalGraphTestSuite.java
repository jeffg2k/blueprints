package com.tinkerpop.blueprints;

import com.tinkerpop.blueprints.TransactionalGraph.Conclusion;
import com.tinkerpop.blueprints.impls.GraphTest;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class TransactionalGraphTestSuite extends TestSuite {

    public TransactionalGraphTestSuite() {
    }

    public TransactionalGraphTestSuite(final GraphTest graphTest) {
        super(graphTest);
    }

    public void testNestedTransactionException() {
        TransactionalGraph graph = (TransactionalGraph) graphTest.generateGraph();
        graph.startTransaction();
        try {
            graph.startTransaction();
            assertTrue(false);
        } catch (IllegalStateException e) {
            assertTrue(true);
        }
        graph.shutdown();
    }

    public void testAutoStartTransaction() {
        TransactionalGraph graph = (TransactionalGraph) graphTest.generateGraph();
        Vertex v1 = graph.addVertex(null);
        if (graph.getFeatures().supportsVertexIteration) {
            assertEquals(count(graph.getVertices()), 1);
        }
        assertEquals(v1,graph.getVertex(v1.getId()));
        graph.stopTransaction(Conclusion.SUCCESS);
        assertEquals(graph.getVertex(v1.getId()), v1);
        if (graph.getFeatures().supportsVertexIteration) {
            assertEquals(count(graph.getVertices()), 1);
        }
        graph.shutdown();

    }


    public void testTransactionsForVertices() {
        TransactionalGraph graph = (TransactionalGraph) graphTest.generateGraph();

        graph.startTransaction();
        Vertex v1 = graph.addVertex(null);
        graph.stopTransaction(Conclusion.SUCCESS);

        graph.startTransaction();
        this.stopWatch();
        if (graph.getFeatures().supportsVertexIteration)
            assertEquals(count(graph.getVertices()), 1);
        assertEquals(v1,graph.getVertex(v1.getId()));
        Vertex v2 = graph.addVertex(null);
        if (graph.getFeatures().supportsVertexIteration)
            assertEquals(count(graph.getVertices()), 2);
        assertEquals(v1,graph.getVertex(v1.getId()));
        assertEquals(v2,graph.getVertex(v2.getId()));
        graph.stopTransaction(Conclusion.FAILURE);

        graph.startTransaction();
        this.stopWatch();
        if (graph.getFeatures().supportsVertexIteration)
            assertEquals(count(graph.getVertices()), 1);
        assertEquals(v1,graph.getVertex(v1.getId()));
        assertEquals(null,graph.getVertex(v2.getId()));
        printPerformance(graph.toString(), 1, "vertex not added in failed transaction", this.stopWatch());
        Vertex v3 = graph.addVertex(null);
        if (graph.getFeatures().supportsVertexIteration)
            assertEquals(count(graph.getVertices()), 2);
        assertEquals(v1,graph.getVertex(v1.getId()));
        assertEquals(null,graph.getVertex(v2.getId()));
        assertEquals(v3,graph.getVertex(v3.getId()));
        graph.stopTransaction(Conclusion.SUCCESS);
        printPerformance(graph.toString(), 1, "vertex added in successful transaction", this.stopWatch());

        if (graph.getFeatures().supportsVertexIteration)
            assertEquals(count(graph.getVertices()), 2);
        assertEquals(v1,graph.getVertex(v1.getId()));
        assertEquals(null,graph.getVertex(v2.getId()));
        assertEquals(v3,graph.getVertex(v3.getId()));

        graph.shutdown();
    }

    public void testBruteVertexTransactions() {
        TransactionalGraph graph = (TransactionalGraph) graphTest.generateGraph();

        this.stopWatch();
        Set<Vertex> added = new HashSet<Vertex>();
        Set<Vertex> notadded = new HashSet<Vertex>();
        for (int i = 0; i < 100; i++) {
            graph.startTransaction();
            added.add(graph.addVertex(null));
            graph.stopTransaction(Conclusion.SUCCESS);
        }
        printPerformance(graph.toString(), 100, "vertices added in 100 successful transactions", this.stopWatch());
        if (graph.getFeatures().supportsVertexIteration)
            assertEquals(count(graph.getVertices()), 100);
        for (Vertex v : added) assertNotNull(graph.getVertex(v.getId()));
        graph.stopTransaction(Conclusion.SUCCESS);
        
        this.stopWatch();
        for (int i = 0; i < 100; i++) {
            graph.startTransaction();
            notadded.add(graph.addVertex(null));
            graph.stopTransaction(Conclusion.FAILURE);
        }
        printPerformance(graph.toString(), 100, "vertices not added in 100 failed transactions", this.stopWatch());

        if (graph.getFeatures().supportsVertexIteration)
            assertEquals(count(graph.getVertices()), 100);
        for (Vertex v : added) assertNotNull(graph.getVertex(v.getId()));
        for (Vertex v : notadded) assertNull(graph.getVertex(v.getId()));
        graph.stopTransaction(Conclusion.SUCCESS);

        graph.startTransaction();
        if (graph.getFeatures().supportsVertexIteration)
            assertEquals(count(graph.getVertices()), 100);
        for (Vertex v : added) assertNotNull(graph.getVertex(v.getId()));
        for (Vertex v : notadded) assertNull(graph.getVertex(v.getId()));

        graph.stopTransaction(Conclusion.FAILURE);
        if (graph.getFeatures().supportsVertexIteration)
            assertEquals(count(graph.getVertices()), 100);
        for (Vertex v : added) assertNotNull(graph.getVertex(v.getId()));
        for (Vertex v : notadded) assertNull(graph.getVertex(v.getId()));

        graph.stopTransaction(Conclusion.SUCCESS);

        this.stopWatch();
        graph.startTransaction();
        for (int i = 0; i < 100; i++) {
            added.add(graph.addVertex(null));
        }
        if (graph.getFeatures().supportsVertexIteration)
            assertEquals(count(graph.getVertices()), 200);
        for (Vertex v : added) assertNotNull(graph.getVertex(v.getId()));
        for (Vertex v : notadded) assertNull(graph.getVertex(v.getId()));

        graph.stopTransaction(Conclusion.SUCCESS);
        printPerformance(graph.toString(), 100, "vertices added in 1 successful transactions", this.stopWatch());

        graph.startTransaction();
        if (graph.getFeatures().supportsVertexIteration)
            assertEquals(count(graph.getVertices()), 200);
        for (Vertex v : added) assertNotNull(graph.getVertex(v.getId()));
        for (Vertex v : notadded) assertNull(graph.getVertex(v.getId()));

        this.stopWatch();
        for (int i = 0; i < 100; i++) {
            notadded.add(graph.addVertex(null));
        }
        if (graph.getFeatures().supportsVertexIteration)
            assertEquals(count(graph.getVertices()), 300);

        graph.stopTransaction(Conclusion.FAILURE);
        printPerformance(graph.toString(), 100, "vertices not added in 1 failed transactions", this.stopWatch());

        if (graph.getFeatures().supportsVertexIteration)
            assertEquals(count(graph.getVertices()), 200);
        for (Vertex v : added) assertNotNull(graph.getVertex(v.getId()));
        for (Vertex v : notadded) assertNull(graph.getVertex(v.getId()));

        graph.shutdown();
    }

    public void testTransactionsForEdges() {
        TransactionalGraph graph = (TransactionalGraph) graphTest.generateGraph();

        Vertex v = graph.addVertex(null);
        Vertex u = graph.addVertex(null);
        graph.stopTransaction(Conclusion.SUCCESS);

        this.stopWatch();
        graph.startTransaction();
        Edge e = graph.addEdge(null, v, u, convertId(graph, "test"));


        assertEquals(graph.getVertex(v.getId()), v);
        assertEquals(graph.getVertex(u.getId()), u);
        if (!graph.getFeatures().isRDFModel)
            assertEquals(graph.getEdge(e.getId()), e);

        if (graph.getFeatures().supportsVertexIteration)
            assertEquals(count(graph.getVertices()), 2);
        if (graph.getFeatures().supportsEdgeIteration)
            assertEquals(count(graph.getEdges()), 1);

        graph.stopTransaction(Conclusion.FAILURE);
        printPerformance(graph.toString(), 1, "edge not added in failed transaction (w/ iteration)", this.stopWatch());

        assertEquals(graph.getVertex(v.getId()), v);
        assertEquals(graph.getVertex(u.getId()), u);
        if (!graph.getFeatures().isRDFModel)
            assertNull(graph.getEdge(e.getId()));

        if (graph.getFeatures().supportsVertexIteration)
            assertEquals(count(graph.getVertices()), 2);
        if (graph.getFeatures().supportsEdgeIteration)
            assertEquals(count(graph.getEdges()), 0);

        this.stopWatch();
        graph.startTransaction();

        e = graph.addEdge(null, u, v, convertId(graph, "test"));

        assertEquals(graph.getVertex(v.getId()), v);
        assertEquals(graph.getVertex(u.getId()), u);
        if (!graph.getFeatures().isRDFModel)
            assertEquals(graph.getEdge(e.getId()), e);

        if (graph.getFeatures().supportsVertexIteration)
            assertEquals(count(graph.getVertices()), 2);
        if (graph.getFeatures().supportsEdgeIteration)
            assertEquals(count(graph.getEdges()), 1);
        graph.stopTransaction(Conclusion.SUCCESS);
        printPerformance(graph.toString(), 1, "edge added in successful transaction (w/ iteration)", this.stopWatch());

        if (graph.getFeatures().supportsVertexIteration)
            assertEquals(count(graph.getVertices()), 2);
        if (graph.getFeatures().supportsEdgeIteration)
            assertEquals(count(graph.getEdges()), 1);

        assertEquals(graph.getVertex(v.getId()), v);
        assertEquals(graph.getVertex(u.getId()), u);
        if (!graph.getFeatures().isRDFModel)
            assertEquals(graph.getEdge(e.getId()), e);

        graph.shutdown();
    }

    public void testBruteEdgeTransactions() {
        TransactionalGraph graph = (TransactionalGraph) graphTest.generateGraph();
        this.stopWatch();
        for (int i = 0; i < 100; i++) {
            graph.startTransaction();
            Vertex v = graph.addVertex(null);
            Vertex u = graph.addVertex(null);
            graph.addEdge(null, v, u, convertId(graph, "test"));
            graph.stopTransaction(Conclusion.SUCCESS);
        }
        printPerformance(graph.toString(), 100, "edges added in 100 successful transactions (2 vertices added for each edge)", this.stopWatch());
        if (graph.getFeatures().supportsVertexIteration)
            assertEquals(count(graph.getVertices()), 200);
        if (graph.getFeatures().supportsEdgeIteration)
            assertEquals(count(graph.getEdges()), 100);

        this.stopWatch();
        for (int i = 0; i < 100; i++) {
            graph.startTransaction();
            Vertex v = graph.addVertex(null);
            Vertex u = graph.addVertex(null);
            graph.addEdge(null, v, u, convertId(graph, "test"));
            graph.stopTransaction(Conclusion.FAILURE);
        }
        printPerformance(graph.toString(), 100, "edges not added in 100 failed transactions (2 vertices added for each edge)", this.stopWatch());
        if (graph.getFeatures().supportsVertexIteration)
            assertEquals(count(graph.getVertices()), 200);
        if (graph.getFeatures().supportsEdgeIteration)
            assertEquals(count(graph.getEdges()), 100);

        this.stopWatch();
        graph.startTransaction();
        for (int i = 0; i < 100; i++) {
            Vertex v = graph.addVertex(null);
            Vertex u = graph.addVertex(null);
            graph.addEdge(null, v, u, convertId(graph, "test"));
        }
        if (graph.getFeatures().supportsVertexIteration)
            assertEquals(count(graph.getVertices()), 400);
        if (graph.getFeatures().supportsEdgeIteration)
            assertEquals(count(graph.getEdges()), 200);
        graph.stopTransaction(Conclusion.SUCCESS);
        printPerformance(graph.toString(), 100, "edges added in 1 successful transactions (2 vertices added for each edge)", this.stopWatch());
        if (graph.getFeatures().supportsVertexIteration)
            assertEquals(count(graph.getVertices()), 400);
        if (graph.getFeatures().supportsEdgeIteration)
            assertEquals(count(graph.getEdges()), 200);

        this.stopWatch();
        graph.startTransaction();
        for (int i = 0; i < 100; i++) {
            Vertex v = graph.addVertex(null);
            Vertex u = graph.addVertex(null);
            graph.addEdge(null, v, u, convertId(graph, "test"));
        }
        if (graph.getFeatures().supportsVertexIteration)
            assertEquals(count(graph.getVertices()), 600);
        if (graph.getFeatures().supportsEdgeIteration)
            assertEquals(count(graph.getEdges()), 300);
        graph.stopTransaction(Conclusion.FAILURE);
        printPerformance(graph.toString(), 100, "edges not added in 1 failed transactions (2 vertices added for each edge)", this.stopWatch());
        if (graph.getFeatures().supportsVertexIteration)
            assertEquals(count(graph.getVertices()), 400);
        if (graph.getFeatures().supportsEdgeIteration)
            assertEquals(count(graph.getEdges()), 200);

        graph.shutdown();
    }

    public void testPropertyTransactions() {
        TransactionalGraph graph = (TransactionalGraph) graphTest.generateGraph();
        if (!graph.getFeatures().isRDFModel) {
            this.stopWatch();
            graph.startTransaction();
            Vertex v = graph.addVertex(null);
            Object id = v.getId();
            v.setProperty("name", "marko");
            graph.stopTransaction(Conclusion.SUCCESS);
            printPerformance(graph.toString(), 1, "vertex added with string property in a successful transaction", this.stopWatch());


            this.stopWatch();
            graph.startTransaction();
            v = graph.getVertex(id);
            assertNotNull(v);
            assertEquals(v.getProperty("name"), "marko");
            v.setProperty("age", 30);
            assertEquals(v.getProperty("age"), 30);
            graph.stopTransaction(Conclusion.FAILURE);
            printPerformance(graph.toString(), 1, "integer property not added in a failed transaction", this.stopWatch());

            this.stopWatch();
            graph.startTransaction();
            v = graph.getVertex(id);
            assertNotNull(v);
            assertEquals(v.getProperty("name"), "marko");
            assertNull(v.getProperty("age"));
            printPerformance(graph.toString(), 2, "vertex properties checked in a successful transaction", this.stopWatch());


            Edge edge = graph.addEdge(null, v, graph.addVertex(null), "test");
            if (graph.getFeatures().supportsEdgeIteration)
                assertEquals(count(graph.getEdges()), 1);
            graph.stopTransaction(Conclusion.SUCCESS);
            if (graph.getFeatures().supportsEdgeIteration)
                assertEquals(count(graph.getEdges()), 1);

            this.stopWatch();
            graph.startTransaction();
            edge = graph.getVertex(id).getEdges(Direction.OUT).iterator().next();
            edge.setProperty("transaction-1", "success");
            assertEquals(edge.getProperty("transaction-1"), "success");
            graph.stopTransaction(Conclusion.SUCCESS);
            printPerformance(graph.toString(), 1, "edge property added and checked in a successful transaction", this.stopWatch());


            this.stopWatch();
            graph.startTransaction();
            edge = graph.getVertex(id).getEdges(Direction.OUT).iterator().next();
            assertEquals(edge.getProperty("transaction-1"), "success");
            edge.setProperty("transaction-2", "failure");
            assertEquals(edge.getProperty("transaction-1"), "success");
            assertEquals(edge.getProperty("transaction-2"), "failure");
            graph.stopTransaction(Conclusion.FAILURE);
            printPerformance(graph.toString(), 1, "edge property added and checked in a failed transaction", this.stopWatch());
            edge = graph.getVertex(id).getEdges(Direction.OUT).iterator().next();
            assertEquals(edge.getProperty("transaction-1"), "success");
            assertNull(edge.getProperty("transaction-2"));
        }
        graph.shutdown();
    }

    public void testIndexTransactions() {
        TransactionalGraph graph = (TransactionalGraph) graphTest.generateGraph();
        if (graph.getFeatures().supportsVertexIndex) {
            this.stopWatch();
            graph.startTransaction();
            Index<Vertex> index = ((IndexableGraph) graph).createIndex("txIdx", Vertex.class);
            Vertex v = graph.addVertex(null);
            Object id = v.getId();
            v.setProperty("name", "marko");
            index.put("name", "marko", v);
            if (graph.getFeatures().supportsVertexIteration)
                assertEquals(count(graph.getVertices()), 1);
            v = ((IndexableGraph) graph).getIndex("txIdx", Vertex.class).get("name", "marko").iterator().next();
            assertEquals(v.getId(), id);
            assertEquals(v.getProperty("name"), "marko");
            graph.stopTransaction(Conclusion.SUCCESS);
            printPerformance(graph.toString(), 1, "vertex added and retrieved from index in a successful transaction", this.stopWatch());


            this.stopWatch();
            if (graph.getFeatures().supportsVertexIteration)
                assertEquals(count(graph.getVertices()), 1);
            v = ((IndexableGraph) graph).getIndex("txIdx", Vertex.class).get("name", "marko").iterator().next();
            assertEquals(v.getId(), id);
            assertEquals(v.getProperty("name"), "marko");
            printPerformance(graph.toString(), 1, "vertex retrieved from index outside successful transaction", this.stopWatch());


            this.stopWatch();
            graph.startTransaction();
            v = graph.addVertex(null);
            v.setProperty("name", "pavel");
            index.put("name", "pavel", v);
            if (graph.getFeatures().supportsVertexIteration)
                assertEquals(count(graph.getVertices()), 2);
            v = ((IndexableGraph) graph).getIndex("txIdx", Vertex.class).get("name", "marko").iterator().next();
            assertEquals(v.getProperty("name"), "marko");
            v = ((IndexableGraph) graph).getIndex("txIdx", Vertex.class).get("name", "pavel").iterator().next();
            assertEquals(v.getProperty("name"), "pavel");
            graph.stopTransaction(Conclusion.FAILURE);
            printPerformance(graph.toString(), 1, "vertex not added in a failed transaction", this.stopWatch());

            this.stopWatch();
            if (graph.getFeatures().supportsVertexIteration)
                assertEquals(count(graph.getVertices()), 1);
            assertEquals(count(((IndexableGraph) graph).getIndex("txIdx", Vertex.class).get("name", "pavel")), 0);
            printPerformance(graph.toString(), 1, "vertex not retrieved in a successful transaction", this.stopWatch());
            v = ((IndexableGraph) graph).getIndex("txIdx", Vertex.class).get("name", "marko").iterator().next();
            assertEquals(v.getProperty("name"), "marko");
        }
        graph.shutdown();
    }

    // public void testAutomaticIndexKeysRollback()

    public void testAutomaticSuccessfulTransactionOnShutdown() {

        TransactionalGraph graph = (TransactionalGraph) graphTest.generateGraph();
        if (graph.getFeatures().isPersistent && !graph.getFeatures().isRDFModel) {
            Vertex v = graph.addVertex(null);
            Object id = v.getId();
            v.setProperty("count", "1");
            v.setProperty("count", "2");
            graph.shutdown();
            graph = (TransactionalGraph) graphTest.generateGraph();
            Vertex reloadedV = graph.getVertex(id);
            assertEquals("2", reloadedV.getProperty("count"));

        }
        graph.shutdown();
    }


    public void testBulkTransactionsOnEdges() {
        TransactionalGraph graph = (TransactionalGraph) graphTest.generateGraph();
        Vertex v = graph.addVertex(null);
        graph.stopTransaction(Conclusion.SUCCESS);
        
        v = graph.getVertex(v.getId());
        for (int i = 0; i < 5; i++) {
            graph.addEdge(null, v, graph.addVertex(null), convertId(graph, "test"));
        }
        if (graph.getFeatures().supportsEdgeIteration)
            assertEquals(count(graph.getEdges()), 5);
        assertEquals(5,count(v.getEdges(Direction.BOTH)));
        graph.stopTransaction(Conclusion.FAILURE);
        
        if (graph.getFeatures().supportsEdgeIteration)
            assertEquals(count(graph.getEdges()), 0);
        v = graph.getVertex(v.getId());
        assertEquals(0,count(v.getEdges(Direction.BOTH)));
        for (int i = 0; i < 4; i++) {
            graph.addEdge(null, v, graph.addVertex(null), convertId(graph, "test"));
        }
        if (graph.getFeatures().supportsEdgeIteration)
            assertEquals(count(graph.getEdges()), 4);
        assertEquals(4,count(v.getEdges(Direction.BOTH)));
        graph.stopTransaction(Conclusion.FAILURE);

        if (graph.getFeatures().supportsEdgeIteration)
            assertEquals(count(graph.getEdges()), 0);
        v = graph.getVertex(v.getId());
        assertEquals(0,count(v.getEdges(Direction.BOTH)));

        for (int i = 0; i < 3; i++) {
            graph.addEdge(null, v, graph.addVertex(null), convertId(graph, "test"));
        }
        if (graph.getFeatures().supportsEdgeIteration)
            assertEquals(count(graph.getEdges()), 3);
        assertEquals(3,count(v.getEdges(Direction.BOTH)));
        graph.stopTransaction(Conclusion.SUCCESS);

        if (graph.getFeatures().supportsEdgeIteration)
            assertEquals(count(graph.getEdges()), 3);
        v = graph.getVertex(v.getId());
        assertEquals(3,count(v.getEdges(Direction.BOTH)));

        graph.shutdown();
    }


    public void testCompetingThreads() {
        final TransactionalGraph graph = (TransactionalGraph) graphTest.generateGraph();
        //Warm up
        Vertex a = graph.addVertex(null);
        Vertex b = graph.addVertex(null);
        Edge e = graph.addEdge(null, a, b, convertId(graph, "friend"));

        if (!graph.getFeatures().isRDFModel) {
            a.setProperty("test", 100);
            b.setProperty("blah", 0.5f);
            e.setProperty("bloop", 5000);
        }
        graph.stopTransaction(Conclusion.SUCCESS);

        int totalThreads = 250;
        final AtomicInteger vertices = new AtomicInteger(0);
        final AtomicInteger edges = new AtomicInteger(0);
        final AtomicInteger completedThreads = new AtomicInteger(0);
        for (int i = 0; i < totalThreads; i++) {
            new Thread() {
                public void run() {
                    Random random = new Random();
                    if (random.nextBoolean()) {
                        Vertex a = graph.addVertex(null);
                        Vertex b = graph.addVertex(null);
                        Edge e = graph.addEdge(null, a, b, convertId(graph, "friend"));

                        if (!graph.getFeatures().isRDFModel) {
                            a.setProperty("test", this.getId());
                            b.setProperty("blah", random.nextFloat());
                            e.setProperty("bloop", random.nextInt());
                        }
                        vertices.getAndAdd(2);
                        edges.getAndAdd(1);
                        graph.stopTransaction(Conclusion.SUCCESS);
                    } else {
                        graph.startTransaction();
                        Vertex a = graph.addVertex(null);
                        Vertex b = graph.addVertex(null);
                        Edge e = graph.addEdge(null, a, b, convertId(graph, "friend"));
                        if (!graph.getFeatures().isRDFModel) {
                            a.setProperty("test", this.getId());
                            b.setProperty("blah", random.nextFloat());
                            e.setProperty("bloop", random.nextInt());
                        }
                        if (random.nextBoolean()) {
                            graph.stopTransaction(Conclusion.SUCCESS);
                            vertices.getAndAdd(2);
                            edges.getAndAdd(1);
                        } else {
                            graph.stopTransaction(Conclusion.FAILURE);
                        }
                    }
                    completedThreads.getAndAdd(1);
                }
            }.start();
        }

        while (completedThreads.get() < totalThreads) {
        }
        assertEquals(completedThreads.get(), 250);
        if (graph.getFeatures().supportsVertexIteration)
            assertEquals(count(graph.getVertices()), vertices.get());
        if (graph.getFeatures().supportsEdgeIteration)
            assertEquals(count(graph.getEdges()), edges.get());
        graph.shutdown();
    }

    public void testRemoveInTransaction() {
        TransactionalGraph graph = (TransactionalGraph) graphTest.generateGraph();

        graph.startTransaction();
        assertEquals(0, count(graph.getEdges()));

        Vertex v1 = graph.addVertex(null);
        Vertex v2 = graph.addVertex(null);
        Edge e1 = graph.addEdge(null, v1, v2, convertId(graph, "test-edge"));
        graph.stopTransaction(Conclusion.SUCCESS);

        graph.startTransaction();
        assertEquals(1, count(graph.getVertex(v1.getId()).getEdges(Direction.BOTH)));
        e1 = graph.getVertex(v1.getId()).getEdges(Direction.BOTH).iterator().next();
        graph.removeEdge(e1);
        assertEquals(0, count(graph.getVertex(v1.getId()).getEdges(Direction.BOTH)));
        graph.stopTransaction(Conclusion.FAILURE);

        graph.startTransaction();
        assertEquals(1, count(graph.getVertex(v1.getId()).getEdges(Direction.BOTH)));
        e1 = graph.getVertex(v1.getId()).getEdges(Direction.BOTH).iterator().next();
        graph.removeEdge(e1);
        graph.stopTransaction(Conclusion.SUCCESS);

        assertEquals(0, count(graph.getVertex(v1.getId()).getEdges(Direction.BOTH)));
        graph.shutdown();
    }

}
