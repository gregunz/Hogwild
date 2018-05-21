# # Scala and sbt Dockerfile #
FROM openjdk:8u171

# Env variables
ENV SCALA_VERSION 2.12.6
ENV SBT_VERSION 1.1.5

ENV SCALA_HOME /docker-scala-home
ENV PATH=$SCALA_HOME/scala-$SCALA_VERSION/bin:$PATH

# Scala expects this file
RUN touch /usr/lib/jvm/java-8-openjdk-amd64/release

# Install Scala ## Piping curl directly in tar
RUN \
  mkdir $SCALA_HOME && \
  curl -fsL https://downloads.typesafe.com/scala/$SCALA_VERSION/scala-$SCALA_VERSION.tgz | tar xfz - -C $SCALA_HOME

# Install sbt
RUN \
  curl -L -o sbt-$SBT_VERSION.deb https://dl.bintray.com/sbt/debian/sbt-$SBT_VERSION.deb && \
  dpkg -i sbt-$SBT_VERSION.deb && \
  rm sbt-$SBT_VERSION.deb && \
  apt-get update && \
  apt-get install sbt && \
  sbt sbtVersion

# Define working directory
WORKDIR /Hogwild
ADD . /Hogwild

#CMD sbt
#CMD /bin/bash start_worker.sh $mode $status $coord_ip
CMD /bin/bash start_node.sh $MY_POD_NAME $MY_POD_IP
