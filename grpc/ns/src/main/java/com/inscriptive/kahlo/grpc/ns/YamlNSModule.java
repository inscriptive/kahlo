package com.inscriptive.kahlo.grpc.ns;

import com.google.common.base.Throwables;
import com.google.inject.BindingAnnotation;
import com.inscriptive.common.injection.AbstractPerEnvModule;
import org.yaml.snakeyaml.Yaml;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;

public class YamlNSModule extends AbstractPerEnvModule {
  @Override
  public void configureCommon() {
  }

  @Override
  public void configureDev() {
    bind(Browser.class).to(YamlBrowser.class);
    bind(Registry.class).to(YamlRegistry.class);

    bind(Topology.class).toProvider(topologyProvider).asEagerSingleton();
    bind(InputStream.class).annotatedWith(Topo.class).toProvider(localTopology)
        .asEagerSingleton();
  }

  @Override
  public void configureCircleCi() {
    configureDev();
  }

  @Override
  public void configureProd() {
  }

  private static final Provider<InputStream> localTopology = () ->
      YamlNSModule.class.getResourceAsStream("/topology-local.yaml");

  private static final Provider<InputStream> prodTopology = () -> {
    try {
      return new FileInputStream("/var/inscriptive/topology.yaml");
    } catch (FileNotFoundException e) {
      throw Throwables.propagate(e);
    }
  };

  private static final Provider<Topology> topologyProvider = new Provider<Topology>() {
    @Inject
    @Topo
    InputStream topologyYaml;

    @Override
    public Topology get() {
      ImmutableTopology.Builder builder = ImmutableTopology.builder();
      Yaml yaml = new Yaml();
      Map<String, Object> map = ((Map<String, Object>)
          yaml.load(topologyYaml));

      List<Map<String, Object>> services = ((List<Map<String, Object>>) map.get("services"));
      services.forEach(service -> builder.putServices((String) service.get("name"),
          ImmutableServiceDescriptor.builder()
              .id((String) service.get("id"))
              .name((String) service.get("name"))
              .addr((String) service.get("addr"))
              .port(((Integer) service.get("port")))
              .state(ServiceDescriptor.State.HEALTHY)
              .build()));

      return builder.build();
    }
  };

  @BindingAnnotation
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.PARAMETER, ElementType.FIELD})
  @interface Topo {
  }
}
