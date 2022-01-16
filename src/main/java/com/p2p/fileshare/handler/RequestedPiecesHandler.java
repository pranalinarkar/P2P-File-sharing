package com.p2p.fileshare.handler;

import com.p2p.fileshare.config.BeansUtil;
import com.p2p.fileshare.constant.Constants;
import com.p2p.fileshare.constant.MessageType;
import com.p2p.fileshare.dto.PieceIndex;
import com.p2p.fileshare.util.CommonUtil;
import com.p2p.fileshare.wrapper.BitfieldWrapper;
import com.p2p.fileshare.wrapper.PeersWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.BitSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;

public class RequestedPiecesHandler implements Runnable
{
    private final BitfieldWrapper bitfieldWrapper;
    private final PeersWrapper peersWrapper;
    private final ExecutorService executorService;

    private static final Logger LOGGER = LogManager.getLogger(RequestedPiecesHandler.class);

    public RequestedPiecesHandler()
    {
        bitfieldWrapper = BeansUtil.getBean(Constants.BITFIELD, BitfieldWrapper.class);
        peersWrapper = BeansUtil.getBean(PeersWrapper.class);
        executorService = BeansUtil.getBean(ExecutorService.class);
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

            DelayQueue<PieceIndex> requestedPieces = bitfieldWrapper.getDelayQueue();
            PieceIndex expiredPieceIndex;

            while (Objects.nonNull(expiredPieceIndex = requestedPieces.poll()))
            {
                bitfieldWrapper.removeTimedOutPieceIndex(expiredPieceIndex.getIndex());

                for (Map.Entry<Integer, BitSet> bitfieldEntry : peersWrapper.getPeerBitfields().entrySet())
                {
                    if (bitfieldEntry.getValue().get(expiredPieceIndex.getIndex()))
                    {
                        executorService.execute(new MessageSender(
                                peersWrapper.getPeerSocket(bitfieldEntry.getKey()).getOutputStream(),
                                CommonUtil.getMessage(0, MessageType.INTERESTED, null)
                            )
                        );
                    }
                }
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Error while processing requested pieces", e);
        }
    }
}
