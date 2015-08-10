Webservice based on Securesocial + Play
========================================

This sample code shows how to use [Securesocial](http://securesocial.ws/) and [Play](https://www.playframework.com) to create a webservice in Scala.
The reason for this if that I couldn't find a code that did NOT require authenticating through a webpage.

Note that for simplicity, the authentification service is a sample in memory service from Securesocial.
You should replace it if you plan to use this code in production !

----
## Requirements

Requires Typesafe [Activator](https://www.typesafe.com/get-started) (>= 1.3.2)

----
## Configuration

You MUST configure the smtp section conf/application.conf

----
## Running the webservice

>activator run

The webservice will by available on port 9000 (http) and 9100 (https)

----
## Testing with CURL

># Signup with your email adress
>curl -v -H "Content-Type: application/json" -X POST http://127.0.0.1:9000/signup -d '{"userName":"jdoe@doe.com","firstName":"john","lastName":"doe","password":"secret"}'
>
># Click on the link you received to confirm
>
># Retrieve an authentication token
>export TOKEN=$(curl -v -d "username=jdoe@doe.com" -d "password=secret" http://127.0.0.1:9000/authenticate/userpass)
>
># Call the webservice
>curl -v -H "X-Auth-Token: $TOKEN" http://127.0.0.1:9000/test/
