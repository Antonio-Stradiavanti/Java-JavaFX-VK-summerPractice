package ru.manannikov.summerpractice_;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class FriendHistogramAndPhotosController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(FriendHistogramAndPhotosController.class);

    @FXML
    private AnchorPane histogramPane;
    @FXML
    private FlowPane imagesPane;

    private VBox localChart;
    private HistogramController histogramController;

    private Long friendId;

    private VBox createImageCart(PhotoModel photoModel) {
        VBox cardBox = new VBox();

        final Label photoDescription = new Label("Фотография была опубликована " +
                Mapper.mapLocalDateTimeToString(photoModel.publicationDate())
        );
        photoDescription.setWrapText(true);
        photoDescription.setMaxWidth(Util.PHOTO_SIZE);

        Image img = photoModel.photo();
        ImageView imageView = new ImageView(img);

        Rectangle2D viewport;
        double w = img.getWidth(), h = img.getHeight();
        if (h > w) {
            viewport = new Rectangle2D(0.0, (img.getHeight() - Util.PHOTO_SIZE) / 2.5, Util.PHOTO_SIZE, Util.PHOTO_SIZE);
        } else {
            viewport = new Rectangle2D((img.getWidth() - Util.PHOTO_SIZE) / 2.0, 0.0, Util.PHOTO_SIZE, Util.PHOTO_SIZE);
        }
        imageView.setViewport(viewport);

//        LOG.info("Размер фотки w = {} ; h = {}", img.getWidth(), img.getHeight());

        imageView.setOnMouseClicked(event -> {
            Long photoId = photoModel.photoId();
            SummerPracticeApplication.getHostService().showDocument(
                String.format("https://vk.com/album%1$d_00?z=photo%1$d_%2$d%%2Falbum%1$d_00", friendId, photoId)
            );
        });

        cardBox.getChildren().addAll(imageView, photoDescription);

        cardBox.setAlignment(Pos.TOP_CENTER);

        cardBox.getStyleClass().add("vbox");
        return cardBox;
    }

    public void populateFriendHistogramAndPhotos(final FriendModel friend) {
        // Сформируем гистограмму
        // Вызываем метод localChartController
        this.friendId = friend.friendId();

        histogramController.populateHistogram(
                friend.wallPhotosNumberByYearMonthMap(),

                friend.lastName() + " " + friend.firstName() + " опубликовал больше всего фотографий " + Mapper.mapMaxPhotosByYearMonthToString(friend.maxPhotosNumberByYearMonth()),

                "Статистика опубликованных пользователем " + friend.firstName() + " " + friend.lastName() + " по годам и месяцам."
                );
        // Сформируем перечень фоток
        imagesPane.getChildren().clear();
        for (PhotoModel wallPhoto : friend.wallPhotos()) {
            VBox imageCart = createImageCart(wallPhoto);
            imagesPane.getChildren().add(imageCart);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        try {
            FXMLLoader localChartLoader = new FXMLLoader(getClass().getResource("fxml/histogram.fxml"));
            localChart = localChartLoader.load();
            histogramController = localChartLoader.getController();

            histogramPane.getChildren().add(localChart);
            AnchorPane.setBottomAnchor(localChart, 0.0);
            AnchorPane.setTopAnchor(localChart, 0.0);
            AnchorPane.setLeftAnchor(localChart, 0.0);
            AnchorPane.setRightAnchor(localChart, 0.0);

        } catch (IOException error) {
            LOG.error("При сериализации fxml файла произошла ошибка, см.\n{}", error.toString());
        }
    }
}
