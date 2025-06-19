package gui;

import model.Graph;
import model.Node;
import model.Edge;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;

import java.util.List;
import java.util.Map;

public class GraphPanel extends JPanel {
    private Graph graph;
    private Integer selectedOrigin;
    private Integer selectedDestination;
    private List<Integer> shortestPath;

    // Parâmetros de escalonamento e offset
    private double scaleX; // Escala independente para o eixo X
    private double scaleY; // Escala independente para o eixo Y
    private double offsetX; // Offset para pan horizontal
    private double offsetY; // Offset para pan vertical
    
    // Bounding box dos dados brutos do grafo (após a transformação inicial do PolyReader)
    private double minXData, maxXData, minYData, maxYData;

    private final int NODE_RADIUS = 8; // Raio dos nós para desenho
    private final int CLICK_TOLERANCE_PX = 10; // Tolerância de clique em pixels

    // Cor personalizada para LIME (verde limão) - para o caminho mais curto
    private static final Color CUSTOM_LIME_COLOR = new Color(50, 205, 50);

    // --- Construtor ---
    public GraphPanel() {
        setBackground(Color.WHITE); // Cor de fundo do painel
        
        // Adiciona listeners para interatividade de zoom e pan
        // CORREÇÃO: Usar 'this' para referenciar o objeto interno que está sendo instanciado
        // ou declarar as classes internas como static se elas não acessam membros não-estáticos.
        // Vamos usar a segunda opção, declarando-as static.
        addMouseWheelListener(new MouseWheelHandler(this)); // Passa a instância do painel
        addMouseListener(new MousePressHandler(this));
        addMouseMotionListener(new MouseDragHandler(this));

        // Inicializa as escalas como 0.0 para indicar que elas precisam ser calculadas
        this.scaleX = 0.0;
        this.scaleY = 0.0;
    }

    // --- Getters e Setters para dados do grafo e seleção ---
    public void setGraph(Graph graph) {
        this.graph = graph;
        this.scaleX = 0.0; 
        this.scaleY = 0.0;
        calculateScalingParameters();
    }

    public void setSelectedOrigin(Integer selectedOrigin) {
        this.selectedOrigin = selectedOrigin;
    }

    public void setSelectedDestination(Integer selectedDestination) {
        this.selectedDestination = selectedDestination;
    }

    public void setShortestPath(List<Integer> shortestPath) {
        this.shortestPath = shortestPath;
    }

    // Novos setters para controle de zoom externo
    public void setZoomX(double newScaleX) {
        if (newScaleX > 0) {
            this.scaleX = newScaleX;
            recalculateOffsets();
            repaint();
        }
    }

    public void setZoomY(double newScaleY) {
        if (newScaleY > 0) {
            this.scaleY = newScaleY;
            recalculateOffsets();
            repaint();
        }
    }

    public void setZoomGeneral(double newScaleFactor) {
        if (newScaleFactor > 0) {
            if (minXData == Double.POSITIVE_INFINITY || minXData == Double.NEGATIVE_INFINITY) {
                calculateInitialBoundingBox();
            }

            double panelCenterX = getWidth() / 2.0;
            double panelCenterY = getHeight() / 2.0;

            double graphXCenter = (panelCenterX - offsetX) / scaleX; // Usa a escala atual para reverter
            double graphYCenter = (panelCenterY - offsetY) / scaleY;
            
            this.scaleX = newScaleFactor;
            this.scaleY = newScaleFactor;

            offsetX = panelCenterX - graphXCenter * this.scaleX;
            offsetY = panelCenterY - graphYCenter * this.scaleY;
            
            repaint();
        } else { // newScaleFactor é 0 ou negativo, significa "resetar para fit"
            this.scaleX = 0.0; 
            this.scaleY = 0.0;
            calculateScalingParameters(); 
            repaint();
        }
    }

    // Recalcula o bounding box dos dados brutos
    private void calculateInitialBoundingBox() {
        if (graph == null || graph.getNodes().isEmpty()) {
            minXData = 0.0; maxXData = 1.0; minYData = 0.0; maxYData = 1.0;
            return;
        }
        
        minXData = Double.POSITIVE_INFINITY;
        maxXData = Double.NEGATIVE_INFINITY;
        minYData = Double.POSITIVE_INFINITY;
        maxYData = Double.NEGATIVE_INFINITY;

        for (Node node : graph.getNodes().values()) {
            if (node.getX() < minXData) minXData = node.getX();
            if (node.getX() > maxXData) maxXData = node.getX();
            if (node.getY() < minYData) minYData = node.getY();
            if (node.getY() > maxYData) maxYData = node.getY();
        }
    }

    // Calcula os parâmetros de escalonamento e offset para o desenho (fit inicial ou reset)
    private void calculateScalingParameters() {
        calculateInitialBoundingBox();

        int panelWidth = getWidth();
        int panelHeight = getHeight();

        if (panelWidth <= 0 || panelHeight <= 0) {
            panelWidth = 800;
            panelHeight = 600;
        }

        double padding = 20.0;
        double rangeX = maxXData - minXData;
        double rangeY = maxYData - minYData;

        double fitScaleX = (panelWidth - 2 * padding) / (rangeX > 0 ? rangeX : 1.0);
        double fitScaleY = (panelHeight - 2 * padding) / (rangeY > 0 ? rangeY : 1.0);

        if (this.scaleX == 0.0 || this.scaleY == 0.0) { 
            this.scaleX = Math.min(fitScaleX, fitScaleY); 
            this.scaleY = this.scaleX; 
        }
        
        recalculateOffsets();

        System.out.printf("DEBUG_DRAW_PANEL: Painel dim: %dx%d, Padding: %.1f%n", panelWidth, panelHeight, padding);
        System.out.printf("DEBUG_DRAW_PANEL: Dados min/max: X[%.6f, %.6f], Y[%.6f, %.6f]%n", minXData, maxXData, minYData, maxYData);
        System.out.printf("DEBUG_DRAW_PANEL: Amplitude Dados: RX=%.6f, RY=%.6f%n", rangeX, rangeY);
        System.out.printf("DEBUG_DRAW_PANEL: Escalas Calc: SX=%.6f, SY=%.6f%n", this.scaleX, this.scaleY);
        System.out.printf("DEBUG_DRAW_PANEL: Offsets Calc: OX=%.6f, OY=%.6f%n", offsetX, offsetY);
    }

    // Recalcula apenas os offsets com base nas escalas atuais
    private void recalculateOffsets() {
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        if (panelWidth <= 0 || panelHeight <= 0) {
            panelWidth = 800;
            panelHeight = 600;
        }
        double padding = 20.0;

        double contentWidth = (maxXData - minXData) * scaleX;
        double contentHeight = (maxYData - minYData) * scaleY;

        offsetX = padding + (panelWidth - 2 * padding - contentWidth) / 2.0 - minXData * scaleX;
        offsetY = padding + (panelHeight - 2 * padding - contentHeight) / 2.0 - minYData * scaleY;
    }


    // --- Método de Desenho ---
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (graph == null || graph.getNodes().isEmpty()) {
            return;
        }

        if (this.scaleX == 0.0 || this.scaleY == 0.0 || getWidth() != getPreferredSize().width || getHeight() != getPreferredSize().height) {
             calculateScalingParameters();
        }

        System.out.println("DEBUG_DRAW_PANEL: Desenhando grafo no painel. Nós: " + graph.getNumVertices() + ", Arestas: " + graph.getNumEdges());

        // --- Desenhar Arestas ---
        g2d.setStroke(new BasicStroke(1));
        
        for (Map.Entry<Integer, Map<Integer, Edge>> entryU : graph.getAdj().entrySet()) {
            Node uNode = graph.getNodes().get(entryU.getKey());
            if (uNode == null) continue;

            double x1 = uNode.getX() * scaleX + offsetX;
            double y1 = uNode.getY() * scaleY + offsetY;

            for (Map.Entry<Integer, Edge> entryV : entryU.getValue().entrySet()) {
                Edge edge = entryV.getValue();
                Node vNode = graph.getNodes().get(edge.getV());
                if (vNode == null) continue;

                if (!edge.isDirected() && uNode.getIdInterno() > vNode.getIdInterno()) {
                    continue;
                }

                double x2 = vNode.getX() * scaleX + offsetX;
                double y2 = vNode.getY() * scaleY + offsetY;

                Color lineColor = Color.BLUE;
                int lineWidth = 3;
                
                if (shortestPath != null && isEdgeInPath(uNode.getIdInterno(), vNode.getIdInterno())) {
                    lineColor = Color.GREEN;
                    lineWidth = 4;
                }
                
                g2d.setColor(lineColor);
                g2d.setStroke(new BasicStroke(lineWidth));
                g2d.drawLine((int) x1, (int) y1, (int) x2, (int) y2);

                if (edge.isDirected()) {
                    drawArrow(g2d, x1, y1, x2, y2, lineColor);
                }
            }
        }

        // --- Desenhar Nós ---
        for (Node node : graph.getNodes().values()) {
            double x = node.getX() * scaleX + offsetX;
            double y = node.getY() * scaleY + offsetY;

            Color nodeColor = Color.RED;
            Color outlineColor = Color.DARK_GRAY;

            if (selectedOrigin != null && node.getIdInterno() == selectedOrigin) {
                nodeColor = Color.BLUE;
            } else if (selectedDestination != null && node.getIdInterno() == selectedDestination) {
                nodeColor = Color.ORANGE;
            }
            else if (shortestPath != null && shortestPath.contains(node.getIdInterno())) {
                nodeColor = CUSTOM_LIME_COLOR;
            }

            g2d.setColor(nodeColor);
            g2d.fillOval((int) (x - NODE_RADIUS), (int) (y - NODE_RADIUS), NODE_RADIUS * 2, NODE_RADIUS * 2);
            g2d.setColor(outlineColor);
            g2d.drawOval((int) (x - NODE_RADIUS), (int) (y - NODE_RADIUS), NODE_RADIUS * 2, NODE_RADIUS * 2);

            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            g2d.drawString(node.getLabel(), (int) x + NODE_RADIUS + 2, (int) y + NODE_RADIUS + 2);
        }
        System.out.println("DEBUG_DRAW_PANEL: Desenho completo.");
    }

    private boolean isEdgeInPath(int uId, int vId) {
        if (shortestPath == null || shortestPath.size() < 2) return false;
        for (int i = 0; i < shortestPath.size() - 1; i++) {
            int pathU = shortestPath.get(i);
            int pathV = shortestPath.get(i + 1);
            
            if (pathU == uId && pathV == vId) {
                return true;
            }
            if (graph.getAdj().containsKey(uId) && graph.getAdj().get(uId).containsKey(vId) && !graph.getAdj().get(uId).get(vId).isDirected()) {
                if (pathU == vId && pathV == uId) {
                    return true;
                }
            }
        }
        return false;
    }

    private void drawArrow(Graphics2D g2, double x1, double y1, double x2, double y2, Color color) {
        g2.setColor(color);
        int ARR_SIZE = 8;

        double dx = x2 - x1;
        double dy = y2 - y1;
        double angle = Math.atan2(dy, dx);

        double distance = Math.sqrt(dx*dx + dy*dy);
        double ratio = (distance > NODE_RADIUS) ? (distance - NODE_RADIUS) / distance : 0; 
        
        double arrowX = x1 + dx * ratio;
        double arrowY = y1 + dy * ratio;

        AffineTransform oldTransform = g2.getTransform();
        g2.translate(arrowX, arrowY);
        g2.rotate(angle);

        Path2D arrowHead = new Path2D.Double();
        arrowHead.moveTo(0, 0);
        arrowHead.lineTo(-ARR_SIZE, ARR_SIZE / 2.0);
        arrowHead.lineTo(-ARR_SIZE, -ARR_SIZE / 2.0);
        arrowHead.closePath();
        g2.fill(arrowHead);

        g2.setTransform(oldTransform);
    }

    public Integer getNearestNodeIdFromClick(int clickX, int clickY) {
        if (graph == null || graph.getNodes().isEmpty()) {
            return null;
        }

        Integer nearestNodeId = null;
        double minDistanceSq = Double.POSITIVE_INFINITY;

        for (Node node : graph.getNodes().values()) {
            double nodeXCanvas = node.getX() * scaleX + offsetX;
            double nodeYCanvas = node.getY() * scaleY + offsetY;

            double distSq = Math.pow(clickX - nodeXCanvas, 2) + Math.pow(clickY - nodeYCanvas, 2);

            if (distSq < minDistanceSq && distSq <= Math.pow(CLICK_TOLERANCE_PX, 2)) {
                minDistanceSq = distSq;
                nearestNodeId = node.getIdInterno();
            }
        }
        return nearestNodeId;
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        if (graph != null && (this.getWidth() != width || this.getHeight() != height)) {
            calculateScalingParameters();
            repaint();
        }
    }

    // --- Handlers de Eventos do Mouse para Zoom e Pan ---

    // A classe MousePressHandler agora acessa membros do GraphPanel
    // Ela precisa ser uma classe interna "normal" (não estática) para acessar 'lastMousePressPoint' diretamente,
    // ou receber o GraphPanel via construtor se for estática.
    // Optaremos por passar 'this' (a instância do GraphPanel) para ela.
    private class MousePressHandler extends MouseAdapter {
        private GraphPanel panel; // Referência para o GraphPanel

        public MousePressHandler(GraphPanel panel) {
            this.panel = panel;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            panel.lastMousePressPoint = e.getPoint(); // Acessa 'lastMousePressPoint' do painel
        }
    }

    private class MouseDragHandler extends MouseAdapter {
        private GraphPanel panel; // Referência para o GraphPanel

        public MouseDragHandler(GraphPanel panel) {
            this.panel = panel;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (panel.graph == null || panel.lastMousePressPoint == null) return;
            
            double dx = e.getX() - panel.lastMousePressPoint.getX();
            double dy = e.getY() - panel.lastMousePressPoint.getY();

            panel.offsetX += dx;
            panel.offsetY += dy;

            panel.lastMousePressPoint = e.getPoint();
            panel.repaint();
        }
    }

    private class MouseWheelHandler implements MouseWheelListener {
        private GraphPanel panel; // Referência para o GraphPanel

        public MouseWheelHandler(GraphPanel panel) {
            this.panel = panel;
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (panel.graph == null || panel.graph.getNodes().isEmpty()) {
                return;
            }

            double zoomFactor = 1.1;
            if (e.getWheelRotation() < 0) { // Roda para cima (zoom in)
                panel.scaleX *= zoomFactor;
                panel.scaleY *= zoomFactor;
            } else { // Roda para baixo (zoom out)
                panel.scaleX /= zoomFactor;
                panel.scaleY /= zoomFactor;
            }

            double mouseX = e.getX();
            double mouseY = e.getY();

            // Calcule a escala antiga para determinar o ponto fixo
            double oldScaleX = (e.getWheelRotation() < 0) ? (panel.scaleX / zoomFactor) : (panel.scaleX * zoomFactor);
            double oldScaleY = (e.getWheelRotation() < 0) ? (panel.scaleY / zoomFactor) : (panel.scaleY * zoomFactor);

            double graphXFixedPoint = (mouseX - panel.offsetX) / oldScaleX;
            double graphYFixedPoint = (mouseY - panel.offsetY) / oldScaleY;
            
            panel.offsetX = mouseX - graphXFixedPoint * panel.scaleX;
            panel.offsetY = mouseY - graphYFixedPoint * panel.scaleY;

            panel.repaint();
        }
    }

    // Campo para armazenar a última posição do clique para pan (deve ser um campo do GraphPanel)
    private Point lastMousePressPoint; 
}