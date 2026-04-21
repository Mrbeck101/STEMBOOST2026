package DatabaseController;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

final class DbPropertiesLoader {
	private DbPropertiesLoader() {
	}

	static Properties load() throws Exception {
		Properties props = new Properties();

		// Classpath defaults packaged with the app.
		loadFromClasspath(props, "db.properties");


		// External files (if present) override packaged defaults.
		loadFromFile(props, "db.properties");


		if (props.isEmpty()) {
			throw new IllegalStateException("No db/dp properties file found in classpath or working directory.");
		}
		return props;
	}

	private static void loadFromClasspath(Properties props, String name) throws Exception {
		try (InputStream in = DbPropertiesLoader.class.getClassLoader().getResourceAsStream(name)) {
			if (in != null) {
				props.load(in);
			}
		}
	}

	private static void loadFromFile(Properties props, String name) throws Exception {
		Path path = Path.of(name);
		if (!Files.exists(path)) {
			return;
		}
		try (InputStream in = Files.newInputStream(path)) {
			props.load(in);
		}
	}
}

