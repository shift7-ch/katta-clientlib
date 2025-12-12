FROM ubuntu:24.04
RUN apt-get update && apt-get install -y wget curl nginx systemctl gettext-base && \
    wget https://dl.min.io/server/minio/release/linux-amd64/minio.RELEASE.2025-09-07T16-13-09Z -O minio && \
    chmod +x minio &&\
    systemctl enable nginx.service

ENTRYPOINT ["/minio"]
