package io;

import model.Graph;
import model.Node;
import model.Edge; 

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PolyReader {

    private static class RawEdgeData {
        int uId;
        int vId;
        boolean isDirected;

        public RawEdgeData(int uId, int vId, boolean isDirected) {
            this.uId = uId;
            this.vId = vId;
            this.isDirected = isDirected;
        }
    }

    public Graph readPolyFile(String filepath) throws IOException, NumberFormatException, IllegalArgumentException {
        Graph graph = new Graph();

        // --- Variáveis declaradas no início do método para ter escopo completo ---
        Map<Integer, double[]> nodesRawCoords = new HashMap<>(); // {node_id: [lat, lon]}
        List<RawEdgeData> edgesToProcess = new ArrayList<>(); // Lista de arestas brutas para processar depois
        List<Node> tempNodesListByInternalId = new ArrayList<>(); // Lista temporária para nós com ID interno sequencial
        // Fim das variáveis movidas para o escopo do método
        
        System.out.println("DEBUG: Lendo arquivo .poly: " + filepath);

        try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
            String line;
            int lineIdx = 0;

            // --- Seção de Vértices ---
            line = reader.readLine(); lineIdx++;
            if (line == null) throw new IOException("Arquivo .poly vazio.");
            String[] headerNodesParts = line.trim().split("\\s+");
            if (headerNodesParts.length < 4) {
                throw new IllegalArgumentException("Cabeçalho de vértice mal formatado na linha " + lineIdx + ": " + line);
            }
            int numVerticesExpected = Integer.parseInt(headerNodesParts[0]);
            System.out.println("DEBUG: .poly: Cabeçalho de Vértices: Esperado " + numVerticesExpected + " vértices.");

            // Adiciona nós a nodesRawCoords E a tempNodesListByInternalId
            for (int i = 0; i < numVerticesExpected; i++) {
                line = reader.readLine(); lineIdx++;
                if (line == null) throw new IOException("Número inesperado de linhas de vértice. Esperado " + numVerticesExpected + ", lido " + i);
                String[] parts = line.trim().split("\\s+");
                if (parts.length < 3) {
                    System.out.println("DEBUG: .poly: Aviso: Linha de vértice mal formatada ignorada na linha " + lineIdx + ": " + line);
                    continue;
                }
                int nodeId = Integer.parseInt(parts[0]);
                double lat = Double.parseDouble(parts[1]);
                double lon = Double.parseDouble(parts[2]);
                
                nodesRawCoords.put(nodeId, new double[]{lat, lon});
                // É CRÍTICO que tempNodesListByInternalId seja preenchida aqui
                // para que a lógica de fallback e transformação funcione
                tempNodesListByInternalId.add(new Node(nodeId, lon, lat)); // idInterno, x=lon, y=lat (brutos)
            }
            System.out.println("DEBUG: .poly: " + nodesRawCoords.size() + " vértices lidos para dados brutos.");
            System.out.println("DEBUG: .poly: " + tempNodesListByInternalId.size() + " nós adicionados a tempNodesListByInternalId.");


            // --- Seção de Arestas ---
            line = reader.readLine(); lineIdx++;
            if (line == null) throw new IOException("Seção de arestas ausente.");
            String[] headerEdgesParts = line.trim().split("\\s+");
            if (headerEdgesParts.length < 2) {
                throw new IllegalArgumentException("Cabeçalho de aresta mal formatado na linha " + lineIdx + ": " + line);
            }
            int numEdgesExpected = Integer.parseInt(headerEdgesParts[0]);
            System.out.println("DEBUG: .poly: Cabeçalho de Arestas: Esperado " + numEdgesExpected + " arestas.");

            for (int i = 0; i < numEdgesExpected; i++) {
                line = reader.readLine(); lineIdx++;
                if (line == null) throw new IOException("Número inesperado de linhas de aresta. Esperado " + numEdgesExpected + ", lido " + i);
                String[] parts = line.trim().split("\\s+");
                if (parts.length < 4) {
                    System.out.println("DEBUG: .poly: Aviso: Linha de aresta incompleta ou mal formatada ignorada na linha " + lineIdx + ": " + line);
                    continue;
                }
                int uId = Integer.parseInt(parts[1]);
                int vId = Integer.parseInt(parts[2]);
                boolean isDirected = (Integer.parseInt(parts[3]) != 0);
                edgesToProcess.add(new RawEdgeData(uId, vId, isDirected));
            }
            System.out.println("DEBUG: .poly: " + edgesToProcess.size() + " arestas lidas em dados brutos temporários.");

            // Linha final "0"
            line = reader.readLine(); lineIdx++;
            if (line == null || Integer.parseInt(line.trim()) != 0) {
                System.out.println("DEBUG: .poly: Aviso: Linha final '0' não encontrada ou formato inesperado na linha " + lineIdx + ".");
            } else {
                System.out.println("DEBUG: .poly: Linha final '0' encontrada.");
            }

        } // reader fecha automaticamente


        // --- CONSTRUÇÃO DE UM GRAFO TEMPORÁRIO PARA CÁLCULO DE GRAU E FILTRAGEM ---
        Graph tempGraphForConnectivity = new Graph();
        // Adiciona todos os nós lidos para o grafo temporário
        for (Node node : tempNodesListByInternalId) { 
            tempGraphForConnectivity.addNode(node); 
        }
        // Em seguida, adiciona todas as arestas brutas lidas
        for (RawEdgeData rawEdge : edgesToProcess) {
            double lat1 = nodesRawCoords.get(rawEdge.uId)[0];
            double lon1 = nodesRawCoords.get(rawEdge.uId)[1];
            double lat2 = nodesRawCoords.get(rawEdge.vId)[0];
            double lon2 = nodesRawCoords.get(rawEdge.vId)[1];
            double weight = Math.sqrt(Math.pow(lon1 - lon2, 2) + Math.pow(lat1 - lat2, 2));
            if (weight == 0) weight = 0.001;

            tempGraphForConnectivity.addEdge(rawEdge.uId, rawEdge.vId, weight, rawEdge.isDirected);
        }
        System.out.println("DEBUG: Grafo temporário para conectividade criado. Nós: " + tempGraphForConnectivity.getNumVertices() + ", Arestas: " + tempGraphForConnectivity.getNumEdges());

        // --- CÁLCULO DE GRAU E IDENTIFICAÇÃO DOS NÓS MAIS CONECTADOS ---
        Map<Integer, Integer> nodeDegrees = new HashMap<>();
        for (Node node : tempGraphForConnectivity.getNodes().values()) {
            nodeDegrees.put(node.getIdInterno(), tempGraphForConnectivity.getAdj().get(node.getIdInterno()).size());
        }

        List<Map.Entry<Integer, Integer>> sortedNodesByDegree = new ArrayList<>(nodeDegrees.entrySet());
        sortedNodesByDegree.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        int DEGREE_THRESHOLD = 2; // Nível mínimo de conexões
        Set<Integer> filteredNodeIds = new HashSet<>();
        for (Map.Entry<Integer, Integer> entry : nodeDegrees.entrySet()) {
            if (entry.getValue() >= DEGREE_THRESHOLD) {
                filteredNodeIds.add(entry.getKey());
            }
        }
        System.out.println("DEBUG: Nós com grau >= " + DEGREE_THRESHOLD + ": " + filteredNodeIds.size() + " nós.");

        // Contingência: Se nenhum nó atingir o limiar, pega um percentual dos mais conectados
        if (filteredNodeIds.isEmpty() && !tempNodesListByInternalId.isEmpty()) { // CORREÇÃO: tempNodesListByInternalId agora está acessível
            System.out.println("DEBUG: Nenhum nó atingiu o limiar de grau. Selecionando os 10% nós com maior grau para garantir um grafo mínimo.");
            int numTopNodes = Math.max(1, tempGraphForConnectivity.getNumVertices() / 10);
            for (int i = 0; i < Math.min(numTopNodes, sortedNodesByDegree.size()); i++) {
                filteredNodeIds.add(sortedNodesByDegree.get(i).getKey());
            }
            System.out.println("DEBUG: Selecionados " + filteredNodeIds.size() + " nós com maior grau (fallback).");
        }


        // --- Construção do GRAFO FINAL (Graph) com APENAS os Nós Filtrados e Arestas Válidas entre eles ---
        if (filteredNodeIds.isEmpty()) {
            System.out.println("DEBUG: Nenhum nó filtrado para o grafo final. Retornando grafo vazio.");
            return new Graph();
        }
        
        List<Node> finalNodesForGraph = new ArrayList<>();
        // Usa tempNodesListByInternalId para obter os objetos Node originais com suas lat/lon brutas
        for (Node node : tempNodesListByInternalId) { // CORREÇÃO: tempNodesListByInternalId agora está acessível
            if (filteredNodeIds.contains(node.getIdInterno())) {
                finalNodesForGraph.add(node);
            }
        }
        System.out.println("DEBUG: " + finalNodesForGraph.size() + " nós finais preparados para o grafo (após filtragem de conectividade).");

        // Recalcular min/max das coordenadas BRUTAS APENAS DOS NÓS FINAIS para a transformação de display
        double minXRaw = Double.POSITIVE_INFINITY;
        double maxXRaw = Double.NEGATIVE_INFINITY;
        double minYRaw = Double.POSITIVE_INFINITY;
        double maxYRaw = Double.NEGATIVE_INFINITY;

        for (Node node : finalNodesForGraph) {
            if (node.getX() < minXRaw) minXRaw = node.getX();
            if (node.getX() > maxXRaw) maxXRaw = node.getX();
            if (node.getY() < minYRaw) minYRaw = node.getY();
            if (node.getY() > maxYRaw) maxYRaw = node.getY();
        }
        System.out.println("DEBUG: Bounding box dos nós finais (brutos): X[" + minXRaw + ", " + maxXRaw + "], Y[" + minYRaw + ", " + maxYRaw + "]");


        // Aplica a transformação de coordenadas (offset, reducer, inversão Y) para os NÓS FINAIS
        double reducer = 2.0; 
        double maxTransformedY = Double.NEGATIVE_INFINITY;

        for (Node node : finalNodesForGraph) {
            node.setX((node.getX() - minXRaw) / reducer);
            node.setY((node.getY() - minYRaw) / reducer);
            if (node.getY() > maxTransformedY) maxTransformedY = node.getY(); 
        }
        for (Node node : finalNodesForGraph) {
            node.setY(maxTransformedY - node.getY());
            graph.addNode(node); // Adiciona o nó já com as coordenadas finais ao grafo
        }
        System.out.println("DEBUG: " + graph.getNumVertices() + " nós finais adicionados ao grafo.");


        // --- Adicionar Arestas Válidas ao Grafo Final (apenas entre nós que estão no grafo final) ---
        for (RawEdgeData rawEdge : edgesToProcess) { // Iterar sobre TODAS as arestas brutas lidas do .poly
            Node uNode = graph.getNodes().get(rawEdge.uId);
            Node vNode = graph.getNodes().get(rawEdge.vId);

            if (uNode != null && vNode != null) { // Se ambos os nós estão no grafo FINAL (filtrado)
                double weight = Math.sqrt(Math.pow(uNode.getX() - vNode.getX(), 2) + Math.pow(uNode.getY() - vNode.getY(), 2));
                if (weight == 0) {
                    weight = 0.001; 
                }
                graph.addEdge(uNode.getIdInterno(), vNode.getIdInterno(), weight, rawEdge.isDirected);
            } else {
                System.out.println("DEBUG: Aviso: Aresta " + rawEdge.uId + "-" + rawEdge.vId + " ignorada. Nó(s) não encontrado(s) no grafo FINAL após filtragem de conectividade.");
            }
        }
        System.out.println("DEBUG: " + graph.getNumEdges() + " arestas adicionadas ao grafo final.");

        System.out.println("DEBUG: Leitura e filtragem do .poly concluída. Grafo final: " + graph.getNumVertices() + " vértices, " + graph.getNumEdges() + " arestas.");
        return graph;
    }
}