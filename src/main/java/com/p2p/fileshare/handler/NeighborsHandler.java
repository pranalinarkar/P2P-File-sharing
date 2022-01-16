package com.p2p.fileshare.handler;

import com.p2p.fileshare.config.BeansUtil;
import com.p2p.fileshare.constant.MessageType;
import com.p2p.fileshare.util.CommonUtil;
import com.p2p.fileshare.wrapper.PeersWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public class NeighborsHandler implements Runnable
{
    private final int id;
    private final PeersWrapper peersWrapper;
    private final ExecutorService executorService;

    private final Logger LOGGER = LogManager.getLogger(NeighborsHandler.class);

    public NeighborsHandler(int id)
    {
        this.id = id;
        peersWrapper = BeansUtil.getBean(PeersWrapper.class);
        executorService = BeansUtil.getBean(ExecutorService.class);
    }

    @Override
    public void run()
    {
        if (Thread.currentThread().isInterrupted())
        {
            return;
        }

        peersWrapper.reselectNeighbors();

        if (peersWrapper.getUnchokedNeighbors().size() > 0)
        {
            sendChokeUnchokeMessage(peersWrapper.getUnchokedNeighbors(), MessageType.UNCHOKE);

            LOGGER.info(
                    "[{}]: Peer {} has the preferred neighbors {}.",
                    CommonUtil.getCurrentTime(), id, peersWrapper.getUnchokedNeighbors().stream().map(String::valueOf).collect(Collectors.joining(", "))
            );
        }

        if (peersWrapper.getChockedNeighbors().size() > 0)
        {
            sendChokeUnchokeMessage(peersWrapper.getChockedNeighbors(), MessageType.CHOKE);
        }

        int optimisticNeighbor = peersWrapper.getOptimisticNeighbor();

        if (optimisticNeighbor != -1)
        {
            sendChokeUnchokeMessage(Collections.singleton(optimisticNeighbor), MessageType.UNCHOKE);
        }
    }

    private void sendChokeUnchokeMessage(Set<Integer> neighbors, MessageType messageType)
    {
        for (int id : neighbors)
        {
            Optional.ofNullable(peersWrapper.getPeerSocket(id)).ifPresent(socket -> {
                try
                {
                    executorService.execute(new MessageSender(socket.getOutputStream(), CommonUtil.getMessage(0, messageType, null)));
                }
                catch (Exception e)
                {
                    LOGGER.error("Error while sending {} message to {}", messageType.name(), id, e);
                }
            });
        }
    }
}
