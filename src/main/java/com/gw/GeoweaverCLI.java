package com.gw;

import com.gw.commands.TopEntryCommand;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import picocli.CommandLine;
import picocli.CommandLine.IFactory;

@SpringBootApplication
@EnableJpaRepositories(basePackages = {"com.gw.database.*"})
@ComponentScan(basePackages = {"com.gw.commands.*", "com.gw.database.*", "com.gw.jpa.*", 
"com.gw.local.*", "com.gw.search.*", "com.gw.tasks.*", "com.gw.tools.*", "com.gw.user.*",
"com.gw.utils.*",  "com.gw.workers.*"})
public class GeoweaverCLI implements CommandLineRunner {

    final TopEntryCommand topEntryCommand;
    final IFactory factory;

    @Override
    public void run(String... args) {

        // System.exit(new CommandLine(topEntryCommand, factory).execute(args));
        new CommandLine(topEntryCommand, factory).execute(args);

    }

    public static void main(String[] args) throws Exception {
        
        SpringApplicationBuilder builder = new SpringApplicationBuilder(GeoweaverCLI.class);

        System.exit(SpringApplication.exit(
            builder.web(WebApplicationType.NONE)
                // .headless(false)
                .run(args)));
        // System.exit(SpringApplication.exit(SpringApplication.run(GeoweaverCLI.class, args)));
		
    }

    public GeoweaverCLI(TopEntryCommand topEntryCommand, IFactory factory) {
        this.topEntryCommand = topEntryCommand;
        this.factory = factory;
    }
    
}
