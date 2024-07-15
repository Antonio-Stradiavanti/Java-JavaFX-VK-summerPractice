package ru.manannikov.summerpractice_;

import com.google.gson.JsonParseException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.scene.layout.HBox;
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
import java.time.YearMonth;
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
    // fxml
    private VBox globalHistogram;
    private HistogramController globalHistogramController;

    private TabPane friendHistogramAndPhotos;
    private FriendHistogramAndPhotosController friendHistogramAndPhotosController;

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
    @FXML
    private ToggleButton matchCaseButton;
    @FXML
    private TextField searchField;
    @FXML
    private HBox searchBox;
    @FXML
    private VBox leftPaneContainer;
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
    @FXML
    private ListView<FriendModel> leftPane;
    @FXML
    private HBox rightPane;

    private void populateListView() {
        ObservableList<FriendModel> items = FXCollections.observableArrayList();
        for (Long friendId : model.getFriendIds()) {
            FriendModel friend = model.getFriendProfiles().get(friendId);
            // В дальнейшем буду оперировать только индексами ListView
            if (friend != null) {
                items.add(friend);
            }
        }
        // Перезаписывает
        leftPane.setItems(items);
    }

    private void updateRightPane(boolean isShouldMakeGlobalHistogramVisible) {
        globalHistogram.setVisible(isShouldMakeGlobalHistogramVisible);
        globalHistogram.setManaged(isShouldMakeGlobalHistogramVisible);

        friendHistogramAndPhotos.setVisible(!isShouldMakeGlobalHistogramVisible);
        friendHistogramAndPhotos.setManaged(!isShouldMakeGlobalHistogramVisible);
    }

    // Отображаю полученные от API данные
    private void loadFetchedData() {

        populateListView();

        Map<YearMonth, Integer> maxFriendsPhotosNumberByYearMonth = Util.findMaxPhotosByYearMonthMap(model.getFriendProfiles());
        globalHistogramController.populateHistogram(
                maxFriendsPhotosNumberByYearMonth,

                String.format("У пользователя %s %s всего друзей %d, у %d из них открытые профили и они опубликовали хотя бы одну фотографию. Ваши друзья опубликовали больше всего фотографий %s",
                        model.getFirstName(),
                        model.getLastName(),
                        model.getFriendIds().size(),
                        model.getFriendProfiles().size(),
                        Mapper.mapMaxPhotosByYearMonthToString(Util.flattenPhotosNumberByYearMonthMapToMax(maxFriendsPhotosNumberByYearMonth))
                ),
                "Общая статистика опубликованных вашими друзьями фотографий по годам и месяцам"
        );
        // Показываем глобальную гистограмму
        leftPaneContainer.setVisible(true);
        leftPaneContainer.setManaged(true);
        updateRightPane(true);
        toggleFileOperations(false);
    }

    private void changeSendRequestButtonState(String titleText) {
        onceStarted = false;
        sendRequest.setText("Отправить запрос");
        title.textProperty().unbind();
        title.setText(titleText);
        friendsPhotosFetcherService.reset();
    }
    // Следует вызывать только после того как модель была сериализована из файла или получена от vk API, то есть надо гарантировать, что model != null.
    private void addSplitPaneEventHandlers() {

        leftPane.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            // Нажата клавиша ESC, нужно показать глобальную гистограмму
            if (newValue.intValue() == -1) {
                updateRightPane(true);
            // Глобальная гистограмма уже отображена
            } else {
                if (oldValue.intValue() == -1) {
                    // Скрываем глобальную гистограмму
                    updateRightPane(false);
                }
                // В противном случае просто обновляю данные в tabPane
                friendHistogramAndPhotosController.populateFriendHistogramAndPhotos(
                        leftPane.getItems().get(newValue.intValue())
                );
            }
        });

        leftPane.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().equals(KeyCode.ESCAPE)) {
                leftPane.getSelectionModel().clearSelection();
            }
        });
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

    private void hideSplitPaneContent() {

        leftPaneContainer.setVisible(false);
        leftPaneContainer.setManaged(false);

        globalHistogram.setVisible(false);
        globalHistogram.setManaged(false);

        friendHistogramAndPhotos.setVisible(false);
        friendHistogramAndPhotos.setManaged(false);
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
            loadFetchedData();
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

    private void onSaveAction(ActionEvent event) {
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
        hideSplitPaneContent();
        toggleFileOperations(true);

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

        globalHistogram.snapshot(callback, null, null);
    }
    // Обработчики нажатия на кнопки, которые не зависят от данных, таких как: загрузить из файла и т.п.
    private void addFileOperationsEventHandlers() {
        // Настройка fileDialog
        fileDialog.setInitialDirectory(new File(System.getProperty("user.home")));
        fileDialog.setInitialFileName(Util.INIT_FILE_NAME);

        resetButton.setOnAction(this::onResetAction);
        resetAction.setOnAction(this::onResetAction);

        serializeButton.setOnAction(this::onSaveFileAsAction);
        saveAsAction.setOnAction(this::onSaveFileAsAction);

        saveButton.setOnAction(this::onSaveAction);
        saveAction.setOnAction(this::onSaveAction);

        deserializeButton.setOnAction(this::onOpenFileAction);
        deserializeAction.setOnAction(this::onOpenFileAction);

        exportChartBarAction.setOnAction(this::onExportBarChartAction);
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
            model = null;
            changeSendRequestButtonState("Выполнение задачи прервано.");
        });

        friendsPhotosFetcherService.setOnFailed(event -> {
            model = null;
            changeSendRequestButtonState("При выполнении запроса к vk API произошла ошибка. " + friendsPhotosFetcherService.getException());
        });

        friendsPhotosFetcherService.setOnSucceeded(event -> {
            model = friendsPhotosFetcherService.getValue();

            changeSendRequestButtonState("Задача успешно выполнена.");

            if (model.getFriendProfiles().isEmpty()) {
                title.setText("У ваших друзей отсутствуют фотографии, дальнейшая работа с полученными данными не имеет смысла, попробуйте загрузить данные другого пользователя.");
                hideSplitPaneContent();
                toggleFileOperations(true);
                return;
            }

            loadFetchedData();
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
            FXMLLoader globalHistogramLoader = new FXMLLoader(getClass().getResource("fxml/histogram.fxml"));
            globalHistogram = globalHistogramLoader.load();
            globalHistogramController = globalHistogramLoader.getController();

            FXMLLoader friendHistogramAndPhotosLoader = new FXMLLoader(getClass().getResource("fxml/friend-histogram-and-photos.fxml"));
            friendHistogramAndPhotos = friendHistogramAndPhotosLoader.load();
            friendHistogramAndPhotosController = friendHistogramAndPhotosLoader.getController();

        } catch (IOException error) {
            LOG.error("При сериализации fxml файлов возникла ошибка, дальнейшая работа приложения невозможна, см.\n{}", error.toString());
            return;
        }

        leftPane.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        leftPane.setCellFactory(
                new Callback<ListView< FriendModel>, ListCell<FriendModel> >() {
                    @Override
                    public ListCell<FriendModel> call(ListView<FriendModel> param) {
                        return new FriendListCell();
                    }
                }
        );

        rightPane.getChildren().addAll(globalHistogram, friendHistogramAndPhotos);

        rightPane.setFillHeight(true);
        HBox.setHgrow(globalHistogram, Priority.ALWAYS);
        HBox.setHgrow(friendHistogramAndPhotos, Priority.ALWAYS);

        hideSplitPaneContent();
        addSplitPaneEventHandlers();
        addFileOperationsEventHandlers();

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