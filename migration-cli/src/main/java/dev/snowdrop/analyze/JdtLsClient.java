package dev.snowdrop.analyze;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import dev.snowdrop.analyze.model.Rule;
import dev.snowdrop.analyze.utils.LSClient;
import dev.snowdrop.model.Query;
import dev.snowdrop.parser.QueryUtils;
import dev.snowdrop.parser.QueryVisitor;
import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageServer;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static dev.snowdrop.analyze.utils.FileUtils.resolvePath;
import static dev.snowdrop.analyze.utils.JdtLsUtils.getLocationCode;
import static dev.snowdrop.analyze.utils.JdtLsUtils.getLocationName;

public class JdtLsClient {
	private static final Logger logger = Logger.getLogger(JdtLsClient.class);
	private static final long TIMEOUT = 30000;

	private Process process = null;
	private LanguageServer remoteProxy;
	private Launcher<LanguageServer> launcher;

	public CompletableFuture<InitializeResult> future;
	private final Config config;

	private JdtLsClient(Config config) {
		this.config = config;

	}

	public void start() throws Exception {
		launchLsProcess();
		createLaunchLsClient();
		initLanguageServer();
	}

	public void stop() {
		if (process != null && process.isAlive()) {
			logger.infof("🛑 Shutting down JDT Language Server...");
			process.destroyForcibly();
		}
	}

	public void initLanguageServer() throws ExecutionException, InterruptedException, TimeoutException {
		remoteProxy = launcher.getRemoteProxy();

		InitializeParams p = new InitializeParams();
		p.setProcessId((int) ProcessHandle.current().pid());
		// The path to the application to be scanned should be created as URI (file:///) !!
		p.setRootUri(resolvePath(config.appPath()).toUri().toString());
		p.setCapabilities(new ClientCapabilities());

		String bundlePath = String.format("[\"%s\"]", Paths.get(config.jdtLsPath(), "java-analyzer-bundle",
				"java-analyzer-bundle.core", "target", "java-analyzer-bundle.core-1.0.0-SNAPSHOT.jar"));
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
		Path wksDir = Paths.get(config.jdtWks());

		String os = System.getProperty("os.name").toLowerCase();
		Path configPath = os.contains("win")
				? Paths.get(config.jdtLsPath(), "config_win")
				: os.contains("mac")
						? Paths.get(config.jdtLsPath(), "config_mac_arm")
						: Paths.get(config.jdtLsPath(), "config_linux");

		String launcherJar = Objects.requireNonNull(new File(config.jdtLsPath(), "plugins")
				.listFiles((dir, name) -> name.startsWith("org.eclipse.equinox.launcher_")))[0].getName();

		ProcessBuilder pb = new ProcessBuilder("java", "-Declipse.application=org.eclipse.jdt.ls.core.id1",
				"-Dosgi.bundles.defaultStartLevel=4", "-Dosgi.checkConfiguration=true",
				"-Dosgi.sharedConfiguration.area.readOnly=true", "-Dosgi.configuration.cascaded=true",
				"-Declipse.product=org.eclipse.jdt.ls.core.product", "-Dlog.level=ALL", "-Djdt.ls.debug=true",
				"-noverify", "-Xmx1G", "--add-modules=ALL-SYSTEM", "--add-opens", "java.base/java.util=ALL-UNNAMED",
				"--add-opens", "java.base/java.lang=ALL-UNNAMED", "-jar",
				Paths.get(config.jdtLsPath(), "plugins", launcherJar).toString(), "-configuration",
				configPath.toString(), "-data", wksDir.resolve(".jdt_workspace").toString());
		pb.redirectErrorStream(true);

		String javaHome = Optional.ofNullable(System.getProperty("JAVA_HOME")).orElse(System.getProperty("java.home"));

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

		launcher = LSPLauncher.createClientLauncher(client, process.getInputStream(), process.getOutputStream(),
				executor, (writer) -> writer // No-op, we don't want to wrap the writer
		);

		launcher.startListening();
	}

	public static class JdtLsClientBuilder {
		private Config config;

		public JdtLsClientBuilder withConfig(Config config) {
			this.config = config;
			return this;
		}

		public JdtLsClient build() {
			if (config == null) {
				throw new IllegalStateException("JdtLsConfig is required");
			}
			return new JdtLsClient(config);
		}
	}

	//	public List<SymbolInformation> executeCommand(Config config, Query q, Object dto) {
	//		// TODO: Find a way to cast properly
	//		JavaClassDTO javaClassDTO = (JavaClassDTO) dto;
	//		List<Object> cmdArguments = List.of(javaClassDTO.cmdParams());
	//
	//		if (process == null || !process.isAlive()) {
	//			throw new IllegalStateException("The jdt-ls server/process is not running");
	//		}
	//
	//		try {
	//			CompletableFuture<List<SymbolInformation>> symbolsFuture = future
	//					.thenApplyAsync(ignored -> executeLsCmd(config, cmdArguments)).exceptionally(throwable -> {
	//						logger.errorf(
	//								String.format("Error executing LS command for query %s-%s", q.fileType(), q.symbol()),
	//								throwable.getMessage(), throwable);
	//						return new ArrayList<SymbolInformation>();
	//					});
	//
	//			return symbolsFuture.get(); // Wait for completion
	//		} catch (InterruptedException | ExecutionException e) {
	//			logger.errorf(String.format("Failed to execute command for %s-%s", q.fileType(), q.symbol()),
	//					e.getMessage());
	//			return null;
	//		}
	//	}

	@Deprecated
	public List<SymbolInformation> executeCommand(Config config, Query query) {

		String location = getLocationCode(query.symbol());
		if (location == null || location.equals("0")) {
			throw new IllegalStateException(String.format(
					"The language server's location code don't exist using the when condition of the query: %s-%s",
					query.fileType(), query.symbol()));
		}

		// Map the Query object with the RuleEntry parameters to be sent to the Language Server
		var paramsMap = Map.of("project", query.fileType().toLowerCase(), // This value should be java
				"location", location, // The symbol should correspond to one of the value that LS
				// supports: annotation, etc
				"query", query.keyValues().get("name"), // TODO: To be improved as we need a mapper able to extract the k=v
				// and convert them to the pattern
				"analysisMode", "source-only" // 2 modes are supported: source-only and full
		);

		List<Object> cmdArguments = List.of(paramsMap);

		if (process == null || !process.isAlive()) {
			throw new IllegalStateException("The jdt-ls server/process is not running");
		}

		try {
			CompletableFuture<List<SymbolInformation>> symbolsFuture = future
					.thenApplyAsync(ignored -> executeLsCmd(config, cmdArguments)).exceptionally(throwable -> {
						logger.errorf(String.format("Error executing LS command for query %s-%s", query.fileType(),
								query.symbol()), throwable.getMessage(), throwable);
						return new ArrayList<SymbolInformation>();
					});

			return symbolsFuture.get(); // Wait for completion
		} catch (InterruptedException | ExecutionException e) {
			logger.errorf(String.format("Failed to execute command for %s-%s", query.fileType(), query.symbol()),
					e.getMessage());
			return null;
		}
	}

	public List<SymbolInformation> executeLsCmd(Config config, List<Object> arguments) {
		List<Object> cmdArguments = (arguments != null && !arguments.isEmpty()) ? arguments : Collections.EMPTY_LIST;

		ExecuteCommandParams commandParams = new ExecuteCommandParams(config.lsCmd(), cmdArguments);

		CompletableFuture<Object> commandResult = remoteProxy.getWorkspaceService().executeCommand(commandParams)
				.exceptionally(t -> {
					t.printStackTrace();
					return null;
				});

		Object result = commandResult.join();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		List<SymbolInformation> symbolInformationList = new ArrayList<>();

		if (result != null) {
			logger.infof("==== CLIENT: --- Search Results found !");
			logger.infof("==== CLIENT: --- Command executed with params: %s.", commandParams);
			logger.infof("==== CLIENT: --- JSON response: %s", gson.toJson(result));

			try {
				if (result instanceof List) {
					List<?> resultList = (List<?>) result;
					for (Object item : resultList) {
						SymbolInformation symbol = new SymbolInformation();

						// Extract data from the result object and populate SymbolInformation
						// This assumes the result contains objects with name, kind, and location data
						if (item instanceof java.util.Map) {
							java.util.Map<?, ?> itemMap = (java.util.Map<?, ?>) item;

							// Set name if available
							if (itemMap.containsKey("name")) {
								symbol.setName(String.valueOf(itemMap.get("name")));
							}

							// Set kind if available (convert to SymbolKind)
							if (itemMap.containsKey("kind")) {
								Object kindValue = itemMap.get("kind");
								if (kindValue instanceof Number) {
									symbol.setKind(SymbolKind.forValue(((Number) kindValue).intValue()));
								}
							}

							// Set location if available
							if (itemMap.containsKey("location")) {
								// Parse location data - this would need to be adapted based on actual structure
								Object locationData = itemMap.get("location");
								Location location = gson.fromJson(gson.toJson(locationData), Location.class);
								symbol.setLocation(location);
							}

							symbolInformationList.add(symbol);
						}
					}
				} else {
					// Fallback to direct GSON conversion if result is not a List
					Type SymbolInformationListType = new TypeToken<List<SymbolInformation>>() {
					}.getType();
					symbolInformationList = gson.fromJson(gson.toJson(result), SymbolInformationListType);
				}
			} catch (Exception e) {
				logger.warnf("==== CLIENT: Failed to create SymbolInformation objects: %s", e.getMessage());
				// Fallback to GSON conversion
				try {
					Type SymbolInformationListType = new TypeToken<List<SymbolInformation>>() {
					}.getType();
					symbolInformationList = gson.fromJson(gson.toJson(result), SymbolInformationListType);
				} catch (JsonSyntaxException | ClassCastException ex) {
					logger.warnf("==== CLIENT: Failed fallback GSON conversion: %s", ex.getMessage());
				}
			}

			if (symbolInformationList.isEmpty()) {
				logger.infof("==== CLIENT: SymbolInformation List is empty.");
			} else {
				Map<String, Object> args = (Map<String, Object>) arguments.get(0);
				logger.infof("==== CLIENT: Found %s usage(s) of symbol: %s, name: %s", symbolInformationList.size(),
						getLocationName(args.get("location").toString()), args.get("query"));
				for (SymbolInformation si : symbolInformationList) {
					logger.debugf("==== CLIENT: Found %s at line %s, char: %s - %s within the file: %s)", si.getName(),
							si.getLocation().getRange().getStart().getLine() + 1,
							si.getLocation().getRange().getStart().getCharacter(),
							si.getLocation().getRange().getEnd().getCharacter(), si.getLocation().getUri());
				}
			}
			logger.infof("==== CLIENT: ----------------------");
		} else {
			logger.warn("==== CLIENT: Received null result for command.");
		}

		return symbolInformationList;
	}

	@Deprecated
	public Map<String, List<SymbolInformation>> executeLsCmd(Rule rule) {
		Map<String, List<SymbolInformation>> ruleResults = new HashMap<>();

		// Log the LS Query command to be executed on the LS server
		logger.infof("==== CLIENT: Sending the command '%s' ...", config.lsCmd());

		if (process == null || !process.isAlive()) {
			throw new IllegalStateException("JDT-LS process is not running");
		}

		// Parse first the Rule condition to populate the Query object using the YAML condition query
		// See the parser maven project for examples, unit tests
		QueryVisitor visitor = QueryUtils.parseAndVisit(rule.when().condition());

		/*
		 * Handle the 3 supported cases where the query contains:
		 *
		 * - One clause: FIND java.annotation WHERE (name='@SpringBootApplication')
		 *
		 * - Clauses separated with the OR operator:
		 *
		 * FIND java.annotation WHERE (name='@SpringBootApplication') OR java.annotation WHERE (name='@Deprecated')
		 *
		 * - Clauses separated with the AND operator:
		 *
		 * FIND java.annotation WHERE (name='@SpringBootApplication') AND pom.dependency WHERE
		 * (groupId='org.springframework.boot', artifactId='spring-boot', version='3.4.2')
		 *
		 */
		if (visitor.getSimpleQueries().size() == 1) {
			visitor.getSimpleQueries().stream().findFirst().ifPresent(q -> {
				List<SymbolInformation> results = executeCommand(config, q);
				ruleResults.putAll(Map.of(rule.ruleID(), results));
			});
		} else if (visitor.getOrQueries().size() > 1) {
			visitor.getOrQueries().stream().forEach(q -> {
				List<SymbolInformation> results = executeCommand(config, q);
				ruleResults.putAll(Map.of(rule.ruleID(), results));
			});
		} else if (visitor.getAndQueries().size() > 1) {
			visitor.getAndQueries().stream().forEach(q -> {
				List<SymbolInformation> results = executeCommand(config, q);
				ruleResults.putAll(Map.of(rule.ruleID(), results));
			});
		} else {
			logger.warnf("Rule %s has no valid condition(s)", rule.ruleID());
			ruleResults.put(rule.ruleID(), new ArrayList<>());
		}

		/*
		 * Old code deprecated in favor of the Antlr parser ! // Handle three cases: single java.referenced, OR
		 * conditions, AND conditions if (rule.when().or() != null && !rule.when().or().isEmpty()) {
		 * logger.infof("Rule When includes: %s between java.referenced", "OR"); rule.when().or().forEach(condition -> {
		 * List<SymbolInformation> results = executeCommandForCondition(config, rule, condition.javaReferenced());
		 * ruleResults.putAll(Map.of(rule.ruleID(),results)); }); } else if (rule.when().and() != null &&
		 * !rule.when().and().isEmpty()) { logger.infof("Rule When includes: %s between java.referenced", "AND");
		 * rule.when().and().forEach(condition -> { List<SymbolInformation> results = executeCommandForCondition(config,
		 * rule, condition.javaReferenced()); ruleResults.putAll(Map.of(rule.ruleID(),results)); }); } else if
		 * (rule.when().javaReferenced() != null) { logger.infof("Rule When includes: single java.referenced");
		 * List<SymbolInformation> results = executeCommandForCondition(config, rule, rule.when().javaReferenced());
		 * ruleResults.putAll(Map.of(rule.ruleID(),results)); } else {
		 * logger.warnf("Rule %s has no valid java.referenced conditions", rule.ruleID());
		 * ruleResults.put(rule.ruleID(), new ArrayList<>()); }
		 */

		return ruleResults;
	}

	@Deprecated
	private List<SymbolInformation> executeCommandForCondition(JdtLsClient client, Rule rule,
			Rule.JavaReferenced javaReferenced) {
		var paramsMap = Map.of("project", "java", // hard coded value to java within the analyzer java external-provider
				"location", getLocationCode(javaReferenced.location()), "query", javaReferenced.pattern(), // pattern
				// from the
				// rule
				"analysisMode", "source-only" // 2 modes are supported: source-only and full
		);

		List<Object> cmdArguments = List.of(paramsMap);

		try {
			CompletableFuture<List<SymbolInformation>> symbolsFuture = client.future
					.thenApplyAsync(ignored -> executeLsCmd(config, cmdArguments)).exceptionally(throwable -> {
						logger.errorf("Error executing LS command for rule %s: %s", rule.ruleID(),
								throwable.getMessage(), throwable);
						return new ArrayList<SymbolInformation>();
					});

			return symbolsFuture.get(); // Wait for completion
		} catch (InterruptedException | ExecutionException e) {
			logger.errorf("Failed to execute command for rule %s: %s", rule.ruleID(), e.getMessage());
			return null;
		}
	}

}