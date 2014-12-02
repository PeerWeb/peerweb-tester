PeerWeb Tester
==============

PeerWeb testing tool, which uses [Selenium][selenium] to traverse a series of web pages in a
configurable fashion. See `BrowserTest.java` for details on available options that can be passed to
the `run_selenium.sh` wrapper script. Hosts, ports, and the base visit URL are pulled from
`selenium_config.sh`.

Dependencies
------------

The tester must be compiled and run using [Java][java] (tested and run in Java 6). A `pom.xml` is
provided for building with [Maven][maven] builds.

A Selenium instance or Selenium [Grid][grid] hub is required for tester operation; the instance or
(for Grid) nodes must have the [ChromeDriver][chromedriver] available.

Compiling
---------

```sh
mvn clean package
```

Running
-------

```sh
./run_selenium.sh ${options}
```

[selenium]:     http://www.seleniumhq.org/
[java]:         https://www.oracle.com/java/
[maven]:        https://maven.apache.org/
[grid]:         https://code.google.com/p/selenium/wiki/Grid2
[chromedriver]: https://sites.google.com/a/chromium.org/chromedriver/
[node]:         http://nodejs.org/
