# Binaries are no longer available from dl.min.io (410 Gone)
FROM quay.io/minio/minio:RELEASE.2025-09-07T16-13-09Z AS minio

FROM ubuntu:24.04
COPY --from=minio /usr/bin/minio /minio
RUN apt-get update && apt-get install -y wget curl nginx systemctl gettext-base && \
    systemctl enable nginx.service

ENTRYPOINT ["/minio"]
