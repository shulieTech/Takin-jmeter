package org.apache.jmeter.shulie.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import org.apache.commons.lang3.StringUtils;

/**
 * TraceId生成器
 *
 */
public class TraceIdGenerator {

    private static String IP_16 = "ffffffff";
    private static final String LOCAL_IP_ADDRESS = getLocalInetAddress();

    /**
     * 根据本机ip和当前线程序号生成TraceId
     *
     * @param currentThreadNum 线程序号
     *
     * @return
     */
    public static String generateTraceId(int currentThreadNum) {
        // add by lipeng  jmeter每个pod启动一个实例  ip不变  所以如果IP_16算好了 就不用每次进行计算了
        StringBuilder appender = new StringBuilder(30);
        return appender.append(IP_16)
                .append(System.currentTimeMillis())
                .append(StringUtils.leftPad(currentThreadNum+"", 9, '0'))
                .toString();
    }

    static {
        try {
            String ipAddress = TraceIdGenerator.getLocalAddress();
            if (ipAddress != null) {
                IP_16 = getIP_16(ipAddress);
            }
        } catch (Throwable e) {
        }
    }

    public static String getLocalAddress() {
        return LOCAL_IP_ADDRESS;
    }

    private static String getLocalInetAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress address = null;
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    address = addresses.nextElement();
                    if (!address.isLoopbackAddress() && address.getHostAddress().indexOf(":") == -1) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Throwable t) {
        }
        return "127.0.0.1";
    }

    private static String getIP_16(String ip) {
        String[] ips = StringUtils.split(ip, '.');
        StringBuilder sb = new StringBuilder();
        for (int i = ips.length - 1; i >= 0; --i) {
            String hex = Integer.toHexString(Integer.parseInt(ips[i]));
            if (hex.length() == 1) {
                sb.append('0').append(hex);
            } else {
                sb.append(hex);
            }

        }
        return sb.toString();
    }

    public boolean isTraceSampled(String traceId) {
        int si = 100;
        if (traceId == null) {
            return false;
        }

        if (traceId.length() < 25) {
            return traceId.hashCode() % si == 0;
        }
        int count = traceId.charAt(21) - '0';
        count = count * 10 + traceId.charAt(22) - '0';
        count = count * 10 + traceId.charAt(23) - '0';
        count = count * 10 + traceId.charAt(24) - '0';
        return count % si == 0;
    }

}
