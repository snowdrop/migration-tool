package dev.snowdrop.analyze.services;

import dev.snowdrop.analyze.Config;
import dev.snowdrop.analyze.JdtLsClient;
import org.jboss.logging.Logger;

import static dev.snowdrop.analyze.JdtLsClient.*;

public class ScannerFactory {

    private static final Logger logger = Logger.getLogger(ScannerFactory.class);

    public CodeScanner createScanner(Scanner scannerType, Config config) {
        return switch (scannerType) {
        case Scanner.JDTLS -> {
            JdtLsClient client = new JdtLsClientBuilder().withConfig(config).build();
            yield new JdtLsScanner(config, client);
        }
        case Scanner.OPENREWRITE -> {
            RewriteService service = new RewriteService();
            yield new OpenRewriteScanner(config, service);
        }
        default -> throw new IllegalArgumentException("Unknown scanner: " + scannerType);
        };
    }

    public enum Scanner {
        OPENREWRITE("openrewrite"), JDTLS("jdtls");

        private final String label;

        Scanner(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public static Scanner fromLabel(String label) {
            for (Scanner s : values()) {
                if (s.label.equalsIgnoreCase(label)) {
                    return s;
                }
            }
            throw new IllegalArgumentException("Unknown scanner: " + label);
        }
    }
}
