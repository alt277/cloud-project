package org.myexample.cloud.project;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.myexample.cloud.project.common.Sender;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;

public class MainController implements Initializable {
    public Button ButtonSYNC;
    @FXML
    Label client_title;
    public  boolean isAuthorized;
    @FXML TextField     loginField;

    @FXML PasswordField passwordField;

    @FXML
    TextField tfFileName;
    @FXML
    ListView<String> filesList;
    @FXML
    ListView<String> filesList1;
    @FXML
    ListView<String> filesList2;

    public  ListView<String> getFilesList1(){
        return filesList1;
    }

    @FXML HBox   buttonPanel1;
    @FXML HBox   buttonPanel2;
    @FXML HBox   authorisePanel;
    @FXML HBox   infoPanel;

    public void setAuthorized(boolean isAuthorized){
       // this.isAuthorized=isAuthorized;
        if(!isAuthorized){
            authorisePanel.setVisible(true);
            authorisePanel.setManaged(true);
            buttonPanel1.setVisible(false);
            buttonPanel1.setManaged(false);
            buttonPanel2.setVisible(false);
            buttonPanel2.setManaged(false);
            infoPanel.setVisible(false);
            infoPanel.setManaged(false);
            ButtonSYNC.setVisible(false);
            ButtonSYNC.setManaged(false);
        }
        else {
            authorisePanel.setVisible(false);
            authorisePanel.setManaged(false);
            buttonPanel1.setVisible(true);
            buttonPanel1.setManaged(true);
            buttonPanel2.setVisible(true);
            buttonPanel2.setManaged(true);
            infoPanel.setVisible(true);
            infoPanel.setManaged(true);
            ButtonSYNC.setVisible(true);
            ButtonSYNC.setManaged(true);

        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {


        CountDownLatch networkStarter = new CountDownLatch(1);
        new Thread(() -> ByteNetwork.getInstance().start(networkStarter,()->{
                    setAuthorized(true);
                    refreshAll();

            },
                ()->{
                   refreshAll();
                    setAuthorized(true);
                    } )).start();
        try {
            networkStarter.await();                   //   подождать открытия соединения

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
           setAuthorized(false);
        refreshAll();
    }

    public void pressOnDownloadBtnSend(ActionEvent actionEvent) throws IOException {
        if (tfFileName.getLength() > 0) {
            Path pathToFile=(Paths.get(ClientHandler1.storage_way +tfFileName.getText()));
            System.out.println(pathToFile);

            if (Files.exists(pathToFile)) {
               Sender.sendFile(pathToFile,
//            if (Files.exists(Paths.get("client_storage/" +tfFileName.getText() ))) {
//                Sender.sendFile(Paths.get("client_storage/" + tfFileName.getText()),
                        ByteNetwork.getInstance().getCurrentChannel(), future -> {
                            if (!future.isSuccess()) {
                                future.cause().printStackTrace();
                            }
                            if (future.isSuccess()) {
                                System.out.println("Btn send: Файл передан с клиента" + tfFileName.getText());
                            }
                        });
            }
                tfFileName.clear();
                System.out.println("Button Send works");

        }
    }
    public void pressOnDownloadBtnGet(ActionEvent actionEvent) throws IOException {
        if (tfFileName.getLength() > 0) {
                Sender.getFile( tfFileName.getText(),
                        ByteNetwork.getInstance().getCurrentChannel(), future -> {
                            if (!future.isSuccess()) {
                                future.cause().printStackTrace();

                            }
                            if (future.isSuccess()) {
                                System.out.println("Запрос файла передан с клиента" + tfFileName.getText());

                            }
                        });
                tfFileName.clear();
                System.out.println("Button Get works");
            }
    }
    public void pressOnDownloadBtnOpenAcc(ActionEvent actionEvent) throws IOException {
        if (tfFileName.getLength() > 0) {
            Sender.openAccess( tfFileName.getText(),
                    ByteNetwork.getInstance().getCurrentChannel(), future -> {
                        if (!future.isSuccess()) {
                            future.cause().printStackTrace();
                        }
                        if (future.isSuccess()) {
                            System.out.println("Запрос файла передан с клиента" + tfFileName.getText());

                        }
                    });
            tfFileName.clear();
            System.out.println("Button Open Access works");
        }
    }
    public void pressOnDownloadBtnCloseAcc(ActionEvent actionEvent) throws IOException {
        if (tfFileName.getLength() > 0) {
            Sender.closeAccess( tfFileName.getText(),
                    ByteNetwork.getInstance().getCurrentChannel(), future -> {
                        if (!future.isSuccess()) {
                            future.cause().printStackTrace();
                        }
                        if (future.isSuccess()) {
                            System.out.println("Запрос  передан с клиента" + tfFileName.getText());

                        }
                    });
            tfFileName.clear();
            System.out.println("Button Open Access works");
        }
    }
    public void pressOnDownloadBtnDelete(ActionEvent actionEvent) throws IOException {
        if (tfFileName.getLength() > 0) {
            Sender.deleteFile( tfFileName.getText(),
                    ByteNetwork.getInstance().getCurrentChannel(), future -> {
                        if (!future.isSuccess()) {
                            future.cause().printStackTrace();
                        }
                        if (future.isSuccess()) {
                            System.out.println("Запрос файла передан с клиента" + tfFileName.getText());
                        }
                    });
            tfFileName.clear();
            System.out.println("Button Open Access works");
        }
    }
    public void refreshLocalFilesList() {
        Platform.runLater(() -> {
            try {
                filesList.getItems().clear();
                Files.list(Paths.get(ClientHandler1.storage_way))
                        .filter(p -> !Files.isDirectory(p))
                        .map(p -> p.getFileName().toString())
                        .forEach(o -> filesList.getItems().add(o));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public  void refreshAll() {
        Platform.runLater(() -> {
            try {
                System.out.println("путь для обновления: "+ClientHandler1.storage_way);
                filesList.getItems().clear();
                if (Files.exists(Paths.get(ClientHandler1.storage_way))) {
                    Files.list(Paths.get(ClientHandler1.storage_way))
                            .filter(p -> !Files.isDirectory(p))
                            .map(p -> p.getFileName().toString())
                            .forEach(o -> filesList.getItems().add(o));
                }

                filesList1.getItems().clear();
                 ClientHandler2.refreshingFiles.stream()
                        .forEach(o -> filesList1.getItems().add(o));
   //        ClientHandler2.refreshingFiles.clear();

                filesList2.getItems().clear();
                Files.list(Paths.get("Access_storage"))
                        .filter(p -> !Files.isDirectory(p))
                        .map(p -> p.getFileName().toString())
                        .forEach(o -> filesList2.getItems().add(o));
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void pressOnDownloadBtnSync(ActionEvent actionEvent) throws IOException {
        List<String> clientFiles =new ArrayList<>();
        Files.list(Paths.get(ClientHandler1.storage_way))
                .filter(p -> !Files.isDirectory(p))
                .map(p -> p.getFileName().toString())
                .forEach(o -> clientFiles.add(o));     // получаем список файлов клиента
        System.out.println("Controller: список клиента: ");
        clientFiles.stream().forEach(o -> System.out.println(o));

            Sender.sendSYNC( clientFiles,
                    ByteNetwork.getInstance().getCurrentChannel(), future -> {
                        if (!future.isSuccess()) {
                            future.cause().printStackTrace();
                        }
                        if (future.isSuccess()) {
                            System.out.println("Controller: Список файлов передан с клиента" );
                          refreshAll();
                        }
                    });
            //clientFiles.clear();
            System.out.println("Button Sync works");
        }

    public void pressOnDownloadBtnEnter(ActionEvent actionEvent) throws IOException {
        if ((loginField.getLength() > 0) && (passwordField.getLength() > 0)) {
            String message = loginField.getText() + "?" + passwordField.getText();
            Sender.authorizeCMD(message,
                    ByteNetwork.getInstance().getCurrentChannel(), future -> {
                        if (!future.isSuccess()) {
                            future.cause().printStackTrace();
                        }
                        if (future.isSuccess()) {
                            System.out.println("Запрос авторизации передан с клиента");

                        }
                    });
            loginField.clear();
            passwordField.clear();
            System.out.println("Button Enter works");

        }
    }


}
