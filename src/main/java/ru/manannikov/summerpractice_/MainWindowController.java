package ru.manannikov.summerpractice_;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.time.YearMonth;
import java.util.*;

// Я буду передавать короткие имена в виде https://vk.com/senioravanti или @senioravanti;
public class MainWindowController implements Initializable {
    private static final Logger LOG = LoggerFactory.getLogger(MainWindowController.class);

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

    private VBox globalHistogram;
    private HistogramController globalHistogramController;

    private TabPane friendHistogramAndPhotos;
    private FriendHistogramAndPhotosController friendHistogramAndPhotosController;

    @FXML
    private TextField screenName;
    @FXML
    private Button sendRequest;
    @FXML
    private Button saveButton;
    @FXML
    private Button serializeButton;
    @FXML
    private Button deserialiseButton;
    @FXML
    private Button resetButton;
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

        leftPane.setItems(items);
    }

    private void updateRightPane(boolean isShouldMakeGlobalHistogramVisible) {
        globalHistogram.setVisible(isShouldMakeGlobalHistogramVisible);
        globalHistogram.setManaged(isShouldMakeGlobalHistogramVisible);

        friendHistogramAndPhotos.setVisible(!isShouldMakeGlobalHistogramVisible);
        friendHistogramAndPhotos.setManaged(!isShouldMakeGlobalHistogramVisible);
    }

    // Отображаю полученные от API данные
    private void loadFetchedData() throws Exception {

        if (model.getFriendProfiles().isEmpty()) {
            throw new Exception("Количество друзей: " + model.getFriendIds().size() + "; количество полученных профилей: " + model.getFriendProfiles());
        }

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
//                "У пользователя " + model.getFirstName() + " " + model.getLastName() + " всего друзей " + model.getFriendIds().size() + " у " + model.getFriendProfiles().size() + " из них открытые профили и есть опубликованные фотографии. Ваши друзья опубликовали больше всего фотографий " + Mapper.mapMaxPhotosByYearMonthToString(Util.flattenPhotosNumberByYearMonthMapToMax(maxFriendsPhotosNumberByYearMonth)),

                "Общая статистика опубликованных вашими друзьями фотографий по годам и месяцам"
        );
        // Показываем глобальную гистограмму
        updateRightPane(true);

    }

    private void changeSendRequestButtonState(String titleText) {
        onceStarted = false;
        sendRequest.setText("Отправить запрос");
        title.textProperty().unbind();
        title.setText(titleText);
        friendsPhotosFetcherService.reset();
    }

    private void selectionChanged(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {

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
    // Обработчики нажатия на кнопки, которые не зависят от данных, таких как: загрузить из файла и т.п.
    private void addCommonEventHandlers() {

    }

    // обработчики событий связанных с работой с vk API
    private void addVkEventHandlers() {

        title.setText("Загрузите информацию о фотографиях своих друзей из файла, или отправьте запрос к vk API");

        sendRequest.setOnAction(e -> {
            if (onceStarted) {

                friendsPhotosFetcherService.cancel();
            } else {

                friendsPhotosFetcherService.start();
                sendRequest.setText("Прервать выполнение запроса");
                title.textProperty().bind(friendsPhotosFetcherService.messageProperty());
                onceStarted = true;
            }
        });

        friendsPhotosFetcherService.setOnCancelled(e -> {
            model = null;
            changeSendRequestButtonState("Выполнение задачи прервано.");
        });

        friendsPhotosFetcherService.setOnFailed(event -> {
            model = null;
            changeSendRequestButtonState("При выполнении запроса к vk API возникло исключение. " + friendsPhotosFetcherService.getException());
        });

        friendsPhotosFetcherService.setOnSucceeded(event -> {
            TargetUserModel oldModelValue = model;
            model = friendsPhotosFetcherService.getValue();

            changeSendRequestButtonState("Задача успешно выполнена.");

            try {
                loadFetchedData();
                if (oldModelValue == null) addSplitPaneEventHandlers();
            } catch (Exception error) {
                model = null;
                LOG.error("При обработке полученных от vk API данных произошла ошибка, см. " + error.toString());
                title.setText("У ваших друзей отсутствуют фотографии, дальнейшая работа с полученными данными не имеет смысла, попробуйте загрузить данные другого пользователя.");
            }
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

        globalHistogram.setVisible(false);
        globalHistogram.setManaged(false);

        friendHistogramAndPhotos.setVisible(false);
        friendHistogramAndPhotos.setManaged(false);

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
            addVkEventHandlers();

        } catch (Exception e) {
            title.setText("Загрузите информацию о фотографиях своих друзей из файла");
            screenName.setText("При загрузке идентификатора и сервисного ключа приложения возникло исключение " + e + ", дальнейшая работа с vk API невозможна.");
            screenName.setDisable(true);
            sendRequest.setDisable(true);
            // Тогда будет доступна только загрузка из файла
        } finally {
            saveButton.setDisable(true);
            serializeButton.setDisable(true);
            resetButton.setDisable(true);
        }
    }
}