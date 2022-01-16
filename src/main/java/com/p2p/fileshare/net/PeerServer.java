package com.p2p.fileshare.net;

import com.p2p.fileshare.config.BeansUtil;
import com.p2p.fileshare.dto.PeersConfig;
import com.p2p.fileshare.wrapper.PeerServerWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

public class PeerServer implements Runnable
{
    private final int id;
    private final CountDownLatch countDownLatch;
    private final ServerSocket serverSocket;

    private static final Logger LOGGER = LogManager.getLogger(PeerServer.class);

    public PeerServer(int id, PeerServerWrapper peerServerWrapper, CountDownLatch countDownLatch)
    {
        this.id = id;
        this.countDownLatch = countDownLatch;
        this.serverSocket = peerServerWrapper.getServerSocket();
    }

    @Override
    public void run()
    {
        PeersConfig.PeerInfo peerInfo = BeansUtil.getBean(PeersConfig.class).getPeer(id);

        try
        {
            LOGGER.info("Peer server started successfully with id {}", id);

            while (true)
            {
                if (Thread.currentThread().isInterrupted())
                {
                    countDownLatch.countDown();
                    break;
                }

                Socket socket = serverSocket.accept();
                BeansUtil.getBean(ExecutorService.class).execute(new Peer(id, socket, countDownLatch));
            }
        }
        catch (UnknownHostException e)
        {
            LOGGER.error("Error while parsing hostname {}", peerInfo.getHostName(), e);
            System.exit(0);
        }
        catch (SocketException e)
        {
            countDownLatch.countDown();
        }
        catch (IOException e)
        {
            LOGGER.error("Error while accepting requests from other peers", e);
        }
    }
}
