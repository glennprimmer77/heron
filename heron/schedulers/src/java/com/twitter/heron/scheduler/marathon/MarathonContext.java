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

package com.twitter.heron.scheduler.marathon;


import com.twitter.heron.spi.common.Config;
import com.twitter.heron.spi.common.Context;

public final class MarathonContext extends Context {
  public static final String HERON_MARATHON_SCHEDULER_URI = "heron.marathon.scheduler.uri";
  public static final String HERON_EXECUTOR_DOCKER_IMAGE = "heron.executor.docker.image";
  public static final String HERON_MARATHON_SCHEDULER_AUTH_TOKEN =
      "heron.marathon.scheduler.auth.token";

  private MarathonContext() {
  }

  public static String getSchedulerURI(Config config) {
    return config.getStringValue(HERON_MARATHON_SCHEDULER_URI);
  }

  public static String getExecutorDockerImage(Config config) {
    return config.getStringValue(HERON_EXECUTOR_DOCKER_IMAGE);
  }

  public static String getSchedulerAuthToken(Config config) {
    return config.getStringValue(HERON_MARATHON_SCHEDULER_AUTH_TOKEN);
  }
}
