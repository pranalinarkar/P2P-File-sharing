package com.p2p.fileshare.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.OutputStream;
import java.net.SocketException;

public class MessageSender implements Runnable
{
    private final OutputStream outputStream;
    private final byte[] message;

    private static final Logger LOGGER = LogManager.getLogger(MessageSender.class);

    public MessageSender(OutputStream outputStream, byte[] message)
    {
        this.outputStream = outputStream;
        this.message = message;
    }

    @Override
    public void run()
    {
        try
        {
            if (Thread.currentThread().isInterrupted())
            {
                return;
            }

            synchronized (outputStream)
            {
                Thread.sleep(10);
                outputStream.write(message);
            }
        }
        catch (SocketException | InterruptedException e)
        {
            return;
        }
        catch (Exception e)
        {
            LOGGER.error("Error while sending message", e);
        }
    }
}
