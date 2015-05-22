/**
 * Licensed to the Apache Software Foundation (ASF) under one
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
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import kafka.server.KafkaConfig;
import kafka.server.KafkaServerStartable;

import org.apache.commons.io.FileUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.InstanceSpec;
import org.apache.curator.test.TestingServer;

/**
 * Date: 11/01/2014
 * Time: 13:15
 */
public class KafkaTestBroker {

    private int port;
    private KafkaServerStartable kafka;//模拟kafka服务端
    private TestingServer server;//模拟zk服务引擎
    private CuratorFramework zookeeper;//模拟zk的客户端
    private File logDir;

    public KafkaTestBroker() {
        try {
//        	server = new TestingServer();
            server = new CustomerTestingServer();
            String zookeeperConnectionString = server.getConnectString();
            
            ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 3);
            zookeeper = CuratorFrameworkFactory.newClient(zookeeperConnectionString, retryPolicy);
            zookeeper.start();
//            port = InstanceSpec.getRandomPort();
            port = server.getPort();
//            logDir = new File(System.getProperty("java.io.tmpdir"), "kafka/logs/kafka-test-" + port);
            logDir = new File("E:/zookper", "kafka/logs/kafka-test-" + port);
            KafkaConfig config = buildKafkaConfig(zookeeperConnectionString);
            kafka = new KafkaServerStartable(config);
            kafka.startup();
        } catch (Exception ex) {
            throw new RuntimeException("Could not start test broker", ex);
        }
    }

    private kafka.server.KafkaConfig buildKafkaConfig(String zookeeperConnectionString) {
        Properties p = new Properties();
        p.setProperty("zookeeper.connect", zookeeperConnectionString);//127.0.0.1:51564
        p.setProperty("broker.id", "0");
        p.setProperty("port", "" + port);
        p.setProperty("log.dirs", logDir.getAbsolutePath());//E:\zookper\kafka\logs\kafka-test-51654
        return new KafkaConfig(p);
    }

    public String getBrokerConnectionString() {
        return "192.168.33.14:" + port;
    }

    public int getPort() {
        return port;
    }
    public void shutdown() {
        kafka.shutdown();
        if (zookeeper.getState().equals(CuratorFrameworkState.STARTED)) {
            zookeeper.close();
        }
        try {
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileUtils.deleteQuietly(logDir);
    }
}
