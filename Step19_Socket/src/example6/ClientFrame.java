package example6;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.json.JSONArray;
import org.json.JSONObject;
/*
 *  - 대화방에 현재 누가 참여하고 있는지 목록 출력하기
 *  - 누가 새로 입장했는지 메세지 출력하기
 *  - 누가 퇴장 했는지 메세지 출력하기
 *  
 *  서버에 전달되는 메세지의 종류
 *  
 *  1. 일반 대화 메세지
 *     {"type":"msg", "name":"김구라", "content":"안녕하세요!"}
 *  2. 누군가 입장했다는 메세지
 *     {"type":"enter", "name":"해골"}
 *  3. 누군가 퇴장했다는 메세지
 *     {"type":"out", "name":"원숭이"}
 *  4. 참여자 목록 메세지
 *     {"type":"members", "list":["주뎅이","덩어리","개다리"]}
 * 
 */
public class ClientFrame extends JFrame 
		implements ActionListener, KeyListener{
	//필요한 필드 정의하기 
	JTextField tf;
	Socket socket;
	BufferedWriter bw;
	BufferedReader br;
	JTextArea ta;
	String chatName; //대화명을 저장할 필드 
	JList<String> jList;
	
	//생성자
	public ClientFrame(String title) {
		super(title);
		setLayout(new BorderLayout());
		
		//페널을 만들어서 프레임의 위쪽에 배치하기 
		JPanel topPanel=new JPanel();
		topPanel.setBackground(Color.PINK);
		add(topPanel, BorderLayout.SOUTH);
		
		//프레임에 UI 배치하기
		tf=new JTextField(20);
		JButton sendBtn=new JButton("Send");
		topPanel.add(tf);
		topPanel.add(sendBtn);
		//버튼 리스너 등록하기
		sendBtn.addActionListener(this);
		//체팅 메세지를 출력할 TextArea 객체를 생성해서 
		ta=new JTextArea();
		//스크롤 가능한 UI 에 포장후 
		JScrollPane scPane=new JScrollPane(ta,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		//스크롤 가능한 UI 를 프레임의 가운데에 배치한다. 
		add(scPane, BorderLayout.CENTER);
		//오직 출력 용도로 사용하기 위해 
		ta.setEditable(false);
		//JTextField 에 KeyListener 등록하기
		tf.addKeyListener(this);
		
		//참여자 목록을 출력할 준비
		jList=new JList<>();
		jList.setBackground(Color.PINK);
		
		JPanel rightPanel=new JPanel();
		rightPanel.add(jList);
		add(rightPanel, BorderLayout.EAST);
		
		Vector<String> enterList=new Vector<>();
		enterList.add("참여자 목록");
		enterList.add("김구라");
		enterList.add("해골");
		
		jList.setListData(enterList);
		
		//소켓 접속하기 
		connect();
	}
	
	public static void main(String[] args) {
		//프레임객체 생성하면서 프레임의 제목을 chatting 으로 지정하기 
		ClientFrame f=new ClientFrame("chatting");
		//프레임의 x좌표, y좌표, width, height 설정하기
		f.setBounds(700, 200, 500, 500);
		//프레임을 닫을때 프로세스도 종료 되도록 한다. 
		f.setDefaultCloseOperation(EXIT_ON_CLOSE);
		//프레임을 화면상에 보이게 하기 
		f.setVisible(true);
	}
	
	//Socket 서버에 접속을 하는 메소드
	public void connect() {
		//대화명을 입력 받아서 필드에 저장하기
		chatName=JOptionPane.showInputDialog(this, "대화명을 입력하세요.");
		if(chatName == null || chatName.equals("")) {
			chatName="바보";
		}
		try {
			//소켓객체 
			socket=new Socket("14.63.164.99", 5000);
			//서버에 문자열을 출력할 객체 
			bw=new BufferedWriter
					(new OutputStreamWriter(socket.getOutputStream()));
			//서버가 전송하는 문자열을 읽어들일 객체 
			br=new BufferedReader
					(new InputStreamReader(socket.getInputStream()));
			//새로운 스레드를 시작 시켜서 서버에서 문자열이 도착하는지 지속적으로 대기한다.
			new ClientThread().start();
			//내가 입장한다고 서버에 메세지를 보낸다.
			//JSONObject 에 정보를 담고 
			JSONObject jsonObj=new JSONObject();
			jsonObj.put("type", "enter");
			jsonObj.put("name", chatName);
			//JSONObject 에 담긴정보를 JSON 문자열로 만들어서 
			String msg=jsonObj.toString();
			//서버에 출력하기 
			bw.write(msg);
			bw.newLine();
			bw.flush();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	//전송 버튼을 눌렀을때 
	@Override
	public void actionPerformed(ActionEvent arg0) {
		sendMessage();
	}
	//서버에서 문자열이 전송되는지 지속적으로 대기하는 스레드 객체를 생성할 클래스 설계
	class ClientThread extends Thread{
		@Override
		public void run() {
			while(true) {
				try {
					//대기하다가 문자열이 도착하면 메소드가 리턴한다.
					String line=br.readLine();
					//JSON 문자열을 이용해서 JSONObject 객체를 생성한다.
					JSONObject jsonObj=new JSONObject(line);
					// 어떤 종류의 메세지 인지 읽어와 본다.
					String type=jsonObj.getString("type");
					if(type.equals("enter")) {
						String name=jsonObj.getString("name");
						ta.append("[ "+name+" ] 님이 입장했습니다.");
						ta.append("\r\n");
					}else if(type.equals("msg")) {
						String name=jsonObj.getString("name");
						String content=jsonObj.getString("content");
						ta.append(name+" : "+content);
						ta.append("\r\n");
					}else if(type.equals("members")) {
						// "list" 라는 키값으로 저장된 JSONArray 객체 얻어오기 
						JSONArray jsonArr=jsonObj.getJSONArray("list");
						//참여자 목록을 저장할 Vector
						Vector<String> list=new Vector<>();
						//반복문 돌면서 
						for(int i=0; i<jsonArr.length(); i++) {
							// JSONArray 에서 i 번째 참여자 명단을 얻어와서 
							String tmp=jsonArr.getString(i);
							// Vector 에 누적 시키기
							list.add(tmp);
						}
						//반복문 돌고 난후 참여자 목록 Vector 를 JList 에 연결하기
						jList.setListData(list);
					}else if(type.equals("out")) {
						String name=jsonObj.getString("name");
						ta.append("# "+name+" # 님이 퇴장 했습니다.");
						ta.append("\r\n");
					}
					
					if(line==null) { //서버와의 접속이 끈기면 
						break;// while 문 탈출
					}
					
					//출력할 문서의 높이
					int height=ta.getDocument().getLength();
					//높이 만큼 JTextArea 를 스크롤시켜서 가장 아래에 있는 문자열이 보이게
					ta.setCaretPosition(height);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	@Override
	public void keyPressed(KeyEvent e) {
		//만일 엔터키를 눌렀다면 
		if(e.getKeyCode() == KeyEvent.VK_ENTER) {
			sendMessage();
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	//메세지를 전송하는 메소드
	public void sendMessage() {
		//전송할 문자열
		String msg=tf.getText();
		try {
			JSONObject jsonObj=new JSONObject();
			jsonObj.put("type", "msg");
			jsonObj.put("name", chatName);
			jsonObj.put("content", msg);
			String json=jsonObj.toString();
			bw.write(json);
			bw.newLine();
			bw.flush();
		}catch(Exception e) {
			e.printStackTrace();
		}
		tf.setText("");
	}
}// class ClientFrame



/*
 * 
 *    JSON  vs  XML
 *    
 *    {"num":1, "name":"김구라", "addr":"노량진}
 *    
 *    vs
 *    
 *    <member>
 *    	<num>1</num>
 *    	<name>김구라</name>
 *    	<addr>노량진</addr>
 *    </member>
 *    
 *    ---------------------------------------
 *    
 *    {"num":1, "friends":["김구라","해골","원숭이"]}
 *    
 *    vs
 *    
 *    <info>
 *    	<num>1</num>
 *    	<friends>
 *    		<item>김구라</item>
 *    		<item>해골</item>
 *    		<item>원숭이</item>
 *    	</friends>
 *    </info>
 */













