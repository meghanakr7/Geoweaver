package com.gw.local;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import javax.websocket.Session;

import com.gw.jpa.ExecutionStatus;
import com.gw.jpa.GWProcess;
import com.gw.jpa.History;
import com.gw.server.CommandServlet;
import com.gw.tools.HistoryTool;
import com.gw.tools.HostTool;
import com.gw.tools.ProcessTool;
import com.gw.utils.BaseTool;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * 
 * This is for Linux/Mac
 * 
 * @author JensenSun
 *
 */
@Service
@Scope("prototype")
public class LocalSessionNixImpl implements LocalSession {

	Logger log  = Logger.getLogger(this.getClass());
	
	@Autowired
	ProcessTool pt;
	
	@Autowired
	BaseTool bt;

	@Autowired
	HostTool ht;
	
	@Autowired
	HistoryTool history_tool;
	
	private boolean			 isTerminal;
	
	private BufferedReader   input;
	    
//    private OutputStream     output;

	@Autowired
    private LocalSessionOutput sender;
    
    private Thread           thread;
    
	/**
	 * Http Session Id
	 */
    private String           token;
    
    private History          history;
//    
//    private HistoryTool      history_tool = new HistoryTool();
    
    private String           tempfile;
    
    @Value("${geoweaver.workspace}")
    private String           workspace;
    
    
    private String           workspace_folder_path;

	private Process          process;
    
    
    public LocalSessionNixImpl() {
    	
    	//this is for spring
    	
    }
    
    /**
	 * Initialize history object when process execution starts
	 * @param script
	 * @param processid
	 * @param isjoin
	 * @param token
	 */
	public void initHistory(String history_id, String script, String processid, boolean isjoin, String token) {
		
		this.token = token;
		
		this.isTerminal = isjoin;
		
		history = history_tool.initProcessHistory(history_id, processid, script);
		
	}
	
	/**
	 * If the process ends with error
	 * @param token
	 * @param message
	 */
	public void endWithError(String token, String message) {
		
		try {
			
			Session wsout = CommandServlet.findSessionById(token);
			
			if(!bt.isNull(wsout) && wsout.isOpen() ) {
				
				log.info("The failed message has been sent to client");
				if(!bt.isNull(message))
					wsout.getBasicRemote().sendText(message);
				
				wsout.getBasicRemote().sendText("The process " + this.history.getHistory_id() + " is stopped.");
				
			}
			
		} catch (IOException e1) {
			
			e1.printStackTrace();
			
		}
		
		this.stop();
		
		this.history.setHistory_end_time(bt.getCurrentSQLDate());
		
		this.history.setHistory_output(message);
		
		this.history.setIndicator(ExecutionStatus.FAILED);
		
		this.history_tool.saveHistory(this.history);
		
	}
    
	@Override
	public void runBash(String history_id, String script, String processid, boolean isjoin, String token) {
		
//		this.history = history_tool.initProcessHistory(history, processid, script);
		
		this.initHistory(history_id, script, processid, isjoin, token);
    	
    	try {
    		
    		log.info("starting command");
    		
    		workspace_folder_path = bt.normalizedPath(workspace);
    		
    		tempfile = workspace_folder_path + "/gw-" + token + "-" + history.getHistory_id() + ".sh";

    		// script += "\necho \"==== Geoweaver Bash Output Finished ====\"";
    		
    		bt.writeString2File(script, tempfile);
    		
    		Runtime.getRuntime().exec(new String[] {"chmod", "+x", tempfile}).waitFor();

			bt.sleep(1000);
    		
    		ProcessBuilder builder = new ProcessBuilder();
    		
    		builder.directory(new File(workspace_folder_path));
    		
    		builder.command(tempfile);
    		
    		builder.redirectErrorStream(true);
    		
    		Process process = builder.start();
    		
    		InputStream stdout = process.getInputStream ();
    		
    		input = new BufferedReader(new InputStreamReader(stdout));
            
    		sender.init(input, token, history_id);
    		
    		thread = new Thread(sender);
            
            thread.setName("Local Command output thread");
            
            log.info("starting sending thread from local command");
            
            thread.start();

			if(isjoin) process.waitFor();
            
            log.info("returning to the client..");
    		
		} catch (Exception e) {
			
			e.printStackTrace();
			
			this.endWithError(token, e.getLocalizedMessage());
			
		}
    	
		
	}

	@Override
	public void runJupyter(String history_id, String notebookjson, String processid, 
			boolean isjoin, String bin, String env, String basedir,
			String token) {
		
		this.initHistory(history_id, notebookjson, processid, isjoin, token);
		
    	try {
    		
    		log.info("starting command");
    		
    		workspace_folder_path = bt.normalizedPath(workspace);
    		
    		tempfile = workspace_folder_path + "/gw-" + token + "-" + history.getHistory_id() + ".ipynb";

    		bt.writeString2File(notebookjson, tempfile);
    		
    		// Get a list of all environment variables
            final Map<String, String> envMap = new HashMap<String, String>(System.getenv());
            
    		if(bt.isNull(bin)||"default".equals(bin)) {

//    			cmdline += "python python-" + history_id + ".py;";
    			
//    			cmdline += "python " + filename + "; ";
    			
    		}else {
    			
//    			cmdline += "conda init; ";
    			
//    			cmdline += "source activate " + env + "; "; //for demo only
    			
    			Runtime.getRuntime().exec(new String[] {"source", "activate", env}).waitFor();
    			
//    			cmdline += bin + " " + filename + "; ";
    			
    		}
    		
    		ProcessBuilder builder = new ProcessBuilder();
    		
    		builder.directory(new File(workspace_folder_path));
    		
    		builder.command(new String[] {"jupyter", "nbconvert", "--to", "notebook", "--execute", tempfile} );
    		
    		builder.redirectErrorStream(true);
    		
    		Process process = builder.start();
    		
    		InputStream stdout = process.getInputStream ();
    		
            log.info("Local session established");
            
            input = new BufferedReader(new InputStreamReader(stdout));
            
//            sender = new SSHSessionOutput(input, token);
            sender.init(input, token, history_id);
            
            //moved here on 10/29/2018
            //all SSH sessions must have a output thread
            
            thread = new Thread(sender);
            
            thread.setName("SSH Command output thread");
            
            log.info("starting sending thread");
            
            thread.start();
            
            log.info("returning to the client..");
            
			if(isjoin) process.waitFor();

            // if(isjoin) thread.join(7*24*60*60*1000); //longest waiting time - a week
            
		} catch (Exception e) {
			
			e.printStackTrace();
			
			this.endWithError(token, e.getLocalizedMessage());
			
		}
		
	}

	@Override
	public void runPython(String history_id, String python, String processid, boolean isjoin, String bin, String pyenv, String basedir, String token) {
		
		this.initHistory(history_id, python, processid, isjoin, token);
    	
    	try {
    		
    		log.info("save to local file: " + python);
    		
    		GWProcess pro = pt.getProcessById(processid);
    		
    		ProcessBuilder builder = new ProcessBuilder();
    		
    		workspace_folder_path = bt.normalizedPath(workspace);
    		
    		builder.directory(new File(workspace_folder_path + "/" + token));
    		
    		String pythonfilename = pro.getName();
    		
    		if(!pythonfilename.endsWith(".py")) pythonfilename += ".py";
    		
    		builder.command(new String[] {"python", pythonfilename} );
    		
    		builder.redirectErrorStream(true);
    		
    		Process process = builder.start();
    		
    		InputStream stdout = process.getInputStream ();
    		
            log.info("Local session established");
            
            input = new BufferedReader(new InputStreamReader(stdout));
    		
            sender.init(input, token, history_id);
            
            //moved here on 10/29/2018
            //all SSH sessions must have a output thread
            
            thread = new Thread(sender);
            
            thread.setName("SSH Command output thread");
            
            log.info("starting sending thread");
            
            thread.start();
            
            log.info("returning to the client..");
            
			if(isjoin) process.waitFor();
            // if(isjoin) thread.join(7*24*60*60*1000); //longest waiting time - a week
            
		} catch (Exception e) {
			
			e.printStackTrace();
			
			this.endWithError(token, e.getLocalizedMessage());
			
		}
		
	}

	@Override
	public void runMultipleBashes(String history_id, String[] script, String processid) {
		
		
		
	}

	@Override
	public void saveHistory(String logs, String status) {
		
		history.setHistory_output(logs);
		
		history.setIndicator(status);
		
		history_tool.saveHistory(history);
		
		pt.updateJupyter(history, this.token);
		
	}

	@Override
	public String getToken() {
		
		return this.token;
		
	}

	@Override
	public boolean isTerminal() {
		
		return this.isTerminal;
		
	}

	@Override
	public BufferedReader getLocalInput() {
		
		return this.input;
		
	}

	@Override
	public History getHistory() {
		
		return this.history;
		
	}

	@Override
	public void setHistory(History history) {
		
		this.history = history;
		
	}

	@Override
	public boolean stop() {
		
		return false;
	}

	@Override
	public boolean clean() {
		
		File temp = new File(tempfile);
		
		return temp.delete();
		
	}

	void readWhere() throws IOException, InterruptedException{

		ProcessBuilder builder = new ProcessBuilder();
    		
		builder.command("whereis", "python"); //bash.exe of cygwin must be in the $PATH

		builder.redirectErrorStream(true);

		process = builder.start();

		process.waitFor();

		BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String s = null;
		while ((s = in.readLine()) != null) {
			System.out.println(s);
		}
	}

	void readConda() throws IOException, InterruptedException{

		ProcessBuilder builder = new ProcessBuilder();
    		
		builder.command("whereis", "python"); //bash.exe of cygwin must be in the $PATH

		builder.redirectErrorStream(true);

		process = builder.start();

		process.waitFor();

		BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String s = null;

		while ((s = in.readLine()) != null) {
		
			System.out.println(s);
		
		}
	
	}

	@Override
	public String readPythonEnvironment(String hostid, String password) {

		String resp = null;

		try {

			this.readWhere();

			this.readConda();

			resp = ht.getEnvironments(hostid);

		} catch (Exception e) {

			e.printStackTrace();

		}

		return resp;

	}
	

}
