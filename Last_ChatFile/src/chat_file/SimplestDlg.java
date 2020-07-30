package chat_file;


import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;


import java.awt.FileDialog;

public class SimplestDlg extends JFrame implements BaseLayer {

	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	BaseLayer UnderLayer;

	private static LayerManager m_LayerMgr = new LayerManager();

	private JTextField ChattingWrite; //한 줄의 문자열 입력
	JTextField fileSelect; //한 줄의 문자열 입력
	
	JProgressBar progressBar;
	
	Container contentPane;//chatting과 setting을 담는 컴포넌트

	JTextArea ChattingArea;//여러 줄의 문자열 입력을 위한 컴포넌트
	JComboBox<String> selAdapt;
	JTextArea srcAddress;
	JTextArea dstAddress;

	FileDialog openDialog;
	JLabel lblsrc;
	JLabel lbldst;
	JLabel lbladapt;
	
	String sourceDir;
	String sourceFile;

	JButton Select_file;
	JButton Setting_Button;
	JButton Chat_send_Button;
	JButton start_progress;
	
	Thread fileThread =null;
	Thread chatThread = null;

	static JComboBox<String> NICComboBox;

	int adapterNumber = 0;

	String Text;
	NILayer m_NILayer;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		m_LayerMgr.AddLayer(new NILayer("NILayer"));
		m_LayerMgr.AddLayer(new ChatAppLayer("ChatApp"));
		m_LayerMgr.AddLayer(new EthernetLayer("EthernetLayer"));
		m_LayerMgr.AddLayer(new SimplestDlg("SimplestDlg"));
		m_LayerMgr.AddLayer(new FileAppLayer("FileApp"));

		m_LayerMgr.ConnectLayers(" NILayer ( *EthernetLayer ( *ChatApp  ( +SimplestDlg ( ) ) *FileApp ( +SimplestDlg ( ) ) ) ) ");
	}

	public SimplestDlg(String pName) {
		pLayerName = pName;

		setTitle("IPC");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(250, 250, 644, 425);
		contentPane = new JPanel();
		((JComponent) contentPane).setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		JPanel chattingPanel = new JPanel();// chatting panel
		chattingPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "chatting",
				TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		chattingPanel.setBounds(10, 5, 360, 276);
		contentPane.add(chattingPanel);
		chattingPanel.setLayout(null);

		JPanel chattingEditorPanel = new JPanel();// chatting write panel
		chattingEditorPanel.setBounds(10, 15, 340, 210);
		chattingPanel.add(chattingEditorPanel);
		chattingEditorPanel.setLayout(null);

		ChattingArea = new JTextArea(); 
		ChattingArea.setEditable(false);
		ChattingArea.setBounds(0, 0, 340, 210);
		chattingEditorPanel.add(ChattingArea);// chatting edit

		JPanel chattingInputPanel = new JPanel();// chatting write panel
		chattingInputPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		chattingInputPanel.setBounds(10, 230, 250, 20);
		chattingPanel.add(chattingInputPanel);
		chattingInputPanel.setLayout(null);

		ChattingWrite = new JTextField(); //채팅 입력창 text
		ChattingWrite.setBounds(2, 2, 250, 20);// 249
		chattingInputPanel.add(ChattingWrite);
		ChattingWrite.setColumns(10);// writing area
		
		
		JPanel fileInputPanel = new JPanel();// file panel
		fileInputPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "file",
				TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		fileInputPanel.setBounds(10, 280, 360, 85);
		contentPane.add(fileInputPanel);
		fileInputPanel.setLayout(null);

		fileSelect = new JTextField(); //file 입력창 text
		fileSelect.setBounds(10, 20, 250, 20);// 249
		fileInputPanel.add(fileSelect);
		fileSelect.setColumns(10);// writing area

		progressBar = new JProgressBar();
		progressBar.setMaximum(1000);
		progressBar.setBounds(10,50,250,20);
		fileInputPanel.add(progressBar);
		
		JPanel settingPanel = new JPanel();
		settingPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "setting",
				TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		settingPanel.setBounds(380, 5, 236, 371);
		contentPane.add(settingPanel);
		settingPanel.setLayout(null);

		JPanel sourceAdaptPanel = new JPanel();
		sourceAdaptPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		sourceAdaptPanel.setBounds(10, 40, 170, 20);
		settingPanel.add(sourceAdaptPanel);
		sourceAdaptPanel.setLayout(null);
		
		
		JPanel sourceAddressPanel = new JPanel();
		sourceAddressPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		sourceAddressPanel.setBounds(10, 96, 170, 20);
		settingPanel.add(sourceAddressPanel);
		sourceAddressPanel.setLayout(null);

		lbladapt = new JLabel("MAC 선택");
		lbladapt.setBounds(10, 20, 170, 20);
		settingPanel.add(lbladapt);

		//////////////////////////////////////////////////////
		adapterNumber = ((NILayer)m_LayerMgr.GetLayer("NILayer")).m_pAdapterList.size();
	
		selAdapt = new JComboBox<String>(); 
		try {
			if(adapterNumber == 0) {
				System.out.println("어댑터 연결이 없음.");
			}
			for(int i=0;i<adapterNumber;i++) {
				selAdapt.addItem(((NILayer)m_LayerMgr.GetLayer("NILayer")).m_pAdapterList.get(i).getDescription());
			}
			
		}catch(Exception err) {
			System.out.println("error : "+err);
		}
		
		selAdapt.setBounds(2, 2, 170, 20);
		selAdapt.addActionListener(new aListener());
		sourceAdaptPanel.add(selAdapt);
		selAdapt.setEnabled(true);
		
		lblsrc = new JLabel("Source MAC Address");
		lblsrc.setBounds(10, 75, 170, 20);
		settingPanel.add(lblsrc);
		
		srcAddress = new JTextArea();
		srcAddress.setBounds(2, 2, 170, 20);
		sourceAddressPanel.add(srcAddress);
		srcAddress.setEnabled(true);

		JPanel destinationAddressPanel = new JPanel();
		destinationAddressPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		destinationAddressPanel.setBounds(10, 212, 170, 20);
		settingPanel.add(destinationAddressPanel);
		destinationAddressPanel.setLayout(null);

		lbldst = new JLabel("Destination MAC Address");
		lbldst.setBounds(10, 187, 190, 20);
		settingPanel.add(lbldst);

		dstAddress = new JTextArea();
		dstAddress.setBounds(2, 2, 170, 20);
		destinationAddressPanel.add(dstAddress);// dst address

		Setting_Button = new JButton("Setting");// setting
		Setting_Button.setBounds(80, 270, 100, 20);
		Setting_Button.addActionListener(new setAddressListener());
		settingPanel.add(Setting_Button);// setting
		
		openDialog = new FileDialog(this, "파일 열기", FileDialog.LOAD);
		Select_file = new JButton("파일...");
		Select_file.setBounds(270, 20, 80, 20);
		Select_file.addActionListener(new setFileListener());
		fileInputPanel.add(Select_file);// setting
		
		start_progress = new JButton("전송");
		start_progress.setBounds(270, 50, 80, 20);
		start_progress.addActionListener(new startProgressListener());
		fileInputPanel.add(start_progress);// setting
		start_progress.setEnabled(false);

		Chat_send_Button = new JButton("Send");
		Chat_send_Button.setBounds(270, 230, 80, 20);
		Chat_send_Button.addActionListener(new setAddressListener());
		chattingPanel.add(Chat_send_Button);// chatting send button

		setVisible(true);

	}
	class startProgressListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub

//			fileTransfer_Thread file = new fileTransfer_Thread();
//			Thread fileThread = new Thread(file);
//			fileThread.start();
			((FileAppLayer) m_LayerMgr.GetLayer("FileApp")).Send(fileSelect.getText().getBytes(), fileSelect.getText().getBytes().length);
			
		}
		
	}
	class setFileListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			openDialog.setVisible(true);
			sourceDir = openDialog.getDirectory();
			sourceFile = openDialog.getFile();
			fileSelect.setText(sourceDir+sourceFile);
//			((FileAppLayer)m_LayerMgr.GetLayer("FileApp")).fileName = sourceFile;
			fileSelect.setEnabled(false);
			if(!dstAddress.getText().equals("") && !srcAddress.getText().equals("")) {
				start_progress.setEnabled(true);
			}
		}
		
	}
	class aListener implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent e) {
			String byteAdd="";
			((NILayer) m_LayerMgr.GetLayer("NILayer")).SetAdapterNumber(selAdapt.getSelectedIndex());
			try {
				byte[] srcADDR = ((NILayer) m_LayerMgr.GetLayer("NILayer")).m_pAdapterList.get(selAdapt.getSelectedIndex()).getHardwareAddress();
				for(int j=0;j<srcADDR.length;j++) {
					if(j!=0) byteAdd= byteAdd+"-";
					byteAdd = byteAdd +Integer.toHexString((int)(srcADDR[j]&0xff));
				}
				srcAddress.setText(byteAdd.toUpperCase());
				
				srcAddress.setEnabled(true);
			}catch(Exception err){
				System.out.println("error : "+err);
			}
			
		}
	}


	class setAddressListener implements ActionListener {
		
		@Override
		public void actionPerformed(ActionEvent e) {

			if(e.getSource() == Setting_Button) {
				if(Setting_Button.getText() == "Reset") {
					
					dstAddress.setText("");
					srcAddress.setText("");
					Setting_Button.setText("Setting");
					
					((NILayer) m_LayerMgr.GetLayer("NILayer")).m_iNumAdapter = 0;
					((EthernetLayer) m_LayerMgr.GetLayer("EthernetLayer")).ResetHeader();
					
					ChattingArea.setText("");
					dstAddress.setEnabled(true);
					srcAddress.setEnabled(false);
					selAdapt.setEnabled(true);
					
				} else {

					if (dstAddress.getText().equals("") || srcAddress.getText().equals("")) {
						System.out.println("주소 미설정.");
						if (srcAddress.getText().equals(""))
							srcAddress.setText("주소 미설정");
						if (dstAddress.getText().equals(""))
							dstAddress.setText("주소 미설정");
						srcAddress.setEnabled(false);
						dstAddress.setEnabled(true);
						selAdapt.setEnabled(true);
					} 
					else {
						StringTokenizer st = new StringTokenizer(srcAddress.getText(), "-");
						if(st.countTokens() != 6) {
							System.out.println("주소 형식 오류.");
							return;
						}
						st = new StringTokenizer(dstAddress.getText(), "-");

						if (st.countTokens() != 6) {
							dstAddress.setText("주소 형식 오류");
							return;
						}

						byte[] dst = new byte[6];
						for (int i = 0; i < 6; i++) {
							String ss = st.nextToken();
							int s = Integer.parseInt(ss, 16);
							dst[i] = (byte) (s & 0xFF);
						}

						byte[] src = new byte[6];
						st = new StringTokenizer(srcAddress.getText(), "-");
						for (int i = 0; i < 6; i++) {
							String ss = st.nextToken();
							int s = Integer.parseInt(ss, 16);
							src[i] = (byte) (s & 0xFF);
						}

						
						((EthernetLayer) m_LayerMgr.GetLayer("EthernetLayer")).SetEnetDstAddress(dst);
						((EthernetLayer) m_LayerMgr.GetLayer("EthernetLayer")).SetEnetSrcAddress(src);
						((NILayer)m_LayerMgr.GetLayer("NILayer")).Receive();
						
						Setting_Button.setText("Reset");
						dstAddress.setEnabled(false);
						srcAddress.setEnabled(false);
						selAdapt.setEnabled(false);
					}

				}
			}
			if (e.getSource() == Chat_send_Button) {
				if (Setting_Button.getText() == "Setting") {

					if(dstAddress.getText() == null) dstAddress.setText("Destination Address를 설정하세요.");
					dstAddress.setEnabled(true);
					srcAddress.setEnabled(false);
					selAdapt.setEnabled(true);
				}
				else {
					String toSend = ChattingWrite.getText();
					ChattingArea.setText(ChattingArea.getText()+"[SEND] "+toSend+"\n");

					int stlen = toSend.getBytes().length;
					byte[] bt = new byte[stlen];
					bt = toSend.getBytes();
					
					((ChatAppLayer) m_LayerMgr.GetLayer("ChatApp")).Send(bt, bt.length);
					ChattingWrite.setText("");

				}
			}



		}
	}

	public boolean Receive(byte[] input) {
		//StringBuffer text = new StringBuffer();
		String str = new String(input);
		ChattingArea.setText(ChattingArea.getText()+"[RECV] "+str.toString()+"\n");
		return true;
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
	public void SetUpperUnderLayer(BaseLayer pUULayer) {
		this.SetUpperLayer(pUULayer);
		pUULayer.SetUnderLayer(this);

	}
//	class fileTransfer_Thread implements Runnable{
//
//		@Override
//		public void run() {
//			// TODO Auto-generated method stub
//			String temp_name = fileSelect.getText();
//
//			try {
//				byte[] b = new byte[1448];
//				byte[] whole = null;
//				byte[] temp_whole=null;
//				int wholeLen=0;
//				int i=0; 
//				int size=0;
//				InputStream inputStream = new FileInputStream(temp_name);
//				while((i = inputStream.read(b))!=-1) {
//					wholeLen = size+i;
//					temp_whole = new byte[wholeLen];
//					if(size !=0) {
//						System.arraycopy(whole, 0, temp_whole, 0, size);
//					}
//					System.arraycopy(b, 0, temp_whole,size,i);
//					size = wholeLen;
//					whole = temp_whole;
//					b= new byte[1448];
//				}
//				
//				((FileAppLayer) m_LayerMgr.GetLayer("FileApp")).Send(whole, whole.length);			
//				inputStream.close();
//			}catch(Exception err) {
//				err.printStackTrace();
//			}
//			
//		}
//		
//	}
//	class ChatTransfer_Thread implements Runnable{
//
//		@Override
//		public void run() {
//			// TODO Auto-generated method stub
//			if (Setting_Button.getText() == "Setting") {
//
//				if(dstAddress.getText() == null) dstAddress.setText("Destination Address를 설정하세요.");
//				dstAddress.setEnabled(true);
//				srcAddress.setEnabled(false);
//				selAdapt.setEnabled(true);
//			}
//			else {
//				String toSend = ChattingWrite.getText();
//				ChattingArea.setText(ChattingArea.getText()+"[SEND] "+toSend+"\n");
//
//				int stlen = toSend.getBytes().length;
//				byte[] bt = new byte[stlen];
//				bt = toSend.getBytes();
//				
//				((ChatAppLayer) m_LayerMgr.GetLayer("ChatApp")).Send(bt, bt.length);
//				ChattingWrite.setText("");
//
//			}
//		}
//		
//	}

}
