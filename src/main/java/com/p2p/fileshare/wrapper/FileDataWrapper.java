package com.p2p.fileshare.wrapper;

import com.p2p.fileshare.config.BeansUtil;
import com.p2p.fileshare.constant.Constants;
import com.p2p.fileshare.dto.CommonConfig;
import com.p2p.fileshare.util.CommonUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileDataWrapper
{
    private final String tempFilesPath;
    private final String finalFilePath;
    private final CommonConfig commonConfig;

    private final Logger LOGGER = LogManager.getLogger(FileDataWrapper.class);

    public FileDataWrapper(int id)
    {
        commonConfig = BeansUtil.getBean(CommonConfig.class);

        String basePath = System.getProperty(Constants.WORKING_DIR) + File.separator + Constants.PEER + id + File.separator;
        tempFilesPath = basePath + Constants.TEMP + File.separator;
        finalFilePath = basePath + commonConfig.getFileName();
    }

    public byte[] getFilePiece(int pieceIndex) throws IOException
    {
        try (FileInputStream fileInputStream = new FileInputStream(tempFilesPath + Constants.TEMP + pieceIndex + Constants.TEMP_EXT))
        {
            int length = (int) fileInputStream.getChannel().size();
            byte[] pieceData = new byte[length];
            int readBytes = 0;

            while (readBytes < length)
            {
                readBytes += fileInputStream.read(pieceData, readBytes, length);
            }

            return pieceData;
        }
        catch (IOException e)
        {
            LOGGER.error("Error while reading file data.", e);
            throw e;
        }
    }

    public void savePiece(int pieceIndex, byte[] pieceData) throws IOException
    {
        String piecePath = tempFilesPath + Constants.TEMP + pieceIndex + Constants.TEMP_EXT;

        try
        {
            Files.createDirectories(Paths.get(tempFilesPath));
        }
        catch (IOException e)
        {
            LOGGER.error("Error while creating temp directory", e);
        }

        try (FileOutputStream os = new FileOutputStream(piecePath))
        {
            os.write(pieceData);
        }
        catch (IOException e)
        {
            LOGGER.error("Error while saving piece data with index {}", pieceIndex, e);
            throw e;
        }
    }

    public void joinAndSavePieces() throws IOException
    {
        try (FileOutputStream os = new FileOutputStream(finalFilePath, true))
        {
            for (int i=0; i<commonConfig.getPiecesCount(); i++)
            {
                os.write(Files.readAllBytes(Paths.get(tempFilesPath + Constants.TEMP + i + Constants.TEMP_EXT)));
            }
        }
        catch (IOException e)
        {
            LOGGER.error("Error while saving the final file", e);
            throw e;
        }
    }

    public void breakFileAndSavePieces() throws IOException
    {
        for (int i=0; i<commonConfig.getPiecesCount(); i++)
        {
            try (FileInputStream in = new FileInputStream(finalFilePath))
            {
                int offset = commonConfig.getPieceSize() * i;
                byte[] pieceData = getPieceData(in, offset);
                savePiece(i, pieceData);
            }
            catch (IOException e)
            {
                LOGGER.error("Error while breaking down the file into pieces", e);
                throw e;
            }
        }
    }

    private byte[] getPieceData(FileInputStream fileInputStream, int offset) throws IOException
    {
        int length;

        if (commonConfig.getFileSize() < offset + commonConfig.getPieceSize())
        {
            length = (int) (commonConfig.getFileSize() - offset);
        }
        else
        {
            length = commonConfig.getPieceSize();
        }

        fileInputStream.skip(offset);
        byte[] pieceData = new byte[length];
        int readBytesCount = 0;

        while (readBytesCount < length)
        {
            int readBytes = fileInputStream.read(pieceData, readBytesCount, length);
            readBytesCount += readBytes;
        }

        return pieceData;
    }

    public synchronized void deleteTempDir() throws IOException
    {
        Path tempPath = Paths.get(tempFilesPath);

        if (Files.exists(tempPath))
        {
            CommonUtil.deleteDirectory(tempFilesPath);
            Files.delete(tempPath);
        }
    }
}
