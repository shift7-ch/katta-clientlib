FROM ubuntu:24.04
RUN apt-get update && apt-get install -y wget curl nginx systemctl jq && \
    wget https://dl.min.io/client/mc/release/linux-amd64/archive//mc.RELEASE.2024-11-05T11-29-45Z -O mc && \
    chmod +x mc &&\
    systemctl start nginx

ENTRYPOINT ["/mc"]
