package main.java.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sound.sampled.UnsupportedAudioFileException;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.snmp4j.agent.mo.DefaultMOTable;
import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.agent.mo.MOMutableColumn;
import org.snmp4j.agent.mo.MOScalar;
import org.snmp4j.agent.mo.MOTableIndex;
import org.snmp4j.agent.mo.MOTableSubIndex;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.SMIConstants;
import org.snmp4j.smi.Variable;

public class TestSNMPAgent {
	public static final String PATH_CONFIG = "/home/leandro/Documentos/GR/TP3/Config/config.json",
			OID_SCAN_FILES = "1.3.6.1.3.42.1.1.0",
			OID_SCAN_TIME = "1.3.6.1.3.42.1.2.0",
			OID_PLAY_TABLE = "1.3.6.1.3.42.2.1.1",
			OID_TOTAL_TABLE = "1.3.6.1.3.42.3.1.1",
			OID_GENRE_TABLE = "1.3.6.1.3.42.4.1.1",
			OID_ARTIST_TABLE = "1.3.6.1.3.42.5.1.1",
			OID_ALBUM_TABLE = "1.3.6.1.3.42.6.1.1",
			OID_SONG_TABLE = "1.3.6.1.3.42.7.1.1",
			OID_HISTORY_TABLE = "1.3.6.1.3.42.8.1.1",
			OID_QUEUE_TABLE = "1.3.6.1.3.42.9.1.1",
			OID_QUEUE_ADDER = "1.3.6.1.3.42.9.2.0";

	public static int SNMP_PORT;
	public static String SNMP_COMMUNITY, SCAN_TIME, PATH_MUSIC, PATH_SONGS,
			PATH_FAVORITES, PATH_LOG;

	public static ArrayList<Genre> genres = new ArrayList<>();
	public static ArrayList<Artist> artists = new ArrayList<>();
	public static ArrayList<Album> albums = new ArrayList<>();
	public static ArrayList<Song> songs = new ArrayList<>();
	public static ArrayList<Integer> total = new ArrayList<>();
	public static ArrayList<String> favorites = new ArrayList<>();

	public static void main(String[] args) throws IOException, InterruptedException, CannotReadException, TagException,
			ReadOnlyFileException, InvalidAudioFrameException, UnsupportedAudioFileException, ParseException {
		System.out.println("Inicializing Agent...");
		TestSNMPAgent client = new TestSNMPAgent("udp:127.0.0.1/161");
		client.init();
	}

	static SNMPAgent agent = null;

	String address = null;

	/**
	 * Constructor
	 *
	 * @param add
	 */
	public TestSNMPAgent(String add) {
		address = add;
	}

	public static ArrayList<String> scanFiles() {
		//System.out.println("Scanning " + PATH_MUSIC + "...");
		writeLog("Scanning song files directory -> " + PATH_MUSIC);

		ArrayList<String> paths = new ArrayList<>();
		ArrayList<String> songs = new ArrayList<>();

		try {
			Stream<Path> walk = Files.walk(Paths.get(PATH_MUSIC));
			paths = (ArrayList<String>) walk.filter(Files::isRegularFile)
					.map(x -> x.toString().substring(PATH_MUSIC.length() + 1, x.toString().length()))
					.collect(Collectors.toList());
			walk.close();
			//paths.forEach(System.out::println);
			FileWriter songsFile = new FileWriter(PATH_SONGS);
			songsFile.write("");

			for (String path: paths) {
				File file = new File(PATH_MUSIC + "/" + path);
				//System.out.println("File file = new File(\"" + PATH_MUSIC + "/" + path + "\"");
				
				if (file.exists()) {
					//System.out.println("file.exists");
					String size = "" + file.length();
					//System.out.println("String size = \"\" + " + file.length());

					try {
						AudioFile audioFile = AudioFileIO.read(file);
						//System.out.println("AudioFile audioFile = AudioFileIO.read(\"" + PATH_MUSIC + "/" + path + "\")");
						AudioHeader audioHeader = audioFile.getAudioHeader();
						//System.out.println("AudioHeader audioHeader = audioFile.getAudioHeader()");
						String trackLength = "" + audioHeader.getTrackLength();
						//System.out.println("String trackLength = \"\" + " + audioHeader.getTrackLength());
						String line = size + " " + trackLength + " " + path;
						songs.add(line);
						songsFile.append(line + "\n");
					}
					
					catch (CannotReadException | TagException | ReadOnlyFileException
							| InvalidAudioFrameException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

			songsFile.close();
		}

		catch (IOException e) {
			e.printStackTrace();
		}

		//writeLog("Scanned " + result.size() + " song files");

		return songs;
	}

	public static void generateMusic(ArrayList<String> songLines, ArrayList<String> favoritePaths) {
		total = new ArrayList<>();
		genres = new ArrayList<>();
		artists = new ArrayList<>();
		albums = new ArrayList<>();
		songs = new ArrayList<>();
		
		for (int i = 0; i < songLines.size(); i ++) {
			String line = songLines.get(i);
			String[] params1 = line.split(" ", 3);
			long songSize = Long.parseLong(params1[0]);
			long songLength = Long.parseLong(params1[1]);
			String path = params1[2];
			String[] params = path.split("/", 4);
			String fileName = params[params.length - 1];
			String songName, songFormat = null;
			boolean favorited = favoritePaths.contains(path);

			if (fileName.contains(".")) {
				songName = fileName.substring(0, fileName.lastIndexOf("."));
				songFormat = fileName.substring(fileName.lastIndexOf(".") + 1);
			}

			else {
				songName = fileName;
			}

			int genreIndex = 0, artistIndex = 0, albumIndex = 0, songIndex = i + 1;

			if (params.length > 1) {
				Genre genre = new Genre(params[0]);

				if (!genres.contains(genre)) {
					genres.add(genre);
				}

				genreIndex = genres.indexOf(genre) + 1;
				genres.get(genreIndex - 1).incSongs();
				genres.get(genreIndex - 1).addSong(songIndex);

				if (params.length > 2) {
					Artist artist = new Artist(params[1], genreIndex);

					if (!artists.contains(artist)) {
						artists.add(artist);
						genres.get(genreIndex - 1).incArtists();
					}

					artistIndex = artists.indexOf(artist) + 1;
					artists.get(artistIndex - 1).incSongs();
					artists.get(artistIndex - 1).addSong(songIndex);

					if (params.length > 3) {
						Album album = new Album(params[2], genreIndex, artistIndex);

						if (!albums.contains(album)) {
							albums.add(album);
							artists.get(artistIndex - 1).incAlbums();
							genres.get(genreIndex - 1).incAlbums();
						}

						albumIndex = albums.indexOf(album) + 1;
						albums.get(albumIndex - 1).incSongs();
						albums.get(albumIndex - 1).addSong(songIndex);
					}
				}
			}

			Song song = new Song(PATH_MUSIC + "/" + path, path, songName,
					songFormat, songLength, songSize, genreIndex, artistIndex,
					albumIndex, favorited);
			songs.add(song);
			total.add(songIndex);
		}

		writeLog("Found " + genres.size() + " genres, " + artists.size() + " artists, " + albums.size() + " albums and " + songs.size() + " songs where " + favorites.size() + " are favorites");
	}

	public static void writeConf(String time) throws IOException {
		writeLog("Updating config file -> " + PATH_CONFIG);
		SCAN_TIME = time;

		String jsonContent = "{\n"
				+ "\t\"community\": \"" + SNMP_COMMUNITY + "\",\n"
				+ "\t\"port\": " + SNMP_PORT + ",\n"
				+ "\t\"scanTime\": \"" + SCAN_TIME + "\",\n"
				+ "\t\"songsDirectory\": \"" + PATH_MUSIC + "\",\n"
				+ "\t\"songsList\": \"" + PATH_SONGS + "\",\n"
				+ "\t\"favoritesList\": \"" + PATH_FAVORITES + "\",\n"
				+ "\t\"logFile\": \"" + PATH_LOG + "\"\n"
				+ "}";

		/*JSONObject jsonObject = new JSONObject();
		jsonObject.put("community", SNMP_COMMUNITY);
		jsonObject.put("port", SNMP_PORT);
		jsonObject.put("scanTime", SCAN_TIME);
		jsonObject.put("songsDirectory", PATH_MUSIC);
		jsonObject.put("songsList", PATH_SONGS);
		jsonObject.put("favoritesList", PATH_FAVORITES);
		jsonObject.put("logFile", PATH_LOG);*/

		FileWriter fWriter = new FileWriter(PATH_CONFIG);
		fWriter.write(jsonContent);
		fWriter.flush();
		fWriter.close();
	}

	public static void writeLog(String action) {
		DateTimeFormatter timeDTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime now = LocalDateTime.now();
		String time = timeDTF.format(now);

		try {
			FileWriter fWriter = new FileWriter(PATH_LOG, true);
			System.out.println(">>> LOGGING >>> " + time + " | " + action);
			fWriter.append(time + " | " + action + "\n");
			fWriter.flush();
			fWriter.close();
		}
		
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void init() throws IOException, InterruptedException, ParseException {
		System.out.println("Reading " + PATH_CONFIG + "...");

		JSONParser configJson = new JSONParser();
		FileReader configReader = new FileReader(PATH_CONFIG);
		JSONObject jsonObject = (JSONObject) configJson.parse(configReader);

		SNMP_COMMUNITY = (String) jsonObject.get("community");
		SNMP_PORT = ((Long) jsonObject.get("port")).intValue();
		SCAN_TIME = (String) jsonObject.get("scanTime");
		PATH_MUSIC = (String) jsonObject.get("songsDirectory");
		PATH_SONGS = (String) jsonObject.get("songsList");
		PATH_FAVORITES = (String) jsonObject.get("favoritesList");
		PATH_LOG = (String) jsonObject.get("logFile");

		System.out.println("* Community: " + SNMP_COMMUNITY
				+ "\n* Port: " + SNMP_PORT
				+ "\n* Last scan time: " + SCAN_TIME
				+ "\n* Songs directory: " + PATH_MUSIC
				+ "\n* Songs list: " + PATH_SONGS
				+ "\n* Favorites list: " + PATH_FAVORITES
				+ "\n* Log file: " + PATH_LOG);
		
		writeLog("Initializing agent");
		writeLog("Last scan time: " + SCAN_TIME);
		agent = new SNMPAgent("0.0.0.0/" + SNMP_PORT, SNMP_COMMUNITY, PATH_FAVORITES);
		agent.start();

		// Since BaseAgent registers some MIBs by default we need to unregister
		// one before we register our own sysDescr. Normally you would
		// override that method and register the MIBs that you need

		writeLog("Registering managed objects");
		agent.unregisterManagedObject(agent.getSnmpv2MIB());

		DefaultMOTable playTable = createPlayTable(),
				totalTable = createTotalTable(),
				genreTable = createGenreTable(),
				artistTable = createArtistTable(),
				albumTable = createAlbumTable(),
				songTable = createSongTable(),
				historyTable = createHistoryTable(),
				queueTable = createQueueTable();

		agent.registerManagedTables(playTable, totalTable, genreTable,
				artistTable, albumTable, songTable, historyTable, queueTable);

		OID scanFilesOid = new OID(OID_SCAN_FILES),
				scanTimeOid = new OID(OID_SCAN_TIME),
				queueAdderOid = new OID(OID_QUEUE_ADDER);
		agent.registerManagedScalar(new MOScalar(scanFilesOid, MOAccessImpl.ACCESS_READ_WRITE, new Integer32(1)));
		agent.registerManagedScalar(new MOScalar(scanTimeOid, MOAccessImpl.ACCESS_READ_ONLY, new OctetString(SCAN_TIME)));
		agent.registerManagedScalar(new MOScalar(queueAdderOid, MOAccessImpl.ACCESS_WRITE_ONLY, new Integer32(0)));

		File songsFile = new File(PATH_SONGS); 
		ArrayList<String> songLines = new ArrayList<>();

		if (songsFile.createNewFile()) {
			songLines = scanFiles();
		}

		else {
			//System.out.println("Reading " + PATH_SONGS + "...");
			writeLog("Reading songs list file -> " + PATH_SONGS);
			BufferedReader songsBR = new BufferedReader(new FileReader(songsFile)); 
			String songLine;

			while ((songLine = songsBR.readLine()) != null) {
				songLines.add(songLine);
			}

			songsBR.close();
		}

		for (String songLine: songLines) {
			System.out.println("> " + songLine);
		}

		writeLog("Reading favorite songs list file -> " + PATH_FAVORITES);
		File favoritesFile = new File(PATH_FAVORITES);

		if (favoritesFile.exists()) {
			//System.out.println("Reading " + PATH_FAVORITES + "...");
			favorites = new ArrayList<>();
			BufferedReader favoritesBR = new BufferedReader(new FileReader(favoritesFile)); 
			String favoritesPath;

			while ((favoritesPath = favoritesBR.readLine()) != null) {
				System.out.println("* " + favoritesPath);
				favorites.add(favoritesPath);
			}

			favoritesBR.close();
		}

		generateMusic(songLines, favorites);

		/*ArrayList<String> genreNames = new ArrayList<>(),
				artistNames = new ArrayList<>(),
				albumNames = new ArrayList<>(),
				songNames = new ArrayList<>();*/

		/*System.out.println("SONGS:::");

		for (Song s: songs) {
			System.out.println("* " + s.getGenre() + " " + s.getArtist() + " " + s.getAlbum() + " " + s.getName());
		}*/

		agent.setMusic();
		writeLog("SNMP Agent ready");

		while (true) {
			Thread.sleep(20000);
		}
	}

	public DefaultMOTable createPlayTable() {
		MOTableSubIndex[] subIndexes = new MOTableSubIndex[] { new MOTableSubIndex(SMIConstants.SYNTAX_INTEGER32) };

		MOTableIndex indexDef = new MOTableIndex(subIndexes, false);

		MOMutableColumn musicPlayGenre = new MOMutableColumn<>(1, SMIConstants.SYNTAX_INTEGER32, MOAccessImpl.ACCESS_READ_WRITE),
				musicPlayArtist = new MOMutableColumn<>(2, SMIConstants.SYNTAX_INTEGER32, MOAccessImpl.ACCESS_READ_WRITE),
				musicPlayAlbum = new MOMutableColumn<>(3, SMIConstants.SYNTAX_INTEGER32, MOAccessImpl.ACCESS_READ_WRITE),
				musicPlaySong = new MOMutableColumn<>(4, SMIConstants.SYNTAX_INTEGER32, MOAccessImpl.ACCESS_READ_WRITE),
				musicPlayName = new MOMutableColumn<>(5, SMIConstants.SYNTAX_OCTET_STRING, MOAccessImpl.ACCESS_READ_ONLY),
				musicPlayTime = new MOMutableColumn<>(6, SMIConstants.SYNTAX_TIMETICKS, MOAccessImpl.ACCESS_READ_ONLY),
				musicPlayLength = new MOMutableColumn<>(7, SMIConstants.SYNTAX_TIMETICKS, MOAccessImpl.ACCESS_READ_ONLY),
				musicPlayStatus = new MOMutableColumn<>(8, SMIConstants.SYNTAX_INTEGER, MOAccessImpl.ACCESS_READ_WRITE),
				musicPlayOrder = new MOMutableColumn<>(9, SMIConstants.SYNTAX_INTEGER, MOAccessImpl.ACCESS_READ_WRITE),
				musicPlayList = new MOMutableColumn<>(10, SMIConstants.SYNTAX_INTEGER, MOAccessImpl.ACCESS_READ_WRITE),
				musicPlayIndex = new MOMutableColumn<>(11, SMIConstants.SYNTAX_COUNTER32, MOAccessImpl.ACCESS_READ_ONLY),
				musicPlayQueue = new MOMutableColumn<>(12, SMIConstants.SYNTAX_COUNTER32, MOAccessImpl.ACCESS_READ_WRITE);

		MOMutableColumn<Variable>[] columns = new MOMutableColumn[] {
			musicPlayGenre,
			musicPlayArtist,
			musicPlayAlbum,
			musicPlaySong,
			musicPlayName,
			musicPlayTime,
			musicPlayLength,
			musicPlayStatus,
			musicPlayOrder,
			musicPlayList,
			musicPlayIndex,
			musicPlayQueue
		};

		DefaultMOTable moTable = new DefaultMOTable(new OID(OID_PLAY_TABLE), indexDef, columns);
		moTable.setVolatile(true);

		return moTable;
	}

	public DefaultMOTable createTotalTable() {
		MOTableSubIndex[] subIndexes = new MOTableSubIndex[] { new MOTableSubIndex(SMIConstants.SYNTAX_INTEGER32) };

		MOTableIndex indexDef = new MOTableIndex(subIndexes, false);
		MOMutableColumn musicTotalGenres = new MOMutableColumn<>(1, SMIConstants.SYNTAX_COUNTER32, MOAccessImpl.ACCESS_READ_ONLY),
				musicTotalArtists = new MOMutableColumn<>(2, SMIConstants.SYNTAX_COUNTER32, MOAccessImpl.ACCESS_READ_ONLY),
				musicTotalAlbums = new MOMutableColumn<>(3, SMIConstants.SYNTAX_COUNTER32, MOAccessImpl.ACCESS_READ_ONLY),
				musicTotalSongs = new MOMutableColumn<>(4, SMIConstants.SYNTAX_COUNTER32, MOAccessImpl.ACCESS_READ_ONLY);

		MOMutableColumn<Variable>[] columns = new MOMutableColumn[] {
			musicTotalGenres,
			musicTotalArtists,
			musicTotalAlbums,
			musicTotalSongs
		};

		DefaultMOTable moTable = new DefaultMOTable(new OID(OID_TOTAL_TABLE), indexDef, columns);
		moTable.setVolatile(true);

		return moTable;
	}

	public DefaultMOTable createGenreTable() {
		MOTableSubIndex[] subIndexes = new MOTableSubIndex[] { new MOTableSubIndex(SMIConstants.SYNTAX_INTEGER32) };

		MOTableIndex indexDef = new MOTableIndex(subIndexes, false);

		MOMutableColumn musicGenreIndex = new MOMutableColumn<>(1, SMIConstants.SYNTAX_INTEGER32, MOAccessImpl.ACCESS_READ_ONLY),
				musicGenreName = new MOMutableColumn<>(2, SMIConstants.SYNTAX_OCTET_STRING, MOAccessImpl.ACCESS_READ_ONLY),
				musicGenreArtists = new MOMutableColumn<>(3, SMIConstants.SYNTAX_COUNTER32, MOAccessImpl.ACCESS_READ_ONLY),
				musicGenreAlbums = new MOMutableColumn<>(4, SMIConstants.SYNTAX_COUNTER32, MOAccessImpl.ACCESS_READ_ONLY),
				musicGenreSongs = new MOMutableColumn<>(5, SMIConstants.SYNTAX_COUNTER32, MOAccessImpl.ACCESS_READ_ONLY);

		MOMutableColumn<Variable>[] columns = new MOMutableColumn[] {
			musicGenreIndex,
			musicGenreName,
			musicGenreArtists,
			musicGenreAlbums,
			musicGenreSongs
		};

		DefaultMOTable moTable = new DefaultMOTable(new OID(OID_GENRE_TABLE), indexDef, columns);
		moTable.setVolatile(true);

		return moTable;
	}

	public DefaultMOTable createArtistTable() {
		MOTableSubIndex[] subIndexes = new MOTableSubIndex[] { new MOTableSubIndex(SMIConstants.SYNTAX_INTEGER32) };

		MOTableIndex indexDef = new MOTableIndex(subIndexes, false);

		MOMutableColumn musicArtistIndex = new MOMutableColumn<>(1, SMIConstants.SYNTAX_INTEGER32, MOAccessImpl.ACCESS_READ_ONLY),
				musicArtistGenre = new MOMutableColumn<>(2, SMIConstants.SYNTAX_INTEGER32, MOAccessImpl.ACCESS_READ_ONLY),
				musicArtistName = new MOMutableColumn<>(3, SMIConstants.SYNTAX_OCTET_STRING, MOAccessImpl.ACCESS_READ_ONLY),
				musicArtistAlbums = new MOMutableColumn<>(4, SMIConstants.SYNTAX_COUNTER32, MOAccessImpl.ACCESS_READ_ONLY),
				musicArtistSongs = new MOMutableColumn<>(5, SMIConstants.SYNTAX_COUNTER32, MOAccessImpl.ACCESS_READ_ONLY);

		MOMutableColumn<Variable>[] columns = new MOMutableColumn[] {
			musicArtistIndex,
			musicArtistGenre,
			musicArtistName,
			musicArtistAlbums,
			musicArtistSongs
		};

		DefaultMOTable moTable = new DefaultMOTable(new OID(OID_ARTIST_TABLE), indexDef, columns);
		moTable.setVolatile(true);

		return moTable;
	}

	public DefaultMOTable createAlbumTable() {
		MOTableSubIndex[] subIndexes = new MOTableSubIndex[] { new MOTableSubIndex(SMIConstants.SYNTAX_INTEGER32) };

		MOTableIndex indexDef = new MOTableIndex(subIndexes, false);

		MOMutableColumn musicAlbumIndex = new MOMutableColumn<>(1, SMIConstants.SYNTAX_INTEGER32, MOAccessImpl.ACCESS_READ_ONLY),
				musicAlbumGenre = new MOMutableColumn<>(2, SMIConstants.SYNTAX_INTEGER32, MOAccessImpl.ACCESS_READ_ONLY),
				musicAlbumArtist = new MOMutableColumn<>(3, SMIConstants.SYNTAX_INTEGER32, MOAccessImpl.ACCESS_READ_ONLY),
				musicAlbumName = new MOMutableColumn<>(4, SMIConstants.SYNTAX_OCTET_STRING, MOAccessImpl.ACCESS_READ_ONLY),
				musicAlbumSongs = new MOMutableColumn<>(5, SMIConstants.SYNTAX_COUNTER32, MOAccessImpl.ACCESS_READ_ONLY);

		MOMutableColumn<Variable>[] columns = new MOMutableColumn[] {
			musicAlbumIndex,
			musicAlbumGenre,
			musicAlbumArtist,
			musicAlbumName,
			musicAlbumSongs
		};

		DefaultMOTable moTable = new DefaultMOTable(new OID(OID_ALBUM_TABLE), indexDef, columns);
		moTable.setVolatile(true);

		return moTable;
	}

	public DefaultMOTable createSongTable() {
		MOTableSubIndex[] subIndexes = new MOTableSubIndex[] { new MOTableSubIndex(SMIConstants.SYNTAX_INTEGER32) };

		MOTableIndex indexDef = new MOTableIndex(subIndexes, false);

		MOMutableColumn musicSongIndex = new MOMutableColumn<>(1, SMIConstants.SYNTAX_INTEGER32, MOAccessImpl.ACCESS_READ_ONLY),
				musicSongGenre = new MOMutableColumn<>(2, SMIConstants.SYNTAX_INTEGER32, MOAccessImpl.ACCESS_READ_ONLY),
				musicSongArtist = new MOMutableColumn<>(3, SMIConstants.SYNTAX_INTEGER32, MOAccessImpl.ACCESS_READ_ONLY),
				musicSongAlbum = new MOMutableColumn<>(4, SMIConstants.SYNTAX_INTEGER32, MOAccessImpl.ACCESS_READ_ONLY),
				musicSongName = new MOMutableColumn<>(5, SMIConstants.SYNTAX_OCTET_STRING, MOAccessImpl.ACCESS_READ_ONLY),
				musicSongFormat = new MOMutableColumn<>(6, SMIConstants.SYNTAX_OCTET_STRING, MOAccessImpl.ACCESS_READ_ONLY),
				musicSongLength = new MOMutableColumn<>(7, SMIConstants.SYNTAX_TIMETICKS, MOAccessImpl.ACCESS_READ_ONLY),
				musicSongSize = new MOMutableColumn<>(8, SMIConstants.SYNTAX_INTEGER32, MOAccessImpl.ACCESS_READ_ONLY),
				musicSongPath = new MOMutableColumn<>(9, SMIConstants.SYNTAX_OCTET_STRING, MOAccessImpl.ACCESS_READ_ONLY),
				musicSongFavorited = new MOMutableColumn<>(10, SMIConstants.SYNTAX_INTEGER, MOAccessImpl.ACCESS_READ_WRITE);

		MOMutableColumn<Variable>[] columns = new MOMutableColumn[] {
			musicSongIndex,
			musicSongGenre,
			musicSongArtist,
			musicSongAlbum,
			musicSongName,
			musicSongFormat,
			musicSongLength,
			musicSongSize,
			musicSongPath,
			musicSongFavorited
		};

		DefaultMOTable moTable = new DefaultMOTable(new OID(OID_SONG_TABLE), indexDef, columns);
		moTable.setVolatile(true);

		return moTable;
	}

	public DefaultMOTable createHistoryTable() {		
		MOTableSubIndex[] subIndexes = new MOTableSubIndex[] { new MOTableSubIndex(SMIConstants.SYNTAX_INTEGER32) };

		MOTableIndex indexDef = new MOTableIndex(subIndexes, false);

		MOMutableColumn musicHistoryPosition = new MOMutableColumn<>(1, SMIConstants.SYNTAX_INTEGER32, MOAccessImpl.ACCESS_READ_ONLY),
				musicHistoryGenre = new MOMutableColumn<>(2, SMIConstants.SYNTAX_OCTET_STRING, MOAccessImpl.ACCESS_READ_ONLY),
				musicHistoryArtist = new MOMutableColumn<>(3, SMIConstants.SYNTAX_OCTET_STRING, MOAccessImpl.ACCESS_READ_ONLY),
				musicHistoryAlbum = new MOMutableColumn<>(4, SMIConstants.SYNTAX_OCTET_STRING, MOAccessImpl.ACCESS_READ_ONLY),
				musicHistorySong = new MOMutableColumn<>(5, SMIConstants.SYNTAX_OCTET_STRING, MOAccessImpl.ACCESS_READ_WRITE),
				musicHistoryName = new MOMutableColumn<>(6, SMIConstants.SYNTAX_OCTET_STRING, MOAccessImpl.ACCESS_READ_ONLY),
				musicHistoryLength = new MOMutableColumn<>(7, SMIConstants.SYNTAX_TIMETICKS, MOAccessImpl.ACCESS_READ_ONLY);

		MOMutableColumn<Variable>[] columns = new MOMutableColumn[] {
			musicHistoryPosition,
			musicHistoryGenre,
			musicHistoryArtist,
			musicHistoryAlbum,
			musicHistorySong,
			musicHistoryName,
			musicHistoryLength
		};

		DefaultMOTable moTable = new DefaultMOTable(new OID(OID_HISTORY_TABLE), indexDef, columns);
		moTable.setVolatile(true);

		return moTable;
	}

	public static DefaultMOTable createQueueTable() {		
		MOTableSubIndex[] subIndexes = new MOTableSubIndex[] { new MOTableSubIndex(SMIConstants.SYNTAX_INTEGER32) };

		MOTableIndex indexDef = new MOTableIndex(subIndexes, false);

		MOMutableColumn musicQueuePosition = new MOMutableColumn<>(1, SMIConstants.SYNTAX_INTEGER32, MOAccessImpl.ACCESS_READ_ONLY),
				musicQueueGenre = new MOMutableColumn<>(2, SMIConstants.SYNTAX_OCTET_STRING, MOAccessImpl.ACCESS_READ_ONLY),
				musicQueueArtist = new MOMutableColumn<>(3, SMIConstants.SYNTAX_OCTET_STRING, MOAccessImpl.ACCESS_READ_ONLY),
				musicQueueAlbum = new MOMutableColumn<>(4, SMIConstants.SYNTAX_OCTET_STRING, MOAccessImpl.ACCESS_READ_ONLY),
				musicQueueSong = new MOMutableColumn<>(5, SMIConstants.SYNTAX_INTEGER32, MOAccessImpl.ACCESS_READ_WRITE),
				musicQueueName = new MOMutableColumn<>(6, SMIConstants.SYNTAX_OCTET_STRING, MOAccessImpl.ACCESS_READ_ONLY),
				musicQueueLength = new MOMutableColumn<>(7, SMIConstants.SYNTAX_TIMETICKS, MOAccessImpl.ACCESS_READ_ONLY);

		MOMutableColumn<Variable>[] columns = new MOMutableColumn[] {
			musicQueuePosition,
			musicQueueGenre,
			musicQueueArtist,
			musicQueueAlbum,
			musicQueueSong,
			musicQueueName,
			musicQueueLength
		};

		DefaultMOTable moTable = new DefaultMOTable(new OID(OID_QUEUE_TABLE), indexDef, columns);
		moTable.setVolatile(true);

		return moTable;
	}

}
