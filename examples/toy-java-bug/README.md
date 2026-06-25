# toy-java-bug

A tiny broken Java project used to demonstrate the sac-agent4j loop.

The bug is intentional:

```java
return left - right;
```

The test expects addition, so `./test.sh` fails until the agent applies the patch.
