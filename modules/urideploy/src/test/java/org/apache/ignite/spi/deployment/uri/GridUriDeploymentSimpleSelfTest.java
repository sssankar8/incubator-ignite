/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.spi.deployment.uri;

import org.apache.ignite.cluster.*;
import org.apache.ignite.compute.*;
import org.apache.ignite.internal.util.typedef.internal.*;
import org.apache.ignite.spi.deployment.*;
import org.apache.ignite.testframework.config.*;
import org.apache.ignite.testframework.junits.spi.*;

import java.io.*;
import java.util.*;

/**
 *
 */
@GridSpiTest(spi = UriDeploymentSpi.class, group = "Deployment SPI")
public class GridUriDeploymentSimpleSelfTest extends GridSpiAbstractTest<UriDeploymentSpi> {
    /**
     * @return List of URI to use as deployment source.
     */
    @GridSpiTestConfig
    public List<String> getUriList() {
        // No real gar file is required. Add one just to avoid failure because of missed default directory.
        return Collections.singletonList(GridTestProperties.getProperty("ant.urideployment.gar.uri").
            replace("EXTDATA", U.resolveIgnitePath("modules/extdata").getAbsolutePath()));
    }

    /**
     * @throws Exception If failed.
     */
    public void testSimpleDeploy() throws Exception {
        UriDeploymentSpi spi = getSpi();

        spi.register(TestTask.class.getClassLoader(), TestTask.class);

        DeploymentResource task = spi.findResource(TestTask.class.getName());

        assert task != null;
        assert task.getResourceClass() == TestTask.class;
        assert spi.findResource("TestTaskWithName") == null;

        spi.unregister(TestTask.class.getName());

        assert spi.findResource(TestTask.class.getName()) == null;
    }

    /**
     * @throws Exception If failed.
     */
    public void testSimpleRedeploy() throws Exception {
        for (int i = 0; i < 100; i++)
            testSimpleDeploy();
    }

    /**
     * @throws Exception If failed.
     */
    public void testSimpleDeployWithName() throws Exception {
        UriDeploymentSpi spi = getSpi();

        spi.register(TestTaskWithName.class.getClassLoader(), TestTaskWithName.class);

        DeploymentResource task = spi.findResource("TestTaskWithName");

        assert task != null;
        assert task.getResourceClass() == TestTaskWithName.class;
        assert spi.findResource(TestTaskWithName.class.getName()) != null;

        spi.unregister("TestTaskWithName");

        assert spi.findResource("TestTaskWithName") == null;
    }

    /**
     * @throws Exception If failed.
     */
    public void testSimpleRedeployWithName() throws Exception {
        for (int i = 0; i < 100; i++)
            testSimpleDeployWithName();
    }

    /**
     * @throws Exception If failed.
     */
    public void testSimpleDeployTwoTasks() throws Exception {
        UriDeploymentSpi spi = getSpi();

        spi.register(TestTask.class.getClassLoader(), TestTask.class);
        spi.register(TestTaskWithName.class.getClassLoader(), TestTaskWithName.class);

        DeploymentResource task1 = spi.findResource("TestTaskWithName");
        DeploymentResource task2 = spi.findResource(TestTask.class.getName());

        assert task1 != null;
        assert task1.getResourceClass() == TestTaskWithName.class;
        assert spi.findResource(TestTaskWithName.class.getName()) != null;

        assert task2 != null;
        assert task2.getResourceClass() == TestTask.class;
        assert spi.findResource("TestTask") == null;

        spi.unregister("TestTaskWithName");

        assert spi.findResource("TestTaskWithName") == null;

        spi.unregister(TestTask.class.getName());

        assert spi.findResource(TestTask.class.getName()) == null;
    }

    /**
     * @throws Exception If failed.
     */
    public void testSimpleRedeployTwoTasks() throws Exception {
        for (int i = 0; i < 100; i++)
            testSimpleDeployTwoTasks();
    }

    /**
     * Test task.
     */
    private static class TestTask extends ComputeTaskAdapter<Object, Object> {
        /** {@inheritDoc} */
        @Override public Map<? extends ComputeJob, ClusterNode> map(List<ClusterNode> subgrid, Object arg) {
            assert subgrid.size() == 1;

            return Collections.singletonMap(new ComputeJobAdapter() {
                @Override public Serializable execute() { return "result"; }
            }, subgrid.get(0));
        }

        /** {@inheritDoc} */
        @Override public Object reduce(List<ComputeJobResult> results) {
            assert results.size() == 1;

            return results.get(0).getData();
        }
    }

    /**
     * Named test task.
     */
    @ComputeTaskName("TestTaskWithName")
    private static class TestTaskWithName extends ComputeTaskAdapter<Object, Object> {
        /** {@inheritDoc} */
        @Override public Map<? extends ComputeJob, ClusterNode> map(List<ClusterNode> subgrid, Object arg) {
            assert subgrid.size() == 1;

            return Collections.singletonMap(new ComputeJobAdapter() {
                @Override public Serializable execute() { return "result"; }
            }, subgrid.get(0));
        }

        /** {@inheritDoc} */
        @Override public Object reduce(List<ComputeJobResult> results) {
            assert results.size() == 1;

            return results.get(0).getData();
        }
    }
}
