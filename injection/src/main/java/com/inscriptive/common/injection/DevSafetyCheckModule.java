package com.inscriptive.common.injection;

import com.google.inject.AbstractModule;
import com.inscriptive.common.Logger;

/**
 * Attempts to ensure that we explode on startup if we ever attempt to run dev-only code in prod.
 *
 * TODO(gian): Make this more robust.
 */
public class DevSafetyCheckModule extends AbstractModule {
  private static final Logger logger = Logger.getLogger(DevSafetyCheckModule.class);
  public static final String MACOSX = "Mac OS X";

  private static boolean checked = false;
  @Override
  protected void configure() {
    // Guice modules can be installed and configured multiple times.
    // DevSafetyCheckModule's job only needs to be done once.
    if (checked) {
      return;
    }

    String osName = System.getProperty("os.name");

    logger.warn("*** WARNING: Development mode is enabled ***");
    logger.info("Checking system to ensure it is a dev environment: %s", osName);

    if (isDev(osName)) {
      logger.info("System appears to be a dev machine. All is well.", osName);
    } else {
      throw new RuntimeException(
          "Dev mode is enabled, but we could not confirm that '" + osName + "' is a dev system.");
    }

    checked = true;
  }

  public boolean isDev(String osName) {
    if (osName.equals(MACOSX)) {
      return true;
    }

    // This is an env var that they set on their containers.
    String circleCi = System.getenv("CIRCLECI");
    return ("true".equals(circleCi));
  }
}
