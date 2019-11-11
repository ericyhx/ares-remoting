package ares.remoting.framework.cluster.impl;

import org.apache.commons.lang.StringUtils;

import java.util.Arrays;

/**
 * @author liyebing created on 17/4/23.
 * @version $Id$
 */
public enum ClusterStrategyEnum {

    //随机算法
    Random("Random"),
    //权重随机算法
    WeightRandom("WeightRandom"),
    //轮询算法
    Polling("Polling"),
    //权重轮询算法
    WeightPolling("WeightPolling"),
    //源地址hash算法
    Hash("Hash");

    ClusterStrategyEnum(String code) {
        this.code = code;
    }


    public static ClusterStrategyEnum queryByCode(String code) {
      return   Arrays.stream(ClusterStrategyEnum.values()).filter(o->o.code.equals(code)).findAny().orElse(null);
    }

    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
