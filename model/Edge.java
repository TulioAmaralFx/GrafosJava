package model;

public class Edge {
    private int u; // Nó de origem (ID interno)
    private int v; // Nó de destino (ID interno)
    private double weight; // Peso (distância)
    private boolean isDirected; // Indica se é mão única
    private String label; // Rótulo para exibição

    public Edge(int u, int v, double weight, boolean isDirected) {
        this.u = u;
        this.v = v;
        this.weight = weight;
        this.isDirected = isDirected;
        this.label = String.format("%.1f", weight);
    }

    // Getters
    public int getU() { return u; }
    public int getV() { return v; }
    public double getWeight() { return weight; }
    public boolean isDirected() { return isDirected; }
    public String getLabel() { return label; }
}