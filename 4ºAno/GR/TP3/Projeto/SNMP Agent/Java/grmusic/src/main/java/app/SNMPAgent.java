package main.java.app;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import org.snmp4j.CommandResponderEvent;
import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.agent.BaseAgent;
import org.snmp4j.agent.CommandProcessor;
import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.agent.MOGroup;
import org.snmp4j.agent.MOServer;
import org.snmp4j.agent.mo.DefaultMOFactory;
import org.snmp4j.agent.mo.DefaultMOTable;
import org.snmp4j.agent.mo.MOChangeEvent;
import org.snmp4j.agent.mo.MOChangeListener;
import org.snmp4j.agent.mo.MOScalar;
import org.snmp4j.agent.mo.MOTableRow;
import org.snmp4j.agent.mo.snmp.CoexistenceInfo;
import org.snmp4j.agent.mo.snmp.RowStatus;
import org.snmp4j.agent.mo.snmp.SnmpCommunityMIB;
import org.snmp4j.agent.mo.snmp.SnmpCommunityMIB.SnmpCommunityEntryRow;
import org.snmp4j.agent.mo.snmp.SnmpNotificationMIB;
import org.snmp4j.agent.mo.snmp.SnmpTargetMIB;
import org.snmp4j.agent.mo.snmp.StorageType;
import org.snmp4j.agent.mo.snmp.VacmMIB;
import org.snmp4j.agent.security.MutableVACM;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.security.USM;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.Counter32;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.Variable;
import org.snmp4j.transport.TransportMappings;

import javazoom.jl.decoder.JavaLayerException;

public class SNMPAgent extends BaseAgent {
	public static final String OID_PLAY = "1.3.6.1.3.42.2",
			OID_SONG = "1.3.6.1.3.42.7",
			OID_QUEUE = "1.3.6.1.3.42.9",
			OID_SCAN_FILES = "1.3.6.1.3.42.1.1.0",
			OID_SCAN_TIME = "1.3.6.1.3.42.1.2.0",
			OID_PLAY_GENRE = "1.3.6.1.3.42.2.1.1.1.0",
			OID_PLAY_ARTIST = "1.3.6.1.3.42.2.1.1.2.0",
			OID_PLAY_ALBUM = "1.3.6.1.3.42.2.1.1.3.0",
			OID_PLAY_SONG = "1.3.6.1.3.42.2.1.1.4.0",
			OID_PLAY_STATUS = "1.3.6.1.3.42.2.1.1.8.0",
			OID_PLAY_ORDER = "1.3.6.1.3.42.2.1.1.9.0",
			OID_PLAY_LIST = "1.3.6.1.3.42.2.1.1.10.0",
			OID_PLAY_INDEX = "1.3.6.1.3.42.2.1.1.11.0",
			OID_PLAY_QUEUE = "1.3.6.1.3.42.2.1.1.12.0",
			OID_SONG_FAVORITED = "1.3.6.1.3.42.7.1.1.10",
			OID_QUEUE_SONG = "1.3.6.1.3.42.9.1.1.5",
			OID_QUEUE_ADDER = "1.3.6.1.3.42.9.2.0";

	public static String PATH_FAVORITES;

	private PausablePlayer player;
	private String address, community;
	private int playSong = 0, playStatus = 1, playOrder = 1, playList = 1, playListIndex = 0;
	private long playTime = 0;
	private ArrayList<Genre> genres = new ArrayList<>();
	private ArrayList<Artist> artists = new ArrayList<>();
	private ArrayList<Album> albums = new ArrayList<>();
	private ArrayList<Song> songs = new ArrayList<>();
	private ArrayList<Integer> total = new ArrayList<>(),
			playlist = new ArrayList<>(),
			favorites = new ArrayList<>(),
			history = new ArrayList<>(),
			queue = new ArrayList<>();
	public SNMPAgent context = this;
	public DefaultMOTable playTable, totalTable, genreTable, artistTable,
			albumTable, songTable, historyTable, queueTable;
	public MOScalar timeScalar, filesScalar;
	public MOChangeListener listener = listenPlayTable();

	/**
	 *
	 * @param address
	 * @throws IOException
	 */
	public SNMPAgent(String address, String community, String favorites) throws IOException {

		/**
		 * Creates a base agent with boot-counter, config file, and a CommandProcessor
		 * for processing SNMP requests. Parameters: "bootCounterFile" - a file with
		 * serialized boot-counter information (read/write). If the file does not exist
		 * it is created on shutdown of the agent. "configFile" - a file with serialized
		 * configuration information (read/write). If the file does not exist it is
		 * created on shutdown of the agent. "commandProcessor" - the CommandProcessor
		 * instance that handles the SNMP requests.
		 */
		super(new File("conf.agent"), new File("bootCounter.agent"),
				new CommandProcessor(new OctetString(MPv3.createLocalEngineID())));
		this.address = address;
		this.community = community;
		TestSNMPAgent.writeLog("SNMP Agent initialized: -c " + community + " " + address);
		PATH_FAVORITES = favorites;
	}

	/**
	 * Adds community to security name mappings needed for SNMPv1 and SNMPv2c.
	 */
	@Override
	protected void addCommunities(SnmpCommunityMIB communityMIB) {
		Variable[] com2sec = new Variable[] { new OctetString(community), new OctetString("cpublic"), // security name
				getAgent().getContextEngineID(), // local engine ID
				new OctetString(community), // default context name
				new OctetString(), // transport tag
				new Integer32(StorageType.nonVolatile), // storage type
				new Integer32(RowStatus.active) // row status
		};
		MOTableRow row = communityMIB.getSnmpCommunityEntry()
				.createRow(new OctetString("public2public").toSubIndex(true), com2sec);
		communityMIB.getSnmpCommunityEntry().addRow((SnmpCommunityEntryRow) row);

	}

	/**
	 * Adds initial notification targets and filters.
	 */
	@Override
	protected void addNotificationTargets(SnmpTargetMIB arg0, SnmpNotificationMIB arg1) {
		// TODO Auto-generated method stub
		// System.out.println(">>> addNotificationTargets");

	}

	/**
	 * Adds all the necessary initial users to the USM.
	 */
	@Override
	protected void addUsmUser(USM arg0) {
		// TODO Auto-generated method stub
		// System.out.println(">>> addUsmUser");

	}

	/**
	 * Adds initial VACM configuration.
	 */
	@Override
	protected void addViews(VacmMIB vacm) {
		vacm.addGroup(SecurityModel.SECURITY_MODEL_SNMPv2c, new OctetString("cpublic"), new OctetString("v1v2group"),
				StorageType.nonVolatile);

		vacm.addAccess(new OctetString("v1v2group"), new OctetString(community), SecurityModel.SECURITY_MODEL_ANY,
				SecurityLevel.NOAUTH_NOPRIV, MutableVACM.VACM_MATCH_EXACT, new OctetString("fullReadView"),
				new OctetString("fullWriteView"), new OctetString("fullNotifyView"), StorageType.nonVolatile);

		vacm.addViewTreeFamily(new OctetString("fullReadView"), new OID("1.3"), new OctetString(),
				VacmMIB.vacmViewIncluded, StorageType.nonVolatile);

		vacm.addViewTreeFamily(new OctetString("fullWriteView"), new OID(OID_SCAN_FILES), new OctetString(),
				VacmMIB.vacmViewIncluded, StorageType.nonVolatile);

		vacm.addViewTreeFamily(new OctetString("fullWriteView"), new OID(OID_PLAY), new OctetString(),
				VacmMIB.vacmViewIncluded, StorageType.nonVolatile);

		vacm.addViewTreeFamily(new OctetString("fullWriteView"), new OID(OID_SONG), new OctetString(),
				VacmMIB.vacmViewIncluded, StorageType.nonVolatile);

		vacm.addViewTreeFamily(new OctetString("fullWriteView"), new OID(OID_QUEUE), new OctetString(),
				VacmMIB.vacmViewIncluded, StorageType.nonVolatile);

	}

	/**
	 * Unregister the basic MIB modules from the agent's MOServer.
	 */
	@Override
	protected void unregisterManagedObjects() {
		// TODO Auto-generated method stub
		// System.out.println(">>> unregisterManagedObjects");

	}

	/**
	 * Register additional managed objects at the agent's server.
	 */
	@Override
	protected void registerManagedObjects() {
		// TODO Auto-generated method stub
		// System.out.println(">>> registerManagedObjects");

	}

	protected void initTransportMappings() throws IOException {
		transportMappings = new TransportMapping[1];
		Address addr = GenericAddress.parse(address);
		TransportMapping tm = TransportMappings.getInstance().createTransportMapping(addr);
		transportMappings[0] = tm;
	}

	/**
	 * Start method invokes some initialization methods needed to start the agent
	 *
	 * @throws IOException
	 */
	public void start() throws IOException {
		init();
		// This method reads some old config from a file and causes
		// unexpected behavior.
		// loadConfig(ImportModes.REPLACE_CREATE);
		addShutdownHook();
		getServer().addContext(new OctetString(community));
		finishInit();
		run();
		sendColdStartNotification();

		// TransportMapping transportMapping;
		MessageDispatcherImpl dispatcher = new MessageDispatcherImpl();
		OctetString engineId = new OctetString(MPv3.createLocalEngineID());
		dispatcher.addMessageProcessingModel(new MPv2c());

		CommandProcessor processor = new CommandProcessor(engineId) {

			// override since we don't have coexistence info
			@Override
			protected OctetString getViewName(CommandResponderEvent req, CoexistenceInfo cinfo, int viewType) {
				return new OctetString();
			}

			public MOServer getServer(OctetString context) {
				return server;
			}

		};

		dispatcher.addCommandResponder(processor);

		Snmp session = new Snmp(dispatcher);
		session.listen();

		// System.out.println("here I am");
	}

	public void setMusic() {
		this.total = TestSNMPAgent.total;
		this.genres = TestSNMPAgent.genres;
		this.artists = TestSNMPAgent.artists;
		this.albums = TestSNMPAgent.albums;
		this.songs = TestSNMPAgent.songs;

		for (int i = 0; i < songs.size(); i ++) {
			if (songs.get(i).isFavorite()) {
				this.favorites.add(i + 1);
			}
		}

		playlist = this.total;

		updatePlayTable();
		updateTotalTable();
		updateGenreTable();
		updateArtistTable();
		updateAlbumTable();
		updateSongTable();
	}

	
	/**
	 * Clients can register the MO they need
	 */
	public void registerManagedScalar(MOScalar mo) {
		// System.out.println(mo.toString());
		try {
			if (mo.getOid().equals(new OID(OID_SCAN_TIME))) {
				timeScalar = mo;
				server.register(timeScalar, null);
			}

			else {
				mo.addMOChangeListener(listener);
				server.register(mo, null);
				
				if (mo.getOid().equals(new OID(OID_SCAN_FILES))) {
					filesScalar = mo;
				}
			}
		}
		
		catch (DuplicateRegistrationException ex) {
			System.out.println(">>> registerManagedScalar | DuplicateRegistrationException <<<");
			throw new RuntimeException(ex);
		}
	}

	public void registerManagedTables(DefaultMOTable playTable, DefaultMOTable totalTable,
			DefaultMOTable genreTable, DefaultMOTable artistTable, DefaultMOTable albumTable,
			DefaultMOTable songTable, DefaultMOTable historyTable, DefaultMOTable queueTable) {
		try {
			this.playTable = playTable;
			server.register(playTable, null);
			playTable.addMOChangeListener(listener);

			this.totalTable = totalTable;
			server.register(totalTable, null);

			this.genreTable = genreTable;
			server.register(genreTable, null);
			
			this.artistTable = artistTable;
			server.register(artistTable, null);

			this.albumTable = albumTable;
			server.register(albumTable, null);

			this.songTable = songTable;
			server.register(songTable, null);
			songTable.addMOChangeListener(listener);

			this.historyTable = historyTable;
			server.register(historyTable, null);

			this.queueTable = queueTable;
			server.register(queueTable, null);
			queueTable.addMOChangeListener(listener);
			
		}
		
		catch (DuplicateRegistrationException ex) {
			System.out.println(">>> registerManagedTables | DuplicateRegistrationException <<<");
			throw new RuntimeException(ex);
		}
	}

	public void updatePlayTable() {
		if (player != null) {
			playTime = player.getTime();
		}
		
		//playTable.removeRow(new OID("0"));
		playTable.removeAll();

		MOTableRow row;
		//System.out.println("ADDING ROW " + playSong);

		if (playSong > 0) {
			Song song = songs.get(playSong - 1);
			//System.out.println("ADDING STUFFY ROW");
			row = DefaultMOFactory.getInstance().createRow (
				new OID("0"), new Variable[] {
					song.getI32Genre(),
					song.getI32Artist(),
					song.getI32Album(),
					new Integer32(playSong),
					song.get8SName(),
					new TimeTicks(playTime / 10),
					song.getTTLength(),
					new Integer32(playStatus),
					new Integer32(playOrder),
					new Integer32(playList),
					playListIndex > 0 ? new Integer32(playListIndex) : null,
					new Counter32(queue.size())
				}
			);
		}

		else {
			//System.out.println("ADDING EMPTY ROW");
			playTime = 0;
			player = null;
			row = DefaultMOFactory.getInstance().createRow (
				new OID("0"), new Variable[] {
					null,
					null,
					null,
					null,
					null,
					new TimeTicks(playTime / 10),
					null,
					new Integer32(playStatus),
					new Integer32(playOrder),
					new Integer32(playList),
					playListIndex > 0 ? new Integer32(playListIndex) : null,
					new Counter32(queue.size())
				}
			);
		}

		playTable.addRow(row);
	}

	public void updateTotalTable() {
		totalTable.removeAll();

		MOTableRow row = DefaultMOFactory.getInstance().createRow (
			new OID("0"), new Variable[] {
				new Counter32(genres.size()),
				new Counter32(artists.size()),
				new Counter32(albums.size()),
				new Counter32(songs.size())
			}
		);

		totalTable.addRow(row);
	}

	public void updateGenreTable() {
		genreTable.removeAll();

		for (int i = 0; i < genres.size(); i ++) {
			MOTableRow row = DefaultMOFactory.getInstance().createRow (
				new OID("" + (i + 1)), new Variable[] {
					new Integer32(i + 1),
					genres.get(i).get8SName(),
					genres.get(i).getC32Artists(),
					genres.get(i).getC32Albums(),
					genres.get(i).getC32Songs()
				}
			);

			genreTable.addRow(row);
		}
	}

	public void updateArtistTable() {
		artistTable.removeAll();

		for (int i = 0; i < artists.size(); i ++) {
			MOTableRow row = DefaultMOFactory.getInstance().createRow (
				new OID("" + (i + 1)), new Variable[] {
					new Integer32(i + 1),
					artists.get(i).getI32Genre(),
					artists.get(i).get8SName(),
					artists.get(i).getC32Albums(),
					artists.get(i).getC32Songs()
				}
			);

			artistTable.addRow(row);
		}
	}

	public void updateAlbumTable() {
		albumTable.removeAll();

		for (int i = 0; i < albums.size(); i ++) {
			MOTableRow row = DefaultMOFactory.getInstance().createRow (
				new OID("" + (i + 1)), new Variable[] {
					new Integer32(i + 1),
					albums.get(i).getI32Genre(),
					albums.get(i).getI32Artist(),
					albums.get(i).get8SName(),
					albums.get(i).getC32Songs()
				}
			);

			albumTable.addRow(row);
		}
	}

	public void updateSongTable() {
		songTable.removeAll();

		for (int i = 0; i < songs.size(); i ++) {
			MOTableRow row = DefaultMOFactory.getInstance().createRow (
				new OID("" + (i + 1)), new Variable[] {
					new Integer32(i + 1),
					songs.get(i).getI32Genre(),
					songs.get(i).getI32Artist(),
					songs.get(i).getI32Album(),
					songs.get(i).get8SName(),
					songs.get(i).get8SFormat(),
					songs.get(i).getTTLength(),
					songs.get(i).getI32Size(),
					songs.get(i).get8SPath(),
					songs.get(i).getI32Favorited()
				}
			);

			songTable.addRow(row);
		}
	}

	public void updateHistoryTable() {
		historyTable.removeAll();

		for (int i = 1; i < history.size(); i ++) {
			int historyId = history.get(history.size() - i - 1);
			Song song = songs.get(historyId - 1);
			MOTableRow row = DefaultMOFactory.getInstance().createRow (
				new OID("" + i), new Variable[] {
					new Integer32(i),
					song.getI32Genre(),
					song.getI32Artist(),
					song.getI32Album(),
					new Integer32(historyId),
					song.get8SName(),
					song.getTTLength()
				}
			);

			historyTable.addRow(row);
		}
	}

	public void updateQueueTable() {
		queueTable.removeAll();

		for (int i = 0; i < queue.size(); i ++) {
			int queueId = queue.get(i);
			Song song = songs.get(queueId - 1);
			MOTableRow row = DefaultMOFactory.getInstance().createRow (
				new OID("" + (i + 1)), new Variable[] {
					new Integer32(i + 1),
					song.getI32Genre(),
					song.getI32Artist(),
					song.getI32Album(),
					new Integer32(queueId),
					song.get8SName(),
					song.getTTLength()
				}
			);

			queueTable.addRow(row);
		}
	}

	public void updateFavoritesFile() {
		TestSNMPAgent.writeLog("Updating favorite songs list file -> " + PATH_FAVORITES);
		try {
			FileWriter fWriter = new FileWriter(PATH_FAVORITES);
			fWriter.write("");

			for (Integer f: favorites) {
				fWriter.append(songs.get(f - 1).getRelativePath() + "\n");
			}

			fWriter.close();
		}
		
		catch (IOException e) {
			e.printStackTrace();
		}   
	}

	public void playThread(Song song) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					if (player != null) {
						player.stop();
					}

					BufferedInputStream buffer = new BufferedInputStream(new FileInputStream(song.getPath()));
					player = new PausablePlayer(buffer);
					player.play(context);
				}
				
				catch (JavaLayerException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();
	}

	public void playMusic(int status, String oidString, int setValue) {
		Song song;
		playStatus = status;

		switch (playStatus) {
			case 0:
				song = songs.get(playSong - 1);
				TestSNMPAgent.writeLog("Playing #" + playSong + " " + song.getName());
				//System.out.println("▶ Playing #" + playSong + " " + artists.get(song.getArtist() - 1).getName() + " - " + song.getName() + ".");
				
				if (history.size() > 0) {
					if (history.get(history.size() - 1) != playSong) {
						history.add(playSong);
						updateHistoryTable();
					}
				}

				else {
					history.add(playSong);
					updateHistoryTable();
				}

				playThread(song);
				playStatus = 3;
				updatePlayTable();

				break;

			case 1:	// stop
				TestSNMPAgent.writeLog("SNMP SET request [" + oidString + " = " + setValue + "] to stop playing");
				//System.out.println("⏹ Stopping.");
				history.clear();
				updateHistoryTable();
				queue.clear();
				updateQueueTable();
				playlist = total;
				playList = 1;
				playListIndex = 0;				
				player.stop();
				player = null;
				playSong = 0;
				updatePlayTable();

				break;

			case 2:	// pause
				song = songs.get(playSong - 1);
				TestSNMPAgent.writeLog("SNMP SET request [" + oidString + " = " + setValue + "] to pause #" + playSong + " " + song.getName());
				//TestSNMPAgent.writeLog("Playing #" + playSong + " " + song.getName());
				//System.out.println("⏸ Pausing #" + playSong + " " + artists.get(song.getArtist() - 1).getName() + " - " + song.getName() + ".");
				
				if (!player.isPaused()) {
					player.pause();
				}

				updatePlayTable();
				
				break;

			case 3:	// play/resume
				song = songs.get(playSong - 1);
				TestSNMPAgent.writeLog("SNMP SET request [" + oidString + " = " + setValue + "] to resume #" + playSong + " " + song.getName());
				//System.out.println("▶ Resuming #" + playSong + " " + artists.get(song.getArtist() - 1).getName() + " - " + song.getName() + ".");

				if (player.isPaused()) {					
					player.resume();
				}

				updatePlayTable();
				
				break;

			case 4:	// play previous
				if (history.size() > 1) {
					playSong = history.get(history.size() - 2);
					history.remove(history.size() - 1);
					history.remove(history.size() - 1);
					history.add(playSong);
					updateHistoryTable();

					if (!playlist.contains(playSong)) {
						playlist = total;
						playList = 1;
						playListIndex = 0;						
					}

					song = songs.get(playSong - 1);
					TestSNMPAgent.writeLog("SNMP SET request [" + oidString + " = " + setValue + "] to play previous #" + playSong + " " + song.getName());
					//System.out.println("⏮ Playing previous #" + playSong + " " + artists.get(song.getArtist() - 1).getName() + " - " + song.getName() + ".");
				}

				else {
					song = songs.get(playSong - 1);
					TestSNMPAgent.writeLog("SNMP SET request [" + oidString + " = " + setValue + "] to repeat #" + playSong + " " + song.getName());
				}

				playThread(song);
				updatePlayTable();
				playStatus = 3;
				
				break;

			case 5:	// replaying
				song = songs.get(playSong - 1);
				TestSNMPAgent.writeLog("SNMP SET request [" + oidString + " = " + setValue + "] to repeat #" + playSong + " " + song.getName());
				//System.out.println("🔄 Replaying #" + playSong + " " + artists.get(song.getArtist() - 1).getName() + " - " + song.getName() + ".");
				playThread(song);
				updatePlayTable();
				playStatus = 3;

				break;

			case 6:	// skipping
				int previousId = playSong;

				switch (playOrder) {
					case 1:	// not repeating
						if (queue.size() > 0) {
							playSong = queue.get(0);
							queue.remove(0);
							updateQueueTable();
						}

						else {
							// not end of playlist
							if (playSong != playlist.get(playlist.size() - 1)) {
								playSong = playlist.get(playlist.indexOf(playSong) + 1);
							}
				
							else {
								playlist = total;
								playList = 1;
								playListIndex = 0;
								
								if (playSong >= songs.size()) {
									playSong = 0;
								}
							}
						}

						break;

					case 2:	// repeating
						if (queue.size() > 0) {
							playSong = queue.get(0);
							queue.remove(0);
							updateQueueTable();
						}

						else {
							// not end of playlist
							if (playSong != playlist.get(playlist.size() - 1)) {
								playSong = playlist.get(playlist.indexOf(playSong) + 1);
							}
				
							else {
								playSong = playlist.get(0);
							}
						}

						break;

					case 3:	// repeating track
						/*if (playSong == 0) {
							playlist = total;
							playList = 1;
							playSong = playlist.get(0);
						}*/
						if (queue.size() > 0) {
							playSong = queue.get(0);
							queue.remove(0);
							updateQueueTable();
						}

						else {
							// not end of playlist
							if (playSong != playlist.get(playlist.size() - 1)) {
								playSong = playlist.get(playlist.indexOf(playSong) + 1);
							}
				
							else {
								playlist = total;
								playList = 1;
								playListIndex = 0;
								
								if (playSong >= songs.size()) {
									playSong = 0;
								}
							}
						}

						break;

					case 4:	// shuffling
						if (queue.size() > 0) {
							playSong = queue.get(0);
							queue.remove(0);
							updateQueueTable();
						}

						else {
							Random r = new Random();
							playSong = playlist.get(r.nextInt(playlist.size()));
						}

						break;
				}

				if (playSong > 0) {
					if (history.size() > 0) {
						if (history.get(history.size() - 1) != playSong) {
							history.add(playSong);
							updateHistoryTable();
						}
					}
	
					else {
						history.add(playSong);
						updateHistoryTable();
					}

					song = songs.get(playSong - 1);
					
					if (previousId > 0) {
						TestSNMPAgent.writeLog("SNMP SET request [" + oidString + " = " + setValue + "] to skip to #" + playSong + " " + song.getName());
						//System.out.println("⏭ Skipping to #" + playSong + " " + artists.get(song.getArtist() - 1).getName() + " - " + song.getName() + ".");
					}

					else {
						TestSNMPAgent.writeLog("SNMP SET request [" + oidString + " = " + setValue + "] to play #" + playSong + " " + song.getName());
						//System.out.println("▶ Playing #" + playSong + " " + artists.get(song.getArtist() - 1).getName() + " - " + song.getName() + ".");
					}

					playThread(song);
					updatePlayTable();
					playStatus = 3;
				}

				else {
					TestSNMPAgent.writeLog("SNMP SET request [" + oidString + " = " + setValue + "] to stop playing");
					//System.out.println("⏹ Stopping.");
					history.clear();
					updateHistoryTable();
					queue.clear();
					updateQueueTable();
					playlist = total;
					playList = 1;
					playListIndex = 0;
					player.stop();
					player = null;
					updatePlayTable();
					playStatus = 1;
				}

				break;

			case 7:	// auto-skipping
				switch (playOrder) {	
					case 1:	// not repeating
						if (queue.size() > 0) {
							playSong = queue.get(0);
							queue.remove(0);
							updateQueueTable();
						}

						else {
							//System.out.println("yeet s " + playSong + " l " + playlist.get(0));
							if (playSong == 0) {
								playSong = playlist.get(0);
							}
							// not end of playlist
							else if (playSong != playlist.get(playlist.size() - 1)) {
								playSong = playlist.get(playlist.indexOf(playSong) + 1);
							}
				
							else {
								playlist = total;
								playList = 1;
								playListIndex = 0;
								
								if (playSong >= songs.size()) {
									playSong = 0;
								}
							}
						}

						break;

					case 2:	// repeating
						if (queue.size() > 0) {
							playSong = queue.get(0);
							queue.remove(0);
							updateQueueTable();
						}

						else {
							if (playSong == 0) {
								playSong = playlist.get(0);
							}
							// not end of playlist
							if (playSong != playlist.get(playlist.size() - 1)) {
								playSong = playlist.get(playlist.indexOf(playSong) + 1);
							}
				
							else {
								playSong = playlist.get(0);
							}
						}

						break;

					case 3:	// repeating track
						if (playSong == 0) {
							playSong = playlist.get(0);
						}

						break;

					case 4:	// shuffling
						if (queue.size() > 0) {
							playSong = queue.get(0);
							queue.remove(0);
							updateQueueTable();
						}

						else {
							Random r = new Random();
							playSong = playlist.get(r.nextInt(playlist.size()));
						}

						break;
				}

				if (playSong > 0) {
					if (history.size() > 0) {
						if (history.get(history.size() - 1) != playSong) {
							history.add(playSong);
							updateHistoryTable();
						}
					}
	
					else {
						history.add(playSong);
						updateHistoryTable();
					}

					song = songs.get(playSong - 1);
					
					
					TestSNMPAgent.writeLog("Playing #" + playSong + " " + song.getName());
					//System.out.println("▶ Playing #" + playSong + " " + artists.get(song.getArtist() - 1).getName() + " - " + song.getName() + ".");
					playThread(song);
					playStatus = 3;
					updatePlayTable();
				}

				else {
					TestSNMPAgent.writeLog("Stopping");
					//System.out.println("⏹ Stopping.");
					history.clear();
					updateHistoryTable();
					queue.clear();
					updateQueueTable();
					playlist = total;
					playList = 1;
					playListIndex = 0;
					player.stop();
					player = null;
					playStatus = 0;
					updatePlayTable();
				}

				break;
		}
	}

	public MOChangeListener listenPlayTable() {
		MOChangeListener l = new MOChangeListener() {

			@Override
			public void beforePrepareMOChange(MOChangeEvent arg0) {	
				OID setOid = arg0.getOID();
				int setValue = arg0.getNewValue().toInt();
				String oidString = setOid.toString();
				//System.out.println(">>> beforePrepareMOChange <<< " + oidString);
				String[] oidSplit = oidString.replace(".", "-").split("-");
				int playIndex = Integer.parseInt(oidSplit[oidSplit.length - 1]);
				//System.out.println(">>> POSITION = " + playIndex);
				
				if (setOid.equals(new OID(OID_SCAN_FILES))) {
					if (setValue == 2) {
						TestSNMPAgent.writeLog("SNMP SET request [" + oidString + " = " + setValue + "] to re-scan song files");
						
						new Thread(new Runnable(){
							@Override
							public void run() {
								ArrayList<String> songLines = TestSNMPAgent.scanFiles();
								TestSNMPAgent.generateMusic(songLines, TestSNMPAgent.favorites);
								setMusic();
								filesScalar.setValue(new Integer32(1));
								DateTimeFormatter timeDTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
								LocalDateTime now = LocalDateTime.now();
								String time = timeDTF.format(now);
								timeScalar.setValue(new OctetString(time));

								try {
									TestSNMPAgent.writeConf(time);
								}
								
								catch (IOException e) {
									e.printStackTrace();
								}
							}
						}).start();
					}

					else {
						System.out.println("The only valid value is 2...");
						arg0.setDenyReason(SnmpConstants.SNMP_ERROR_BAD_VALUE);
					}
				}
				
				// Setting genre
				else if (setOid.equals(new OID(OID_PLAY_GENRE))) {
					if (setValue >= 1 && setValue <= genres.size()) {
						playSong = 0;
						playList = 2;
						playListIndex = setValue;
						Genre genre = genres.get(setValue - 1);
						playlist = genre.getPlaylist();
						TestSNMPAgent.writeLog("SNMP SET request [" + oidString + " = " + setValue + "] to play Genre playlist #" + setValue + " " + genre.getName());
						playMusic(7, oidString, setValue);
					}

					else {
						System.out.println("No genre with that index...");
						arg0.setDenyReason(SnmpConstants.SNMP_ERROR_BAD_VALUE);
					}
				}
				
				// Setting artist
				else if (setOid.equals(new OID(OID_PLAY_ARTIST))) {
					if (setValue >= 1 && setValue <= artists.size()) {
						playSong = 0;
						playList = 3;
						playListIndex = setValue;
						Artist artist = artists.get(setValue - 1);
						playlist = artist.getPlaylist();
						TestSNMPAgent.writeLog("SNMP SET request [" + oidString + " = " + setValue + "] to play Artist playlist #" + setValue + " " + artist.getName());
						playMusic(7, oidString, setValue);
					}

					else {
						System.out.println("No artist with that index...");
						arg0.setDenyReason(SnmpConstants.SNMP_ERROR_BAD_VALUE);
					}
				}
				
				// Setting album
				else if (setOid.equals(new OID(OID_PLAY_ALBUM))) {
					if (setValue >= 1 && setValue <= albums.size()) {
						playSong = 0;
						playList = 4;
						playListIndex = setValue;
						Album album = albums.get(setValue - 1);
						playlist = album.getPlaylist();
						TestSNMPAgent.writeLog("SNMP SET request [" + oidString + " = " + setValue + "] to play Album playlist #" + setValue + " " + album.getName());
						playMusic(7, oidString, setValue);
					}

					else {
						System.out.println("No album with that index...");
						arg0.setDenyReason(SnmpConstants.SNMP_ERROR_BAD_VALUE);
					}
				}
				
				// Setting song
				else if (setOid.equals(new OID(OID_PLAY_SONG))) {
					if (setValue >= 0 && setValue <= songs.size()) {
						playSong = setValue;

						if (playSong > 0) {
							Song song = songs.get(playSong - 1);
							TestSNMPAgent.writeLog("SNMP SET request [" + oidString + " = " + setValue + "] to play song #" + playSong + " " + song.getName());
							if (!playlist.contains(playSong)) {
								playlist = total;
								playList = 1;
								playListIndex = 0;
								System.out.println("Leaving playlist.");
							}

							playMusic(0, oidString, setValue);
						}

						else {
							TestSNMPAgent.writeLog("SNMP SET request [" + oidString + " = " + setValue + "] to play unspecified song from playlist");
							playMusic(7, oidString, setValue);
						}
					}

					else {
						System.out.println("No song with that index...");
						arg0.setDenyReason(SnmpConstants.SNMP_ERROR_BAD_VALUE);
					}
				}

				// Setting status
				else if (setOid.equals(new OID(OID_PLAY_STATUS))) {
					if (player != null) {
						if (setValue >= 1 && setValue <= 6) {
							playStatus = setValue;
							playMusic(playStatus, oidString, setValue);
						}

						else {
							System.out.println("Status value must be between 1 and 6...");
							arg0.setDenyReason(SnmpConstants.SNMP_ERROR_BAD_VALUE);
						}
					}

					else {
						System.out.println("You must select a music/playlist first...");
						arg0.setDenyReason(SnmpConstants.SNMP_ERROR_BAD_VALUE);
					}
				}

				// Setting order
				else if (setOid.equals(new OID(OID_PLAY_ORDER))) {
					switch (setValue) {
						case 1:
							playOrder = 1;
							updatePlayTable();
							TestSNMPAgent.writeLog("SNMP SET request [" + oidString + " = " + setValue + "] to not repeat playlist");
							//System.out.println("🔁️ Not repeating.");

							break;

						case 2:
							playOrder = 2;
							updatePlayTable();
							TestSNMPAgent.writeLog("SNMP SET request [" + oidString + " = " + setValue + "] to repeat playlist");
							//System.out.println("🔃 Repeating.");

							break;

						case 3:
							playOrder = 3;
							updatePlayTable();
							TestSNMPAgent.writeLog("SNMP SET request [" + oidString + " = " + setValue + "] to repeat track");
							//System.out.println("🔂 Repeating track.");

							break;

						case 4:
							playOrder = 4;
							updatePlayTable();
							TestSNMPAgent.writeLog("SNMP SET request [" + oidString + " = " + setValue + "] to shuffle playlist");
							//System.out.println("🔀 Shuffling.");

							break;

						default:
							System.out.println("Order value must be between 1 and 4...");
							arg0.setDenyReason(SnmpConstants.SNMP_ERROR_BAD_VALUE);
							
							break;
					}
				}

				// Setting playlist
				else if (setOid.equals(new OID(OID_PLAY_LIST))) {
					if (player != null && playSong != 0 || setValue == 5) {
						Song song;

						switch (setValue) {
							case 1:
								playList = 1;
								playlist = total;
								playListIndex = 0;
								updatePlayTable();
								TestSNMPAgent.writeLog("SNMP SET request [" + oidString + " = " + setValue + "] to leave playlist");
								//System.out.println("Leaving playlist.");

								break;

							case 2:
								song = songs.get(playSong - 1);

								if (song.getGenre() > 0) {
									playList = 2;
									Genre genre = genres.get(song.getGenre() - 1);
									playlist = genre.getPlaylist();
									playListIndex = song.getGenre();
									updatePlayTable();
									TestSNMPAgent.writeLog("SNMP SET request [" + oidString + " = " + setValue + "] to play Genre playlist #" + song.getGenre() + " " + genre.getName());
									//System.out.println("Playing " + genre.getName() + " playlist.");
								}

								else {
									System.out.println("The current song doesn't have a genre...");
									arg0.setDenyReason(SnmpConstants.SNMP_ERROR_BAD_VALUE);
								}

								break;

							case 3:
								song = songs.get(playSong - 1);

								if (song.getArtist() > 0) {
									playList = 3;
									Artist artist = artists.get(song.getArtist() - 1);
									playlist = artist.getPlaylist();
									playListIndex = song.getArtist();
									updatePlayTable();
									TestSNMPAgent.writeLog("SNMP SET request [" + oidString + " = " + setValue + "] to play Artist playlist #" + song.getArtist() + " " + artist.getName());
									//System.out.println("Playing " + artist.getName() + "'s playlist.");
								}

								else {
									System.out.println("The current song doesn't have an artist...");
									arg0.setDenyReason(SnmpConstants.SNMP_ERROR_BAD_VALUE);
								}

								break;

							case 4:
								song = songs.get(playSong - 1);

								if (song.getAlbum() > 0) {
									playList = 4;
									Album album = albums.get(song.getAlbum() - 1);
									playlist = album.getPlaylist();
									playListIndex = song.getAlbum();
									updatePlayTable();
									TestSNMPAgent.writeLog("SNMP SET request [" + oidString + " = " + setValue + "] to play Album playlist #" + song.getAlbum() + " " + album.getName());
									//System.out.println("Playing " + album.getName() + " playlist.");
								}

								else {
									System.out.println("The current song doesn't have an album...");
									arg0.setDenyReason(SnmpConstants.SNMP_ERROR_BAD_VALUE);
								}

								break;

							case 5:
								if (favorites.size() > 0) {
									if (player != null) {
										player.stop();
									}

									playSong = 0;
									playList = 5;
									playlist = favorites;
									playListIndex = 0;
									updatePlayTable();
									TestSNMPAgent.writeLog("SNMP SET request [" + oidString + " = " + setValue + "] to play favorites playlist");
									playMusic(7, oidString, setValue);
									//System.out.println("Playing favorites' playlist.");
								}

								else {
									System.out.println("There are no favorite songs...");
									arg0.setDenyReason(SnmpConstants.SNMP_ERROR_BAD_VALUE);
								}

								break;

							default:
								System.out.println("Playlist value must be between 1 and 4...");
								arg0.setDenyReason(SnmpConstants.SNMP_ERROR_BAD_VALUE);
								
								break;
						}
					}

					else {
						System.out.println("You must select a song/playlist first...");
						arg0.setDenyReason(SnmpConstants.SNMP_ERROR_BAD_VALUE);
					}
				}
				
				// Setting number of musics in queue
				else if (setOid.equals(new OID(OID_PLAY_QUEUE))) {
					if (setValue >= 0 && setValue <= queue.size()) {
						TestSNMPAgent.writeLog("SNMP SET request [" + oidString + " = " + setValue + "] to set number of songs in queue to " + setValue);
						//System.out.println("Setting number of songs in queue to " + setValue);
						
						while (setValue < queue.size()) {
							queue.remove(setValue);
						}
						
						updateQueueTable();
					}

					else {
						System.out.println("Number of songs must be between 0 and the current number of queued songs.");
						arg0.setDenyReason(SnmpConstants.SNMP_ERROR_BAD_VALUE);
					}
				}
				
				// Setting favorite song
				else if (oidString.contains(OID_SONG_FAVORITED) && playIndex > 0 && playIndex <= songs.size()) {
					//System.out.println("FAVORITING " + setValue);
					if (setValue == 1 || setValue == 2) {
						//System.out.println("...FAVORITING " + setValue);
						if (setValue == 1) {
							Song song = songs.get(playIndex - 1);
							TestSNMPAgent.writeLog("SNMP SET request [" + oidString + " = " + setValue + "] to remove #" + playIndex + " " + song.getName() + " from favorites");

							if (songs.get(playIndex - 1).isFavorite()) {
								//System.out.println("♡ Removing " + songs.get(playIndex - 1).getName() + " from favorites.");
								songs.get(playIndex - 1).setFavorite(false);
								favorites.remove(favorites.indexOf(playIndex));
								favorites.trimToSize();
								updateFavoritesFile();
							}

							/*else {
								System.out.println("♡ " + songs.get(playIndex - 1).getName() + " isn't in your favorites...");
							}*/
						}
						
						else {
							Song song = songs.get(playIndex - 1);
							TestSNMPAgent.writeLog("SNMP SET request [" + oidString + " = " + setValue + "] to add #" + playIndex + " " + song.getName() + " to favorites");

							if (!songs.get(playIndex - 1).isFavorite()) {
								//System.out.println("♥ Adding " + songs.get(playIndex - 1).getName() + " to favorites.");
								songs.get(playIndex - 1).setFavorite(true);
								favorites.add(playIndex);
								Collections.sort(favorites);
								updateFavoritesFile();
							}

							/*else {
								System.out.println("♥ " + songs.get(playIndex - 1).getName() + " is already in your favorites...");
							}*/
						}
					}

					else {
						System.out.println("Favoriting value must be either 1 or 2.");
						arg0.setDenyReason(SnmpConstants.SNMP_ERROR_BAD_VALUE);
					}
				}
				
				// Shifting/removing from queue
				else if (oidString.contains(OID_QUEUE_SONG) && playIndex > 0 && playIndex <= queue.size()) {
					if (setValue > 0 && setValue <= songs.size()) {
						Song song = songs.get(setValue - 1);
						TestSNMPAgent.writeLog("SNMP SET request [" + oidString + " = " + setValue + "] to add #" + setValue + " " + song.getName() + " to queue (" + playIndex + ")");

						//System.out.println("Adding to queue (" + playIndex + ") #" + setValue + " " + artists.get(song.getArtist() - 1).getName() + " - " + song.getName() + ".");

						queue.add(queue.get(queue.size() - 1));

						for (int i = queue.size() - 2; i >= playIndex; i --) {
							queue.set(i, queue.get(i - 1));
						}

						queue.set(playIndex - 1, setValue);
						updateQueueTable();
						updatePlayTable();
					}

					else if (setValue == 0) {
						setValue = queue.get(playIndex - 1);
						Song song = songs.get(setValue - 1);

						TestSNMPAgent.writeLog("SNMP SET request [" + oidString + " = 0] to remove #" + setValue + " " + song.getName() + " from queue (" + playIndex + ")");
						//System.out.println("Removing from queue (" + playIndex + ") #" + setValue + " " + artists.get(song.getArtist() - 1).getName() + " - " + song.getName() + ".");
						queue.remove(playIndex - 1);
						updateQueueTable();
						updatePlayTable();
					}

					else {
						System.out.println("No song with that index...");
						arg0.setDenyReason(SnmpConstants.SNMP_ERROR_BAD_VALUE);
					}
				}

				// Adding to queue
				else if (setOid.equals(new OID(OID_QUEUE_ADDER))) {
					if (player != null && playSong != 0) {
						if (setValue > 0 && setValue <= songs.size()) {
							Song song = songs.get(setValue - 1);
							TestSNMPAgent.writeLog("SNMP SET request [" + oidString + " = " + setValue + "] to add #" + setValue + " " + song.getName() + " to queue (" + (queue.size() + 1) + ")");
							//System.out.println("Adding to queue (" + (queue.size() + 1) + ") #" + setValue + " " + artists.get(song.getArtist() - 1).getName() + " - " + song.getName() + ".");

							queue.add(setValue);
							updateQueueTable();
							updatePlayTable();
						}

						else {
							System.out.println("No song with that index...");
							arg0.setDenyReason(SnmpConstants.SNMP_ERROR_BAD_VALUE);
						}
					}

					else {
						System.out.println("You must select a song/playlist first...");
						arg0.setDenyReason(SnmpConstants.SNMP_ERROR_BAD_VALUE);
					}
				}

				else {
					System.out.println("Invalid OID...");
					arg0.setDenyReason(SnmpConstants.SNMP_ERROR_NO_ACCESS);
				}
			}

			@Override
			public void beforeMOChange(MOChangeEvent arg0) {
				//System.out.println(">>> beforeMOChange <<<");
				// TODO Auto-generated method stub

			}

			@Override
			public void afterPrepareMOChange(MOChangeEvent arg0) {
				//System.out.println(">>> afterPrepareMOChange <<<");
				// TODO Auto-generated method stub

			}

			@Override
			public void afterMOChange(MOChangeEvent arg0) {
				//System.out.println(">>> afterMOChange <<<");
				// TODO Auto-generated method stub

			}
		};

		return l;
	}

	public void unregisterManagedObject(MOGroup moGroup) {
		//System.out.println(">>> unregisterManagedObject");
		moGroup.unregisterMOs(server, getContext(moGroup));
	}

}
