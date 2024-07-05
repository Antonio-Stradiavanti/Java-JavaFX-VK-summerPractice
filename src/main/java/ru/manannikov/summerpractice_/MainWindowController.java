package ru.manannikov.summerpractice_;

import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.*;

public class MainWindowController implements Initializable {
    private static final Logger LOG = LoggerFactory.getLogger(MainWindowController.class);

    private FriendsPhotosFetcher friendsPhotosFetcher;

    @FXML
    public TextField shortName;

    @FXML
    public Button sendRequest;

    @FXML
    public Button saveButton;

    @FXML
    public Button serializeButton;

    @FXML
    public Button deserialiseButton;

    @FXML
    public Button resetButton;

    @FXML
    public Label title;

    private void startTask() {
        // Я буду передавать короткие имена в виде https://vk.com/senioravanti или @senioravanti;
        // Этот префикс надо срезать перед передачей в метод.
        friendsPhotosFetcher.setName(
                shortName.getText().trim().replace("https://vk.com/", "").replace("@", "")
        );
        Thread backgroundThread = new Thread(friendsPhotosFetcher);
        backgroundThread.setDaemon(true);
        backgroundThread.start();
    }

    private void changeSendRequestButtonState(boolean isRunning) {
        if (!isRunning) {
            sendRequest.setText("Отправить запрос");
            sendRequest.setOnMouseClicked(e -> startTask());
        } else {
            sendRequest.setText("Прервать выполнение запроса");
            sendRequest.setOnMouseClicked(e -> friendsPhotosFetcher.cancel());
        }
    }

    private void allowRequestToVkApi() {
        sendRequest.setOnMouseClicked(e -> startTask());

        title.textProperty().bind(friendsPhotosFetcher.messageProperty());

        friendsPhotosFetcher.setOnRunning(event -> {
            changeSendRequestButtonState(true);
        });

        friendsPhotosFetcher.setOnCancelled(event -> {
            LOG.info("Выполнение задачи прервано пользователем");
            changeSendRequestButtonState(false);
        });

        friendsPhotosFetcher.setOnFailed(event -> {
            LOG.info("В ходе выполнения задачи возникла ошибка");
            changeSendRequestButtonState(false);
        });

        friendsPhotosFetcher.setOnSucceeded(event -> {
            LOG.info("Задача успешно выполнена");
            changeSendRequestButtonState(false);
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        String resource = "secrets.properties";
        Properties p = new Properties();
        // Оператор try с ресурсами, после выхода из блока try ресурс fin будет автоматически освобожден.
        try (
                var fin = getClass().getResourceAsStream(resource)
        ) {
            p.load(fin);

            String appId = p.getProperty("vk.app-id");
            String serviceToken = p.getProperty("vk.service-token");

            if (appId == null || serviceToken == null) throw new IllegalArgumentException("В файле отсутствуют требуемые с-ва");

            friendsPhotosFetcher = new FriendsPhotosFetcher(Integer.valueOf(appId), serviceToken);
            allowRequestToVkApi();

        } catch (Exception e) {
            shortName.setText("При загрузке идентификатора и сервисного ключа приложения возникло исключение " + e + ", дальнейшая работа с vk API невозможна.");
            shortName.setDisable(true);
            sendRequest.setDisable(true);
            // Тогда будет доступна только загрузка из файла
        } finally {
            saveButton.setDisable(true);
            serializeButton.setDisable(true);
            resetButton.setDisable(true);
        }
    }
}