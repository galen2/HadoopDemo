import storm.kafka.Broker;
import storm.kafka.BrokerHosts;
import storm.kafka.SpoutConfig;
import storm.kafka.StaticHosts;
import storm.kafka.trident.GlobalPartitionInformation;


public class SpoutConfigTest {

    private static String topic = "testing";

	
	public static void main(String[] args) {
		KafkaTestBroker broker = new KafkaTestBroker();

        
		GlobalPartitionInformation globalPartitionInformation = new GlobalPartitionInformation();
	    globalPartitionInformation.addPartition(0, Broker.fromString(broker.getBrokerConnectionString()));
	        
        BrokerHosts brokerHosts = new StaticHosts(globalPartitionInformation);
		SpoutConfig conf = new SpoutConfig(brokerHosts, topic, "E:/zookper", "0");
	}
}
