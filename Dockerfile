FROM java:8

RUN  apt-get update && apt-get install -y curl git maven unzip

COPY ./entrypoint.sh /

RUN chmod +x /entrypoint.sh

EXPOSE 8080

CMD ["./entrypoint.sh"]
