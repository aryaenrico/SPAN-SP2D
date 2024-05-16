# SP2D_CHECK_NEGATIVE_AMOUNT

SPAN SP2D CHECK NEGATIVE AMOUNT AND OTHER CUSTOM HANDLER FOR SPAN PROJECT


## Prerequisite

- mysql 
- oracle
- java : 8+
- maven : 3.6.3
- Oracle 19 or 23 (Please see notes in file `span-play-project/database/oracledb/span-ora-sql-notes.txt`)

## Pre-Installation

- Local Running/serve
    ``` bash
  # Command: [checkNegativeAmount/excludeOutOfBalance/includeOutOfBalance]
  $ mvn compile exec:java -Dexec.mainClass="com.bsi.MainCHK" -Dexec.args="path-to-file/conf bo2span.properties command"
    ``` 

- Build for production
  ``` bash     

  # install the dependencies
  $ mvn install:install-file -Dfile=tesDs/digitalSignature.jar -DgroupId=com.bsm.ds -DartifactId=digital-signature -Dversion=1.0.0 -Dpackaging=jar
  
  $ mvn install:install-file -Dfile=tesDs/ojdbc8.jar -DgroupId=com.oracle.jdbc -DartifactId=ojdbc8 -Dversion=19.22.0.0.0 -Dpackaging=jar

  # clean the projects
  $ mvn clean

  # build jar
  $ mvn install
  ```
  Artifacts path:  `target/SP2D_CHECK_NEGATIVE_AMOUNT-1.0-jar-with-dependencies.jar`

## Deploy on server

1. rename  `SP2D_CHECK_NEGATIVE_AMOUNT-1.0-jar-with-dependencies.jar` with `span_custom_handler.jar`


2. upload upload the file into `/home/span/apps/bo2span/tools/bo2span_commands`

3. change the configuration files in `/home/span/apps/bo2span/data/config/bo2span.properties` for this section
    ```bash
    db_bo2span_database_name=SPANDB
    db_bo2span_host_name=jdbc:oracle:thin:@//localhost:1521/SPANDB
    db_bo2span_password=1q2w3e4r5t6y
    db_bo2span_port=1521
    db_bo2span_user_name=SPAN
    ```
