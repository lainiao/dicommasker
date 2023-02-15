package net.lainiao.dicom.ui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import net.lainiao.dicom.utils.CompressZipAndRarUtil;
import org.apache.commons.lang.StringUtils;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.DicomOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.rmi.runtime.Log;

import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainController {
    private static Logger logger = LoggerFactory.getLogger(MainController.class);
    @FXML
    private TextArea txtInfo;
    @FXML
    private TextArea txtError;
    @FXML
    private Label txtSource;
    @FXML
    private Label txtTemp;
    @FXML
    private Label txtTarget;
    @FXML
    private Button btnStart;
    @FXML
    private Button btnEnd;
    @FXML
    private Button btnSelectSource;
    @FXML
    private Button btnSelectTemp;
    @FXML
    private Button btnSelectTarget;
    @FXML
    private CheckBox cbFilter;
    @FXML
    private CheckBox cbFilter1;

    private Stage state;
    DirectoryChooser directoryChooser = new DirectoryChooser();
    private File fileSource;
    private File fileTemp;
    private File fileTarget;

    public void init(Stage stage) {
        this.state = stage;
        txtInfo.setEditable(false);
        txtError.setEditable(false);
    }

    public void btnSelectSourceAction(ActionEvent actionEvent) {
        fileSource = directoryChooser.showDialog(state);
        txtSource.setText(fileSource.getAbsolutePath());
    }

    public void btnSelectTempAction(ActionEvent actionEvent) {
        fileTemp = directoryChooser.showDialog(state);
        txtTemp.setText(fileTemp.getAbsolutePath());
    }

    public void btnSelectTargetAction(ActionEvent actionEvent) {
        fileTarget = directoryChooser.showDialog(state);
        txtTarget.setText(fileTarget.getAbsolutePath());
    }

    public void btnStartAction(ActionEvent actionEvent) {
        setButtonState(true);
        if (cbFilter1.isSelected()) {
            cbFilter.setSelected(false);
            startTask1();
        } else {
            startTask();
        }
    }

    private void startTask1() {
        new Thread(new Runnable() {
            private List<File> studyFiles = new ArrayList<>();

            @Override
            public void run() {
                sendInfo("Start Task");
                eachForFile(fileSource);
                sendInfo("Over Task");

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        setButtonState(false);
                    }
                });
            }


            private void eachForFile(File inputFile) {
                sendInfo("hander file:"+inputFile.getAbsolutePath());
                if (inputFile.isFile()) {
                    String name = inputFile.getName().toLowerCase();
                    if (name.endsWith(".zip")||name.endsWith(".7z")||name.endsWith(".rar")) {
                        return;
                    }
                    else if (name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".bmp")) {
                        return;
                    } else {
                        dicomFileHandler(inputFile);
                    }
                } else {
                    File[] files = inputFile.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            eachForFile(file);
                        }
                    }

                }
            }

            private void dicomFileHandler(File inputFile) {
                try {
                    if (isDicomFile(inputFile)) {
                        DicomInputStream dicomInputStream = null;
                        DicomObject dicomObject = null;

                        //上传dicom文件路径
                        String fileUploadUrl = null;
                        try {
                            dicomInputStream = new DicomInputStream(inputFile);
                            dicomObject = dicomInputStream.readDicomObject();
                            dicomInputStream.close();
                            String studyId = dicomObject.getString(Tag.StudyID);
                            String seriesIuId = dicomObject.getString(Tag.SeriesInstanceUID);
                            String studyIuid = dicomObject.getString(Tag.StudyInstanceUID);
                            String sopInstanceUID = dicomObject.getString(Tag.SOPInstanceUID);
                            if (StringUtils.isEmpty(seriesIuId) || StringUtils.isEmpty(sopInstanceUID)) {
                                sendError("Series ID or SOP-ID cannot be read:" + inputFile.getAbsolutePath(), null);
                                return;
                            }

                            String accessNumber = dicomObject.getString(Tag.AccessionNumber);
                            dicomObject.remove(Tag.PatientID);
                            dicomObject.putString(Tag.PatientID, VR.PN, accessNumber);
                            dicomObject.remove(Tag.PatientName);
                            dicomObject.putString(Tag.PatientName, VR.PN, accessNumber);
                            dicomObject.remove(0x00611001);
                            dicomObject.putString(0x00611001, VR.PN, accessNumber);
                            dicomObject.remove(Tag.InstitutionName);
                            dicomObject.putString(Tag.InstitutionName, VR.PN, "PANDA");
                            dicomObject.remove(Tag.ImplementationVersionName);
                            dicomObject.putString(Tag.ImplementationVersionName, VR.PN, "PANDA");
                            dicomObject.remove(Tag.OperatorsName);
                            dicomObject.putString(Tag.OperatorsName, VR.PN, "PANDA");
                            dicomObject.remove(Tag.InstitutionalDepartmentName);
                            dicomObject.putString(Tag.InstitutionalDepartmentName, VR.PN, "PANDA");
                            dicomObject.remove(Tag.InstitutionAddress);
                            dicomObject.putString(Tag.InstitutionAddress, VR.PN, "PANDA");
                            dicomObject.remove(Tag.Manufacturer);
                            dicomObject.putString(Tag.Manufacturer, VR.LO, "PANDA");

                            Iterator<DicomElement> iterator = dicomObject.datasetIterator();
                            while (iterator.hasNext()) {
                                DicomElement dicomElement = iterator.next();
                                if (dicomElement.tag() > 0x00610000 && dicomElement.tag() < 0x7FDF0000) {
                                    dicomObject.remove(dicomElement.tag());
                                }
                            }

                            FileOutputStream outputStream = new FileOutputStream(inputFile.getAbsolutePath());
                            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
                            DicomOutputStream dicomOutputStream = new DicomOutputStream(bufferedOutputStream);
                            dicomOutputStream.writeDicomFile(dicomObject);
                            dicomOutputStream.close();
                            bufferedOutputStream.close();
                            outputStream.close();
                            dicomInputStream.close();
                            dicomInputStream = null;
                        } finally {
                            if (dicomInputStream != null) {
                                dicomInputStream.close();
                            }
                        }
                    } else {
                        sendError("Non-DICOM File:" + inputFile.getAbsolutePath(), null);
                    }
                } catch (Exception e) {
                    sendError("Handling file errors:" + inputFile.getAbsolutePath(), e);
                }
            }



            private void unZipFile(File inputFile) {
                try {
                    sendInfo("Unpacking files:" + inputFile.getAbsolutePath());
                    logger.info("Unpacking files:" + inputFile.getAbsolutePath());
                    List<String> filePaths = new ArrayList<>();
                    List<String> errors = new ArrayList<>();
                    String uuid = newUUID();
                    File temFileFolder = new File(fileTemp, uuid);
                    temFileFolder.mkdir();
                    String temFolderPath = temFileFolder.getAbsolutePath();
                    sendInfo("Create a new temporary directory:" + temFolderPath);
                    logger.info("Create a new temporary directory:" + temFolderPath);

                    boolean state = CompressZipAndRarUtil.decompressFile(inputFile.getAbsolutePath(), temFolderPath, filePaths, errors);
                    if (state) {
                        sendInfo("Unpacked files:" + inputFile.getAbsolutePath());
                        logger.error("Unpacked files:" + inputFile.getAbsolutePath());
                        for (String err : errors) {
                            sendError(err, null);
                        }
                        for (String filePath : filePaths) {
                            eachForFile(new File(filePath));
                        }
                    } else {
                        sendError("Error decompressing the file:" + inputFile.getAbsolutePath(), null);
                    }
                    deleteFolder(new File(temFolderPath));
                } catch (Exception e) {
                    sendError("Error decompressing the file:" + inputFile.getAbsolutePath(), e);
                }

            }

            public String newUUID() {
                return UUID.randomUUID().toString().replaceAll("-", "");
            }


        }).start();
    }

    private void startTask() {
        new Thread(new Runnable() {
            private List<File> studyFiles = new ArrayList<>();

            @Override
            public void run() {
                sendInfo("Start Task");
                eachForFile(fileSource);
                if (cbFilter.isSelected()) {
                    sendInfo("Redundant sequences are being removed");
                    filterDICOMStudy();
                }
                sendInfo("Over Task");
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        setButtonState(false);
                    }
                });
            }

            private void filterDICOMStudy() {
                for (File file : studyFiles) {
                    File[] seriesFiles = file.listFiles();
                    if (seriesFiles == null) {
                        continue;
                    }
                    int count = 0;
                    File preFile = null;
                    for (File serfiesFile : seriesFiles) {
                        int fileCount = serfiesFile.list().length;
                        if (count == 0) {
                            preFile = serfiesFile;
                            count = fileCount;
                        } else {
                            if (fileCount > count) {
                                count = fileCount;
                                deleteFolder(preFile);
                                preFile = serfiesFile;
                            } else {
                                deleteFolder(serfiesFile);
                            }
                        }
                    }
                }
            }


            private void eachForFile(File inputFile) {
                sendInfo("hander file:"+inputFile.getAbsolutePath());
                if (inputFile.isFile()) {
                    String name = inputFile.getName().toLowerCase();
                    if (name.endsWith(".zip")) {
                        unZipFile(inputFile);
                    } else if (name.endsWith(".7z")) {
                        unZipFile(inputFile);
                    } else if (name.endsWith(".rar")) {
                        unZipFile(inputFile);
                    } else if (name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".bmp")) {
                        return;
                    } else {
                        dicomFileHandler(inputFile);
                    }
                } else {
                    File[] files = inputFile.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            eachForFile(file);
                        }
                    }

                }
            }

            private void dicomFileHandler(File inputFile) {
                try {
                    if (isDicomFile(inputFile)) {
                        DicomInputStream dicomInputStream = null;
                        DicomObject dicomObject = null;

                        //上传dicom文件路径
                        String fileUploadUrl = null;
                        try {
                            dicomInputStream = new DicomInputStream(inputFile);
                            dicomObject = dicomInputStream.readDicomObject();
                            String studyId = dicomObject.getString(Tag.StudyID);
                            String seriesIuId = dicomObject.getString(Tag.SeriesInstanceUID);
                            String studyIuid = dicomObject.getString(Tag.StudyInstanceUID);
                            String sopInstanceUID = dicomObject.getString(Tag.SOPInstanceUID);
                            if (StringUtils.isEmpty(seriesIuId) || StringUtils.isEmpty(sopInstanceUID)) {
                                sendError("Series ID or SOP-ID cannot be read:" + inputFile.getAbsolutePath(), null);
                                return;
                            }
                            File targetFolderFile = new File(fileTarget.getAbsolutePath());



                            targetFolderFile = new File(targetFolderFile, studyIuid);
                            if (!targetFolderFile.exists()) {
                                targetFolderFile.mkdir();
                                studyFiles.add(targetFolderFile);
                            }

                            targetFolderFile = new File(targetFolderFile, seriesIuId);
                            if (!targetFolderFile.exists()) {
                                targetFolderFile.mkdir();
                            }

                            File targetFile = new File(targetFolderFile, sopInstanceUID + ".dcm");
                            String accessNumber = dicomObject.getString(Tag.AccessionNumber);
                            dicomObject.remove(Tag.PatientID);
                            dicomObject.putString(Tag.PatientID, VR.PN, accessNumber);
                            dicomObject.remove(Tag.PatientName);
                            dicomObject.putString(Tag.PatientName, VR.PN, accessNumber);
                            dicomObject.remove(0x00611001);
                            dicomObject.putString(0x00611001, VR.PN, accessNumber);
                            dicomObject.remove(Tag.InstitutionName);
                            dicomObject.putString(Tag.InstitutionName, VR.PN, "PANDA");
                            dicomObject.remove(Tag.ImplementationVersionName);
                            dicomObject.putString(Tag.ImplementationVersionName, VR.PN, "PANDA");
                            dicomObject.remove(Tag.OperatorsName);
                            dicomObject.putString(Tag.OperatorsName, VR.PN, "PANDA");
                            dicomObject.remove(Tag.InstitutionalDepartmentName);
                            dicomObject.putString(Tag.InstitutionalDepartmentName, VR.PN, "PANDA");
                            dicomObject.remove(Tag.InstitutionAddress);
                            dicomObject.putString(Tag.InstitutionAddress, VR.PN, "PANDA");
                            dicomObject.remove(Tag.Manufacturer);
                            dicomObject.putString(Tag.Manufacturer, VR.LO, "PANDA");

                            Iterator<DicomElement> iterator = dicomObject.datasetIterator();
                            while (iterator.hasNext()) {
                                DicomElement dicomElement = iterator.next();
                                if (dicomElement.tag() > 0x00610000 && dicomElement.tag() < 0x7FDF0000) {
                                    dicomObject.remove(dicomElement.tag());
                                }
                            }

                            FileOutputStream outputStream = new FileOutputStream(targetFile);
                            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
                            DicomOutputStream dicomOutputStream = new DicomOutputStream(bufferedOutputStream);
                            dicomOutputStream.writeDicomFile(dicomObject);
                            dicomOutputStream.close();
                            bufferedOutputStream.close();
                            outputStream.close();
                            dicomInputStream.close();
                            dicomInputStream = null;
                        } finally {
                            if (dicomInputStream != null) {
                                dicomInputStream.close();
                            }
                        }
                    } else {
                        sendError("Non-Dicom File" + inputFile.getAbsolutePath(), null);
                    }
                } catch (Exception e) {
                    sendError("Handling file errors:" + inputFile.getAbsolutePath(), null);
                }
            }



            private void unZipFile(File inputFile) {
                try {
                    sendInfo("Unpacking files:" + inputFile.getAbsolutePath());
                    logger.info("Unpacking files::" + inputFile.getAbsolutePath());
                    List<String> filePaths = new ArrayList<>();
                    List<String> errors = new ArrayList<>();
                    String uuid = newUUID();
                    File temFileFolder = new File(fileTemp, uuid);
                    temFileFolder.mkdir();
                    String temFolderPath = temFileFolder.getAbsolutePath();
                    sendInfo("Create a new temporary directory:" + temFolderPath);
                    logger.info("Create a new temporary directory:" + temFolderPath);

                    boolean state = CompressZipAndRarUtil.decompressFile(inputFile.getAbsolutePath(), temFolderPath, filePaths, errors);
                    if (state) {
                        sendInfo("Unpacked files:" + inputFile.getAbsolutePath());
                        for (String err : errors) {
                            sendError(err, null);
                        }
                        for (String filePath : filePaths) {
                            eachForFile(new File(filePath));
                        }
                    } else {
                        sendError("Error decompressing the file:" + inputFile.getAbsolutePath(), null);
                    }
                    deleteFolder(new File(temFolderPath));
                } catch (Exception e) {
                    sendError("Error decompressing the file:" + inputFile.getAbsolutePath(), e);
                }

            }

            public String newUUID() {
                return UUID.randomUUID().toString().replaceAll("-", "");
            }
        }).start();
    }

    private void sendInfo(String text) {
        logger.info(text);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                txtInfo.appendText(text + "\n");
            }
        });
    }


    private void sendError(String text, Throwable e) {
        logger.error(text, e);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                txtError.appendText(text + e.toString() + "\n");
            }
        });
    }

    private void deleteFolder(File preFile) {
        if (preFile.isDirectory()) {
            File[] files = preFile.listFiles();
            if (files != null) {
                for (File file : preFile.listFiles()) {
                    deleteFolder(file);
                }
            }
        }
        preFile.delete();
    }

    public boolean isDicomFile(File inputFile) {
        FileInputStream is = null;
        try {
            is = new FileInputStream(inputFile);
            is.skip(128);
            byte[] buf = new byte[4];
            is.read(buf);
            String msg_DCM = new String(buf);
            if (msg_DCM.equals("DICM")) {
                return true;
            }
        } catch (IOException e) {

        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {

            }
        }
        return false;
    }

    private void setButtonState(boolean b) {
        btnStart.setDisable(b);
        btnSelectSource.setDisable(b);
        btnSelectTemp.setDisable(b);
        btnSelectTarget.setDisable(b);
    }

    public void btnEndAction(ActionEvent actionEvent) {
        System.exit(0);
    }
}
