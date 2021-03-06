package mollusc.linguasubtitle.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mollusc <MolluscLab@gmail.com>
 */
public class Vocabulary {
	//<editor-fold desc="Private Fields">
	/**
	 * Version of the database
	 */
	private final int versionDB = 3;
	/**
	 * Name of the database
	 */
	private final String nameDB;
	/**
	 * Connection to the database
	 */
	private Connection connection;
	/**
	 * Statement
	 */
	private Statement statement;
	//</editor-fold>

	//<editor-fold desc="Constructor">

	/**
	 * Constructor of the class Vocabulary
	 */
	public Vocabulary() {
		nameDB = "Vocabulary";
	}
	//</editor-fold>

	//<editor-fold desc="Public Methods">

	/**
	 * Create a database connection
	 */
	public void createConnection() {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			System.err.println(e.getMessage());
		}

		try {
			connection = DriverManager.getConnection("jdbc:sqlite:" + nameDB + ".db");
			statement = connection.createStatement();
			statement.setQueryTimeout(30);
			correctVersion();
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS Stems (stem VARCHAR NOT NULL , Word VARCHAR NOT NULL, Translate VARCHAR, Language VARCHAR NOT NULL, Known INTEGER NOT NULL  DEFAULT 0, Meeting INTEGER NOT NULL  DEFAULT 0, Study INTEGER NOT NULL  DEFAULT 0, PRIMARY KEY(Stem, Language))");
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS Settings(Parameter VARCHAR PRIMARY KEY ASC, Value VARCHAR)");
			SetVersionDB();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Close a database connection
	 */
	public void closeConnection() {
		try {
			if (connection != null) {
				connection.close();
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Get hard words
	 *
	 * @return array of hard words
	 */
	public ArrayList<String> getHardWords() {
		try {
			ArrayList<String> stems = new ArrayList<String>();
			ResultSet rs = statement
					.executeQuery("SELECT Stem FROM Stems WHERE Known=0 AND Study= 0 AND Meeting >10 ORDER BY Meeting DESC LIMIT 10");
			while (rs.next()) {
				String stem = rs.getString("Stem");
				stems.add(stem);
			}
			return stems;
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return null;
	}

	/**
	 * Update values
	 *
	 * @param stem      stem of the word
	 * @param word      word
	 * @param translate translation of the word
	 * @param language  language of the word
	 * @param isKnown   Is word known?
	 * @param isStudy   Is word study?
	 */
	public void updateValues(String stem, String word, String translate, String language, boolean isKnown, boolean isStudy) {
		try {
			String query = "INSERT OR REPLACE INTO Stems (Stem, Word, Translate, Language, Known, Study, Meeting)  VALUES ("
					+ "'" + escapeCharacter(stem) + "',"
					+ "'" + escapeCharacter(word) + "',"
					+ "'" + escapeCharacter(translate) + "',"
					+ "'" + escapeCharacter(language) + "',"
					+ boolToInt(isKnown) + ","
					+ boolToInt(isStudy) + ","
					+ "COALESCE((SELECT Meeting FROM Stems WHERE"
					+ " Stem='" + escapeCharacter(stem) + "' AND Language='" + escapeCharacter(language) + "') + 1,1))";
			statement.executeUpdate(query);
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Get data from the database
	 *
	 * @param stem     key for search
	 * @param language language of words
	 * @return information about the word
	 */
	public ItemVocabulary getItem(String stem, String language) {
		try {
			ResultSet rs = statement
					.executeQuery("SELECT * FROM Stems WHERE Stem='" + escapeCharacter(stem) + "' AND Language='" + language + "'");
			if (rs.next()) {
				String word = rs.getString("Word");
				String translate = rs.getString("Translate");
				boolean known = rs.getString("Known").equals("1");
				boolean study = rs.getString("Study").equals("1");
				int meeting = Integer.parseInt(rs.getString("Meeting"));
				return new ItemVocabulary(word, translate, known, meeting, study);
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return null;
	}

	/**
	 * Get data from the database
	 *
	 * @param unknown            is word unknown
	 * @param known              is word known
	 * @param study              is word studied
	 * @param noBlankTranslation is no blank translation
	 * @param maxMeeting         maximum of meeting
	 * @param language           language of words
	 */
	public ArrayList<ItemVocabulary> getDump(boolean unknown, boolean known, boolean study, boolean noBlankTranslation, int maxMeeting, String language) {
		ArrayList<ItemVocabulary> result = null;
		try {
			ResultSet rs = statement
					.executeQuery("SELECT * FROM Stems WHERE Meeting>=" + maxMeeting + " AND Language='" + language + "' ORDER BY Meeting DESC");
			result = new ArrayList<ItemVocabulary>();
			while (rs.next()) {
				boolean toAdd = false;
				String rWord = rs.getString("Word");
				String rTranslate = rs.getString("Translate");
				boolean rKnown = rs.getString("Known").equals("1");
				boolean rStudy = rs.getString("Study").equals("1");
				int rMeeting = Integer.parseInt(rs.getString("Meeting"));

				if (noBlankTranslation && (rTranslate == null || rTranslate.trim().equals("")))
					continue;

				if (unknown && !rKnown && !rStudy)
					toAdd = true;

				if (!toAdd && known && rKnown)
					toAdd = true;

				if (!toAdd && !known && study && !rKnown && rStudy)
					toAdd = true;

				if (toAdd)
					result.add(new ItemVocabulary(rWord, rTranslate, rKnown, rMeeting, rStudy));
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return result;
	}

	/**
	 * Update Settings
	 *
	 * @param settings of the program
	 */
	public void updateSettings(Map<String, String> settings) {
		try {
			for (String Key : settings.keySet()) {
				statement.executeUpdate("REPLACE INTO Settings VALUES ('" + Key + "','" + settings.get(Key) + "')");
			}
			statement.executeUpdate("REPLACE INTO Settings VALUES ('versionDB','" + versionDB + "')");

		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}

	}

	/**
	 * Get settings from the database
	 *
	 * @return pairs parameter - value
	 */
	public Map<String, String> getSettings() {
		Map<String, String> result = new HashMap<String, String>();
		ResultSet rs;
		try {
			rs = statement.executeQuery("SELECT Parameter, Value FROM Settings");

			while (rs.next())
				result.put(rs.getString("Parameter"), rs.getString("Value"));

			return result;
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return null;
	}

	/**
	 * Escape characters
	 *
	 * @param string text for escaping
	 * @return escaped text
	 */
	private static String escapeCharacter(String string) {
		string = string.replace("'", "''");
		return string;
	}

	/**
	 * Convert boolean to integer
	 *
	 * @param value boolean value
	 * @return integer value
	 */
	private static int boolToInt(boolean value) {
		return value ? 1 : 0;
	}
	//</editor-fold>

	//<editor-fold desc="Private Methods">

	/**
	 * Correct version of the database
	 */
	private void correctVersion() {
		Map<String, String> settings = getSettings();
		if (settings == null || !settings.containsKey("versionDB")) {
			// From version 0 to version 1
			try {
				// Rename field of the table Stems
				statement.executeUpdate("ALTER TABLE Stems RENAME TO tmp_Stems");
				statement.executeUpdate("CREATE TABLE Stems (Stem VARCHAR PRIMARY KEY  NOT NULL ,Word VARCHAR NOT NULL ,Known INTEGER NOT NULL  DEFAULT (0) ,Meeting INTEGER NOT NULL  DEFAULT (0) ,Translate VARCHAR)");
				statement.executeUpdate("INSERT INTO Stems SELECT Stem, Word, Remember, Meeting, Translate FROM tmp_Stems");
				statement.executeUpdate("DROP TABLE tmp_Stems");
				statement.execute("VACUUM");

				// Add column in the table Stems
				statement.executeUpdate("ALTER TABLE Stems ADD COLUMN Study INTEGER NOT NULL DEFAULT 0");

				// Update Study
				statement.executeUpdate("UPDATE Stems SET Study=1 WHERE Translate=\"\"");

				// Rename table from Sittings to Settings
				statement.executeUpdate("ALTER TABLE Sittings RENAME TO Settings");

				// Add parameter in the table Settings
				statement.executeUpdate("REPLACE INTO Settings VALUES ('versionDB','1')");

			} catch (SQLException e) {
				System.err.println(e.getMessage());
			}
		}

		settings = getSettings();
		if (settings != null && settings.containsKey("versionDB") && settings.get("versionDB").equals("1")) {
			try {
				// Rename field of the table Stems
				statement.executeUpdate("ALTER TABLE Stems RENAME TO tmp_Stems");
				statement.executeUpdate("CREATE TABLE Stems (Stem VARCHAR NOT NULL , Word VARCHAR NOT NULL, Translate VARCHAR, Language VARCHAR NOT NULL, Known INTEGER NOT NULL  DEFAULT 0, Meeting INTEGER NOT NULL  DEFAULT 0, Study INTEGER NOT NULL  DEFAULT 0, PRIMARY KEY(Stem, Language))");
				statement.executeUpdate("INSERT INTO Stems SELECT Stem, Word, Translate, 'english', Known, Meeting, Study FROM tmp_Stems");
				statement.executeUpdate("DROP TABLE tmp_Stems");
				statement.execute("VACUUM");

				// Add parameter in the table Settings
				statement.executeUpdate("REPLACE INTO Settings VALUES ('versionDB','2')");

			} catch (SQLException e) {
				System.err.println(e.getMessage());
			}
		}

		settings = getSettings();
		if (settings != null && settings.containsKey("versionDB") && settings.get("versionDB").equals("2")) {
			try {
				statement.executeUpdate("UPDATE Settings SET Value = '1' WHERE  Parameter = 'hideKnownDialog' AND Value = 'true'");
				statement.executeUpdate("UPDATE Settings SET Value = '0' WHERE  Parameter = 'hideKnownDialog' AND Value = 'false'");
				statement.executeUpdate("REPLACE INTO Settings VALUES ('versionDB','3')");
			} catch (SQLException e) {
				System.err.println(e.getMessage());
			}

		}

	}

	/**
	 * Set database version
	 */
	private void SetVersionDB() {
		Map<String, String> settings;
		settings = getSettings();
		if (settings != null &&
				(!settings.containsKey("versionDB") ||
						((settings.containsKey("versionDB") && !settings.get("versionDB").equals(String.valueOf(versionDB)))))) {
			try {
				statement.executeUpdate("REPLACE INTO Settings VALUES ('versionDB','" + versionDB + "')");
			} catch (SQLException e) {
				System.err.println(e.getMessage());
			}
		}
	}
	//</editor-fold>
}
