package com.inscriptive.common.grpc.client;

import com.google.common.collect.ImmutableList;
import com.inscriptive.common.exceptions.IncompleteSwitchException;
import com.inscriptive.common.hash.KetamaConsistentHash;
import io.grpc.*;

import java.net.SocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.inscriptive.common.util.RandomStrings.randomString;

public class KetamaHashingLoadBalancerFactory extends LoadBalancer.Factory {
  public static final CallOptions.Key<String> KETAMA_HASH_KEY =
      CallOptions.Key.of("ketama_hash_key", null);

  @Override
  public LoadBalancer newLoadBalancer(LoadBalancer.Helper helper) {
    return new LoadBalancer() {
      private List<Subchannel> subchannels = ImmutableList.of();
      private KetamaConsistentHash<Subchannel> subchannelRing =
          new KetamaConsistentHash<>(400, ImmutableList.of());

      private SubchannelPicker picker = new SubchannelPicker() {
        @Override
        public PickResult pickSubchannel(PickSubchannelArgs args) {
          String loadBalanceHashKey = args.getCallOptions().getOption(KETAMA_HASH_KEY);
          Subchannel subchannel = subchannelRing.get(
              Optional.ofNullable(loadBalanceHashKey).orElse(randomString(1)));
          if (subchannel == null) {
            return PickResult.withNoResult();
          }
          return PickResult.withSubchannel(subchannel);
        }
      };

      @Override
      public void handleResolvedAddresses(List<ResolvedServerInfoGroup> serverGroups,
          Attributes attributes) {
        checkArgument(!serverGroups.isEmpty(), "no server groups were resolved");
        checkArgument(serverGroups.size() == 1, "more than one server group");
        List<ResolvedServerInfo> servers = serverGroups.get(0).getResolvedServerInfoList();

        List<SocketAddress> addresses = servers.stream()
            .map(ResolvedServerInfo::getAddress)
            .collect(Collectors.toList());

        // callbacks are serialized so updating the reference is fine
        subchannels = addresses.stream()
            .map(a -> helper.createSubchannel(new EquivalentAddressGroup(a), Attributes.EMPTY))
            .collect(Collectors.toList());
        subchannelRing = new KetamaConsistentHash<>(400, subchannels);
        helper.updatePicker(picker);
      }

      @Override
      public void handleNameResolutionError(Status error) {
        // not sure what to do here
        System.out.println("woooooooooo");
      }

      @Override
      public void handleSubchannelState(Subchannel subchannel,
          ConnectivityStateInfo stateInfo) {
        switch (stateInfo.getState()) {
          case CONNECTING:
            break;
          case READY:
          case IDLE:
            subchannelRing.add(subchannel);
            break;
          case TRANSIENT_FAILURE:
          case SHUTDOWN:
            subchannelRing.remove(subchannel);
            break;
          default:
            throw new IncompleteSwitchException(stateInfo.getState());
        }
        // RPCs that are buffered because they got an IDLE subchannel will not be retried
        // until this happens.
        helper.updatePicker(picker);
      }

      @Override
      public void shutdown() {
        subchannels.forEach(Subchannel::shutdown);
      }
    };
  }
}
