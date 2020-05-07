import java.sql.*;
import java.util.*;
public class Query extends QuerySearchOnly {

	// Logged In User
	private String username; // customer username is unique
	protected ArrayList<Itinerary> EverySingleFlight = allFEntries;
	protected ArrayList<Integer> EveryFlightItinerary = iIndex;
	protected ArrayList<ItineraryIndex> FlightwithIndex = Findex;
	// transactions
	private static final String BEGIN_TRANSACTION_SQL = "SET TRANSACTION ISOLATION LEVEL SERIALIZABLE; BEGIN TRANSACTION;";
	protected PreparedStatement beginTransactionStatement;

	private static final String COMMIT_SQL = "COMMIT TRANSACTION";
	protected PreparedStatement commitTransactionStatement;

	private static final String ROLLBACK_SQL = "ROLLBACK TRANSACTION";
	protected PreparedStatement rollbackTransactionStatement;

	private static final String USER_CREATION = "INSERT INTO Users VALUES (?, ?, ?)";
	protected PreparedStatement beginUserAccountCreation;

	private static final String USER_LOGIN = "SELECT COUNT(*) as cexists FROM Users u WHERE u.username = ? COLLATE SQL_Latin1_General_CP1_CI_AS AND u.password = ? COLLATE SQL_Latin1_General_CP1_CI_AS";
	protected PreparedStatement checkIfUserLoggedIn;

	private static final String USER_EXISTS = "SELECT u.username as uname FROM Users u WHERE u.username = ? COLLATE SQL_Latin1_General_CP1_CI_AS";
	protected PreparedStatement checkIfUserExists;

	private static final String MAKE_RESERVATION = "INSERT INTO Reservations VALUES (?,?,?,?,?, ?, ?)";
	protected PreparedStatement makeReservationStatement;
	
	private static final String CHECK_DATES = "SELECT d.day as days11, d.username as user11 FROM Dates d WHERE d.day = ? AND d.username = ?";
	protected PreparedStatement checkSameDates; 
	
	private static final String INSERT_DATE = "INSERT INTO Dates VALUES (?, ?)";
	protected PreparedStatement insertNewDate;
	
	
	
	private static final String GET_RID = "SELECT MAX(rid) as mrid FROM Reservations";
	protected PreparedStatement maxRID;
	
	private static final String RESERV_NUM = "SELECT COUNT(*) as rcount FROM Reservations r WHERE r.fid1 = ? OR (fid2 <> -1 AND fid2 = ?)";
	protected PreparedStatement numReserv;
	
	private static final String DELETE_RESERV = "DELETE FROM Reservations";
	protected PreparedStatement deleteReserv;
	
	private static final String DELETE_DATES = "DELETE FROM Dates";
	protected PreparedStatement deleteDates;
	
	private static final String DELETE_USERS = "DELETE FROM Users";
	protected PreparedStatement deleteUsers;
	
	private static final String ADD_INIT_RESERVATION = "INSERT INTO Reservations VALUES (NULL, 0, NULL,NULL,NULL,NULL, NULL)";
	protected PreparedStatement addZeroReserv;
	
	private static final String CHECK_FOR_RESERV = "SELECT r.rid as reserve, r.username as username1 FROM "
			+ "Reservations r WHERE r.rid = ? AND r.username = ? AND r.paid = 0";
	protected PreparedStatement checkSpecReserv;
	
	private static final String GET_BALANCE = "SELECT u.balance as bal FROM Users u WHERE u.username = ?";
	protected PreparedStatement getBalance;
	
	private static final String GET_FLIGHT_COST = "SELECT r.price as rprice FROM Reservations r WHERE r.rid = ?";
	protected PreparedStatement getFlightPrice;
	
	private static final String UPDATE_BALANCE = "UPDATE Users SET balance = ? WHERE Users.username = ?";
	protected PreparedStatement updateBalance;
	
	private static final String CHANGE_PAY_STATUS = "UPDATE Reservations SET Reservations.paid = 1 WHERE "
			+ "Reservations.rid = ?";
	protected PreparedStatement changePaid;
	
	private static final String CHECK_ALL_RESERVES = "SELECT r.rid as rrid FROM Reservations r WHERE r.username = ?";
	protected PreparedStatement checkEveryReserve;
	
	private static final String GET_FLIGHT_INFO = "SELECT f.fid as fid, f.day_of_month as daymonth, f.carrier_id as carrier,"
			+ "f.flight_num as flightnum, f.origin_city as ocity, f.dest_city as destcity, f.actual_time as time, f.capacity as capacity, "
			+ "f.price as price FROM Flights f WHERE f.fid = ?";
	protected PreparedStatement getFlightInfo;
	
	private static final String GET_FLIGHTIDS = "SELECT r.rid as rrid, r.fid1 as fid1, r.fid2 as fid2, r.paid FROM Reservations r WHERE r.username = ? AND "
			+ "r.cancel = 0";
	protected PreparedStatement getFlightIDs;
	
	private static final String CHECK_FOR_CANCEL_RESERVATION = "SELECT r.rid as rrid FROM Reservations r WHERE r.username = ? AND r.rid = ? AND"
			+ " r.cancel = 0";
	protected PreparedStatement checkForCancel;

	private static final String RESERVE_TO_CANCEL = "UPDATE Reservations SET Reservations.cancel = 1 WHERE Reservations.rid = ?";
	protected PreparedStatement cancelReserve;
	
	private static final String IF_USER_PAID = "SELECT r.paid as rpaid FROM Reservations r WHERE r.rid = ?";
	protected PreparedStatement userPaid;
	
	private static final String COUNT_ALL_RESERVED_FLIGHTS1 = "SELECT count(*) as count FROM Reservations r WHERE r.fid1 = ?";
	protected PreparedStatement countFID1;
	
	private static final String COUNT_ALL_RESERVED_FLIGHTS2 = "SELECT count(*) as count FROM Reservations r WHERE r.fid2 = ?";
	protected PreparedStatement countFID2;
	
	private static final String ADD_TO_RID = "INSERT INTO ReservationIds Values(1)";
	protected PreparedStatement addToRID;
	
	private static final String SELECT_RID = "SELECT MAX(ri.rid) as rid FROM ReservationIds ri";
	protected PreparedStatement selectRID;
	
	private static final String INSERT_RID = "SELECT (rid) as rid2 FROM ReservationIds; UPDATE ReservationIds SET rid = rid+1";
	protected PreparedStatement insertRID;
	
	private static final String DELETE_FROM_RID = "DELETE FROM ReservationIds";
	protected PreparedStatement deletemyRID;
	
	public Query(String configFilename) {
		super(configFilename);
		this.username = null;
	}
	//public ArrayList<Itinerary> allFlightEntries = allFEntries;

	/**
	 * Clear the data in any custom tables created. Do not drop any tables and do not
	 * clear the flights table. You should clear any tables you use to store reservations
	 * and reset the next reservation ID to be 1.
	 */
	public void clearTables () throws SQLException
	{
		// your code here
		deleteReserv.clearParameters();
		deleteReserv.executeUpdate();
		deleteDates.clearParameters();
		deleteDates.executeUpdate();
		deleteUsers.clearParameters();
		deleteUsers.executeUpdate();
		addZeroReserv.clearParameters();
		addZeroReserv.executeUpdate();
		deletemyRID.clearParameters();
		deletemyRID.executeUpdate();
		addToRID.clearParameters();
		addToRID.executeUpdate();
	}


	/**
	 * prepare all the SQL statements in this method.
	 * "preparing" a statement is almost like compiling it.
	 * Note that the parameters (with ?) are still not filled in
	 */
	@Override
	public void prepareStatements() throws Exception
	{
		super.prepareStatements();
		beginTransactionStatement = conn.prepareStatement(BEGIN_TRANSACTION_SQL);
		commitTransactionStatement = conn.prepareStatement(COMMIT_SQL);
		rollbackTransactionStatement = conn.prepareStatement(ROLLBACK_SQL);

		/* add here more prepare statements for all the other queries you need */
		/* . . . . . . */
		beginUserAccountCreation = conn.prepareStatement(USER_CREATION);
		checkIfUserExists = conn.prepareStatement(USER_EXISTS);
		checkIfUserLoggedIn = conn.prepareStatement(USER_LOGIN);
		makeReservationStatement = conn.prepareStatement(MAKE_RESERVATION);
		checkSameDates = conn.prepareStatement(CHECK_DATES);
		maxRID = conn.prepareStatement(GET_RID);
		numReserv = conn.prepareStatement(RESERV_NUM);
		insertNewDate = conn.prepareStatement(INSERT_DATE);
		deleteReserv = conn.prepareStatement(DELETE_RESERV);
		deleteDates = conn.prepareStatement(DELETE_DATES);
		deleteUsers = conn.prepareStatement(DELETE_USERS);
		addZeroReserv = conn.prepareStatement(ADD_INIT_RESERVATION);
		checkSpecReserv = conn.prepareStatement(CHECK_FOR_RESERV);
		getBalance = conn.prepareStatement(GET_BALANCE);
		getFlightPrice = conn.prepareStatement(GET_FLIGHT_COST);
		updateBalance = conn.prepareStatement(UPDATE_BALANCE);
		changePaid = conn.prepareStatement(CHANGE_PAY_STATUS);
		checkEveryReserve = conn.prepareStatement(CHECK_ALL_RESERVES);
		getFlightInfo = conn.prepareStatement(GET_FLIGHT_INFO);
		getFlightIDs = conn.prepareStatement(GET_FLIGHTIDS);
		checkForCancel = conn.prepareStatement(CHECK_FOR_CANCEL_RESERVATION);
		cancelReserve = conn.prepareStatement(RESERVE_TO_CANCEL);
		userPaid = conn.prepareStatement(IF_USER_PAID);
		countFID1 = conn.prepareStatement(COUNT_ALL_RESERVED_FLIGHTS1);
		countFID2 = conn.prepareStatement(COUNT_ALL_RESERVED_FLIGHTS2);
		addToRID = conn.prepareStatement(ADD_TO_RID);
		selectRID = conn.prepareStatement(SELECT_RID);
		insertRID = conn.prepareStatement(INSERT_RID);
		deletemyRID=conn.prepareStatement(DELETE_FROM_RID);
	}


	/**
	 * Takes a user's username and password and attempts to log the user in.
	 *
	 * @return If someone has already logged in, then return "User already logged in\n"
	 * For all other errors, return "Login failed\n".
	 *
	 * Otherwise, return "Logged in as [username]\n".
	 */
	public String transaction_login(String username, String password) 
	{ try {
		checkIfUserLoggedIn.clearParameters();
		checkIfUserLoggedIn.setString(1, username);
		checkIfUserLoggedIn.setString(2, password);
		ResultSet loggedInNot = checkIfUserLoggedIn.executeQuery();
		loggedInNot.next();
		if (this.username != null && this.username.length() != 0) {
			return "User already logged in\n";
		} else if (loggedInNot.getInt("cexists") == 0) {
			return "Login failed\n";
		} else {
			this.username = username;
			EverySingleFlight = new ArrayList<>();
			EveryFlightItinerary = new ArrayList<>();
			FlightwithIndex = new ArrayList<>();
			return "Logged in as " + username +"\n";
		}
		} catch(SQLException e){
			e.printStackTrace();
			return "Login failed\n";
		}
	}

	/**
	 * Implement the create user function.
	 *
	 * @param username new user's username. User names are unique the system.
	 * @param password new user's password.
	 * @param initAmount initial amount to deposit into the user's account, should be >= 0 (failure otherwise).
	 *
	 * @return either "Created user {@code username}\n" or "Failed to create user\n" if failed.
	 */
	public String transaction_createCustomer (String username, String password, int initAmount)
	{
		try {
			beginTransaction();
			if (initAmount < 0 || password == null ||password.length() == 0 || username == null || username.length() == 0) {
				rollbackTransaction();
				return "Failed to create user\n";
			} else {	
					if(checkIfUserCreated(username)) {
						rollbackTransaction();
						return "Failed to create user\n";
					}
					beginUserAccountCreation.clearParameters();
					beginUserAccountCreation.setString(1, username);
					beginUserAccountCreation.setInt(2, initAmount);
					beginUserAccountCreation.setString(3, password);					
					
					beginUserAccountCreation.execute();
					commitTransaction();
					return "Created user " + username + "\n";		
			}
		} catch(SQLException e) {
			e.printStackTrace();
			try {
				rollbackTransaction();
			} catch (Exception e1) {
				e1.printStackTrace();
				
			}
			return "Failed to create user\n";
		}
	}

	public boolean checkIfUserCreated(String username) throws SQLException{
		checkIfUserExists.clearParameters();
		checkIfUserExists.setString(1, username);
		ResultSet result = checkIfUserExists.executeQuery();
		boolean hasResult = result.next();
		return hasResult;
	}
	/**
	 * Implements the book itinerary function.
	 *
	 * @param itineraryId ID of the itinerary to book. This must be one that is returned by search in the current session.
	 *
	 * @return If the user is not logged in, then return "Cannot book reservations, not logged in\n".
	 * If try to book an itinerary with invalid ID, then return "No such itinerary {@code itineraryId}\n".
	 * If the user already has a reservation on the same day as the one that they are trying to book now, then return
	 * "You cannot book two flights in the same day\n".
	 * For all other errors, return "Booking failed\n".
	 *
	 * And if booking succeeded, return "Booked flight(s), reservation ID: [reservationId]\n" where
	 * reservationId is a unique number in the reservation system that starts from 1 and increments by 1 each time a
	 * successful reservation is made by any user in the system.
	 */
	public String transaction_book(int itineraryId)
	 {
	try{ beginTransaction();  
		EverySingleFlight = allFEntries;
	    EveryFlightItinerary = iIndex;
	    FlightwithIndex = Findex;
		if (this.username == null) {
			rollbackTransaction();
		    return "Cannot book reservations, not logged in\n";
		} else if (itineraryId >= EverySingleFlight.size()) {
			rollbackTransaction();
		    return "No such itinerary " + itineraryId + "\n";
		} 
		int tempIndex = iIndex.get(itineraryId);
		int firstFlight = EverySingleFlight.get(itineraryId).flightid;
		int nextFlight = EverySingleFlight.get(itineraryId).flightid1;
		//write lock
		maxRID.clearParameters();
		ResultSet getMaxRid = maxRID.executeQuery();
		boolean hasMaxRid = getMaxRid.next();
		int realMax = getMaxRid.getInt("mrid");
		
		insertRID.clearParameters();
		ResultSet insertResult = insertRID.executeQuery();
		insertResult.next();
		int realMax1 = insertResult.getInt("rid2");
		
		if (checkSameDay(FlightwithIndex.get(itineraryId).dayMonth,this.username) || 
				(FlightwithIndex.get(itineraryId).dayMonth1 != -1 && 
				checkSameDay(FlightwithIndex.get(itineraryId).dayMonth1, this.username))) {
			rollbackTransaction();
			return "You cannot book two flights in the same day\n";
		}
		
		countFID1.clearParameters();
		countFID1.setInt(1, firstFlight);
		ResultSet flightset1 = countFID1.executeQuery();
		flightset1.next();
		int reservedOne = flightset1.getInt("count");
		int cap11 = checkFlightCapacity(firstFlight);
		int cap22 = nextFlight > 0 ? checkFlightCapacity(nextFlight):-2;
		if (cap11 - reserves(firstFlight) <=0 || (cap22 - reserves(nextFlight) <= 0 && cap22 != -2)) {
			//System.out.println(cap11);
			//System.out.println(cap22);
			//System.out.println(reserves(firstFlight));
			//System.out.println(reserves(nextFlight));
			rollbackTransaction();
			return "Booking failed\n";
		}
		int flightdayMonth = FlightwithIndex.get(itineraryId).dayMonth;
		//System.out.println(flightdayMonth);
		updateFlightDates(this.username, FlightwithIndex.get(itineraryId).dayMonth);
		if (FlightwithIndex.get(itineraryId).dayMonth1 != -1) {
			updateFlightDates(this.username, FlightwithIndex.get(itineraryId).dayMonth1);
		}
		
		makeReservationStatement.clearParameters();
		makeReservationStatement.setInt(1,0);
		makeReservationStatement.setInt(2,realMax1);
		//System.out.println("realMax1 " + realMax1);
		//System.out.println(1);
		makeReservationStatement.setString(3, this.username);
		//System.out.println(2);
		makeReservationStatement.setInt(4, firstFlight);
		//System.out.println(3);
		if (nextFlight > 0) {
			makeReservationStatement.setInt(5, nextFlight);
		} else {
			makeReservationStatement.setInt(5, -1);
		}
		//System.out.println(4);
		makeReservationStatement.setInt(6, FlightwithIndex.get(itineraryId).price);
		//System.out.println(5);
		makeReservationStatement.setInt(7, 0);
		makeReservationStatement.executeUpdate();
		
		//System.out.println(6);
		//System.out.println(7);
		
		
		commitTransaction();
		return "Booked flight(s), reservation ID: " + (realMax + 1) +"\n";
		
	
		} catch (SQLException e) {
			e.printStackTrace();
			try {
				rollbackTransaction();
			} catch(Exception e1) {
				e1.printStackTrace();
			}
			return "Booking failed\n";
		}
	}
	
	public int reserves(int flight) throws SQLException {
		numReserv.clearParameters();
		numReserv.setInt(1, flight);
		numReserv.setInt(2,flight);
		ResultSet results = numReserv.executeQuery();
		results.next();
		return results.getInt("rcount");
	}
	
	public boolean checkSameDay(int day, String username) throws SQLException {
		
		checkSameDates.clearParameters();
		checkSameDates.setInt(1, day);
		checkSameDates.setString(2, username);
		ResultSet myResult= checkSameDates.executeQuery();
		boolean hasNext1 = myResult.next();
		
		//System.out.println(hasNext1);
		return hasNext1 ;
	}
	
	public void updateFlightDates(String username, int date) throws SQLException {
		insertNewDate.clearParameters();
		insertNewDate.setString(1, username);
		insertNewDate.setInt(2, date);
		insertNewDate.executeUpdate();
	}
	/**
	 * Implements the pay function.
	 *
	 * @param reservationId the reservation to pay for.
	 *
	 * @return If no user has logged in, then return "Cannot pay, not logged in\n"
	 * If the reservation is not found / not under the logged in user's name, then return
	 * "Cannot find unpaid reservation [reservationId] under user: [username]\n"
	 * If the user does not have enough money in their account, then return
	 * "User has only [balance] in account but itinerary costs [cost]\n"
	 * For all other errors, return "Failed to pay for reservation [reservationId]\n"
	 *
	 * If successful, return "Paid reservation: [reservationId] remaining balance: [balance]\n"
	 * where [balance] is the remaining balance in the user's account.
	 */
	public String transaction_pay (int reservationId)
	{	try {beginTransaction();
		if (this.username == null) {
			rollbackTransaction();
			return "Cannot pay, not logged in\n";
		} 
		   if (!seeIfUserReserveFlight(this.username, reservationId)) {
			   rollbackTransaction();
			return "Cannot find unpaid reservation " + reservationId + " under user: " + this.username + "\n";
		   } 
		   int balance = userBalance(this.username);
		   int actualPrice = getPrice(reservationId);
		   if (balance < actualPrice) {
			   rollbackTransaction();
			   return "User has only " + balance +" in account but itinerary costs " + actualPrice+ "\n";
		   } else {  
			   int newBalance = balance - actualPrice;
			   updateBalance.clearParameters();
			   updateBalance.setInt(1, newBalance);
			   updateBalance.setString(2, this.username);
			   updateBalance.executeUpdate();
			   changePaid.clearParameters();
			   changePaid.setInt(1, reservationId);
			   changePaid.executeUpdate();
			   commitTransaction();
			   return "Paid reservation: " + reservationId + " remaining balance: " + newBalance + "\n";
		   }
		} catch(SQLException e) {
			e.printStackTrace();
			try {
				rollbackTransaction();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			return "Failed to pay for reservation " + reservationId + "\n";
		}
		
	}

	public int userBalance(String username) throws SQLException{
		getBalance.clearParameters();
		getBalance.setString(1, username);
		ResultSet result = getBalance.executeQuery();
		boolean hasResult = result.next();
		return result.getInt("bal");
	}
	
	public int getPrice(int rid) throws SQLException {
		getFlightPrice.clearParameters();
		getFlightPrice.setInt(1, rid);
		ResultSet result = getFlightPrice.executeQuery();
		boolean hasResult = result.next();
		return result.getInt("rprice");
	}
	
	public boolean seeIfUserReserveFlight(String username, int rid) throws SQLException {
		checkSpecReserv.clearParameters();
		checkSpecReserv.setInt(1, rid);
		checkSpecReserv.setString(2, username);
		ResultSet result = checkSpecReserv.executeQuery();
		boolean ridExisits = result.next();
		return ridExisits;
	}
	
	/**
	 * Implements the reservations function.
	 *
	 * @return If no user has logged in, then return "Cannot view reservations, not logged in\n"
	 * If the user has no reservations, then return "No reservations found\n"
	 * For all other errors, return "Failed to retrieve reservations\n"
	 *
	 * Otherwise return the reservations in the following format:
	 *
	 * Reservation [reservation ID] paid: [true or false]:\n"
	 * [flight 1 under the reservation]
	 * [flight 2 under the reservation]
	 * Reservation [reservation ID] paid: [true or false]:\n"
	 * [flight 1 under the reservation]
	 * [flight 2 under the reservation]
	 * ...
	 *
	 * Each flight should be printed using the same format as in the {@code Flight} class.
	 *
	 * @see Flight#toString()
	 */
	public String transaction_reservations()
	{
		if (this.username == null) {
			return "Cannot view reservations, not logged in\n";
		} try { 
			beginTransaction();
			if (!checkIfUserHasReserv(this.username)) {
				rollbackTransaction();
				return "No reservations found\n";
			}
			checkEveryReserve.clearParameters();
			checkEveryReserve.setString(1, this.username);
			
			getFlightIDs.clearParameters();
			getFlightIDs.setString(1, this.username);
			ResultSet fidResult = getFlightIDs.executeQuery();
			String reservationResults = "";
			while(fidResult.next()) {
				int rrid = fidResult.getInt("rrid");
				int fid = fidResult.getInt("fid1");
				int fid1 = fidResult.getInt("fid2");
				boolean paidOrNot = fidResult.getInt("paid") == 1;
				if (paidOrNot) {
					reservationResults += "Reservation " + rrid + " paid: true:\n";
				} else {
					reservationResults += "Reservation " + rrid + " paid: false:\n";
				}
				getFlightInfo.clearParameters();
				getFlightInfo.setInt(1, fid);
				ResultSet flightResult = getFlightInfo.executeQuery();
				flightResult.next();
				int fid11 = flightResult.getInt("fid");
				int dayMonth11 = flightResult.getInt("daymonth");
				String carrier = flightResult.getString("carrier"); 
				int flight_num11 = flightResult.getInt("flightnum");
				String firstCity = flightResult.getString("ocity");
				String lastCity = flightResult.getString("destcity");
				int realTime11 = flightResult.getInt("time");
				int capacity11 = flightResult.getInt("capacity");
				int price11 = flightResult.getInt("price");
				reservationResults += "ID: " + fid11 + " Day: " + dayMonth11 + " Carrier: " + carrier +
			              " Number: " + flight_num11 + " Origin: " + firstCity + " Dest: " + lastCity + " Duration: " + realTime11 +
			              " Capacity: " + capacity11 + " Price: " + price11 + "\n";
				if (fid1 > 0) {
					getFlightInfo.clearParameters();
					getFlightInfo.setInt(1, fid1);
					ResultSet flightResult1 = getFlightInfo.executeQuery();
					flightResult1.next();
					int fid22 = flightResult1.getInt("fid");
					int dayMonth22 = flightResult1.getInt("daymonth");
					String carrier1 = flightResult1.getString("carrier"); 
					int flight_num22 = flightResult1.getInt("flightnum");
					String firstCity1 = flightResult1.getString("ocity");
					String lastCity1 = flightResult1.getString("destcity");
					int realTime22 = flightResult1.getInt("time");
					int capacity22 = flightResult1.getInt("capacity");
					int price22 = flightResult1.getInt("price");
					reservationResults += "ID: " + fid22 + " Day: " + dayMonth22 + " Carrier: " + carrier1 +
				              " Number: " + flight_num22 + " Origin: " + firstCity1 + " Dest: " + lastCity1 + " Duration: " + realTime22 +
				              " Capacity: " + capacity22 + " Price: " + price22 + "\n";
				}
			}
			commitTransaction();
			return reservationResults;
			
		} catch(SQLException e) {
			e.printStackTrace();
			try {
				rollbackTransaction();
			} catch(Exception e1) {
				e1.printStackTrace();
			}
			return "Failed to retrieve reservations\n";
		}
		
	}

	public boolean checkIfUserHasReserv(String username) throws SQLException {
		checkEveryReserve.clearParameters();
		checkEveryReserve.setString(1, username);
		ResultSet result = checkEveryReserve.executeQuery();
		boolean exists = result.next();
		return exists;
	}
	/**
	 * Implements the cancel operation.
	 *
	 * @param reservationId the reservation ID to cancel
	 *
	 * @return If no user has logged in, then return "Cannot cancel reservations, not logged in\n"
	 * For all other errors, return "Failed to cancel reservation [reservationId]"
	 *
	 * If successful, return "Canceled reservation [reservationId]"
	 *
	 * Even though a reservation has been canceled, its ID should not be reused by the system.
	 */
	public String transaction_cancel(int reservationId)
	{	try {
		beginTransaction();
		if (this.username == null) {
			rollbackTransaction();
		return "Cannot cancel reservations, not logged in\n";
		} else if (!reserveExists(this.username, reservationId)) {
			rollbackTransaction();
			return "Failed to cancel reservation " + reservationId +"\n";
		}
		cancelReserve.clearParameters();
		cancelReserve.setInt(1, reservationId);
		cancelReserve.executeUpdate();
		userPaid.clearParameters();
		userPaid.setInt(1, reservationId);
		ResultSet payResult1 = userPaid.executeQuery();
		payResult1.next();
		int payOrNot = payResult1.getInt("rpaid");
		if (payOrNot == 1) {
			int num = userBalance(this.username);
			int refund = getPrice(reservationId);
			int updatedAmount = num + refund;
			updateBalance.clearParameters();
			updateBalance.setInt(1, updatedAmount);
			updateBalance.setString(2, this.username);
			updateBalance.executeUpdate();
		}
			commitTransaction();
			return "Canceled reservation " + reservationId + "\n";
		} catch(SQLException e) {
		// only implement this if you are interested in earning extra credit for the HW!
			e.printStackTrace();
			try {
				rollbackTransaction();
			} catch(Exception e1) {
				e1.printStackTrace();
				
			}
			return "Failed to cancel reservation " + reservationId + "\n";
		}
	}
	
	public boolean reserveExists(String username, int rid) throws SQLException {
		checkForCancel.clearParameters();
		checkForCancel.setString(1, username);
		checkForCancel.setInt(2, rid);
		ResultSet results = checkForCancel.executeQuery();
		boolean exists = results.next();
		return exists;
	}

	/* some utility functions below */

	public void beginTransaction() throws SQLException
	{
		conn.setAutoCommit(false);
		beginTransactionStatement.executeUpdate();
	}

	public void commitTransaction() throws SQLException
	{
		commitTransactionStatement.executeUpdate();
		conn.setAutoCommit(true);
	}

	public void rollbackTransaction() 
	{	try{
		rollbackTransactionStatement.executeUpdate();
		conn.setAutoCommit(true);
		} catch(SQLException e) {
			e.printStackTrace();
		}
	} 
}
