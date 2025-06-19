package io;

import model.Graph;
import model.Node;
import model.Edge; // Necessário para acessar isDirected() de Edge

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OsmConverter {

    private static final Pattern NODE_PATTERN = Pattern.compile("<node id=\"(\\d+)\" lat=\"([\\-\\d.]+)\" lon=\"([\\-\\d.]+)\"");
    private static final Pattern ND_REF_PATTERN = Pattern.compile("<nd ref=\"(\\d+)\"");

    private static class RawEdgeData {
        int uIdInternal;
        int vIdInternal;
        boolean isDirected;

        public RawEdgeData(int uIdInternal, int vIdInternal, boolean isDirected) {
            this.uIdInternal = uIdInternal;
            this.vIdInternal = vIdInternal;
            this.isDirected = isDirected;
        }
    }

    public Graph convertOsmToGraph(String filepath) throws IOException, NumberFormatException, IllegalArgumentException {
        Graph graph = new Graph(); // Este será o grafo final, potencialmente filtrado
        Map<Long, Node> tempNodesMapByOriginalId = new HashMap<>(); 
        List<Node> tempNodesListByInternalId = new ArrayList<>(); 
        int currentInternalId = 0;

        List<RawEdgeData> rawEdgesFromWays = new ArrayList<>(); // Arestas coletadas das <way>s antes da filtragem

        System.out.println("DEBUG_OSM_CONV: Iniciando conversão de " + filepath);

        try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
            String line;
            List<String> allLines = new ArrayList<>(); // Armazena todas as linhas para duas passagens
            while ((line = reader.readLine()) != null) {
                allLines.add(line);
            }
            
            // --- Passagem 1: Coletar e mapear todos os Nós (temporariamente) ---
            System.out.println("DEBUG_OSM_CONV: Passagem 1: Coletando nós...");
            for (String currentLine : allLines) {
                Matcher nodeMatcher = NODE_PATTERN.matcher(currentLine);
                if (nodeMatcher.find()) {
                    long idOriginal = Long.parseLong(nodeMatcher.group(1));
                    double lat = Double.parseDouble(nodeMatcher.group(2));
                    double lon = Double.parseDouble(nodeMatcher.group(3));

                    Node node = new Node(currentInternalId, lon, lat); // x=lon, y=lat (brutos)
                    node.setOriginalOsmId(idOriginal);
                    
                    tempNodesMapByOriginalId.put(idOriginal, node);
                    tempNodesListByInternalId.add(node);
                    currentInternalId++;
                }
            }
            System.out.println("DEBUG_OSM_CONV: Passagem 1 concluída. " + tempNodesListByInternalId.size() + " nós processados temporariamente.");

            // --- Passagem 2: Coletar Vias e suas Arestas Brutas ---
            System.out.println("DEBUG_OSM_CONV: Passagem 2: Processando vias para coletar arestas...");
            boolean insideWay = false;
            List<Long> currentWayOsmNodeRefs = new ArrayList<>();

            for (String currentLine : allLines) {
                if (currentLine.contains("<way")) {
                    insideWay = true;
                    currentWayOsmNodeRefs.clear();
                } else if (insideWay && currentLine.contains("<nd")) {
                    Matcher refMatcher = ND_REF_PATTERN.matcher(currentLine);
                    if (refMatcher.find()) {
                        long osmRefId = Long.parseLong(refMatcher.group(1));
                        currentWayOsmNodeRefs.add(osmRefId);
                    }
                } else if (insideWay && currentLine.contains("</way>")) {
                    insideWay = false;
                    if (currentWayOsmNodeRefs.size() > 1) {
                        for (int i = 0; i < currentWayOsmNodeRefs.size() - 1; i++) {
                            long osmUId = currentWayOsmNodeRefs.get(i);
                            long osmVId = currentWayOsmNodeRefs.get(i + 1);

                            Node uNode = tempNodesMapByOriginalId.get(osmUId);
                            Node vNode = tempNodesMapByOriginalId.get(osmVId);

                            if (uNode != null && vNode != null) {
                                rawEdgesFromWays.add(new RawEdgeData(uNode.getIdInterno(), vNode.getIdInterno(), false)); // isDirected=false por padrão
                            } else {
                                System.out.println("DEBUG_OSM_CONV: Aviso: Via referencia nó OSM " + (uNode == null ? osmUId : osmVId) + " que não foi encontrado. Aresta ignorada.");
                            }
                        }
                    } else {
                        System.out.println("DEBUG_OSM_CONV: Aviso: Way ignorada (menos de 2 nós para formar aresta).");
                    }
                }
            }
            System.out.println("DEBUG_OSM_CONV: Passagem 2 concluída. " + rawEdgesFromWays.size() + " arestas brutas coletadas de Ways.");

        } // reader fecha automaticamente

        // --- CONSTRUÇÃO DE UM GRAFO TEMPORÁRIO PARA CÁLCULO DE GRAU E FILTRAGEM ---
        // Este grafo temp é para analisar conectividade sem aplicar transformações ainda
        Graph tempGraphForConnectivity = new Graph();
        for (Node node : tempNodesListByInternalId) {
            tempGraphForConnectivity.addNode(node); // Adiciona todos os nós temporários
        }
        for (RawEdgeData rawEdge : rawEdgesFromWays) {
            // Adiciona arestas ao grafo temp. O peso pode ser 1 ou a distância bruta.
            // Para cálculo de grau, o peso não importa, só a existência da aresta.
            tempGraphForConnectivity.addEdge(rawEdge.uIdInternal, rawEdge.vIdInternal, 1.0, rawEdge.isDirected);
        }
        System.out.println("DEBUG_OSM_CONV: Grafo temporário para conectividade criado. Nós: " + tempGraphForConnectivity.getNumVertices() + ", Arestas: " + tempGraphForConnectivity.getNumEdges());

        // --- CÁLCULO DE GRAU E IDENTIFICAÇÃO DOS NÓS MAIS CONECTADOS ---
        Map<Integer, Integer> nodeDegrees = new HashMap<>();
        for (Node node : tempGraphForConnectivity.getNodes().values()) {
            // O grau de um nó é o número de conexões em sua lista de adjacência
            nodeDegrees.put(node.getIdInterno(), tempGraphForConnectivity.getAdj().get(node.getIdInterno()).size());
        }

        // Ordenar nós por grau (decrescente)
        List<Map.Entry<Integer, Integer>> sortedNodesByDegree = new ArrayList<>(nodeDegrees.entrySet());
        sortedNodesByDegree.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        // Definir um critério para "os que mais se conectam"
        // Exemplo: Pegar os nós cujo grau está acima de um certo percentil (ex: top 20% dos graus)
        // Ou pegar os N nós com maior grau.
        
        // Vamos pegar todos os nós cujo grau é maior que um determinado limite (ex: 2, 3, etc.)
        // Ou, uma porcentagem do maior grau.
        // Ou, simplesmente pegar o maior componente conectado (já fizemos isso implicitamente com connectedNodeIds)
        // Para "os que mais se conectam", vamos usar um LIMIAR DE GRAU, ou um PERCENTIL.
        
        // Opção 1: Limiar de Grau Fixo (ajuste conforme o mapa)
        int DEGREE_THRESHOLD = 3; // Nível mínimo de conexões para ser considerado "mais conectado"
        Set<Integer> filteredNodeIds = new HashSet<>();
        for (Map.Entry<Integer, Integer> entry : nodeDegrees.entrySet()) {
            if (entry.getValue() >= DEGREE_THRESHOLD) {
                filteredNodeIds.add(entry.getKey());
            }
        }
        System.out.println("DEBUG_OSM_CONV: Nós com grau >= " + DEGREE_THRESHOLD + ": " + filteredNodeIds.size() + " nós.");

        // Se nenhum nó atingir o limiar, podemos pegar os top N para garantir um grafo mínimo
        if (filteredNodeIds.isEmpty() && !tempNodesListByInternalId.isEmpty()) {
            System.out.println("DEBUG_OSM_CONV: Nenhum nó atingiu o limiar de grau. Selecionando os 10% nós com maior grau.");
            int numTopNodes = Math.max(1, tempGraphForConnectivity.getNumVertices() / 10); // Pelo menos 1 nó, 10%
            for (int i = 0; i < Math.min(numTopNodes, sortedNodesByDegree.size()); i++) {
                filteredNodeIds.add(sortedNodesByDegree.get(i).getKey());
            }
            System.out.println("DEBUG_OSM_CONV: Selecionado os " + filteredNodeIds.size() + " nós com maior grau.");
        }


        // --- Construção do GRAFO FINAL (Graph) com APENAS os Nós Filtrados e Arestas Válidas entre eles ---
        if (filteredNodeIds.isEmpty()) {
            System.out.println("DEBUG_OSM_CONV: Nenhum nó filtrado para o grafo final. Retornando grafo vazio.");
            return new Graph();
        }
        
        // Primeiro, filtre os nós que serão adicionados ao grafo final
        List<Node> finalNodesForGraph = new ArrayList<>();
        for (Node node : tempNodesListByInternalId) {
            if (filteredNodeIds.contains(node.getIdInterno())) {
                finalNodesForGraph.add(node);
            }
        }
        System.out.println("DEBUG_OSM_CONV: " + finalNodesForGraph.size() + " nós finais preparados para o grafo.");

        // Recalcular min/max das coordenadas BRUTAS APENAS DOS NÓS FINAIS para a transformação
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
        System.out.println("DEBUG_OSM_CONV: Bounding box dos nós finais (brutos): X[" + minXRaw + ", " + maxXRaw + "], Y[" + minYRaw + ", " + maxYRaw + "]");


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
        System.out.println("DEBUG_OSM_CONV: " + graph.getNumVertices() + " nós finais adicionados ao grafo.");


        // --- Adicionar Arestas Válidas ao Grafo Final (apenas entre nós que estão no grafo final) ---
        for (RawEdgeData rawEdge : rawEdgesFromWays) { // Iterar sobre TODAS as arestas brutas
            // Verifica se ambos os nós da aresta estão no GRAFO FINAL
            Node uNode = graph.getNodes().get(rawEdge.uIdInternal);
            Node vNode = graph.getNodes().get(rawEdge.vIdInternal);

            if (uNode != null && vNode != null) { // Se ambos os nós estão no grafo final
                double weight = Math.sqrt(Math.pow(uNode.getX() - vNode.getX(), 2) + Math.pow(uNode.getY() - vNode.getY(), 2));
                if (weight == 0) {
                    weight = 0.001; 
                }
                graph.addEdge(uNode.getIdInterno(), vNode.getIdInterno(), weight, rawEdge.isDirected);
            } else {
                // Esta aresta é ignorada porque pelo menos um de seus nós não passou no filtro de conectividade
                // System.out.println("DEBUG_OSM_CONV: Aviso: Aresta " + rawEdge.uIdInternal + "-" + rawEdge.vIdInternal + " ignorada. Nó(s) não encontrado(s) no grafo FINAL após filtragem de conectividade.");
            }
        }
        System.out.println("DEBUG_OSM_CONV: " + graph.getNumEdges() + " arestas adicionadas ao grafo final.");

        System.out.println("DEBUG_OSM_CONV: Conversão OSM concluída. Grafo final: " + graph.getNumVertices() + " vértices, " + graph.getNumEdges() + " arestas.");
        return graph;
    }
}