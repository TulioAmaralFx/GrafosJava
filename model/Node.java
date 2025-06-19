package model;

public class Node {
    private int idInterno;
    private double x; // Coordenada X para visualização
    private double y; // Coordenada Y para visualização
    private long originalOsmId; // Opcional: para manter o ID original do OSM
    private String label; // Rótulo para exibição

    public Node(int idInterno, double x, double y) {
        this.idInterno = idInterno;
        this.x = x;
        this.y = y;
        this.label = String.valueOf(idInterno);
    }

    // Getters e Setters
    public int getIdInterno() { return idInterno; }
    public double getX() { return x; }
    public double getY() { return y; }
    public void setX(double x) { this.x = x; } // Para transformações
    public void setY(double y) { this.y = y; } // Para transformações
    public long getOriginalOsmId() { return originalOsmId; }
    public void setOriginalOsmId(long originalOsmId) { this.originalOsmId = originalOsmId; } // Adicionado/Verificado
    public String getLabel() { return label; }
}