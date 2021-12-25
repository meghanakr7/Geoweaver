package com.gw;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gw.jpa.GWUser;
import com.gw.jpa.Workflow;
import com.gw.ssh.RSAEncryptTool;
import com.gw.tools.UserTool;
import com.gw.utils.BaseTool;
import com.gw.web.GeoweaverController;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PythonTest {

    @Autowired
	UserTool ut;

	@Autowired
	BaseTool bt;

    @Autowired
	private TestRestTemplate testrestTemplate;

    @LocalServerPort
	private int port;

	@Autowired
	RSAEncryptTool rsatool;

    Logger logger  = Logger.getLogger(this.getClass());

    @Test
	void contextLoads() {
		
		
	}
    
    @Test
    public void testPythonProcess() throws Exception{

        HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String pythonjson = bt.readStringFromFile(bt.testResourceFiles()+ "/add_python_process.json" );
    	HttpEntity request = new HttpEntity<>(pythonjson, headers);
		String result = this.testrestTemplate.postForObject("http://localhost:" + this.port + "/Geoweaver/web/add/process", 
			request, 
			String.class);
		logger.debug("the result is: " + result);
		// assertThat(controller).isNotNull();
		assertThat(result).contains("id");

		ObjectMapper mapper = new ObjectMapper();
		Map<String,Object> map = mapper.readValue(result, Map.class);
		String pid = String.valueOf(map.get("id"));

		//get key
		result = this.testrestTemplate.postForObject("http://localhost:" + this.port + "/Geoweaver/web/key", 
			null, 
			String.class);

		JSONParser jsonParser=new JSONParser();

		JSONObject jsonobj = (JSONObject)jsonParser.parse(result);

		String rsakey = jsonobj.get("rsa_public").toString();
		assertNotNull(rsakey);

		//encode the password
		
		KeyPair kpair = rsatool.buildKeyPair();

		String encryppswd = rsatool.byte2Base64(rsatool.encrypt(kpair.getPublic(), "111111"));

		
		
		//run the python process
		// headers.setContentType(MediaType.APPLICATION_JSON);
		// pythonjson = bt.readStringFromFile(bt.testResourceFiles()+ "/run_python_process.txt" );
		// pythonjson = pythonjson.replace("jsff21", pid);
		// pythonjson = pythonjson.replace("<pswdencrypt>", encryppswd);
		
    	// request = new HttpEntity<>(pythonjson, headers);
		// result = this.testrestTemplate.postForObject("http://localhost:" + this.port + "/Geoweaver/web/add/process", 
		// 	request, 
		// 	String.class);
		// logger.debug("the result is: " + result);
		// // assertThat(controller).isNotNull();
		// assertThat(result).contains("id");



		//edit the python process

		//remove the python process
		// id=2avx48&type=process
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    	request = new HttpEntity<>("id="+map.get("id")+"&type=process", headers);
		result = this.testrestTemplate.postForObject("http://localhost:" + this.port + "/Geoweaver/web/del", 
			request, 
			String.class);
		logger.debug("the result is: " + result);
		// assertThat(controller).isNotNull();
		assertThat(result).contains("done");

    }

}
