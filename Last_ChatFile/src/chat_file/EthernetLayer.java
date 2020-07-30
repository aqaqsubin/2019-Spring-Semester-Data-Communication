package chat_file;

import java.util.ArrayList;
import java.util.List;

import org.jnetpcap.Pcap;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;


public class EthernetLayer implements BaseLayer {
	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	List<byte[]> queue = new ArrayList<byte[]>();
	
	FileAppLayer fileApp = null;
	ChatAppLayer chatApp = null;
	int seq=1;
	
	NILayer ni = (NILayer)GetUnderLayer();

	private class _ETHERNET_ADDR{
		private byte[] addr = new byte[6];
		
		public _ETHERNET_ADDR() {
			this.addr[0] = (byte) 0x00;
			this.addr[1] = (byte) 0x00;
			this.addr[2] = (byte) 0x00;
			this.addr[3] = (byte) 0x00;
			this.addr[4] = (byte) 0x00;
			this.addr[5] = (byte) 0x00;
		}
		
	}
	private class _ETHERNET_Frame {
		_ETHERNET_ADDR enet_dstaddr;
		_ETHERNET_ADDR enet_srcaddr;
		byte[] enet_type;
		byte[] enet_data;
		
		public _ETHERNET_Frame() {
			this.enet_dstaddr = new _ETHERNET_ADDR();
			this.enet_srcaddr = new _ETHERNET_ADDR();
			this.enet_type = new byte[2];
			this.enet_data = null;
		}
	}

	_ETHERNET_Frame m_sHeader = new _ETHERNET_Frame();

	public EthernetLayer(String pName) {
		// super(pName);
		// TODO Auto-generated constructor stub
		pLayerName = pName;
		ResetHeader();
	}

	public void ResetHeader() {
		
		m_sHeader.enet_data = null;
		m_sHeader.enet_type = new byte[2];
		m_sHeader.enet_dstaddr = new _ETHERNET_ADDR();
		m_sHeader.enet_srcaddr = new _ETHERNET_ADDR();
	}

	public byte[] ObjToByte(_ETHERNET_Frame Header, byte[] input, int length) {
		byte[] buf = new byte[length + 14];
		
		buf[0] = Header.enet_dstaddr.addr[0];
		buf[1] = Header.enet_dstaddr.addr[1];
		buf[2] = Header.enet_dstaddr.addr[2];
		buf[3] = Header.enet_dstaddr.addr[3];
		buf[4] = Header.enet_dstaddr.addr[4];
		buf[5] = Header.enet_dstaddr.addr[5];
		buf[6] = Header.enet_srcaddr.addr[0];
		buf[7] = Header.enet_srcaddr.addr[1];
		buf[8] = Header.enet_srcaddr.addr[2];
		buf[9] = Header.enet_srcaddr.addr[3];
		buf[10] = Header.enet_srcaddr.addr[4];
		buf[11] = Header.enet_srcaddr.addr[5];
		buf[12] = Header.enet_type[0];
		buf[13] = Header.enet_type[1];

		
		for (int i = 0; i < length; i++) {
			buf[14 + i] = input[i];
		}
		
		return buf;
	}

	public boolean Send(byte[] input, int length, Object ob) {
		m_sHeader.enet_data = input;

		if(ob instanceof ChatAppLayer) {
			m_sHeader.enet_type[0] = (byte) 0x20;// data
			m_sHeader.enet_type[1] = (byte) 0x80;// data
		}else if(ob instanceof FileAppLayer){
			m_sHeader.enet_type[0] = (byte) 0x20;// file
			m_sHeader.enet_type[1] = (byte) 0x90;// file
		}
		
		byte[] bytes = ObjToByte(m_sHeader, input, length);
		
		(this.GetUnderLayer()).Send(bytes,bytes.length);
		return true;
	}

	public byte[] RemoveCappHeader(byte[] input, int length) {
		
		byte[] remvHeader = new byte[length-14];
		for(int i=0;i<length-14;i++) {
			remvHeader[i] = input[i+14];
		}
		return remvHeader;// 변경하세요 필요하시면
	}

	public synchronized boolean Receive(byte[] input) {
		byte[] data;
		
//		fileApp = (FileAppLayer)GetUpperLayer(1);
//		chatApp = (ChatAppLayer)GetUpperLayer(0);
		
		//브로드캐스트
		if(input[0]==-1 && input[1]==-1 && input[2]==-1 
				&& input[3]==-1 && input[4]==-1 && input[5]==-1 
				&& input[12]==0x08 && input[13]==0x06) {
				
			if(input[6]==m_sHeader.enet_srcaddr.addr[0] && input[7]==m_sHeader.enet_srcaddr.addr[1] &&
					input[8]==m_sHeader.enet_srcaddr.addr[2] && input[9]==m_sHeader.enet_srcaddr.addr[3] &&
					input[10]==m_sHeader.enet_srcaddr.addr[4] &&input[11]==m_sHeader.enet_srcaddr.addr[5]) {
				return false;
			}else {
				data = RemoveCappHeader(input, input.length);
				
				if(input[12] ==(byte) 0x20 && input[13]==(byte)0x80) {//data
					((ChatAppLayer)this.GetUpperLayer(0)).Receive(RemoveCappHeader(input, input.length));
					return true;
				}else if(input[12]==(byte)0x20 && input[13]==(byte)0x90) {//file
					System.out.println("ethernet "+seq);
					seq++;
					((FileAppLayer)this.GetUpperLayer(1)).Receive(RemoveCappHeader(input, input.length));
					return true;
				}
				return false;
			}
		}
		
		for (int i = 0; i < 6; i++) {
			if (input[i] != m_sHeader.enet_srcaddr.addr[i]) {
				return false;
			}
			if (input[i+6] != m_sHeader.enet_dstaddr.addr[i]) {
				return false;
			}
		}
		
		if(input[12] ==(byte) 0x20 && input[13]==(byte)0x80) {//data
			System.out.println("eth: "+input.length);
			((ChatAppLayer)this.GetUpperLayer(0)).Receive(RemoveCappHeader(input, input.length));
			return true;
		}else if(input[12]==(byte)0x20 && input[13]==(byte)0x90) {//file
			System.out.println("ethernet "+seq);
			seq++;
			((FileAppLayer)this.GetUpperLayer(1)).Receive(RemoveCappHeader(input, input.length));
			return true;
		}
		return false;
		
	}
	

	@Override
	public String GetLayerName() {
		// TODO Auto-generated method stub
		return pLayerName;
	}

	@Override
	public BaseLayer GetUnderLayer() {
		// TODO Auto-generated method stub
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
	public void SetUnderLayer(BaseLayer pUnderLayer) {
		// TODO Auto-generated method stub
		if (pUnderLayer == null)
			return;
		this.p_UnderLayer = pUnderLayer;
	}

	@Override
	public void SetUpperLayer(BaseLayer pUpperLayer) {
		// TODO Auto-generated method stub
		if (pUpperLayer == null)
			return;
		this.p_aUpperLayer.add(nUpperLayerCount++, pUpperLayer);
	}

	@Override
	public void SetUpperUnderLayer(BaseLayer pUULayer) {
		this.SetUpperLayer(pUULayer);
		pUULayer.SetUnderLayer(this);
	}

	public void SetEnetSrcAddress(byte[] srcAddress) {
		// TODO Auto-generated method stub
		m_sHeader.enet_srcaddr.addr = srcAddress;
	}

	public void SetEnetDstAddress(byte[] dstAddress) {
		// TODO Auto-generated method stub
		m_sHeader.enet_dstaddr.addr = dstAddress;
	}
	

//	class Chat_Receive_Thread implements Runnable {
//		
//		byte[] input;
//		public Chat_Receive_Thread(byte[] input) {
//			// TODO Auto-generated constructor stub
//			this.input = input;
//		}
//		@Override
//		public void run() {
//			byte[] data = RemoveCappHeader(input, input.length);
//			chatApp.Receive(data);
//			SendArrivedAck();
//			return;
//		}
//	}


}

