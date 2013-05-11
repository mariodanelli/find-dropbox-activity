find-dropbox-activity
=====================

The project is based on the Dropbox API and is used to retrieve activities on the file/folder operations. There are 2 versions, one using DROPBOX API and one other using AsyncHTTPClient.

The project has been developed with Eclipse 3.6. It's based on a PostgreSQL 8.3 DB (the file "MarioDanelliTestApp.sql" containing the instructions to create the tables is in "sql" folder). The DB connection parameters have to be modified into a configuration file.
The db consists of two tables:
- users: containg the user credentials and the cursor used to store which is the last cursor retrieved (the sql script upload 3 test 
            e-mail that must be replaced with some real e-mail).
- activites: containg activities entries metadata.

The java code read configuration properties from the file "configuration.properties" contained into the "conf" folder.
It's composed of two main classes:
- RetrieveCredentials: Used to retrieve and store user credentials (the program retrieve the credentials only for the first user and so if 
                                there are 3 users without credentials the program must be launched 3 times). So this program must be only 
                                for the credentials when a new user is added.
- ActivitiesThread: The program that creates a thread for each user with credentials and this thread every 60 seconds (the seconds 
                           number can be modified in the configuration file) check for new activities and in case store them into the DB 
                           together with the new cursor. Together with the store on the DB the program shows informations on activities that 
                           are stored into the DB.
In addition to these core classes there are some help classes:
- Utility: contains the code for the connection to the DB and for configuration file reading.
- UserDAO/ActivityDAO: contain the code for the operations (store/retrieve) on the DB, each regarding its object
- UserBean/ActivityBean: represent the JavaBean classes to mantain the User and Activity data into the java application

At the moment the code doesn't contain ant code line regarding a correct exception handling (only printStackTrace), logging on file (other important point). In addition is not present a real ConnectionManager but every time is needed a connection this is created and closed just used.

The use of the DB instead of file can ensure transactions, back-up, concurrent access. At each restart of the ActivitiesThread the last cursor read by the application is retrieved by the DB. This ensure:
- When the DropBox is down we have the activity history in our DB
- When our server is down at the restart we will restart from a previous situation and not with a request for all the activities history
