package ru.manannikov.summerpractice_;

import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;

public class FriendListCell extends ListCell<FriendModel> {

    private static final String vkLink = "https://vk.com/";

    private ImageView friendIcon;

    private Hyperlink friendFullName;
    private Label info;
    private VBox textContainer;

    private HBox graphicContainer;

    FriendListCell() {
        super();

        double fitSize = 56.0, clipRadius = fitSize / 2.0;
        Circle clip = new Circle(clipRadius, clipRadius, clipRadius);

        friendIcon = new ImageView();
        friendIcon.setPreserveRatio(true);
        friendIcon.setSmooth(true);
        friendIcon.setFitHeight(fitSize);
        friendIcon.setFitWidth(fitSize);
        friendIcon.setClip(clip);

        // Стилизовать
        friendFullName = new Hyperlink();

        info = new Label();
        info.setWrapText(true);

        textContainer = new VBox(friendFullName, info);
        textContainer.setFillWidth(true);
        textContainer.getStyleClass().add("vbox");

        graphicContainer = new HBox(friendIcon, textContainer);
        graphicContainer.getStyleClass().add("friend-list-cell");

    }

    private void setFriendFullNameHref(String screenName) {
        friendFullName.setOnAction(event -> SummerPracticeApplication.getHostService().showDocument(vkLink + screenName));
    }

    @Override
    protected void updateItem(FriendModel item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setGraphic(null);
        } else {

            friendIcon.setImage(item.profilePhoto());

            friendFullName.setText(item.lastName() + " " + item.firstName());
            setFriendFullNameHref(item.screenName());

            info.setText(
                    "Опубликовал больше всего фотографий " + Mapper.mapMaxPhotosByYearMonthToString(item.maxPhotosNumberByYearMonth())
            );

            setGraphic(graphicContainer);
        }

        setText(null);
    }
}
