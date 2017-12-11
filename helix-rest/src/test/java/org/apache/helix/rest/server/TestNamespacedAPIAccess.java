package org.apache.helix.rest.server;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.helix.PropertyKey;
import org.apache.helix.rest.common.HelixRestNamespace;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestNamespacedAPIAccess extends AbstractTestClass {
  @Test
  public void testDefaultNamespaceCompatibility() {
    String testClusterName1 = "testClusterForDefaultNamespaceCompatibility1";
    String testClusterName2 = "testClusterForDefaultNamespaceCompatibility2";

    // Create from namespaced API and ensure we can access it from old apis, and vice-versa
    // Assume other api end points will behave the same way
    put(String.format("/namespaces/%s/clusters/%s", HelixRestNamespace.DEFAULT_NAMESPACE_NAME, testClusterName1), null,
        Entity.entity("", MediaType.APPLICATION_JSON_TYPE), Response.Status.CREATED.getStatusCode());
    get(String.format("/clusters/%s", testClusterName1), Response.Status.OK.getStatusCode(), false);

    put(String.format("/clusters/%s", testClusterName2), null, Entity.entity("", MediaType.APPLICATION_JSON_TYPE),
        Response.Status.CREATED.getStatusCode());
    get(String.format("/namespaces/%s/clusters/%s", HelixRestNamespace.DEFAULT_NAMESPACE_NAME, testClusterName2),
        Response.Status.OK.getStatusCode(), false);
  }


  @Test
  public void testNamespacedCRUD() throws IOException {
    String testClusterName = "testClusterForNamespacedCRUD";

    // Create cluster in test namespace and verify it's only appears in test namespace
    put(String.format("/namespaces/%s/clusters/%s", TEST_NAMESPACE, testClusterName), null,
        Entity.entity("", MediaType.APPLICATION_JSON_TYPE), Response.Status.CREATED.getStatusCode());
    get(String.format("/namespaces/%s/clusters/%s", TEST_NAMESPACE, testClusterName),
        Response.Status.OK.getStatusCode(), false);
    get(String.format("/clusters/%s", testClusterName), Response.Status.NOT_FOUND.getStatusCode(), false);

    // Create cluster with same name in different namespacces
    put(String.format("/clusters/%s", testClusterName), null, Entity.entity("", MediaType.APPLICATION_JSON_TYPE),
        Response.Status.CREATED.getStatusCode());
    get(String.format("/clusters/%s", testClusterName), Response.Status.OK.getStatusCode(), false);

    // Modify cluster in default namespace
    post(String.format("/clusters/%s", testClusterName), ImmutableMap.of("command", "disable"),
        Entity.entity("", MediaType.APPLICATION_JSON_TYPE), Response.Status.OK.getStatusCode());

    // Verify the cluster in default namespace is modified, while the one in test namespace is not.
    PropertyKey.Builder keyBuilder = new PropertyKey.Builder(testClusterName);
    Assert.assertTrue(_baseAccessor.exists(keyBuilder.pause().getPath(), 0));
    Assert.assertFalse(_baseAccessorTestNS.exists(keyBuilder.pause().getPath(), 0));

    // Verify that deleting cluster in one namespace will not affect the other
    delete(String.format("/namespaces/%s/clusters/%s", TEST_NAMESPACE, testClusterName),
        Response.Status.OK.getStatusCode());
    get(String.format("/namespaces/%s/clusters/%s", TEST_NAMESPACE, testClusterName),
        Response.Status.NOT_FOUND.getStatusCode(), false);
    get(String.format("/clusters/%s", testClusterName), Response.Status.OK.getStatusCode(), false);
  }

}