FROM vmware/photon-closure-runner_python_3.4.3_base:1.0

ARG TASK_URI
ARG TOKEN
ENV TASK_URI ${TASK_URI}
ENV TOKEN ${TOKEN}

COPY app/*.* /app/

ENTRYPOINT ["./run.sh"]

RUN chmod +x /app/*.sh && \
    python3 -u /app/dep_install.py



