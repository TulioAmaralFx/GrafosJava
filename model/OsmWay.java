package model;

import java.util.ArrayList;
import java.util.List;

public class OsmWay {
    public List<Integer> nodeInternalIds; // IDs internos dos nós que compõem esta via
    public int count; // Número de nós na via

    public OsmWay() {
        this.nodeInternalIds = new ArrayList<>();
        this.count = 0;
    }
}
