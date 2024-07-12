package ru.manannikov.summerpractice_;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

import java.time.YearMonth;
import java.util.Map;

public class HistogramController {
    @FXML
    private BarChart<String, Number> barChart;
    @FXML
    private Label description;

    public void populateHistogram(final Map<YearMonth, Integer> photosNumberByYearMonthMap, final String descriptionText, final String chartTitle) {
        barChart.setData(
            FXCollections.observableArrayList(
                new XYChart.Series<>(Mapper.mapYearMonthIntegerMapToChartData(photosNumberByYearMonthMap))
            )
        );
        barChart.setTitle(chartTitle);

        description.setText(descriptionText);
    }
}
