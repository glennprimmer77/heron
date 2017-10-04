// Copyright 2016 Twitter. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.


package com.twitter.heron.healthmgr.sensors;

import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import javax.inject.Inject;

import com.microsoft.dhalion.api.MetricsProvider;
import com.microsoft.dhalion.metrics.ComponentMetrics;
import com.microsoft.dhalion.metrics.InstanceMetrics;

import com.twitter.heron.healthmgr.HealthPolicyConfig;
import com.twitter.heron.healthmgr.common.PackingPlanProvider;
import com.twitter.heron.healthmgr.common.TopologyProvider;

import static com.twitter.heron.healthmgr.sensors.BaseSensor.MetricName.METRIC_BACK_PRESSURE;

public class BackPressureSensor extends BaseSensor {
  private static final Logger LOG = Logger.getLogger(BackPressureSensor.class.getName());

  private final MetricsProvider metricsProvider;
  private final PackingPlanProvider packingPlanProvider;
  private final TopologyProvider topologyProvider;

  @Inject
  public BackPressureSensor(PackingPlanProvider packingPlanProvider,
                            TopologyProvider topologyProvider,
                            HealthPolicyConfig policyConfig,
                            MetricsProvider metricsProvider) {
    super(policyConfig, METRIC_BACK_PRESSURE.text(), BackPressureSensor.class.getSimpleName());
    this.packingPlanProvider = packingPlanProvider;
    this.topologyProvider = topologyProvider;
    this.metricsProvider = metricsProvider;
  }

  @Override
  public Map<String, ComponentMetrics> get(String... components) {
    return get();
  }

  /**
   * Computes the average (millis/sec) back-pressure caused by instances in the configured window
   *
   * @return the average value
   */
  public Map<String, ComponentMetrics> get() {
    Map<String, ComponentMetrics> result = new HashMap<>();

    String[] boltComponents = topologyProvider.getBoltNames();
    for (String boltComponent : boltComponents) {
      String[] boltInstanceNames = packingPlanProvider.getBoltInstanceNames(boltComponent);

      Duration duration = getDuration();
      Map<String, InstanceMetrics> instanceMetrics = new HashMap<>();
      for (String boltInstanceName : boltInstanceNames) {
        String metric = getMetricName() + boltInstanceName;
        System.out.println("backpressure sensor metric query " + metric);
        Map<String, ComponentMetrics> stmgrResult = metricsProvider.getComponentMetrics(
            metric, duration, COMPONENT_STMGR);
        System.out.println("backpressure sensor metric query result\n" + metric);

        if (stmgrResult.get(COMPONENT_STMGR) == null) {
          System.out.println("continue stmgr result null");
          continue;
        }

        HashMap<String, InstanceMetrics> streamManagerResult =
            stmgrResult.get(COMPONENT_STMGR).getMetrics();

        if (streamManagerResult.isEmpty()) {
          System.out.println("continue stmgr result empty");
          continue;
        }

        // since a bolt instance belongs to one stream manager, 
        // for tracker rest api: expect just one metrics manager instance in the result;
        // for tmaster/metricscache stat interface: expect a list
//        for (Iterator<InstanceMetrics> it = streamManagerResult.values().iterator();
//          it.hasNext(); ) {
          InstanceMetrics stmgrInstanceResult = streamManagerResult.values().iterator().next();

          System.out.println("continue stmgr result instance metrics " + stmgrInstanceResult);
          Double valueSum = stmgrInstanceResult.getMetricValueSum(metric);
          if (valueSum == null) {
            System.out.println("continue valueSum null");
            continue;
          }
//        }
        double averageBp = valueSum / duration.getSeconds();

        // The maximum value of averageBp should be 1000, i.e. 1000 millis of BP per second. Due to
        // a bug in Heron (Issue: 1753), this value could be higher in some cases. The following
        // check partially corrects the reported BP value
        averageBp = averageBp > 1000 ? 1000 : averageBp;
        InstanceMetrics boltInstanceMetric
            = new InstanceMetrics(boltInstanceName, getMetricName(), averageBp);

        instanceMetrics.put(boltInstanceName, boltInstanceMetric);
      }

      ComponentMetrics componentMetrics = new ComponentMetrics(boltComponent, instanceMetrics);
      result.put(boltComponent, componentMetrics);
    }

    System.out.println("backpressure sensor " + result);
    return result;
  }
}
