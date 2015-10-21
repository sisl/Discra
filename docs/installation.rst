==================
Installing Discra
==================

First clone the repository to your local directory. Open up your console and type

::

  git clone "https://github.com/sisl/Discra"

This step does not install all the dependencies, which we'll be going through next.

Java SDK
========

Our code is written in Scala, which uses JVM. Download Java SE Development Kit 8 for your system if you don't already have it.

IntelliJ IDEA
=============

For our guide, we recommend that the user use the IntelliJ IDEA IDE. Go `here <https://www.jetbrains.com/idea/download/>`_ and download the community edition of the IDE. Follow the installation instructions on their webpage and remember to get the Scala plugin to run our code.

After all that, we want to import the project from the welcome window:

  1. Click the "Import Project" button, which will bring up a window telling you to select the directory to import.

  2. Go to the root of the Discra directory, expand the "src" folder, select the "advisor" folder, and click the OK button. Warning: Do not import the "src" or "root" folder!

  3. Select the "Import project from external model" option and choose "SBT" before clicking the Next button.

  4. Check all the tickboxes (use auto-import, create directories for empty content roots automaticallly, download sources and docs, and download SBT sources and docs).

  5. Select your project SDK and choose JDK 1.8. You might have to click the "New" button and search for it if it's not available from the dropdown menu.

  6. The project format should be .idea (directory based), and you shouldn't have to mess with the global SBT settings.

  7. With all that out of the way, click "Finish" and wait for IntelliJ IDEA to import your local version of Discra. You can ignore the warning about the unregistered VCS root and the Scala version for now.

  8. Repeat the above steps for the "ingestor" and "simulator" directories. We do this as they're separate Spark applications.

  9. For our conflict avoidance algorithm, we've included a smaller version of our utility function lookup table called ``utility.csv``. In order for your local Discra advisor application to find it, you'll have to input the absolute path to the lookup table, otherwise it'll throw a warning about no utility table found. Simply replace the dummy path ``<InsertYourOwnUtilityCSVAbsolutePath>`` in the two following files.

    (a) ``[discra-root]/src/advisor/src/main/scala/spark/worker/policy/Const.scala``
    (b) ``[discra-root]/src/advisor/src/test/scala/spark/worker/Const.scala``

Apache Kafka
============

While IntelliJ IDEA and SBT already takes care of the necessary Kafka packages for our code, we still need to download `Kafka <http://kafka.apache.org/downloads.html>`_ to run the publication-subscription server.

Apache Spark
============

If you've used IntelliJ IDEA, you won't have to install Spark outside of it, since the build dependencies include the necessary Spark packages. Otherwise, you can download `Spark <http://spark.apache.org/downloads.html>`_ and follow the installation instructions.
