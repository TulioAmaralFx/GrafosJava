

import gui.NavigationApp;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Garante que a GUI será criada e atualizada na thread de despacho de eventos do Swing
        SwingUtilities.invokeLater(() -> {
            new NavigationApp().setVisible(true);
        });
    }
}