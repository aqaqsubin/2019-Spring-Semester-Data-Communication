package chat_file;


import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.jnetpcap.Pcap;
import org.jnetpcap.PcapIf;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;


public class NILayer implements BaseLayer {

	private Receive_Thread thread = null; 
	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	
	int m_iNumAdapter;
	public Pcap m_AdapterObject;
	public PcapIf device;
	public List<PcapIf> m_pAdapterList;
	StringBuilder errbuf = new StringBuilder();

	public NILayer(String pName) {//pName을 pLayerName으로 한다
		pLayerName = pName;
		m_pAdapterList = new ArrayList<PcapIf>();
		m_iNumAdapter =0;
		SetAdapterList();
	}
	public void SetAdapterList() {
		int r = Pcap.findAllDevs(m_pAdapterList, errbuf);
		if( r == Pcap.NOT_OK || m_pAdapterList.isEmpty()) {
			System.err.printf("Can't read list of devices, error is %s", errbuf.toString());
			return;
		}
		System.out.println("adapter : "+m_pAdapterList.size());
	}
	public void SetAdapterNumber(int iNum) {
		m_iNumAdapter = iNum;
		PacketStartDriver();
		Receive();
	}
	public void PacketStartDriver() {
		int snaplen = 64 * 1024;
		int flags = Pcap.MODE_PROMISCUOUS;
		int timeout = 10 * 1000;
		m_AdapterObject = Pcap.openLive(m_pAdapterList.get(m_iNumAdapter).getName(), snaplen, flags, timeout, errbuf);
	}

	public boolean Send(byte[] input, int length) {
		ByteBuffer buf = ByteBuffer.wrap(input);
		if(buf.equals(null))System.out.println("buffer is null");
		if(m_AdapterObject.sendPacket(buf) != Pcap.OK) {
			System.err.printf(m_AdapterObject.getErr());
			return false;
		}
		return true;
	}

	public boolean Receive() {
		
		if(thread != null) {
			return false;
		}
		else {
			thread = new Receive_Thread(m_AdapterObject, this.GetUpperLayer(0));
			Thread obj = new Thread(thread);
			obj.start();
			return false;
		}
	}
	

	@Override
	public void SetUnderLayer(BaseLayer pUnderLayer) {
		// TODO Auto-generated method stub
		if (pUnderLayer == null)
			return;
		p_UnderLayer = pUnderLayer;// pUnderLayer을 p_UnderLayer로 한다
	}

	@Override
	public void SetUpperLayer(BaseLayer pUpperLayer) {
		// TODO Auto-generated method stub
		if (pUpperLayer == null)
			return;
		this.p_aUpperLayer.add(nUpperLayerCount++, pUpperLayer); //배열로 하여 this보다 upper인 layer들 쌓기
	}

	@Override
	public String GetLayerName() {
		// TODO Auto-generated method stub
		return pLayerName;
	}

	@Override
	public BaseLayer GetUnderLayer() {
		if (p_UnderLayer == null)
			return null;
		return p_UnderLayer;
	}

	@Override
	public BaseLayer GetUpperLayer(int nindex) {
		// TODO Auto-generated method stub
		if (nindex < 0 || nindex > nUpperLayerCount || nUpperLayerCount < 0)
			return null;
		return p_aUpperLayer.get(nindex);
	}

	@Override
	public void SetUpperUnderLayer(BaseLayer pUULayer) {
		this.SetUpperLayer(pUULayer);
		pUULayer.SetUnderLayer(this);

	}

}

class Receive_Thread implements Runnable {
	byte[] data;
	Pcap AdapterObject;
	BaseLayer UpperLayer;

	public Receive_Thread(Pcap m_AdapterObject, BaseLayer m_UpperLayer) {
		// TODO Auto-generated constructor stub

		AdapterObject = m_AdapterObject;
		UpperLayer = m_UpperLayer;
	}

	@Override
	public void run() {
		while (true) {
			PcapPacketHandler<String> jpacketHandler = new PcapPacketHandler<String>() {
				public void nextPacket(PcapPacket packet, String user) {
					data = packet.getByteArray(0, packet.size());
					UpperLayer.Receive(data);
				}
			};
			AdapterObject.loop(100000, jpacketHandler,"");
		}

	}

}
