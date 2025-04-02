FROM ubuntu:24.04
RUN apt-get update && apt-get install -y wget curl nginx systemctl && \
    wget https://dl.min.io/server/minio/release/linux-amd64/archive/minio.RELEASE.2024-11-07T00-52-20Z -O minio && \
    chmod +x minio &&\
    systemctl enable nginx.service

ENTRYPOINT ["/minio"]
