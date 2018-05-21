FROM openjdk:latest
WORKDIR /opt/docker
ADD opt /opt
RUN chown -R daemon:daemon /opt
USER daemon
ENTRYPOINT ["/opt/docker/bin/hogwild"]
CMD []
