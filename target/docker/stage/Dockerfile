FROM openjdk:jre-alpine
RUN apk add --no-cache bash
ADD opt /opt
WORKDIR /opt/docker/bin
RUN ["chmod", "+x", "hogwild"]
RUN ["chmod", "+x", "start_node.sh"]
ENTRYPOINT ["sh" , "start_node.sh"]
CMD []
