package com.p2p.fileshare.wrapper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

public class PeerServerWrapper
{
    private final ServerSocket serverSocket;

    public PeerServerWrapper(String hostName, int port) throws IOException
    {
        serverSocket = new ServerSocket(port, 50, InetAddress.getByName(hostName));
    }

    public ServerSocket getServerSocket()
    {
        return serverSocket;
    }
}
