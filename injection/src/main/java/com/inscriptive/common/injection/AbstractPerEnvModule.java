package com.inscriptive.common.injection;

import com.google.common.base.Strings;
import com.google.inject.AbstractModule;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static com.inscriptive.common.injection.EnvFlags.INSCRIPTIVE_ENV;

/**
 * A module that is aware of per-environment configurations.
 *
 * TODO(jack / shawn) rename to AbstractPerPlatformModule
 * INSCRIPTIVE_ENV should be renamed to INSCRIPTIVE_PLATFORM, which should have values like
 * OSX, CIRCLE_CI, GCP. It determines what resources are available. Note that this is very different
 * from the Env, which is about Dev vs. Prod vs. Staging.
 */
public abstract class AbstractPerEnvModule extends AbstractModule {
  // Ensure that we default to some environment if the env variable isn't set.
  private static final Env DEFAULT_ENV = Env.DEV;

  /**
   * Configuration suitable for all environments.
   */
  public abstract void configureCommon();

  /**
   * Configuration suitable for development environments.
   */
  public abstract void configureDev();

  /**
   * Configuration to install in Circle CI
   */
  public abstract void configureCircleCi();

  /*
   * Configuration suitable for production environments.
   */
  public abstract void configureProd();

  @Override
  protected void configure() {
    Env env = getEnv();

    configureCommon();

    switch (env) {
      case DEV:
        install(new DevSafetyCheckModule());
        configureDev();
        break;
      case CIRCLE_CI:
        configureCircleCi();
        break;
      case PROD:
        configureProd();
        break;
      default:
        throw new IllegalArgumentException("Invalid environment: " + env);
    }
  }

  /**
   * Figure out and validate the environment we're in.
   *
   * TODO(gian): if there's ever a reason to do more with these, move these environment definitions.
   */
  private Env getEnv() {
    String env = System.getenv(INSCRIPTIVE_ENV);

    if (Strings.isNullOrEmpty(env)) {
      return DEFAULT_ENV;
    }

    Set<String> names = Arrays.stream(Env.values())
        .map(e -> e.envName)
        .collect(Collectors.toSet());
    return Arrays.stream(Env.values())
        .filter(e -> e.envName.equals(env))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(String.format(
            "%s environment variable must belong to %s. Was %s", INSCRIPTIVE_ENV, names, env)));
  }

  private enum Env {
    DEV("dev"),
    CIRCLE_CI("circle_ci"),
    PROD("prod");

    private final String envName;

    Env(String envName) {
      this.envName = envName;
    }
  }
}
