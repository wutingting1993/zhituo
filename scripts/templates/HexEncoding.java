package template.base.infrastructure.common.util;

/**
 * 将业务摘要字节编码为小写十六进制，保持原有摘要和标识符格式。
 */
public final class HexEncoding {
    private static final char[] DIGITS = "0123456789abcdef".toCharArray();

    /**
     * 限制十六进制编码的直接构造，使用公开业务入口。
     */
    private HexEncoding() {
    }

    /**
     * 每个字节固定输出两个字符，保留前导零。
     */
    public static String encode(byte[] bytes) {
        char[] output = new char[bytes.length * 2];
        for (int index = 0; index < bytes.length; index++) {
            int value = bytes[index] & 0xff;
            output[index * 2] = DIGITS[value >>> 4];
            output[index * 2 + 1] = DIGITS[value & 0x0f];
        }
        return new String(output);
    }
}
