FROM ubuntu:24.04
ARG TARGETARCH
RUN apt-get update && apt-get install -y wget curl nginx systemctl jq gettext-base && \
    wget https://dl.min.io/client/mc/release/linux-${TARGETARCH}/archive//mc.RELEASE.2024-11-05T11-29-45Z -O mc && \
    chmod +x mc &&\
    systemctl start nginx

ENTRYPOINT ["/mc"]
