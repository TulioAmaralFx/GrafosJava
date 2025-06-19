package model;

public class OsmNode {
    public long idOriginal; // ID original do nó no OSM
    public double lat;      // Latitude do nó
    public double lon;      // Longitude do nó
    public double x;        // Coordenada X transformada para o .poly
    public double y;        // Coordenada Y transformada para o .poly
    public int idInterno;   // ID sequencial (0 a N-1) para o .poly

    public OsmNode(long idOriginal, double lat, double lon, int idInterno) {
        this.idOriginal = idOriginal;
        this.lat = lat;
        this.lon = lon;
        this.idInterno = idInterno;
        // Inicializa x e y com lon e lat brutos; transformação será aplicada depois
        this.x = lon;
        this.y = lat;
    }
}