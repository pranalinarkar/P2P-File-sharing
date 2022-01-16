package com.p2p.fileshare.util;

import com.p2p.fileshare.constant.Constants;
import com.p2p.fileshare.constant.MessageType;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.BitSet;
import java.util.Date;

public class CommonUtil
{
    private static final byte[] HEADER_BYTES = Constants.HANDSHAKE_HEADER.getBytes();

    public static BufferedReader getReaderFromFile(String fileName) throws FileNotFoundException
    {
        return new BufferedReader(
                new java.io.InputStreamReader(
                        new FileInputStream(fileName)
                )
        );
    }

    public static byte[] getMessage(int length, MessageType messageType, byte[] data)
    {
        byte[] message = new byte[5 + length];
        int counter = mergeByteArrays(message, intToByteArr(length), 0);
        message[counter++] = messageType.getValue();

        if (length > 0)
        {
            mergeByteArrays(message, data, counter);
        }

        return message;
    }

    public static byte[] getHandshakeMessage(int peerId)
    {
        byte[] message = new byte[32];
        int counter = 0;

        for (byte headerByte : HEADER_BYTES)
        {
            message[counter++] = headerByte;
        }

        for (int i=0; i<10; i++)
        {
            message[counter++] = 0;
        }

        for (byte numByte : intToByteArr(peerId))
        {
            message[counter++] = numByte;
        }

        return message;
    }

    public static byte[] intToByteArr(int num)
    {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(num);
        return buffer.array();
    }

    public static int byteArrToInt(byte[] byteArr)
    {
        ByteBuffer buffer = ByteBuffer.wrap(byteArr);
        return buffer.getInt();
    }

    public static String getCurrentTime()
    {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
        return formatter.format(date);
    }

    public static int mergeByteArrays(byte[] arr1, byte[] arr2, int start)
    {
        for (byte byteData : arr2)
        {
            arr1[start++] = byteData;
        }

        return start;
    }

    public static int getFirstClearIndexFromBitfields(BitSet bitfield1, BitSet bitfield2, int from)
    {
        int index = from;

        while ((index = bitfield1.nextClearBit(index)) != -1)
        {
            if (bitfield2.get(index))
            {
                break;
            }
            else if (index >= bitfield1.length())
            {
                index = -1;
                break;
            }

            index++;
        }

        return index;
    }

    public static void deleteDirectory(String path) throws IOException
    {
        Files
                .walk(Paths.get(path))
                .map(Path::toFile)
                .forEach(File::delete);

    }
}
