package com.cmbchina.application.knowledge.support;
import com.cmbchina.application.knowledge.api.dto.InterfaceContext;
import java.util.Locale;
import org.springframework.stereotype.Component;
/** 服务前缀解析。 */
@Component
public class InterfaceAddressParser {
    /** 解析路径形式的接口地址，无效地址返回空值。 */
    public InterfaceContext parse(String method, String address) {
        String value = address.split("[?#]", 2)[0];
        int split = value.indexOf('/', 1);
        if (!value.startsWith("/") || split < 2 || split == value.length() - 1) {
            return null;
        }
        if (value.contains("//") || value.matches(".*\\s.*") || !method.matches("[A-Za-z]+")) {
            return null;
        }
        return InterfaceContext.builder().method(method.toUpperCase(Locale.ROOT)).address(address).serviceCode(value.substring(1, split))
                .path(value.substring(split)).build();
    }

    /** 判断接口模板是否匹配实际路径。 */
    public boolean matches(String template, String path) {
        String[] expected = template.split("/", -1);
        String[] actual = path.split("/", -1);
        if (expected.length != actual.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (!expected[i].equals(actual[i]) && !(expected[i].matches("\\{[^/{}]+\\}") && !actual[i].isEmpty())) {
                return false;
            }
        }
        return true;
    }
}