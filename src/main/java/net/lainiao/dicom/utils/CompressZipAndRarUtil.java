package net.lainiao.dicom.utils;

import net.sf.sevenzipjbinding.*;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Hello world!
 */
public class CompressZipAndRarUtil {

    public static boolean decompressFile(String inputFilePath, final String targetFileDir, List<String> compressList, List<String> errors) {
        //解压7zip文件
        RandomAccessFile randomAccessFile = null;
        IInArchive inArchive = null;

        try {
            // 判断目标目录是否存在，不存在则创建
            File newdir = new File(targetFileDir);
            if (false == newdir.exists()) {
                newdir.mkdirs();
                newdir = null;
            }
            randomAccessFile = new RandomAccessFile(inputFilePath, "r");
            RandomAccessFileInStream t = new RandomAccessFileInStream(randomAccessFile);
            if (inputFilePath.endsWith("rar")) {
                inArchive = SevenZip.openInArchive(ArchiveFormat.RAR5, t);
            } else if (inputFilePath.endsWith("7z")) {
                inArchive = SevenZip.openInArchive(ArchiveFormat.SEVEN_ZIP, t);
            } else {
                inArchive = SevenZip.openInArchive(ArchiveFormat.ZIP, t);
            }
            ISimpleInArchive simpleInArchive = inArchive.getSimpleInterface();
            for (final ISimpleInArchiveItem item : simpleInArchive.getArchiveItems()) {
                final int[] hash = new int[]{0};
                if (!item.isFolder()) {
                    ExtractOperationResult result;
                    final long[] sizeArray = new long[1];
                    result = item.extractSlow(new ISequentialOutStream() {
                        public int write(byte[] data) throws SevenZipException {
                            //写入指定文件
                            FileOutputStream fos;
                            try {
                                if (item.getPath().indexOf(File.separator) > 0) {
                                    String path = targetFileDir + File.separator + item.getPath().substring(0, item.getPath().lastIndexOf(File.separator));
                                    File folderExisting = new File(path);
                                    if (!folderExisting.exists()) {
                                        new File(path).mkdirs();
                                    }
                                }
                                fos = new FileOutputStream(targetFileDir + File.separator + item.getPath(), true);
                                fos.write(data);
                                fos.close();
                            } catch (Exception e) {
                                errors.add(e.toString());
                            }
                            hash[0] ^= Arrays.hashCode(data); // Consume data
                            sizeArray[0] += data.length;
                            return data.length; // Return amount of consumed data
                        }
                    });

                    if (result == ExtractOperationResult.OK) {
                        String filePaht = targetFileDir + File.separator + item.getPath();
                        compressList.add(filePaht);
                    }
                }
            }
            return true;

        } catch (Exception e) {
            errors.add(e.toString());
        } finally {
            if (inArchive != null) {
                try {
                    inArchive.close();
                } catch (SevenZipException e) {

                }
            }
            if (randomAccessFile != null) {
                try {
                    randomAccessFile.close();
                } catch (IOException e) {

                }
            }
        }
        return false;
    }


}
