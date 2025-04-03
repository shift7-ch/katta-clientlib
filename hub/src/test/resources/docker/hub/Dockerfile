ARG KATTA_SERVER_IMAGE
FROM ${KATTA_SERVER_IMAGE}

RUN apt-get update && apt-get -y install curl wget telnet net-tools nginx systemctl  gettext-base && systemctl start nginx
