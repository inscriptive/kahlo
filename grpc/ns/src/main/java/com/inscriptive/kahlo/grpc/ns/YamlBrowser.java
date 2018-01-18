package com.inscriptive.kahlo.grpc.ns;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;

@Singleton
public class YamlBrowser implements Browser {
  private final Topology topology;

  @Inject
  public YamlBrowser(Topology topology) {
    this.topology = topology;
  }

  @Override
  public Set<ServiceDescriptor> find(String name) {
    return topology.services().get(name).stream()
        .collect(Collectors.toSet());
  }

  @Override
  public Map<String, Set<ServiceDescriptor>> all() {
    return topology.services().entries().stream()
        .collect(groupingBy(Map.Entry::getKey,
            mapping(Map.Entry::getValue, Collectors.toSet())));
  }
}
