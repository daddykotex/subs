# Subs web server

This is a pure implementation of the web server that serves https://subs.play


## Design

It's an idea I had and the design is very experimental. I'll lay out the details in [this document](https://docs.google.com/document/d/1CV4-fgR7CYGJE2sigwFuxj1OML3WoD7lqB0AESv0Prc/edit?usp=sharing) because it will be easier to edit and add drawings and stuff. When I feel the design is mature enough, I will bring it into this README.


## Database

This project uses PostgreSQL 9.6. Export the following variables, they will be used for the rest of the section:
```
export DB_HOST=...
export DB_PORT=...
export DB_NAME=...
export DB_USER=...
export DB_PASS=...
export DB_JDBC_URL="jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_NAME"
```

During development, I run the database with docker:
```
docker run -d -e POSTGRES_PASSWORD=postgres --name postgres postgres:9.6

```

### Connection

I use docker to run psql:
```
# local container
docker run --rm -it --net container:postgres --entrypoint=psql postgres:9.6 "postgres://$DB_USER:$DB_PASS@$DB_HOST/$DB_NAME"

#remote
docker run --rm -it --entrypoint=psql postgres:9.6 "postgres://$DB_USER:$DB_PASS@$DB_HOST/$DB_NAME"
```

### Schema

I use Flyway to manage the database schema. Since I use SBT for this build, I thought it would make sense to use the Flyway's SBT plugin. Read the docs: [https://flywaydb.org/documentation/sbt/](https://flywaydb.org/documentation/sbt/).

To check the state of the data base, run:
```
sbt -Dflyway.user=$DB_USER -Dflyway.password=$DB_PASS -Dflyway.url=$DB_JDBC_URL flywayInfo
```

To run the migrations:
```
sbt -Dflyway.user=$DB_USER -Dflyway.password=$DB_PASS -Dflyway.url=$DB_JDBC_URL flywayMigrate
```