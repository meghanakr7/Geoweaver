package com.gw.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gw.database.CheckpointRepository;
import com.gw.database.HistoryRepository;
import com.gw.database.WorkflowRepository;
import com.gw.jpa.ExecutionStatus;
import com.gw.jpa.GWProcess;
import com.gw.jpa.History;
import com.gw.jpa.Workflow;
import com.gw.tasks.GeoweaverWorkflowTask;
import com.gw.tasks.TaskManager;
import com.gw.utils.BaseTool;
import com.gw.utils.RandomString;
import java.io.File;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * @author JensenSun
 */
@Service
@Scope("prototype")
public class WorkflowTool {

  public Map<String, String> token2ws = new HashMap();

  private Logger logger = Logger.getLogger(WorkflowTool.class);

  @Autowired WorkflowRepository workflowrepository;

  @Autowired HistoryRepository historyrepository;

  @Autowired CheckpointRepository checkpointrepository;

  @Autowired TaskManager tm;

  @Autowired ProcessTool pt;

  @Autowired HistoryTool tool;

  @Autowired BaseTool bt;

  @Autowired GeoweaverWorkflowTask task;

  /**
   * For Andrew
   *
   * @param history_id
   * @return
   */
  public String stop(String history_id) {

    History whis = historyrepository.findById(history_id).get();

    String childprocesses = whis.getHistory_output();

    String[] child_process_ids = childprocesses.split(";");

    for (String cid : child_process_ids) {

      if (!BaseTool.isNull(cid)) {

        pt.stop(cid);

        tm.stopTask(cid);
      }
    }

    whis.setIndicator(ExecutionStatus.STOPPED);

    historyrepository.save(whis);

    String resp = "{\"history_id\": \"" + history_id + "\", \"ret\": \"stopped\"}";

    return resp;
  }

  public String toJSON(Workflow w) {

    String json = "{}";
    ObjectMapper mapper = new ObjectMapper();
    try {
      json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(w);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return json;
  }

  public List<Workflow> getWorkflowListByOwner(String ownerid) {

    Iterator<Workflow> wit = workflowrepository.findAllPublic().iterator();

    List<Workflow> actualList = new ArrayList<Workflow>();

    wit.forEachRemaining(actualList::add);

    wit = workflowrepository.findAllPrivateByOwner(ownerid).iterator();

    wit.forEachRemaining(actualList::add);

    return actualList;
  }

  public String list(String owner) {

    Iterator<Workflow> wit = workflowrepository.findAllPublic().iterator();

    StringBuffer json = new StringBuffer("[");

    while (wit.hasNext()) {

      Workflow w = wit.next();

      json.append(toJSON(w)).append(",");
    }

    wit = workflowrepository.findAllPrivateByOwner(owner).iterator();

    while (wit.hasNext()) {

      json.append(toJSON(wit.next())).append(",");
    }

    if (json.length() > 1) json.deleteCharAt(json.length() - 1);

    json.append("]");

    return json.toString();
  }

  public Workflow getById(String id) {

    Optional<Workflow> wo = workflowrepository.findById(id);

    Workflow w = null;

    if (wo.isPresent()) w = wo.get();

    return w;
  }

  public String detail(String id) {

    Optional<Workflow> wo = workflowrepository.findById(id);

    Workflow wf = null;

    if (wo.isPresent()) wf = wo.get();

    return toJSON(wf);
  }

  /**
   * Find a process whose status is not executed, while all of its condition nodes are satisfied.
   *
   * @param nodemap
   * @param flags
   * @param nodes
   * @return
   */
  public String[] findNextProcess(
      Map<String, List> nodemap, ExecutionStatus[] flags, JSONArray nodes) {

    String id = null;

    String num = null;

    for (int i = 0; i < nodes.size(); i++) {

      String currentid = (String) ((JSONObject) nodes.get(i)).get("id");

      if (checkNodeStatus(currentid, flags, nodes).equals(ExecutionStatus.READY)) {

        continue;
      }

      List prenodes = nodemap.get(currentid);

      boolean satisfied = true;

      // check if all the prenodes are satisfied

      for (int j = 0; j < prenodes.size(); j++) {

        String prenodeid = (String) prenodes.get(j);

        // if any of the pre- nodes is not satisfied, this node is passed.

        if (!(checkNodeStatus(prenodeid, flags, nodes).equals(ExecutionStatus.DONE)
            || checkNodeStatus(prenodeid, flags, nodes).equals(ExecutionStatus.FAILED)
            || checkNodeStatus(prenodeid, flags, nodes).equals(ExecutionStatus.SKIPPED))) {

          satisfied = false;

          break;
        }
      }

      if (satisfied) {

        id = currentid;

        num = String.valueOf(i);

        break;
      }
    }

    String[] ret = new String[] {id, num};

    return ret;
  }

  public List<Workflow> getAllWorkflow() {

    List<Workflow> wlist = new ArrayList();

    workflowrepository.findAll().forEach(w -> wlist.add(w));

    return wlist;
  }

  public void save(Workflow w) {

    Workflow wold = this.getById(w.getId());

    if (!BaseTool.isNull(wold)) {

      if (BaseTool.isNull(w.getName())) w.setName(wold.getName());

      if (BaseTool.isNull(w.getConfidential())) w.setConfidential(wold.getConfidential());

      if (BaseTool.isNull(w.getDescription())) w.setDescription(wold.getDescription());

      if (BaseTool.isNull(w.getEdges())) w.setEdges(wold.getEdges());

      if (BaseTool.isNull(w.getNodes())) w.setNodes(wold.getNodes());

      if (BaseTool.isNull(w.getOwner())) w.setOwner(wold.getOwner());
    }

    workflowrepository.save(w);
  }

  /**
   * Check the status of a node
   *
   * @param id
   * @param flags
   * @param nodes
   * @return
   */
  private ExecutionStatus checkNodeStatus(String id, ExecutionStatus[] flags, JSONArray nodes) {

    ExecutionStatus status = null;

    for (int j = 0; j < nodes.size(); j++) {

      String nodeid = (String) ((JSONObject) nodes.get(j)).get("id");

      if (nodeid.equals(id)) {

        status = flags[j];

        break;
      }
    }

    return status;
  }

  /**
   * Execute a workflow
   *
   * @param id id that will be used to label this execution history. If none, a new history id will
   *     be generated.
   * @param mode mode of execution environment, two options: one or multiple
   * @param hosts list of host Ids
   * @param pswd list of host password. This list should match the hosts list exactly.
   * @param token token is the session id with the client browser. In Geoweaver CLI mode, it will be
   *     ignored.
   * @return
   */
  public String execute(
      String history_id,
      String wid,
      String mode,
      String[] hosts,
      String[] pswds,
      String[] envs,
      String token) {

    // use multiple threads to execute the processes

    String resp = null;

    try {

      task.initialize(history_id, wid, mode, hosts, pswds, envs, token);

      task.execute();

      resp =
          "{\"history_id\": \""
              + task.getHistory_id()
              + "\", \"token\": \""
              + token
              + "\", \"ret\": \"success\"}";

    } catch (Exception e) {

      e.printStackTrace();

      throw new RuntimeException(e.getLocalizedMessage());
    }

    return resp;
  }

  /**
   * Update workflow nodes and edges
   *
   * @param wid
   * @param nodes
   * @param edges
   */
  public void update(String wid, String nodes, String edges) {

    Workflow wf = workflowrepository.findById(wid).get();

    wf.setNodes(nodes);

    wf.setEdges(edges);

    workflowrepository.save(wf);
  }

  public String add(String name, String nodes, String edges, String ownerid) {

    String newid = new RandomString(20).nextString();

    Workflow wf = new Workflow();

    wf.setId(newid);

    wf.setName(name);

    wf.setEdges(edges);

    wf.setNodes(nodes);

    wf.setOwner(ownerid);

    workflowrepository.save(wf);

    return newid;
  }

  public String del(String workflowid) {

    if (checkpointrepository.findByWorkflowId(workflowid).size() > 0) {
      checkpointrepository.deleteByWorkflowId(workflowid);
    }

    workflowrepository.deleteById(workflowid);

    return "done";
  }

  /**
   * Get all active processes
   *
   * @return
   */
  public String all_active_process() {

    StringBuffer resp = new StringBuffer();

    List<Object[]> active_his_workflow = historyrepository.findRunningWorkflow();

    try {

      resp.append("[");

      int num = 0;

      for (; num < active_his_workflow.size(); num++) {

        if (num != 0) {

          resp.append(", ");
        }

        Object[] hiscols = active_his_workflow.get(num);

        resp.append("{ \"id\": \"").append(hiscols[0]).append("\", ");

        resp.append("\"begin_time\": \"").append(hiscols[1]).append("\", ");

        resp.append("\"end_time\": \"").append(hiscols[2]).append("\", ");

        resp.append("\"status\": \"").append(bt.escape(String.valueOf(hiscols[3]))).append("\", ");

        resp.append("\"output\": \"").append(hiscols[4]).append("\"}");
      }

      resp.append("]");

    } catch (Exception e) {

      e.printStackTrace();
    }

    return resp.toString();
  }

  /**
   * show the history of every execution of the workflow
   *
   * @param string
   * @return
   */
  public String all_history(String workflow_id) {

    return tool.workflow_all_history(workflow_id);
  }

  /**
   * List to JSON
   *
   * @param list
   * @return
   */
  public String list2JSON(String list) {

    StringBuffer json = new StringBuffer("[");

    String[] ps = list.split(";");

    for (int i = 0; i < ps.length; i++) {

      if (i != 0) {

        json.append(",");
      }

      json.append("\"").append(ps[i]).append("\"");
    }

    json.append("]");

    return json.toString();
  }

  public String recent(int limit) {

    StringBuffer resp = new StringBuffer();

    try {

      List<Object[]> recent_his_workflow = historyrepository.findRecentWorkflow(limit);

      resp.append("[");

      int num = 0;

      for (; num < recent_his_workflow.size(); num++) {

        if (num != 0) {

          resp.append(", ");
        }

        Object[] recent_his = recent_his_workflow.get(num);

        resp.append("{ \"id\": \"").append(recent_his[0]).append("\", "); // history id

        resp.append("\"name\": \"").append(recent_his[13]).append("\", ");

        resp.append("\"end_time\": \"").append(recent_his[2]).append("\", ");

        resp.append("\"begin_time\": \"").append(recent_his[1]).append("\"}");
      }

      resp.append("]");

      if (num == 0) resp = new StringBuffer();

    } catch (Exception e) {

      e.printStackTrace();
    }

    return resp.toString();
  }

  public String one_history(String hid) {

    StringBuffer resp = new StringBuffer();

    try {

      Optional<History> hisopt = historyrepository.findById(hid);

      if (hisopt.isPresent()) {

        History h = hisopt.get();

        resp.append("{ \"hid\": \"").append(h.getHistory_id()).append("\", ");

        resp.append("\"process\": \"").append(h.getHistory_process()).append("\", ");

        resp.append("\"begin_time\":\"").append(h.getHistory_begin_time()).append("\", ");

        resp.append("\"end_time\":\"").append(h.getHistory_end_time()).append("\", ");

        String processes = h.getHistory_input();

        String histories = h.getHistory_output();

        resp.append("\"input\":").append(list2JSON(processes)).append(", ");

        resp.append("\"output\":").append(list2JSON(histories)).append(" }");
      }

    } catch (Exception e) {

      e.printStackTrace();
    }

    return resp.toString();
  }

  /**
   * Get export mode string by number id
   *
   * @param mode_no
   * @return mode string
   */
  public String getExportModeById(int mode_no) {

    String mode = "workflowwithprocesscodehistory";

    switch (mode_no) {
      case 1:
        mode = "workflowonly";
        break;

      case 2:
        mode = "workflowwithprocesscode";
        break;

      case 3:
        mode = "workflowwithprocesscodegoodhistory";
        break;

      default:
        mode = "workflowwithprocesscodehistory";
        break;
    }

    return mode;
  }

  /**
   * Download workflow
   *
   * @param wid workflow id
   * @param option workflowonly | workflowwithprocesscode | workflowwithprocesscodegoodhistory|
   *     workflowwithprocesscodehistory
   * @return
   * @throws ParseException
   */
  public String download(String wid, String option) throws ParseException {

    Workflow wf = this.getById(wid);

    String fileurl = "download/temp/" + wf.getId() + ".zip";

    String savefilepath =
        bt.getFileTransferFolder() + wf.getId() + FileSystems.getDefault().getSeparator();

    File tf = new File(savefilepath);

    bt.deleteDirectory(tf);

    if (!tf.exists()) tf.mkdirs();

    String workflowstring = bt.toJSON(wf);

    bt.writeString2File(workflowstring, savefilepath + "workflow.json");

    if (option.contains("processcode")) {

      JSONParser jsonParser = new JSONParser();

      JSONArray arrayobj = (JSONArray) jsonParser.parse(wf.getNodes());

      String codesavefile = savefilepath + "code" + FileSystems.getDefault().getSeparator();

      File codef = new File(codesavefile);

      if (!codef.exists()) codef.mkdirs();

      StringBuffer processjson = new StringBuffer("[");

      String prefix = "";

      for (int i = 0; i < arrayobj.size(); i++) {

        try {

          JSONObject jsonObj = (JSONObject) arrayobj.get(i);

          String process_workflow_id = (String) jsonObj.get("id");

          String process_id = process_workflow_id.split("-")[0];

          String targetsourcefile = codesavefile + pt.getProcessFileName(process_id);

          if (new File(targetsourcefile).exists()) continue;

          GWProcess p = pt.getProcessById(process_id);

          bt.writeString2File(p.getCode(), targetsourcefile);

          processjson.append(prefix);

          prefix = ",";

          processjson.append(pt.toJSON(p));

        } catch (Exception e) {

          e.printStackTrace();
        }
      }

      processjson.append("]");

      bt.writeString2File(processjson.toString(), codesavefile + "process.json");
    }

    if (option.contains("history")) {

      String wfhistorysavefile =
          savefilepath + "history" + FileSystems.getDefault().getSeparator() + wid + ".json";

      // first save all history of the workflow

      List<History> histlist = historyrepository.findByWorkflowId(wid);

      StringBuffer workflowhistory = new StringBuffer("[");

      String prefix = "";

      for (History h : histlist) {

        if ("workflowwithprocesscodegoodhistory".equals(option)
            && !ExecutionStatus.DONE.equals(h.getIndicator())) {

          continue;
        }

        String historystr = bt.toJSON(h);

        workflowhistory.append(prefix);

        prefix = ",";

        workflowhistory.append(historystr);
      }
      ;

      workflowhistory.append("]");

      bt.writeString2File(workflowhistory.toString(), wfhistorysavefile);

      // second, save process history of one workflow execution into a file
      HashSet<String> process_id_set = new HashSet<>();

      for (History h : histlist) {

        if ("workflowwithprocesscodegoodhistory".equals(option)
            && !ExecutionStatus.DONE.equals(h.getIndicator())) {

          continue;
        }

        String[] processhistorylist = h.getHistory_output().split(";");

        prefix = "";

        String processhistorysavefile =
            savefilepath
                + "history"
                + FileSystems.getDefault().getSeparator()
                + h.getHistory_id()
                + ".json"; // all the process history of one workflow run

        StringBuffer processhistorybuffer = new StringBuffer("[");

        for (String processhitoryid : processhistorylist) {

          Optional<History> hisop = historyrepository.findById(processhitoryid);

          if (hisop.isPresent()) {

            History hist = hisop.get();

            if ("workflowwithprocesscodegoodhistory".equals(option)
                && !ExecutionStatus.DONE.equals(hist.getIndicator())) {

              continue;
            }

            processhistorybuffer.append(prefix);

            prefix = ",";

            processhistorybuffer.append(bt.toJSON(hist));

            if (!process_id_set.contains(hist.getHistory_process()))
              process_id_set.add(hist.getHistory_process());
          }
        }

        processhistorybuffer.append("]");

        bt.writeString2File(processhistorybuffer.toString(), processhistorysavefile);
      }

      // if need all the history of the involved processes, go into this if
      if (option.contains("allhistory") || "workflowwithprocesscodegoodhistory".equals(option)) {

        for (String history_process_id : process_id_set) {

          histlist = historyrepository.findByProcessIdFull(history_process_id);

          StringBuffer allprocesshistorybuffer = new StringBuffer("[");

          // every process has a history file
          String allprocesshistorysavefile =
              savefilepath
                  + "history"
                  + FileSystems.getDefault().getSeparator()
                  + "process_"
                  + history_process_id
                  + ".json";

          for (History hist : histlist) {

            if ("workflowwithprocesscodegoodhistory".equals(option)
                && !ExecutionStatus.DONE.equals(hist.getIndicator())) {

              continue;
            }

            allprocesshistorybuffer.append(bt.toJSON(hist)).append(",");
          }

          allprocesshistorybuffer.append("]");

          bt.writeString2File(allprocesshistorybuffer.toString(), allprocesshistorysavefile);
        }
      }
    }

    String readmeContent = createReadme(wf);
    bt.writeString2File(readmeContent, savefilepath + "README.md");
    bt.zipFolder(savefilepath, bt.getFileTransferFolder() + wf.getId() + ".zip");

    return fileurl;
  }

  public String createReadme(Workflow wf) {
    String readmeTemplate = 
        "![Workflow Badge](https://img.shields.io/badge/Workflow-{workflow_name}-blue.svg)\n\n" +
        "# Workflow Name: {workflow_name}\n\n" +
        "## Description\n" +
        "{description}\n\n" +
        "## Processes\n" +
        "{processes}\n\n" +
        "### Process Descriptions\n" +
        "{processDescriptions}\n\n" +
        "## Steps to use the workflow\n\n" +
        "This section provides detailed instructions on how to use the workflow. Follow these steps to set up and execute the workflow using Geoweaver.\n\n" +
        "### Step-by-Step Instructions\n\n" +
        "### Step 1: Download the zip file\n" +
        "### Step 2: Import the Workflow into Geoweaver\n" +
        "Open Geoweaver running on your local machine. [video guidance](https://youtu.be/jUd1dzi18EQ)\n" +
        "1. Click on \"Weaver\" in the top navigation bar.\n" +
        "2. A workspace to add a workflow opens up. Select the \"Import\" icon in the top navigation bar.\n" +
        "3. Choose the downloaded zip file" +
        "4. Click on \"Start\" to upload the file. If the file is valid, a prompt will ask for your permission to upload. Click \"OK\".\n" +
        "5. Once the file is uploaded, Geoweaver will create a new workflow.\n\n" +
        "### Step 3: Execute the Workflow\n" +
        "1. Click on the execute icon in the top navigation bar to start the workflow execution process.[video guidance](https://youtu.be/PJcMNR00QoE)\n" +
        "2. A wizard will open where you need to select the [video guidance](https://youtu.be/KYiEHI0rn_o) and environment [video guidance](https://www.youtube.com/watch?v=H66AVoBBaHs).\n" +
        "3. Click on \"Execute\" to initiate the workflow. Enter the required password when prompted and click \"Confirm\" to start executing the workflow.\n\n" +
        "### Step 4: Monitor Execution and View Results\n" +
        "1. The workflow execution will begin.\n" +
        "2. Note: Green indicates the process is successful, Yellow indicates the process is running, and Red indicates the process has failed.\n" +
        "3. Once the execution is complete, the results will be available immediately.\n\n" +
        "By following these steps, you will be able to set up and execute the snow cover mapping workflow using Geoweaver.\n";

    String processes = getProcessTitles(wf);
    String processDescriptions = getProcessDescriptions(wf);

    String readmeContent = readmeTemplate.replace("{workflow_name}", wf.getName())
    .replace("{description}", wf.getDescription())
    .replace("{processes}", processes)
    .replace("{processDescriptions}", processDescriptions);

    return readmeContent;
  }

  private String getProcessTitles(Workflow wf) {
    StringBuilder processTitles = new StringBuilder();
    JSONParser jsonParser = new JSONParser();
    try {
        JSONArray nodes = (JSONArray) jsonParser.parse(wf.getNodes());
        for (Object node : nodes) {
            JSONObject jsonObj = (JSONObject) node;
            String title = (String) jsonObj.get("title");
            if (title != null) {
                if (processTitles.length() > 0) {
                    processTitles.append(", ");
                }
                processTitles.append(title);
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    return processTitles.toString();
  } 

  private String getProcessDescriptions(Workflow wf) {
    StringBuilder processDescriptions = new StringBuilder();
    JSONParser jsonParser = new JSONParser();
    try {
        JSONArray nodes = (JSONArray) jsonParser.parse(wf.getNodes());
        for (Object node : nodes) {
            JSONObject jsonObj = (JSONObject) node;
            String process_workflow_id = (String) jsonObj.get("id");
            String process_id = process_workflow_id.split("-")[0];
            GWProcess p = pt.getProcessById(process_id);
            processDescriptions.append(p.getName()).append(": ").append(p.getDescription()).append("\n");
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    return processDescriptions.toString();
  }
  public String precheck(String filename) {

    // { "url": "download/temp/aavthwdfvxinra0a0rsw.zip", "filename": "aavthwdfvxinra0a0rsw.zip" }

    String filepath = bt.getFileTransferFolder() + filename;

    StringBuffer respjson = new StringBuffer();

    if (filename.endsWith(".zip")) {

      try {

        String foldername = filename.substring(0, filename.lastIndexOf("."));

        String folder_path =
            bt.getFileTransferFolder() + foldername + FileSystems.getDefault().getSeparator();

        bt.unzip(filepath, folder_path);

        // Get the workflowjson path, by traversing the folder
        String workflowJsonPath = bt.getWorkflowJsonPath(folder_path);

        // if the workflowjson is not found, return invalid workflow package, cause the workflowjson
        // is required
        if (workflowJsonPath.equals("")) {

          throw new RuntimeException("Invalid workflow package.");
        }

        String workflowjson = bt.readStringFromFile(workflowJsonPath);

        String workflowFolderPath =
            workflowJsonPath.substring(0, workflowJsonPath.lastIndexOf("workflow.json"));

        String codefolder = workflowFolderPath + "code";

        String historyfolder = workflowFolderPath + "history";

        // If the read workflowjson is not valid, return invalid workflow package, cause the
        // workflowjson is required
        if (!BaseTool.isNull(workflowjson)) {

          if (new File(codefolder).exists()) {

            if (new File(historyfolder).exists()) {

              logger.debug("History folder exists");
            }
            respjson.append(workflowjson);

          } else {

            throw new RuntimeException("Cannot import as there is only workflow.json.");
          }

        } else {

          throw new RuntimeException("Invalid workflow package.");
        }

      } catch (Exception e) {

        e.printStackTrace();

        throw new RuntimeException(e.getLocalizedMessage());
      }

    } else {

      throw new RuntimeException("We only support .ZIP workflow file.");
    }

    return respjson.toString();
  }

  public Workflow fromJSON(String json) {

    Workflow w = null;

    try {

      ObjectMapper mapper = new ObjectMapper();

      w = mapper.readValue(json, Workflow.class);

    } catch (Exception e) {

      e.printStackTrace();
    }

    return w;
  }

  public History historyFromJSON(String json) {

    History h = null;

    try {

      ObjectMapper mapper = new ObjectMapper();

      h = mapper.readValue(json, History.class);

    } catch (Exception e) {

      e.printStackTrace();
    }

    return h;
  }

  public String saveWorkflowFromFolder(String wid, String foldername) throws ParseException {

    JSONParser jsonparser = new JSONParser();

    foldername = foldername.substring(0, foldername.lastIndexOf("."));

    String folder_path =
        bt.getFileTransferFolder() + foldername + FileSystems.getDefault().getSeparator();

    String workflowJsonPath = bt.getWorkflowJsonPath(folder_path);

    String workflowFolderPath =
        workflowJsonPath.substring(0, workflowJsonPath.lastIndexOf("workflow.json"));

    String workflowjson = bt.readStringFromFile(workflowJsonPath);

    String codefolder = workflowFolderPath + "code" + FileSystems.getDefault().getSeparator();

    String historyfolder = workflowFolderPath + "history" + FileSystems.getDefault().getSeparator();

    // save workflow
    Workflow w = this.fromJSON(workflowjson);

    this.save(w);

    List<History> historyList = new ArrayList<>();
    File[] files = new File(historyfolder).listFiles();
    if (files != null) {
      for (File file : files) {
        String historyjson = bt.readStringFromFile(file.getAbsolutePath());
        JSONArray historyarray = (JSONArray) jsonparser.parse(historyjson);

        historyarray.forEach(
            (obj) -> {
              String jsonobj = ((JSONObject) obj).toJSONString();
              History hist = historyFromJSON(jsonobj);
              historyList.add(hist);
            });
      }
    }

    historyrepository.saveAll(historyList);

    // save process
    String processjson = bt.readStringFromFile(codefolder + "process.json");

    JSONArray processarray = (JSONArray) jsonparser.parse(processjson);

    processarray.forEach(
        (obj) -> {
          String jsonobj = ((JSONObject) obj).toJSONString();

          // should be changed to check if the process already exists. Use the existing
          // value if the incoming value is null
          GWProcess p = pt.fromJSON(jsonobj);

          pt.save(p);
        });

    return workflowjson;
  }

  public String check_process_skipped(String workflow_id, String workflow_process_id) {

    String isskip = "false";

    try {

      JSONParser parser = new JSONParser();

      if (workflow_id == null) {
        throw new RuntimeException("Please save the  workflow to make changes to skip state");
      }

      Workflow wf = this.getById(workflow_id);

      JSONArray nodes_array = (JSONArray) parser.parse(wf.getNodes());

      for (int i = 0; i < nodes_array.size(); i++) {

        String current_process_id = String.valueOf(((JSONObject) nodes_array.get(i)).get("id"));

        if (workflow_process_id.equals(current_process_id)) {

          String the_skip_str = String.valueOf(((JSONObject) nodes_array.get(i)).get("skip"));

          if (!"null".equals(the_skip_str)) {

            isskip = the_skip_str;
          }

          break;
        }
      }

    } catch (Exception e) {

      e.printStackTrace();

      throw new RuntimeException(
          String.format(
              "Fail to get skip status of process %s in workflow %s ",
              workflow_process_id, workflow_id));
    }

    return isskip;
  }

  public void skip_process(String workflow_id, String workflow_process_id, String skip) {

    try {

      Workflow wf = this.getById(workflow_id);

      JSONParser parser = new JSONParser();

      JSONArray nodes_array = (JSONArray) parser.parse(wf.getNodes());

      for (int i = 0; i < nodes_array.size(); i++) {

        String current_process_id = String.valueOf(((JSONObject) nodes_array.get(i)).get("id"));

        if (workflow_process_id.equals(current_process_id)) {

          ((JSONObject) nodes_array.get(i)).put("skip", skip);

          break;
        }
      }

      wf.setNodes(nodes_array.toJSONString());

      this.save(wf);

      logger.info(
          String.format("Done Skip process %s in workflow %s ", workflow_process_id, workflow_id));

    } catch (Exception e) {

      e.printStackTrace();

      throw new RuntimeException(
          String.format(
              "Fail to skip process %s in workflow %s ", workflow_process_id, workflow_id));
    }
  }
}
