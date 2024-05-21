const { defineConfig } = require('cypress')
const os = require('os');
module.exports = defineConfig({
  projectId: 'u864mu',
  video: true,
  env: {
    codeCoverage: {
      url: 'http://localhost:3000/__coverage__'
    },
    home:  os.homedir(),
  },
  e2e: {
    experimentalStudio: true,
    screenshotsFolder: "cypress/screenshots",
    setupNodeEvents(on, config) {
      on('task', {
        log(message) {
          // Then to see the log messages in the terminal
          //   cy.task("log", "my message");
          console.log(message +'\n\n');
          return null;
        },
      });},
    setupNodeEvents(on, config) {
      on('after:spec', (spec, results) => {
        if (results && results.video) {
          // Do we have failures for any retry attempts?
          const failures = results.tests.some((test) =>
            test.attempts.some((attempt) => attempt.state === 'failed')
          )
          if (!failures) {
            // delete the video if the spec passed and no tests retried
            fs.unlinkSync(results.video)
          }
        }
      })
    }
    

      
    }
  
})
