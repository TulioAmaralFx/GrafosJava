package gui;

import io.PolyReader;
import io.OsmConverter; // IMPORTAÇÃO DA NOVA CLASSE
import model.Graph;
import model.Node; 

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.List;

public class NavigationApp extends JFrame {
    private Graph graph;
    private GraphPanel graphPanel;

    private Integer selectedOrigin = null;
    private Integer selectedDestination = null;
    private List<Integer> shortestPath = null;

    // Componentes da GUI
    private JLabel originLabel;
    private JLabel destinationLabel;
    private JLabel procTimeLabel;
    private JLabel nodesExploredLabel;
    private JLabel totalCostLabel;
    private JButton selectOriginBtn;
    private JButton selectDestBtn;
    private JButton calculatePathBtn;
    private JButton importGraphPolyBtn; // Botão para importar .poly
    private JButton importGraphOsmBtn; // NOVO: Botão para importar .osm

    // Cor personalizada para LIME (verde limão) - para o caminho mais curto
    private static final Color CUSTOM_LIME_COLOR = new Color(50, 205, 50);

    public NavigationApp() {
        setTitle("Sistema de Navegação Primitivo (Java)");
        setSize(1024, 768);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
        createLayout();
    }

    private void initComponents() {
        graphPanel = new GraphPanel();
        graphPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onGraphPanelClick(e);
            }
        });

        originLabel = new JLabel("N/A");
        originLabel.setForeground(Color.BLUE);
        destinationLabel = new JLabel("N/A");
        destinationLabel.setForeground(Color.RED);
        procTimeLabel = new JLabel("Tempo (ms): N/A");
        nodesExploredLabel = new JLabel("Nós Explorados: N/A");
        totalCostLabel = new JLabel("Custo Total: N/A");

        selectOriginBtn = new JButton("Selecionar Origem");
        selectOriginBtn.addActionListener(e -> startSelectOrigin());
        selectDestBtn = new JButton("Selecionar Destino");
        selectDestBtn.addActionListener(e -> startSelectDestination());
        calculatePathBtn = new JButton("Traçar Menor Caminho");
        calculatePathBtn.addActionListener(e -> calculateShortestPath());

        importGraphPolyBtn = new JButton("Importar Grafo (.poly)");
        importGraphPolyBtn.addActionListener(e -> importPolyGraph());

        importGraphOsmBtn = new JButton("Importar Mapa (.osm)"); // NOVO BOTÃO
        importGraphOsmBtn.addActionListener(e -> importOsmGraph()); // NOVO MÉTODO
    }

    private void createLayout() {
        setLayout(new BorderLayout());

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        controlPanel.add(importGraphPolyBtn); // Botão para .poly
        controlPanel.add(importGraphOsmBtn); // Botão para .osm
        controlPanel.add(Box.createVerticalStrut(10));

        controlPanel.add(new JLabel("Estatísticas do Algoritmo:"));
        controlPanel.add(procTimeLabel);
        controlPanel.add(nodesExploredLabel);
        controlPanel.add(totalCostLabel);
        controlPanel.add(Box.createVerticalStrut(10));

        controlPanel.add(new JLabel("Origem:"));
        controlPanel.add(originLabel);
        controlPanel.add(new JLabel("Destino:"));
        controlPanel.add(destinationLabel);
        controlPanel.add(Box.createVerticalStrut(10));

        controlPanel.add(selectOriginBtn);
        controlPanel.add(selectDestBtn);
        controlPanel.add(calculatePathBtn);
        controlPanel.add(Box.createVerticalStrut(10));

        JPanel controlWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlWrapper.add(controlPanel);

        add(controlWrapper, BorderLayout.WEST);

        JScrollPane scrollPane = new JScrollPane(graphPanel);
        add(scrollPane, BorderLayout.CENTER);
    }

    // Método para importar .poly (existente)
    private void importPolyGraph() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Arquivos Poly (*.poly)", "poly"));
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            String filepath = fileChooser.getSelectedFile().getAbsolutePath();
            PolyReader reader = new PolyReader();
            try {
                this.graph = reader.readPolyFile(filepath);
                graphPanel.setGraph(this.graph);
                graphPanel.setZoomGeneral(0.0); // Resetar o zoom para encaixar
                resetSelection();
                JOptionPane.showMessageDialog(this, "Grafo importado de .poly com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);

            } catch (IOException | IllegalArgumentException e) {
                JOptionPane.showMessageDialog(this, "Erro ao importar o grafo de .poly: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    // NOVO MÉTODO: Importar e converter arquivo .osm
    private void importOsmGraph() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Arquivos OSM (*.osm)", "osm"));
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            String filepath = fileChooser.getSelectedFile().getAbsolutePath();
            OsmConverter converter = new OsmConverter(); // Instancia o novo conversor de OSM
            try {
                // Converte o arquivo OSM para um objeto Graph
                this.graph = converter.convertOsmToGraph(filepath); // Obtém o grafo convertido
                
                graphPanel.setGraph(this.graph); // Passa o grafo para o painel de desenho
                graphPanel.setZoomGeneral(0.0); // Reseta o zoom para encaixar o novo grafo
                resetSelection();
                JOptionPane.showMessageDialog(this, "Mapa OSM importado e convertido com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);

            } catch (IOException | IllegalArgumentException e) { // IllegalArgumentException captura NumberFormatException
                JOptionPane.showMessageDialog(this, "Erro ao importar e converter mapa OSM: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    private void drawGraph() {
        graphPanel.setSelectedOrigin(selectedOrigin);
        graphPanel.setSelectedDestination(selectedDestination);
        graphPanel.setShortestPath(shortestPath);
        graphPanel.repaint();
    }

    private void onGraphPanelClick(MouseEvent e) {
        if (graph == null) return;

        Integer clickedNodeId = graphPanel.getNearestNodeIdFromClick(e.getX(), e.getY());

        if (clickedNodeId != null) {
            System.out.println("DEBUG_GUI: Nó clicado: " + clickedNodeId);
            if (selectedOrigin == null) {
                setSelectedOrigin(clickedNodeId);
            } else if (selectedDestination == null) {
                if (!clickedNodeId.equals(selectedOrigin)) {
                    setSelectedDestination(clickedNodeId);
                } else {
                    JOptionPane.showMessageDialog(this, "Selecione um nó de destino diferente do de origem.", "Aviso", JOptionPane.WARNING_MESSAGE);
                    resetSelection();
                }
            } else {
                resetSelection();
                setSelectedOrigin(clickedNodeId);
            }
        } else {
            System.out.println("DEBUG_GUI: Nenhum nó próximo ao clique.");
        }
        drawGraph();
    }

    private void startSelectOrigin() {
        resetSelection();
        JOptionPane.showMessageDialog(this, "Clique no nó no gráfico para definir como ORIGEM.", "Seleção", JOptionPane.INFORMATION_MESSAGE);
    }

    private void startSelectDestination() {
        if (selectedOrigin == null) {
            JOptionPane.showMessageDialog(this, "Por favor, selecione primeiro o nó de ORIGEM.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        shortestPath = null;
        selectedDestination = null;
        destinationLabel.setText("Aguardando clique...");
        drawGraph();
        JOptionPane.showMessageDialog(this, "Clique no nó no gráfico para definir como DESTINO.", "Seleção", JOptionPane.INFORMATION_MESSAGE);
    }

    private void calculateShortestPath() {
        if (graph == null || selectedOrigin == null || selectedDestination == null) {
            JOptionPane.showMessageDialog(this, "Por favor, importe um grafo e selecione a origem e o destino.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        System.out.printf("DEBUG_PATH: Calculando caminho de %d para %d%n", selectedOrigin, selectedDestination);
        
        Graph.PathResult result = graph.dijkstra(selectedOrigin, selectedDestination);

        procTimeLabel.setText(String.format("Tempo (ms): %.2f", result.processingTimeMs));
        nodesExploredLabel.setText(String.format("Nós Explorados: %d", result.nodesExplored));
        totalCostLabel.setText(String.format("Custo Total: %.2f", result.totalCost));

        if (!result.path.isEmpty() && result.totalCost != Double.POSITIVE_INFINITY) {
            shortestPath = result.path;
            JOptionPane.showMessageDialog(this, String.format("Menor caminho encontrado com custo %.2f.", result.totalCost), "Caminho Calculado", JOptionPane.INFORMATION_MESSAGE);
        } else {
            shortestPath = null;
            JOptionPane.showMessageDialog(this, "Não foi possível encontrar um caminho entre os nós selecionados.", "Caminho Não Encontrado", JOptionPane.INFORMATION_MESSAGE);
        }
        drawGraph();
    }

    private void resetSelection() {
        selectedOrigin = null;
        selectedDestination = null;
        shortestPath = null;
        originLabel.setText("N/A");
        destinationLabel.setText("N/A");
        procTimeLabel.setText("Tempo (ms): N/A");
        nodesExploredLabel.setText("Nós Explorados: N/A");
        totalCostLabel.setText("Custo Total: N/A");
        drawGraph();
    }

    public void setSelectedOrigin(Integer node) {
        this.selectedOrigin = node;
        this.originLabel.setText(node != null ? node.toString() : "N/A");
    }

    public void setSelectedDestination(Integer node) {
        this.selectedDestination = node;
        this.destinationLabel.setText(node != null ? node.toString() : "N/A");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new NavigationApp().setVisible(true);
        });
    }
}