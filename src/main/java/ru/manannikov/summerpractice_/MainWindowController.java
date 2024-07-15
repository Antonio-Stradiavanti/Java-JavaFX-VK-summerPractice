package ru.manannikov.summerpractice_;

import com.google.gson.JsonParseException;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.SnapshotResult;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

// Я буду передавать короткие имена в виде https://vk.com/senioravanti или @senioravanti;
/*
    Назначение: обрабатывает операции с файлами и vk API и связывает все компоненты приложения.
*/
public class MainWindowController implements Initializable {
    private static final Logger LOG = LoggerFactory.getLogger(MainWindowController.class);

    // Операции с файлами
    private final FileChooser fileDialog = new FileChooser();
    private final FileChooser.ExtensionFilter jsonFiles = new FileChooser.ExtensionFilter("JSON файлы", "*.json");
    private final FileChooser.ExtensionFilter anyFiles = new FileChooser.ExtensionFilter("Файлы любого типа", "*.*");
    private final FileChooser.ExtensionFilter pngFiles = new FileChooser.ExtensionFilter("Изображения формата PNG", "*.png");
    private final FileChooser.ExtensionFilter jpgFiles = new FileChooser.ExtensionFilter("Изображения формата JPG", "*.jpg", "*.jpeg");
    private File curFile = null;
    // Возможно это можно сделать через boolean property
    private boolean isModelChanged = false;

    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    // Сервис
    private final Service<TargetUserModel> friendsPhotosFetcherService = new Service<TargetUserModel>() {
        @Override
        protected Task<TargetUserModel> createTask() {
            return new FriendsPhotosFetcher(
                    appId,
                    serviceToken,
                    screenName.getText().trim().replace("https://vk.com/", "").replace("@", "")
            );
        }
    };
    private TargetUserModel model = null;

    private Integer appId;
    private String serviceToken;
    private boolean onceStarted = false;

    @FXML
    private VBox container;
    // Операции с vk API
    @FXML
    private TextField screenName;
    @FXML
    private Button sendRequest;
    // Операции с файлами
    @FXML
    private Button saveButton;
    @FXML
    private Button serializeButton;
    @FXML
    private Button deserializeButton;
    @FXML
    private Button resetButton;
    // Пункты меню
    @FXML
    private MenuItem deserializeAction;
    @FXML
    private MenuItem saveAction;
    @FXML
    private MenuItem saveAsAction;
    @FXML
    private MenuItem resetAction;
    @FXML
    private MenuItem exportChartBarAction;
    @FXML
    private Menu resentFilesMenu;
    // Основные элементы управления
    @FXML
    private Label title;

    private SplitPane splitPane;
    private SplitPaneController splitPaneController;

    private boolean isSearchBoxVisible = false;
    // Отображаю полученные от API данные

    private void changeSendRequestButtonState(String titleText) {
        onceStarted = false;
        sendRequest.setText("Отправить запрос");
        title.textProperty().unbind();
        title.setText(titleText);
        friendsPhotosFetcherService.reset();
    }

    private void toggleFileOperations(boolean isDisabled) {
        resetButton.setDisable(isDisabled);
        resetAction.setDisable(isDisabled);

        saveButton.setDisable(isDisabled);
        saveAction.setDisable(isDisabled);

        serializeButton.setDisable(isDisabled);
        saveAsAction.setDisable(isDisabled);

        exportChartBarAction.setDisable(isDisabled);
    }

    private void onOpenFileAction(ActionEvent event) {
        fileDialog.setTitle("Загрузить данные из json файла");
        fileDialog.getExtensionFilters().clear();
        fileDialog.getExtensionFilters().addAll(
            jsonFiles, anyFiles
        );
        // Stage -> производный от Window класс
        File file = fileDialog.showOpenDialog(stage);

        if (file == null) return;

        try {
            model = Util.deserializeModel(file).orElseThrow();
            // Загружаем полученные данные
            splitPaneController.populateSplitPane(model);
            toggleFileOperations(false);
            // Нужно обновить заголовок приложения
            curFile = file;
            isModelChanged = false;
            title.setText("Данные успешно десериализованы из файла " + curFile.getName());
            stage.setTitle(
                String.format(
                    "%s | %s",
                    Util.INIT_WINDOW_TITLE,
                    curFile.getName()
                )
            );

        } catch (JsonParseException | IllegalArgumentException | NoSuchElementException error) {
            title.setText(
                "При десериализации структуры данных из json файла произошла ошибка, передан json файл некорректного формата, см. " + error
            );
        } catch (IOException error) {
            title.setText(
                "При чтении из json файла произошла ошибка " + error
            );
        }
    }

    private void onSaveFileAsAction(ActionEvent event) {
        fileDialog.setTitle("Сохранить данные в json файл");
        fileDialog.getExtensionFilters().clear();
        fileDialog.getExtensionFilters().addAll(
                jsonFiles, anyFiles
        );
        File file = fileDialog.showSaveDialog(stage);

        if (file == null) return;

        try {
            Util.serializeModel(model, file);

            curFile = file;
            isModelChanged = false;
            title.setText("Структура данных успешно сериализована в файл " + curFile.getName());
            stage.setTitle(
                String.format(
                    "%s | %s",
                    Util.INIT_WINDOW_TITLE,
                    curFile.getName()
                )
            );

        } catch (IOException error) {
            title.setText("При записи в json файл произошла ошибка " + error);
        }
    }

    private void onSaveFileAction(ActionEvent event) {
        if (!isModelChanged) return;

        if (curFile != null) {
            try {
                Util.serializeModel(model, curFile);

                title.setText(
                    String.format("Файл %s успешно перезаписан", curFile.getName())
                );
                stage.setTitle(
                    String.format(
                        "%s | %s",
                        Util.INIT_WINDOW_TITLE,
                        curFile.getName()
                    )
                );

            } catch (IOException error) {
                title.setText("При записи в json файл произошла ошибка " + error);
            }
        } else {
            onSaveFileAsAction(event);
        }
    }

    private void onResetAction(ActionEvent event) {
        splitPaneController.updateContentVisibility(false);
        toggleFileOperations(true);
        model = null;

        curFile = null;
        isModelChanged = false;
        title.setText("Вернули приложение в исходное состояние");
        Stage stage = (Stage)((Node) event.getSource()).getScene().getWindow();
        stage.setTitle(
            String.format(
                "%s | %s",
                Util.INIT_WINDOW_TITLE,
                Util.INIT_FILE_NAME
            )
        );
    }

    private void onExportBarChartAction(ActionEvent event) {
        fileDialog.setTitle("Экспорт графика в графическом формате");
        fileDialog.getExtensionFilters().clear();
        fileDialog.getExtensionFilters().addAll(
                pngFiles, jpgFiles, anyFiles
        );
        final File file = fileDialog.showSaveDialog(stage);

        if (file == null) return;

        Callback<SnapshotResult, Void> callback = (SnapshotResult res) -> {

            FileChooser.ExtensionFilter selectedExt = fileDialog.getSelectedExtensionFilter();
            String ext = "png";
            if (selectedExt.equals(jpgFiles)) ext = "jpg";

            WritableImage img = res.getImage();
            try {
                Util.exportBarChart(file, ext, img);
                title.setText(
                    "График успешно экспортирован в файл " + file.getName()
                );
            } catch (IOException error) {
                title.setText(
                    String.format("При экспорте графика в файл %s произошла ошибка %s", file.getName(), error)
                );
            }

            return null;
        };

        splitPaneController.takeGlobalHistogramSnapshot(callback);
    }
    // Обработчики нажатия на кнопки, которые не зависят от данных, таких как: загрузить из файла и т.п.
    private void addCommonEventHandlers() {
        // Настройка fileDialog
        fileDialog.setInitialDirectory(new File(System.getProperty("user.home")));
        fileDialog.setInitialFileName(Util.INIT_FILE_NAME);

        resetButton.setOnAction(this::onResetAction);
        resetAction.setOnAction(this::onResetAction);

        serializeButton.setOnAction(this::onSaveFileAsAction);
        saveAsAction.setOnAction(this::onSaveFileAsAction);

        saveButton.setOnAction(this::onSaveFileAction);
        saveAction.setOnAction(this::onSaveFileAction);

        deserializeButton.setOnAction(this::onOpenFileAction);
        deserializeAction.setOnAction(this::onOpenFileAction);

        exportChartBarAction.setOnAction(this::onExportBarChartAction);

        container.setOnKeyPressed(event -> {
            if (event.isControlDown() && event.getCode().equals(KeyCode.F)) {
                // Попробую через булево свойство.
                splitPaneController.updateSearchBoxVisibilityOrChangeFocus(true);
            }
        });
    }

    // обработчики событий связанных с работой с vk API
    private void addVkAPIOperationsEventHandlers() {

        title.setText("Загрузите информацию о фотографиях своих друзей из файла, или отправьте запрос к vk API");

        sendRequest.setOnAction(event -> {
            if (onceStarted) {

                friendsPhotosFetcherService.cancel();
            } else {

                friendsPhotosFetcherService.start();
                sendRequest.setText("Прервать выполнение запроса");
                title.textProperty().bind(friendsPhotosFetcherService.messageProperty());
                onceStarted = true;
            }
        });

        friendsPhotosFetcherService.setOnCancelled(event -> {
            changeSendRequestButtonState("Выполнение задачи прервано.");
        });

        friendsPhotosFetcherService.setOnFailed(event -> {
            changeSendRequestButtonState("При выполнении запроса к vk API произошла ошибка. " + friendsPhotosFetcherService.getException());
        });

        friendsPhotosFetcherService.setOnSucceeded(event -> {
            model = friendsPhotosFetcherService.getValue();

            changeSendRequestButtonState("Задача успешно выполнена.");

            if (model.getFriendProfiles().isEmpty()) {
                title.setText("У ваших друзей отсутствуют фотографии, дальнейшая работа с полученными данными не имеет смысла, попробуйте загрузить данные другого пользователя.");
                // Чтобы сборщик мусора удалил неиспользуемые данные.
                model = null;
                return;
            }

            splitPaneController.populateSplitPane(model);
            toggleFileOperations(false);

            isModelChanged = true;
            stage.setTitle(
                String.format("%s | *%s",
                    Util.INIT_WINDOW_TITLE,
                    (curFile == null ? Util.INIT_FILE_NAME : curFile.getName())
                )
            );
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        try {
            FXMLLoader splitPaneLoader = new FXMLLoader(getClass().getResource("fxml/split-pane.fxml"));
            splitPane = splitPaneLoader.load();
            splitPaneController = splitPaneLoader.getController();
        } catch (IOException error) {
            LOG.error("При сериализации fxml файлов возникла ошибка {}, дальнейшая работа приложения невозможна.", error.toString());
            Platform.exit();
        }

        container.getChildren().add(splitPane);
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        addCommonEventHandlers();

        String resource = "secrets.properties";
        Properties p = new Properties();
        // Оператор try с ресурсами, после выхода из блока try ресурс fin будет автоматически освобожден.
        try (
            var fin = getClass().getResourceAsStream(resource)
        ) {
            p.load(fin);

            appId = Integer.valueOf(p.getProperty("vk.app-id"));
            serviceToken = p.getProperty("vk.service-token");

            if (serviceToken == null) throw new IllegalArgumentException("В файле отсутствуют требуемые с-ва");
            // Обрабатываю результаты выполнения запроса к vk API
            addVkAPIOperationsEventHandlers();

        } catch (Exception e) {

            title.setText("Загрузите информацию о фотографиях своих друзей из файла");
            screenName.setText("При загрузке идентификатора и сервисного ключа приложения возникло исключение " + e + ", дальнейшая работа с vk API невозможна.");
            screenName.setDisable(true);
            sendRequest.setDisable(true);
            // Тогда будет доступна только загрузка из файла
        } finally {
            toggleFileOperations(true);
        }
    }
}