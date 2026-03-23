###How To Use
1. Make sure you are using a newer version of java maven seems to default to 1.8
    -In VS code you want to do ctrl+shift+p
    -search "java: Configure Java runtime"
    -JDK Runtime tab -> the JDK selector -> choose new verison of java I use SE21
2. Make sure you are on the university wifi (secure not the public one) or the ubc vpn through cisco
3. Run this command
    -mvn exec:java "-Dexec.mainClass=ubc.cosc322.COSC322Test" "-Dexec.args=local local"
4.YOUR DONE 

###BRANCH INFO
This branch is to improve off of baseline 2. Base line 2 can be found in the
JamesAtempt2ToImproveBaseline1 branch

###I Can't see The white queens!!!
1.This is an issue caused by white queens being misnamed instead of .png they are .PNG
2.You need to get to it to rename it this is the path (ps make sure you have hidden folders visable)
3.C:\Users\YOUR-NAME-HERE\.m2\repository\ubc-yong-gao\ygraph-ai-smartfox-client\2.1
4.Once you are here you will see a JAR file you need to open it with something like winRAR (any extraction tool)
5.After that in the extraction tool DO NOT EXTRACT IT, navigate through the folders like such
6.ygraph\ai\smartfox\games\amazons\images
7.Here you will see the white-queen.PNG rename it to white-queen.png
8.Solved