package ru.manannikov.summerpractice_;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

public class SummerPracticeApplication extends Application {

    private static SummerPracticeApplication application;

    public static HostServices getHostService() {
        return application.getHostServices();
    }

    @Override
    public void start(Stage stage) throws IOException {
        application = this;
        Logger logger = LoggerFactory.getLogger(SummerPracticeApplication.class);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/main-window.fxml"));
        Parent root = loader.load();

        MainWindowController controller = loader.getController();
        controller.setStage(stage);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("styles/style.css").toExternalForm());

        // Настройка главного окна приложения
        stage.setScene(scene);
        stage.setTitle(Util.INIT_WINDOW_TITLE);

        // Настройка главного окна приложения завершена.
        stage.show();
    }

    public static void main(String[] args) {

        Application.launch(args);
    }
}