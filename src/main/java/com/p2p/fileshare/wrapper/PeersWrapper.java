package com.p2p.fileshare.wrapper;

import com.p2p.fileshare.config.BeansUtil;
import com.p2p.fileshare.constant.Constants;
import com.p2p.fileshare.dto.CommonConfig;
import com.p2p.fileshare.dto.PeersConfig;

import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class PeersWrapper
{
    private final CommonConfig commonConfig;
    private final PeersConfig peersConfig;
    private final BitfieldWrapper bitfieldWrapper;
    private final int selfId;
    private final Set<Integer> interestedPeers = ConcurrentHashMap.newKeySet();
    private final Set<Integer> unchokedNeighbors = ConcurrentHashMap.newKeySet();
    private final Set<Integer> chockedNeighbors = ConcurrentHashMap.newKeySet();
    private final AtomicInteger optimisticNeighbor = new AtomicInteger(-1);
    private final Map<Integer, Socket> peerSockets = new ConcurrentHashMap<>();
    private final Map<Integer, BitSet> peerBitfields = new ConcurrentHashMap<>();
    private final Set<Integer> completedPeers = new HashSet<>();

    public PeersWrapper(int id)
    {
        commonConfig = BeansUtil.getBean(CommonConfig.class);
        peersConfig = BeansUtil.getBean(PeersConfig.class);
        bitfieldWrapper = BeansUtil.getBean(Constants.BITFIELD, BitfieldWrapper.class);
        selfId = id;
    }

    public Set<Integer> getChockedNeighbors()
    {
        return chockedNeighbors;
    }

    public Set<Integer> getUnchokedNeighbors()
    {
        return unchokedNeighbors;
    }

    public void addInterestedPeer(int peerId)
    {
        interestedPeers.add(peerId);
    }

    public void removeInterestedPeer(int peerId)
    {
        interestedPeers.remove(peerId);
    }

    public void reselectNeighbors()
    {
        unchokedNeighbors.clear();
        chockedNeighbors.clear();

        if (interestedPeers.size() > 0)
        {
            chockedNeighbors.addAll(interestedPeers);

            List<Integer> randomInterestedNeighbors = new ArrayList<>(interestedPeers);
            Collections.shuffle(randomInterestedNeighbors);

            for (int i=0; i<commonConfig.getPreferredNeighborsCount(); i++)
            {
                if (i >= randomInterestedNeighbors.size())
                {
                    break;
                }

                unchokedNeighbors.add(randomInterestedNeighbors.get(i));
                chockedNeighbors.remove(randomInterestedNeighbors.get(i));
            }
        }
    }

    public int getOptimisticNeighbor()
    {
        return optimisticNeighbor.get();
    }

    public void setOptimisticNeighbor(int optimisticNeighbor)
    {
        this.optimisticNeighbor.set(optimisticNeighbor);
    }

    public Set<Integer> getInterestedPeers()
    {
        return interestedPeers;
    }

    public void addPeerSocket(int id, Socket socket)
    {
        peerSockets.put(id, socket);
    }

    public Socket getPeerSocket(int id)
    {
        return peerSockets.get(id);
    }

    public Map<Integer, Socket> getPeerSockets()
    {
        return peerSockets;
    }

    public boolean isUnchoked(int id)
    {
        return unchokedNeighbors.contains(id) || optimisticNeighbor.get() == id;
    }

    public void addPeerBitfield(int peerId, BitSet bitfield)
    {
        peerBitfields.put(peerId, bitfield);
    }

    public Map<Integer, BitSet> getPeerBitfields()
    {
        return peerBitfields;
    }

    public void addCompletedPeer(int peerId)
    {
        completedPeers.add(peerId);
    }

    public synchronized boolean allPeersDone()
    {
        Set<Integer> peerIds = peersConfig.getPeers().stream().map(PeersConfig.PeerInfo::getId).collect(Collectors.toSet());
        peerIds.removeAll(completedPeers);

        if (bitfieldWrapper.allPiecesReceived())
        {
            peerIds.remove(selfId);
        }

        return peerIds.size() == 0;
    }

    public void closeSocket(int peerId) throws IOException
    {
        peerSockets.get(peerId).close();
    }
}
