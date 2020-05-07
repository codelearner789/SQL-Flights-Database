import java.io.FileInputStream;
import java.sql.*;
import java.util.Properties;
import java.util.*;
/**
 * Runs queries against a back-end database.
 * This class is responsible for searching for flights.
 */
public class QuerySearchOnly
{
  // `dbconn.properties` config file
  private String configFilename;

  // DB Connection
  protected Connection conn;

  protected ArrayList<Itinerary> allFEntries = new ArrayList<Itinerary>();
  protected ArrayList<Integer> iIndex = new ArrayList<Integer>();
  protected ArrayList<ItineraryIndex> Findex = new ArrayList<ItineraryIndex>();
  // Canned queries
  private static final String CHECK_FLIGHT_CAPACITY = "SELECT capacity FROM Flights WHERE fid = ?";
  protected PreparedStatement checkFlightCapacityStatement;

  private static final String FLIGHT_SEARCH = "SELECT TOP ( ? ) fid, day_of_month,carrier_id,flight_num,origin_city,dest_city,actual_time,capacity,price "
          + "FROM Flights "
          + "WHERE origin_city =   ?   AND dest_city =  ?  AND day_of_month = ? "
          + "AND canceled = 0 "
          + "ORDER BY actual_time, fid ASC";
  protected PreparedStatement searchFlightStatement;

  private static final String search_indirect = "SELECT TOP ( ? ) f1.fid as fid1, f2.fid as fid2, f1.day_of_month as month1, f2.day_of_month as month2, f1.carrier_id as car1, f2.carrier_id as car2, f1.flight_num as fnum1, f2.flight_num as fnum2, f1.origin_city as oc1, f1.dest_city as dc1, f2.origin_city as oc2,f2.dest_city as dc2, f1.actual_time as t1, f2.actual_time as t2, f1.capacity as cap1, f2.capacity as cap2, f1.price as pr1,f2.price as pr2 "
          + "FROM Flights f1, Flights f2 "
          + "WHERE f1.origin_city =   ?   AND f2.dest_city =  ?  AND f1.day_of_month = ? "
          + "AND f1.dest_city = f2.origin_city AND f2.day_of_month = f1.day_of_month AND f1.canceled = 0 AND f2.canceled = 0 "
          + "ORDER BY f1.actual_time+f2.actual_time ASC, f1.fid ASC, f2.fid ASC";

  protected PreparedStatement searchIndirectFlightStatement;
  class Flight
  {
    public int fid;
    public int dayOfMonth;
    public String carrierId;
    public String flightNum;
    public String originCity;
    public String destCity;
    public int time;
    public int capacity;
    public int price;

    @Override
    public String toString()
    {
      return "ID: " + fid + " Day: " + dayOfMonth + " Carrier: " + carrierId +
              " Number: " + flightNum + " Origin: " + originCity + " Dest: " + destCity + " Duration: " + time +
              " Capacity: " + capacity + " Price: " + price;
    }
  }

  public QuerySearchOnly(String configFilename)
  {
    this.configFilename = configFilename;
  }

  /** Open a connection to SQL Server in Microsoft Azure.  */
  public void openConnection() throws Exception
  {
    Properties configProps = new Properties();
    configProps.load(new FileInputStream(configFilename));

    String jSQLDriver = configProps.getProperty("flightservice.jdbc_driver");
    String jSQLUrl = configProps.getProperty("flightservice.url");
    String jSQLUser = configProps.getProperty("flightservice.sqlazure_username");
    String jSQLPassword = configProps.getProperty("flightservice.sqlazure_password");

    /* load jdbc drivers */
    Class.forName(jSQLDriver).newInstance();

    /* open connections to the flights database */
    conn = DriverManager.getConnection(jSQLUrl, // database
            jSQLUser, // user
            jSQLPassword); // password

    conn.setAutoCommit(true); //by default automatically commit after each statement
    /* In the full Query class, you will also want to appropriately set the transaction's isolation level:
          
       See Connection class's JavaDoc for details.
    */
    //conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
  }

  public void closeConnection() throws Exception
  {
    conn.close();
  }

  /**
   * prepare all the SQL statements in this method.
   * "preparing" a statement is almost like compiling it.
   * Note that the parameters (with ?) are still not filled in
   */
  public void prepareStatements() throws Exception
  {
    checkFlightCapacityStatement = conn.prepareStatement(CHECK_FLIGHT_CAPACITY);
    searchFlightStatement = conn.prepareStatement(FLIGHT_SEARCH);
    searchIndirectFlightStatement = conn.prepareStatement(search_indirect);
    /* add here more prepare statements for all the other queries you need */
    /* . . . . . . */
  }



  /**
   * Implement the search function.
   *
   * Searches for flights from the given origin city to the given destination
   * city, on the given day of the month. If {@code directFlight} is true, it only
   * searches for direct flights, otherwise it searches for direct flights
   * and flights with two "hops." Only searches for up to the number of
   * itineraries given by {@code numberOfItineraries}.
   *
   * The results are sorted based on total flight time.
   *
   * @param originCity
   * @param destinationCity
   * @param directFlight if true, then only search for direct flights, otherwise include indirect flights as well
   * @param dayOfMonth
   * @param numberOfItineraries number of itineraries to return
   *
   * @return If no itineraries were found, return "No flights match your selection\n".
   * If an error occurs, then return "Failed to search\n".
   *
   * Otherwise, the sorted itineraries printed in the following format:
   *
   * Itinerary [itinerary number]: [number of flights] flight(s), [total flight time] minutes\n
   * [first flight in itinerary]\n
   * ...
   * [last flight in itinerary]\n
   *
   * Each flight should be printed using the same format as in the {@code Flight} class. Itinerary numbers
   * in each search should always start from 0 and increase by 1.
   *
   * @see Flight#toString()
   */
  public String transaction_search(String originCity, String destinationCity, boolean directFlight, int dayOfMonth,
                                   int numberOfItineraries)
  {
    String searchResult = "";
    List<Itinerary> allEntries = new ArrayList<Itinerary>();
    // Please implement your own (safe) version that uses prepared statements rather than string concatenation.
    // You may use the `Flight` class (defined above).
    try {
      allFEntries = new ArrayList<Itinerary>();
      iIndex = new ArrayList<Integer>();
      Findex = new ArrayList<ItineraryIndex>();
      searchFlightStatement.clearParameters();
      searchFlightStatement.setInt(1, numberOfItineraries);
      searchFlightStatement.setString(2, originCity);
      searchFlightStatement.setString(3, destinationCity);
      searchFlightStatement.setInt(4, dayOfMonth);
      ResultSet results = searchFlightStatement.executeQuery();
      int countIt = 0;
      while(results.next() && countIt < numberOfItineraries) {
        int fid_result = results.getInt("fid");
        int day_of_Month_result = results.getInt("day_of_month");
        String result_carrierId = results.getString("carrier_id");
        String result_flightNum = results.getString("flight_num");
        String result_originCity = results.getString("origin_city");
        String result_destCity = results.getString("dest_city");
        int result_time = results.getInt("actual_time");
        int result_capacity = results.getInt("capacity");
        int result_price = results.getInt("price");
        String sResult = ": 1 flight(s), " + result_time + " minutes\nID: " + fid_result + " Day: " + day_of_Month_result + " Carrier: "+ result_carrierId + " Number: " + result_flightNum + " Origin: " +result_originCity+
        " Dest: " + result_destCity + " Duration: " + result_time + " Capacity: " + result_capacity + " Price: " + result_price+ "\n";
        Itinerary oneEntry = new Itinerary(sResult, result_time, fid_result, -1, day_of_Month_result, -1, result_capacity, -1, result_price);
        allEntries.add(oneEntry);
        countIt++;
      }
      if (!directFlight) {
      searchIndirectFlightStatement.clearParameters();
      searchIndirectFlightStatement.setInt(1, numberOfItineraries);
      searchIndirectFlightStatement.setString(2, originCity);
      searchIndirectFlightStatement.setString(3, destinationCity);
      searchIndirectFlightStatement.setInt(4, dayOfMonth);
      ResultSet results2 = searchIndirectFlightStatement.executeQuery();

      while(results2.next() && countIt < numberOfItineraries) {
        int fid_result1 = results2.getInt("fid1");
        int day_of_Month_result1 = results2.getInt("month1");
        String result_carrierId1 = results2.getString("car1");
        String result_flightNum1 = results2.getString("fnum1");
        String result_originCity1 = results2.getString("oc1");
        String result_destCity1 = results2.getString("dc1");
        int result_time1 = results2.getInt("t1");
        int result_capacity1 = results2.getInt("cap1");
        int result_price1 = results2.getInt("pr1");

        int fid_result2 = results2.getInt("fid2");
        int day_of_Month_result2 = results2.getInt("month2");
        String result_carrierId2 = results2.getString("car2");
        String result_flightNum2 = results2.getString("fnum2");
        String result_originCity2 = results2.getString("oc2");
        String result_destCity2 = results2.getString("dc2");
        int result_time2 = results2.getInt("t2");
        int result_capacity2 = results2.getInt("cap2");
        int result_price2 = results2.getInt("pr2");
        String sResult1 = ": 2 flight(s), " + (result_time1+result_time2) + " minutes\nID: "+ fid_result1
        + " Day: " + day_of_Month_result1 + " Carrier: "+ result_carrierId1 + " Number: " + result_flightNum1 + " Origin: " +result_originCity1+
                        " Dest: " + result_destCity1 + " Duration: " + result_time1 + " Capacity: " + result_capacity1 + " Price: " + result_price1+ "\n"
                        + "ID: "+ fid_result2 + " Day: " + day_of_Month_result2 + " Carrier: "+ result_carrierId2 + " Number: " + result_flightNum2
                        + " Origin: " +result_originCity2+ " Dest: " + result_destCity2 + " Duration: "
                        + result_time2 + " Capacity: " + result_capacity2 + " Price: " + result_price2 + "\n";
        countIt++;
        Itinerary entry2 = new Itinerary(sResult1, (result_time1 + result_time2), fid_result1,fid_result2, day_of_Month_result1, 
        		day_of_Month_result2, result_capacity1, result_capacity2, (result_price1 + result_price2));
        allEntries.add(entry2);
        
      }
    }
    Collections.sort(allEntries);
    for (int i = 0; i < countIt; i++) {
      searchResult += "Itinerary " + i + allEntries.get(i).fEntry1;
      String result23 = "Itinerary " + i + allEntries.get(i).fEntry1;
      Itinerary finalEntry = new Itinerary(result23, allEntries.get(i).ftime1, allEntries.get(i).flightid, 
    		  allEntries.get(i).flightid1, allEntries.get(i).dayMonth,allEntries.get(i).dayMonth1,allEntries.get(i).cap, allEntries.get(i).cap1, 
    		  allEntries.get(i).price);
      allFEntries.add(finalEntry);
      ItineraryIndex finalEntry1 = new ItineraryIndex(result23, allEntries.get(i).ftime1, allEntries.get(i).flightid, 
    		  allEntries.get(i).flightid1, i, allEntries.get(i).dayMonth, allEntries.get(i).dayMonth1, allEntries.get(i).cap,allEntries.get(i).cap1, 
    		  allEntries.get(i).price);
      iIndex.add(i);
      Findex.add(finalEntry1);
    }
  } catch (SQLException e) {  e.printStackTrace(); }
  if(searchResult.length() == 0) {
    return "No flights match your selection\n";
  }
    return searchResult;
    // return transaction_search_unsafe(originCity, destinationCity, directFlight, dayOfMonth, numberOfItineraries);
  }

   class ItineraryIndex {
	  public String fEntry1;
	  public int ftime1;
	  public int flightid;
	  public int flightid1;
	  public int index;
	  public int dayMonth;
	  public int dayMonth1;
	  public int cap;
	  public int cap1;
	  public int price;

	  public ItineraryIndex(String fEntry1, int ftime1, int flightid, int flightid1, 
			  int index, int dayMonth, int dayMonth1, int cap, int cap1, int price) {
	     this.fEntry1 = fEntry1;
	     this.ftime1 = ftime1;
	     this.flightid = flightid;
	     this.flightid1 = flightid1;
	     this.index = index;
	     this.dayMonth = dayMonth;
	     this.dayMonth1 = dayMonth1;
	     this.cap = cap;
	     this.cap1 = cap1;
	     this.price = price;
	  }
  }
  public class Itinerary implements Comparable<Itinerary>  {
    public String fEntry1;
    public int ftime1;
    public int flightid;
    public int flightid1;
    public int dayMonth;
    public int dayMonth1;
    public int cap;
    public int cap1;
    public int price;

    public Itinerary(String fEntry1, int ftime1, int flightid, int flightid1, 
    		int dayMonth, int dayMonth1, int cap, int cap1, int price) {
      this.fEntry1 = fEntry1;
      this.ftime1 = ftime1;
      this.flightid = flightid;
      this.flightid1 = flightid1;
      this.dayMonth = dayMonth;
      this.dayMonth1 = dayMonth;
      this.cap = cap;
      this.cap1 = cap1;
      this.price = price;
    }

    public int compareTo(Itinerary other) {
      if (this.ftime1 != other.ftime1) {
        return this.ftime1 - other.ftime1;
      }else {
        return this.flightid - other.flightid;
      }
    }
  }
  /**
   * Same as {@code transaction_search} except that it only performs single hop search and
   * do it in an unsafe manner.
   *
   * @param originCity
   * @param destinationCity
   * @param directFlight
   * @param dayOfMonth
   * @param numberOfItineraries
   *
   * @return The search results. Note that this implementation *does not conform* to the format required by
   * {@code transaction_search}.
   */
  private String transaction_search_unsafe(String originCity, String destinationCity, boolean directFlight,
                                          int dayOfMonth, int numberOfItineraries)
  {
    StringBuffer sb = new StringBuffer();

    try
    {
      // one hop itineraries
      String unsafeSearchSQL =
              "SELECT TOP (" + numberOfItineraries + ") day_of_month,carrier_id,flight_num,origin_city,dest_city,actual_time,capacity,price "
                      + "FROM Flights "
                      + "WHERE origin_city = \'" + originCity + "\' AND dest_city = \'" + destinationCity + "\' AND day_of_month =  " + dayOfMonth + " "
                      + "ORDER BY actual_time ASC";

      Statement searchStatement = conn.createStatement();
      ResultSet oneHopResults = searchStatement.executeQuery(unsafeSearchSQL);

      while (oneHopResults.next())
      {
        int result_dayOfMonth = oneHopResults.getInt("day_of_month");
        String result_carrierId = oneHopResults.getString("carrier_id");
        String result_flightNum = oneHopResults.getString("flight_num");
        String result_originCity = oneHopResults.getString("origin_city");
        String result_destCity = oneHopResults.getString("dest_city");
        int result_time = oneHopResults.getInt("actual_time");
        int result_capacity = oneHopResults.getInt("capacity");
        int result_price = oneHopResults.getInt("price");

        sb.append("Day: ").append(result_dayOfMonth)
                .append(" Carrier: ").append(result_carrierId)
                .append(" Number: ").append(result_flightNum)
                .append(" Origin: ").append(result_originCity)
                .append(" Destination: ").append(result_destCity)
                .append(" Duration: ").append(result_time)
                .append(" Capacity: ").append(result_capacity)
                .append(" Price: ").append(result_price)
                .append('\n');
      }
      oneHopResults.close();
    } catch (SQLException e) { e.printStackTrace(); }

    return sb.toString();
  }

  /**
   * Shows an example of using PreparedStatements after setting arguments.
   * You don't need to use this method if you don't want to.
   */
  protected int checkFlightCapacity(int fid) throws SQLException
  {
    checkFlightCapacityStatement.clearParameters();
    checkFlightCapacityStatement.setInt(1, fid);
    ResultSet results = checkFlightCapacityStatement.executeQuery();
    results.next();
    int capacity = results.getInt("capacity");
    results.close();

    return capacity;
  }
}
