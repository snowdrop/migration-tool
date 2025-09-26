package dev.snowdrop.analyze;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.snowdrop.commands.AnalyzeCommand;
import dev.snowdrop.analyze.utils.LSClient;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static dev.snowdrop.analyze.services.LsSearchService.analyzeCodeFromRule;
import static dev.snowdrop.analyze.utils.FileUtils.resolvePath;

@ApplicationScoped
public class JdtLsFactory {
    private static final Logger logger = Logger.getLogger(JdtLsFactory.class);
    private static final long TIMEOUT = 30000;

    public Process process = null;
    public LanguageServer remoteProxy;
    public Launcher<LanguageServer> launcher;

    public String jdtLsPath;
    public String jdtWks;
    public String appPath;
    public Path rulesPath;
    public String lsCmd;

    public CompletableFuture<InitializeResult> future;

    public static void main(String[] args) throws Exception {
        JdtLsFactory jdtlsFactory = new JdtLsFactory();
        jdtlsFactory.initProperties(null);
        jdtlsFactory.launchLsProcess();
        jdtlsFactory.createLaunchLsClient();
        jdtlsFactory.initLanguageServer();
        jdtlsFactory.analyze();
    }

    public void initProperties(AnalyzeCommand analyzeCommand) {
        String appPathString = Optional
            .ofNullable(System.getProperty("APP_PATH"))
            .or(() -> Optional.ofNullable(analyzeCommand).map(cmd -> cmd.appPath))
            .or(() -> Optional.ofNullable(ConfigProvider.getConfig().getValue("analyzer.app-path", String.class)))
            .orElseThrow(() -> new RuntimeException("Path to the project to scan is missing !"));
        appPath = resolvePath(appPathString).toString();

        String jdtLsPathString = Optional
            .ofNullable(System.getProperty("JDT_LS_PATH"))
            .or(() -> Optional.ofNullable(analyzeCommand).map(cmd -> cmd.jdtLsPath))
            .or(() -> Optional.ofNullable(ConfigProvider.getConfig().getValue("analyzer.jdt-ls-path", String.class)))
            .orElseThrow(() -> new RuntimeException("JDT_LS_PATH system property is missing !"));
        jdtLsPath = resolvePath(jdtLsPathString).toString();

        String jdtWksString = Optional
            .ofNullable(System.getProperty("JDT_WKS"))
            .or(() -> Optional.ofNullable(analyzeCommand).map(cmd -> cmd.jdtWorkspace))
            .or(() -> Optional.ofNullable(ConfigProvider.getConfig().getValue("analyzer.jdt-workspace-path", String.class)))
            .orElseThrow(() -> new RuntimeException("JDT_WKS system property is missing !"));
        jdtWks = resolvePath(jdtWksString).toString();

        String rulesPathString = Optional
            .ofNullable(System.getProperty("RULES_PATH"))
            .or(() -> Optional.ofNullable(analyzeCommand).map(cmd -> cmd.rulesPath))
            .or(() -> Optional.ofNullable(ConfigProvider.getConfig().getValue("analyzer.rules-path", String.class)))
            .orElseThrow(() -> new RuntimeException("Path of the rules folder is missing !"));
        rulesPath = resolvePath(rulesPathString);

        lsCmd = Optional
            .ofNullable(System.getProperty("LS_CMD"))
            .or(() -> Optional.ofNullable(analyzeCommand).map(cmd -> cmd.lsCommand))
            .or(() -> Optional.ofNullable(ConfigProvider.getConfig().getValue("analyzer.jdt-ls-command", String.class)))
            .orElseThrow(() -> new RuntimeException("Command to be executed against the LS server is missing !"));

        logger.infof("APP_PATH: %s", appPath);
        logger.infof("JDT_LS_PATH: %s", jdtLsPath);
        logger.infof("JDT_WKS: %s", jdtWks);
        logger.infof("RULES_PATH: %s", rulesPath);
        logger.infof("LS_CMD: %s", lsCmd);
    }

    public void initLanguageServer() throws Exception {
        remoteProxy = launcher.getRemoteProxy();

        InitializeParams p = new InitializeParams();
        p.setProcessId((int) ProcessHandle.current().pid());
        // The path to the application to be scanned should be created as URI (file:///) !!
        p.setRootUri(resolvePath(appPath).toUri().toString());
        p.setCapabilities(new ClientCapabilities());

        String bundlePath = String.format("[\"%s\"]", Paths.get(jdtLsPath, "java-analyzer-bundle", "java-analyzer-bundle.core", "target", "java-analyzer-bundle.core-1.0.0-SNAPSHOT.jar"));
        logger.infof("bundle path is %s", bundlePath);

        String json = String.format("""
            {
               "bundles": %s
            }""", bundlePath);
        logger.infof("initializationOptions: %s", json);

        Object initializationOptions = new Gson().fromJson(json, JsonObject.class);
        p.setInitializationOptions(initializationOptions);

        future = remoteProxy.initialize(p);
        future.get(TIMEOUT, TimeUnit.MILLISECONDS).toString();

        InitializedParams initialized = new InitializedParams();
        remoteProxy.initialized(initialized);
    }

    public void launchLsProcess() {
        Path wksDir = Paths.get(jdtWks);

        String os = System.getProperty("os.name").toLowerCase();
        Path configPath = os.contains("win") ? Paths.get(jdtLsPath, "config_win") :
            os.contains("mac") ? Paths.get(jdtLsPath, "config_mac_arm") :
                Paths.get(jdtLsPath, "config_linux");

        String launcherJar = Objects
            .requireNonNull(
                new File(jdtLsPath, "plugins")
                    .listFiles((dir, name) -> name.startsWith("org.eclipse.equinox.launcher_")))[0].getName();

        ProcessBuilder pb = new ProcessBuilder(
            "java",
            "-Declipse.application=org.eclipse.jdt.ls.core.id1",
            "-Dosgi.bundles.defaultStartLevel=4",
            "-Dosgi.checkConfiguration=true",
            "-Dosgi.sharedConfiguration.area.readOnly=true",
            "-Dosgi.configuration.cascaded=true",
            "-Declipse.product=org.eclipse.jdt.ls.core.product",
            "-Dlog.level=ALL",
            "-Djdt.ls.debug=true",
            "-noverify",
            "-Xmx1G",
            "--add-modules=ALL-SYSTEM",
            "--add-opens", "java.base/java.util=ALL-UNNAMED",
            "--add-opens", "java.base/java.lang=ALL-UNNAMED",
            "-jar", Paths.get(jdtLsPath, "plugins", launcherJar).toString(),
            "-configuration", configPath.toString(),
            "-data", wksDir.resolve(".jdt_workspace").toString()
        );
        pb.redirectErrorStream(true);

        String javaHome = Optional.ofNullable(System.getProperty("JAVA_HOME"))
            .orElse(System.getProperty("java.home"));

        Map<String, String> env = pb.environment();
        env.put("JAVA_HOME", javaHome);

        try {
            process = pb.start();
            logger.infof("====== Language Server Process id: %s ====== ", process.info());
            logger.infof("====== jdt ls started =======");
            logger.infof("====== Workspace project directory: %s ======", wksDir);
        } catch (IOException exception) {
            logger.errorf("====== Failed to create process :%s", String.valueOf(exception));
            System.exit(1);
        }

    }

    public void createLaunchLsClient() {
        ExecutorService executor;

        logger.info("Connecting to the JDT Language Server ...");

        executor = Executors.newSingleThreadExecutor();
        LSClient client = new LSClient();

        launcher = LSPLauncher.createClientLauncher(
            client,
            process.getInputStream(),
            process.getOutputStream(),
            executor,
            (writer) -> writer // No-op, we don't want to wrap the writer
        );

        launcher.startListening();
    }

    private void analyze() throws IOException {
        analyzeCodeFromRule(this);
    }
}