package com.p2p.fileshare.wrapper;

import com.p2p.fileshare.config.BeansUtil;
import com.p2p.fileshare.dto.CommonConfig;
import com.p2p.fileshare.dto.PieceIndex;
import com.p2p.fileshare.util.CommonUtil;

import java.util.BitSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BitfieldWrapper
{
    private final BitSet bitfield;
    private final int piecesCount;
    private final Set<Integer> requestedPieces = ConcurrentHashMap.newKeySet();
    private final DelayQueue<PieceIndex> delayQueue = new DelayQueue<>();

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();

    public BitfieldWrapper(BitSet bitfield)
    {
        this.bitfield = bitfield;
        this.piecesCount = BeansUtil.getBean(CommonConfig.class).getPiecesCount();
    }

    public BitSet getBitfield()
    {
        return bitfield;
    }

    private int getNextClearIndex(BitSet peerBitField)
    {
        int index = -1;

        do
        {
            index = CommonUtil.getFirstClearIndexFromBitfields(bitfield, peerBitField, index+1);
        }
        while (requestedPieces.contains(index));

        return index;
    }

    public boolean isInterested(BitSet peerBitField)
    {
        try
        {
            readLock();
            return getNextClearIndex(peerBitField) != -1;
        }
        finally
        {
            readUnlock();
        }
    }

    public int getNextInterestedPieceIndex(BitSet peerBitField)
    {
        try
        {
            readLock();
            int nextPieceIndex = getNextClearIndex(peerBitField);

            if (nextPieceIndex != -1)
            {
                requestedPieces.add(nextPieceIndex);
                delayQueue.add(new PieceIndex(nextPieceIndex));
            }

            return nextPieceIndex;
        }
        finally
        {
            readUnlock();
        }
    }

    public void removeReceivedPieceIndex(int pieceIndex)
    {
        try
        {
            writeLock();
            bitfield.set(pieceIndex);
            requestedPieces.remove(pieceIndex);
        }
        finally
        {
            writeUnlock();
        }
    }

    public void removeTimedOutPieceIndex(int pieceIndex)
    {
        requestedPieces.remove(pieceIndex);
    }

    public int getSetPiecesCount()
    {
        return bitfield.cardinality();
    }

    public DelayQueue<PieceIndex> getDelayQueue()
    {
        return delayQueue;
    }

    public boolean allPiecesReceived()
    {
        int nextClearIndex = bitfield.nextClearBit(0);
        return nextClearIndex == -1 || nextClearIndex >= piecesCount;
    }

    public void readLock()
    {
        readLock.lock();
    }

    public void readUnlock()
    {
        readLock.unlock();
    }

    public void writeLock()
    {
        writeLock.lock();
    }

    public void writeUnlock()
    {
        writeLock.unlock();
    }
}
