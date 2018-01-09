# Consonance-webservice

This is the prototype web service for consonance. 

## Usage

### Starting Up

1. Fill in the template hello-world.yml and stash it somewhere outside the git repo (like ~/.consonance)
2. Start with `java -jar target/consonance-webservice-*SNAPSHOT.jar server ~/.consonance/run-fox.yml`

### View Swagger UI

1. Browse to [http://localhost:8080/static/swagger-ui/index.html](http://localhost:8080/static/swagger-ui/index.html)

## TODO

1. we need to define how this interacts with a single sign-on service
   1. in general, users should be able to list their own information (such as tokens and repos)
   2. only admin users (or our other services) should be able to list all information  
