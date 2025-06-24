package gui;

import io.PolyReader;
import io.OsmConverter;
import model.Graph;
import model.Node;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

// Em gui/NavigationApp.java, no topo com as outras importações
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

    // Em NavigationApp.java, junto com os outros componentes

    // Componentes da GUI
    private JLabel originLabel;
    private JLabel destinationLabel;
    private JLabel procTimeLabel;
    private JLabel nodesExploredLabel;
    private JLabel totalCostLabel;
    private JLabel statusBarLabel;
    private JButton selectOriginBtn;
    private JButton selectDestBtn;
    private JButton calculatePathBtn;
    private JButton importGraphPolyBtn;
    private JButton importGraphOsmBtn;

    // Checkboxes de controle de exibição
    private JCheckBox showEdgeLabelsCheckbox;
    // O checkbox para nós foi removido a pedido

    private ButtonGroup editingModeGroup;
    private JRadioButton addNodeRadio;
    private JRadioButton addEdgeRadio;
    private JRadioButton removeElementRadio;
    private JRadioButton noneModeRadio;
    private Integer firstNodeForEdge = null;

    public NavigationApp() {
        setTitle("Sistema de Navegação Primitivo (Java)");
        setSize(1280, 800);
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
        destinationLabel.setForeground(new Color(255, 140, 0));
        procTimeLabel = new JLabel("N/A");
        nodesExploredLabel = new JLabel("N/A");
        totalCostLabel = new JLabel("N/A");
        statusBarLabel = new JLabel("Pronto.");

        selectOriginBtn = new JButton("Origem");
        selectDestBtn = new JButton("Destino");
        calculatePathBtn = new JButton("Traçar Menor Caminho");

        importGraphPolyBtn = new JButton("Importar .poly");
        importGraphOsmBtn = new JButton("Importar .osm");

        showEdgeLabelsCheckbox = new JCheckBox("Rotular Arestas", true);

        // Listeners
        selectOriginBtn.addActionListener(e -> startSelectOrigin());
        selectDestBtn.addActionListener(e -> startSelectDestination());
        calculatePathBtn.addActionListener(e -> calculateShortestPath());
        importGraphPolyBtn.addActionListener(e -> importPolyGraph());
        importGraphOsmBtn.addActionListener(e -> importOsmGraph());

        showEdgeLabelsCheckbox.addActionListener(e -> {
            if (graphPanel != null) {
                graphPanel.setShowEdgeLabels(showEdgeLabelsCheckbox.isSelected());
                graphPanel.repaint();
            }
        });

        editingModeGroup = new ButtonGroup();
        addNodeRadio = new JRadioButton("Adicionar Vértice");
        addEdgeRadio = new JRadioButton("Adicionar Aresta");
        removeElementRadio = new JRadioButton("Remover Elemento");
        noneModeRadio = new JRadioButton("Nenhum");
        noneModeRadio.setSelected(true);

        editingModeGroup.add(addNodeRadio);
        editingModeGroup.add(addEdgeRadio);
        editingModeGroup.add(removeElementRadio);
        editingModeGroup.add(noneModeRadio);
    }

    private void createLayout() {
        setLayout(new BorderLayout(5, 5));

        JPanel controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setBorder(BorderFactory.createTitledBorder("Controles"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        controlPanel.add(importGraphPolyBtn, gbc);
        gbc.gridx = 1;
        controlPanel.add(importGraphOsmBtn, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        controlPanel.add(new JSeparator(), gbc);

        gbc.gridy = 2;
        gbc.gridwidth = 1;
        controlPanel.add(new JLabel("Origem:"), gbc);
        gbc.gridx = 1;
        controlPanel.add(originLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        controlPanel.add(new JLabel("Destino:"), gbc);
        gbc.gridx = 1;
        controlPanel.add(destinationLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        controlPanel.add(selectOriginBtn, gbc);
        gbc.gridx = 1;
        controlPanel.add(selectDestBtn, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        controlPanel.add(calculatePathBtn, gbc);

        gbc.gridy = 6;
        controlPanel.add(new JSeparator(), gbc);

        gbc.gridy = 7;
        controlPanel.add(new JLabel("Modo de Edição:"), gbc);

        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 1;
        controlPanel.add(noneModeRadio, gbc);
        gbc.gridx = 1;
        controlPanel.add(addEdgeRadio, gbc);

        gbc.gridx = 0;
        gbc.gridy = 9;
        controlPanel.add(addNodeRadio, gbc);
        gbc.gridx = 1;
        controlPanel.add(removeElementRadio, gbc);

        gbc.gridy = 10;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        controlPanel.add(showEdgeLabelsCheckbox, gbc);

        gbc.gridy = 11;
        controlPanel.add(new JSeparator(), gbc);

        gbc.gridy = 12;
        controlPanel.add(new JLabel("Estatísticas do Algoritmo:"), gbc);

        gbc.gridy = 13;
        controlPanel.add(procTimeLabel, gbc);
        gbc.gridy = 14;
        controlPanel.add(nodesExploredLabel, gbc);
        gbc.gridy = 15;
        controlPanel.add(totalCostLabel, gbc);

        gbc.gridy = 16;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        controlPanel.add(new JLabel(""), gbc);

        add(new JScrollPane(controlPanel), BorderLayout.WEST);
        add(new JScrollPane(graphPanel), BorderLayout.CENTER);

        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusBar.setBorder(BorderFactory.createEtchedBorder());
        statusBar.add(statusBarLabel);
        add(statusBar, BorderLayout.SOUTH);
    }

    private void importPolyGraph() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Arquivos Poly (*.poly)", "poly"));
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String filepath = fileChooser.getSelectedFile().getAbsolutePath();
            try {
                this.graph = new PolyReader().readPolyFile(filepath);
                graphPanel.setGraph(this.graph);
                resetSelection();
                statusBarLabel.setText("Grafo " + filepath + " importado com sucesso.");
                JOptionPane.showMessageDialog(this, "Grafo importado de .poly com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException | IllegalArgumentException e) {
                JOptionPane.showMessageDialog(this, "Erro ao importar o grafo de .poly: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void importOsmGraph() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Arquivos OSM (*.osm)", "osm"));
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String filepath = fileChooser.getSelectedFile().getAbsolutePath();
            try {
                this.graph = new OsmConverter().convertOsmToGraph(filepath);
                graphPanel.setGraph(this.graph);
                resetSelection();
                statusBarLabel.setText("Mapa OSM " + filepath + " importado e convertido com sucesso.");
                JOptionPane.showMessageDialog(this, "Mapa OSM importado e convertido com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Erro ao importar e converter mapa OSM: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
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

        if (noneModeRadio.isSelected()) {
            Integer clickedNodeId = graphPanel.getNearestNodeIdFromClick(e.getX(), e.getY());
            if (clickedNodeId != null) {
                if (selectedOrigin == null || selectedDestination != null) {
                    resetSelection();
                    setSelectedOrigin(clickedNodeId);
                } else {
                    if (!clickedNodeId.equals(selectedOrigin)) {
                        setSelectedDestination(clickedNodeId);
                    } else {
                        JOptionPane.showMessageDialog(this, "Selecione um nó de destino diferente do de origem.", "Aviso", JOptionPane.WARNING_MESSAGE);
                    }
                }
            }
        } else if (addNodeRadio.isSelected()) {
            addNewNode(e.getX(), e.getY());
        } else if (addEdgeRadio.isSelected()) {
            handleEdgeAdditionClick(e.getX(), e.getY());
        } else if (removeElementRadio.isSelected()) {
            handleRemoveElementClick(e.getX(), e.getY());
        }

        drawGraph();
    }
    
    // MÉTODO ATUALIZADO para restaurar a remoção de arestas
    private void handleRemoveElementClick(int clickX, int clickY) {
        if (graph == null) return;

        Integer clickedNodeId = graphPanel.getNearestNodeIdFromClick(clickX, clickY);

        if (clickedNodeId != null) { // Usuário clicou perto de um nó
            int confirm = JOptionPane.showConfirmDialog(this, "Remover nó " + clickedNodeId + " e todas as suas arestas?", "Confirmar Remoção", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                graph.removeNode(clickedNodeId);
                resetSelection();
            }
        } else { // Usuário clicou fora de um nó, assume que quer remover uma aresta
            String input = JOptionPane.showInputDialog(this, "Para remover uma aresta, digite os IDs dos nós separados por vírgula (ex: 1,2):", "Remover Aresta", JOptionPane.QUESTION_MESSAGE);
            if (input != null && !input.trim().isEmpty()) {
                try {
                    String[] parts = input.split(",");
                    if (parts.length == 2) {
                        int u = Integer.parseInt(parts[0].trim());
                        int v = Integer.parseInt(parts[1].trim());
                        
                        if (graph.removeEdge(u, v)) {
                            JOptionPane.showMessageDialog(this, "Aresta entre " + u + " e " + v + " removida com sucesso.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(this, "Aresta entre " + u + " e " + v + " não encontrada.", "Aviso", JOptionPane.WARNING_MESSAGE);
                        }
                    } else {
                        JOptionPane.showMessageDialog(this, "Formato inválido. Por favor, use o formato 'ID1,ID2'.", "Erro de Formato", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Os IDs dos nós devem ser números inteiros.", "Erro de Formato", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void addNewNode(int clickX, int clickY) {
        if (graph == null) {
            graph = new Graph();
            graphPanel.setGraph(graph);
        }
        
        double graphX = (clickX - graphPanel.getOffsetX()) / graphPanel.getScaleX();
        double graphY = (clickY - graphPanel.getOffsetY()) / graphPanel.getScaleY();

        int newId = 0;
        if (graph.getNodes() != null && !graph.getNodes().isEmpty()) {
            newId = graph.getNodes().keySet().stream().max(Integer::compare).orElse(0) + 1;
        }
        
        Node newNode = new Node(newId, graphX, graphY);
        graph.addNode(newNode);
    }

    private void handleEdgeAdditionClick(int clickX, int clickY) {
        if (graph == null || graph.getNodes().isEmpty()) return;
        
        Integer clickedNodeId = graphPanel.getNearestNodeIdFromClick(clickX, clickY);

        if (clickedNodeId == null) {
            firstNodeForEdge = null;
            return;
        }

        if (firstNodeForEdge == null) {
            firstNodeForEdge = clickedNodeId;
            statusBarLabel.setText("Primeiro nó selecionado: " + firstNodeForEdge + ". Clique no segundo nó.");
        } else {
            if (firstNodeForEdge.equals(clickedNodeId)) return;

            Node node1 = graph.getNodes().get(firstNodeForEdge);
            Node node2 = graph.getNodes().get(clickedNodeId);

            double weight = Math.sqrt(Math.pow(node1.getX() - node2.getX(), 2) + Math.pow(node1.getY() - node2.getY(), 2));
            graph.addEdge(firstNodeForEdge, clickedNodeId, weight, false);
            
            statusBarLabel.setText("Aresta adicionada entre " + firstNodeForEdge + " e " + clickedNodeId + ".");
            firstNodeForEdge = null;
        }
    }

    private void startSelectOrigin() {
        resetSelection();
        statusBarLabel.setText("Clique no nó no gráfico para definir como ORIGEM.");
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
        statusBarLabel.setText("Clique no nó no gráfico para definir como DESTINO.");
        JOptionPane.showMessageDialog(this, "Clique no nó no gráfico para definir como DESTINO.", "Seleção", JOptionPane.INFORMATION_MESSAGE);
    }

    private void calculateShortestPath() {
        if (graph == null || selectedOrigin == null || selectedDestination == null) {
            JOptionPane.showMessageDialog(this, "Por favor, importe um grafo e selecione a origem e o destino.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Graph.PathResult result = graph.dijkstra(selectedOrigin, selectedDestination);

        procTimeLabel.setText(String.format("Tempo: %.2f ms", result.processingTimeMs));
        nodesExploredLabel.setText(String.format("Nós Explorados: %d", result.nodesExplored));
        totalCostLabel.setText(String.format("Custo Total: %.2f", result.totalCost));

        if (result.path != null && !result.path.isEmpty()) {
            shortestPath = result.path;
            statusBarLabel.setText(String.format("Caminho encontrado de %d para %d com custo %.2f.", selectedOrigin, selectedDestination, result.totalCost));
        } else {
            shortestPath = null;
            statusBarLabel.setText(String.format("Não foi possível encontrar um caminho entre %d e %d.", selectedOrigin, selectedDestination));
        }
        drawGraph();
    }

    private void resetSelection() {
        selectedOrigin = null;
        selectedDestination = null;
        shortestPath = null;
        originLabel.setText("N/A");
        destinationLabel.setText("N/A");
        procTimeLabel.setText("N/A");
        nodesExploredLabel.setText("N/A");
        totalCostLabel.setText("N/A");
        statusBarLabel.setText("Seleção reiniciada. Escolha uma nova origem.");
        if (graphPanel != null) drawGraph();
    }

    public void setSelectedOrigin(Integer node) {
        this.selectedOrigin = node;
        this.originLabel.setText(node != null ? node.toString() : "N/A");
        statusBarLabel.setText("Origem selecionada: " + (node != null ? node : "Nenhum") + ". Selecione um destino.");
    }

    public void setSelectedDestination(Integer node) {
        this.selectedDestination = node;
        this.destinationLabel.setText(node != null ? node.toString() : "N/A");
        statusBarLabel.setText("Destino selecionado: " + (node != null ? node : "Nenhum") + ". Calcule o caminho.");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new NavigationApp().setVisible(true));
    }
}