package io;

import model.Graph;
import model.Node;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OsmConverter {

    /**
     * Ponto de entrada principal para a conversão.
     * Orquestra a leitura em dois passos para garantir a integridade dos dados.
     */
    public Graph convertOsmToGraph(String filepath) throws IOException {
        try {
            // --- PASSO 1: Mapear todos os nós e suas coordenadas ---
            System.out.println("DEBUG_OSM_CONV: Iniciando PASSO 1 - Mapeando todos os nós...");
            Map<Long, OsmNode> allNodes = mapAllNodes(filepath);
            System.out.println("DEBUG_OSM_CONV: PASSO 1 Concluído. Total de " + allNodes.size() + " nós únicos mapeados.");

            if (allNodes.isEmpty()) {
                System.out.println("DEBUG_OSM_CONV: Aviso: Nenhum nó foi encontrado no arquivo. Retornando grafo vazio.");
                return new Graph();
            }

            // --- PASSO 2: Construir o grafo processando as 'ways' ---
            System.out.println("DEBUG_OSM_CONV: Iniciando PASSO 2 - Construindo o grafo a partir das 'ways'...");
            Graph finalGraph = buildGraphFromWays(filepath, allNodes);

            System.out.println("DEBUG_OSM_CONV: Conversão finalizada.");
            return finalGraph;

        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Erro ao analisar (parse) o arquivo XML do OSM: " + e.getMessage(), e);
        }
    }

    /**
     * PASSO 1: Lê o arquivo OSM e mapeia todos os nós para um Map.
     */
    private Map<Long, OsmNode> mapAllNodes(String filepath) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();
        NodeHandler nodeHandler = new NodeHandler();
        saxParser.parse(new File(filepath), nodeHandler);
        return nodeHandler.getNodesMap();
    }
    
    /**
     * PASSO 2: Lê o arquivo OSM novamente, construindo o grafo a partir das 'ways'
     * e usando o mapa de nós pré-processado.
     */
    private Graph buildGraphFromWays(String filepath, Map<Long, OsmNode> allNodes) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();
        WayHandler wayHandler = new WayHandler(allNodes);
        saxParser.parse(new File(filepath), wayHandler);
        return wayHandler.getGraph();
    }


    /**
     * Classe auxiliar interna para guardar temporariamente os dados de um nó OSM.
     */
    private static class OsmNode {
        final double lat;
        final double lon;

        public OsmNode(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }
    }

    /**
     * Handler SAX customizado para processar apenas os elementos <node> do arquivo OSM (PASSO 1).
     */
    private static class NodeHandler extends DefaultHandler {
        private final Map<Long, OsmNode> nodesMap = new HashMap<>();

        public Map<Long, OsmNode> getNodesMap() { return nodesMap; }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            if ("node".equals(qName)) {
                try {
                    String idStr = attributes.getValue("id");
                    String latStr = attributes.getValue("lat");
                    String lonStr = attributes.getValue("lon");
                    if (idStr != null && latStr != null && lonStr != null) {
                        nodesMap.put(Long.parseLong(idStr), new OsmNode(Double.parseDouble(latStr), Double.parseDouble(lonStr)));
                    }
                } catch (NumberFormatException e) {
                    System.err.println("AVISO (NodeHandler): Ignorando nó com formato de número inválido.");
                }
            }
        }
    }

    /**
     * Handler SAX customizado para processar os elementos <way> e construir o grafo (PASSO 2).
     */
    private static class WayHandler extends DefaultHandler {
        private final Graph graph = new Graph();
        private final Map<Long, OsmNode> allNodes; // Mapa de nós do Passo 1

        // Estado do Way atual
        private List<Long> currentWayNodes;
        private boolean isHighway;
        private boolean isOneWay;
        
        // Mapeamento para evitar duplicar nós no nosso grafo final
        private int internalNodeIdCounter = 0;
        private final Map<Long, Integer> osmIdToInternalId = new HashMap<>();

        public WayHandler(Map<Long, OsmNode> allNodes) {
            this.allNodes = allNodes;
        }

        public Graph getGraph() { return graph; }
        
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            if ("way".equals(qName)) {
                // Inicia um novo 'way', reseta o estado
                currentWayNodes = new ArrayList<>();
                isHighway = false;
                isOneWay = false;
            } else if ("nd".equals(qName)) {
                // Adiciona uma referência de nó à lista do 'way' atual
                if (currentWayNodes != null) {
                    try {
                        currentWayNodes.add(Long.parseLong(attributes.getValue("ref")));
                    } catch (NumberFormatException e) {
                        System.err.println("AVISO (WayHandler): Ignorando <nd> com 'ref' inválido.");
                    }
                }
            } else if ("tag".equals(qName)) {
                // Verifica as tags para determinar se é uma rua e se é mão única
                String key = attributes.getValue("k");
                if ("highway".equals(key)) {
                    isHighway = true;
                } else if ("oneway".equals(key) && "yes".equals(attributes.getValue("v"))) {
                    isOneWay = true;
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            // O elemento 'way' terminou, hora de processar e construir as arestas
            if ("way".equals(qName)) {
                if (isHighway && currentWayNodes != null && currentWayNodes.size() > 1) {
                    // Itera sobre os pares de nós consecutivos para criar as arestas
                    for (int i = 0; i < currentWayNodes.size() - 1; i++) {
                        long osmUId = currentWayNodes.get(i);
                        long osmVId = currentWayNodes.get(i + 1);
                        
                        // Processa a criação da aresta
                        processEdge(osmUId, osmVId, isOneWay);
                    }
                }
                // Limpa o estado para o próximo 'way'
                currentWayNodes = null;
            }
        }
        
        /**
         * Cria os nós no nosso grafo (se já não existirem) e a aresta entre eles.
         */
        private void processEdge(long osmUId, long osmVId, boolean directed) {
            Node u = getOrCreateNode(osmUId);
            Node v = getOrCreateNode(osmVId);

            // Se ambos os nós são válidos (foram encontrados no mapa do Passo 1)
            if (u != null && v != null) {
                // Calcula o peso como distância euclidiana (simplificado)
                // Para maior precisão, usar a fórmula de Haversine com lat/lon
                double weight = Math.sqrt(Math.pow(u.getX() - v.getX(), 2) + Math.pow(u.getY() - v.getY(), 2));
                if (weight == 0) weight = 0.001; // Evita peso zero
                
                graph.addEdge(u.getIdInterno(), v.getIdInterno(), weight, directed);
            }
        }
        
        /**
         * Verifica se um nó do OSM já foi adicionado ao nosso grafo.
         * Se não, ele cria um novo model.Node, adiciona ao grafo e o retorna.
         * Se sim, apenas o retorna.
         */
        private Node getOrCreateNode(long osmId) {
            // Se já convertemos este nó do OSM para um nó interno, não fazemos nada
            if (osmIdToInternalId.containsKey(osmId)) {
                int internalId = osmIdToInternalId.get(osmId);
                return graph.getNodes().get(internalId);
            }
            
            // Se for a primeira vez que vemos este nó do OSM...
            // 1. Buscamos suas coordenadas no mapa do Passo 1
            OsmNode rawNode = allNodes.get(osmId);
            if (rawNode == null) {
                // Este é o cenário do seu aviso original. O nó é referenciado mas não definido.
                // Agora, com o Passo 1, isso deve acontecer com menos frequência.
                return null; 
            }
            
            // 2. Criamos um novo nó no nosso formato (model.Node)
            int newInternalId = internalNodeIdCounter++;
            // Usamos longitude como X e latitude como Y para consistência
            Node newNode = new Node(newInternalId, rawNode.lon, rawNode.lat);
            newNode.setLabel(String.valueOf(osmId)); // Opcional: rotular com o ID original do OSM

            // 3. Adicionamos ao nosso grafo e ao mapa de mapeamento
            graph.addNode(newNode);
            osmIdToInternalId.put(osmId, newInternalId);
            
            return newNode;
        }
    }
}