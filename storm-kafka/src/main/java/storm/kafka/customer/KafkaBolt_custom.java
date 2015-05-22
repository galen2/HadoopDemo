package storm.kafka.customer;

import java.util.Map;

import backtype.storm.task.TopologyContext;
import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.topology.IBasicBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class KafkaBolt_custom implements IBasicBolt{
    public void prepare(Map conf, TopologyContext context) {
   	  System.out.println(conf);
     }
    
     public void execute(Tuple tuple, BasicOutputCollector collector) {
         String sentence = tuple.getString(0);
         collector.emit(new Values(sentence));
        /*  for(String word: sentence.split(" ")) {
                   collector.emit(new Values(word));
             }*/
     }

   public void cleanup() {}

   public void declareOutputFields(OutputFieldsDeclarer declarer) {
//           declarer.declare(new Fields("singleWord"));
   }

	public Map<String, Object> getComponentConfiguration() {
		// TODO Auto-generated method stub
		return null;
	}
} 
