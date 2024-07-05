package ru.manannikov.summerpractice_;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

public class SummerPracticeApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        Logger logger = LoggerFactory.getLogger(SummerPracticeApplication.class);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/main-window.fxml"));
        MainWindowController controller = loader.getController();

        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("styles/style.css").toExternalForm());

        // Настройка главного окна приложения
        stage.setScene(scene);
        stage.setTitle("ЛГТУ АСУ Летняя практика | Мананников А. О. ПИ-22-2");

        // Настройка главного окна приложения завершена.
        stage.show();
    }

    public static void main(String[] args) {

        Application.launch(args);
    }
}