package gui;

import model.Graph;
import model.Node;
import model.Edge;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
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
    
    private boolean showEdgeLabels = true;

    private double scaleX;
    private double scaleY;
    private double offsetX;
    private double offsetY;
    
    private double minXData, maxXData, minYData, maxYData;

    private final int NODE_RADIUS = 6;
    private final int CLICK_TOLERANCE_PX = 10;

    private Point lastMousePressPoint;

    public GraphPanel() {
        setBackground(Color.WHITE);
        
        addMouseWheelListener(new MouseWheelHandler(this));
        addMouseListener(new MousePressHandler(this));
        addMouseMotionListener(new MouseDragHandler(this));

        this.scaleX = 0.0;
        this.scaleY = 0.0;
    }

    // --- Setters e Getters ---
    public void setGraph(Graph graph) {
        this.graph = graph;
        this.scaleX = 0.0;
        this.scaleY = 0.0;
        calculateScalingParameters();
    }

    public void setSelectedOrigin(Integer selectedOrigin) { this.selectedOrigin = selectedOrigin; }
    public void setSelectedDestination(Integer selectedDestination) { this.selectedDestination = selectedDestination; }
    public void setShortestPath(List<Integer> shortestPath) { this.shortestPath = shortestPath; }
    public void setShowEdgeLabels(boolean show) { this.showEdgeLabels = show; }
    
    public double getScaleX() { return scaleX; }
    public double getScaleY() { return scaleY; }
    public double getOffsetX() { return offsetX; }
    public double getOffsetY() { return offsetY; }

    // --- Métodos de Cálculo de Layout ---
    private void calculateInitialBoundingBox() {
        if (graph == null || graph.getNodes().isEmpty()) {
            minXData = 0.0; maxXData = 1.0; minYData = 0.0; maxYData = 1.0;
            return;
        }
        
        minXData = Double.POSITIVE_INFINITY; maxXData = Double.NEGATIVE_INFINITY;
        minYData = Double.POSITIVE_INFINITY; maxYData = Double.NEGATIVE_INFINITY;

        for (Node node : graph.getNodes().values()) {
            if (node.getX() < minXData) minXData = node.getX();
            if (node.getX() > maxXData) maxXData = node.getX();
            if (node.getY() < minYData) minYData = node.getY();
            if (node.getY() > maxYData) maxYData = node.getY();
        }
    }

    private void calculateScalingParameters() {
        calculateInitialBoundingBox();
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        if (panelWidth <= 0 || panelHeight <= 0) { panelWidth = 800; panelHeight = 600; }
        double padding = 20.0;
        double rangeX = maxXData - minXData;
        double rangeY = maxYData - minYData;
        double fitScaleX = (panelWidth - 2 * padding) / (rangeX > 0 ? rangeX : 1.0);
        double fitScaleY = (panelHeight - 2 * padding) / (rangeY > 0 ? rangeY : 1.0);
        this.scaleX = Math.min(fitScaleX, fitScaleY);
        this.scaleY = this.scaleX;
        recalculateOffsets();
    }

    private void recalculateOffsets() {
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        if (panelWidth <= 0 || panelHeight <= 0) { panelWidth = 800; panelHeight = 600; }
        double padding = 20.0;
        double contentWidth = (maxXData - minXData) * scaleX;
        double contentHeight = (maxYData - minYData) * scaleY;
        offsetX = padding + (panelWidth - 2 * padding - contentWidth) / 2.0 - minXData * scaleX;
        offsetY = padding + (panelHeight - 2 * padding - contentHeight) / 2.0 - minYData * scaleY;
    }

    // --- MÉTODO DE DESENHO PRINCIPAL ---
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (graph == null || graph.getNodes().isEmpty()) return;
        if (this.scaleX == 0.0) calculateScalingParameters();

        // --- DEFINIÇÃO DAS CORES ---
        Color undirectedEdgeColor = new Color(170, 170, 170); // Cinza para mão dupla
        Color directedEdgeColor = new Color(100, 100, 255);   // Azul para mão única
        Color pathColor = Color.RED;                          // Vermelho para o caminho
        Color nodeDefaultColor = Color.GRAY;

        // --- Desenhar Arestas ---
        for (Map.Entry<Integer, Map<Integer, Edge>> entryU : graph.getAdj().entrySet()) {
            Node uNode = graph.getNodes().get(entryU.getKey());
            if (uNode == null) continue;

            double x1 = uNode.getX() * scaleX + offsetX;
            double y1 = uNode.getY() * scaleY + offsetY;

            for (Map.Entry<Integer, Edge> entryV : entryU.getValue().entrySet()) {
                Edge edge = entryV.getValue();
                Node vNode = graph.getNodes().get(edge.getV());
                if (vNode == null || (!edge.isDirected() && uNode.getIdInterno() > vNode.getIdInterno())) continue;

                double x2 = vNode.getX() * scaleX + offsetX;
                double y2 = vNode.getY() * scaleY + offsetY;

                Color lineColor;
                int lineWidth = 1;
                
                // --- LÓGICA DE SELEÇÃO DE COR DA ARESTA ATUALIZADA ---
                if (shortestPath != null && isEdgeInPath(uNode.getIdInterno(), vNode.getIdInterno())) {
                    lineColor = pathColor;
                    lineWidth = 2;
                } else if (edge.isDirected()) {
                    lineColor = directedEdgeColor; // Cor para mão única
                } else {
                    lineColor = undirectedEdgeColor; // Cor para mão dupla
                }
                
                g2d.setColor(lineColor);
                g2d.setStroke(new BasicStroke(lineWidth));
                g2d.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
                
                if (this.showEdgeLabels) {
                    g2d.setColor(Color.BLACK);
                    g2d.setFont(new Font("Arial", Font.PLAIN, 10));
                    int midX = (int) ((x1 + x2) / 2);
                    int midY = (int) ((y1 + y2) / 2);
                    g2d.drawString(edge.getLabel(), midX, midY);
                }

                if (edge.isDirected()) {
                    drawArrow(g2d, x1, y1, x2, y2, lineColor);
                }
            }
        }

        // --- Desenhar Nós ---
        for (Node node : graph.getNodes().values()) {
            double x = node.getX() * scaleX + offsetX;
            double y = node.getY() * scaleY + offsetY;
            Color nodeColor = nodeDefaultColor;
            Color outlineColor = Color.DARK_GRAY;

            if (selectedOrigin != null && node.getIdInterno() == selectedOrigin) {
                nodeColor = Color.BLUE;
            } else if (selectedDestination != null && node.getIdInterno() == selectedDestination) {
                nodeColor = Color.ORANGE;
            } else if (shortestPath != null && shortestPath.contains(node.getIdInterno())) {
                nodeColor = pathColor;
            }

            g2d.setColor(nodeColor);
            g2d.fillOval((int) (x - NODE_RADIUS), (int) (y - NODE_RADIUS), NODE_RADIUS * 2, NODE_RADIUS * 2);
            g2d.setColor(outlineColor);
            g2d.drawOval((int) (x - NODE_RADIUS), (int) (y - NODE_RADIUS), NODE_RADIUS * 2, NODE_RADIUS * 2);
        }
    }

    private boolean isEdgeInPath(int uId, int vId) {
        if (shortestPath == null || shortestPath.size() < 2) return false;
        for (int i = 0; i < shortestPath.size() - 1; i++) {
            int pathU = shortestPath.get(i);
            int pathV = shortestPath.get(i + 1);
            if ((pathU == uId && pathV == vId) || (pathU == vId && pathV == uId)) {
                return true;
            }
        }
        return false;
    }

    private void drawArrow(Graphics2D g2, double x1, double y1, double x2, double y2, Color color) {
        g2.setColor(color);
        int ARR_SIZE = 8;
        double dx = x2 - x1, dy = y2 - y1;
        double angle = Math.atan2(dy, dx);
        double distance = Math.sqrt(dx*dx + dy*dy);
        double ratio = (distance > NODE_RADIUS) ? (distance - NODE_RADIUS) / distance : 0;
        double arrowX = x1 + dx * ratio;
        double arrowY = y1 + dy * ratio;
        AffineTransform oldTransform = g2.getTransform();
        g2.translate(arrowX, arrowY);
        g2.rotate(angle);
        Path2D.Double arrowHead = new Path2D.Double();
        arrowHead.moveTo(0, 0);
        arrowHead.lineTo(-ARR_SIZE, ARR_SIZE / 2.0);
        arrowHead.lineTo(-ARR_SIZE, -ARR_SIZE / 2.0);
        arrowHead.closePath();
        g2.fill(arrowHead);
        g2.setTransform(oldTransform);
    }
    
    public Integer getNearestNodeIdFromClick(int clickX, int clickY) {
        if (graph == null) return null;
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
    
    // --- Handlers de Eventos Internos ---
    private static class MousePressHandler extends MouseAdapter {
        private final GraphPanel panel;
        public MousePressHandler(GraphPanel panel) { this.panel = panel; }
        @Override
        public void mousePressed(MouseEvent e) { panel.lastMousePressPoint = e.getPoint(); }
    }

    private static class MouseDragHandler extends MouseAdapter {
        private final GraphPanel panel;
        public MouseDragHandler(GraphPanel panel) { this.panel = panel; }
        @Override
        public void mouseDragged(MouseEvent e) {
            if (panel.graph == null || panel.lastMousePressPoint == null) return;
            panel.offsetX += e.getX() - panel.lastMousePressPoint.getX();
            panel.offsetY += e.getY() - panel.lastMousePressPoint.getY();
            panel.lastMousePressPoint = e.getPoint();
            panel.repaint();
        }
    }

    private static class MouseWheelHandler implements MouseWheelListener {
        private final GraphPanel panel;
        public MouseWheelHandler(GraphPanel panel) { this.panel = panel; }
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (panel.graph == null) return;
            double zoomFactor = 1.1;
            double currentScaleX = panel.scaleX;
            if (e.getWheelRotation() < 0) {
                panel.scaleX *= zoomFactor;
                panel.scaleY *= zoomFactor;
            } else {
                panel.scaleX /= zoomFactor;
                panel.scaleY /= zoomFactor;
            }
            double graphXFixedPoint = (e.getX() - panel.offsetX) / currentScaleX;
            double graphYFixedPoint = (e.getY() - panel.offsetY) / currentScaleX;
            panel.offsetX = e.getX() - graphXFixedPoint * panel.scaleX;
            panel.offsetY = e.getY() - graphYFixedPoint * panel.scaleY;
            panel.repaint();
        }
    }
}