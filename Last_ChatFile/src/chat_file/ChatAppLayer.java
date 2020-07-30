package chat_file;

import java.util.ArrayList;


public class ChatAppLayer implements BaseLayer {
	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	private ArrayList<byte[]> bufferr = new ArrayList<byte[]>();
	private byte[] buffer;
	private byte[] toSendData;
	private int iterateNum=1;
	private int upperNum =0;

	private class _CAPP_HEADER {
		byte[] capp_totlen;
		byte capp_type;
		byte capp_unused;
		byte[] capp_data;
		
		public _CAPP_HEADER() {
			this.capp_totlen = new byte[2];
			this.capp_type = 0x00;
			this.capp_unused = 0x00;
			this.capp_data = null;
		}
	}

	_CAPP_HEADER m_sHeader = new _CAPP_HEADER();

	public ChatAppLayer(String pName) {
		// super(pName);
		// TODO Auto-generated constructor stub
		pLayerName = pName;
		ResetHeader();
	}

	public void ResetHeader() {
		for (int i = 0; i < 2; i++) {
			m_sHeader.capp_totlen[i] = (byte) 0x00;
		}
	}

	public byte[] ObjToByte(_CAPP_HEADER Header, byte[] input, int length) {
		byte[] buf = new byte[length + 4];

		buf[0] = Header.capp_totlen[0];
		buf[1] = Header.capp_totlen[1];
		buf[2] = Header.capp_type;
		buf[3] = Header.capp_unused;
		
		for (int i = 0; i < length; i++) {
			buf[4 + i] = input[i];
		}
		return buf;
	}

	public boolean Send(byte[] input, int length) {
		HandlingChat chatting = new HandlingChat("Send", input, length);
		Thread thr = new Thread(chatting);
		thr.start();
		return true;
	}
	public boolean SendChat(byte[] input, int length) {
		byte[] bytes, fragmentByte;

		if(upperNum==0) {
			toSendData = input;
		}
		if(!toSendData.equals(input)) {
			System.out.println(new String(toSendData));
			return false;
		}
//		System.out.println(toSendData.length+"????");
		if(toSendData.length > 1456) {//data가 1456바이트 초과일 경우
	
			if(toSendData.length%1456 ==0) upperNum = toSendData.length/1456;//iterateNum = fragment 개수
			else upperNum = toSendData.length/1456 +1;
			
			m_sHeader.capp_totlen[0] = (byte) (length & 0xFF);
			m_sHeader.capp_totlen[1] = (byte) ((0xFF00 & length) >> 8);//헤더에 총 길이 삽입
			for(int i=1;i<upperNum;i++) {
				fragmentByte = new byte[1456];
				
				if (i==1) {
					m_sHeader.capp_type = (byte)(0x01);
				}else {
					 m_sHeader.capp_type = (byte)0x02;
				}
				for(int j=0;j<1456;j++) {//data를 10바이트로 자르기
					fragmentByte[j]=input[(i-1)*1456 + j];
				}
				
				m_sHeader.capp_data = fragmentByte;
				bytes = ObjToByte(m_sHeader,fragmentByte,fragmentByte.length);//헤더 붙이기
				((EthernetLayer)this.GetUnderLayer()).Send(bytes,bytes.length, this);//Ethernet으로 Send	
				
			}
			if(toSendData.length%1456 !=0) fragmentByte = new byte[toSendData.length- (toSendData.length/1456)*1456];
			else fragmentByte = new byte[1456];
			for(int j=0;j<fragmentByte.length;j++) {//data를 10바이트로 자르기
				fragmentByte[j]=input[(upperNum-1)*1456 + j];
			}
			m_sHeader.capp_type = (byte)0x03;
			bytes = ObjToByte(m_sHeader,fragmentByte,fragmentByte.length);//헤더 붙이기
			((EthernetLayer)this.GetUnderLayer()).Send(bytes,bytes.length, this);//Ethernet으로 Send	
			
		}
		else {//1456바이트 이하일 경우,
			System.out.println(toSendData.length+">>>>>>>>>>>");
			upperNum =1;
			toSendData = input;
			m_sHeader.capp_totlen[0] = (byte) (length & 0xFF);
			m_sHeader.capp_totlen[1] = (byte) ((0xFF00 & length) >> 8);//헤더에 총 길이 삽입
			m_sHeader.capp_type = (byte)0x00;
			m_sHeader.capp_data = input;
			bytes = ObjToByte(m_sHeader,input,length);
			((EthernetLayer)this.GetUnderLayer()).Send(bytes,bytes.length,this);
		}
		upperNum =0;
		toSendData =null;
		return true;
	}

	public byte[] RemoveCappHeader(byte[] input, int length) {
		
		byte[] remvHeader = new byte[length-4];
		for(int i=0;i<length-4;i++) {
			remvHeader[i] = input[i+4];
		}
		return remvHeader;// 변경하세요 필요하시면
	}

	public synchronized boolean Receive(byte[] input) {
		HandlingChat forReceive = new HandlingChat("Receive",input);
		Thread receiveThread = new Thread(forReceive);
		receiveThread.start();
		return true;
	}

	public synchronized boolean ReceiveChat(byte[] input) {
		byte[] data;
		int wholeLength = 0, num;
		int lastFrag;
		
		wholeLength = (int) (input[0] & 0xFF) + (int) ((input[1] & 0xFF) <<8);
				
		if(wholeLength%1456 ==0) lastFrag = wholeLength/1456;
		else lastFrag = wholeLength/1456+1;
		System.out.println("whole len : "+wholeLength);
		if(wholeLength > 1456) {//must be fragment
			if(input[2] == (byte)0x01) buffer = new byte[wholeLength];
			data = RemoveCappHeader(input, input.length);

			if(input[2] != (byte)0x03) {
				byte[] frag = new byte[1456];
				System.arraycopy(data, 0, frag, 0, 1456);
				bufferr.add(frag);
			}else {
				if(wholeLength%1456==0) {
					byte[] frag = new byte[1456];
					System.arraycopy(data, 0, frag, 0, 1456);
					bufferr.add(frag);
				}else {
					byte[] frag = new byte[wholeLength- (wholeLength/1456)*1456];
					System.arraycopy(data, 0, frag, 0, frag.length);
					bufferr.add(frag);
				}
			}
			if(iterateNum == lastFrag) {
				System.out.println(bufferr.size());
				int buffer_element=0;
				for(int i=0;i<bufferr.size();i++) {
					System.out.println("pop");
					System.arraycopy(bufferr.get(i), 0, buffer, buffer_element, bufferr.get(i).length);
					buffer_element +=bufferr.get(i).length;
				}
				this.GetUpperLayer(0).Receive(buffer);
				
				iterateNum=1;
				bufferr = new ArrayList<byte[]>();
				return true;
			}
			iterateNum++;
			
		}else { //not fragment
			data = RemoveCappHeader(input, input.length);
			this.GetUpperLayer(0).Receive(data);
			iterateNum=1;
			return true;
		}
		
		
		return true;
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
		// nUpperLayerCount++;

	}

	@Override
	public void SetUpperUnderLayer(BaseLayer pUULayer) {
		this.SetUpperLayer(pUULayer);
		pUULayer.SetUnderLayer(this);
	}
	class HandlingChat implements Runnable{
		byte[] input=null;
		String type = null;
		int length;
		public HandlingChat(String type, byte[] input) {
			this.type = type;
			this.input =input;
		}
		public HandlingChat(String type, byte[] input, int length) {
			this.input = input;
			this.type = type;
			this.length =length;
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			if(this.type.equals("Receive")) {
				ReceiveChat(input);
			}else if(this.type.equals("Send")){
				SendChat(input,length);
			}
			return;
		}
	}
}
