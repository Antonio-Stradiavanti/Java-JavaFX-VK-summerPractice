package ru.manannikov.summerpractice_;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;

// Я буду передавать короткие имена в виде https://vk.com/senioravanti или @senioravanti;
public class MainWindowController implements Initializable {
    private static final Logger LOG = LoggerFactory.getLogger(MainWindowController.class);

    private Service<FetchedDataModel> friendsPhotosFetcherService = new Service<FetchedDataModel>() {
        @Override
        protected Task<FetchedDataModel> createTask() {
            return new FriendsPhotosFetcher(
                    appId,
                    serviceToken,
                    screenName.getText().trim().replace("https://vk.com/", "").replace("@", "")
            );
        }
    };
    private FetchedDataModel model = null;

    private Integer appId;
    private String serviceToken;
    private boolean onceStarted = false;

    @FXML
    public TextField screenName;

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

    @FXML
    public ListView leftPane;

    @FXML
    public AnchorPane rightPane;

    private VBox createBarChartContainer(String chartBarTitle) {
        // Создаю график
        /* TODO

        - [ ] Надо сделать так чтобы он полностью вписывался в anchorPane, то есть ужимался когда следует и т.п.
            - [ ] Узнать больше про anchorPane

        - [ ] Перед вызовом этого метода надо убедиться, что фотки у друзей вообще есть ))

        */
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Год/Месяц");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Количество фотографий");

        XYChart.Series<String, Number> seria = new XYChart.Series<>();
        // Сформировать данные, вылетает null pointer exception.
        // Сначала сформируем Map, а потом ее преобразуем в XYChart.Data.
        Map<YearMonth, Integer> maxFriendsPhotosNumberByYearMonth = model.findMaxFriendsPhotosNumberByYearMonth();
        if (maxFriendsPhotosNumberByYearMonth.isEmpty()) throw new NoSuchElementException();

        seria.getData().setAll(
                Mapper.mapYearMonthIntegerMapToChartData(
                        maxFriendsPhotosNumberByYearMonth
                )
        );

        ObservableList< XYChart.Series<String, Number> > data = FXCollections.observableArrayList();
        data.add(seria);

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setData(data);

        barChart.setTitle(chartBarTitle);
        barChart.setMaxWidth(Double.MAX_VALUE);
        barChart.setMaxHeight(Double.MAX_VALUE);
        barChart.setLegendVisible(false);

        // Создаю Описание графика
        Label description = new Label();
        description.getStyleClass().add("chart-description");

        Map.Entry<YearMonth, Integer> maxPhotosNumber = maxFriendsPhotosNumberByYearMonth.entrySet().stream().max(
                Map.Entry.comparingByValue()
        ).orElseThrow();
        String month = maxPhotosNumber.getKey().getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("ru"));
        description.setText("У пользователя " + model.getUser().getFirstName() + " " + model.getUser().getLastName() + " -> всего друзей n = " + model.getFriendIds().size() + ", друзья опубликовали больше всего фотографий c = " + maxPhotosNumber.getValue() + " в " + month.substring(0, month.length() - 1).concat("е") + " месяце " + maxPhotosNumber.getKey().getYear() + " го года.");
        // Создаю контейнер
        VBox barChartContainer = new VBox(12.0, barChart, description);
        barChartContainer.setFillWidth(true);
        barChartContainer.setAlignment(Pos.CENTER);

        barChartContainer.getStyleClass().add("bar-chart-container");

        VBox.setVgrow(barChart, Priority.ALWAYS);

//        LOG.info(seria.getData().toString());
        return barChartContainer;
    }

    // Отображаю полученные от API данные
    private void loadFetchedData() {
        try {
            var barChartContainer = createBarChartContainer("Общее количество опубликованных вашими друзьями фотографий по годам и месяцам");

            AnchorPane.setBottomAnchor(barChartContainer, 0.0);
            AnchorPane.setTopAnchor(barChartContainer, 0.0);
            AnchorPane.setLeftAnchor(barChartContainer, 0.0);
            AnchorPane.setRightAnchor(barChartContainer, 0.0);

            rightPane.getChildren().add(barChartContainer);

        } catch (NoSuchElementException e) {
            title.setText("У ваших друзей отсутствуют фотографии, дальнейшая работа с полученными данными не имеет смысла, попробуйте загрузить данные другого пользователя.");
        }
    }

    private void changeSendRequestButtonState(String titleText) {
        onceStarted = false;
        sendRequest.setText("Отправить запрос");
        title.textProperty().unbind();
        title.setText(titleText);
        friendsPhotosFetcherService.reset();
    }

    private void allowRequestToVkApi() {

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
            model = friendsPhotosFetcherService.getValue();
            changeSendRequestButtonState("Задача успешно выполнена.");
            loadFetchedData();
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

            appId = Integer.valueOf(p.getProperty("vk.app-id"));
            serviceToken = p.getProperty("vk.service-token");

            if (serviceToken == null) throw new IllegalArgumentException("В файле отсутствуют требуемые с-ва");

            // Обрабатываю результаты выполнения запроса к vk API
            allowRequestToVkApi();

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