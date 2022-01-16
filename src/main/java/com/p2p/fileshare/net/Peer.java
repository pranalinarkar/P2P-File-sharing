package com.p2p.fileshare.net;

import com.p2p.fileshare.config.BeansUtil;
import com.p2p.fileshare.constant.Constants;
import com.p2p.fileshare.constant.MessageType;
import com.p2p.fileshare.dto.PeersConfig;
import com.p2p.fileshare.handler.MessageSender;
import com.p2p.fileshare.util.CommonUtil;
import com.p2p.fileshare.wrapper.BitfieldWrapper;
import com.p2p.fileshare.wrapper.FileDataWrapper;
import com.p2p.fileshare.wrapper.PeerServerWrapper;
import com.p2p.fileshare.wrapper.PeersWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;

public class Peer implements Runnable
{
    private final int selfId;
    private int peerId;

    private final Socket socket;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final CountDownLatch countDownLatch;

    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;
    private final BitfieldWrapper bitfieldWrapper;
    private final PeersWrapper peersWrapper;
    private final FileDataWrapper fileDataWrapper;

    private boolean handshakeInitiated = false;

    private BitSet bitfield = null;
    private boolean isChoked = true;

    private static final Logger LOGGER = LogManager.getLogger(Peer.class);

    public Peer(int selfId, int peerId, Socket socket, CountDownLatch countDownLatch) throws IOException
    {
        this.selfId = selfId;
        this.peerId = peerId;

        this.socket = socket;
        this.inputStream = socket.getInputStream();
        this.outputStream = socket.getOutputStream();
        this.countDownLatch = countDownLatch;

        this.executorService = BeansUtil.getBean(ExecutorService.class);
        this.scheduledExecutorService = BeansUtil.getBean(ScheduledExecutorService.class);
        this.bitfieldWrapper = BeansUtil.getBean(Constants.BITFIELD, BitfieldWrapper.class);
        this.peersWrapper = BeansUtil.getBean(PeersWrapper.class);
        this.fileDataWrapper = BeansUtil.getBean(FileDataWrapper.class);
    }

    public Peer(int id, Socket socket, CountDownLatch countDownLatch) throws IOException
    {
        this(id, id, socket, countDownLatch);
    }

    @Override
    public void run()
    {
        if (selfId != peerId)
        {
            try
            {
                sendHandshake();
                handshakeInitiated = true;
            }
            catch (Exception e)
            {
                LOGGER.error("Error while sending handshake to peer {}", peerId, e);
                return;
            }
        }

        try
        {
            completeHandshake();
        }
        catch (Exception e)
        {
            LOGGER.error("Error while completing handshake", e);
            return;
        }

        peersWrapper.addPeerSocket(peerId, socket);

        try
        {
            while (true)
            {
                if (Thread.currentThread().isInterrupted())
                {
                    inputStream.close();
                    outputStream.close();
                    socket.close();
                    countDownLatch.countDown();
                    break;
                }

                byte[] messageData = inputStream.readNBytes(5);

                if (messageData.length > 0)
                {
                    int messageLength = CommonUtil.byteArrToInt(Arrays.copyOfRange(messageData, 0, 4));
                    MessageType messageType = MessageType.getByValue(messageData[4]);

                    if (Objects.nonNull(messageType))
                    {
                        switch (messageType)
                        {
                            case BITFIELD:
                                acceptBitfield(messageLength);
                                break;
                            case INTERESTED:
                                addInterestedPeer();
                                break;
                            case UNCHOKE:
                                handleUnchoking();
                                break;
                            case CHOKE:
                                handleChoking();
                                break;
                            case REQUEST:
                                handlePieceRequest(messageLength);
                                break;
                            case PIECE:
                                handlePieceResponse(messageLength);
                                break;
                            case NOT_INTERESTED:
                                handleNotInterestedMessage();
                                break;
                            case HAVE:
                                handleHaveMessage(messageLength);
                                break;
                            case DONE:
                                handleDoneMessage();
                                break;
                        }
                    }
                }
                else
                {
                    handleDoneMessage();
                }
            }
        }
        catch (SocketException e)
        {
            countDownLatch.countDown();
        }
        catch (RejectedExecutionException e)
        {
            LOGGER.error("Error while sending message through executor service. It might have been shutdown.");
        }
        catch (Exception e)
        {
            LOGGER.error("Error while communicating with peer {}", peerId, e);
        }
    }

    private void sendHandshake() throws Exception
    {
        LOGGER.info("[{}] Peer {} sent handshake message to Peer {}", CommonUtil.getCurrentTime(), selfId, peerId);
        outputStream.write(CommonUtil.getHandshakeMessage(selfId));
    }

    private void completeHandshake() throws Exception
    {
        byte[] handshakeResponse = inputStream.readNBytes(32);
        String responseHeader = new String(Arrays.copyOfRange(handshakeResponse, 0, 18), StandardCharsets.UTF_8);
        int receivedId = CommonUtil.byteArrToInt(Arrays.copyOfRange(handshakeResponse, 28, 32));

        if(receivedId != selfId)
        {
            LOGGER.info("[{}] Peer {} received handshake message from Peer {}", CommonUtil.getCurrentTime(), selfId, peerId);
        }

        if (!Constants.HANDSHAKE_HEADER.equals(responseHeader))
        {
            throw new IllegalArgumentException(String.format("Invalid handshake header received %s from %d", responseHeader, receivedId));
        }

        if (handshakeInitiated)
        {
            if (receivedId != peerId)
            {
                throw new IllegalArgumentException(String.format("Invalid peer id received %d when requested to %d", receivedId, peerId));
            }

            LOGGER.info("[{}]: Peer [{}] makes a connection to Peer [{}].", CommonUtil.getCurrentTime(), selfId, peerId);
        }
        else
        {
            sendHandshake();
            peerId = receivedId;
            LOGGER.info("[{}]: Peer [{}] is connected from Peer [{}].", CommonUtil.getCurrentTime(), selfId, peerId);
        }

        sendBitfield();

        if (bitfieldWrapper.allPiecesReceived())
        {
            executorService.execute(new MessageSender(
                    outputStream,
                    CommonUtil.getMessage(0, MessageType.DONE, null)
            ));
        }
    }

    private void sendBitfield() throws Exception
    {
        try
        {
            bitfieldWrapper.readLock();
            byte[] bitfieldData = bitfieldWrapper.getBitfield().toByteArray();
            outputStream.write(CommonUtil.getMessage(bitfieldData.length, MessageType.BITFIELD, bitfieldData));
        }
        finally
        {
            bitfieldWrapper.readUnlock();
        }
    }

    private void acceptBitfield(int messageLength) throws Exception
    {
        bitfield = BitSet.valueOf(inputStream.readNBytes(messageLength));
        peersWrapper.addPeerBitfield(peerId, bitfield);
        sendIfInterested();

        if(bitfield.nextClearBit(0) != 0)
        {
            LOGGER.info("[{}]: Peer {} received bitfield from {}.", CommonUtil.getCurrentTime(), selfId, peerId);
        }
    }

    private void sendIfInterested()
    {
        if (bitfieldWrapper.isInterested(bitfield))
        {
            executorService.execute(
                    new MessageSender(
                            outputStream,
                            CommonUtil.getMessage(0, MessageType.INTERESTED, null)
                    )
            );
        }
    }

    private void addInterestedPeer()
    {
        LOGGER.info("[{}]: Peer {} received the 'interested' message from {}.", CommonUtil.getCurrentTime(), selfId, peerId);
        peersWrapper.addInterestedPeer(peerId);
    }

    private void handleUnchoking()
    {
        LOGGER.info("[{}]: Peer {} is unchoked by {}.", CommonUtil.getCurrentTime(), selfId, peerId);
        isChoked = false;
        sendPieceRequest();
    }

    private void handleChoking()
    {
        LOGGER.info("[{}]: Peer {} is choked by {}.", CommonUtil.getCurrentTime(), selfId, peerId);
        isChoked = true;
    }

    private void sendPieceRequest()
    {
        if (!isChoked)
        {
            int nextPieceIndex = bitfieldWrapper.getNextInterestedPieceIndex(bitfield);

            if (nextPieceIndex != -1)
            {
                executorService.execute(new MessageSender(
                        outputStream,
                        CommonUtil.getMessage(4, MessageType.REQUEST, CommonUtil.intToByteArr(nextPieceIndex))
                ));
            }
            else
            {
                sendNotInterestedMessage();
            }
        }
    }

    private void sendNotInterestedMessage()
    {
        executorService.execute(new MessageSender(
                outputStream,
                CommonUtil.getMessage(0, MessageType.NOT_INTERESTED, null)
        ));
    }

    private void handlePieceRequest(int messageLength) throws IOException
    {
        int pieceIndex = CommonUtil.byteArrToInt(inputStream.readNBytes(messageLength));

        LOGGER.info("[{}]: Peer {} received REQUEST for piece {} from peer {}", CommonUtil.getCurrentTime(), selfId, peerId, pieceIndex);

        if (peersWrapper.isUnchoked(peerId))
        {
            byte[] pieceData = fileDataWrapper.getFilePiece(pieceIndex);
            byte[] pieceResponse = new byte[4 + pieceData.length];

            int counter = CommonUtil.mergeByteArrays(pieceResponse, CommonUtil.intToByteArr(pieceIndex), 0);
            CommonUtil.mergeByteArrays(pieceResponse, pieceData, counter);

            executorService.execute(new MessageSender(
                    outputStream,
                    CommonUtil.getMessage(pieceData.length + 4, MessageType.PIECE, pieceResponse)
            ));
        }
    }

    private void handlePieceResponse(int messageLength) throws IOException
    {
        int pieceIndex = CommonUtil.byteArrToInt(inputStream.readNBytes(4));
        byte[] pieceData = inputStream.readNBytes(messageLength - 4);
        fileDataWrapper.savePiece(pieceIndex, pieceData);
        bitfieldWrapper.removeReceivedPieceIndex(pieceIndex);
        broadcastHaveMessage(pieceIndex);

        LOGGER.info(
                "[{}]: Peer {} has downloaded the piece {} from {}. Now the number of pieces it has is {}.",
                CommonUtil.getCurrentTime(), selfId, pieceIndex, peerId, bitfieldWrapper.getSetPiecesCount());

        if (bitfieldWrapper.allPiecesReceived())
        {
            fileDataWrapper.joinAndSavePieces();
            LOGGER.info("[{}]: Peer {} has downloaded the complete file.", CommonUtil.getCurrentTime(), selfId);
            sendNotInterestedMessage();
            broadcastDoneMessage();
        }
        else
        {
            sendPieceRequest();
        }
    }

    private void handleNotInterestedMessage()
    {
        LOGGER.info("[{}]: Peer {} received the 'not interested' message from {}.", CommonUtil.getCurrentTime(), selfId, peerId);
        peersWrapper.removeInterestedPeer(peerId);
    }

    private void broadcastHaveMessage(int pieceIndex) throws IOException
    {
        for (Socket socket : peersWrapper.getPeerSockets().values())
        {
            executorService.execute(new MessageSender(
                    socket.getOutputStream(),
                    CommonUtil.getMessage(4, MessageType.HAVE, CommonUtil.intToByteArr(pieceIndex))
            ));
        }
    }

    private void broadcastDoneMessage() throws IOException
    {
        for (Socket socket : peersWrapper.getPeerSockets().values())
        {
            executorService.execute(new MessageSender(
                    socket.getOutputStream(),
                    CommonUtil.getMessage(0, MessageType.DONE, null)
            ));
        }
    }

    private void handleHaveMessage(int messageLength) throws IOException
    {
        int pieceIndex = CommonUtil.byteArrToInt(inputStream.readNBytes(messageLength));
        bitfield.set(pieceIndex);

        LOGGER.info(
                "[{}]: Peer {} received the 'have' message from {} for the piece {}.",
                CommonUtil.getCurrentTime(), selfId, peerId, pieceIndex
        );

        sendIfInterested();
    }

    private void handleDoneMessage() throws IOException
    {
        peersWrapper.addCompletedPeer(peerId);

        if (peersWrapper.allPeersDone())
        {
            LOGGER.info("[{}]: All peers have successfully downloaded the file. Shutting down the service.", CommonUtil.getCurrentTime());
            fileDataWrapper.deleteTempDir();
            executorService.shutdownNow();
            scheduledExecutorService.shutdownNow();
            BeansUtil.getBean(PeerServerWrapper.class).getServerSocket().close();
            peersWrapper.closeSocket(peerId);
        }
    }
}
