package ru.manannikov.summerpractice_;

import com.vk.api.sdk.objects.users.responses.GetResponse;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public class Mapper {
    public static FriendModel mapUserFullToFriendModel(GetResponse userFull) {
        return new FriendModel(userFull.getScreenName(), userFull.getFirstName(), userFull.getLastName(), userFull.getPhoto200());
    }
    public static ObservableList< XYChart.Data<String, Number> > mapYearMonthIntegerMapToChartData(Map<YearMonth, Integer> maxFriendsPhotosNumberByYearMonth) {
        List< XYChart.Data<String, Number> > list = maxFriendsPhotosNumberByYearMonth.entrySet().stream()
                .map( entry -> {
                    return new XYChart.Data<>(entry.getKey().toString(), (Number) entry.getValue());
                })
        .toList();

        return FXCollections.observableList(list);
    }
}
