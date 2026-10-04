package io.github.ctgnz.fxtivity.example;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import io.github.ctgnz.fxtivity.Effectivity;
import io.github.ctgnz.fxtivity.employment.Register;

/**
 * The example application: Acme's story, or any register saved from it, seen on one effective date at a time.
 * <p>
 * Run it with {@code mvn -pl fxtivity-example -am javafx:run} from the root of the repository. It opens with the story built in; File opens and saves registers as YAML.
 */
public final class ExampleApp extends Application {

    private final BorderPane window = new BorderPane();
    private final CheckMenuItem editing = new CheckMenuItem("Edit mode");
    private Stage stage;
    private ExampleView view;
    private Path file;

    @Override
    public void start(Stage primaryStage) throws IOException {
        this.stage = primaryStage;
        Effectivity.forDates(AcmeStory.FIRST.plusYears(10), AcmeStory.FIRST, AcmeStory.AFTER);
        window.setTop(menus());
        try (InputStream story = ExampleApp.class.getResourceAsStream("acme.yml")) {
            show(Yaml.read(story), null);
        }
        primaryStage.setScene(new Scene(window, 1000, 700));
        primaryStage.show();
    }

    private MenuBar menus() {
        MenuItem open = new MenuItem("Open...");
        open.setOnAction(evt -> open());
        MenuItem save = new MenuItem("Save");
        save.setOnAction(evt -> save(file));
        MenuItem saveAs = new MenuItem("Save as...");
        saveAs.setOnAction(evt -> save(null));
        MenuItem exit = new MenuItem("Exit");
        exit.setOnAction(evt -> Platform.exit());
        editing.setOnAction(evt -> view.setEditing(editing.isSelected()));
        return new MenuBar(new Menu("File", null, open, save, saveAs, exit), new Menu("View", null, editing));
    }

    private void show(Register register, Path from) {
        this.file = from;
        this.view = new ExampleView(register);
        view.setEditing(editing.isSelected());
        window.setCenter(view);
        stage.setTitle("fxtivity example - " + (from == null ? "Acme's story" : from.getFileName()));
    }

    private void open() {
        File chosen = chooser().showOpenDialog(stage);
        if (chosen != null) {
            try {
                show(Yaml.read(chosen.toPath()), chosen.toPath());
            } catch (IOException e) {
                // A register that breaks the rules fails to load, and the message names the entry - the reason loading fails rather than leaving entries out.
                new Alert(Alert.AlertType.ERROR, "Could not open " + chosen.getName() + ":\n" + e.getMessage()).showAndWait();
            }
        }
    }

    private void save(Path to) {
        Path target = to;
        if (target == null) {
            File chosen = chooser().showSaveDialog(stage);
            if (chosen == null) {
                return;
            }
            target = chosen.toPath();
        }
        try {
            Yaml.write(view.register(), target);
            this.file = target;
            stage.setTitle("fxtivity example - " + target.getFileName());
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Could not save " + target.getFileName() + ":\n" + e.getMessage()).showAndWait();
        }
    }

    private static FileChooser chooser() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Registers", "*.yml", "*.yaml"));
        return chooser;
    }

    /**
     * Starts the application.
     *
     * @param args
     *            ignored
     */
    public static void main(String[] args) {
        launch(args);
    }

}
