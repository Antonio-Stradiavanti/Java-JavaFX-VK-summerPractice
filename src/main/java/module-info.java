module ru.manannikov.summerpractice_ {
    requires javafx.web;
    requires javafx.fxml;
    requires javafx.swing;
    requires sdk;
    requires org.slf4j;

    opens ru.manannikov.summerpractice_ to javafx.fxml;
    exports ru.manannikov.summerpractice_;
}