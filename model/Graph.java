package model;

import java.util.*;
import java.util.concurrent.TimeUnit; // Para tempo de processamento

public class Graph {
    private Map<Integer, Node> nodes; // {idInterno: Node_obj}
    private Map<Integer, Map<Integer, Edge>> adj; // {u_id: {v_id: Edge_obj}}
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
            System.out.println("DEBUG_GRAPH: Nó " + node.getIdInterno() + " adicionado. Total de nós: " + numVertices);
        } else {
            System.out.println("DEBUG_GRAPH: Aviso: Tentativa de adicionar nó " + node.getIdInterno() + " que já existe.");
        }
    }

    public void addEdge(int uId, int vId, double weight, boolean isDirected) {
        System.out.printf("DEBUG_GRAPH: add_edge chamado para %d -> %d. Tipo direcionado: %b. Peso: %.2f%n", uId, vId, isDirected, weight);

        if (!nodes.containsKey(uId)) {
            System.out.println("DEBUG_GRAPH: ERRO: Nó de origem " + uId + " NÃO existe no self.nodes. Aresta não adicionada.");
            return;
        }
        if (!nodes.containsKey(vId)) {
            System.out.println("DEBUG_GRAPH: ERRO: Nó de destino " + vId + " NÃO existe no self.nodes. Aresta não adicionada.");
            return;
        }

        // Garante que as entradas de adjacência existem
        adj.putIfAbsent(uId, new HashMap<>());
        adj.putIfAbsent(vId, new HashMap<>());

        System.out.println("DEBUG_GRAPH: Nós " + uId + " e " + vId + " existem e têm adjacências. Prosseguindo para adicionar aresta.");

        Edge edge = new Edge(uId, vId, weight, isDirected);
        if (adj.get(uId).containsKey(vId)) {
            System.out.println("DEBUG_GRAPH: Aresta " + uId + " -> " + vId + " já existe. Sobrescrevendo.");
        }
        adj.get(uId).put(vId, edge);
        numEdges++;
        System.out.println("DEBUG_GRAPH: Aresta " + uId + " -> " + vId + " (direcionada) adicionada. num_edges atualizado para = " + numEdges);

        if (!isDirected) {
            Edge reverseEdge = new Edge(vId, uId, weight, false); // Nao direcionada, peso igual
            if (adj.get(vId).containsKey(uId)) {
                System.out.println("DEBUG_GRAPH: Aresta reversa " + vId + " -> " + uId + " já existe. Sobrescrevendo.");
            }
            adj.get(vId).put(uId, reverseEdge);
            System.out.println("DEBUG_GRAPH: Aresta reversa " + vId + " -> " + uId + " adicionada (não direcionada).");
        }
        System.out.printf("DEBUG_GRAPH: Fim de add_edge para %d -> %d. num_edges final na função: %d%n", uId, vId, numEdges);
    }

    // --- Implementação do Algoritmo de Dijkstra ---
    public PathResult dijkstra(int startNodeId, int endNodeId) {
        System.out.println("DEBUG_GRAPH: Iniciando Dijkstra de " + startNodeId + " para " + endNodeId);
        if (!nodes.containsKey(startNodeId) || !nodes.containsKey(endNodeId)) {
            System.out.println("DEBUG_GRAPH: Nós de origem ou destino não encontrados para Dijkstra.");
            return new PathResult(new ArrayList<>(), Double.POSITIVE_INFINITY, 0, 0);
        }

        long startTime = System.nanoTime(); // Tempo de início em nanossegundos

        Map<Integer, Double> distances = new HashMap<>();
        Map<Integer, Integer> predecessors = new HashMap<>();
        PriorityQueue<DijkstraNode> pq = new PriorityQueue<>(Comparator.comparingDouble(DijkstraNode::getDistance));
        Set<Integer> visited = new HashSet<>();
        int nodesExploredCount = 0;

        // Inicialização
        for (int nodeId : nodes.keySet()) {
            distances.put(nodeId, Double.POSITIVE_INFINITY);
            predecessors.put(nodeId, null);
        }
        distances.put(startNodeId, 0.0);
        pq.add(new DijkstraNode(startNodeId, 0.0));

        while (!pq.isEmpty()) {
            DijkstraNode currentDNode = pq.poll();
            int currentNodeId = currentDNode.getNodeId();
            double currentDistance = currentDNode.getDistance();

            System.out.printf("DEBUG_GRAPH: Dijkstra: Processando nó %d com distância %.2f%n", currentNodeId, currentDistance);

            if (visited.contains(currentNodeId)) {
                System.out.println("DEBUG_GRAPH: Dijkstra: Nó " + currentNodeId + " já visitado. Pulando.");
                continue;
            }

            visited.add(currentNodeId);
            nodesExploredCount++;

            if (currentNodeId == endNodeId) {
                System.out.println("DEBUG_GRAPH: Dijkstra: Destino " + endNodeId + " alcançado.");
                break;
            }

            // Explora vizinhos
            Map<Integer, Edge> neighbors = adj.getOrDefault(currentNodeId, new HashMap<>());
            for (Map.Entry<Integer, Edge> entry : neighbors.entrySet()) {
                int neighborId = entry.getKey();
                Edge edge = entry.getValue();

                System.out.printf("DEBUG_GRAPH: Dijkstra: Explorando vizinho %d de %d (peso %.2f)%n", neighborId, currentNodeId, edge.getWeight());

                if (visited.contains(neighborId)) {
                    System.out.println("DEBUG_GRAPH: Dijkstra: Vizinho " + neighborId + " já visitado. Pulando.");
                    continue;
                }

                double newDistance = currentDistance + edge.getWeight();
                if (newDistance < distances.get(neighborId)) {
                    System.out.printf("DEBUG_GRAPH: Dijkstra: Distância para %d atualizada de %.2f para %.2f%n", neighborId, distances.get(neighborId), newDistance);
                    distances.put(neighborId, newDistance);
                    predecessors.put(neighborId, currentNodeId);
                    pq.add(new DijkstraNode(neighborId, newDistance));
                } else {
                    System.out.printf("DEBUG_GRAPH: Dijkstra: Distância para %d (%.2f) não é menor que a existente (%.2f).%n", neighborId, newDistance, distances.get(neighborId));
                }
            }
        }

        long endTime = System.nanoTime();
        double processingTimeMs = (double) TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

        // Reconstruir o caminho
        List<Integer> path = new ArrayList<>();
        double totalCost = distances.getOrDefault(endNodeId, Double.POSITIVE_INFINITY);

        if (totalCost == Double.POSITIVE_INFINITY) {
            System.out.println("DEBUG_GRAPH: Dijkstra: Não foi possível encontrar um caminho para " + endNodeId + ".");
            return new PathResult(new ArrayList<>(), Double.POSITIVE_INFINITY, processingTimeMs, nodesExploredCount);
        }

        Integer current = endNodeId;
        while (current != null) {
            path.add(current);
            current = predecessors.get(current);
        }
        Collections.reverse(path);

        System.out.printf("DEBUG_GRAPH: Dijkstra concluído. Caminho encontrado: %s, Custo: %.2f, Tempo: %.2fms, Nós explorados: %d%n", path.toString(), totalCost, processingTimeMs, nodesExploredCount);
        return new PathResult(path, totalCost, processingTimeMs, nodesExploredCount);
    }

    // Classe auxiliar para o PriorityQueue do Dijkstra
    private static class DijkstraNode {
        int nodeId;
        double distance;

        public DijkstraNode(int nodeId, double distance) {
            this.nodeId = nodeId;
            this.distance = distance;
        }

        public int getNodeId() { return nodeId; }
        public double getDistance() { return distance; }
    }

    // Classe para retornar os resultados do Dijkstra
    public static class PathResult {
        public List<Integer> path;
        public double totalCost;
        public double processingTimeMs;
        public int nodesExplored;

        public PathResult(List<Integer> path, double totalCost, double processingTimeMs, int nodesExplored) {
            this.path = path;
            this.totalCost = totalCost;
            this.processingTimeMs = processingTimeMs;
            this.nodesExplored = nodesExplored;
        }
    }

    // Getters para informações do grafo
    public Map<Integer, Node> getNodes() { return nodes; }
    public int getNumVertices() { return numVertices; }
    public int getNumEdges() { return numEdges; }
    public Map<Integer, Map<Integer, Edge>> getAdj() { return adj; }
}