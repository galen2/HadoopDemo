import java.io.File;
import java.io.IOException;

import org.apache.curator.test.TestingServer;


public class CustomerTestingServer extends TestingServer {
	public final int kafkaPort = 9092;
	public final String zkHost = "192.168.33.14:2181";
	
	public CustomerTestingServer() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}

	@Override
	public int getPort() {
		// TODO Auto-generated method stub
		return kafkaPort;
	}

	@Override
	public File getTempDirectory() {
		// TODO Auto-generated method stub
		return super.getTempDirectory();
	}

	@Override
	public String getConnectString() {
		// TODO Auto-generated method stub
		return zkHost;
	}

	@Override
	public void stop() throws IOException {
		// TODO Auto-generated method stub
		super.stop();
	}
	
	
	
	

}
