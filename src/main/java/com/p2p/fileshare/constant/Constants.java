package com.p2p.fileshare.constant;

import com.p2p.fileshare.dto.MethodInvokeHelper;

import java.util.Collections;
import java.util.Map;

public class Constants
{
    //File name
    public static final String COMMON_CFG_FILE = "Common.cfg";
    public static final String PEER_CFG_FILE = "PeerInfo.cfg";

    //VM arg names
    public static final String WORKING_DIR = "working.dir";

    //Common.cfg properties
    public static final Map<String, MethodInvokeHelper> COMMON_CONFIG_PROPERTIES = Map.of(
            "NumberOfPreferredNeighbors", new MethodInvokeHelper("setPreferredNeighborsCount", new Class<?>[]{int.class}, Integer::parseInt),
            "UnchokingInterval", new MethodInvokeHelper("setUnchokingInterval", new Class<?>[]{int.class}, Integer::parseInt),
            "OptimisticUnchokingInterval", new MethodInvokeHelper("setOptimisticUnchokingInterval", new Class<?>[]{int.class}, Integer::parseInt),
            "FileName", new MethodInvokeHelper("setFileName", new Class<?>[]{String.class}, null),
            "FileSize", new MethodInvokeHelper("setFileSize", new Class<?>[]{int.class}, Integer::parseInt),
            "PieceSize", new MethodInvokeHelper("setPieceSize", new Class<?>[]{int.class}, Integer::parseInt)
    );

    //Headers
    public static final String HANDSHAKE_HEADER = "P2PFILESHARINGPROJ";

    //Bean names
    public static final String BITFIELD = "bitfield";

    //Common constants
    public static final String PEER = "peer_";
    public static final String TEMP = "temp";
    public static final String TEMP_EXT = ".tmp";
}
