package ru.manannikov.summerpractice_;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.SnapshotResult;
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
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class SplitPaneController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(SplitPaneController.class);

    private static final PseudoClass ERROR_PSEUDO_CLASS = PseudoClass.getPseudoClass("error");
    private final BooleanProperty invalidRegexp = new SimpleBooleanProperty(false);
    private boolean isSearchBoxVisible = false;

    @FXML
    private VBox leftPaneContainer;
    @FXML
    private HBox searchBox;
    @FXML
    private ToggleButton matchCaseButton;
    @FXML
    private Button closeButton;
    @FXML
    private TextField searchField;
    @FXML
    private ListView<FriendModel> listView;
    private final ObservableList<FriendModel> originItems = FXCollections.observableArrayList();

    @FXML
    private HBox rightPaneContainer;
    private VBox globalHistogram;
    private HistogramController globalHistogramController;

    private TabPane friendHistogramAndPhotos;
    private FriendHistogramAndPhotosController friendHistogramAndPhotosController;

    public void updateContentVisibility(boolean isVisible) {
        leftPaneContainer.setVisible(isVisible);
        leftPaneContainer.setManaged(isVisible);

        rightPaneContainer.setVisible(isVisible);
        rightPaneContainer.setManaged(isVisible);
    }

    private void updateRightPaneVisibility(boolean isShouldMakeGlobalHistogramVisible) {
        globalHistogram.setVisible(isShouldMakeGlobalHistogramVisible);
        globalHistogram.setManaged(isShouldMakeGlobalHistogramVisible);

        friendHistogramAndPhotos.setVisible(!isShouldMakeGlobalHistogramVisible);
        friendHistogramAndPhotos.setManaged(!isShouldMakeGlobalHistogramVisible);
    }

    public void takeGlobalHistogramSnapshot(Callback<SnapshotResult, Void> callback) {
        globalHistogram.snapshot(callback, null, null);
    }

    private void filterListView(String stringPattern) {
        ObservableList<FriendModel> foundItems = FXCollections.observableArrayList();

        if (stringPattern.isEmpty()) {
            listView.setItems(originItems);
            return;
        }

        Pattern pattern;
        try {
            if (!matchCaseButton.isSelected())
                pattern = Pattern.compile(stringPattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            else
                pattern = Pattern.compile(stringPattern);
        } catch (PatternSyntaxException error) {
            // Переводим searchField в пользовательское псевдосостояние
            invalidRegexp.set(true);
            searchField.setText("");
            searchField.setPromptText(stringPattern + " --> недопустимое регулярное выражение.");
            listView.requestFocus();
            return;
        }

        Matcher matcher = pattern.matcher("");
        for (FriendModel friend : originItems) {
            matcher.reset(friend.firstName() + " " + friend.lastName());
            if (matcher.find()) foundItems.add(friend);
        }

        listView.setItems(foundItems);
    }

    public void updateSearchBoxVisibilityOrChangeFocus(boolean flag) {
        if (!flag) {
            searchBox.setVisible(false);
            searchBox.setManaged(false);

            isSearchBoxVisible = false;

            listView.requestFocus();
        } else {

            if (!isSearchBoxVisible) {
                searchBox.setVisible(true);
                searchBox.setManaged(true);

                isSearchBoxVisible = true;
            }

            searchField.requestFocus();
        }
    }

    private void addLeftPaneEventHandlers() {
        listView.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            // Нажата клавиша ESC, нужно показать глобальную гистограмму
            if (newValue.intValue() == -1) {
                updateRightPaneVisibility(true);
                // Глобальная гистограмма уже отображена
            } else {
                if (oldValue.intValue() == -1) {
                    // Скрываем глобальную гистограмму
                    updateRightPaneVisibility(false);
                }
                // В противном случае просто обновляю данные в tabPane
                friendHistogramAndPhotosController.populateFriendHistogramAndPhotos(
                        listView.getItems().get(newValue.intValue())
                );
            }
        });

        listView.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().equals(KeyCode.ESCAPE)) {
                listView.getSelectionModel().clearSelection();
            }
        });

        searchField.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER)) {
                filterListView(searchField.getText());
            }
        });

        invalidRegexp.addListener(inv ->
            searchField.pseudoClassStateChanged(ERROR_PSEUDO_CLASS, invalidRegexp.get())
        );



        searchField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue && invalidRegexp.get()) {
                searchField.setText("");
                searchField.setPromptText("Введите регулярное выражение");
                invalidRegexp.set(false);
            }
        });

        closeButton.setOnAction(event -> {
            updateSearchBoxVisibilityOrChangeFocus(false);
            searchField.setText("");
            listView.setItems(originItems);
        });
    }

    private void populateListView(final List<Long> friendIds, final Map<Long, FriendModel> friendProfiles) {
        ObservableList<FriendModel> items = FXCollections.observableArrayList();
        for (Long friendId : friendIds) {
            FriendModel friend = friendProfiles.get(friendId);
            // В дальнейшем буду оперировать только индексами ListView
            if (friend != null) {
                items.add(friend);
            }
        }
        // Перезаписывает
        listView.setItems(items);
        originItems.setAll(items);
    }

    public void populateSplitPane(TargetUserModel model) {
        populateListView(model.getFriendIds(), model.getFriendProfiles());

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
        updateContentVisibility(true);
        updateRightPaneVisibility(true);
        updateSearchBoxVisibilityOrChangeFocus(false);
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
            LOG.error("При сериализации fxml файлов возникла ошибка {}, дальнейшая работа приложения невозможна.", error.toString());
            Platform.exit();
        }

        listView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        listView.setCellFactory(
                new Callback<ListView< FriendModel>, ListCell<FriendModel> >() {
                    @Override
                    public ListCell<FriendModel> call(ListView<FriendModel> param) {
                        return new FriendListCell();
                    }
                }
        );

        rightPaneContainer.getChildren().addAll(globalHistogram, friendHistogramAndPhotos);
        HBox.setHgrow(globalHistogram, Priority.ALWAYS);
        HBox.setHgrow(friendHistogramAndPhotos, Priority.ALWAYS);

        searchField.setPromptText("Введите регулярное выражение");

        addLeftPaneEventHandlers();
        updateContentVisibility(false);

    }
}
