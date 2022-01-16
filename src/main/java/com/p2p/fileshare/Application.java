package com.p2p.fileshare;

import com.p2p.fileshare.config.BeansUtil;
import com.p2p.fileshare.dto.CommonConfig;
import com.p2p.fileshare.dto.PeersConfig;
import com.p2p.fileshare.handler.NeighborsHandler;
import com.p2p.fileshare.handler.OptimisticNeighborHandler;
import com.p2p.fileshare.handler.RequestedPiecesHandler;
import com.p2p.fileshare.net.Peer;
import com.p2p.fileshare.net.PeerServer;
import com.p2p.fileshare.wrapper.PeerServerWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Application
{
    private static final Logger LOGGER = LogManager.getLogger(Application.class);

    public static void main(String[] args)
    {
        int selfId = Integer.parseInt(args[0]);

        BeansUtil.init(selfId);
        CountDownLatch countDownLatch = initPeerConnections(selfId);
        initPeerServer(selfId, countDownLatch);
        initScheduledHandlers(selfId);

        try
        {
            countDownLatch.await();
        }
        catch (Exception e)
        {
            LOGGER.error("Error while waiting for countdown latch");
        }

        LOGGER.info("Shutting down the service.");
    }

    public static CountDownLatch initPeerConnections(int id)
    {
        ExecutorService executorService = BeansUtil.getBean(ExecutorService.class);
        PeersConfig peersConfig = BeansUtil.getBean(PeersConfig.class);
        CountDownLatch countDownLatch = new CountDownLatch(peersConfig.getPeers().size());

        for (PeersConfig.PeerInfo peerInfo : peersConfig.getPeers())
        {
            if (peerInfo.getId() == id)
            {
                break;
            }

            try
            {
                Socket socket = null;

                while (Objects.isNull(socket))
                {
                    try
                    {
                        socket = new Socket(peerInfo.getHostName(), peerInfo.getPort());
                    }
                    catch (Exception e)
                    {
                        //Waiting for peer process to start
                    }
                }

                executorService.execute(new Peer(id, peerInfo.getId(), socket, countDownLatch));
            }
            catch (IOException e)
            {
                LOGGER.error("Error while connecting to peer {}", peerInfo.toString(), e);
            }
        }

        return countDownLatch;
    }

    public static void initPeerServer(int id, CountDownLatch countDownLatch)
    {
        try
        {
            ExecutorService executorService = BeansUtil.getBean(ExecutorService.class);
            PeersConfig peersConfig = BeansUtil.getBean(PeersConfig.class);
            PeersConfig.PeerInfo peerInfo = peersConfig.getPeer(id);
            PeerServerWrapper peerServerWrapper = new PeerServerWrapper(peerInfo.getHostName(), peerInfo.getPort());
            BeansUtil.addBean(PeerServerWrapper.class, peerServerWrapper);
            executorService.execute(new PeerServer(id, peerServerWrapper, countDownLatch));
        }
        catch (Exception e)
        {
            LOGGER.error("Error while starting the peer server", e);
            System.exit(0);
        }
    }

    public static void initScheduledHandlers(int id)
    {
        CommonConfig commonConfig = BeansUtil.getBean(CommonConfig.class);
        ScheduledExecutorService scheduler = BeansUtil.getBean(ScheduledExecutorService.class);

        scheduler.scheduleAtFixedRate(new NeighborsHandler(id), 0L, commonConfig.getUnchokingInterval(), TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(new OptimisticNeighborHandler(), 0L, commonConfig.getOptimisticUnchokingInterval(), TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(new RequestedPiecesHandler(), 0L, 30, TimeUnit.SECONDS);
    }
}
