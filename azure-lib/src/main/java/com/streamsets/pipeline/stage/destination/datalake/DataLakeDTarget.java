/**
 * Copyright 2016 StreamSets Inc.
 *
 * Licensed under the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamsets.pipeline.stage.destination.datalake;

import com.streamsets.pipeline.api.ConfigDefBean;
import com.streamsets.pipeline.api.ConfigGroups;
import com.streamsets.pipeline.api.ExecutionMode;
import com.streamsets.pipeline.api.GenerateResourceBundle;
import com.streamsets.pipeline.api.StageDef;
import com.streamsets.pipeline.api.Target;
import com.streamsets.pipeline.configurablestage.DTarget;

@StageDef(
    version = 1,
    label = "Data Lake Store",
    description = "Writes data to Azure Data Lake Store",
    icon = "data-lake-store.png",
    execution = ExecutionMode.STANDALONE,
    recordsByRef = true,
    onlineHelpRefUrl = "index.html#Destinations/DataLake.html#task_jfl_nf4_zx"
)
@ConfigGroups(value = Groups.class)
@GenerateResourceBundle
public class DataLakeDTarget extends DTarget {
  @ConfigDefBean(groups = {"DATALAKE"})
  public DataLakeConfigBean dataLakeConfig;

  @Override
  protected Target createTarget() {
    return new DataLakeTarget(dataLakeConfig);
  }
}
