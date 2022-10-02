package com.gw.commands;

import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Component
@Command(name = "workflow")
public class ImportWorkflowCommand {


    @Parameters(index = "0", description = "Geoweaver workflow zip file path")
    String workflow_file_path;
    
    public void run() {
    
        
    
    }

}
