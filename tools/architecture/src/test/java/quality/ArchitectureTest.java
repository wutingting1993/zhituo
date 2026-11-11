package quality;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.Test;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import static org.junit.Assert.assertTrue;

/** 基于编译产物检查分层与接口依赖。 */
public class ArchitectureTest {
    private static final String DOMAIN = "domain";
    private static final String APPLICATION = "application";
    private static final String ADAPTER = "adapter";
    private static final String INFRASTRUCTURE = "infrastructure";
    private static final String COMMON = ".infrastructure.common.";

    /** 验证所有输入目录中的分层依赖。 */
    @Test
    public void enforceBoundaries() {
        JavaClasses classes = loadClasses();
        List<String> errors = new ArrayList<>();
        Map<String, Set<String>> graph = new HashMap<>();
        int layeredClasses = 0;
        for (JavaClass origin : classes) {
            if (layer(origin).isEmpty()) {
                continue;
            }
            layeredClasses++;
            checkLocation(origin, errors);
            checkDependencies(origin, errors, graph);
        }
        assertTrue("No layered packages found", layeredClasses > 0);
        assertTrue(String.join("\n", errors), errors.isEmpty());
        for (String current : graph.keySet()) {
            assertTrue("Layer cycle involving " + current, !cycles(graph, current, current, new HashSet<>()));
        }
    }

    /** 导入显式指定的编译目录，空输入直接失败。 */
    private JavaClasses loadClasses() {
        String locations = System.getProperty("quality.classes", "");
        assertTrue("Supply quality.classes", !locations.isEmpty());
        List<java.nio.file.Path> paths = new ArrayList<>();
        for (String location : locations.split(java.io.File.pathSeparator)) {
            paths.add(Paths.get(location));
        }
        JavaClasses classes = new ClassFileImporter().importPaths(paths);
        assertTrue("No compiled classes found", classes.size() > 0);
        return classes;
    }

    /** 按角色检查目录归属及实现接口。 */
    private void checkLocation(JavaClass origin, List<String> errors) {
        String name = origin.getSimpleName();
        String pkg = origin.getPackageName();
        if (name.endsWith("Controller") && !pkg.contains(".adapter.")) {
            errors.add("Wrong Controller layer: " + origin.getName());
        }
        if (name.endsWith("Mapper") && !pkg.contains(".infrastructure.")) {
            errors.add("Wrong Mapper layer: " + origin.getName());
        }
        if (isServiceImplementation(origin) && origin.getAllRawInterfaces().isEmpty()) {
            errors.add("Service implementation needs an interface: " + origin.getName());
        }
    }

    /** 识别需实现接口的服务类。 */
    private boolean isServiceImplementation(JavaClass type) {
        if (type.isInterface() || type.isEnum() || type.getName().contains("$")) {
            return false;
        }
        boolean namedService = type.getSimpleName().endsWith("Service") || type.getSimpleName().endsWith("Impl");
        return namedService && servicePackage(type.getPackageName());
    }

    /** 检查直接依赖，并构建层间图。 */
    private void checkDependencies(JavaClass origin, List<String> errors, Map<String, Set<String>> graph) {
        String source = layer(origin);
        for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
            JavaClass target = dependency.getTargetClass();
            String dest = layer(target);
            if (!dest.isEmpty() && !source.equals(dest) && !(target.getPackageName() + ".").contains(COMMON)) {
                graph.computeIfAbsent(source, key -> new HashSet<>()).add(dest);
            }
            if (forbiddenLayer(source, dest, target.getPackageName())) {
                errors.add(dependency.getDescription());
            }
            if (concreteService(target) && !origin.equals(target)) {
                errors.add("Depend on service interface: " + dependency.getDescription());
            }
        }
    }

    /** 判断禁止的依赖方向。 */
    private boolean forbiddenLayer(String source, String dest, String pkg) {
        if (DOMAIN.equals(source)) {
            if (APPLICATION.equals(dest) || ADAPTER.equals(dest)) {
                return true;
            }
            return INFRASTRUCTURE.equals(dest) && !(pkg + ".").contains(COMMON);
        }
        if (APPLICATION.equals(source)) {
            return ADAPTER.equals(dest) || (INFRASTRUCTURE.equals(dest) && !(pkg + ".").contains(COMMON));
        }
        return ADAPTER.equals(source) && (pkg.contains(".dao.") || pkg.contains(".client."));
    }

    /** 判断应用或领域服务包。 */
    private boolean servicePackage(String pkg) {
        return pkg.contains(".application.service") || pkg.contains(".domain.service") || pkg.contains(".supportimpl");
    }

    /** 识别调用方不可直接依赖的具体服务。 */
    private boolean concreteService(JavaClass target) {
        if (target.isInterface() || target.isEnum() || target.getName().contains("$")) {
            return false;
        }
        String pkg = target.getPackageName();
        if (pkg.contains(".dto")) {
            return false;
        }
        return servicePackage(pkg) || pkg.contains(".application.api") || pkg.contains(".domain.support");
    }

    /** 检测能否从当前层返回起始层。 */
    private boolean cycles(Map<String, Set<String>> graph, String start, String current, Set<String> visited) {
        if (!visited.add(current)) {
            return false;
        }
        for (String next : graph.getOrDefault(current, Collections.emptySet())) {
            if (next.equals(start) || cycles(graph, start, next, visited)) {
                return true;
            }
        }
        return false;
    }

    /** 从包名解析所属层。 */
    private static String layer(JavaClass type) {
        for (String candidate : new String[]{ADAPTER, APPLICATION, DOMAIN, INFRASTRUCTURE}) {
            if (("." + type.getPackageName() + ".").contains("." + candidate + ".")) {
                return candidate;
            }
        }
        return "";
    }
}
