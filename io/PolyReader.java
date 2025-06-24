package io;

import model.Graph;
import model.Node;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        Map<Integer, double[]> nodesRawCoords = new HashMap<>();
        List<RawEdgeData> edgesToProcess = new ArrayList<>();
        List<Node> tempNodesListByInternalId = new ArrayList<>();
        
        System.out.println("DEBUG: Lendo arquivo .poly: " + filepath);

        try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
            String line;
            int lineIdx = 0;

            line = reader.readLine(); lineIdx++;
            if (line == null) throw new IOException("Arquivo .poly vazio.");
            String[] headerNodesParts = line.trim().split("\\s+");
            if (headerNodesParts.length < 4) {
                throw new IllegalArgumentException("Cabeçalho de vértice mal formatado na linha " + lineIdx + ": " + line);
            }
            int numVerticesExpected = Integer.parseInt(headerNodesParts[0]);
            
            for (int i = 0; i < numVerticesExpected; i++) {
                line = reader.readLine(); lineIdx++;
                if (line == null) throw new IOException("Número inesperado de linhas de vértice. Esperado " + numVerticesExpected + ", lido " + i);
                String[] parts = line.trim().split("\\s+");
                if (parts.length < 3) {
                    continue;
                }
                int nodeId = Integer.parseInt(parts[0]);
                double xCoord = Double.parseDouble(parts[1]);
                double yCoord = Double.parseDouble(parts[2]);
                
                nodesRawCoords.put(nodeId, new double[]{xCoord, yCoord});
                tempNodesListByInternalId.add(new Node(nodeId, xCoord, yCoord));
            }
            
            line = reader.readLine(); lineIdx++;
            if (line == null) throw new IOException("Seção de arestas ausente.");
            String[] headerEdgesParts = line.trim().split("\\s+");
            if (headerEdgesParts.length < 2) {
                throw new IllegalArgumentException("Cabeçalho de aresta mal formatado na linha " + lineIdx + ": " + line);
            }
            int numEdgesExpected = Integer.parseInt(headerEdgesParts[0]);

            for (int i = 0; i < numEdgesExpected; i++) {
                line = reader.readLine(); lineIdx++;
                if (line == null) throw new IOException("Número inesperado de linhas de aresta. Esperado " + numEdgesExpected + ", lido " + i);
                String[] parts = line.trim().split("\\s+");
                if (parts.length < 4) {
                    continue;
                }
                int uId = Integer.parseInt(parts[1]);
                int vId = Integer.parseInt(parts[2]);
                boolean isDirected = (Integer.parseInt(parts[3]) != 0);
                edgesToProcess.add(new RawEdgeData(uId, vId, isDirected));
            }
        }

        // A lógica de filtragem de conectividade que existia aqui foi removida para simplificar.
        // O grafo será construído com todos os nós e arestas do arquivo .poly.

        // Adiciona todos os nós ao grafo
        for (Node node : tempNodesListByInternalId) {
            graph.addNode(node);
        }

        // Adiciona as arestas ao grafo final
        for (RawEdgeData rawEdge : edgesToProcess) {
            Node uNode = graph.getNodes().get(rawEdge.uId);
            Node vNode = graph.getNodes().get(rawEdge.vId);

            if (uNode != null && vNode != null) {
                // Calcula a distância euclidiana com base nas coordenadas
                double weight = Math.sqrt(Math.pow(uNode.getX() - vNode.getX(), 2) + Math.pow(uNode.getY() - vNode.getY(), 2));
                
                // --- CORREÇÃO APLICADA AQUI ---
                // Aumenta a escala do peso para que seja um número visível e significativo.
                // O fator 10000 é um exemplo; pode ser ajustado conforme necessário.
                weight *= 10000; 
                // --- FIM DA CORREÇÃO ---

                if (weight == 0) {
                    weight = 0.001; 
                }
                graph.addEdge(uNode.getIdInterno(), vNode.getIdInterno(), weight, rawEdge.isDirected);
            }
        }
        
        System.out.println("DEBUG: Leitura do .poly concluída. Grafo final: " + graph.getNumVertices() + " vértices, " + graph.getNumEdges() + " arestas.");
        return graph;
    }
}