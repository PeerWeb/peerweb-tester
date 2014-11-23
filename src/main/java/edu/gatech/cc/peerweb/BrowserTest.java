package edu.gatech.cc.peerweb;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PatternOptionBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * PeerWeb tester using Selenium grid to load one or more test websites.
 *
 * References
 * ----------
 *  - https://stackoverflow.com/questions/13724778/how-to-run-selenium-webdriver-test-cases-in-chrome
 *  - https://sites.google.com/a/chromium.org/chromedriver/getting-started
 *  - https://code.google.com/p/chromedriver/issues/detail?id=799
 *  - https://stackoverflow.com/questions/19026295/run-chrome-browser-in-inconginto-mode-in-selenium
 *  - http://itsallabtamil.blogspot.com/2013/02/setting-up-chrome-firefox-ec2-selenium-java.html
 *  - https://stackoverflow.com/questions/2835179/how-to-get-selenium-to-wait-for-ajax-response
 *  - https://groups.google.com/forum/#!topic/selenium-users/ybksxVkadds
 *  - http://docs.seleniumhq.org/docs/04_webdriver_advanced.jsp
 *  - https://stackoverflow.com/questions/9567682/how-to-set-up-selenium-with-chromedriver-on-jenkins-hosted-grid
 */
public class BrowserTest {

    private static enum IterationStrategy {
        ADDITIVE,
        EXPONENTIAL,
        SLOW
    }

    private static class ChromeWrapper implements Runnable {

        private final String name;
        private final Integer minStartWait;
        private final Integer maxStartWait;
        private final Integer minLinger;
        private final Integer maxLinger;
        private final String visitUrl;
        private final CyclicBarrier startBarrier;
        private final URL hubUrl;
        private WebDriver driver;
        private final Random random;

        public ChromeWrapper(final String name,
                             final Integer minStartWait,
                             final Integer maxStartWait,
                             final Integer minLinger,
                             final Integer maxLinger,
                             final String hubUrl,
                             final String visitUrl,
                             final CyclicBarrier startBarrier) {

            this.name = name;
            this.minStartWait = minStartWait;
            this.maxStartWait = maxStartWait;
            try {
                this.hubUrl = new URL(hubUrl + "/wd/hub");
            } catch (final MalformedURLException ex) {
                throw new RuntimeException(ex);
            }
            this.minLinger = minLinger;
            this.maxLinger = maxLinger;
            this.visitUrl = visitUrl;
            this.startBarrier = startBarrier;
            this.random = new SecureRandom();

        }

        private void waitForStart() {
            // wait until all the drivers are ready
            try {
                startBarrier.await();
            } catch (final InterruptedException ex) {
                ex.printStackTrace(); // ignore
            } catch (final BrokenBarrierException ex) {
                ex.printStackTrace(); // ignore
            }
        }

        private void waitQuietly(final long forMs) {

            try {
                Thread.sleep(forMs);
            } catch (final InterruptedException ex) {
                System.err.println("Sleep was interrupted; continuing...");
                ex.printStackTrace();
            }
        }

        private void waitForPageComplete() {
            WebDriverWait waitForPageComplete = new WebDriverWait(driver, 30000);
            waitForPageComplete.until(ExpectedConditions.presenceOfElementLocated(By.id("peerweb_page_complete")));
        }

        private void randomWait(Integer minSecInc, Integer maxSecIncl) {

            // http://stackoverflow.com/a/6029518
            Integer waitForSecs = random.nextInt(maxSecIncl - minSecInc + 1) + minSecInc;

            // http://stackoverflow.com/q/12858972
            waitQuietly(waitForSecs * 1000);

        }

        public void run() {

            // http://stackoverflow.com/a/3294313
            String visitUrlWithHash = visitUrl + '#' + name;

            DesiredCapabilities capabilities = DesiredCapabilities.chrome();
            {
                ChromeOptions options = new ChromeOptions();
                options.addArguments("test-type");
                options.addArguments("incognito");
                capabilities.setCapability(ChromeOptions.CAPABILITY, options);
            }

            driver = new RemoteWebDriver(hubUrl, capabilities);
            waitForStart();

            /*
             * The test involves a series of three web pages. After a page has loaded, the client will wait for a
             * random amount of time between 5 and 30 seconds before "clicking" a link to go to the next page.
             *
             * First, we wait for a random amount of time, allowing us to stretch visits over a long time (specify a
             * higher max) or quickly (specify a low max, or 0 for all instantaneously).
             */

            randomWait(minStartWait, maxStartWait);

            driver.get(visitUrlWithHash);
            waitForPageComplete();
            randomWait(minLinger, maxLinger);

            while (true) {
                WebElement next;
                try {
                    next = driver.findElement(By.id("next"));
                } catch (final NoSuchElementException none) {
                    break;
                }
                next.click();
                waitForPageComplete();
                randomWait(minLinger, maxLinger);
            }

            driver.quit();
            System.out.println(String.format("Instance %s COMPLETE!", name));
        }

    }

    public static void main(final String[] args) throws MalformedURLException, ParseException {
        // https://commons.apache.org/proper/commons-cli/usage.html
        // https://code.google.com/p/selenium/wiki/Grid2

        Options options = new Options();
        options.addOption("hub", true, "Selenium Grid hub URL");
        options.addOption("visit", true, "URL each instance should visit");
        options.addOption("initial", true, "number of instances to create in initial iteration");
        options.addOption("iterations", true, "number of iterations (each spawns (last)^2 instances)");
        options.addOption("spacing", true, "spacing (in seconds) between instances");
        options.addOption("maxWait", true, "maximum amount of time (in seconds) an instance can wait to start");
        options.addOption("strategy", true, "strategy for traffic increase ('additive' or 'exponential')");
        options.addOption("minLinger", true, "minimum amount of time (in seconds) to wait after a page has loaded");
        options.addOption("maxLinger", true, "maximum amount of time (in seconds) to wait after a page has loaded");

        CommandLineParser parser = new BasicParser();
        CommandLine cli = parser.parse(options, args);

        String hub = cli.getOptionValue("hub");
        String visit = cli.getOptionValue("visit");
        Integer initial = Integer.parseInt(cli.getOptionValue("initial", "1"));
        Integer iterations = Integer.parseInt(cli.getOptionValue("iterations", "1"));
        Integer spacing = Integer.parseInt(cli.getOptionValue("spacing", "0"));
        Integer maxWait = Integer.parseInt(cli.getOptionValue("maxWait", "0"));
        IterationStrategy strategy = IterationStrategy.valueOf(cli.getOptionValue("strategy", "additive").toUpperCase());
        Integer minLinger = Integer.parseInt(cli.getOptionValue("minLinger", "0"));
        Integer maxLinger = Integer.parseInt(cli.getOptionValue("maxLinger", "0"));

        for (int iteration = 0; iteration < iterations; iteration++) {

            Integer instances;
            switch (strategy) {
                case ADDITIVE:
                    instances = initial + iteration;
                    break;
                case EXPONENTIAL:
                    instances = new Double(Math.pow(initial, iteration)).intValue();
                    break;
                case SLOW:
                    instances = initial;
                    break;
                default:
                    throw new RuntimeException();
            }
            CyclicBarrier startBarrier = new CyclicBarrier(instances);

            Integer startTime = iteration * spacing;
            Integer endTime = startTime + maxWait;

            System.out.println(String.format("---- Iteration #%d (%d Instances) ----", iteration, instances));
            for (int instance = 0; instance < instances; instance++) {
                String name = iteration + "." + instance;
                System.out.println(String.format("Instance( instance = %d, startTime = %d, endTime = %d )", instance, startTime, endTime));
                new Thread(new ChromeWrapper(name, startTime, endTime, minLinger, maxLinger, hub, visit, startBarrier)).start();
            }
        }
    }
}
