package com.p2p.fileshare.dto;

public class CommonConfig
{
    private int preferredNeighborsCount = 0;
    private int unchokingInterval = 0;
    private int optimisticUnchokingInterval = 0;
    private String fileName = "";
    private int pieceSize = 0;
    private int fileSize = 0;
    private int piecesCount = 0;

    public CommonConfig()
    {

    }

    public CommonConfig(int preferredNeighborsCount, int unchockingInterval, int optimisticUnchockingInterval, String fileName, int pieceSize)
    {
        this.preferredNeighborsCount = preferredNeighborsCount;
        this.unchokingInterval = unchockingInterval;
        this.optimisticUnchokingInterval = optimisticUnchockingInterval;
        this.fileName = fileName;
        this.pieceSize = pieceSize;
        this.piecesCount = (int) Math.ceil((float) fileSize/pieceSize);
    }

    public int getPreferredNeighborsCount()
    {
        return preferredNeighborsCount;
    }

    public int getUnchokingInterval()
    {
        return unchokingInterval;
    }

    public int getOptimisticUnchokingInterval()
    {
        return optimisticUnchokingInterval;
    }

    public String getFileName()
    {
        return fileName;
    }

    public int getPieceSize()
    {
        return pieceSize;
    }

    public int getFileSize()
    {
        return fileSize;
    }

    public int getPiecesCount()
    {
        return piecesCount;
    }

    public void setPreferredNeighborsCount(int preferredNeighborsCount)
    {
        this.preferredNeighborsCount = preferredNeighborsCount;
    }

    public void setUnchokingInterval(int unchokingInterval)
    {
        this.unchokingInterval = unchokingInterval;
    }

    public void setOptimisticUnchokingInterval(int optimisticUnchokingInterval)
    {
        this.optimisticUnchokingInterval = optimisticUnchokingInterval;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public void setPieceSize(int pieceSize)
    {
        this.pieceSize = pieceSize;
    }

    public void setFileSize(int fileSize)
    {
        this.fileSize = fileSize;
    }

    public void postConstruct()
    {
        this.piecesCount = (int) Math.ceil(((float) fileSize)/pieceSize);
    }

    @Override
    public String toString()
    {
        return "[NumberOfPreferredNeighbors: " + preferredNeighborsCount + ", " +
                "UnchokingInterval: " + unchokingInterval + ", " +
                "OptimisticUnchokingInterval: " + optimisticUnchokingInterval + ", " +
                "FileName: " + fileName + ", " +
                "FileSize: " + pieceSize + ", " +
                "PieceSize: " + fileSize + "]";
    }
}
