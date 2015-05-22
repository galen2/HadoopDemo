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
package storm.kafka;

import backtype.storm.Config;
import backtype.storm.metric.api.IMetric;
import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import kafka.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clojure.main;
import storm.kafka.PartitionManager.KafkaMessageId;

import java.util.*;

// TODO: need to add blacklisting
// TODO: need to make a best effort to not re-emit messages if don't have to
public class KafkaSpouts extends BaseRichSpout {
    public static class MessageAndRealOffset {
        public Message msg;
        public long offset;

        public MessageAndRealOffset(Message msg, long offset) {
            this.msg = msg;
            this.offset = offset;
        }
    }

    static enum EmitState {
        EMITTED_MORE_LEFT,
        EMITTED_END,
        NO_EMITTED
    }

    public static final Logger LOG = LoggerFactory.getLogger(KafkaSpouts.class);

    String _uuid = UUID.randomUUID().toString();
    SpoutConfig _spoutConfig;
    SpoutOutputCollector _collector;
    PartitionCoordinator _coordinator;
    DynamicPartitionConnections _connections;
    ZkState _state;

    long _lastUpdateMs = 0;

    int _currPartitionIndex = 0;

    public KafkaSpouts(SpoutConfig spoutConf) {
        _spoutConfig = spoutConf;
    }

    public void open(Map conf, final TopologyContext context, final SpoutOutputCollector collector) {
        _collector = collector;
        /**
         * {"storm.id" "wordCounter-1-1432104445", "dev.zookeeper.path" "/tmp/dev-storm-zookeeper",
         *  "topology.tick.tuple.freq.secs" 30, "topology.builtin.metrics.bucket.size.secs" 60, 
         *  "topology.fall.back.on.java.serialization" true, 
         *  "topology.max.error.report.per.interval" 5, "zmq.linger.millis" 0,
         *   "topology.skip.missing.kryo.registrations" true, 
         *   "storm.messaging.netty.client_worker_threads" 1,
         *    "ui.childopts" "-Xmx768m", "storm.zookeeper.session.timeout" 20000, "nimbus.reassign" true, "topology.trident.batch.emit.interval.millis" 50, "storm.messaging.netty.flush.check.interval.ms" 10, "nimbus.monitor.freq.secs" 10, "logviewer.childopts" "-Xmx128m", "java.library.path" "/usr/local/lib:/opt/local/lib:/usr/lib", "topology.executor.send.buffer.size" 1024, "storm.local.dir" "C:\\Users\\ADMINI~1\\AppData\\Local\\Temp\\8443fa11-216b-4c8b-9528-e057d7afe985", "storm.messaging.netty.buffer_size" 5242880, "supervisor.worker.start.timeout.secs" 120, "topology.enable.message.timeouts" true, "nimbus.cleanup.inbox.freq.secs" 600, "nimbus.inbox.jar.expiration.secs" 3600, "drpc.worker.threads" 64, "storm.meta.serialization.delegate" "backtype.storm.serialization.DefaultSerializationDelegate", "topology.worker.shared.thread.pool.size" 4, "nimbus.host" "localhost", "storm.messaging.netty.min_wait_ms" 100, "storm.zookeeper.port" 2000, "transactional.zookeeper.port" nil, "topology.executor.receive.buffer.size" 1024, "transactional.zookeeper.servers" nil, "storm.zookeeper.root" "/storm", "storm.zookeeper.retry.intervalceiling.millis" 30000, "supervisor.enable" true, "storm.messaging.netty.server_worker_threads" 1, "storm.zookeeper.servers" ["localhost"], "transactional.zookeeper.root" "/transactional", "topology.acker.executors" nil, "topology.kryo.decorators" (), "topology.name" "wordCounter", "topology.transfer.buffer.size" 1024, "topology.worker.childopts" nil, "drpc.queue.size" 128, "worker.childopts" "-Xmx768m", "supervisor.heartbeat.frequency.secs" 5, "topology.error.throttle.interval.secs" 10, "zmq.hwm" 0, "drpc.port" 3772, "supervisor.monitor.frequency.secs" 3, "drpc.childopts" "-Xmx768m", "topology.receiver.buffer.size" 8, "task.heartbeat.frequency.secs" 3, "topology.tasks" nil, "storm.messaging.netty.max_retries" 300, "topology.spout.wait.strategy" "backtype.storm.spout.SleepSpoutWaitStrategy", "nimbus.thrift.max_buffer_size" 1048576, "topology.max.spout.pending" nil, "storm.zookeeper.retry.interval" 1000, "topology.sleep.spout.wait.strategy.time.ms" 1, "nimbus.topology.validator" "backtype.storm.nimbus.DefaultTopologyValidator", "supervisor.slots.ports" (1024 1025 1026), "topology.environment" nil, "topology.debug" true, "nimbus.task.launch.secs" 120, "nimbus.supervisor.timeout.secs" 60, "topology.kryo.register" nil, "topology.message.timeout.secs" 30, "task.refresh.poll.secs" 10, "topology.workers" 2, "supervisor.childopts" "-Xmx256m", "nimbus.thrift.port" 6627, "topology.stats.sample.rate" 0.05, "worker.heartbeat.frequency.secs" 1, "topology.tuple.serializer" "backtype.storm.serialization.types.ListDelegateSerializer", "topology.disruptor.wait.strategy" "com.lmax.disruptor.BlockingWaitStrategy", "topology.multilang.serializer" "backtype.storm.multilang.JsonSerializer", "nimbus.task.timeout.secs" 30, "storm.zookeeper.connection.timeout" 15000, "topology.kryo.factory" "backtype.storm.serialization.DefaultKryoFactory", "drpc.invocations.port" 3773, "logviewer.port" 8000, "zmq.threads" 1, "storm.zookeeper.retry.times" 5, "topology.worker.receiver.thread.count" 1, "storm.thrift.transport" "backtype.storm.security.auth.SimpleTransportPlugin", "topology.state.synchronization.timeout.secs" 60, "supervisor.worker.timeout.secs" 30, "nimbus.file.copy.expiration.secs" 600, "storm.messaging.transport" "backtype.storm.messaging.netty.Context", "logviewer.appender.name" "A1", "storm.messaging.netty.max_wait_ms" 1000, "drpc.request.timeout.secs" 600, "storm.local.mode.zmq" false, "ui.port" 8080, "nimbus.childopts" "-Xmx1024m", "storm.cluster.mode" "local", "topology.max.task.parallelism" nil,
         *  "storm.messaging.netty.transfer.batch.size" 262144, "topology.classpath" nil}
         */
        Map stateConf = new HashMap(conf);
        List<String> zkServers = _spoutConfig.zkServers;
        if (zkServers == null) {
            zkServers = (List<String>) conf.get(Config.STORM_ZOOKEEPER_SERVERS);
        }
        Integer zkPort = _spoutConfig.zkPort;
        if (zkPort == null) {
            zkPort = ((Number) conf.get(Config.STORM_ZOOKEEPER_PORT)).intValue();
        }
        stateConf.put(Config.TRANSACTIONAL_ZOOKEEPER_SERVERS, zkServers);
        stateConf.put(Config.TRANSACTIONAL_ZOOKEEPER_PORT, zkPort);
        stateConf.put(Config.TRANSACTIONAL_ZOOKEEPER_ROOT, _spoutConfig.zkRoot);
        _state = new ZkState(stateConf);

        _connections = new DynamicPartitionConnections(_spoutConfig, KafkaUtils.makeBrokerReader(conf, _spoutConfig));

        // using TransactionalState like this is a hack
        int totalTasks = context.getComponentTasks(context.getThisComponentId()).size();// conf.setNumWorkers(1);确定
        if (_spoutConfig.hosts instanceof StaticHosts) {
            _coordinator = new StaticCoordinator(_connections, conf, _spoutConfig, _state, context.getThisTaskIndex(), totalTasks, _uuid);
        } else {
            _coordinator = new ZkCoordinator(_connections, conf, _spoutConfig, _state, context.getThisTaskIndex(), totalTasks, _uuid);
        }

        context.registerMetric("kafkaOffset", new IMetric() {
            KafkaUtils.KafkaOffsetMetric _kafkaOffsetMetric = new KafkaUtils.KafkaOffsetMetric(_spoutConfig.topic, _connections);

            public Object getValueAndReset() {
                List<PartitionManager> pms = _coordinator.getMyManagedPartitions();
                Set<Partition> latestPartitions = new HashSet();
                for (PartitionManager pm : pms) {
                    latestPartitions.add(pm.getPartition());
                }
                _kafkaOffsetMetric.refreshPartitions(latestPartitions);
                for (PartitionManager pm : pms) {
                    _kafkaOffsetMetric.setLatestEmittedOffset(pm.getPartition(), pm.lastCompletedOffset());
                }
                return _kafkaOffsetMetric.getValueAndReset();
            }
        }, _spoutConfig.metricsTimeBucketSizeInSecs);

        context.registerMetric("kafkaPartition", new IMetric() {
            public Object getValueAndReset() {
                List<PartitionManager> pms = _coordinator.getMyManagedPartitions();
                Map concatMetricsDataMaps = new HashMap();
                for (PartitionManager pm : pms) {
                    concatMetricsDataMaps.putAll(pm.getMetricsDataMap());
                }
                return concatMetricsDataMaps;
            }
        }, _spoutConfig.metricsTimeBucketSizeInSecs);
    }

    @Override
    public void close() {
        _state.close();
    }

    public void nextTuple() {
        List<PartitionManager> managers = _coordinator.getMyManagedPartitions();
        for (int i = 0; i < managers.size(); i++) {

            try {
                // in case the number of managers decreased
                _currPartitionIndex = _currPartitionIndex % managers.size();
                
                /**
                 *每次消费一条消息tuple， 然后提交commit（）
                 */
                EmitState state = managers.get(_currPartitionIndex).next(_collector);
                if (state != EmitState.EMITTED_MORE_LEFT) {
                    _currPartitionIndex = (_currPartitionIndex + 1) % managers.size();
                }
                if (state != EmitState.NO_EMITTED) {
                    break;
                }
            } catch (FailedFetchException e) {
                LOG.warn("Fetch failed", e);
                _coordinator.refresh();
            }
        }

        long now = System.currentTimeMillis();
        if ((now - _lastUpdateMs) > _spoutConfig.stateUpdateIntervalMs) {
            commit();
        }
    }

    @Override
    public void ack(Object msgId) {
        KafkaMessageId id = (KafkaMessageId) msgId;
        PartitionManager m = _coordinator.getManager(id.partition);
        if (m != null) {
            m.ack(id.offset);
        }
    }

    @Override
    public void fail(Object msgId) {
        KafkaMessageId id = (KafkaMessageId) msgId;
        PartitionManager m = _coordinator.getManager(id.partition);
        if (m != null) {
            m.fail(id.offset);
        }
    }

    @Override
    public void deactivate() {
        commit();
    }

    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(_spoutConfig.scheme.getOutputFields());
    }

    private void commit() {
        _lastUpdateMs = System.currentTimeMillis();
        for (PartitionManager manager : _coordinator.getMyManagedPartitions()) {
            manager.commit();
        }
    }

}
