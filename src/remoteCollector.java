/**
 * @ClassName: remoteCollector.
 * @Project: AutoMonitorTool.
 * @Package: remotecollector.
 * @Description: The basic java client to collect remote server hardware information via SSH connection.
 * @Author: Hades.Yang 
 * @Version: V1.0
 * @Date: 2015-08-28
 * @History: 
 *    1.2015-01-04 First version of remoteCollector was written.
 */

package remotecollector;


import com.jcraft.jsch.*;  

import java.io.BufferedReader;  
import java.io.IOException;  
import java.io.InputStream;  
import java.io.InputStreamReader;  
import java.util.HashMap;  
import java.util.Map;  
  
/** 
 * @ClassName: remoteCollector
 * @Description: The java client which is used to run shell command for fetching remote server information.
 * 
 */  
public class remoteCollector 
{  
	/**
     * @FieldName: CPU_MEM_SHELL.
     * @Description: the command which is used to fetch CPU & Memory information.
     */
    private static final String CPU_MEM_SHELL = "top -b -n 1";  
    
	/**
     * @FieldName: FILES_SHELL.
     * @Description: the command which is used to fetch disk information.
     */
    private static final String FILES_SHELL = "df -hl"; 
    
	/**
     * @FieldName: COMMANDS.
     * @Description: the command array to execute via ssh.
     */
    private static final String[] COMMANDS = {CPU_MEM_SHELL, FILES_SHELL};  
    
	/**
     * @FieldName: LINE_SEPARATOR.
     * @Description: line separator which is used to separate lines.
     */
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    
	/**
     * @FieldName: session.
     * @Description: the work session of jsch.
     */
    private static Session session;  
    
	/**
     * @FieldName: CPU_Load.
     * @Description: the string which is used to record cpu load.
     *               (Format:CPU_User_Usage:0.1%)
     */
    public String CPU_Load;
    
	/**
     * @FieldName: Mem_Usage.
     * @Description: the string which is used to record memory usage.
     *               (Format:Memory_Usage:3924688KTotal.1690092KUsed.2234596KFree.181212KBuffers.)
     */
    public String Mem_Usage;
    
	/**
     * @FieldName: Disk_Status.
     * @Description: the string which is used to record disk status.
     *               (Format:Disk_Status:53GTotal.50GUsed.3GFree)
     */
    public String Disk_Status;
  
    /** 
     * @Title: connectRemoteHost.
     * @Description: Connect to specific host by using user & passwd.
     * @Param: user,String type,the user name to login the remote server.
     * @Param: passwd,String type,the password to login the remote server.
     * @Param: host,String type,the ip address of the remote server.
     * @return boolean value,which shows connected or not. 
     * @throws JSchException JSchException 
     */  
    private boolean connectRemoteHost(String user, String passwd, String host) 
    {  
        JSch jsch = new JSch();  
        
        try 
        {  
            session = jsch.getSession(user, host, 22);  
            session.setPassword(passwd);  
  
            java.util.Properties config = new java.util.Properties();  
            config.put("StrictHostKeyChecking", "no");  
            session.setConfig(config);  
  
            session.connect();  
            
        } 
        catch (JSchException e) 
        {  
            e.printStackTrace();  
            System.out.println("connectRemoteHost execute error !");  
            return false;  
        }  
        return true;  
    }  
  
   
    /** 
     * @Title: runRemoteShell.
     * @Description: Connect to specific host and run shell.
     * @Param: commands,String[],which store the command to execute.
     * @Param: user,String type,the user name to login the remote server.
     * @Param: passwd,String type,the password to login the remote server.
     * @Param: host,String type,the ip address of the remote server.
     * @return The result of the command return. 
     */
    private Map<String, String> runRemoteShell(String[] commands, String user, String passwd, String host) 
    {  
        if (!connectRemoteHost(user, passwd, host)) 
        {  
            return null;  
        }  
        
        Map<String, String> map = new HashMap<>();  
        StringBuilder stringBuffer;  
        BufferedReader reader = null;  
        Channel channel = null;  
        
        try 
        {  
            for (String command : commands) 
            {  
                stringBuffer = new StringBuilder();  
                
                //initialize the channel.
                channel = session.openChannel("exec");  
                ((ChannelExec) channel).setCommand(command);  
                channel.setInputStream(null);  
                ((ChannelExec) channel).setErrStream(System.err);  
                channel.connect();  
                InputStream in = channel.getInputStream();  
                reader = new BufferedReader(new InputStreamReader(in));  
                
                String buf;  
                while ((buf = reader.readLine()) != null) 
                {  
                    //discard the PID information. 
                    if (buf.contains("PID")) 
                    {  
                        break;  
                    }  
                    stringBuffer.append(buf.trim()).append(LINE_SEPARATOR);  
                }  
                
                //store each command with its execute result into the hash map.
                map.put(command, stringBuffer.toString());  
            }  
        }
        catch (IOException | JSchException e) 
        {  
            e.printStackTrace();  
        } 
        finally   
        {  
        	//Clear the resources.
            try 
            {  
                if (reader != null) 
                {  
                    reader.close();  
                }  
            } 
            catch (IOException e) 
            {  
                e.printStackTrace();  
            }  
            
            if (channel != null) 
            {  
                channel.disconnect();  
            }  
            session.disconnect();  
        }  
        return map;  
    }  
   
    /** 
     * @Title: runLocalShell.
     * @Description: Run shell command locally.
     * @Param: commands,String[],which store the command to execute.
     * @return The result of the command return. 
     */
    public Map<String, String> runLocalShell(String[] commands) 
    {  
        Runtime runtime = Runtime.getRuntime();  
  
        Map<String, String> map = new HashMap<>();  
        StringBuilder stringBuffer;  
        BufferedReader reader;  
        Process process; 
        
        for (String command : commands) 
        {  
            stringBuffer = new StringBuilder();  
            try 
            {  
                process = runtime.exec(command);  
                InputStream inputStream = process.getInputStream();  
                reader = new BufferedReader(new InputStreamReader(inputStream));  
                
                String buf;  
                while ((buf = reader.readLine()) != null) 
                {  
                    //discard the PID information. 
                    if (buf.contains("PID")) 
                    {  
                        break;  
                    }  
                    stringBuffer.append(buf.trim()).append(LINE_SEPARATOR);  
                }  
            } 
            catch (IOException e) 
            {  
                e.printStackTrace();  
                return null;  
            }  
            
            //store each command with its execute result into the hash map. 
            map.put(command, stringBuffer.toString());  
        }  
        return map;  
    }  
  
   
    /** 
     * @Title: disposeResultMessage.
     * @Description: Dispose the result message and return the useful information.
     * @Param: result,map of the command & command execute result.
     * @return The useful information of the command result.
     */
    private String disposeResultMessage(Map<String, String> result) 
    {  
  
        StringBuilder buffer = new StringBuilder();  
  
        for (String command : COMMANDS) 
        {	
            String commandResult = result.get(command);  
            if (null == commandResult) continue;  
  
            if (command.equals(CPU_MEM_SHELL)) 
            {  
            	//separate the result by using LINE_SEPARATOR.
                String[] strings = commandResult.split(LINE_SEPARATOR);  
                
                for (String line : strings) 
                {  
                    line = line.toUpperCase(); 
  
                    //Deal with CPU Cpu(s): 10.8%us,  0.9%sy,  0.0%ni, 87.6%id,  0.7%wa,  0.0%hi,  0.0%si,  0.0%st  
                    if (line.startsWith("CPU(S):")) 
                    {  
                        String cpuStr = "CPU_User_Usage:";  
                        try 
                        {  
                            cpuStr += line.split(":")[1].split(",")[0].replace("US", "").replace(" ","");  
                        }
                        catch (Exception e) 
                        {  
                            e.printStackTrace();  
                            cpuStr += " Process Error! \n";  
                        }  
                        buffer.append(cpuStr).append(LINE_SEPARATOR);  
  
                        this.CPU_Load = cpuStr; 
                    } 
                    else if (line.startsWith("MEM")) //Deal with Mem:  66100704k total, 65323404k used,   777300k free,    89940k buffers 
                    {  
                        String memStr = "Memory_Usage:";  
                        try 
                        {  
                            memStr += line.split(":")[1]
                            		.replace(" ", "")
                                    .replace("TOTAL", "Total.")  
                                    .replace("USED", "Used.")  
                                    .replace("FREE", "Free.")  
                                    .replace("BUFFERS", "Buffers.")
                                    .replace(",", "");
  
                        } 
                        catch (Exception e) 
                        {  
                            e.printStackTrace();  
                            memStr += " Process Error! \n";  
                            buffer.append(memStr).append(LINE_SEPARATOR);  
                            continue;  
                        }  
                        buffer.append(memStr).append(LINE_SEPARATOR);  
                        
                        this.Mem_Usage = memStr;
                    }  
                }  
            } 
            else if (command.equals(FILES_SHELL)) 
            {  
                //Deal with disk status.  
                buffer.append("Disk_Status:");  
                try 
                {  
                    buffer.append(disposeFilesSystem(commandResult)).append(LINE_SEPARATOR);  
                } 
                catch (Exception e) 
                {  
                    e.printStackTrace();  
                    buffer.append(" Process Error! \n").append(LINE_SEPARATOR);  
                }  
            }  
        }  
  
        return buffer.toString();  
    }  
  
    //处理系统磁盘状态  
  
    /** 
     * Filesystem            Size  Used Avail Use% Mounted on 
     * /dev/sda3             442G  327G   93G  78% / 
     * tmpfs                  32G     0   32G   0% /dev/shm 
     * /dev/sda1             788M   60M  689M   8% /boot 
     * /dev/md0              1.9T  483G  1.4T  26% /ezsonar 
     */ 
    /** 
     * @Title: disposeFilesSystem.
     * @Description: Dispose the result message and return the useful information.
     * @Param: result,map of the command & command execute result.
     * @return The useful information of the command result.
     */
    private String disposeFilesSystem(String commandResult) 
    {  
        String[] strings = commandResult.split(LINE_SEPARATOR);  
        String diskStr = null;
  
        // final String PATTERN_TEMPLATE = "([a-zA-Z0-9%_/]*)\\s";  
        int size = 0;  
        int used = 0;  
        for (int i = 0; i < strings.length - 1; i++) 
        {  
            if (i == 0) 
            {
            	continue;  
            }
  
            int temp = 0;  
            for (String s : strings[i].split("\\b")) 
            {  
                if (temp == 0) 
                {  
                    temp++;  
                    continue;  
                }  
                if (!s.trim().isEmpty()) 
                {  
                    if (temp == 1) 
                    {  
                        size += disposeUnit(s);  
                        temp++;  
                    } 
                    else 
                    {  
                        used += disposeUnit(s);  
                        temp = 0;  
                    }  
                }  
            }  
        }  
        
        diskStr = new StringBuilder().append(size).append("G Total. ").append(used).append("G  Used. ").append(size - used).append("G  Free \n")  
                  .toString().replace(" ", "");
        
        this.Disk_Status = diskStr;
        return diskStr;  
    }  
  

    /** 
     * @Title: disposeUnit.
     * @Description: Dispose the storage measurement "K/KB/M/T" to 'G'.
     * @Param: String which contains the storage size with measurement of "K/KB/M/T".
     * @return The storage size with measurement of 'G'.
     */
    private int disposeUnit(String s) 
    {  
        try 
        {  
            s = s.toUpperCase();  
            String lastIndex = s.substring(s.length() - 1);  
            String num = s.substring(0, s.length() - 1);  
            int parseInt = Integer.parseInt(num);  
            
            if (lastIndex.equals("G")) 
            {  
                return parseInt;  
            } 
            else if (lastIndex.equals("T")) 
            {  
                return parseInt * 1024;  
            } 
            else if (lastIndex.equals("M")) 
            {  
                return parseInt / 1024;  
            } 
            else if (lastIndex.equals("K") || lastIndex.equals("KB")) 
            {  
                return parseInt / (1024 * 1024);  
            }  
        } catch (NumberFormatException e) 
        {  
            e.printStackTrace();  
            return 0;  
        }  
        return 0;  
    }  
  
    //Class local test main function.
    public static void main(String[] args) 
    {  
    	remoteCollector rCollector = new remoteCollector();
        Map<String, String> result = rCollector.runRemoteShell(COMMANDS, "root", "sinosun", "192.168.40.74");  
        System.out.println(rCollector.disposeResultMessage(result));  
        System.out.println("=================================");
        System.out.println(rCollector.CPU_Load);
        System.out.println(rCollector.Mem_Usage);
        System.out.println(rCollector.Disk_Status);
        //runLocalShell(COMMANDS);  
    }  
}
  
