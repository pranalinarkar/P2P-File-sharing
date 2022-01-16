package com.p2p.fileshare.config;

import com.p2p.fileshare.constant.Constants;
import com.p2p.fileshare.dto.CommonConfig;
import com.p2p.fileshare.dto.MethodInvokeHelper;
import com.p2p.fileshare.dto.PeersConfig;
import com.p2p.fileshare.util.CommonUtil;
import com.p2p.fileshare.wrapper.BitfieldWrapper;
import com.p2p.fileshare.wrapper.FileDataWrapper;
import com.p2p.fileshare.wrapper.PeersWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.p2p.fileshare.constant.Constants.COMMON_CONFIG_PROPERTIES;

public class BeansUtil
{
    private static final Map<Class<?>, Object> beansMap = new HashMap<>();
    private static final Map<String, Object> namedBeansMap = new HashMap<>();

    private static final Logger LOGGER = LogManager.getLogger(BeansUtil.class);

    public static void init(int id)
    {
        initConfig();
        initBitfield(id);
        initPeersWrapper(id);
        initFileDataWrapper(id);
        beansMap.put(ExecutorService.class, Executors.newFixedThreadPool(16));
        beansMap.put(ScheduledExecutorService.class, Executors.newScheduledThreadPool(5));
    }

    private static void initConfig()
    {
        beansMap.put(CommonConfig.class, getCommonConfig());
        beansMap.put(PeersConfig.class, getPeersConfig());
    }

    public static <T> T getBean(Class<T> clazz)
    {
        return clazz.cast(beansMap.get(clazz));
    }

    public static <T> T getBean(String beanName, Class<T> beanClass)
    {
        return beanClass.cast(namedBeansMap.get(beanName));
    }

    private static CommonConfig getCommonConfig()
    {
        try (
                BufferedReader reader = CommonUtil.getReaderFromFile(
                        System.getProperty(Constants.WORKING_DIR) + File.separator + Constants.COMMON_CFG_FILE
                )
        )
        {
            CommonConfig commonConfig = new CommonConfig();
            String line = reader.readLine();

            while (Objects.nonNull(line))
            {
                String[] lineSplit = line.split(" ");

                MethodInvokeHelper invokeHelper = COMMON_CONFIG_PROPERTIES.get(lineSplit[0]);
                commonConfig.getClass()
                        .getMethod(invokeHelper.getMethodName(), invokeHelper.getParams())
                        .invoke(
                                commonConfig,
                                Objects.nonNull(invokeHelper.getParser()) ? invokeHelper.getParser().apply(lineSplit[1]) : lineSplit[1]
                        );

                line = reader.readLine();
            }

            commonConfig.postConstruct();
            LOGGER.info("Loaded Common Config: {}", commonConfig.toString());
            return commonConfig;
        }
        catch (IOException e)
        {
            LOGGER.error("Error while processing Common.cfg file", e);
            System.exit(0);
        }
        catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e)
        {
            LOGGER.error("Error while populating common config", e);
            System.exit(0);
        }

        return null;
    }

    private static PeersConfig getPeersConfig()
    {
        try(
            BufferedReader reader = CommonUtil.getReaderFromFile(
                    System.getProperty(Constants.WORKING_DIR) + File.separator + Constants.PEER_CFG_FILE
            )
        )
        {
            PeersConfig peersConfig = new PeersConfig();
            String line = reader.readLine();

            while (Objects.nonNull(line))
            {
                Object[] lineSplit = line.split(" ");
                peersConfig.getClass()
                        .getMethod("addPeer", String.class, String.class, String.class, String.class)
                        .invoke(peersConfig, lineSplit);

                line = reader.readLine();
            }

            for (PeersConfig.PeerInfo  peerInfo : peersConfig.getPeers())
            {
                LOGGER.info("Loaded peer info: {}", peerInfo.toString());
            }

            return peersConfig;
        }
        catch (IOException e)
        {
            LOGGER.error("Error while reading PeerInfo.cfg file", e);
            System.exit(0);
        }
        catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e)
        {
            LOGGER.error("Error while populating peer config", e);
            System.exit(0);
        }

        return null;
    }

    private static void initBitfield(int id)
    {
        CommonConfig commonConfig = getBean(CommonConfig.class);

        BitSet bitSet = new BitSet(commonConfig.getPiecesCount());

        if (getBean(PeersConfig.class).getPeer(id).isHasFile())
        {
            bitSet.set(0, commonConfig.getPiecesCount());
        }

        namedBeansMap.put(Constants.BITFIELD, new BitfieldWrapper(bitSet));
    }

    private static void initPeersWrapper(int id)
    {
        beansMap.put(PeersWrapper.class, new PeersWrapper(id));
    }

    private static void initFileDataWrapper(int id)
    {
        FileDataWrapper fileDataWrapper = new FileDataWrapper(id);
        beansMap.put(FileDataWrapper.class, fileDataWrapper);

        if (getBean(PeersConfig.class).getPeer(id).isHasFile())
        {
            try
            {
                fileDataWrapper.breakFileAndSavePieces();
            }
            catch (Exception e)
            {
                LOGGER.error("Error while processing the file", e);
                System.exit(0);
            }
        }
    }

    public static void addBean(Class<?> beanClass, Object beanInstance)
    {
        beansMap.put(beanClass, beanInstance);
    }
}
