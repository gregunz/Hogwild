FROM openjdk:latest
WORKDIR /opt/docker
ADD opt /opt
RUN ["chown", "-R", "daemon:daemon", "/opt"]
USER daemon
RUN ["chmod", "+x", "/opt/docker/bin/hogwild"]
ENTRYPOINT ["/opt/docker/bin/hogwild"]
CMD []
