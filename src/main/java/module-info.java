module ru.manannikov.summerpractice_ {
    requires javafx.web;
    requires javafx.fxml;
    requires javafx.swing;
    requires sdk;
    requires org.slf4j;
    requires com.google.gson;

    opens ru.manannikov.summerpractice_ to javafx.fxml, com.google.gson;
    exports ru.manannikov.summerpractice_;
}