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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamsets.datacollector.runner;

import com.streamsets.datacollector.main.RuntimeInfo;
import com.streamsets.datacollector.runner.production.OffsetFileUtil;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.File;

public class TestOffsetFileUtil {

  @Rule
  public TemporaryFolder tempFolder= new TemporaryFolder();

  @Test
  public void testSaveAndGetOffset() throws Exception {
    RuntimeInfo runtimeInfo = Mockito.mock(RuntimeInfo.class);
    File offsetFolder = tempFolder.newFolder();
    Mockito.when(runtimeInfo.getDataDir()).thenReturn(offsetFolder.getPath());
    OffsetFileUtil.saveIfEmpty(runtimeInfo, "foo", "1");
    Assert.assertNull(OffsetFileUtil.getOffset(runtimeInfo, "foo", "1"));
    OffsetFileUtil.saveOffset(runtimeInfo, "foo", "1", "offset:100");
    Assert.assertEquals("offset:100", OffsetFileUtil.getOffset(runtimeInfo, "foo", "1"));
  }

  @Test
  public void testResetOffset() throws Exception {
    RuntimeInfo runtimeInfo = Mockito.mock(RuntimeInfo.class);
    File offsetFolder = tempFolder.newFolder();
    Mockito.when(runtimeInfo.getDataDir()).thenReturn(offsetFolder.getPath());
    OffsetFileUtil.saveOffset(runtimeInfo, "foo", "1", "offset:1000");
    OffsetFileUtil.resetOffset(runtimeInfo, "foo", "1");
    Assert.assertNull(OffsetFileUtil.getOffset(runtimeInfo, "foo", "1"));
  }
}
