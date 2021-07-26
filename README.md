# Dockerizing your JVM Apps
### Best Practices & Common Pitfalls

1. Use small base images
    * `docker build -t demo:1 -f docker/01-basic-dockerfile .`
    * `docker build -t demo:2 -f ./docker/02-smaller-dockerfile .`
    * `docker build -t demo:3 -f docker/03-even-smaller-dockerfile .`
    * `docker images | grep demo ` (show differences in image size)

2. .dockerignore - exclude everything and add back what you need
    * Reduces build time
    * Helps avoid cases in which sensitive files get copied by mistake into an image (when copying directories)

3. Use unprivileged (non-root) containers:
    * `docker run -d --rm --name demo3 demo:3 && docker exec -it demo3 sh`                    
        * `ls -la` (application belongs to root)
        * `ps -ef` (java running as root)
        * `id` (root)
    * `docker build -t demo:4 -f docker/04-non-root-dockerfile .`
    * `docker run -d --rm --name demo4 demo:4 && docker exec -it demo4 sh`                    
        * Show that we're not running as root anymore:
        * `ls -la` (application belongs to demouser)
        * `ps -ef` (java running as demouser)
        * `id` (demouser)
        * `su -` (won't work)
        * `apk --no-cache add curl` : won't work

4. Minimize # of layers
    * Only the instructions RUN, COPY, & ADD create layers. Other instructions create temporary intermediate images, and do not increase the size of the build.
 
5. Use multi-stage builds
    * Multi-stage builds allow you to drastically reduce the size of your final image, without struggling to reduce the number of intermediate layers and files.
    * To keep your production image lean but allow for debugging, consider using the production image as the base image for the debug-image. Additional testing or debugging tooling can be added on top of the production image.

6. Leverage build cache
    * ADD and COPY: checksum. The rest (RUN): the command string itself is used to find a match.
    * clean up first: `docker rmi -f $(docker images 'demo*' -q) && docker builder prune`
    * `docker build -t demo:6 -f docker/06-layered-dockerfile .`
    * Edit DemoApplication & `./mvnw clean install`
    * `docker build -t demo:6 -f docker/06-layered-dockerfile` .  => steps 3, 4 & 7 will **not** be taken from the cache.
    * You can further reduce the image size by changing owner of directories **before** copying in your files, 
    and then copying your files to the image **while specifying the correct owner**:
```bash
COPY --from=builder --chown=$USER_NAME:$GROUP_NAME application/dependencies/ ./
```

7. Handle signals correctly:
    * `docker build -t demo:7 -f docker/07-with-script-shell-form .`
    * `docker run --rm -d --name demo7 demo:7 && docker exec -it demo7 sh`
    * `ps -ef` (see that we're running not as PID 1)
    * `time docker stop demo7` (10 seconds after which docker sends SIGKILL)
    * Then add `exec` in run.sh and do the whole thing again.

8. Configure your JVM
    * Run with 1G and 2G - show 25% default until (and including) 512MB heap, and SerialGC until 2G total ram.
    * Show serial with 2G and 1 cpu
```bash
docker run --rm --memory 2G --cpus 2 adoptopenjdk/openjdk11:alpine \
    sh -c "java -XX:+PrintFlagsFinal -version | grep -E ' MaxHeapSize|MaxRAMPercentage|UseG1GC|UseSerialGC'"

docker run --rm --memory 1G --cpus 2 adoptopenjdk/openjdk11:alpine \
    sh -c "java -XX:+PrintFlagsFinal -version | grep -E ' MaxHeapSize|MaxRAMPercentage|UseG1GC|UseSerialGC'"
    
docker run --rm --memory 256M --cpus 2 adoptopenjdk/openjdk11:alpine \
    sh -c "java -XX:+PrintFlagsFinal -version | grep -E ' MaxHeapSize|MaxRAMPercentage|UseG1GC|UseSerialGC'"
 
docker run --rm --memory 1G --cpus 2 -it adoptopenjdk/openjdk11:alpine
>  Runtime.getRuntime().availableProcessors()
```
* We can specify the jvm options by incorporating a custom ENV in our dockerfile
    * `docker build -t demo:10 -f docker/10-specify-jvm-options .`
    * `docker run  -it --rm --name demo10 -m512M --cpus 2 -e JAVA_OPTS='-XX:MaxRAMPercentage=75' demo:10`   
* But it's easier to use the built in JAVA_TOOL_OPTIONS:
    * `docker run  -it --rm --name demo3 -m512M --cpus 2 -e JAVA_TOOL_OPTIONS='-XX:MaxRAMPercentage=75' demo:3`

9. Other tips:
* RUN - Split long or complex RUN statements on multiple lines separated with backslashes to make your Dockerfile more readable, understandable, and maintainable.
* Sort multi-line arguments - This helps to avoid duplication of packages and make the list much easier to update. This also makes PRs a lot easier to read and review

```bash
# Ubuntu:
RUN apt-get update && apt-get install -y \
  cvs \
  git \
  mercurial \
  subversion \
  && rm -rf /var/lib/apt/lists/*

# alpine:
RUN apk update && apk add \
    cvs \
    git \
    mercurial \
    subversion \
    && rm -rf /var/cache/apk/* 

or:
RUN apk add --no-cache \
    cvs \
    git \
    mercurial \
    subversion
```
