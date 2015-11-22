import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.Iterator;

import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class MusicMachine implements MetaEventListener {
	JFrame mainFrame;
	JPanel mainPanel;
	JList listOfReceived;
	JTextField userCommunique;
	ArrayList<JCheckBox> checkboxesList;
	int nextNum;
	Vector<String> vectorList = new Vector<String>();
	String user;
	ObjectOutputStream out;
	ObjectInputStream in;
	HashMap<String, boolean[]> mapOfReceivedSongs = new HashMap<String, boolean[]>();

	Sequencer sequencer;
	Sequence sequence;
	Sequence mySequence = null;
	Track track;

	String[] instrumentsNames = { "Bass Drum", "Closed Hi-Hat", "Open Hi-Hat",
			"Acoustic Snare", "Crash", "Cymbal", "Hand Clap", "High Tom",
			"Hi Bongo", "Maracas", "Whistle", "Low Conga", "Cowbell",
			"Vibraslap", "Low-mid Tom", "High Agogo", "Open Hi Conga" };
	int[] instruments = { 35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58,
			47, 67, 63 };

	public static void main(String[] args) {
		new MusicMachine().apConfiguration("Piotr");
	}

	public void apConfiguration(String name) {
		user = name;
		try {
			Socket sock = new Socket("127.0.0.1", 4999);
			out = new ObjectOutputStream(sock.getOutputStream());
			in = new ObjectInputStream(sock.getInputStream());
			Thread thread = new Thread(new RemoteReader());
			thread.start();
		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println("No connection, you are in offline mode");
		}
		midiConfiguration();
		guiConfiguration();
	}

	public void guiConfiguration(){
		mainFrame = new JFrame("Music Machine");
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		BorderLayout layout = new BorderLayout();
		JPanel backgroundPanel = new JPanel(layout);
		backgroundPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		
		checkboxesList = new ArrayList<JCheckBox>();
		
		Box buttonArea = new Box(BoxLayout.Y_AXIS);
		JButton start = new JButton("Start");
		start.addActionListener(new StartListener());
		buttonArea.add(start);
		
		JButton stop = new JButton("Stop");
		stop.addActionListener(new StopListener());
		buttonArea.add(stop);
		
		JButton speedUp = new JButton("Faster");
		speedUp.addActionListener(new SpeedUpListener());
		buttonArea.add(speedUp);
		
		JButton speedDown = new JButton("Slower");
		speedDown.addActionListener(new SpeedDownListener());
		buttonArea.add(speedDown);
		
		JButton send = new JButton("Send");
		send.addActionListener(new SendListener());
		buttonArea.add(send);
		
		userCommunique = new JTextField();
		buttonArea.add(userCommunique);
		
		listOfReceived = new JList();
		listOfReceived.addListSelectionListener(new ChoiceFromListListener());
		listOfReceived.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane list = new JScrollPane(listOfReceived);
		buttonArea.add(list);
		listOfReceived.setListData(vectorList);
		
		Box namesArea = new Box(BoxLayout.Y_AXIS);
		for (int i =0; i<16;i++){
			namesArea.add(new Label(instrumentsNames[i]));
		}
		backgroundPanel.add(BorderLayout.EAST, buttonArea);
		backgroundPanel.add(BorderLayout.WEST, namesArea);
		
		mainFrame.getContentPane().add(backgroundPanel);
		GridLayout gridBoxes = new GridLayout(16,16);
		gridBoxes.setVgap(1);
		gridBoxes.setHgap(2);
		mainPanel = new JPanel(gridBoxes);
		backgroundPanel.add(BorderLayout.CENTER, mainPanel);
		
		for (int i =0; i <256; i++) {
			JCheckBox c = new JCheckBox();
			c.setSelected(false);
			checkboxesList.add(c);
			mainPanel.add(c);
		}
		mainFrame.setBounds(50,50,50,50);
		mainFrame.pack();
		mainFrame.setVisible(true);
	
		
	}

	public void midiConfiguration() {
		try {
			sequencer = MidiSystem.getSequencer();
			sequencer.open();
			sequencer.addMetaEventListener(this);
			sequence = new Sequence(Sequence.PPQ, 4);
			track = sequence.createTrack();
			sequencer.setTempoInBPM(120);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void createTrackPlay() {
		ArrayList<Integer> tracklist = null;
		sequence.deleteTrack(track);
		track = sequence.createTrack();
		for (int i = 0; i < 16; i++) {
			tracklist = new ArrayList<Integer>();
			for (int j = 0; j < 16; j++) {
				JCheckBox jc = (JCheckBox) checkboxesList.get(j + (16 * i));
				if (jc.isSelected()) {
					int key = instruments[i];
					tracklist.add(new Integer(key));
				} else {
					tracklist.add(null);
				}
			}
			createTrack(tracklist);
		}
		track.add(createEvent(192, 9, 1, 0, 15));
		try {
			sequencer.setSequence(sequence);
			sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
			sequencer.start();
			sequencer.setTempoInBPM(120);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public class StartListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent a) {
			createTrackPlay();
		}
	}

	public class StopListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			sequencer.stop();
		}
	}

	public class SpeedUpListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			float speedRatio = sequencer.getTempoFactor();
			sequencer.setTempoFactor((float) (speedRatio * 1.03));
		}
	}

	public class SpeedDownListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			float speedRatio = sequencer.getTempoFactor();
			sequencer.setTempoFactor((float) (speedRatio * .97));
		}
	}
	
	public class SendListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			boolean[] stateCheckboxes = new boolean[256];
			for (int i =0; i<256;i++){
				JCheckBox field = (JCheckBox) checkboxesList.get(i);
				if (field.isSelected()){
					stateCheckboxes[i] = true;
				}
			}
			String CommuniqueToSend = null;
			try {
				out.writeObject(user + nextNum++ + ": " + userCommunique.getText());
				out.writeObject(stateCheckboxes);
			} catch (Exception ex) {
				ex.printStackTrace();
				System.out.println("i couldn't send message");
			}
			userCommunique.setText("");
		}
	}
	
	public class ChoiceFromListListener implements ListSelectionListener {
			public void valueChanged(ListSelectionEvent le){
				if (!le.getValueIsAdjusting()){
					String choseOption = (String) listOfReceived.getSelectedValue();
					if (choseOption!= null){
						boolean[] stateSelected = (boolean[]) mapOfReceivedSongs.get(choseOption);
						changeSequence(stateSelected);
						sequencer.stop();
						createTrackPlay();
					}
				}
				
			}
			
		}
		
		public class RemoteReader implements Runnable{
		boolean[] stateCheckbox = null;
		String presentedName = null;
		Object obj = null;
			@Override
			public void run() {
				try {
					while ((obj=in.readObject()) != null) {
						System.out.println("Object from server has been downloaded");
						System.out.println(obj.getClass());
						String nameToPresent = (String) obj;
						stateCheckbox = (boolean[]) in.readObject();
						mapOfReceivedSongs.put(nameToPresent, stateCheckbox);
						vectorList.add(nameToPresent);
						listOfReceived.setListData(vectorList);
					}
				} catch (Exception ex){
					ex.printStackTrace();
				}
				
			} 
		
	}
		
		public class PlayListener implements ActionListener{
			@Override
			public void actionPerformed(ActionEvent e) {
				if (mySequence != null){
					sequence = mySequence;
				}		
			}
		}
		
		public void changeSequence(boolean[] stateCheckbox){
			for (int i = 0; i<256;i++){
				JCheckBox field = (JCheckBox) checkboxesList.get(i);
				if (stateCheckbox[i]){
					field.setSelected(true);
				} else {
					field.setSelected(false);
				}
			}
		}
		
		public void createTrack(ArrayList list){
			Iterator iter = list.iterator();
			for(int i =0; i<16;i++){
				Integer num = (Integer) iter.next();
				if(num != null){
					int numK = num.intValue();
					track.add(createEvent(144,9,numK,100,i));
					track.add(createEvent(128,9,numK,100,i+1));
				}
			}
		}
		public MidiEvent createEvent(int plc, int channel, int one, int two, int tact){
			MidiEvent event = null;
			try {
				ShortMessage a = new ShortMessage();
				a.setMessage(plc,channel,one,two);
				event = new MidiEvent(a,tact);
			} catch (Exception e){
				e.printStackTrace();
			}
			return event;
		}

	@Override
	public void meta(MetaMessage arg0) {
		// TODO Auto-generated method stub

	}

}
