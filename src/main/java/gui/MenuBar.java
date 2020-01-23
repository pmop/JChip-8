package gui;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

public final class MenuBar extends javafx.scene.control.MenuBar {

    private static Menu file;
    private static MenuItem load;
    private static MenuItem exit;
    static {
        file = new Menu("File");
        load = new MenuItem("Load game");
        exit = new MenuItem("Exit");
        file.getItems().addAll(load,exit);
    }

    public MenuBar() {
        this.getMenus().add(file);
    }
}
