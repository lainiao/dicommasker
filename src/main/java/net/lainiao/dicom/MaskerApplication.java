package net.lainiao.dicom;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import net.lainiao.dicom.ui.MainController;

import java.net.URL;

public class MaskerApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        URL url=getClass().getResource("ui/MainFrm.fxml");
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(url);
        fxmlLoader.setBuilderFactory(new JavaFXBuilderFactory());
        Parent root = fxmlLoader.load();
        primaryStage.setTitle("DICOM desensitization program (support ZIP, RAR, DICOM files)");
        primaryStage.getIcons().add(new Image("/image/logo.png"));
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();
        MainController mainController=fxmlLoader.getController();
        mainController.init(primaryStage);
    }


    public static void main(String[] args) {
        launch(args);
    }
}
