    This utility tries to detect latency issues across cluster nodes. 
    Run it simultaneously on two or more cluster nodes and they will
    report exactly when changes are seen on the clustersync folder.
    
    Usage: java -jar rscachelatency.jar sleepInMilliseconds directoryName
    e.g.: java -jar rscachelatency.jar 50 /home/csuser/Shared/clustersync
