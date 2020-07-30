package chat_file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class FileAppLayer implements BaseLayer {
	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	private List<byte[]> buffer = new ArrayList<byte[]>();
	private List<Object> index_buffer = new ArrayList<Object>();
	private byte[] toSendData = null;
	private int upperNum =0;
	private int iterateNum=0;
	
	forProgressBar progress = new forProgressBar();
	
	
	OutputStream output = null;
	String fileName = null;

	private String receive_fileName = null;
	
	Thread sendThread=null;
	Thread receiveThread=null;

	private class _FAPP_HEADER {
		byte[] fapp_totlen;
		byte[] fapp_type;
		byte fapp_msg_type;
		byte ed;
		byte[] fapp_seq_num;
		byte[] fapp_data;
		
		public _FAPP_HEADER() {
			this.fapp_totlen = new byte[4];
			this.fapp_type = new byte[2];
			this.fapp_msg_type =0x00;
			this.ed= 0x00;
			this.fapp_seq_num = new byte[4];
			this.fapp_data = null;
		}
	}

	_FAPP_HEADER m_sHeader = new _FAPP_HEADER();

	public FileAppLayer(String pName) {
		// super(pName);
		// TODO Auto-generated constructor stub
		pLayerName = pName;
		ResetHeader();
	}

	public void ResetHeader() {
		for (int i = 0; i < 4; i++) {
			m_sHeader.fapp_totlen[i] = (byte) 0x00;
		}
	}

	public byte[] ObjToByte(_FAPP_HEADER Header, byte[] input, int length) {
		byte[] buf = new byte[length + 12];

		buf[0] = Header.fapp_totlen[0];
		buf[1] = Header.fapp_totlen[1];
		buf[2] = Header.fapp_totlen[2];
		buf[3] = Header.fapp_totlen[3];
		buf[4] = Header.fapp_type[0];
		buf[5] = Header.fapp_type[1];
		buf[6] = Header.fapp_msg_type;
		buf[7] = Header.ed;
		buf[8] = Header.fapp_seq_num[0];
		buf[9] = Header.fapp_seq_num[1];
		buf[10] = Header.fapp_seq_num[2];
		buf[11] = Header.fapp_seq_num[3];

		for (int i = 0; i < length; i++) {
			buf[12 + i] = input[i];
		}
		return buf;
	}
	public boolean Send(byte[] input, int length) {
		
		HandlingFile sendFile = new HandlingFile("Send",input, length);
		Thread snd = new Thread(sendFile);
		snd.start();
		return true;
	}
	public boolean SendFile(byte[] input, int length) {
		
		byte[] bytes, fragmentByte;
		byte[] whole = null;
		
		String temp_name = new String(input);
		System.out.println(temp_name);
		String[] temp_file_name = temp_name.split("\\\\");
		((SimplestDlg)GetUpperLayer(0)).fileSelect.setText("파일 읽는 중...");
		try {
			InputStream inputStream = new FileInputStream(temp_name);
			byte[] b = new byte[inputStream.available()];
			
			byte[] temp_whole=null;
			int wholeLen=0;
			int i=0; 
			int size=0;
			
			while((i = inputStream.read(b))!=-1) {
				wholeLen = size+i;
				temp_whole = new byte[wholeLen];
				if(size !=0) {
					System.arraycopy(whole, 0, temp_whole, 0, size);
				}
				System.arraycopy(b, 0, temp_whole,size,i);
				size = wholeLen;
				whole = temp_whole;
//				System.out.println();
//				b= new byte[inputStream.available()];
			}
			inputStream.close();
		}catch(Exception err) {
			err.printStackTrace();
		}
		((SimplestDlg)GetUpperLayer(0)).fileSelect.setText("파일 전송 중...");
		if(upperNum==0) {
			toSendData = whole;
		}
		if(!toSendData.equals(whole)) {
			System.out.println("dumped!");
			return false;
		}
		if(toSendData.length > 1448) {//file이 1448바이트 초과일 경우
			if(iterateNum==0) {
				Thread progressBar = new Thread(progress);
				progressBar.start();
			}
	
			if(toSendData.length%1448 ==0) upperNum = toSendData.length/1448+1;//iterateNum = fragment 개수
			else upperNum = toSendData.length/1448+2;
			///fileName
			m_sHeader.fapp_totlen[0] = (byte) (whole.length & 0xFF);
			m_sHeader.fapp_totlen[1] = (byte) ((whole.length & 0xFF00) >> 8);//헤더에 총 길이 삽입
			m_sHeader.fapp_totlen[2] = (byte) ((whole.length & 0xFF0000) >> 16);
			m_sHeader.fapp_totlen[3] = (byte) ((whole.length & 0xFF000000) >> 24);//헤더에 총 길이 삽입
			m_sHeader.fapp_msg_type = (byte) 0x00;
			m_sHeader.fapp_type[0] = (byte) 0x00;
			m_sHeader.fapp_type[1] = (byte) 0x00;
			
			fileName = temp_file_name[temp_file_name.length-1].trim();
			System.out.println(fileName+"전송");
//			fragmentByte = new byte[input.length];
			fragmentByte = temp_file_name[temp_file_name.length-1].trim().getBytes();
			bytes = ObjToByte(m_sHeader,fragmentByte,fragmentByte.length);//헤더 붙이기
			
			((EthernetLayer)this.GetUnderLayer()).Send(bytes,bytes.length, this );//Ethernet으로 Send
//			((SimplestDlg)GetUpperLayer(0)).progressBar.setValue(((SimplestDlg)GetUpperLayer(0)).progressBar.getValue()+ 1000/upperNum);

			
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			iterateNum++;
			//data
			for (int i=1;i <upperNum-1 ;i++) {
//				System.out.println(i+"번째   "+upperNum);
				fragmentByte = new byte[1448];
				m_sHeader.fapp_msg_type=0x01;
				if(i==1) {
					m_sHeader.fapp_type[0] = (byte) 0x00;
					m_sHeader.fapp_type[1] = (byte) 0x00;
				}else {
					m_sHeader.fapp_type[0] = (byte) 0x01;
					m_sHeader.fapp_type[1] = (byte) 0x00;
				}
				m_sHeader.fapp_seq_num[0] = (byte)((i) & 0xFF); //1번째는 0x01, 2번째는 0x02 ...
				m_sHeader.fapp_seq_num[1] = (byte) ((i & 0xFF00) >> 8);
				m_sHeader.fapp_seq_num[2] = (byte) ((i & 0xFF0000) >> 16);
				m_sHeader.fapp_seq_num[3] = (byte) ((i & 0xFF000000) >> 24);
			
				for(int j=0;j<1448;j++) {//data를 1448바이트로 자르기
					fragmentByte[j]=whole[1448*(i-1)+j];
				}
				
				bytes = ObjToByte(m_sHeader,fragmentByte,fragmentByte.length);//헤더 붙이기
				((EthernetLayer)this.GetUnderLayer()).Send(bytes,bytes.length, this );//Ethernet으로 Send
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				iterateNum=i;
			}
			iterateNum++;
			//last fragment
			int lastLength=0;
			
			if(toSendData.length%1448 == 0) lastLength=1448;
			else lastLength = toSendData.length-(int)(toSendData.length/1448)*1448;
			
			fragmentByte = new byte[lastLength];
			System.out.println("lastLength : "+lastLength);
			m_sHeader.fapp_msg_type=0x01;
			m_sHeader.fapp_type[0] = (byte) 0x02;
			m_sHeader.fapp_type[1] = (byte) 0x00;
			m_sHeader.fapp_seq_num[0] = (byte)((upperNum-1) & 0xFF); //1번째는 0x01, 2번째는 0x02 ...
			m_sHeader.fapp_seq_num[1] = (byte) (((upperNum-1) & 0xFF00) >> 8);
			m_sHeader.fapp_seq_num[2] = (byte) (((upperNum-1) & 0xFF0000) >> 16);
			m_sHeader.fapp_seq_num[3] = (byte) (((upperNum-1) & 0xFF000000) >> 24);
			
			System.out.println("마지막"+iterateNum);
			for(int j=0;j<lastLength;j++) {//data를 10바이트로 자르기
				fragmentByte[j]=whole[(toSendData.length/1448)*1448+j];
			}
			bytes = ObjToByte(m_sHeader,fragmentByte,fragmentByte.length);//헤더 붙이기
			((EthernetLayer)this.GetUnderLayer()).Send(bytes,bytes.length, this);//Ethernet으로 Send
			iterateNum ++;
		}
		else {//1448바이트 이하일 경우,
			if(iterateNum==0) {
				Thread progressBar = new Thread(progress);
				progressBar.start();
			}
			upperNum = 2;
			m_sHeader.fapp_totlen[0] = (byte) (whole.length & 0xFF);
			m_sHeader.fapp_totlen[1] = (byte) ((whole.length & 0xFF00) >> 8);//헤더에 총 길이 삽입
			m_sHeader.fapp_totlen[2] = (byte) ((whole.length & 0xFF0000) >> 16);
			m_sHeader.fapp_totlen[3] = (byte) ((whole.length & 0xFF000000) >> 24);//헤더에 총 길이 삽입
			m_sHeader.fapp_msg_type =0x00;
			
			System.out.println(fileName);
			System.out.println("fileName.getBytes.len "+fileName.getBytes().length);
			bytes = ObjToByte(m_sHeader,fileName.getBytes(),fileName.getBytes().length);
			
//			((SimplestDlg)GetUpperLayer(0)).progressBar.setValue(500);
			((EthernetLayer)this.GetUnderLayer()).Send(bytes,bytes.length, this);
			iterateNum=1;
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("data");
			m_sHeader.fapp_msg_type =0x01;
			m_sHeader.fapp_type[0] = (byte) 0x02;
			m_sHeader.fapp_type[1] = (byte) 0x00;
			m_sHeader.fapp_seq_num[0] = (byte)(0x01); //1번째는 0x01, 2번째는 0x02 ...
			m_sHeader.fapp_seq_num[1] = (byte)(0x00); 
			m_sHeader.fapp_seq_num[2] = (byte)(0x00); 
			m_sHeader.fapp_seq_num[3] = (byte)(0x00); 
			
			bytes = ObjToByte(m_sHeader,whole,whole.length);
//			((SimplestDlg)GetUpperLayer(0)).progressBar.setValue(1000);
			((EthernetLayer)this.GetUnderLayer()).Send(bytes,bytes.length, this);
			iterateNum=2;
			
		}
		((SimplestDlg)GetUpperLayer(0)).fileSelect.setText("");
		iterateNum=0;
		upperNum =0;
		toSendData = null;
		return true;
	}

	public byte[] RemoveCappHeader(byte[] input, int length) {
		
		
		byte[] remvHeader = new byte[length-12];
		for(int i=0;i<length-12;i++) {
			remvHeader[i] = input[i+12];
		}
		return remvHeader;// 변경하세요 필요하시면
	}
	public synchronized boolean Receive(byte[] input) {
		HandlingFile forReceive = new HandlingFile("Receive",input);
		Thread receiveThread = new Thread(forReceive);
		receiveThread.start();
		return true;
	}

	public synchronized boolean ReceiveFile(byte[] input) {
		byte[] data;
		int wholeLength = 0, num, type;
		
		wholeLength = (int) (input[0] & 0xFF) + (int) ((input[1] & 0xFF)<<8)+(int) ((input[2] & 0xFF) << 16)+(int) ((input[3] & 0xFF) << 24);
		if(wholeLength%1448 ==0) upperNum = wholeLength/1448+1;
		else {
			if(wholeLength > 1448) upperNum = wholeLength/1448+2;
			else upperNum = 2;
		}
		data = RemoveCappHeader(input, input.length);
		if(!((SimplestDlg)GetUpperLayer(0)).fileSelect.getText().equals("")) ((SimplestDlg)GetUpperLayer(0)).start_progress.setEnabled(false);
		
		num = (int)(0xFF & input[8]) + (int)((0xFF & input[9]) << 8) +(int)((0xFF & input[10]) << 16)+(int)((0xFF & input[11]) << 24);
		type = (int)(0xFF & input[4]) + (int)((0xFF & input[5]) << 8);//type
		System.out.println("num : "+num+ " type : "+type);
		if(iterateNum==0) {
			Thread progressBar = new Thread(progress);
			progressBar.start();
		}
		int isName = (int)(0xFF & input[6]);
		if(wholeLength > 1448) {//must be fragment

			if(isName == 0) {
				System.out.println("file name");
				receive_fileName = new String(data).trim();
				try {
					String path = System.getProperty("user.home")+File.separator+"Desktop";
					
					output = new FileOutputStream(path+File.separator+receive_fileName);
				}catch (IOException e){//예외의 경우 오류를 출력한다.
					e.printStackTrace();
				}
				iterateNum++;
			
			}else if(isName ==1) {
				if(type == 0) {//first fragment
					System.out.println("first frag" + data.length);
					System.out.println(iterateNum+" / "+upperNum);
					index_buffer.add(num);
					buffer.add(data);
					iterateNum++;
				}else if(type ==1) {
					System.out.println(iterateNum+" / "+upperNum +" len : " + data.length);
					index_buffer.add(num);
					buffer.add(data);
					iterateNum++;
				}else if(type == 2) {
					System.out.println("last frag "+ data.length);
					System.out.println(iterateNum+" / "+upperNum);
					if(wholeLength%1448 ==0) index_buffer.add(upperNum);
					else  index_buffer.add(num);
					buffer.add(data);
					iterateNum++;	
				}
				
			}
			if (iterateNum == upperNum) {
				System.out.println("파일 작성");
				try {
					for(int i=0;i<buffer.size();i++) {
						output.write(buffer.get((int)index_buffer.indexOf(i+1)));
					}
					output.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				System.out.println("종료");
				iterateNum=0;
				upperNum =0;
				index_buffer = new ArrayList<>();
				buffer = new ArrayList<byte[]>();
			}
			else return false;
			
			
		} else { // not fragment
			isName = (int)(0xFF & input[6]);
			if (isName == 0) {
				receive_fileName = new String(data).trim();
				try {
					String path = System.getProperty("user.home") + File.separator + "Desktop";
					output = new FileOutputStream(path+File.separator+receive_fileName);
				} catch (IOException e) {// 예외의 경우 오류를 출력한다.
					e.printStackTrace();
				}
				iterateNum++;
				return true;
				
			} else if(isName ==1) {
				if (type == 2) {
					// 파일 작성
					System.out.println("마지막 파일");
					iterateNum++;
					try {
						output.write(data);
						output.close();
					} catch (Exception err) {
						err.printStackTrace();
					}
				} else
					return false;
			}
			iterateNum=0;
			upperNum =0;
			index_buffer = new ArrayList<>();
			buffer = new ArrayList<byte[]>();

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
	
	class forProgressBar implements Runnable {
		@Override
		public void run() {
			int i=0;
			// TODO Auto-generated method stub
			while(true) {
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(i != iterateNum) {
					i= iterateNum;
					if(upperNum !=0)((SimplestDlg)GetUpperLayer(0)).progressBar.setValue(((1000*iterateNum)/upperNum));
					else ((SimplestDlg)GetUpperLayer(0)).progressBar.setValue(0);

					if(i==upperNum&&i!=0) {
						((SimplestDlg)GetUpperLayer(0)).progressBar.setValue(1000);
						i=0;
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						i=0;
						((SimplestDlg)GetUpperLayer(0)).fileSelect.setText("");
						((SimplestDlg)GetUpperLayer(0)).progressBar.setValue(0);
						return;
					}
				}
			}
		}	
	}
	class HandlingFile implements Runnable{
		byte[] input=null;
		String type = null;
		int length=0;
		public HandlingFile(String type, byte[] input, int length) {
			this.input = input;
			this.length =length;
			this.type = type;
		}
		public HandlingFile(String type, byte[] input) {
			this.input = input;
			this.type = type;
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			if(this.type.equals("Receive")) {
				ReceiveFile(input);
			}else if(this.type.equals("Send")) {
				SendFile(input, length);
			}
		}
	}
}
