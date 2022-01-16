package com.p2p.fileshare.dto;

import java.util.*;

public class PeersConfig
{
    private Map<Integer, PeerInfo> peers = new LinkedHashMap<>();

    public void addPeer(String id, String hostName, String port, String hasFile)
    {
        peers.put(Integer.parseInt(id), new PeerInfo(id, hostName, port, hasFile));
    }

    public PeerInfo getPeer(int id)
    {
        return peers.get(id);
    }

    public List<PeerInfo> getPeers()
    {
        return new ArrayList<>(peers.values());
    }

    public static class PeerInfo
    {
        private int id;
        private String hostName;
        private int port;
        private boolean hasFile;

        public PeerInfo(String id, String hostName, String port, String hasFile)
        {
            this.id = Integer.parseInt(id);
            this.hostName = hostName;
            this.port = Integer.parseInt(port);
            this.hasFile = Integer.parseInt(hasFile) == 1;
        }

        public int getId()
        {
            return id;
        }

        public String getHostName()
        {
            return hostName;
        }

        public int getPort()
        {
            return port;
        }

        public boolean isHasFile()
        {
            return hasFile;
        }

        public void setId(int id)
        {
            this.id = id;
        }

        public void setHostName(String hostName)
        {
            this.hostName = hostName;
        }

        public void setPort(int port)
        {
            this.port = port;
        }

        public void setHasFile(boolean hasFile)
        {
            this.hasFile = hasFile;
        }

        @Override
        public String toString()
        {
            return String.format("[id=%d, hostName=%s, port=%d, hasFile=%b]", id, hostName, port, hasFile);
        }
    }
}
