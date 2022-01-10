[![Build Status](https://travis-ci.org/ZihengSun/Geoweaver.svg?branch=master)](https://travis-ci.org/ESIPFed/Geoweaver) [![License](https://img.shields.io/github/license/ESIPFed/Geoweaver.svg)](https://github.com/ESIPFed/Geoweaver/blob/master/LICENSE) [![Stars](https://img.shields.io/github/stars/ESIPFed/Geoweaver.svg)](https://github.com/ESIPFed/Geoweaver/stargazers) [![Forks](https://img.shields.io/github/forks/ESIPFed/Geoweaver.svg)](https://github.com/ESIPFed/Geoweaver/network/members) [![Issues](https://img.shields.io/github/issues/ESIPFed/Geoweaver.svg)](https://github.com/ESIPFed/Geoweaver/issues) [![Coverage](https://img.shields.io/badge/coverage-100%25-success.svg)](https://codecov.io/)

# [Geoweaver](https://esipfed.github.io/Geoweaver/)

2018 ESIP Lab Incubator Project

Geoweaver is an in-browser software allowing users to easily compose and execute full-stack data processing workflows via taking advantage of online spatial data facilities, high-performance computation platforms, and open-source deep learning libraries. It provides all-in-one capacity covering server management, code repository, workflow orchestration software, and history recorder. 

It can be run from both local and remote (distributed) machines.

GeoWeaver is a community effort. Any contribution is welcome and greatly appreciated! 

[Geoweaver Online API](https://zihengsun.github.io/Geoweaver/)

##### Table of Contents

- [Software Goals](#software-goals)
- [Installation](#installation)
  * [Dependencies](#dependencies)
  * [Quick Install (Recommended Linux, Mac, Windows)](#quick-install)
  * [Build from source](#build-from-source)
  * [Reset Password for Localhost](#reset-password-for-localhost)
  * [Set up HTTP Proxy](#set-up-http-proxy)
- [Demo](#demo)
- [Tutorial](#tutorial)
- [Citation](#citation)
- [Dependencies](#dependencies)
- [License](#license)


# Software Goals

Only two things basically:

1. Make it time affordable for less-coder scientists (who know nothing about WfMS) to manage their data processing workflows

2. Preserve all the model run history and share them along with the code files

# Installation

## Dependencies

Java 1.8+ (OpenJDK 8 or higher)

!(only for install via docker) [Docker](https://docs.docker.com/install/) 18.09.1+

!(only for install via docker) [Docker-compose](https://docs.docker.com/compose/install/) 1.23.1+ 

## Quick Install

(Recommended for Linux, Mac, and Windows)

* Step 1: Download the latest version of [geoweaver.jar](https://github.com/ESIPFed/Geoweaver/releases/download/latest/geoweaver.jar)

* Step 2: Run the command: 

```shell
java -jar geoweaver.jar 
```

* Step 3: Open browser and enter: http://localhost:8070/Geoweaver/ .That's it!

## Build from source

Use maven to build. In the command line go to the root folder and execute `mvn install`. After a success build, the Geoweaver jar package will be under the directory: `Geoweaver/target/Geoweaver-<version>.jar`. 

## Reset Password for Localhost

Geoweaver will automatically create a password for localhost. It will only show once at first run of Geoweaver. It is recommended to copy and save it at a safe place. If forget or missed that password, please run the following command to reset:

```
java -jar geoweaver.jar resetpassword
```

## [Set up HTTP Proxy](docs/http-proxy.md)

# Tutorial

## [Create and Manipulate Hosts](docs/host.md)

## [Create and Run Processes](docs/process.md)

## [Create, Run, and Export Workflows](docs/workflow.md)

[Geoweaver Tutorial](https://andrewmagill.github.io/#/) - A beginner tutorial about what Geoweaver can do and how it works

# Demo

[A live demo site](https://cloud.csiss.gmu.edu/Geoweaver) is available in George Mason University.

# Citation

If you found Geoweaver helpful in your research, please cite: 

Sun, Z. et al., "Geoweaver: Advanced cyberinfrastructure for managing hybrid geoscientific AI workflows." ISPRS International Journal of Geo-Information 9, no. 2 (2020): 119.

# Dependencies

This project is impossible without the support of several fantastic open source libraries.

[d3.js](https://github.com/d3/d3) - BSD 3-Clause

[graph-creator](https://github.com/cjrd/directed-graph-creator) - MIT License

[bootstrap](https://github.com/twbs/bootstrap) - MIT License

[CodeMirror](https://github.com/codemirror/CodeMirror) - MIT License

[JQuery Terminal](https://github.com/jcubic/jquery.terminal) - MIT License

# [Community](docs/authors.md)

# License

MIT


