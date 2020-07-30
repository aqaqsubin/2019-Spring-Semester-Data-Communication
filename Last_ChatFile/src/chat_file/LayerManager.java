package chat_file;

import java.util.ArrayList;
import java.util.StringTokenizer;

public class LayerManager {
	
	private class _NODE{
		private String token;
		private _NODE next;
		public _NODE(String input){
			this.token = input;
			this.next = null;
		}
	}

	_NODE mp_sListHead;
	_NODE mp_sListTail;
	
	private int m_nTop;
	private int m_nLayerCount;

	private ArrayList<BaseLayer> mp_Stack = new ArrayList<BaseLayer>();
	private ArrayList<BaseLayer> mp_aLayers = new ArrayList<BaseLayer>() ; //�����Ǵ� ��� layer�� ��� �ִ�.
	

	public LayerManager(){
		m_nLayerCount = 0;
		mp_sListHead = null;
		mp_sListTail = null;
		m_nTop = -1;
	}
	
	public void AddLayer(BaseLayer pLayer){//mp_aLayers ����Ʈ�� pLayer ����,
		mp_aLayers.add(m_nLayerCount++, pLayer);
	}
	
	
	public BaseLayer GetLayer(int nindex){ //mp_aLayers ����Ʈ nindex �ε����� �ִ� BaseLayer ��ȯ
		return mp_aLayers.get(nindex);
	}
	
	public BaseLayer GetLayer(String pName){//mp_aLayers ����Ʈ���� pName���� ã��
		for( int i=0; i < m_nLayerCount; i++){
			if(pName.compareTo(mp_aLayers.get(i).GetLayerName()) == 0)
				return mp_aLayers.get(i);
		}
		return null;
	}
	
	public void ConnectLayers(String pcList){
		MakeList(pcList);//pcList�� �߶� linkedList �����
		LinkLayer(mp_sListHead);		
	}

	private void MakeList(String pcList){//pcList�� �߶� linkedList �����
		StringTokenizer tokens = new StringTokenizer(pcList, " "); // " "�� �������� �ڸ���
		
		for(; tokens.hasMoreElements();){ //token�� �ִ� ���� �ݺ�
			_NODE pNode = AllocNode(tokens.nextToken()); //tokens�� token���� �ϴ� ��� ����
			AddNode(pNode);//��� ���� mp_sListHead�� �������� mp_sListTail�� ��
		}	
	}

	private _NODE AllocNode(String pcName){//pcName�� token���� �ϴ� ��� ����
		_NODE node = new _NODE(pcName);
				
		return node;				
	}
	
	private void AddNode(_NODE pNode){//��� ���� mp_sListHead�� ù ������ ����Ű��, mp_sListTail�� ���� ����Ŵ
		if(mp_sListHead == null){
			mp_sListHead = mp_sListTail = pNode;
		}else{
			mp_sListTail.next = pNode;
			mp_sListTail = pNode;
		}
	}

	private void Push (BaseLayer pLayer){
		mp_Stack.add(++m_nTop, pLayer);
		//mp_Stack.add(pLayer);
		//m_nTop++;
	}

	private BaseLayer Pop(){
		BaseLayer pLayer = mp_Stack.get(m_nTop);
		mp_Stack.remove(m_nTop);
		m_nTop--;
		
		return pLayer;
	}
	
	private BaseLayer Top(){
		return mp_Stack.get(m_nTop);//m_nTop�� ���� ���� ����Ŵ, top�� �ִ� BaseLayer ��ȯ
	}
	
	private void LinkLayer(_NODE pNode){
		BaseLayer pLayer = null;
		
		while(pNode != null){
			if( pLayer == null)//pLayer��  pNode�� ��ū���� token���� �ϴ� BaseLayer�� �ʱ�ȭ
				pLayer = GetLayer (pNode.token);
			else{
				if(pNode.token.equals("("))//ó�� ��ū ���� (�� ���, pLayer�� mp_Stack�� push��
					Push (pLayer);//BaseLayer�� ���ÿ� push
				else if(pNode.token.equals(")")) // BaseLayer ���ÿ��� pop
					Pop();
				else{
					char cMode = pNode.token.charAt(0);//pNode�� ��ū �� ù���ڸ� cMode�� ����
					String pcName = pNode.token.substring(1, pNode.token.length());//ù ���� �� ������ ���ڿ��� pcName�� ����
					
					pLayer = GetLayer (pcName); // pcName�� token���� �ϴ� BaseLayer�� pLayer�� ����
					
					switch(cMode){//ù ���ڰ� ���������� ����
					case '*':
						Top().SetUpperUnderLayer( pLayer );//stack�� top BaseLayer�� upper�� pLayer��, pLayer�� under�� top BaseLayer��
						break;
					case '+':
						Top().SetUpperLayer( pLayer );//top�� upperLayer�� pLayer�� �Ѵ�.
						break;
					case '-':
						Top().SetUnderLayer( pLayer );//top�� �ٷ� under�� underLayer�� pLayer�� �Ѵ�.
						break;
					}					
				}
			}
			
			pNode = pNode.next;
				
		}
	}
	
	
}
