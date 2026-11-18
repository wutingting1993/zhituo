package template.base.infrastructure.common.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generates compact IDs using four bytes of time, two bytes of sequence, and
 * two bytes derived from the local IPv4 address.
 */
public final class CUuid {
    private static final LocalDateTime BEGIN_TIME = LocalDateTime.of(2000, 1, 1, 8, 0, 0);

    private static final Random RANDOM = new Random();

    private static final int SEQUENCE_INITIAL = RANDOM.nextInt(60_000);

    private static final int SEQUENCE_LIMIT = 65_536;

    private static final Lock LOCK = new ReentrantLock();

    private static final byte[] MACHINE_ID = new byte[2];

    private static final Logger LOGGER = Logger.getLogger(CUuid.class.getName());

    private static long lastIdTime;

    private static int sequence;

    static {
        try {
            byte[] ipAddress = getIpAddress();
            MACHINE_ID[0] = ipAddress[2];
            MACHINE_ID[1] = ipAddress[3];
        } catch (RuntimeException | SocketException exception) {
            MACHINE_ID[0] = (byte) RANDOM.nextInt(256);
            MACHINE_ID[1] = (byte) RANDOM.nextInt(256);
            LOGGER.log(Level.WARNING, "Cannot resolve a usable IPv4 address; using random machine bytes", exception);
        }
    }

    /**
     * 限制业务标识符的直接构造，使用公开业务入口。
     */
    private CUuid() {
    }

    /**
     * 生成业务唯一标识符，保留既有时间与机器编码格式。
     */
    public static String nextId() {
        long time = getTime();
        int currentSequence;
        LOCK.lock();
        try {
            if (time <= lastIdTime) {
                time = lastIdTime;
                if (sequence >= SEQUENCE_LIMIT - 1) {
                    time++;
                    sequence = SEQUENCE_INITIAL;
                    lastIdTime = time;
                } else {
                    sequence++;
                }
            } else {
                lastIdTime = time;
                sequence = SEQUENCE_INITIAL;
            }
            currentSequence = sequence;
        } finally {
            LOCK.unlock();
        }
        return HexEncoding.encode(new byte[]{(byte) ((time >> 24) & 0xff), (byte) ((time >> 16) & 0xff), (byte) ((time >> 8) & 0xff),
                (byte) (time & 0xff), (byte) ((currentSequence >> 8) & 0xff), (byte) (currentSequence & 0xff), MACHINE_ID[0], MACHINE_ID[1]});
    }

    /**
     * 读取业务标识符的 Time 属性。
     */
    private static long getTime() {
        return Duration.between(BEGIN_TIME, LocalDateTime.now()).getSeconds();
    }

    /**
     * 读取业务标识符的 IpAddress 属性。
     */
    private static byte[] getIpAddress() throws SocketException {
        Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
        while (networks != null && networks.hasMoreElements()) {
            NetworkInterface network = networks.nextElement();
            Enumeration<InetAddress> addresses = network.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                if (address instanceof Inet4Address && !address.isLoopbackAddress() && !address.isLinkLocalAddress()
                        && !address.isMulticastAddress()) {
                    return address.getAddress();
                }
            }
        }
        throw new IllegalStateException("No usable IPv4 address found");
    }
}
