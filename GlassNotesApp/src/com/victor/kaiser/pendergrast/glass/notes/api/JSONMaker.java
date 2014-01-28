
public class JSONMaker {
	
	public static String makeJSON(String notes){
		String json = "{ \"notes\" : \"" + notes + "\"," +
				"\"lastModified\" : \"" + System.currentTimeMillis() + "\" } ";
	}

}
