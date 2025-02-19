package hudson;

import static org.junit.Assert.assertNotNull;

import jenkins.plugins.dynamic_extension_loading.CustomExtensionLoadedViaConstructor;
import jenkins.plugins.dynamic_extension_loading.CustomExtensionLoadedViaListener;
import jenkins.plugins.dynamic_extension_loading.CustomPeriodicWork;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.RealJenkinsRule;
import org.jvnet.hudson.test.RealJenkinsRule.SyntheticPlugin;

public class ExtensionListRjrTest {
    @Rule
    public RealJenkinsRule rjr = new RealJenkinsRule();

    /**
     * Check that dynamically loading a plugin does not lead to extension lists with duplicate entries.
     * In particular we test extensions that load other extensions in their constructors and extensions that have
     * methods that load other extensions and which are called by an {@link ExtensionListListener}.
     */
    @Test
    public void checkDynamicLoad_singleRegistration() throws Throwable {
        var pluginJpi = rjr.createSyntheticPlugin(new SyntheticPlugin(CustomPeriodicWork.class.getPackage())
                .shortName("dynamic-extension-loading")
                .header("Plugin-Dependencies", "variant:0"));
        var fqcn1 = CustomExtensionLoadedViaListener.class.getName();
        var fqcn2 = CustomExtensionLoadedViaConstructor.class.getName();
        rjr.then(r -> {
            r.jenkins.pluginManager.dynamicLoad(pluginJpi);
            assertSingleton(r, fqcn1);
            assertSingleton(r, fqcn2);
        });
    }

    private static void assertSingleton(JenkinsRule r, String fqcn) throws Exception {
        var clazz = r.jenkins.pluginManager.uberClassLoader.loadClass(fqcn);
        try {
            assertNotNull(ExtensionList.lookupSingleton(clazz));
        } catch (Throwable t) {
            var list = ExtensionList.lookup(clazz).stream().map(e ->
                    // TODO: Objects.toIdentityString in Java 19+
                    e.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(e))).toList();
            System.out.println("Duplicates are: " + list);
            throw t;
        }
    }
}
