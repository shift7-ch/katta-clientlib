# Binaries are no longer available from dl.min.io (410 Gone)
FROM quay.io/minio/mc:RELEASE.2024-11-05T11-29-45Z AS mc

FROM ubuntu:24.04
COPY --from=mc /usr/bin/mc /mc
RUN apt-get update && apt-get install -y wget curl nginx systemctl jq gettext-base && \
    systemctl start nginx

ENTRYPOINT ["/mc"]
