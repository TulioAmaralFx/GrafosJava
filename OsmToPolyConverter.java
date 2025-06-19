import model.OsmNode;
import model.OsmWay;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OsmToPolyConverter {

    // Regex patterns para extrair atributos das linhas
    private static final Pattern NODE_PATTERN = Pattern.compile("<node id=\"(\\d+)\" lat=\"([\\-\\d.]+)\" lon=\"([\\-\\d.]+)\"");
    private static final Pattern ND_REF_PATTERN = Pattern.compile("<nd ref=\"(\\d+)\"");

    // Limites de array (os mesmos do código C original e python, ajustados)
    private static final int MAX_NODES_LIMIT = 200000;
    private static final int MAX_WAYS_LIMIT = 200000;

    // Variáveis estáticas para armazenar os dados do grafo durante a conversão
    private static Map<Long, OsmNode> nodesMapByOriginalId = new HashMap<>(); // {idOriginalOsm: OsmNode_obj}
    private static List<OsmNode> nodesListByInternalId = new ArrayList<>();   // Lista para acesso por idInterno (sequencial)
    private static int totalProcessedNodes = 0;

    private static List<OsmWay> waysProcessedList = new ArrayList<>();
    private static int totalProcessedWays = 0;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Uso: java -cp bin br.ufg.inf.aed2.osmconverter.main.OsmToPolyConverter <arquivo.osm>");
            System.err.println("Exemplo: java -cp bin br.ufg.inf.aed2.osmconverter.main.OsmToPolyConverter meu_mapa.osm");
            return;
        }
        convertOsmToPoly(args[0]);
    }

    // Procura o ID interno de um nó dado seu ID original do OSM
    private static int getInternalNodeId(long osmId) {
        OsmNode node = nodesMapByOriginalId.get(osmId);
        return (node != null) ? node.idInterno : -1;
    }

    // Reduz a escala das coordenadas x/y e inverte o eixo Y para visualização no .poly
    private static void reduceScaleAndInvertY(List<OsmNode> nodes) {
        if (nodes.isEmpty()) {
            return;
        }

        // Calcula os limites (min/max) das coordenadas x/y (lon/lat brutas)
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        for (OsmNode node : nodes) {
            if (node.x < minX) minX = node.x;
            if (node.x > maxX) maxX = node.x;
            if (node.y < minY) minY = node.y;
            if (node.y > maxY) maxY = node.y;
        }

        // O redutor (scale_factor) é 2.0 para consistência com o código C e Python
        double reducer = 2.0; 

        // Aplica o offset e o redutor de escala
        for (OsmNode node : nodes) {
            node.x = (node.x - minX) / reducer;
            node.y = (node.y - minY) / reducer;
        }

        // Encontra o novo maxY após a redução para a inversão do eixo
        double newMaxY = Double.NEGATIVE_INFINITY;
        for (OsmNode node : nodes) {
            if (node.y > newMaxY) newMaxY = node.y;
        }

        // Inverte o eixo Y
        for (OsmNode node : nodes) {
            node.y = newMaxY - node.y;
        }
    }

    public static void convertOsmToPoly(String osmFilename) {
        // Reinicializa as variáveis estáticas para cada nova conversão
        nodesMapByOriginalId.clear();
        nodesListByInternalId.clear();
        totalProcessedNodes = 0;
        waysProcessedList.clear();
        totalProcessedWays = 0;

        String polyFilename = osmFilename.substring(0, osmFilename.lastIndexOf('.')) + ".poly";

        System.out.println("--- Iniciando conversão de " + osmFilename + " para " + polyFilename + " ---");

        try (BufferedReader reader = new BufferedReader(new FileReader(osmFilename));
             PrintWriter writer = new PrintWriter(new FileWriter(polyFilename))) {

            String line;
            List<String> allLines = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                allLines.add(line);
            }
            
            // --- Primeira Passagem: Coletar e mapear todos os Nós ---
            System.out.println("DEBUG_CONV: Passagem 1: Coletando nós...");
            int nodesFoundInFile = 0;
            for (String currentLine : allLines) {
                Matcher nodeMatcher = NODE_PATTERN.matcher(currentLine);
                if (nodeMatcher.find()) {
                    nodesFoundInFile++;
                    long idOriginal = Long.parseLong(nodeMatcher.group(1));
                    double lat = Double.parseDouble(nodeMatcher.group(2));
                    double lon = Double.parseDouble(nodeMatcher.group(3));

                    if (totalProcessedNodes < MAX_NODES_LIMIT) {
                        OsmNode node = new OsmNode(idOriginal, lat, lon, totalProcessedNodes++);
                        nodesMapByOriginalId.put(idOriginal, node);
                        nodesListByInternalId.add(node); // Mantém em ordem pelo ID interno
                    } else {
                        System.out.println("DEBUG_CONV: Aviso: Limite de MAX_NODES (" + MAX_NODES_LIMIT + ") excedido. Ignorando nós restantes.");
                    }
                }
            }
            System.out.println("DEBUG_CONV: Passagem 1 concluída. Nós encontrados no arquivo: " + nodesFoundInFile + ". Nós processados: " + totalProcessedNodes);

            // --- Segunda Passagem: Coletar Vias e suas Arestas ---
            System.out.println("DEBUG_CONV: Passagem 2: Processando vias...");
            boolean insideWay = false;
            OsmWay currentOsmWay = null;
            int waysFoundInFile = 0;

            for (String currentLine : allLines) {
                if (currentLine.contains("<way")) {
                    insideWay = true;
                    currentOsmWay = new OsmWay();
                    waysFoundInFile++;
                } else if (insideWay && currentLine.contains("<nd")) {
                    Matcher refMatcher = ND_REF_PATTERN.matcher(currentLine);
                    if (refMatcher.find()) {
                        long osmRefId = Long.parseLong(refMatcher.group(1));
                        int internalId = getInternalNodeId(osmRefId); // Obtém o ID interno
                        if (internalId != -1) {
                            currentOsmWay.nodeInternalIds.add(internalId);
                            currentOsmWay.count++;
                        } else {
                            System.out.println("DEBUG_CONV: Aviso: Via referencia nó OSM " + osmRefId + " que não foi encontrado. Aresta(s) desta via podem ser ignoradas.");
                        }
                    }
                } else if (insideWay && currentLine.contains("</way>")) {
                    insideWay = false;
                    // Uma via deve ter pelo menos 2 nós para formar arestas
                    if (currentOsmWay.count > 1 && totalProcessedWays < MAX_WAYS_LIMIT) { 
                        waysProcessedList.add(currentOsmWay);
                        totalProcessedWays++;
                    } else {
                        System.out.println("DEBUG_CONV: Aviso: Way ignorada (menos de 2 nós ou limite " + MAX_WAYS_LIMIT + " excedido).");
                    }
                }
            }
            System.out.println("DEBUG_CONV: Passagem 2 concluída. Ways encontradas no arquivo: " + waysFoundInFile + ". Ways processadas para grafo: " + totalProcessedWays);

            // --- Transformar Coordenadas dos Nós (x/y para visualização no .poly) ---
            System.out.println("DEBUG_CONV: Transformando coordenadas dos nós...");
            if (!nodesListByInternalId.isEmpty()) {
                reduceScaleAndInvertY(nodesListByInternalId); 
                System.out.println("DEBUG_CONV: Coordenadas transformadas.");
            } else {
                System.out.println("DEBUG_CONV: Sem nós para transformar coordenadas.");
                return; // Não há nada para escrever no .poly
            }

            // --- Escrever o Arquivo .poly ---
            System.out.println("DEBUG_CONV: Escrevendo arquivo .poly...");

            // Cabeçalho dos vértices: <num_vertices> <dim> <attrs> <boundary_markers>
            writer.printf("%d\t%d\t%d\t%d%n", totalProcessedNodes, 2, 0, 1);
            
            // Dados dos vértices: <id interno> <lat_original> <lon_original>
            for (OsmNode node : nodesListByInternalId) {
                // Escreve lat/lon ORIGINAIS no .poly, mesmo que x/y foram usados para transformação interna.
                writer.printf("%d\t%.6f\t%.6f%n", node.idInterno, node.lat, node.lon);
            }
            System.out.println("DEBUG_CONV: Vértices escritos. ");

            // Cabeçalho das arestas: <número de arestas> <limites>
            int numEdgesToWrite = 0;
            for (OsmWay way : waysProcessedList) {
                numEdgesToWrite += (way.count - 1); // Cada via com N nós gera N-1 arestas
            }
            System.out.println("DEBUG_CONV: Número total de arestas a serem escritas: " + numEdgesToWrite);
            writer.printf("%d\t%d%n", numEdgesToWrite, 1);

            // Dados das arestas: <id aresta> <origem_id_interno> <destino_id_interno> <flag_direcional>
            int edgeIdCounter = 0;
            for (OsmWay way : waysProcessedList) {
                for (int i = 0; i < way.count - 1; i++) {
                    int fromNodeInternalId = way.nodeInternalIds.get(i);
                    int toNodeInternalId = way.nodeInternalIds.get(i + 1);
                    // Flag 0 para não direcionado (bidirecional no .poly), como no C original
                    writer.printf("%d\t%d\t%d\t%d%n", edgeIdCounter++, fromNodeInternalId, toNodeInternalId, 0);
                }
            }
            System.out.println("DEBUG_CONV: Arestas escritas.");
            
            // Linha final do arquivo .poly
            writer.println(0);
            System.out.println("DEBUG_CONV: Finalizador do .poly escrito.");

            System.out.println("--- Conversão concluída. Arquivo \"" + polyFilename + "\" criado com sucesso. ---");

        } catch (IOException e) {
            System.err.println("Erro de E/S ao processar o arquivo: " + e.getMessage());
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.err.println("Erro: Dados numéricos inválidos no arquivo OSM: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Ocorreu um erro inesperado durante a conversão: " + e.getMessage());
            e.printStackTrace();
        }
    }
}