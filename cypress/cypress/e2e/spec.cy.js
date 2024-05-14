describe('Write Password into .secret', () => {
  it('Should write secret to a file', () => {
    // Hash value to be written to the file
    // const dataToWrite = '4205c81c1aaafae4406dc56bd6c8b26edeb816c6d18294cf0aeee4a948146e0fa3e7cf0ea3e3a6de0b7fe990d7de28ec3060f953b88e4cef5ade04c12ff917ee';
    const homeDirectory = Cypress. env('home');
   
    cy.log('Home Directory:', homeDirectory); // Debug statement
    const filePath = `${homeDirectory}/gw-workspace/.secret`;
    console.log(filePath)
    cy.task('log', 'Logging home directory: ' + homeDirectory)
    cy.task('log', 'Logging filepath: ' + filePath)
    // cy.writeFile(filePath, dataToWrite, 'binary')
    //   .then(() => {
    //     cy.readFile(filePath).should('contain', dataToWrite);
    //     cy.readFile(filePath).then((fileContents) => {
    //       cy.task('log', 'File Contents: ' + fileContents);
    //       cy.log('File Contents:', fileContents);
    //     });
    //   })

      const dataToWrite = "4205c81c1aaafae4406dc56bd6c8b26edeb816c6d18294cf0aeee4a948146e0fa3e7cf0ea3e3a6de0b7fe990d7de28ec3060f953b88e4cef5ade04c12ff917ee";

      cy.writeFile(filePath, dataToWrite)
        .then(() => {
          cy.readFile(filePath).should('eq', dataToWrite); // Check if file content is equal to dataToWrite
          cy.readFile(filePath).then((fileContents) => {
            cy.task('log', 'File Contents: ' + fileContents);
            cy.log('File Contents:', fileContents);
          });
        });
  });


});

describe('Create Python process and run it', () => {
  it('creates python process and runs', () => {
    cy.visit('http://localhost:8070/Geoweaver');
    cy.get('.introjs-skipbutton').click();
    cy.get('#newprocess').click();

    cy.get('form select.form-control.form-control-sm').select('Python');
    cy.get('form > :nth-child(1) > :nth-child(4)').type('hello_world.py');


    cy.get('.CodeMirror-lines').type("\nprint('hello world!')");
    cy.get('.modal-footer').contains('Add').click();

    cy.get('ul#process_folder_python_target').contains('hello_world.py').click();

    cy.get('#processid').then(($input) => {
      const processId = $input.val(); // Get the value of the input field
      console.log('process id ',processId)
      const selector = `[onclick="GW.process.runProcess('${processId}', 'hello_world.py', 'python')"]`;
      cy.get(selector).click(); 
    });
   
    cy.get('#host-execute-btn').click();

    cy.get('#process-confirm-btn').click();
    cy.get('#inputpswd').clear('1');
    cy.get('#inputpswd').type('1234');
    cy.get('#pswd-confirm-btn').click();
    cy.intercept('POST','/Geoweaver/web/executeProcess').as('executeProcess'); 
    cy.wait('@executeProcess').its('response.statusCode').should('eq', 200);
    // cy.get('#single-console-content').should('contain', 'hello world!');
    // cy.get('#single-console-content').should('contain', 'Exit Code: 0');
    // cy.get('#process-log-window').click();
    // cy.get('#process-log-window').should('be.visible');
   
  });
});



describe('Hosts Testing', () => {
  it('Create New Host', () => {
    cy.visit('http://localhost:8070/Geoweaver/web/geoweaver');
    cy.get('.introjs-skipbutton').click();
    cy.get('#newhost').click();
    cy.get('#hostip').clear('1');
    cy.get('#hostip').type('1.1.1.1');
    cy.get('#hostport').clear('2');
    cy.get('#hostport').type('22');
    cy.get('#username').clear('n');
    cy.get('#username').type('newuser');
    cy.get('#host-add-btn').click();
    cy.get('ul#host_folder_ssh_target').should('contain', 'New Host');
  }) 

  it('Delete New Host', () => {
    cy.visit('http://localhost:8070/Geoweaver/web/geoweaver');
    cy.get('.introjs-skipbutton').click();
    cy.get('#host_folder_ssh > a').click();
    cy.get('ul#host_folder_ssh_target').contains('New Host').click();
    cy.get('.fa-minus').click();
    cy.get('#del-confirm-btn').click();
  })

  it('LocalHost testing', () => {
    cy.visit('http://localhost:8070/Geoweaver/web/geoweaver');
    cy.get('.introjs-skipbutton').click();
    cy.get('#host_folder_ssh > a').click();
    cy.get('#host-100001').click();
    cy.get('#_host_name').should('have.value', 'Localhost');
    cy.get('#_host_ip').should('have.value', '127.0.0.1');
    cy.get('#_host_port').should('have.value', '22');
    cy.get('#_host_username').should('have.value', 'publicuser');
    cy.get('#_host_url').should('have.value', 'http://localhost/');
    cy.get('#_host_type').click();
    cy.get('#_host_type').should('have.text', 'ssh');
  })

  it('LocalHost Read Python Env', () => {
    cy.visit('http://localhost:8070/Geoweaver/web/geoweaver');
    cy.get('.introjs-skipbutton').click();
    cy.get('#host_folder_ssh > a').click();
    cy.get('#host-100001').click();
    cy.get('.fab').click();
    cy.get('#inputpswd').clear('1');
    cy.get('#inputpswd').type('1234');
    cy.get('#pswd-confirm-btn').click();
    cy.intercept('POST', '/Geoweaver/web/readEnvironment').as('readEnvironment');
    cy.wait('@readEnvironment').then((interception) => {
      expect(interception.response.statusCode).to.equal(200);
    });
  })

  it('LocalHost File Upload', () => {
    cy.visit('http://localhost:8070/Geoweaver/web/geoweaver');
    cy.get('.introjs-skipbutton').click();
    cy.get('#host_folder_ssh > a').click();
    cy.get('#host-100001').click();
    cy.get('p > .fa-upload').click();
    cy.get('#inputpswd').clear('1');
    cy.get('#inputpswd').type('1234');
    cy.get('#pswd-confirm-btn').click();
    cy.get('#host-file-uploader').click();
    cy.intercept('POST', 'http://localhost:8070/Geoweaver/web/authenticateUser').as('authenticateUser');
    cy.wait('@authenticateUser').its('response.statusCode').should('eq', 200);
  })

});