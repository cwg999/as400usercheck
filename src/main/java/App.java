
// https://www.ibm.com/support/knowledgecenter/en/ssw_ibm_i_71/rzahh/javadoc/overview-summary.html
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.QueuedMessage;
import com.ibm.as400.access.User;
import com.ibm.as400.access.UserGroup;
import com.ibm.as400.access.UserList;
import com.ibm.as400.access.HistoryLog;
import com.ibm.as400.access.ObjectDescription;
public class App {
  public static void main(String[] args) {
    AS400 as400 = new AS400("POWER7", "USERNAME", "PASSWORD");
    UserList users = new UserList(as400,UserList.ALL,UserGroup.NONE);
    try{
      // Get history Log
      HistoryLog hist = new HistoryLog(as400);
      String[] ids = new String[2];
      ids[0] ="CPF1124"; // Sign-on
      ids[1] ="CPF1164"; // Sign-off
      hist.setMessageIDs(ids);
      Date startDate = new SimpleDateFormat( "yyyyMMdd" ).parse( "20171226"); // Should be later than endDate
      Date endDate = new SimpleDateFormat( "yyyyMMdd" ).parse( "20171225"); // Should be earlier than startDate
      hist.setStartingDate(startDate);
      hist.setEndingDate(endDate);

      Enumeration<QueuedMessage> histEnum = hist.getMessages();
      Map<String,ArrayList<String>> userLogin = new HashMap<String,ArrayList<String>>();
      Map<String,ArrayList<String>> userLogout = new HashMap<String,ArrayList<String>>();
      while(histEnum.hasMoreElements()){
        QueuedMessage msg = histEnum.nextElement();
        String id = msg.getID();
        String user = msg.getCurrentUser();
        Calendar createDate = msg.getDate();
        String createDateStr = createDate == null 
          ? "" 
          : " "+(new SimpleDateFormat( "yyyyMMdd HHmmss" ).format(createDate.getTime()));
        if(id!=null){
          if(id.equals("CPF1124")){
            // System.out.println("Login:"+user+createDateStr);
            if(userLogin.get(user)==null){
              ArrayList<String> lst = new ArrayList<String>();
              lst.add(createDateStr);
              userLogin.put(user,lst);
            }else{
              ArrayList<String> lst = userLogin.get(user);
              lst.add(createDateStr);
              userLogin.replace(user,lst);
            }
            
            
          }else if(id.equals("CPF1164")){
            // System.out.println("Logout:"+user+createDateStr);
            if(userLogout.get(user)==null){
              ArrayList<String> lst = new ArrayList<String>();
              lst.add(createDateStr);
              userLogout.put(user,lst);
            }else{
              ArrayList<String> lst = userLogout.get(user);
              lst.add(createDateStr);
              userLogout.replace(user,lst);
            }
          }
        }

      }
      hist.close();

      Enumeration<User> e = users.getUsers();
      while(e.hasMoreElements()){
        User user = e.nextElement();
        user.getPreviousSignedOnDate();
        String username = user.getName();
        ObjectDescription userDescription = new ObjectDescription(as400, "QSYS", username, "USRPRF");
        String createDate = userDescription.getValueAsString(ObjectDescription.CREATION_DATE);
        ArrayList<String> logins = userLogin.get(username);
        ArrayList<String> logouts = userLogout.get(username);
        System.out.println(username);
        System.out.println("Creation Date:"+createDate);
        System.out.println("Logins:\r\n"+String.join(",\r\n",logins==null ? new ArrayList<String>():logins));
        System.out.println("Logouts:\r\n"+String.join(",\r\n",logouts==null ? new ArrayList<String>():logouts));
      }
    }catch(Exception e){
      System.out.println(e);
    }
    
    as400.disconnectAllServices();
    System.exit(0);
  }
}