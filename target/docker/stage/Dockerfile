FROM openjdk:jre-alpine
WORKDIR /opt/docker
ADD opt /opt
RUN ["chmod", "+x", "/opt/docker/bin/hogwild"]
RUN ["chmod", "+x", "/opt/docker/bin/start_node.sh"]
CMD ["/opt/docker/bin/start_node.sh"]
