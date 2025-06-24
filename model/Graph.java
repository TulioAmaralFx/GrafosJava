package model;

import java.util.*;

public class Graph {
    private Map<Integer, Node> nodes;
    private Map<Integer, Map<Integer, Edge>> adj;
    private int numVertices;
    private int numEdges;

    public Graph() {
        this.nodes = new HashMap<>();
        this.adj = new HashMap<>();
        this.numVertices = 0;
        this.numEdges = 0;
    }

    public void addNode(Node node) {
        if (!nodes.containsKey(node.getIdInterno())) {
            nodes.put(node.getIdInterno(), node);
            adj.put(node.getIdInterno(), new HashMap<>());
            numVertices++;
        }
    }

    public void addEdge(int uId, int vId, double weight, boolean isDirected) {
        if (!nodes.containsKey(uId) || !nodes.containsKey(vId)) {
            return;
        }
        
        adj.putIfAbsent(uId, new HashMap<>());
        adj.putIfAbsent(vId, new HashMap<>());

        // Evita adicionar arestas duplicadas conceitualmente
        if (adj.get(uId).containsKey(vId)) {
            return; // Já existe, não faz nada
        }
        
        Edge edge = new Edge(uId, vId, weight, isDirected);
        adj.get(uId).put(vId, edge);
        
        if (!isDirected) {
            Edge reverseEdge = new Edge(vId, uId, weight, false);
            adj.get(vId).put(uId, reverseEdge);
        }
        numEdges++; // Incrementa apenas uma vez por aresta conceitual
    }

    // MÉTODO ATUALIZADO para corrigir a contagem de arestas na remoção de nós
    public void removeNode(int nodeId) {
        if (!nodes.containsKey(nodeId)) {
            return;
        }

        // Primeiro, remove todas as arestas de outros nós que chegam em 'nodeId'
        List<Edge> edgesToRemove = new ArrayList<>();
        for (Map<Integer, Edge> neighborMap : adj.values()) {
            Edge edge = neighborMap.get(nodeId);
            if (edge != null) {
                edgesToRemove.add(edge);
            }
        }
        
        for (Edge edge : edgesToRemove) {
            // Usa o método removeEdge que já tem a lógica de contagem correta
            removeEdge(edge.getU(), edge.getV());
        }

        // Agora, remove as arestas que saem de 'nodeId'
        // Como as arestas não direcionadas já foram removidas no passo anterior,
        // este loop só vai remover arestas direcionadas que ainda restam.
        if (adj.containsKey(nodeId)) {
            List<Edge> outgoingEdges = new ArrayList<>(adj.get(nodeId).values());
            for (Edge edge : outgoingEdges) {
                removeEdge(edge.getU(), edge.getV());
            }
        }
        
        // Finalmente, remove o nó e sua entrada na lista de adjacência
        adj.remove(nodeId);
        nodes.remove(nodeId);
        numVertices--;
    }
    
    public boolean removeEdge(int uId, int vId) {
        if (!adj.containsKey(uId) || !adj.get(uId).containsKey(vId)) {
            return false;
        }

        boolean isDirected = adj.get(uId).get(vId).isDirected();
        adj.get(uId).remove(vId);

        if (!isDirected) {
            if (adj.containsKey(vId) && adj.get(vId).containsKey(uId)) {
                adj.get(vId).remove(uId);
            }
        }
        
        numEdges--;
        return true;
    }

    public PathResult dijkstra(int startNodeId, int endNodeId) {
        if (!nodes.containsKey(startNodeId) || !nodes.containsKey(endNodeId)) {
            return new PathResult(new ArrayList<>(), Double.POSITIVE_INFINITY, 0, 0);
        }

        long startTime = System.nanoTime();
        Map<Integer, Double> distances = new HashMap<>();
        Map<Integer, Integer> predecessors = new HashMap<>();
        PriorityQueue<DijkstraNode> pq = new PriorityQueue<>(Comparator.comparingDouble(DijkstraNode::getDistance));
        Set<Integer> visited = new HashSet<>();
        int nodesExploredCount = 0;

        for (int nodeId : nodes.keySet()) {
            distances.put(nodeId, Double.POSITIVE_INFINITY);
        }
        distances.put(startNodeId, 0.0);
        pq.add(new DijkstraNode(startNodeId, 0.0));

        while (!pq.isEmpty()) {
            DijkstraNode currentDNode = pq.poll();
            int currentNodeId = currentDNode.getNodeId();

            if (visited.contains(currentNodeId)) {
                continue;
            }

            if (currentNodeId == endNodeId) {
                break;
            }

            visited.add(currentNodeId);
            nodesExploredCount++;

            if (adj.get(currentNodeId) == null) continue;

            for (Edge edge : adj.get(currentNodeId).values()) {
                int neighborId = edge.getV();
                if (visited.contains(neighborId)) continue;
                
                double newDist = distances.get(currentNodeId) + edge.getWeight();

                if (newDist < distances.get(neighborId)) {
                    distances.put(neighborId, newDist);
                    predecessors.put(neighborId, currentNodeId);
                    pq.add(new DijkstraNode(neighborId, newDist));
                }
            }
        }

        long endTime = System.nanoTime();
        double processingTimeMs = (endTime - startTime) / 1_000_000.0;

        List<Integer> path = new ArrayList<>();
        double totalCost = distances.getOrDefault(endNodeId, Double.POSITIVE_INFINITY);

        if (totalCost != Double.POSITIVE_INFINITY) {
            Integer current = endNodeId;
            while (current != null) {
                path.add(current);
                current = predecessors.get(current);
            }
            Collections.reverse(path);
        }
        
        return new PathResult(path, totalCost, processingTimeMs, nodesExploredCount);
    }
    
    // Getters
    public Map<Integer, Node> getNodes() { return nodes; }
    public int getNumVertices() { return numVertices; }
    public int getNumEdges() { return numEdges; }
    public Map<Integer, Map<Integer, Edge>> getAdj() { return adj; }

    // Classes aninhadas
    private static class DijkstraNode {
        int nodeId;
        double distance;
        public DijkstraNode(int nodeId, double distance) {
            this.nodeId = nodeId;
            this.distance = distance;
        }
        public double getDistance() { return distance; }
        public int getNodeId() { return nodeId; }
    }

    public static class PathResult {
        public final List<Integer> path;
        public final double totalCost;
        public final double processingTimeMs;
        public final int nodesExplored;

        public PathResult(List<Integer> path, double totalCost, double processingTimeMs, int nodesExplored) {
            this.path = path;
            this.totalCost = totalCost;
            this.processingTimeMs = processingTimeMs;
            this.nodesExplored = nodesExplored;
        }
    }
}