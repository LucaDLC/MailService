# MailService 
### A Java Client-Server application to implement an email service built with JavaFX 

- The ClientAPP required a login and support the basic email operation like send an email, reply, reply all and forward it with an user-friendly UI

- The ServerAPP manage the registered users, adding or removing emails, and manage client request as emails storage and emails fetch


Both the side are built following the MVC architecture based on the Observer-Observable pattern, with thread/threadPools to handle the multiple operation (serve multiple client from server, updating client and server UI), Socket to build the connections between between the 2 hosts and Synchronized data structure/lock for concurrent operations (writing reading on the email server database).
