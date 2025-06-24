package model;

public class Node {
    private int idInterno;
    private double x;
    private double y;
    private String label;

    public Node(int idInterno, double x, double y) {
        this.idInterno = idInterno;
        this.x = x;
        this.y = y;
        // O construtor provavelmente já define um rótulo padrão
        this.label = String.valueOf(idInterno);
    }

    // --- GETTERS (já devem existir) ---
    public int getIdInterno() { return idInterno; }
    public double getX() { return x; }
    public double getY() { return y; }
    public String getLabel() { return label; }

    // --- SETTERS (setX e setY já devem existir) ---
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    
    // --- MÉTODO ADICIONADO (A CORREÇÃO) ---
    public void setLabel(String label) {
        this.label = label;
    }
}