/*
 * Copyright (c) 2008 - 2013 10gen, Inc. <http://10gen.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mongodb.connection;

import org.junit.Before;
import org.junit.Test;
import org.mongodb.MongoClientOptions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.mongodb.connection.ClusterConnectionMode.Discovering;
import static org.mongodb.connection.ClusterType.ReplicaSet;
import static org.mongodb.connection.ServerConnectionStatus.Connected;

public class DefaultMultiClusterTest {
    private TestClusterableServerFactory factory;

    @Before
    public void setUp() {
        factory = new TestClusterableServerFactory();
    }
    @Test
    public void testDiscovery() {
        ServerAddress serverAddress = new ServerAddress("localhost:27017");
        DefaultMultiServerCluster cluster = new DefaultMultiServerCluster(Arrays.asList(serverAddress), null,
                MongoClientOptions.builder().build(), factory);
        factory.getServer(serverAddress).sendNotification(
                ServerDescription.builder().address(serverAddress).ok(true).status(Connected)
                        .type(ServerType.ReplicaSetSecondary)
                        .hosts(new HashSet<String>(Arrays.asList("localhost:27017", "localhost:27018", "localhost:27019")))
                        .build());
        final ClusterDescription clusterDescription = cluster.getDescription();
        assertTrue(clusterDescription.isConnecting());
        assertEquals(ReplicaSet, clusterDescription.getType());
        assertEquals(Discovering, clusterDescription.getMode());
        Iterator<ServerDescription> iter = clusterDescription.getAll().iterator();
        assertTrue(iter.hasNext());
        ServerDescription first = iter.next();
        assertEquals(factory.getServer(serverAddress).getDescription(), first);
        assertTrue(iter.hasNext());
        ServerDescription second = iter.next();
        TestServer secondServer = factory.getServer(new ServerAddress("localhost:27018"));
        assertNotNull(secondServer);
        assertEquals(secondServer.getDescription(), second);
        assertTrue(iter.hasNext());
        ServerDescription third = iter.next();
        TestServer thirdServer = factory.getServer(new ServerAddress("localhost:27019"));
        assertNotNull(thirdServer);
        assertEquals(thirdServer.getDescription(), third);
    }
}