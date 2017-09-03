# Subs web server

This is a pure implementation of the web server that serves https://subs.play


## Design

It's an idea I had and the design is very experimental. I'll lay out the details in [this document](https://docs.google.com/document/d/1CV4-fgR7CYGJE2sigwFuxj1OML3WoD7lqB0AESv0Prc/edit?usp=sharing) because it will be easier to edit and add drawings and stuff. When I feel the design is mature enough, I will bring it into this README.


## Database

This project uses PostgreSQL 9.6.

### Connection

I use docker to run psql:
```
DB_HOST=...
DB_PORT=...
DB_NAME=...
DB_USER=...
DB_PASS=...
docker run --rm -it --entrypoint=psql postgres:9.6 "postgres://$DB_USER:$DB_PASS@$DB_HOST/$DB_NAME"
```