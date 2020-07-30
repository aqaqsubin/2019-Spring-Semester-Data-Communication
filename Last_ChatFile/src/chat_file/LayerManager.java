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
	private ArrayList<BaseLayer> mp_aLayers = new ArrayList<BaseLayer>() ; //생성되는 모든 layer을 담고 있다.
	

	public LayerManager(){
		m_nLayerCount = 0;
		mp_sListHead = null;
		mp_sListTail = null;
		m_nTop = -1;
	}
	
	public void AddLayer(BaseLayer pLayer){//mp_aLayers 리스트에 pLayer 삽입,
		mp_aLayers.add(m_nLayerCount++, pLayer);
	}
	
	
	public BaseLayer GetLayer(int nindex){ //mp_aLayers 리스트 nindex 인덱스에 있는 BaseLayer 반환
		return mp_aLayers.get(nindex);
	}
	
	public BaseLayer GetLayer(String pName){//mp_aLayers 리스트에서 pName으로 찾기
		for( int i=0; i < m_nLayerCount; i++){
			if(pName.compareTo(mp_aLayers.get(i).GetLayerName()) == 0)
				return mp_aLayers.get(i);
		}
		return null;
	}
	
	public void ConnectLayers(String pcList){
		MakeList(pcList);//pcList를 잘라 linkedList 만들기
		LinkLayer(mp_sListHead);		
	}

	private void MakeList(String pcList){//pcList를 잘라 linkedList 만들기
		StringTokenizer tokens = new StringTokenizer(pcList, " "); // " "를 기준으로 자르기
		
		for(; tokens.hasMoreElements();){ //token이 있는 동안 반복
			_NODE pNode = AllocNode(tokens.nextToken()); //tokens를 token으로 하는 노드 생성
			AddNode(pNode);//노드 연결 mp_sListHead를 시작으로 mp_sListTail이 끝
		}	
	}

	private _NODE AllocNode(String pcName){//pcName을 token으로 하는 노드 생성
		_NODE node = new _NODE(pcName);
				
		return node;				
	}
	
	private void AddNode(_NODE pNode){//노드 연결 mp_sListHead가 첫 시작을 가리키고, mp_sListTail이 끝을 가리킴
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
		return mp_Stack.get(m_nTop);//m_nTop은 가장 위를 가리킴, top에 있는 BaseLayer 반환
	}
	
	private void LinkLayer(_NODE pNode){
		BaseLayer pLayer = null;
		
		while(pNode != null){
			if( pLayer == null)//pLayer을  pNode의 토큰값을 token으로 하는 BaseLayer로 초기화
				pLayer = GetLayer (pNode.token);
			else{
				if(pNode.token.equals("("))//처음 토큰 값이 (인 경우, pLayer을 mp_Stack에 push함
					Push (pLayer);//BaseLayer을 스택에 push
				else if(pNode.token.equals(")")) // BaseLayer 스택에서 pop
					Pop();
				else{
					char cMode = pNode.token.charAt(0);//pNode의 토큰 값 첫글자를 cMode라 하자
					String pcName = pNode.token.substring(1, pNode.token.length());//첫 글자 뺀 나머지 문자열을 pcName에 저장
					
					pLayer = GetLayer (pcName); // pcName을 token으로 하는 BaseLayer을 pLayer로 하자
					
					switch(cMode){//첫 글자가 무엇인지에 따라
					case '*':
						Top().SetUpperUnderLayer( pLayer );//stack의 top BaseLayer의 upper을 pLayer로, pLayer의 under을 top BaseLayer로
						break;
					case '+':
						Top().SetUpperLayer( pLayer );//top의 upperLayer을 pLayer로 한다.
						break;
					case '-':
						Top().SetUnderLayer( pLayer );//top의 바로 under인 underLayer을 pLayer로 한다.
						break;
					}					
				}
			}
			
			pNode = pNode.next;
				
		}
	}
	
	
}
