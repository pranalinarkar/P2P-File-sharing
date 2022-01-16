package com.p2p.fileshare.handler;

import com.p2p.fileshare.config.BeansUtil;
import com.p2p.fileshare.constant.MessageType;
import com.p2p.fileshare.util.CommonUtil;
import com.p2p.fileshare.wrapper.PeersWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutorService;

public class OptimisticNeighborHandler implements Runnable
{
    private final PeersWrapper peersWrapper;
    private final ExecutorService executorService;
    private final Random random;

    private static final Logger LOGGER = LogManager.getLogger(OptimisticNeighborHandler.class);

    public OptimisticNeighborHandler()
    {
        this.peersWrapper = BeansUtil.getBean(PeersWrapper.class);
        this.executorService = BeansUtil.getBean(ExecutorService.class);
        this.random = new Random();
    }

    @Override
    public void run()
    {
        if (Thread.currentThread().isInterrupted())
        {
            return;
        }

        if (peersWrapper.getChockedNeighbors().size() > 0)
        {
            List<Integer> chokedNeighbors = new ArrayList<>(peersWrapper.getChockedNeighbors());
            int newOptimisticNeighbor = chokedNeighbors.get(random.nextInt(chokedNeighbors.size()));
            peersWrapper.getChockedNeighbors().remove(newOptimisticNeighbor);
            peersWrapper.setOptimisticNeighbor(newOptimisticNeighbor);

            Optional.ofNullable(peersWrapper.getPeerSocket(newOptimisticNeighbor)).ifPresent(socket -> {
                try
                {
                    executorService.execute(new MessageSender(
                            socket.getOutputStream(),
                            CommonUtil.getMessage(0, MessageType.UNCHOKE, null)
                        )
                    );
                }
                catch (IOException e)
                {
                    LOGGER.error("Error while re selecting unchoked neighbor", e);
                }
            });
        }
    }
}
