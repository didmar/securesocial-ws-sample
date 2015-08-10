Webservice based on Securesocial + Play
========================================

This sample code shows how to use [Securesocial](http://securesocial.ws/) and [Play](https://www.playframework.com) to create a webservice in Scala.

----
## How to use this

I had to separate the webservice from the front-end part, using HTTP headers for authentication.
For the signup part, it works as such:

- your front-end calls /signup with the user email adress in the header
- an email is sent, containing the following link: http://www.yourfrontend.com/signup/some_signup_token
- your front-end calls /signup/some_token with user credentials as a JSON object in the header

See the [Testing with CURL](##testing-with-curl)

Possible improvement: modify the mail token such that the front-end is not required (send the credentials first and then confirm)

Note that for simplicity, the authentification service is a sample in memory service from Securesocial.
You should replace it if you plan to use this code in production !

----
## Requirements

Requires Typesafe [Activator](https://www.typesafe.com/get-started) (>= 1.3.2)

----
## Configuration

In conf/application.conf:

- Configure the smtp section
- Set application.mailer.baseURL with the url of your front-end

----
## Running the webservice

>activator run

The webservice will by available on port 9000 

----
## Testing with cURL

># Signup with your email adress
>curl -v -H "Content-Type: text/plain" -d "jdoe@doe.com" -X POST http://127.0.0.1:9000/signup
># Click on the link you received to confirm
># Or, if you don't have a front-end providing the signup form, copy the signup token:
>export SIGNUP_TOKEN=abc123...
>curl -v -H "Content-Type: application/json" -X POST http://127.0.0.1:9000/signup -d '{"userName":"jdoe@doe.com","firstName":"john","lastName":"doe",          "password":"secret"}'
>
># Retrieve an authentication token
>export TOKEN=$(curl -v -d "username=jdoe@doe.com" -d "password=secret" http://127.0.0.1:9000/authenticate/userpass)
>
># Call the webservice
>curl -v -H "X-Auth-Token: $TOKEN" http://127.0.0.1:9000/test/
