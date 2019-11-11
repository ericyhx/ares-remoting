package ares.remoting.framework.cluster.engine;

import ares.remoting.framework.cluster.ClusterStrategy;
import ares.remoting.framework.cluster.impl.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description:
 * @author: yuhongxi
 * @date:2019/11/7
 */
public class ClusterEngine {
    private static final Map<ClusterStrategyEnum,ClusterStrategy> clusterStrategyMap=new ConcurrentHashMap<>();
    static {
        clusterStrategyMap.put(ClusterStrategyEnum.Random,new RandomClusterStrategyImpl());
        clusterStrategyMap.put(ClusterStrategyEnum.WeightRandom,new WeightRandomClusterStrategyImpl());
        clusterStrategyMap.put(ClusterStrategyEnum.Polling,new PollingClusterStrategyImpl());
        clusterStrategyMap.put(ClusterStrategyEnum.WeightPolling,new WeightPollingClusterStrategyImpl());
        clusterStrategyMap.put(ClusterStrategyEnum.Hash,new HashClusterStrategyImpl());
    }
    public static ClusterStrategy queryClusterStrategy(String clusterStrategy){
        ClusterStrategyEnum strategy = ClusterStrategyEnum.queryByCode(clusterStrategy);
        if(strategy==null){
            strategy=ClusterStrategyEnum.Random;
        }
        return clusterStrategyMap.get(strategy);
    }
}
